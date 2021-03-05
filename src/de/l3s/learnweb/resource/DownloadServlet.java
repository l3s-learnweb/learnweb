package de.l3s.learnweb.resource;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.user.User;
import de.l3s.util.bean.BeanHelper;

/**
 * Servlet for Streaming Files to the Clients Browser.
 */
@WebServlet(name = "DownloadServlet", urlPatterns = "/download/*", loadOnStartup = 2)
public class DownloadServlet extends HttpServlet {
    private static final long serialVersionUID = 7083477094183456614L;
    private static final Logger log = LogManager.getLogger(DownloadServlet.class);

    private static final int CACHE_DURATION_IN_SECOND = 60 * 60 * 24 * 365; // 1 year
    private static final long CACHE_DURATION_IN_MS = CACHE_DURATION_IN_SECOND * 1000L;
    private static final int BUFFER_SIZE = 8192; // 8KB
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    private static final String URL_PATTERN = "/download/";

    @Inject
    private FileDao fileDao;

    @Inject
    private LogDao logDao;

    /**
     * Process HEAD request. This returns the same headers as GET request, but without content.
     */
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Process request without content.
        processRequest(request, response, false);
    }

    /**
     * Process GET request.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Process request with content.
        processRequest(request, response, true);
    }

    /**
     * Process the actual request.
     *
     * @param request The request to be processed.
     * @param response The response to be created.
     * @param content Whether the request body should be written (GET) or not (HEAD).
     * @throws IOException If something fails at I/O level.
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content) throws IOException {
        // extract the file id from the request string
        String requestString = request.getRequestURI();
        // remove urlPattern from requestString
        requestString = requestString.substring(requestString.indexOf(URL_PATTERN) + URL_PATTERN.length());

        int slashIndex = requestString.indexOf('/');
        if (slashIndex == -1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String requestFileId = requestString.substring(0, slashIndex);
        //String requestFileName = requestString.substring(index + 1);

        int fileId = NumberUtils.toInt(requestFileId);

        if (fileId == 0) { // download url is incomplete
            String referrer = request.getHeader("referer");

            // only log the error if the referrer is uni-hannover.de. Otherwise we have no chance to fix the link
            Level logLevel = StringUtils.contains(referrer, "uni-hannover.de") ? Level.ERROR : Level.WARN;
            log.log(logLevel, "Invalid download URL: {}; {}", requestString, BeanHelper.getRequestSummary(request));

            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Prepare streams.
        RandomAccessFile input = null;
        OutputStream output = null;

        try {
            response.setHeader("Access-Control-Allow-Origin", "*");

            // Check if file actually exists in filesystem.
            File file = fileDao.findById(fileId).orElse(null);
            if (null == file) {
                log.warn("Requested file {} does not exist or was deleted", fileId);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            long lastModified = file.getLastModified().toEpochSecond(ZoneOffset.UTC);
            String eTag = file.getName() + "_" + file.getLength() + "_" + lastModified;
            long expires = System.currentTimeMillis() + CACHE_DURATION_IN_MS;

            /* Validate request headers for caching */

            // If-None-Match header should contain "*" or ETag. If so, then return 304.
            String ifNoneMatch = request.getHeader("If-None-Match");
            if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.setHeader("ETag", eTag);
                response.setDateHeader("Expires", expires);
                return;
            }

            // If-Modified-Since header should be greater than LastModified. If so, then return 304.
            // This header is ignored if any If-None-Match header is specified.
            long ifModifiedSince = -1;
            try {
                ifModifiedSince = request.getDateHeader("If-Modified-Since");
            } catch (IllegalArgumentException e) {
                log.error("Illegal If-Modified-Since header: {}; {}", e.getMessage(), BeanHelper.getRequestSummary(request));
            }

            if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.setHeader("ETag", eTag);
                response.setDateHeader("Expires", expires);
                return;
            }

            /* Validate request headers for resume */

            // If-Match header should contain "*" or ETag. If not, then return 412.
            String ifMatch = request.getHeader("If-Match");
            if (ifMatch != null && !matches(ifMatch, eTag)) {
                response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                return;
            }

            // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
            long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
            if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
                response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                return;
            }

            /* Validate and process range */

            long length = file.getLength();
            // Prepare some variables. The full Range represents the complete file.
            Range fullRange = new Range(0, length - 1, length);
            List<Range> ranges = new ArrayList<>();

            // Validate and process Range and If-Range headers.
            String range = request.getHeader("Range");
            if (range != null) {

                // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
                if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                    response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return;
                }

                // If-Range header should either match ETag or be greater then LastModified. If not, then return full file.
                String ifRange = request.getHeader("If-Range");
                if (ifRange != null && !ifRange.equals(eTag)) {
                    try {
                        long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                        if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                            ranges.add(fullRange);
                        }
                    } catch (IllegalArgumentException ignore) {
                        ranges.add(fullRange);
                    }
                }

                // If any valid If-Range header, then process each part of byte range.
                if (ranges.isEmpty()) {
                    for (String part : range.substring(6).split(",")) {
                        // Assuming a file with length of 100, the following examples returns bytes at:
                        // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                        long start = sublong(part, 0, part.indexOf('-'));
                        long end = sublong(part, part.indexOf('-') + 1, part.length());

                        if (start == -1) {
                            start = length - end;
                            end = length - 1;
                        } else if (end == -1 || end > length - 1) {
                            end = length - 1;
                        }

                        // Check if Range is syntactically valid. If not, then return 416.
                        if (start > end) {
                            response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                            return;
                        }

                        // Add range.
                        ranges.add(new Range(start, end, length));
                    }
                }
            }

            if (file.getType() == File.TYPE.MAIN) {
                HttpSession session = request.getSession(true);
                User user = null;
                Integer userId = (Integer) session.getAttribute("learnweb_user_id");

                if (userId != null) {
                    user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
                }

                if (null != user) {
                    logDao.insert(user, Action.downloading, 0, file.getResourceId(), Integer.toString(file.getId()), session.getId());
                }
            }

            /* Prepare and initialize response */
            String contentType = file.getMimeType() != null ? file.getMimeType() : getServletContext().getMimeType(file.getName());
            boolean acceptsGzip = false;
            String disposition = "inline";

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            if (contentType.startsWith("text")) {
                // If content type is text, then determine whether GZIP content encoding is supported by
                // the browser and expand content type with the one and right character encoding.
                String acceptEncoding = request.getHeader("Accept-Encoding");
                acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
                contentType += ";charset=UTF-8";
            } else if (!contentType.startsWith("image")) {
                // Else, expect for images, determine content disposition. If content type is supported by
                // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
                String accept = request.getHeader("Accept");
                disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
            }

            // Initialize response.
            response.reset();
            response.setBufferSize(BUFFER_SIZE);
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Content-Disposition", disposition + ";filename=\"" + file.getName() + "\"");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("ETag", eTag);
            response.addHeader("Cache-Control", "max-age=" + CACHE_DURATION_IN_SECOND);
            //response.addHeader("Cache-Control", "must-revalidate");//optional
            response.setDateHeader("Last-Modified", lastModified);
            response.setDateHeader("Expires", expires);

            /* Send requested file (part(s)) to client */

            // Open streams.
            input = new RandomAccessFile(file.getActualFile(), "r");
            output = response.getOutputStream();

            if (ranges.isEmpty() || ranges.get(0) == fullRange) {
                // Return full file.
                response.setContentType(contentType);

                if (content) {
                    if (acceptsGzip) {
                        // The browser accepts GZIP, so GZIP the content.
                        response.setHeader("Content-Encoding", "gzip");
                        output = new GZIPOutputStream(output, BUFFER_SIZE);
                    } else {
                        // Content length is not directly predictable in case of GZIP.
                        // So only add it if there is no means of GZIP, else browser will hang.
                        response.setHeader("Content-Length", String.valueOf(fullRange.length));
                    }

                    // Copy full range.
                    copyFileTo(input, output, fullRange.start, fullRange.length);
                }
            } else if (ranges.size() == 1) {
                // Return single part of file.
                Range r = ranges.get(0);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Copy single part range.
                    copyFileTo(input, output, r.start, r.length);
                }
            } else {
                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Cast back to ServletOutputStream to get the easy println methods.
                    ServletOutputStream sos = (ServletOutputStream) output;

                    // Copy multi part range.
                    for (Range r : ranges) {
                        // Add multipart boundary and header fields for every range.
                        sos.println();
                        sos.println("--" + MULTIPART_BOUNDARY);
                        sos.println("Content-Type: " + contentType);
                        sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                        // Copy single part range of multi part range.
                        copyFileTo(input, output, r.start, r.length);
                    }

                    // End with multipart boundary.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY + "--");
                }
            }
        } catch (IOException ignored) {
            // Usually thrown when a client aborts connection during copying bytes to output stream
        } catch (IllegalStateException e) {
            log.error("File {} cannot be downloaded because it isn't present in the file system", fileId);
            response.setStatus(500);
        } catch (Exception e) {
            log.error("Error while downloading file {}", fileId, e);
            response.setStatus(500);
        } finally {
            close(input);
            close(output);
        }
    }

    /**
     * Close the given resource.
     *
     * @param resource The resource to be closed.
     */
    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignored) {
                // Generally only thrown when the client aborts a request
            }
        }
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param input The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    private static void copyFileTo(RandomAccessFile input, OutputStream output, long start, long length) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        if (input.length() == length) {
            // Write full range.
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
        } else {
            // Write partial range.
            input.seek(start);
            long toRead = length;

            while ((read = input.read(buffer)) > 0) {
                toRead -= read;
                if (toRead > 0) {
                    output.write(buffer, 0, read);
                } else {
                    output.write(buffer, 0, (int) toRead + read);
                    break;
                }
            }
        }
    }

    /**
     * Returns true if the given accept header accepts the given value.
     *
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*([,;])\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
            || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
            || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     *
     * @param matchHeader The match header.
     * @param toMatch The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     *
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return substring.isEmpty() ? -1 : Long.parseLong(substring);
    }

    /**
     * This class represents a byte range.
     */
    protected static class Range {
        final long start;
        final long end;
        final long length;
        final long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }
}

package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;
import de.l3s.util.bean.BeanHelper;

/**
 * Servlet for Streaming Files to the Clients Browser.
 */
@WebServlet(name = "DownloadServlet", urlPatterns = "/download/*", loadOnStartup = 2)
public class DownloadServlet extends HttpServlet {
    private static final long serialVersionUID = 7083477094183456614L;
    private static final Logger log = LogManager.getLogger(DownloadServlet.class);

    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    private static final Duration CACHE_DURATION = Duration.ofDays(365);
    private static final int BUFFER_SIZE = 8192; // 8KB

    private static final String URL_PATTERN = "/download/";

    @Inject
    private FileDao fileDao;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private UserBean userBean;

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
    protected void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content) throws IOException {
        try {
            String referrer = request.getHeader("referer");
            String requestURI = StringUtils.substringAfter(request.getRequestURI(), URL_PATTERN); // remove the servlet's urlPattern
            RequestData requestData = parseRequestURI(request, requestURI, referrer);

            // Check && retrieve file, if the file is missing will throw an error
            //File file = fileDao.findByIdOrElseThrow(requestData.fileId);
            // for local testing only, to check files that are not present locally:
            File file = fileDao.findById(requestData.fileId, true).orElseThrow(BeanAssert.NOT_FOUND);

            validatePermissions(request, file, requestData, referrer);

            sendFile(request, response, file, content);
        } catch (HttpException e) {
            // Happens when file is not found or request URL is invalid, for humans
            response.sendError(e.getStatus());
        } catch (Exception e) {
            log.fatal("Unexpected error in download servlet {}", request.getRequestURI(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected RequestData parseRequestURI(HttpServletRequest request, String requestURI, String referrer) {
        String[] partsURI = requestURI.split("/");

        try {
            if (partsURI.length == 2) {
                return new RequestData(0, Integer.parseInt(partsURI[0]), partsURI[1]);
            }

            if (partsURI.length == 3) {
                return new RequestData(Integer.parseInt(partsURI[0]), Integer.parseInt(partsURI[1]), partsURI[2]);
            }

            throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            // only log the error if the referrer is uni-hannover.de. Otherwise we have no chance to fix the link
            Level logLevel = StringUtils.contains(referrer, "uni-hannover.de") ? Level.ERROR : Level.WARN;
            log.log(logLevel, "Invalid download URL: {}; {}", requestURI, BeanHelper.getRequestSummary(request));
            throw new HttpException(HttpServletResponse.SC_BAD_REQUEST, "Download URL is invalid", e);
        }
    }

    protected void validatePermissions(HttpServletRequest request, File file, RequestData requestData, String referrer) {
        if (!file.getType().isResourceFile()) {
            return;
        }

        // Small thumbnail files which are shown during resource upload and thus are not connected to a resource yet.
        if (file.getType() == File.FileType.THUMBNAIL_SMALL) {
            return;
        }

        // Check && retrieve resource
        Optional<Resource> resource = resourceDao.findById(requestData.resourceId);

        // Files which are attached to a resource should have the resourceId in the URL but there exist many old links on the web that don't include the resourceId
        if (resource.isEmpty()) {
            log.debug("A resource file accessed without resourceId in the URL; {}", BeanHelper.getRequestSummary(request));

            // TODO: When implementing access control remember that some files have to be accessed by our converter and other services. See File.getAbsoluteUrl()
            return;
        }

        // Get user from session
        User user = userBean.getUser();

        //TODO block invalid requests. But for a while we will only log them
        if (!file.getEncodedName().equals(requestData.fileName)) {
            log.debug("A resource file accessed invalid file name; db name: {}; request name: {}; request: {}", file.getEncodedName(), requestData.fileName, BeanHelper.getRequestSummary(request));
        }

        if (!resource.get().getFiles().containsValue(file)) {
            log.debug("A resource file accessed with an invalid resource id; request: {}", BeanHelper.getRequestSummary(request));
        }
        //BeanAssert.validate(file.getEncodedName().equals(requestData.fileName)); // validate file name
        //BeanAssert.validate(resource.get().getFiles().containsValue(file)); // validate the file belongs to the resource

        boolean hasPermission = resource.get().canViewResource(user);
        if (!hasPermission) {
            BeanAssert.authorized(user); // resource is definitely not readable by anonymous users, hence make sure that the user is logged in
        }
        BeanAssert.hasPermission(hasPermission); // validate user has access to the resource

        // log downloading of the MAIN files
        if (file.getType() == File.FileType.MAIN) {
            if (null != user) {
                HttpSession session = request.getSession(true);
                logDao.insert(user, Action.downloading, resource.get().getGroupId(), resource.get().getId(), Integer.toString(file.getId()), session.getId());
            }
        }
    }

    protected void sendFile(HttpServletRequest request, HttpServletResponse response, File file, boolean content) throws IOException {
        try {
            long length = file.getLength();
            long lastModified = file.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
            String eTag = file.getName() + "_" + length + "_" + lastModified;
            long expires = System.currentTimeMillis() + CACHE_DURATION.toMillis();

            validateCacheHeaders(request, response, lastModified, expires, eTag);

            List<Range> ranges = parseRangeHeader(request, response, lastModified, length, eTag);

            /* Prepare and initialize response */
            String contentType = file.getMimeType();
            String disposition = "inline";

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            if (contentType.startsWith("text")) {
                // If content type is text, expand content type with the one and right character encoding.
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
            response.addHeader("Cache-Control", "max-age=" + CACHE_DURATION.toSeconds());
            response.addHeader("Cache-Control", "must-revalidate");
            response.setDateHeader("Last-Modified", lastModified);
            response.setDateHeader("Expires", expires);

            if (ranges.isEmpty()) {
                // Return full file.
                response.setContentType(contentType);
                response.setHeader("Content-Length", String.valueOf(length));
            } else if (ranges.size() == 1) {
                // Return single part of file.
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + ranges.get(0).start + "-" + ranges.get(0).end + "/" + ranges.get(0).total);
                response.setHeader("Content-Length", String.valueOf(ranges.get(0).length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
            } else {
                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
            }

            if (content) {
                sendContent(response, file, ranges, contentType);
            }
        } catch (ValidationException e) {
            // Usually happens when cache is valid or headers not parsable, not for humans
            response.setStatus(e.statusCode);
        }
    }

    private void sendContent(HttpServletResponse response, File file, List<Range> ranges, String contentType) throws IOException {
        try (RandomAccessFile input = new RandomAccessFile(file.getActualFile(), "r");
            ServletOutputStream output = response.getOutputStream()) {

            if (ranges.isEmpty()) {
                // Copy full range.
                copyFullRange(input, output);
            } else if (ranges.size() == 1) {
                // Copy single part range.
                copyPartRange(input, output, ranges.get(0).start, ranges.get(0).length);
            } else {
                // Copy multi part range.
                for (Range r : ranges) {
                    // Add multipart boundary and header fields for every range.
                    output.println();
                    output.println("--" + MULTIPART_BOUNDARY);
                    output.println("Content-Type: " + contentType);
                    output.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                    // Copy single part range of multi part range.
                    copyPartRange(input, output, r.start, r.length);
                }

                // End with multipart boundary.
                output.println();
                output.println("--" + MULTIPART_BOUNDARY + "--");
            }
        } catch (IOException ignored) {
            // Usually thrown when a client aborts connection, just ignore it
        } catch (IllegalStateException e) {
            // This happens when we can't find or read the file on the server
            log.error("File {} cannot be downloaded because it isn't present in the file system", file);
            response.sendError(422);
        }
    }

    private static void validateCacheHeaders(HttpServletRequest request, HttpServletResponse response, long lastModified, long expires, String eTag) throws ValidationException {
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            response.setHeader("ETag", eTag);
            response.setDateHeader("Expires", expires);
            throw new ValidationException(HttpServletResponse.SC_NOT_MODIFIED);
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
            response.setHeader("ETag", eTag);
            response.setDateHeader("Expires", expires);
            throw new ValidationException(HttpServletResponse.SC_NOT_MODIFIED);
        }

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            throw new ValidationException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            throw new ValidationException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }
    }

    private static List<Range> parseRangeHeader(HttpServletRequest request, HttpServletResponse response, long lastModified, long length, String eTag) throws ValidationException {
        // Prepare some variables. The full Range represents the complete file.
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {
            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                throw new ValidationException(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            }

            // If-Range header should either match ETag or be greater then LastModified. If not, then return full file.
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        return ranges;
                    }
                } catch (IllegalArgumentException ignore) {
                    return ranges;
                }
            }

            // If any valid If-Range header, then process each part of byte range.
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
                    throw new ValidationException(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                }

                // Add range.
                ranges.add(new Range(start, end, length));
            }
        }

        return ranges;
    }

    /**
     * Copy the full range of the given input to the given output.
     *
     * @param input The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @throws IOException If something fails at I/O level.
     */
    private static void copyFullRange(RandomAccessFile input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        while ((read = input.read(buffer)) > 0) {
            output.write(buffer, 0, read);
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
    private static void copyPartRange(RandomAccessFile input, OutputStream output, long start, long length) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        if (input.length() == length) {
            // Write full range.
            copyFullRange(input, output);
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
    private static class Range {
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
        Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }

    protected static class RequestData {
        final int resourceId;
        final int fileId;
        final String fileName;

        RequestData(int resourceId, int fileId, String fileName) {
            this.resourceId = resourceId;
            this.fileId = fileId;
            this.fileName = fileName;

            if (fileId == 0 || StringUtils.isEmpty(fileName)) {
                throw new IllegalArgumentException();
            }
        }
    }

    private static class ValidationException extends Exception {
        private static final long serialVersionUID = -3869278798616542070L;

        final int statusCode;

        ValidationException(int statusCode) {
            this.statusCode = statusCode;
        }
    }
}

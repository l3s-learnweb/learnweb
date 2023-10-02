package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.StreamingUpdateRequest;
import org.apache.solr.common.util.NamedList;

import de.l3s.util.MimeTypes;
import de.l3s.util.StringHelper;

public class FileInspector {
    private static final Logger log = LogManager.getLogger(FileInspector.class);
    private static final Pattern INVALID_CHARS_FILENAME = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final Pattern INVALID_CHARS_TEXT = Pattern.compile("(?m)^[ \t]*\r?\n");

    private final SolrClient solrClient;

    public FileInspector(final SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public FileInfo inspect(InputStream inputStream, String fileName) {
        FileInfo fileInfo = inspectFileName(fileName);

        if (fileInfo.getMimeType().startsWith("video/")) { // solr/tika doesn't work good with videos
            return fileInfo;
        }

        try (inputStream) {
            NamedList<Object> result = requestSolrExtract(fileInfo.getMimeType(), inputStream);
            saveSolrMetadata(result, fileInfo);
        } catch (SolrServerException | IOException e) {
            log.error("FileInspector: Can't extract Text from File", e);
        }

        return fileInfo;
    }

    private NamedList<Object> requestSolrExtract(String contentType, InputStream stream) throws IOException, SolrServerException {
        Http2SolrClient server = solrClient.getHttpSolrClient();

        StreamingUpdateRequest up = new StreamingUpdateRequest("/update/extract", new RequestWriter.ContentWriter() {
            @Override
            public void write(final OutputStream os) throws IOException {
                stream.transferTo(os);
            }

            @Override
            public String getContentType() {
                return contentType;
            }
        });

        up.setParam("extractOnly", "true");
        up.setParam("extractFormat", "text"); // default : xml(with xml tags)
        return server.request(up);
    }

    public static FileInfo inspectFileName(String fileName) {
        fileName = StringUtils.defaultString(fileName); // if null, use empty string

        FileInfo fileInfo = new FileInfo();
        fileInfo.fileName = INVALID_CHARS_FILENAME.matcher(fileName).replaceAll("_");

        int indexOf = fileName.lastIndexOf('.');
        if (indexOf != -1) {
            fileInfo.mimeType = MimeTypes.getMimeType(fileName.substring(indexOf + 1));
            fileInfo.title = fileName.substring(0, indexOf);
        } else {
            fileInfo.mimeType = "application/octet-stream";
            fileInfo.title = fileName;
        }

        return fileInfo;
    }

    private static void saveSolrMetadata(NamedList<Object> result, FileInfo fileInfo) {
        NamedList<?> metadata = (NamedList<?>) result.get("null_metadata");

        if (metadata.indexOf("title", 0) > -1) {
            String title = metadata2String(metadata.get("title"));
            if (title != null) {
                fileInfo.title = title;
            }
        }
        if (metadata.indexOf("description", 0) > -1) {
            String description = metadata2String(metadata.get("description"));
            if (description != null) {
                fileInfo.description = description;
            }
        }
        if (metadata.indexOf("Author", 0) > -1) {
            String author = metadata2String(metadata.get("Author"));
            if (author != null) {
                fileInfo.author = author;
            }
        }
        if (metadata.indexOf("Content-Type", 0) > -1) {
            String mimeType = metadata2String(metadata.get("Content-Type"));
            if (mimeType != null) {
                fileInfo.mimeType = mimeType.split(";")[0];
            }
        }

        fileInfo.textContent = INVALID_CHARS_TEXT.matcher(result.getVal(1).toString().trim()).replaceAll("");
    }

    @SuppressWarnings("unchecked")
    private static String metadata2String(Object obj) {
        List<String> descriptions = (List<String>) obj;
        if (!descriptions.isEmpty() && !descriptions.get(0).isEmpty()) {
            return descriptions.get(0);
        }

        return null;
    }

    public static class FileInfo {
        private String fileName;
        private String title;
        private String mimeType;

        private String textContent;
        private String author;
        private String description;

        public String getFileName() {
            return fileName;
        }

        public String getTitle() {
            return title == null ? fileName : title;
        }

        public String getMimeType() {
            if (null == mimeType) {
                return null;
            }
            return mimeType.toLowerCase();
        }

        public String getTextContent() {
            return textContent;
        }

        public String getAuthor() {
            return author;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return "FileInfo [" +
                "mimeType=" + mimeType + ", " +
                "title=" + title + ", " +
                "fileName=" + fileName + ", " +
                "author=" + author + ", " +
                "textContent:" + System.lineSeparator() + StringHelper.shortnString(textContent, 80)
                + "]";
        }
    }
}

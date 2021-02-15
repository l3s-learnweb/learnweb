package de.l3s.learnweb.resource.search.solrClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;

import de.l3s.util.MimeTypes;
import de.l3s.util.Misc;
import de.l3s.util.StringHelper;
import de.l3s.util.UrlHelper;

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
            NamedList<Object> result = requestSolrExtract(new MyContentStream(fileInfo.getFileName(), fileInfo.getMimeType(), inputStream));
            saveSolrMetadata(result, fileInfo);
        } catch (SolrServerException | IOException e) {
            log.error("FileInspector: Can't extract Text from File; {}", Misc.getSystemDescription(), e);
        }

        return fileInfo;
    }

    private NamedList<Object> requestSolrExtract(MyContentStream contentStream) throws IOException, SolrServerException {
        HttpSolrClient server = solrClient.getHttpSolrClient();

        ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
        up.addContentStream(contentStream);
        up.setParam("extractOnly", "true");
        up.setParam("extractFormat", "text"); // default : xml(with xml tags)

        return server.request(up);
    }

    public static FileInfo inspectFileName(String fileName) {
        fileName = StringUtils.defaultString(fileName); // if null, use empty string

        FileInfo fileInfo = new FileInfo();
        fileInfo.fileName = INVALID_CHARS_FILENAME.matcher(fileName).replaceAll("_");

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            fileInfo.mimeType = MimeTypes.getMimeType(fileName.substring(i + 1));
            fileInfo.title = fileName.substring(0, i);
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

    public static InputStream openStream(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("User-Agent", UrlHelper.USER_AGENT);
        return con.getInputStream();
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

    private static class MyContentStream implements ContentStream {
        private final String name;
        private final String contentType;
        private final InputStream stream;

        MyContentStream(String name, String contentType, InputStream stream) {
            this.name = name;
            this.contentType = contentType;
            this.stream = stream;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Reader getReader() {
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        @Override
        public Long getSize() {
            return null;
        }

        @Override
        public String getSourceInfo() {
            // I don't know what they expect it seems to be unnecessary
            return null;
        }

        @Override
        public InputStream getStream() {
            return stream;
        }
    }
}

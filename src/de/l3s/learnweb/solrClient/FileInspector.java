package de.l3s.learnweb.solrClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.MimeTypes;
import de.l3s.util.StringHelper;

public class FileInspector
{
    private final static Logger log = Logger.getLogger(FileInspector.class);
    private SolrClient solrClient;

    public FileInspector(Learnweb learnweb)
    {
        solrClient = learnweb.getSolrClient();
    }

    @SuppressWarnings("unchecked")
    public FileInfo inspect(InputStream inputStream, String fileName)
    {
        FileInfo info = new FileInfo();
        info.fileName = fileName;

        int i = fileName.lastIndexOf('.');
        if(i > 0)
        {
            String extension = fileName.substring(i + 1);
            info.mimeType = MimeTypes.getMimeType(extension);
            info.title = fileName.substring(0, i);
        }
        else
        {
            info.mimeType = "application/octet-stream";
            info.title = fileName;
        }

        if(info.mimeType.startsWith("video/")) // solr/tika doesn't work good with videos
            return info;

        HttpSolrClient server = solrClient.getSolrServer();

        ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
        up.addContentStream(new MyContentStream(info.fileName, info.mimeType, inputStream));
        up.setParam("extractOnly", "true");
        up.setParam("extractFormat", "text"); // default : xml(with xml tags)

        NamedList<Object> result;
        try
        {
            result = server.request(up);
        }
        catch(Exception e)
        {
            log.error("FileInspector: Can't extract Text from File", e);

            return info;
        }

        NamedList<Object> metadata = (NamedList<Object>) result.get("null_metadata");

        /*
        if(metadata.indexOf("title", 0) > -1)
        {
            List<String> titles = (List<String>) metadata.get("title");
            if(titles.size() > 0 && titles.get(0).length() > 0)
                info.title = titles.get(0);
        }
        if(metadata.indexOf("description", 0) > -1)
        {
            List<String> descriptions = (List<String>) metadata.get("description");
            if(descriptions.size() > 0 && descriptions.get(0).length() > 0)
                info.description = descriptions.get(0);
        }
        if(metadata.indexOf("Author", 0) > -1)
        {
            List<String> authors = (List<String>) metadata.get("Author", 0);
            info.author = authors.get(0);
        }
        if(metadata.indexOf("Content-Type", 0) > -1)
        {
            List<String> types = (List<String>) metadata.get("Content-Type");
            if(types.size() > 0 && types.get(0).length() > 0)
                info.mimeType = types.get(0).split(";")[0];
        }*/
        if(metadata.indexOf("title", 0) > -1)
        {
            String title = metadata2String(metadata.get("title"));
            if(title != null)
                info.title = title;
        }
        if(metadata.indexOf("description", 0) > -1)
        {
            String description = metadata2String(metadata.get("description"));
            if(description != null)
                info.description = description;
        }
        if(metadata.indexOf("Author", 0) > -1)
        {
            String author = metadata2String(metadata.get("Author"));
            if(author != null)
                info.description = author;
        }
        if(metadata.indexOf("Content-Type", 0) > -1)
        {
            String mimeType = metadata2String(metadata.get("Content-Type"));
            if(mimeType != null)
                info.mimeType = mimeType.split(";")[0];
        }
        info.textContent = result.getVal(1).toString().trim().replaceAll("(?m)^[ \t]*\r?\n", "");

        return info;
    }

    @SuppressWarnings("unchecked")
    private static String metadata2String(Object obj)
    {
        List<String> descriptions = (List<String>) obj;
        if(descriptions.size() > 0 && descriptions.get(0).length() > 0)
            return descriptions.get(0);

        return null;
    }

    public static InputStream openStream(String url) throws IOException
    {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:32.0) Gecko/20100101 Firefox/32.0");

        return con.getInputStream();
    }

    // tests
    public static void main(String[] args) throws Exception
    {
        /*
        Learnweb learnweb = Learnweb.getInstance();
        List<Resource> resources = learnweb.getResourceManager().getResources("SELECT * FROM `lw_resource` WHERE `source` LIKE 'flickr' AND `max_image_url` LIKE '%z.jpg' ORDER BY `resource_id` ASC ", null); // loads all resources (very slow)
        
        for(Resource resource : resources)
        {
            getBestImage(resource);
        
        }
        */
    }

    public class FileInfo
    {
        private String mimeType;
        private String title;
        private String textContent;
        private String fileName;
        private String author;
        private String description;

        public String getFileName()
        {
            return fileName;
        }

        public String getMimeType()
        {
            if(null == mimeType)
                return null;
            return mimeType.toLowerCase();
        }

        public String getTitle()
        {
            return title == null ? fileName : title;
        }

        public String getTextContent()
        {
            return textContent;
        }

        public String getAuthor()
        {
            return author;
        }

        public String getDescription()
        {
            return description;
        }

        @Override
        public String toString()
        {
            return "FileInfo [mimeType=" + mimeType + ", title=" + title + ", fileName=" + fileName + ", author=" + author + ", textContent:\n" + StringHelper.shortnString(textContent, 80) + "]";
        }
    }

    private class MyContentStream implements ContentStream
    {
        private String fileName;
        private String mimeType;
        private InputStream inputStream;

        public MyContentStream(String fileName, String mimeType, InputStream inputStream)
        {
            super();
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.inputStream = inputStream;
        }

        @Override
        public String getContentType()
        {
            return mimeType;
        }

        @Override
        public String getName()
        {
            return fileName;
        }

        @Override
        public Reader getReader() throws IOException
        {
            return new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        }

        @Override
        public Long getSize()
        {
            return null;
        }

        @Override
        public String getSourceInfo()
        {
            // I don't know what they expect it seems to be unnecessary
            return null;
        }

        @Override
        public InputStream getStream() throws IOException
        {
            return inputStream;
        }

    }
}

package de.l3s.learnweb.resource.office;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.ResourceType;

public class FileUtility
{
    private static final Logger log = LogManager.getLogger(FileUtility.class);
    
    private static final String OFFICE_FILES_FOLDER = "/de/l3s/learnweb/office/documents/";

    private static final String TEXT = "text";
    private static final String PRESENTATION = "presentation";
    private static final String SPREADSHEET = "spreadsheet";

    //private static final List<String> EXT_DOCUMENT = Arrays.asList("docx", "doc", "odt", "rtf", "txt", "html", "htm", "mht", "pdf", "djvu", "fb2", "epub", "xps");
    private static final List<String> EXT_SPREADSHEET = Arrays.asList("xls", "xlsx", "ods", "csv");
    private static final List<String> EXT_PRESENTATION = Arrays.asList("pps", "ppsx", "ppt", "pptx", "odp");
    private static final List<String> EXT_DOCUMENT_CONVERT = Arrays.asList("mht", "docm", "dot", "dotm", "dotx", "fodt");
    private static final List<String> EXT_SPREADSHEET_CONVERT = Arrays.asList("fods", "xlsm", "xlt", "xltm", "xltx");
    private static final List<String> EXT_PRESENTATION_CONVERT = Arrays.asList("fodp", "pot", "potm", "potx", "pps", "ppsx", "pptm", "ppsm");

    private static final String SAMPLE_PPTX = "sample.pptx";
    private static final String SAMPLE_XLSX = "sample.xlsx";
    private static final String SAMPLE_DOCX = "sample.docx";

    public static boolean canBeViewed(String fileExt)
    {
        return !(EXT_DOCUMENT_CONVERT.contains(fileExt) || EXT_SPREADSHEET_CONVERT.contains(fileExt) || EXT_PRESENTATION_CONVERT.contains(fileExt));
    }

    public static String getFileType(String fileName)
    {
        String ext = getFileExtension(fileName);
        if(EXT_SPREADSHEET.contains(ext))
            return SPREADSHEET;
        if(EXT_PRESENTATION.contains(ext))
            return PRESENTATION;
        return TEXT;
    }

    public static String getInfoForKey(File file)
    {
        String infoForKey = null;
        if(file != null && file.getLastModified() != null)
        {
            infoForKey = Long.toString(file.getLastModified().getTime()) + file.getId();
        }
        return infoForKey;
    }

    public static String generateRevisionId(File file)
    {
        String expectedKey = getInfoForKey(file);
        if(expectedKey.length() > 20)
            expectedKey = Integer.toString(expectedKey.hashCode());

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "");

        return key.substring(0, Math.min(key.length(), 20));
    }

    public static String getFileExtension(String fileName)
    {
        if(fileName != null)
        {
            int lastIndex = fileName.lastIndexOf('.');
            if(lastIndex <= 0)
                return null;
            String fileExt = fileName.substring(lastIndex + 1);
            return fileExt.toLowerCase();
        }
        return null;
    }

    public static String getFileName(String url)
    {
        if(StringUtils.isEmpty(url))
            return "unknownFileName";

        try
        {
            URI uri = new URI(url);

            String path = uri.getPath();

            if(StringUtils.isEmpty(path) || path.equals("/"))
            {
                path = uri.getHost();
                if(path.startsWith("www."))
                    path = path.substring(4);
            }

            return Paths.get(path).getFileName().toString();
        }
        catch(Throwable e)
        {
            log.error("Can't get filename from URL: " + url, e);
        }

        return "unknownFileName";
    }

    public static java.io.File getSampleOfficeFile(ResourceType resourceType) throws URISyntaxException
    {
        String sampleFileName = getSampleFileName(resourceType);
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(OFFICE_FILES_FOLDER + sampleFileName);
        return new java.io.File(resourceUrl.toURI());
    }

    public static String getInternalExtension(ResourceType fileType)
    {
        switch(fileType)
        {
        case spreadsheet:
            return ".xlsx";
        case presentation:
            return ".pptx";
        default:
            return ".docx";
        }
    }

    public static String getSampleFileName(ResourceType fileType)
    {
        switch(fileType)
        {
        case document:
            return SAMPLE_DOCX;
        case spreadsheet:
            return SAMPLE_XLSX;
        case presentation:
            return SAMPLE_PPTX;
        default:
            return null;
        }
    }
}

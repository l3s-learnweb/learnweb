package de.l3s.office;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.Resource.ResourceType;

public class FileUtility
{
    private static final String TEXT = "text";

    private static final String PRESENTATION = "presentation";

    private static final String SPREADSHEET = "spreadsheet";

    public static final List<String> EXT_DOCUMENT = Arrays.asList(".docx", ".doc", ".odt", ".rtf", ".txt", ".html", ".htm", ".mht", ".pdf", ".djvu", ".fb2", ".epub", ".xps");

    public static final List<String> EXT_SPREADSHEET = Arrays.asList(".xls", ".xlsx", ".ods", ".csv");

    public static final List<String> EXT_PRESENTATION = Arrays.asList(".pps", ".ppsx", ".ppt", ".pptx", ".odp");

    public static final List<String> EXT_DOCUMENT_CONVERT = Arrays.asList(".mht", ".docm", ".dot", ".dotm", ".dotx", ".fodt");

    public static final List<String> EXT_SPREADSHEET_CONVERT = Arrays.asList(".fods", ".xlsm", ".xlt", ".xltm", ".xltx");

    public static final List<String> EXT_PRESENTATION_CONVERT = Arrays.asList(".fodp", ".pot", ".potm", ".potx", ".pps", ".ppsx", ".pptm", ".ppsm");

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
            int lastIndex = fileName.lastIndexOf(".");
            if(lastIndex <= 0)
                return null;
            String fileExt = fileName.substring(lastIndex);
            return fileExt.toLowerCase();
        }
        return null;
    }

    public static void main(String[] arg)
    {
        System.out.println(getFileName("https://www.google.de"));
        System.out.println("--------");
        System.out.println(getFileName("http://www.google.de/"));
        System.out.println("--------");

        System.out.println(getFileName("http://127.0.0.1"));
        System.out.println("--------");

        System.out.println(getFileName("https://www.google.de/dfgdfgdfg"));
        System.out.println("--------");

        System.out.println(getFileName("https://www.google.de/dfgdfgdfg/"));
        System.out.println("--------");

        System.out.println(getFileName("https://www.google.de/dfgdfgdfg/asd.xmk"));
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
            System.out.println("path: " + path);

            return Paths.get(path).getFileName().toString();
        }
        catch(Throwable e)
        {
            Logger.getLogger(FileUtility.class).error("Can't get filename from URL: " + url, e);
        }

        return "unknownFileName";
    }

    public static String getInternalExtension(ResourceType fileType)
    {
        if(fileType.equals(ResourceType.document))
            return ".docx";

        if(fileType.equals(ResourceType.spreadsheet))
            return ".xlsx";

        if(fileType.equals(ResourceType.presentation))
            return ".pptx";

        return ".docx";
    }

    public static String getRightSampleName(ResourceType fileType)
    {
        if(fileType != null)
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
                break;
            }
        }
        return null;
    }
}

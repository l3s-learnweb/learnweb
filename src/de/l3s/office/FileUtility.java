package de.l3s.office;

import java.util.Arrays;
import java.util.List;

import de.l3s.learnweb.File;
import de.l3s.learnweb.Resource.ResourceType;

public class FileUtility
{

    private static final String TEXT = "text";

    private static final String PRESENTATION = "presentation";

    private static final String SPREADSHEET = "spreadsheet";

    public static final List<String> EXT_DOCUMENT = Arrays.asList(".docx", ".doc", ".odt", ".rtf", ".txt", ".html", ".htm", ".mht", ".pdf", ".djvu", ".fb2", ".epub", ".xps");

    public static final List<String> EXT_SPREADSHEET = Arrays.asList(".xls", ".xlsx", ".ods", ".csv");

    public static final List<String> EXT_PRESENTATION = Arrays.asList(".pps", ".ppsx", ".ppt", ".pptx", ".odp");

    private static final String SAMPLE_PPTX = "sample.pptx";

    private static final String SAMPLE_XLSX = "sample.xlsx";

    private static final String SAMPLE_DOCX = "sample.docx";

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
            String fileExt = fileName.substring(fileName.lastIndexOf("."));
            return fileExt.toLowerCase();
        }
        return null;
    }

    public static String getFileName(String url)
    {
        if(url == null)
            return null;

        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
        return fileName;
    }

    public static String getInternalExtension(String fileType)
    {
        if(fileType.equals(ResourceType.document.toString()))
            return ".docx";

        if(fileType.equals(ResourceType.spreadsheet.toString()))
            return ".xlsx";

        if(fileType.equals(ResourceType.presentation.toString()))
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

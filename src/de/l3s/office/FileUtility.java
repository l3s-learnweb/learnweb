
package de.l3s.office;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.File;

public class FileUtility
{

    public static final List<String> EXT_DOCUMENT = Arrays.asList(".docx", ".doc", ".odt", ".rtf", ".txt", ".html", ".htm", ".mht", ".pdf", ".djvu", ".fb2", ".epub", ".xps");

    public static final List<String> EXT_SPREADSHEET = Arrays.asList(".xls", ".xlsx", ".ods", ".csv");

    public static final List<String> EXT_PRESENTATION = Arrays.asList(".pps", ".ppsx", ".ppt", ".pptx", ".odp");

    public static String getFileType(String fileName)
    {
        String ext = getFileExtension(fileName);

        if(EXT_SPREADSHEET.contains(ext))
            return FileType.SPREADSHEET;

        if(EXT_PRESENTATION.contains(ext))
            return FileType.PRESENTATION;

        return FileType.TEXT;
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

    public static String generateRevisionId(String expectedKey)
    {
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

    public static String getFileNameWithoutExtension(String url)
    {
        String fileName = getFileName(url);
        if(fileName == null)
            return null;
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        return fileNameWithoutExt;
    }

    public static String getFileName(String url)
    {
        if(url == null)
            return null;

        //for external file url
        String tempstorage = ConfigManager.GetProperty("files.docservice.url.tempstorage");
        if(!tempstorage.isEmpty() && url.startsWith(tempstorage))
        {
            Map<String, String> params = GetUrlParams(url);
            return params == null ? null : params.get("filename");
        }

        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
        return fileName;
    }

    public static Map<String, String> GetUrlParams(String url)
    {
        try
        {
            String query = new URL(url).getQuery();
            String[] params = query.split("&");
            Map<String, String> map = new HashMap<>();
            for(String param : params)
            {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
            return map;
        }
        catch(Exception ex)
        {
            return null;
        }
    }
}

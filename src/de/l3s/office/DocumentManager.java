
package de.l3s.office;

import java.io.File;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class DocumentManager
{
    private static HttpServletRequest request;
    private static HttpServletResponse response;

    private final static Logger logger = Logger.getLogger(DocumentManager.class);

    private static final String LOCAL_NETWORK_IP = ConfigManager.GetProperty("local.network.ip");

    public static void Init(HttpServletRequest req, HttpServletResponse resp)
    {
        request = req;
        response = resp;
    }

    public static long GetMaxFileSize()
    {
        long size;

        try
        {
            size = Long.parseLong(ConfigManager.GetProperty("filesize-max"));
        }
        catch(Exception ex)
        {
            size = 0;
        }

        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public static List<String> GetFileExts()
    {
        List<String> res = new ArrayList<>();

        res.addAll(GetViewedExts());
        res.addAll(GetEditedExts());

        return res;
    }

    public static String GetCorrectName(String fileName)
    {
        String baseName = FileUtility.getFileNameWithoutExtension(fileName);
        String ext = FileUtility.getFileExtension(fileName);
        String name = baseName + ext;

        File file = new File(StoragePath(name, null));

        for(int i = 1; file.exists(); i++)
        {
            name = baseName + " (" + i + ")" + ext;
            file = new File(StoragePath(name, null));
        }

        return name;
    }

    public static List<String> GetViewedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.viewed-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static List<String> GetEditedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.edited-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static String CurUserHostAddress(String userAddress)
    {
        if(userAddress == null)
        {
            try
            {
                userAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch(Exception ex)
            {
                userAddress = "";
            }
        }

        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    public static String StoragePath(String fileName, String userAddress)
    {
        String serverPath = request.getSession().getServletContext().getRealPath("");
        String storagePath = "app_data";
        String hostAddress = CurUserHostAddress(userAddress);

        String directory = serverPath + "\\" + storagePath + "\\";
        File file = new File(directory);

        if(!file.exists())
        {
            file.mkdir();
        }

        directory = directory + hostAddress + "\\";
        file = new File(directory);

        if(!file.exists())
        {
            file.mkdir();
        }
        logger.info(directory + fileName);
        return directory + fileName;
    }

    public static String GetFileUri(String fileName) throws Exception
    {
        try
        {
            String serverPath = request.getScheme() + "://" + LOCAL_NETWORK_IP + ":" + request.getServerPort() + request.getContextPath();
            String storagePath = ConfigManager.GetProperty("storage-folder");
            String hostAddress = CurUserHostAddress(null);

            String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + URLEncoder.encode(fileName);

            return filePath;
        }
        catch(Exception ex)
        {
            throw ex;
        }
    }

    public static String GetServerUrl()
    {
        return request.getScheme() + "://" + LOCAL_NETWORK_IP + ":" + request.getServerPort() + request.getContextPath();
    }

    public static String GetInternalExtension(String fileType)
    {
        if(fileType.equals(FileType.TEXT))
            return ".docx";

        if(fileType.equals(FileType.SPREADSHEET))
            return ".xlsx";

        if(fileType.equals(FileType.PRESENTATION))
            return ".pptx";

        return ".docx";
    }
}

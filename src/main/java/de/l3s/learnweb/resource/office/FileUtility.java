package de.l3s.learnweb.resource.office;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.ResourceType;

public final class FileUtility {
    private static final Logger log = LogManager.getLogger(FileUtility.class);

    private static final String OFFICE_FILES_FOLDER = "/de/l3s/learnweb/office/documents/";

    private static final String DOCUMENT = "word";
    private static final String PRESENTATION = "cell";
    private static final String SPREADSHEET = "slide";

    private static final List<String> EXT_DOCUMENT = Arrays.asList("djvu", "doc", "docm", "docx", "docxf", "dot", "dotm", "dotx", "epub", "fb2", "fodt", "htm", "html", "mht", "mhtml", "odt", "oform", "ott", "oxps", "pdf", "rtf", "stw", "sxw", "txt", "wps", "wpt", "xml", "xps");
    private static final List<String> EXT_SPREADSHEET = Arrays.asList("csv", "et", "ett", "fods", "ods", "ots", "sxc", "xls", "xlsb", "xlsm", "xlsx", "xlt", "xltm", "xltx", "xml");
    private static final List<String> EXT_PRESENTATION = Arrays.asList("dps", "dpt", "fodp", "odp", "otp", "pot", "potm", "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", "sxi");

    private static final String SAMPLE_PPTX = "sample.pptx";
    private static final String SAMPLE_XLSX = "sample.xlsx";
    private static final String SAMPLE_DOCX = "sample.docx";

    public static boolean isSupportedFileType(String fileExt) {
        return EXT_DOCUMENT.contains(fileExt) || EXT_SPREADSHEET.contains(fileExt) || EXT_PRESENTATION.contains(fileExt);
    }

    public static String getFileType(String fileExt) {
        if (EXT_SPREADSHEET.contains(fileExt)) {
            return SPREADSHEET;
        }
        if (EXT_PRESENTATION.contains(fileExt)) {
            return PRESENTATION;
        }
        return DOCUMENT;
    }

    public static String getInfoForKey(File file) {
        String infoForKey = null;
        if (file != null && file.getCreatedAt() != null) {
            infoForKey = Long.toString(file.getCreatedAt().toEpochSecond(ZoneOffset.UTC)) + file.getId();
        }
        return infoForKey;
    }

    public static String generateRevisionId(File file) {
        String expectedKey = getInfoForKey(file);
        if (expectedKey.length() > 20) {
            expectedKey = Integer.toString(expectedKey.hashCode());
        }

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "");

        return key.substring(0, Math.min(key.length(), 20));
    }

    public static String getFileExtension(String fileName) {
        if (fileName != null) {
            int lastIndex = fileName.lastIndexOf('.');
            if (lastIndex == -1) {
                return null;
            }
            String fileExt = fileName.substring(lastIndex + 1);
            return fileExt.toLowerCase();
        }
        return null;
    }

    public static String getFileName(String url) {
        if (StringUtils.isNotEmpty(url)) {
            try {
                URI uri = new URI(url);

                String path = uri.getPath();

                if (StringUtils.isEmpty(path) || path.equals("/")) {
                    path = uri.getHost();
                    if (path.startsWith("www.")) {
                        path = path.substring(4);
                    }
                }

                Path fileName = Paths.get(path).getFileName();
                if (fileName != null) {
                    return fileName.toString();
                }
            } catch (Throwable e) {
                log.error("Can't get filename from URL: {}", url, e);
            }
        }

        return "unknownFileName";
    }

    public static java.io.File getSampleOfficeFile(ResourceType resourceType) throws URISyntaxException {
        String sampleFileName = getSampleFileName(resourceType);
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(OFFICE_FILES_FOLDER + sampleFileName);
        assert resourceUrl != null;
        return new java.io.File(resourceUrl.toURI());
    }

    public static String getInternalExtension(ResourceType fileType) {
        return switch (fileType) {
            case spreadsheet -> ".xlsx";
            case presentation -> ".pptx";
            default -> ".docx";
        };
    }

    public static String getSampleFileName(ResourceType fileType) {
        return switch (fileType) {
            case document -> SAMPLE_DOCX;
            case spreadsheet -> SAMPLE_XLSX;
            case presentation -> SAMPLE_PPTX;
            default -> null;
        };
    }
}

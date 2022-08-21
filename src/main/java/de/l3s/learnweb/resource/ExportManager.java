package de.l3s.learnweb.resource;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.hp.gagawa.java.Document;
import com.hp.gagawa.java.DocumentType;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Meta;
import com.hp.gagawa.java.elements.Style;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Tbody;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Thead;
import com.hp.gagawa.java.elements.Tr;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File.FileType;
import de.l3s.learnweb.user.User;

public final class ExportManager {
    private static final Logger log = LogManager.getLogger(ExportManager.class);
    private static final String EXPORT_FILE_PREFIX = "learnweb-";
    private static final String EXPORT_FILE_EXT = ".zip";
    private static final String EXPORT_CONTENT_TYPE = "application/zip";

    public static StreamedContent streamResources(User user) throws IOException {
        return streamResources(packResources(null, user.getResources()), user.getUsername().toLowerCase());
    }

    public static StreamedContent streamResources(final Group group) throws IOException {
        return streamResources(packResources(group.getTitle(), group.getResources()), group.getTitle());
    }

    private static StreamedContent streamResources(final Map<String, InputStream> resourcesToPack, final String fileSuffix) {
        return DefaultStreamedContent.builder()
            .name(getFileName(fileSuffix))
            .contentType(EXPORT_CONTENT_TYPE)
            .stream(() -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                createArchive(resourcesToPack, baos);
                return new ByteArrayInputStream(baos.toByteArray());
            })
            .build();
    }

    public static StreamedContent streamSelectedResources(Group group, List<Folder> folders, List<Resource> list) throws IOException {
        if (!folders.isEmpty()) {
            list.addAll(getResources(folders));
        }
        return streamResources(packResources(group.getTitle(), list), group.getTitle());
    }

    private static List<Resource> getResources(List<Folder> folders) {
        List<Resource> res = new ArrayList<>();
        for (Folder folder : folders) {
            if (!folder.getSubFolders().isEmpty()) {
                res.addAll(getResources(folder.getSubFolders()));
            }
            if (!folder.getResources().isEmpty()) {
                res.addAll(folder.getResources());
            }
        }
        return res;
    }

    /**
     * TODO: should be improved to use Group/Folder name as file name
     */
    private static String getFileName(final String fileSuffix) {
        return EXPORT_FILE_PREFIX + fileSuffix + EXPORT_FILE_EXT;
    }

    private static void createArchive(final Map<String, InputStream> resourcesToPack, final OutputStream os) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(os))) {
            zipOutputStream.setLevel(Deflater.NO_COMPRESSION);

            for (Map.Entry<String, InputStream> entry : resourcesToPack.entrySet()) {
                ZipEntry fileEntry = new ZipEntry(entry.getKey());
                zipOutputStream.putNextEntry(fileEntry);
                try {
                    InputStream inputStream = entry.getValue();
                    inputStream.transferTo(zipOutputStream);
                } catch (IOException e) {
                    log.error("Can't get content of the file {}", entry, e);
                }
                zipOutputStream.closeEntry();
            }
        } catch (IOException e) {
            log.error("Unable to create an archive", e);
        }
    }

    private static Map<String, InputStream> packResources(final String groupTitle, final List<Resource> resources) {
        List<Resource> learnwebResources = new ArrayList<>();
        List<Resource> webResources = new ArrayList<>();

        for (Resource resource : resources) {
            if (resource.isWebResource()) {
                webResources.add(resource);
                if (resource.getFile(FileType.THUMBNAIL_LARGE) != null) {
                    learnwebResources.add(resource);
                }
            } else {
                learnwebResources.add(resource);
            }
        }

        Map<String, InputStream> filesToPack = new HashMap<>();
        if (!webResources.isEmpty()) {
            filesToPack.put("web_resources.html", getWebResourcesAsHtml(webResources));
        }
        if (!learnwebResources.isEmpty()) {
            filesToPack.putAll(getLearnwebResources(learnwebResources, groupTitle));
        }
        return filesToPack;
    }

    /**
     * TODO: should try other files if MAIN file doesn't exists
     */
    private static Map<String, InputStream> getLearnwebResources(List<Resource> resources, String groupRootFolder) {
        Map<String, InputStream> files = new HashMap<>();

        for (Resource resource : resources) {
            Folder folder = resource.getFolder();
            String folderName = createFolderPath(folder, groupRootFolder);

            File webThumbnail = resource.getFile(FileType.THUMBNAIL_LARGE);
            if (resource.isWebResource() && webThumbnail != null) {
                files.put(folderName + webThumbnail.getName(), webThumbnail.getInputStream());
            }

            File mainFile = resource.getFile(FileType.MAIN);
            if (mainFile != null) {
                files.put(folderName + mainFile.getName(), mainFile.getInputStream());
            } else {
                log.error("Can't get main file for resource {}", resource.getId());
            }
        }
        return files;
    }

    private static String createFolderPath(Folder folder, String groupRootFolder) {
        StringBuilder folderPath = new StringBuilder();

        Folder currentFolder = folder;
        while (null != currentFolder) {
            folderPath.insert(0, currentFolder.getTitle() + "/");
            currentFolder = currentFolder.getParentFolder();
        }

        return folderPath.toString();
    }

    private static InputStream getWebResourcesAsHtml(List<Resource> webResources) {
        Document indexFile = new Document(DocumentType.HTMLStrict);
        indexFile.head.appendChild(new Meta("text/html;charset=UTF-8"));

        Style tableStyle = new Style("text/css");
        tableStyle.appendText("table{font-family:'Trebuchet MS',Arial,Helvetica,sans-serif;border-collapse:collapse;width:100%;}" +
            "td,th{border:1px solid #ddd;padding:8px;}tr:nth-child(even){background-color:#f2f2f2;}tr:hover{background-color:#ddd;}" +
            "th{padding-top:12px;padding-bottom:12px;text-align:left;background-color:#4CAF50;color:#fff;}");
        indexFile.head.appendChild(tableStyle);

        Table table = new Table();

        Tr headerRow = new Tr();
        headerRow.appendChild(new Td().appendText("Link"));
        headerRow.appendChild(new Td().appendText("Type"));
        headerRow.appendChild(new Td().appendText("Group"));
        headerRow.appendChild(new Td().appendText("Location"));
        headerRow.appendChild(new Td().appendText("Added by"));
        table.appendChild(new Thead().appendChild(headerRow));

        Tbody tbody = new Tbody();
        for (Resource resource : webResources) {
            Tr row = new Tr();
            row.appendChild(new Td().appendChild(new A(resource.getUrl(), "_blank", resource.getTitle())));
            row.appendChild(new Td().appendText(resource.getType().name()));
            row.appendChild(new Td().appendText(resource.getGroup() != null ? resource.getGroup().getTitle() : ""));
            row.appendChild(new Td().appendText(resource.getPrettyPath() != null ? resource.getPrettyPath() : ""));
            row.appendChild(new Td().appendText(resource.getUser().getRealUsername()));
            tbody.appendChild(row);
        }

        indexFile.body.appendChild(table.appendChild(tbody));
        String document = indexFile.write();

        return new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8));
    }
}

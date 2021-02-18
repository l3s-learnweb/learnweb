package de.l3s.learnweb.resource;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
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

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.user.User;

public class ExportManager {
    private static final Logger log = LogManager.getLogger(ExportManager.class);
    private static final String EXPORT_FILE_PREFIX = "learnweb-";
    private static final String EXPORT_FILE_EXT = ".zip";

    private final Learnweb learnweb;

    public ExportManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public void handleResponse(User user) throws IOException {
        handleResponse(packResources(null, user.getResources()), user.getUsername().toLowerCase());
    }

    public void handleResponse(final Group group) throws IOException {
        handleResponse(packResources(group.getTitle(), group.getResources()), "group_" + group.getId());
    }

    /**
     * Entry point for handling HTTP request.
     */
    private void handleResponse(final Map<String, InputStream> resourcesToPack, final String fileSuffix) throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + EXPORT_FILE_PREFIX + fileSuffix + EXPORT_FILE_EXT + "\"");

        OutputStream responseOutputStream = response.getOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(responseOutputStream))) {
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
        }
        responseOutputStream.flush();
        responseOutputStream.close();
        facesContext.responseComplete();
    }

    private Map<String, InputStream> packResources(final String groupTitle, final List<Resource> resources) throws IOException {
        List<Resource> learnwebResources = new ArrayList<>();
        List<Resource> webResources = new ArrayList<>();

        for (Resource resource : resources) {
            if (resource.getStorageType() == Resource.LEARNWEB_RESOURCE) {
                learnwebResources.add(resource);
            } else if (resource.getStorageType() == Resource.WEB_RESOURCE) {
                webResources.add(resource);
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

    private Map<String, InputStream> getLearnwebResources(List<Resource> resources, String groupRootFolder) throws IOException {
        Map<String, InputStream> files = new HashMap<>();

        for (Resource resource : resources) {
            Folder folder = learnweb.getDaoProvider().getFolderDao().findById(resource.getFolderId());
            String folderName = createFolderPath(folder, groupRootFolder);

            File mainFile = resource.getFile(TYPE.FILE_MAIN);

            // TODO @astappiev: remove when all files copied from originals
            if (mainFile == null && resource.getOriginalResourceId() != null) {
                Resource originalResource = learnweb.getDaoProvider().getResourceDao().findById(resource.getOriginalResourceId());
                if (originalResource != null) {
                    mainFile = originalResource.getFile(TYPE.FILE_MAIN);
                }
            }

            if (mainFile != null) {
                files.put(folderName + resource.getFileName(), new FileInputStream(mainFile.getActualFile()));
            } else {
                log.error("Can't get main file for resource {}", resource.getId());
            }
        }

        return files;
    }

    private String createFolderPath(Folder folder, String groupRootFolder) {
        StringBuilder folderPath = new StringBuilder();

        Folder currentFolder = folder;
        while (null != currentFolder) {
            folderPath.insert(0, currentFolder.getTitle() + "/");
            currentFolder = currentFolder.getParentFolder();
        }

        if (groupRootFolder != null) {
            folderPath.insert(0, groupRootFolder + "/");
        } else if (folder != null && folder.getGroup() != null) {
            folderPath.insert(0, folder.getGroup().getTitle() + "/");
        } else {
            folderPath.insert(0, "Private resources/");
        }

        return folderPath.toString();
    }

    private InputStream getWebResourcesAsHtml(List<Resource> webResources) {
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

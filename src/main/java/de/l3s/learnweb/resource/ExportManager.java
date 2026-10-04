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
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.omnifaces.util.Faces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.resource.File.FileType;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.glossary.GlossaryXLSXExporter;
import de.l3s.learnweb.user.User;
import de.l3s.util.bean.BeanHelper;

public final class ExportManager {
    private static final Logger log = LogManager.getLogger(ExportManager.class);

    private static final String EXPORT_FILE_PREFIX = "learnweb-";
    private static final String EXPORT_FILE_EXT = ".zip";
    private static final String EXPORT_CONTENT_TYPE = "application/zip";

    public static StreamedContent streamResources(User user) {
        return streamResources(packResources(null, user.getResources()), user.getUsername().toLowerCase());
    }

    public static StreamedContent streamResources(final Group group) {
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
            } else if (resource instanceof GlossaryResource glossaryResource) {
                try {
                    GlossaryXLSXExporter exporter = new GlossaryXLSXExporter(Locale.ENGLISH);
                    files.put(folderName + glossaryResource.getTitle() + ".xlsx", exporter.streamWorkbook(glossaryResource));
                } catch (IOException e) {
                    log.error("Unable to export glossary to XLSX", e);
                }
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
        // jsoup escapes the text and attributes
        Document indexFile = Document.createShell("");
        indexFile.prependChild(new DocumentType("html", "", ""));
        indexFile.head().appendElement("meta").attr("charset", "UTF-8");
        indexFile.head().appendElement("style").appendChild(new DataNode(
            "table{font-family:'Trebuchet MS',Arial,Helvetica,sans-serif;border-collapse:collapse;width:100%;}" +
            "td,th{border:1px solid #ddd;padding:8px;}tr:nth-child(even){background-color:#f2f2f2;}tr:hover{background-color:#ddd;}" +
            "th{padding-top:12px;padding-bottom:12px;text-align:left;background-color:#4CAF50;color:#fff;}"));

        Element table = indexFile.body().appendElement("table");

        ResourceBundle bundle = MessagesBundle.of(Faces.getLocale());
        Element headerRow = table.appendElement("thead").appendElement("tr");
        headerRow.appendElement("th").text(bundle.getString("title"));
        headerRow.appendElement("th").text(bundle.getString("type"));
        headerRow.appendElement("th").text(bundle.getString("group"));
        headerRow.appendElement("th").text(bundle.getString("location"));
        headerRow.appendElement("th").text(StringUtils.capitalize(bundle.getString("added_by")));

        Element tbody = table.appendElement("tbody");
        for (Resource resource : webResources) {
            Element row = tbody.appendElement("tr");
            Element titleCell = row.appendElement("td");
            if (Strings.CI.startsWithAny(resource.getUrl(), "http://", "https://")) { // no links to other schemes, e.g. javascript:
                titleCell.appendElement("a").attr("href", resource.getUrl()).attr("target", "_blank").text(StringUtils.defaultString(resource.getTitle()));
            } else {
                titleCell.text(StringUtils.defaultString(resource.getTitle()));
            }
            row.appendElement("td").text(BeanHelper.getMessageOrDefault("search_filters." + resource.getType().name(), resource.getType().toString()));
            row.appendElement("td").text(resource.getGroup() != null ? StringUtils.defaultString(resource.getGroup().getTitle()) : "");
            row.appendElement("td").text(StringUtils.defaultString(resource.getPrettyPath()));
            row.appendElement("td").text(resource.getUser().getUsername());
        }

        return new ByteArrayInputStream(indexFile.outerHtml().getBytes(StandardCharsets.UTF_8));
    }
}

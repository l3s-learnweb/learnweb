package de.l3s.learnweb.resource;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;
import com.hp.gagawa.java.Document;
import com.hp.gagawa.java.DocumentType;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Meta;
import com.hp.gagawa.java.elements.Style;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Thead;
import com.hp.gagawa.java.elements.Tr;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.gdpr.ResourcesFileNotFoundException;
import de.l3s.learnweb.gdpr.UnknownResourceTypeException;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.user.User;
import eu.bitwalker.useragentutils.UserAgent;

public class ExportManager
{
    private static final Logger log = Logger.getLogger(ExportManager.class);
    private static final String EXPORT_FILE_NAME = "full_data.zip";

    private User user;
    private Learnweb learnweb;

    public ExportManager(User user, Learnweb learnweb)
    {
        this.user = user;
        this.learnweb = learnweb;
    }

    /**
     * Entry point for handling HTTP request.
     */
    public void handleResponse(String resourcesType) throws Exception
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        response.setContentType("Content-type: application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + EXPORT_FILE_NAME + "\"");

        OutputStream responseOutputStream = response.getOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(responseOutputStream));
        zipOutputStream.setLevel(Deflater.NO_COMPRESSION);

        Map<String, InputStream> resourcesToPack = new HashMap<>();
        switch(resourcesType)
        {
        case "group":
            // TODO needs refactoring. group id should not be passed this way. Why isn't the platform put into account for user downloads
            String userAgent = facesContext.getExternalContext().getRequestHeaderMap().get("User-Agent");
            String platform = UserAgent.parseUserAgentString(userAgent).getOperatingSystem().getName();
            int groupId = Integer.parseInt(facesContext.getExternalContext().getRequestParameterMap().get("request_group_form:group_id"));
            resourcesToPack = packGroupResources(this.learnweb.getGroupManager().getGroupById(groupId), platform);
            break;
        case "user":
            resourcesToPack = packUserResources(user.getResources());
            break;
        default:
            break;
        }

        for(Map.Entry<String, InputStream> resourceFile : resourcesToPack.entrySet())
        {
            ZipEntry fileEntry = new ZipEntry(resourceFile.getKey());
            zipOutputStream.putNextEntry(fileEntry);
            try
            {
                byte[] b = new byte[2048];

                while((resourceFile.getValue().read(b)) != -1)
                {
                    zipOutputStream.write(b);
                }
            }
            catch(Throwable t)
            {
                throw new ResourcesFileNotFoundException(resourceFile.toString());
            }
            zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
        responseOutputStream.flush();
        responseOutputStream.close();
        facesContext.responseComplete();
    }

    /**
     * Put internal resources as files inside archive, index file allows to navigate to external resources.
     */
    private Map<String, InputStream> packUserResources(List<Resource> userResources) throws Exception
    {
        Document indexFile = createIndexFile(userResources);
        InputStream inputStream = new ByteArrayInputStream(indexFile.write().getBytes());
        Map<String, InputStream> filesToPack = new HashMap<>();
        filesToPack.put("index.html", inputStream);
        filesToPack.putAll(this.getInternalResources(userResources));
        return filesToPack;
    }

    /**
     * Simply put all resources as files inside archive.
     */
    private Map<String, InputStream> packGroupResources(Group group, String platform) throws Exception
    {
        final List<Resource> groupResources = group.getResources();
        Map<String, InputStream> filesToPack = new HashMap<>();
        filesToPack.putAll(this.getAllResources(groupResources, platform, group.getTitle()));
        return filesToPack;
    }

    /**
     * Future feature to export only selected group resources.
     * TODO:
     * - print list of all resources in dialog box
     * - add checkboxes in front of every resource with resource ID as value with checked value by default
     * - parse checkboxes in handleResponse and pass list of not selected resource IDs to packGroupResources
     * - filter resources list before putting them to filesToPack list
     *
     * private Map<String, InputStream> packGroupResources(Group group, String platform, List<Integer> selectedResources) throws Exception
     * {
     * final List<Resource> groupResources = group.getResources();
     *
     * for(Resource r : groupResources)
     * {
     * if(!selectedResources.contains(r.getId()))
     * {
     * groupResources.remove(r);
     * }
     * }
     *
     * Map<String, InputStream> filesToPack = new HashMap<>();
     * filesToPack.putAll(this.getAllResources(groupResources, platform, group.getTitle()));
     * return filesToPack;
     * }
     */

    private Map<String, InputStream> getInternalResources(List<Resource> resources) throws FileNotFoundException
    {
        Map<String, InputStream> files = new HashMap<>();

        for(Resource lwResource : resources)
        {
            if(lwResource.getStorageType() == Resource.LEARNWEB_RESOURCE)
            {
                files.put(createFileName("resources/", lwResource.getFileName()), new FileInputStream(lwResource.getFile(TYPE.FILE_MAIN).getActualFile()));
            }
        }

        return files;
    }

    private Map<String, InputStream> getAllResources(List<Resource> resources, String platform, String groupRootFolder) throws IOException, SQLException
    {
        Map<String, InputStream> files = new HashMap<>();

        for(Resource lwResource : resources)
        {
            Folder folder = this.learnweb.getGroupManager().getFolder(lwResource.getFolderId());
            String folderName = this.createFolderPath(folder, groupRootFolder);

            if(lwResource.getStorageType() == Resource.LEARNWEB_RESOURCE)
            {
                File mainFile = lwResource.getFile(TYPE.FILE_MAIN);
                if(null == mainFile)
                    log.error("Can't get main file for resource=" + lwResource.getId());
                else
                    files.put(folderName + lwResource.getFileName(), new FileInputStream(mainFile.getActualFile()));
            }
            else if(lwResource.getStorageType() == Resource.WEB_RESOURCE)
            {
                ImmutablePair<String, InputStream> file = createUrlFile(lwResource.getUrl(), platform, lwResource.getTitle());
                files.put(createFileName(folderName, file.getLeft()), file.getRight());
            }
        }

        return files;
    }

    private String createFolderPath(Folder folder, String groupRootFolder) throws SQLException
    {
        Folder currentFolder = folder;
        StringBuilder folderPath = new StringBuilder();
        while(null != currentFolder)
        {
            folderPath.insert(0, currentFolder.getTitle() + "/");
            currentFolder = currentFolder.getParentFolder();
        }
        folderPath.insert(0, groupRootFolder + "/");
        return folderPath.toString();
    }

    private Document createIndexFile(List<Resource> userResources) throws SQLException, UnknownResourceTypeException
    {
        Document indexFile = new Document(DocumentType.HTMLStrict);

        Style tableStyle = new Style("text/css");
        tableStyle.appendText("#resources {\n" + "  font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif;\n" + "  border-collapse: collapse;\n" + "  width: 100%;\n" + "}\n" + "\n" + "#resources td, #resources th {\n" + "  border: 1px solid #ddd;\n" + "  padding: 8px;\n"
                + "}\n" + "\n" + "#resources tr:nth-child(even){background-color: #f2f2f2;}\n" + "\n" + "#resources tr:hover {background-color: #ddd;}\n" + "\n" + "#resources th {\n" + "  padding-top: 12px;\n" + "  padding-bottom: 12px;\n" + "  text-align: left;\n"
                + "  background-color: #4CAF50;\n" + "  color: white;\n" + "}");
        indexFile.head.appendChild(tableStyle);

        Meta charset = new Meta("text/html;charset=UTF-8");
        indexFile.head.appendChild(charset);

        Table resourcesTable = new Table();
        resourcesTable.setId("resources");

        Thead tableHeader = new Thead();
        Tr headerRow = new Tr();
        headerRow.appendChild(new Td().appendText("Your Resources"));
        headerRow.appendChild(new Td().appendText("Resource Type"));
        headerRow.appendChild(new Td().appendText("Description"));
        headerRow.appendChild(new Td().appendText("Author"));
        headerRow.appendChild(new Td().appendText("Owner Username"));
        tableHeader.appendChild(headerRow);
        resourcesTable.appendChild(tableHeader);

        for(Resource lwResource : userResources)
        {
            //final LinkedHashMap<Integer, de.l3s.learnweb.resource.File> resourceThumbnails = lwResource.getFiles();

            List<String> metadata = new ArrayList<>();

            metadata.add(lwResource.getType().name());
            metadata.add(lwResource.getDescription());
            metadata.add(lwResource.getAuthor());
            metadata.add(lwResource.getUser().getRealUsername());

            switch(lwResource.getStorageType())
            {
            case Resource.LEARNWEB_RESOURCE:
                A internalLink = new A("resources/" + lwResource.getFileName(), "", lwResource.getFileName());
                appendRow(composeRow(internalLink, metadata), resourcesTable);
                break;
            case Resource.WEB_RESOURCE:
                A externalLink = new A(lwResource.getUrl(), "_blank", lwResource.getTitle());
                appendRow(composeRow(externalLink, metadata), resourcesTable);
                break;
            default:
                throw new UnknownResourceTypeException(lwResource.getStringStorageType());
            }
        }

        indexFile.body.appendChild(resourcesTable);

        return indexFile;
    }

    private Tr composeRow(A link, List<String> metadata)
    {
        Tr row = new Tr();

        Td cell = new Td();
        cell.appendChild(link);
        row.appendChild(cell);

        for(String item : metadata)
        {
            Td tmpCell = new Td();
            tmpCell.appendText(item);
            row.appendChild(tmpCell);
        }

        return row;
    }

    private void appendRow(Tr row, Table resourcesTable)
    {
        resourcesTable.appendChild(row);
    }

    private ImmutablePair<String, InputStream> createUrlFile(String url, String platform, String title) throws IOException
    {
        switch(platform)
        {
        case "Linux":
            String desktopFile = "[Desktop Entry]\n" +
                    "Encoding=UTF-8\n" +
                    "Icon=text-html\n" +
                    "Type=Link\n" +
                    "URL=" + url;

            return new ImmutablePair<String, InputStream>(title + ".desktop", new ByteArrayInputStream(desktopFile.getBytes()));
        case "Windows":
            String urlFile = "[InternetShortcut]\n" +
                    "URL=" + url;
            return new ImmutablePair<String, InputStream>(title + ".url", new ByteArrayInputStream(urlFile.getBytes()));
        case "macOS":
        case "Mac OS X":
            NSDictionary root = new NSDictionary();
            root.put("URL", new NSString(url));

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PropertyListParser.saveAsXML(root, byteArrayOutputStream);

            return new ImmutablePair<String, InputStream>(title + ".weblock", new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        default:
            return new ImmutablePair<String, InputStream>(title + ".txt", new ByteArrayInputStream(url.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private String createFileName(final String path, final String fileName)
    {
        String result = path + fileName;
        // remove illegal symbols to avoid file system errors
        result = result.replaceAll("[\"<>|*?:/\\\\]", "_");
        return result;
    }
}

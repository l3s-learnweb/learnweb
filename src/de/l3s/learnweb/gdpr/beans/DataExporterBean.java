package de.l3s.learnweb.gdpr.beans;

import com.hp.gagawa.java.Document;
import com.hp.gagawa.java.DocumentType;
import com.hp.gagawa.java.elements.*;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.gdpr.exceptions.ResourcesFileNotFoundException;
import de.l3s.learnweb.gdpr.exceptions.UnknownResourceTypeException;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Accordingly to GDPR user can be able to download all collected about him data.
 * This bean responses on request of data with
 * */
@Named
@ViewScoped
public class DataExporterBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(DataExporterBean.class);
    private static final String EXPORT_FILE_NAME = "full_data.zip";
    private static final long serialVersionUID = -505457925640299810L;

    public DataExporterBean(){}

    public void requestInformation() throws Exception
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        final List<Resource> userResources = user.getResources();
        List<de.l3s.learnweb.resource.File> filesToZip = new LinkedList<>();

        Document indexFile = new Document(DocumentType.HTMLStrict);

        Style tableStyle = new Style("text/css");
        tableStyle.appendText("#resources {\n" + "  font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif;\n" + "  border-collapse: collapse;\n" + "  width: 100%;\n" + "}\n" + "\n" + "#resources td, #resources th {\n" + "  border: 1px solid #ddd;\n" + "  padding: 8px;\n" + "}\n" + "\n" + "#resources tr:nth-child(even){background-color: #f2f2f2;}\n" + "\n" + "#resources tr:hover {background-color: #ddd;}\n" + "\n" + "#resources th {\n" + "  padding-top: 12px;\n" + "  padding-bottom: 12px;\n" + "  text-align: left;\n" + "  background-color: #4CAF50;\n" + "  color: white;\n" + "}");
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

        for(Resource lwResource : userResources){
            final LinkedHashMap<Integer, de.l3s.learnweb.resource.File> resourceThumbnails = lwResource.getFiles();

            List<String> metadata = new ArrayList<>();

            metadata.add(lwResource.getType().name());
            metadata.add(lwResource.getDescription());
            metadata.add(lwResource.getAuthor());
            metadata.add(lwResource.getUser().getRealUsername());

            switch(lwResource.getStorageType()){
                case Resource.LEARNWEB_RESOURCE:
                    A internalLink = new A("resources/" + lwResource.getFileName(), "", lwResource.getFileName());
                    appendRow(composeRow(internalLink, metadata), resourcesTable);
                    getMainFile(resourceThumbnails, filesToZip);
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

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        response.setContentType("Content-type: application/zip");
        response.setHeader("Content-Disposition","attachment; filename=\"" + EXPORT_FILE_NAME + "\"");

        OutputStream responseOutputStream = response.getOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(responseOutputStream));
        zipOutputStream.setLevel(Deflater.NO_COMPRESSION);

        // populate zip
        zipOutputStream.putNextEntry( new ZipEntry("index.html"));
        zipOutputStream.write(indexFile.write().getBytes());

        // populate resources folder
        for (de.l3s.learnweb.resource.File resourceFile : filesToZip){
            ZipEntry fileEntry = new ZipEntry("resources/"+resourceFile.getName());
            zipOutputStream.putNextEntry(fileEntry);
            File originalFile = resourceFile.getActualFile();
            try
            {
                InputStream is = new FileInputStream(originalFile);
                byte[] b = new byte[2048];
                int length;
                while ((length = is.read(b)) != -1) {
                    zipOutputStream.write(b);
                }
            } catch(Throwable t){
                throw new ResourcesFileNotFoundException(resourceFile.toString());
            }
        }

        // close all streams
        zipOutputStream.closeEntry();
        zipOutputStream.close();
        responseOutputStream.flush();
        responseOutputStream.close();
        facesContext.responseComplete();
    }

    private Tr composeRow(A link, List<String> metafdata){
        Tr row = new Tr();

        Td cell = new Td();
        cell.appendChild(link);
        row.appendChild(cell);

        for (String item : metafdata){
            Td tmpCell = new Td();
            tmpCell.appendText(item);
            row.appendChild(tmpCell);
        }

        return row;
    }

    private void appendRow(Tr row, Table resourcesTable){
        resourcesTable.appendChild(row);
    }

    private void getMainFile(LinkedHashMap<Integer, de.l3s.learnweb.resource.File> resourceFiles, List<de.l3s.learnweb.resource.File> filesToZip){
        for (Map.Entry entry : resourceFiles.entrySet()){
            final de.l3s.learnweb.resource.File f = (de.l3s.learnweb.resource.File)entry.getValue();
            if (f.getType().name().equals("FILE_MAIN")){
                filesToZip.add(f);
                break;
            }
        }
    }
}

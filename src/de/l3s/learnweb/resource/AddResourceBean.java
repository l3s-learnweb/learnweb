package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.office.FileUtility;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.util.UrlHelper;

@Named
@ViewScoped
public class AddResourceBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1736402639245432708L;
    private static final Logger log = Logger.getLogger(AddResourceBean.class);

    private int formStep = 1;
    private Resource resource;
    private Group targetGroup;
    private Folder targetFolder;

    @Inject
    private SelectLocationBean selectLocationBean;

    // caches
    private transient List<SelectItem> availableGlossaryLanguages;

    public void reset()
    {
        resource = new Resource();
        resource.setUser(getUser());
        resource.setSource(ResourceService.learnweb);
        resource.setLocation("Learnweb");
        resource.setStorageType(Resource.LEARNWEB_RESOURCE);
        resource.setDeleted(true); // hide the resource from the frontend until it is finally saved

        formStep = 1;
    }

    public void setResourceTypeGlossary()
    {
        GlossaryResource glossaryResource = new GlossaryResource();
        glossaryResource.setDeleted(true);
        glossaryResource.setAllowedLanguages(getUser().getOrganisation().getGlossaryLanguages()); // by default select all allowed languages

        this.resource = glossaryResource;
    }

    public void handleFileUpload(FileUploadEvent event)
    {
        try
        {
            log.debug("Handle File upload");
            resource.setSource(ResourceService.desktop);
            resource.setDeleted(true);

            UploadedFile uploadedFile = event.getFile();

            log.debug("Getting the fileInfo from uploaded file...");
            ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();
            FileInfo info = rme.getFileInfo(uploadedFile.getInputStream(), uploadedFile.getFileName());

            log.debug("Saving the file...");
            File file = new File(TYPE.FILE_MAIN, info.getFileName(), info.getMimeType());
            file.setDownloadLogActivated(true);

            FileManager fileManager = getLearnweb().getFileManager();
            fileManager.save(file, uploadedFile.getInputStream());

            resource.addFile(file);
            resource.setUrl(file.getUrl());
            resource.setFileUrl(file.getUrl()); // for Loro resources the file url is different from the url
            resource.setFileName(info.getFileName());

            log.debug("Extracting metadata from the file...");
            rme.processFileResource(resource, info);

            log.debug("Creating thumbnails from the file...");
            Thread createThumbnailThread = new ResourcePreviewMaker.CreateThumbnailThread(resource);
            createThumbnailThread.start();
            createThumbnailThread.join(1000);

            log.debug("Next step");
            formStep++;
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void validateUrl(FacesContext context, UIComponent comp, Object value)
    {
        if(UrlHelper.validateUrl(value.toString()) == null)
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "invalid_url"));
        }
    }

    public void handleUrlInput()
    {
        try
        {
            log.debug("Handle Url input");
            resource.setSource(ResourceService.internet);
            resource.setStorageType(Resource.WEB_RESOURCE);
            resource.setUrl(UrlHelper.validateUrl(resource.getUrl()));

            log.debug("Extracting info from given url...");
            getLearnweb().getResourceMetadataExtractor().processWebResource(resource);

            log.debug("Creating thumbnails from given url...");
            Thread createThumbnailThread = new ResourcePreviewMaker.CreateThumbnailThread(resource);
            createThumbnailThread.start();
            createThumbnailThread.join(1000); // wait for a second. If this take longer

            log.debug("Next step");
            this.formStep++;
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void createDocument()
    {
        log.debug("Creating new document...");
        resource.setSource(ResourceService.learnweb);
        resource.setFileName(resource.getFileName() + FileUtility.getInternalExtension(resource.getType()));

        try(FileInputStream sampleFile = new FileInputStream(FileUtility.getSampleOfficeFile(resource.getType())))
        {
            log.debug("Getting the fileInfo from uploaded file...");
            ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();
            FileInfo info = rme.getFileInfo(sampleFile, resource.getFileName());

            log.debug("Saving file...");
            File file = new File(TYPE.FILE_MAIN, info.getFileName(), info.getMimeType());
            file.setDownloadLogActivated(true);

            FileManager fileManager = getLearnweb().getFileManager();
            fileManager.save(file, sampleFile);

            resource.addFile(file);
            resource.setUrl(file.getUrl());
            resource.setFileUrl(file.getUrl());
            resource.setFileName(info.getFileName());
            resource.setFormat(info.getMimeType());

            log.debug("Creating thumbnails from uploaded file...");
            Thread createThumbnailThread = new ResourcePreviewMaker.CreateThumbnailThread(resource);
            createThumbnailThread.start();
            createThumbnailThread.join(1000);

            log.debug("Next step");
            addResource();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void addResource()
    {
        try
        {
            if(!targetGroup.canAddResources(getUser()))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "group.you_cant_add_resource", targetGroup.getTitle());
                return;
            }

            log.debug("addResource; res=" + resource);

            resource.setDeleted(false);

            // add resource to a group if selected
            resource.setGroupId(targetGroup.getId());
            resource.setFolderId(targetFolder != null ? targetFolder.getId() : 0);
            getUser().setActiveGroup(targetGroup.getId());
            resource.save();

            log(Action.adding_resource, resource.getGroupId(), resource.getId());
            //detailed logging of new metadata (author, language)
            if(resource.getAuthor() != null)
                log(Action.adding_resource_metadata, resource.getGroupId(), resource.getId(), "added Author");
            if(resource.getLanguage() != null)
                log(Action.adding_resource_metadata, resource.getGroupId(), resource.getId(), "added Language");

            // create temporal thumbnails
            resource.postConstruct();

            // create thumbnails for the resource
            if(!resource.isProcessing() && (resource.getThumbnail0() == null || resource.getThumbnail0().getFileId() == 0 || resource.getType() == ResourceType.video))
            {
                new ResourcePreviewMaker.CreateThumbnailThread(resource).start();
            }

            addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());
            reset();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public String getCurrentPath() throws SQLException
    {
        if(targetGroup != null)
        {
            if(targetFolder != null)
            {
                return targetFolder.getPrettyPath();
            }

            return targetGroup.getTitle();
        }

        return getLocaleMessage("myResourcesTitle");
    }

    public Resource getResource()
    {
        return resource;
    }

    public int getFormStep()
    {
        return formStep;
    }

    public void setTarget(Group targetGroup, Folder targetFolder)
    {
        this.targetGroup = targetGroup;
        this.targetFolder = targetFolder;
        this.selectLocationBean.setTargetGroup(targetGroup);
        this.selectLocationBean.setTargetFolder(targetFolder);
    }

    public void updateTargetLocation()
    {
        this.targetGroup = selectLocationBean.getTargetGroup();
        this.targetFolder = selectLocationBean.getTargetFolder();
    }

    public List<SelectItem> getAvailableGlossaryLanguages()
    {
        if(null == availableGlossaryLanguages)
        {
            availableGlossaryLanguages = localesToSelectItems(getUser().getOrganisation().getGlossaryLanguages());
        }
        return availableGlossaryLanguages;
    }

    public SelectLocationBean getSelectLocationBean()
    {
        return selectLocationBean;
    }

    public void setSelectLocationBean(final SelectLocationBean selectLocationBean)
    {
        this.selectLocationBean = selectLocationBean;
    }
}

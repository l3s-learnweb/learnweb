package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.office.FileUtility;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.learnweb.resource.survey.Survey;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;
import de.l3s.util.UrlHelper;

@Named
@ViewScoped
public class AddResourceBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 1736402639245432708L;
    private static final Logger log = LogManager.getLogger(AddResourceBean.class);

    private int formStep = 1;
    private Resource resource;
    private Group targetGroup;
    private Folder targetFolder;

    // caches
    private transient List<SelectItem> availableGlossaryLanguages;

    public void reset() {
        resource = new Resource();
        resource.setUser(getUser());
        resource.setSource(ResourceService.learnweb);
        resource.setLocation("Learnweb");
        resource.setStorageType(Resource.LEARNWEB_RESOURCE);
        resource.setDeleted(true); // hide the resource from the frontend until it is finally saved

        formStep = 1;
    }

    public void create(final String type, Group targetGroup, Folder targetFolder) {
        this.reset();

        // Set target group and folder in beans
        this.targetGroup = targetGroup;
        this.targetFolder = targetFolder;

        // Set target view and defaults
        switch (type) {
            case "file":
                this.resource.setType(ResourceType.file);
                break;
            case "url":
            case "website":
                this.resource.setType(ResourceType.website);
                this.resource.setStorageType(Resource.WEB_RESOURCE);
                break;
            case "glossary":
                this.setResourceTypeGlossary();
                break;
            case "survey":
                this.setResourceTypeSurvey();
                break;
            case "document":
                this.resource.setType(ResourceType.document);
                break;
            case "spreadsheet":
                this.resource.setType(ResourceType.spreadsheet);
                break;
            case "presentation":
                this.resource.setType(ResourceType.presentation);
                break;
            default:
                log.error("Unsupported item type: {}", type);
                break;
        }
    }

    private void setResourceTypeGlossary() {
        GlossaryResource glossaryResource = new GlossaryResource();
        glossaryResource.setUser(getUser());
        glossaryResource.setSource(ResourceService.learnweb);
        glossaryResource.setLocation("Learnweb");
        glossaryResource.setStorageType(Resource.LEARNWEB_RESOURCE);
        glossaryResource.setDeleted(true);
        glossaryResource.setAllowedLanguages(getUser().getOrganisation().getGlossaryLanguages()); // by default select all allowed languages

        this.resource = glossaryResource;
    }

    private void setResourceTypeSurvey() {
        Survey survey = new Survey();
        survey.setOrganizationId(getUser().getOrganisationId());
        survey.setUserId(getUser().getId());
        survey.setTitle("Title placeholder"); // this values are overridden in addResource method
        survey.setDescription("Description placeholder");

        SurveyResource surveyResource = new SurveyResource();
        surveyResource.setSurvey(survey);
        surveyResource.setUser(getUser());
        surveyResource.setDeleted(true);
        surveyResource.setSource(ResourceService.learnweb);
        surveyResource.setLocation("Learnweb");
        surveyResource.setStorageType(Resource.LEARNWEB_RESOURCE);
        surveyResource.setType(ResourceType.survey);

        this.resource = surveyResource;
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            log.debug("Handle File upload");
            resource.setSource(ResourceService.desktop);
            resource.setDeleted(true);

            UploadedFile uploadedFile = event.getFile();

            log.debug("Getting the fileInfo from uploaded file...");
            FileInfo info = getLearnweb().getResourceMetadataExtractor().getFileInfo(uploadedFile.getInputStream(), uploadedFile.getFileName());

            log.debug("Saving the file...");
            File file = new File(TYPE.FILE_MAIN, info.getFileName(), info.getMimeType());
            file.setDownloadLogActivated(true);

            dao().getFileDao().save(file, uploadedFile.getInputStream());

            resource.addFile(file);
            resource.setUrl(file.getUrl());
            resource.setFileUrl(file.getUrl()); // for Loro resources the file url is different from the url
            resource.setFileName(info.getFileName());

            log.debug("Extracting metadata from the file...");
            getLearnweb().getResourceMetadataExtractor().processFileResource(resource, info);

            log.debug("Creating thumbnails from the file...");
            Thread createThumbnailThread = new ResourcePreviewMaker.CreateThumbnailThread(resource);
            createThumbnailThread.start();
            createThumbnailThread.join(1000);

            log.debug("Next step");
            formStep++;
        } catch (InterruptedException | IOException e) {
            addErrorMessage(e);
        }
    }

    public void handleUrlInput() {
        try {
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
            formStep++;
        } catch (InterruptedException e) {
            addErrorMessage(e);
        }
    }

    private void createDocument() {
        log.debug("Creating new document...");
        resource.setSource(ResourceService.learnweb);
        resource.setFileName(resource.getFileName() + FileUtility.getInternalExtension(resource.getType()));

        try {
            log.debug("Getting the fileInfo from uploaded file...");
            java.io.File sampleFile = FileUtility.getSampleOfficeFile(resource.getType());
            FileInfo info = getLearnweb().getResourceMetadataExtractor().getFileInfo(new FileInputStream(sampleFile), resource.getFileName());

            log.debug("Saving file...");
            File file = new File(TYPE.FILE_MAIN, info.getFileName(), info.getMimeType());
            file.setDownloadLogActivated(true);
            dao().getFileDao().save(file, new FileInputStream(sampleFile));

            resource.setTitle(info.getTitle());
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
        } catch (InterruptedException | URISyntaxException | IOException e) {
            addErrorMessage(e);
        }
    }

    public void addResource() {
        if (this.resource.isOfficeResource() && this.resource.getSource() != ResourceService.desktop) {
            this.createDocument();
        }

        if (!targetGroup.canAddResources(getUser())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "group.you_cant_add_resource", targetGroup.getTitle());
            return;
        }

        if (resource.getType() == ResourceType.survey && resource instanceof SurveyResource) {
            SurveyResource surveyResource = (SurveyResource) resource;
            surveyResource.getSurvey().setTitle(resource.getTitle());
            surveyResource.getSurvey().setDescription(resource.getDescription());
        }

        log.debug("addResource; res={}", resource);

        resource.setDeleted(false);

        // add resource to a group if selected
        resource.setGroupId(targetGroup.getId());
        resource.setFolderId(HasId.getIdOrDefault(targetFolder, 0));
        resource.save();
        getUser().setGuide(User.Guide.ADD_RESOURCE, true);

        log(Action.adding_resource, resource.getGroupId(), resource.getId());

        // create temporal thumbnails
        resource.postConstruct();

        // create thumbnails for the resource
        if (!resource.isProcessing()
            && (resource.getSmallThumbnail() == null || resource.getSmallThumbnail().getFileId() == null || resource.getType() == ResourceType.video)) {
            new ResourcePreviewMaker.CreateThumbnailThread(resource).start();
        }

        addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());
    }

    public String getCurrentPath() {
        if (targetFolder != null) {
            return targetGroup.getTitle() + " > " + targetFolder.getPrettyPath();
        }

        return targetGroup.getTitle();
    }

    public Resource getResource() {
        return resource;
    }

    public int getFormStep() {
        return formStep;
    }

    public List<SelectItem> getAvailableGlossaryLanguages() {
        if (null == availableGlossaryLanguages) {
            availableGlossaryLanguages = localesToSelectItems(getUser().getOrganisation().getGlossaryLanguages());
        }
        return availableGlossaryLanguages;
    }
}

package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File.FileType;
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

    @Inject
    private FileDao fileDao;

    // caches
    private transient List<SelectItem> availableGlossaryLanguages;

    public void create(final String type, Group targetGroup, Folder targetFolder) {
        this.formStep = 1;

        // Set target group and folder in beans
        this.targetGroup = targetGroup;
        this.targetFolder = targetFolder;

        // Set target view and defaults
        switch (type) {
            case "file":
                resource = new Resource(Resource.StorageType.LEARNWEB, ResourceType.file, ResourceService.desktop);
                break;
            case "url":
            case "website":
                resource = new Resource(Resource.StorageType.WEB, ResourceType.website, ResourceService.internet);
                break;
            case "glossary":
                GlossaryResource glossaryResource = new GlossaryResource();
                glossaryResource.setAllowedLanguages(getUser().getOrganisation().getGlossaryLanguages()); // by default select all allowed languages
                resource = glossaryResource;
                break;
            case "survey":
                Survey survey = new Survey();
                survey.setOrganisationId(getUser().getOrganisationId());
                survey.setUserId(getUser().getId());
                survey.setTitle("Title placeholder"); // this values are overridden in addResource method
                survey.setDescription("Description placeholder");

                SurveyResource surveyResource = new SurveyResource();
                surveyResource.setSurvey(survey);
                this.resource = surveyResource;
                break;
            case "document":
                resource = new Resource(Resource.StorageType.LEARNWEB, ResourceType.document, ResourceService.learnweb);
                break;
            case "spreadsheet":
                resource = new Resource(Resource.StorageType.LEARNWEB, ResourceType.spreadsheet, ResourceService.learnweb);
                break;
            case "presentation":
                resource = new Resource(Resource.StorageType.LEARNWEB, ResourceType.presentation, ResourceService.learnweb);
                break;
            default:
                log.error("Unsupported resource type: {}", type);
                break;
        }

        resource.setUser(getUser());
        resource.setDeleted(true); // hide the resource from the frontend until it is finally saved
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            log.debug("Handle File upload");
            UploadedFile uploadedFile = event.getFile();

            log.debug("Getting the fileInfo from uploaded file...");
            FileInfo info = getLearnweb().getResourceMetadataExtractor().getFileInfo(uploadedFile.getInputStream(), uploadedFile.getFileName());

            log.debug("Saving the file...");
            File file = new File(FileType.MAIN, info.getFileName(), info.getMimeType());
            fileDao.save(file, uploadedFile.getInputStream());
            resource.addFile(file);

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
        try {
            log.debug("Creating new document...");
            String fileName = resource.getTitle() + FileUtility.getInternalExtension(resource.getType());
            java.io.File sampleFile = FileUtility.getSampleOfficeFile(resource.getType());
            FileInfo info = getLearnweb().getResourceMetadataExtractor().getFileInfo(new FileInputStream(sampleFile), fileName);

            log.debug("Saving file...");
            File file = new File(FileType.MAIN, info.getFileName(), info.getMimeType());
            fileDao.save(file, new FileInputStream(sampleFile));
            resource.addFile(file);
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
        if (this.resource.isOfficeResource() && this.resource.getService() != ResourceService.desktop) {
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

package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serial;
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

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File.FileType;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.office.FileUtility;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.resource.web.WebResource;
import de.l3s.util.HasId;
import de.l3s.util.UrlHelper;
import de.l3s.util.bean.BeanHelper;

@Named
@ViewScoped
public class AddResourceBean extends ApplicationBean implements Serializable {
    @Serial
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
        resource = switch (type) {
            case "file" -> new Resource(Resource.StorageType.LEARNWEB, ResourceType.file, ResourceService.desktop);
            case "url", "website" -> new WebResource();
            case "glossary" -> {
                GlossaryResource glossaryResource = new GlossaryResource();
                glossaryResource.setAllowedLanguages(getUser().getOrganisation().getGlossaryLanguages()); // by default select all allowed languages
                yield glossaryResource;
            }
            case "survey" -> new SurveyResource();
            case "document" -> new Resource(Resource.StorageType.LEARNWEB, ResourceType.document, ResourceService.learnweb);
            case "spreadsheet" -> new Resource(Resource.StorageType.LEARNWEB, ResourceType.spreadsheet, ResourceService.learnweb);
            case "presentation" -> new Resource(Resource.StorageType.LEARNWEB, ResourceType.presentation, ResourceService.learnweb);
            default -> throw new IllegalStateException("Unsupported resource type: " + type);
        };

        resource.setUser(getUser());
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
            throw new HttpException("Failed to process URL input", e);
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
            throw new HttpException("Failed to create document", e);
        }
    }

    public void createResource() {
        if (resource.isOfficeResource() && resource.getService() != ResourceService.desktop) {
            this.createDocument();
        }

        addResource(resource);
    }

    public void addResource(Resource res) {
        if (!targetGroup.canAddResources(getUser())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "group.you_cant_add_resource", targetGroup.getTitle());
            return;
        }

        log.debug("addResource; res={}", res);

        // add resource to a group if selected
        res.setGroupId(targetGroup.getId());
        res.setFolderId(HasId.getIdOrDefault(targetFolder, 0));
        res.save();

        log.debug("addResource; saved={}", res.getId());
        log(Action.adding_resource, res.getGroupId(), res.getId());

        // create temporal thumbnails
        res.postConstruct();

        addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", res.getTitle());
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
            availableGlossaryLanguages = BeanHelper.getLocalesAsSelectItems(getUser().getOrganisation().getGlossaryLanguages(), getLocale());
        }
        return availableGlossaryLanguages;
    }
}

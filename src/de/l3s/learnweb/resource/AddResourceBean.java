package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource.OnlineStatus;
import de.l3s.learnweb.resource.Resource.ResourceType;
import de.l3s.learnweb.resource.glossary.LanguageItem.LANGUAGE;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.learnweb.user.Course;
import de.l3s.office.FileEditorBean;
import de.l3s.office.FileUtility;
import de.l3s.util.StringHelper;

@ViewScoped
@ManagedBean
public class AddResourceBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(AddResourceBean.class);
    private final static long serialVersionUID = 1736402639245432708L;

    private static final String OFFICE_FILES_FOLDER = "/de/l3s/learnweb/office/documents/";

    private Resource resource;

    private Group targetGroup;
    private Folder targetFolder;

    private LANGUAGE[] glossaryLaguage = { LANGUAGE.DE, LANGUAGE.EN, LANGUAGE.FR, LANGUAGE.IT, LANGUAGE.NL };
    private String newUrl;
    @Deprecated
    private Date surveyOpenDate; // TODO if a survey is created then this.resource should become a SurveyResource instance
    @Deprecated
    private Date surveyCloseDate; // TODO if a survey is created then this.resource should become a SurveyResource instance

    private int formStep = 1;

    @ManagedProperty(value = "#{fileEditorBean}")
    private FileEditorBean fileEditorBean;

    public AddResourceBean()
    {
        resource = new Resource();
        resource.setSource("Internet");
        resource.setLocation("Learnweb");
        resource.setStorageType(Resource.LEARNWEB_RESOURCE);
        resource.setDeleted(true); // hide the resource from the frontend until it is finally saved
    }

    public void createFile()
    {
        try
        {
            log.debug("Creating new file..");
            resource.setSource("Learnweb");
            resource.setLocation("Learnweb");
            resource.setStorageType(Resource.LEARNWEB_RESOURCE);
            resource.setUser(getUser());
            resource.setDeleted(true);
            java.io.File newFile = getOfficeFileFromResources();
            FileInfo info = null;
            try
            {
                FileManager fileManager = getLearnweb().getFileManager();
                ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();

                log.debug("Getting the fileInfo from uploaded file...");
                info = rme.getFileInfo(new FileInputStream(newFile), resource.getFileName());

                log.debug("Saving file...");
                File file = new File();
                file.setType(TYPE.FILE_MAIN);
                file.setName(info.getFileName());
                file.setMimeType(info.getMimeType());
                file.setDownloadLogActivated(true);
                fileManager.save(file, new FileInputStream(newFile));
                resource.addFile(file);
                resource.setUrl(file.getUrl());
                resource.setFileUrl(file.getUrl()); // for Loro resources the file url is different from the url
                resource.setFileName(info.getFileName());

                log.debug("Extracting info from uploaded file...");
                rme.processFileResource(resource, info);
                resource.setDescription(StringUtils.EMPTY);
                log.debug("Creating thumbnails from uploaded file...");
                Thread createThumbnailThread = new CreateThumbnailThread(resource);
                createThumbnailThread.start();
                createThumbnailThread.join();
            }
            catch(Exception e)
            {
                log.error("Thumbnail creation failed for " + info);
            }
            addResource();

        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    private java.io.File getOfficeFileFromResources() throws URISyntaxException
    {
        ResourceType resourceType = resource.getType();
        resource.setFileName(resource.getFileName() + FileUtility.getInternalExtension(resourceType));
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL resourceUrl = classloader.getResource(OFFICE_FILES_FOLDER + FileUtility.getRightSampleName(resourceType));
        return new java.io.File(resourceUrl.toURI());
    }

    public void clearForm()
    {
        resource = new Resource();
        resource.setSource("Internet");
        resource.setLocation("Learnweb");
        formStep = 1;
    }

    public void handleFileUpload(FileUploadEvent event)
    {
        try
        {
            log.debug("Handle File upload");

            resource.setSource("Desktop");
            resource.setLocation("Learnweb");
            resource.setStorageType(Resource.LEARNWEB_RESOURCE);
            resource.setDeleted(true);
            resource.setUser(getUser());

            UploadedFile uploadedFile = event.getFile();

            FileManager fileManager = getLearnweb().getFileManager();
            ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();

            log.debug("Getting the fileInfo from uploaded file...");
            FileInfo info = rme.getFileInfo(uploadedFile.getInputstream(), uploadedFile.getFileName());

            log.debug("Saving file to database...");
            File file = new File();
            file.setType(TYPE.FILE_MAIN);
            file.setName(info.getFileName());
            file.setMimeType(info.getMimeType());
            file.setDownloadLogActivated(true);
            fileManager.save(file, uploadedFile.getInputstream());
            resource.addFile(file);
            resource.setUrl(file.getUrl());
            resource.setFileUrl(file.getUrl()); // for Loro resources the file url is different from the url
            resource.setFileName(info.getFileName());

            log.debug("Extracting info from uploaded file...");
            rme.processFileResource(resource, info);

            log.debug("Creating thumbnails from uploaded file...");
            Thread createThumbnailThread = new CreateThumbnailThread(resource);
            createThumbnailThread.start();
            createThumbnailThread.join(500);

            /* not used in any course right now
             * disabled to save time
             *
            User user = getUser();
            //resource = user.addResource(resource);

            // check if the user is logged in at interweb and to which services the file can be uploaded to
            if(user.isLoggedInInterweb())
            {
                String type = resource.getType().toLowerCase();
                List<ServiceInformation> services = user.getInterweb().getAuthorizationInformation(true).getServices();
                uploadServices = new LinkedList<ServiceInformation>(); // the services which accept the resource media type
                for(ServiceInformation service : services)
                {
                    if(service.isAuthorized())
                    {
                        if(service.getId().equals("YouTube") && type.equals("video"))
                        {
                            selectedUploadServices = new ArrayList<String>();
                            selectedUploadServices.add("YouTube");
                            uploadServices.add(service);
                        }
                        else if(service.getId().equals("Flickr") && type.equals("video"))
                            continue;
                        else if(service.getMediaTypes().contains(type))
                            uploadServices.add(service);
                    }
                }
            }
             */

            nextStep();
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void validateUrl(FacesContext context, UIComponent comp, Object value) throws ValidatorException
    {
        if(checkUrl(value.toString().trim()) == null)
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "invalid_url"));
        }
    }

    public void handleUrlInput()
    {
        try
        {
            log.debug("Handle Url input");

            resource.setStorageType(Resource.WEB_RESOURCE);
            resource.setUrl(checkUrl(resource.getUrl()));
            resource.setUser(getUser());
            resource.setSource("Internet");

            log.debug("Extracting info from given url...");
            ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();
            rme.processWebResource(resource);

            log.debug("Creating thumbnails from given url...");
            Thread createThumbnailThread = new CreateThumbnailThread(resource);
            createThumbnailThread.start();
            createThumbnailThread.join(1000);

            nextStep();
        }
        catch(InterruptedException e)
        {
            addFatalMessage(e);
        }
    }

    public void addGlossary() throws IOException
    {
        try
        {
            resource.setDeleted(false);
            resource.setSource("Learnweb");
            resource.setType(ResourceType.glossary);
            resource.setUrl(getLearnweb().getServerUrl() + "/lw/showGlossary.jsf?resource_id=" + Integer.toString(resource.getId()));

            Resource iconResource = getLearnweb().getResourceManager().getResource(200233);
            resource.setThumbnail0(iconResource.getThumbnail0());
            resource.setThumbnail1(iconResource.getThumbnail1());
            resource.setThumbnail2(iconResource.getThumbnail2());
            resource.setThumbnail3(iconResource.getThumbnail3());
            resource.setThumbnail4(iconResource.getThumbnail4());

            // add resource to a group if selected
            resource.setGroupId(getTargetGroupId());
            resource.setFolderId(getTargetFolderId());
            getUser().setActiveGroup(getTargetGroupId());

            if(resource.getId() == -1)
            {
                resource = getUser().addResource(resource);
                getLearnweb().getGlossariesManager().setLanguagePairs(resource.getId(), resource.getLanguageOne().toString(), resource.getLanguageTwo().toString());
            }
            else
                resource.save();

            log(Action.adding_resource, getTargetGroupId(), resource.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());

            UtilBean.getGroupDetailBean().updateResourcesFromSolr();

        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void addSurvey() throws IOException
    {

        try
        {
            resource.setDeleted(false);
            resource.setSource("Survey");
            resource.setType(Resource.ResourceType.survey);
            resource.setUrl(getLearnweb().getServerUrl() + "/xxxxxsurvey.jsf?resource_id=" + Integer.toString(resource.getId()));

            Resource iconResource = getLearnweb().getResourceManager().getResource(204095);

            resource.setThumbnail0(iconResource.getThumbnail0());
            resource.setThumbnail1(iconResource.getThumbnail1());
            resource.setThumbnail2(iconResource.getThumbnail2());
            resource.setThumbnail3(iconResource.getThumbnail3());
            resource.setThumbnail4(iconResource.getThumbnail4());

            // add resource to a group if selected
            resource.setGroupId(getTargetGroupId());
            resource.setFolderId(getTargetFolderId());
            getUser().setActiveGroup(getTargetGroupId());

            if(resource.getId() == -1)
                resource = getUser().addResource(resource);

            else
            {

                resource.save();
            }

            getLearnweb().getCreateSurveyManager().createSurveyResource(resource.getId(), resource.getTitle(), resource.getDescription(), getSurveyOpenDate(), getSurveyCloseDate());
            log(Action.adding_resource, getTargetGroupId(), resource.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());

            UtilBean.getGroupDetailBean().updateResourcesFromSolr();

            //resource.setUrl("");
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public String[] getCourseList()
    {
        List<Course> courseByOrganization = new ArrayList<Course>();
        courseByOrganization = getLearnweb().getCourseManager().getCoursesByOrganisationId(getUser().getOrganisationId());
        ArrayList<String> courseTitles = new ArrayList<String>();
        for(Course c : courseByOrganization)
        {
            courseTitles.add(c.getTitle());
        }

        return courseTitles.toArray(new String[0]);
    }

    public Resource getResource()
    {
        return resource;
    }

    public void validateNewDocName(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        String fileName = (String) value;

        if(StringUtils.isEmpty(fileName))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "empty_file_name"));
        }
    }

    public void addResource()
    {
        try
        {
            log.debug("addResource; res=" + resource);

            if(resource.getStorageType() == Resource.LEARNWEB_RESOURCE && null == resource.getFile(TYPE.FILE_MAIN))
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "Select a file first");
                return;
            }

            if(resource.getStorageType() == Resource.WEB_RESOURCE)
            {
                if(!resource.getUrl().startsWith("http"))
                    resource.setUrl("http://" + resource.getUrl());
            }

            resource.setDeleted(false);
            resource.setUser(getUser());
            if(resource.isOfficeResource())
                getFileEditorBean().fillInFileInfo(resource);

            // add resource to a group if selected
            resource.setGroupId(getTargetGroupId());
            resource.setFolderId(getTargetFolderId());
            getUser().setActiveGroup(getTargetGroupId());

            if(resource.getId() == -1) // a new resource which is not stored in the database yet
                resource = getUser().addResource(resource);
            else
                resource.save();

            // create thumbnails for the resource
            if(!resource.isProcessing() && (resource.getThumbnail2() == null || resource.getThumbnail2().getFileId() == 0 || resource.getType().equals(Resource.ResourceType.video)))
            {
                new CreateThumbnailThread(resource).start();
            }

            log(Action.adding_resource, getTargetGroupId(), resource.getId());

            //detailed logging of new metadata (author, language, media source, media type
            if(resource.getAuthor() != null)
            {
                log(Action.adding_resource_metadata, getTargetGroupId(), resource.getId(), "added Author");
            }
            if(resource.getLanguage() != null)
            {
                log(Action.adding_resource_metadata, getTargetGroupId(), resource.getId(), "added Language");
            }
            if(resource.getMsource() != null)
            {
                log(Action.adding_resource_metadata, getTargetGroupId(), resource.getId(), "added Media source");
            }
            if(resource.getMtype() != null)
            {
                log(Action.adding_resource_metadata, getTargetGroupId(), resource.getId(), "added Media types");
            }

            addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());

            if(resource.getGroupId() != 0)
                UtilBean.getGroupDetailBean().updateResourcesFromSolr();

            UtilBean.getMyResourcesBean().updateResources();
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void cancelUpload()
    {
        resource = new Resource();
        resource.setSource("Internet");
        resource.setLocation("Learnweb");
        resource.setStorageType(Resource.LEARNWEB_RESOURCE);
    }

    public String getNewUrl()
    {
        return newUrl;
    }

    public void setNewUrl(String newUrl)
    {
        this.newUrl = newUrl;

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

    public void nextStep()
    {
        this.formStep++;
    }

    public void setFormStep(int step)
    {
        this.formStep = step;
    }

    public int getFormStep()
    {
        return formStep;
    }

    public void changeGroupListener()
    {
        log.debug("changeGroupListener");
    }

    public void preRenderView() throws IOException, DecoderException
    {
        if(getUser() == null) // not logged in
            return;
    }

    /**
     * This function checks if a given String is a valid url.
     * When the url leads to a redirect the function will return the target of the redirect.
     * Returns null if the url is invalid or not reachable.
     *
     * @param urlStr
     * @return
     */
    public static String checkUrl(String urlStr)
    {
        if(urlStr == null)
            return null;

        if(!urlStr.startsWith("http"))
            urlStr = "http://" + urlStr;

        HttpURLConnection connection;
        try
        {
            urlStr = StringHelper.convertUnicodeURLToAscii(urlStr);

            URL url = new URL(urlStr);

            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");

            int responseCode = connection.getResponseCode();
            if(responseCode / 100 == 2)
            {
                return urlStr;
            }
            else if(responseCode / 100 == 3)
            {
                String location = connection.getHeaderField("Location");
                if(location.startsWith("/"))
                {
                    int index = urlStr.indexOf("/", urlStr.indexOf("//") + 2);
                    String domain = index > 0 ? urlStr.substring(0, index) : urlStr;
                    return domain + location;
                }
                else
                    return location;
            }
            else
                return null;
        }
        catch(UnknownHostException e)
        {
            log.warn("unknown host: " + urlStr, e);
            return null;
        }
        catch(Throwable t)
        {
            log.error("invalid url: " + urlStr, t);
            return null;
        }
    }

    public void setTargetGroupId(int targetGroupId)
    {
        try
        {
            Group group = getLearnweb().getGroupManager().getGroupById(targetGroupId);
            this.targetGroup = group;
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void setTargetFolderId(int targetFolderId)
    {
        try
        {
            Folder folder = getLearnweb().getGroupManager().getFolder(targetFolderId);
            this.targetFolder = folder;
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public int getTargetGroupId()
    {
        if(targetGroup != null)
            return targetGroup.getId();

        return targetFolder != null ? targetFolder.getGroupId() : 0;
    }

    public int getTargetFolderId()
    {
        return targetFolder != null ? targetFolder.getId() : 0;
    }

    public Group getTargetGroup()
    {
        return targetGroup;
    }

    public void setTargetGroup(Group targetGroup)
    {
        this.targetGroup = targetGroup;
    }

    public Folder getTargetFolder()
    {
        return targetFolder;
    }

    public void setTargetFolder(Folder targetFolder)
    {
        this.targetFolder = targetFolder;
    }

    public FileEditorBean getFileEditorBean()
    {
        return fileEditorBean;
    }

    public void setFileEditorBean(FileEditorBean fileEditorBean)
    {
        this.fileEditorBean = fileEditorBean;
    }

    public LANGUAGE[] getGlossaryLaguage()
    {
        return glossaryLaguage;
    }

    public void setGlossaryLaguage(LANGUAGE[] glossaryLaguage)
    {
        this.glossaryLaguage = glossaryLaguage;
    }

    public Date getSurveyOpenDate()
    {
        return surveyOpenDate;
    }

    public void setSurveyOpenDate(Date surveyOpen)
    {
        this.surveyOpenDate = surveyOpen;
    }

    public Date getSurveyCloseDate()
    {
        return surveyCloseDate;
    }

    public void setSurveyCloseDate(Date surveyCloseDate)
    {
        this.surveyCloseDate = surveyCloseDate;
    }

    public static class CreateThumbnailThread extends Thread
    {

        private Resource resource;

        public CreateThumbnailThread(Resource resource)
        {
            log.debug("Create CreateThumbnailThread for " + resource.toString());
            this.resource = resource;
        }

        @Override
        public void run()
        {
            try
            {
                ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
                resource.setOnlineStatus(OnlineStatus.PROCESSING);
                rpm.processResource(resource);
                resource.setOnlineStatus(OnlineStatus.ONLINE);
                resource.save();
            }
            catch(Exception e)
            {
                log.error("Error in CreateThumbnailThread " + e);
            }
        }

    }

}

package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.codec.DecoderException;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import de.l3s.interwebj.AuthorizationInformation.ServiceInformation;
import de.l3s.interwebj.IllegalResponseException;
import de.l3s.learnweb.Course;
import de.l3s.learnweb.File;
import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.FileManager;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceMetadataExtractor;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
import de.l3s.office.FileEditorBean;
import de.l3s.util.StringHelper;

@ViewScoped
@ManagedBean
public class AddResourceBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(AddResourceBean.class);
    private final static long serialVersionUID = 1736402639245432708L;

    private Resource resource;

    // the id of the group and folder the new resource will be added to
    private int resourceTargetGroupId = 0;
    private int resourceTargetFolderId = 0;

    private Folder targetFolder;
    private Group targetGroup;

    private String newUrl;

    private List<ServiceInformation> uploadServices;
    private List<String> selectedUploadServices;

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

    public Resource getClickedResource()
    {
        return resource;
    }

    public void setClickedResource(Resource clickedResource)
    {
        log.warn("setClickedResource() was called but is not implemented");
        // this method might be called due to the strange right_panel implementation
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
            createThumbnailThread.join();

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
            resource.setType(Resource.ResourceType.glossary);
            resource.setUrl("");

            // add resource to a group if selected
            if(resourceTargetGroupId != 0)
            {
                resource.setGroupId(resourceTargetGroupId);
                getUser().setActiveGroup(resourceTargetGroupId);
            }

            if(resourceTargetFolderId != 0)
            {
                resource.setFolderId(resourceTargetFolderId);
            }

            if(resource.getId() == -1)
                resource = getUser().addResource(resource);
            else
            {

                resource.save();
            }

            Resource iconResource = getLearnweb().getResourceManager().getResource(200233);

            resource.setThumbnail0(iconResource.getThumbnail0());
            resource.setThumbnail1(iconResource.getThumbnail1());
            resource.setThumbnail2(iconResource.getThumbnail2());
            resource.setThumbnail3(iconResource.getThumbnail3());
            resource.setThumbnail4(iconResource.getThumbnail4());

            resource.setUrl(getLearnweb().getServerUrl() + "/lw/showGlossary.jsf?resource_id=" + Integer.toString(resource.getId()));
            resource.save();
            log(Action.adding_resource, resourceTargetGroupId, resource.getId(), "");
            addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());

            UtilBean.getGroupDetailBean().updateResourcesFromSolr();

            // TODO: looks strange, maybe redundant
            resource = new Resource();
            resource.setSource("Internet");
            resource.setLocation("Learnweb");
            resource.setStorageType(Resource.LEARNWEB_RESOURCE);
            resource.setDeleted(true);
            //resource.setUrl("");
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    /*public void addSurvey() throws IOException
    {

        try
        {
            resource.setDeleted(false);
            resource.setSource("Survey");
            resource.setType("Survey");
            resource.setUrl("");

            // add resource to a group if selected
            if(resourceTargetGroupId != 0)
            {
                resource.setGroupId(resourceTargetGroupId);
                getUser().setActiveGroup(resourceTargetGroupId);
            }

            if(resourceTargetFolderId != 0)
            {
                resource.setFolderId(resourceTargetFolderId);
            }

            if(resource.getId() == -1)
                resource = getUser().addResource(resource);
            else
            {

                resource.save();
            }

            Resource iconResource = getLearnweb().getResourceManager().getResource(204095);

            resource.setThumbnail0(iconResource.getThumbnail0());
            resource.setThumbnail1(iconResource.getThumbnail1());
            resource.setThumbnail2(iconResource.getThumbnail2());
            resource.setThumbnail3(iconResource.getThumbnail3());
            resource.setThumbnail4(iconResource.getThumbnail4());

            resource.setUrl(getLearnweb().getServerUrl() + "/templates/resources/survey.jsf?resource_id=" + Integer.toString(resource.getId()));
            resource.save();
            getLearnweb().getCreateSurveyManager().createSurveyResource(resource.getId(), resource.getTitle(), resource.getDescription(), resource.getOpenDate(), resource.getCloseDate(), resource.getValidCourses());
            log(Action.adding_resource, resourceTargetGroupId, resource.getId(), "");
            addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());

            UtilBean.getGroupDetailBean().updateResourcesFromSolr();

            resource = new Resource();
            resource.setSource("Internet");
            resource.setLocation("Learnweb");
            resource.setStorageType(Resource.SURVEY_RESOURCE);
            resource.setDeleted(true);

            //resource.setUrl("");
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }
    */
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

            if(null != selectedUploadServices && selectedUploadServices.size() > 0) // the resource has to be uploaded to interweb
            {
                try
                {
                    Resource interwebResource = getUser().getInterweb().upload(resource, selectedUploadServices);
                    if(interwebResource.getLocation().equalsIgnoreCase("YouTube")) // originalSource or source?
                    {
                        getUser().deleteResource(resource);
                        resource = getUser().addResource(interwebResource);
                    }
                }
                catch(IllegalResponseException e)
                {
                    addFatalMessage(e);
                }
            }

            resource.setDeleted(false);
            if(resource.isOfficeResource())
                getFileEditorBean().fillInFileInfo(resource);

            // add resource to a group if selected
            if(resourceTargetGroupId != 0)
            {
                resource.setGroupId(resourceTargetGroupId);
                getUser().setActiveGroup(resourceTargetGroupId);
            }

            if(resourceTargetFolderId != 0)
            {
                resource.setFolderId(resourceTargetFolderId);
            }

            if(resource.getId() == -1 || resource.getUserId() == 0) // a new resource which is not stored in the database yet
                resource = getUser().addResource(resource);
            else
                resource.save();

            // create thumbnails for the resource
            if(!resource.isProcessingStarted() && (resource.getThumbnail2() == null || resource.getThumbnail2().getFileId() == 0 || resource.getType().equals(Resource.ResourceType.video))) {
                new CreateThumbnailThread(resource).start();
            }

            log(Action.adding_resource, resourceTargetGroupId, resource.getId(), "");

            addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());

            UtilBean.getGroupDetailBean().updateResourcesFromSolr();

            resource = new Resource();
            resource.setSource("Internet");
            resource.setLocation("Learnweb");
            resource.setStorageType(Resource.LEARNWEB_RESOURCE);
            resource.setDeleted(true);
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

    public int getResourceTargetGroupId()
    {
        return resourceTargetGroupId;
    }

    public void setResourceTargetGroupId(int resourceTargetGroupId)
    {
        try
        {
            Group group = getLearnweb().getGroupManager().getGroupById(resourceTargetGroupId);
            if(group != null)
            {
                this.targetGroup = group;
                this.resourceTargetGroupId = group.getId();
            }
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public int getResourceTargetFolderId()
    {
        return resourceTargetFolderId;
    }

    public void setResourceTargetFolderId(int resourceTargetFolderId)
    {
        try
        {
            Folder folder = getLearnweb().getGroupManager().getFolder(resourceTargetFolderId);
            if(folder != null)
            {
                this.targetFolder = folder;
                this.resourceTargetFolderId = folder.getId();
            }
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public String getNewUrl()
    {
        return newUrl;
    }

    public void setNewUrl(String newUrl)
    {
        this.newUrl = newUrl;

    }

    /**
     * Returns java script from the embedded code because this can't be loaded thru ajax
     *
     * @return
     */
    public String getEmbeddedSize3()
    {
        if(resource.getEmbeddedSize3() == null)
            return "";

        return Jsoup.clean(resource.getEmbeddedSize3(), UtilBean.getLearnwebBean().getBaseUrl(), Whitelist.basicWithImages().addTags("embed", "object", "param"));
    }

    /*
    // functions and variables for adding resource by get request:
    private String paramUrl;
    private String paramTitle;
    private String paramDescription;
    private String paramSource;
    private String paramType;
    private String paramThumbnail;
    
    public String getParamUrl()
    {
    return paramUrl;
    }
    
    public void setParamUrl(String paramUrl)
    {
    this.paramUrl = paramUrl;
    }
    
    public String getParamTitle()
    {
    return paramTitle;
    }
    
    public void setParamTitle(String paramTitle)
    {
    this.paramTitle = paramTitle;
    }
    
    public String getParamDescription()
    {
    return paramDescription;
    }
    
    public void setParamDescription(String paramDescription)
    {
    this.paramDescription = paramDescription;
    }
    
    public String getParamSource()
    {
    return paramSource;
    }
    
    public void setParamSource(String paramSource)
    {
    this.paramSource = paramSource;
    }
    
    public String getParamType()
    {
    return paramType;
    }
    
    public void setParamType(String paramType)
    {
    this.paramType = paramType;
    }
    
    public String getParamThumbnail()
    {
    return paramThumbnail;
    }
    
    public void setParamThumbnail(String paramThumbnail)
    {
    this.paramThumbnail = paramThumbnail;
    }
    */

    public List<ServiceInformation> getUploadServices()
    {
        return uploadServices;
    }

    public List<String> getSelectedUploadServices()
    {
        return selectedUploadServices;
    }

    public void setSelectedUploadServices(List<String> selectedUploadServices)
    {
        this.selectedUploadServices = selectedUploadServices;
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

        return UtilBean.getLocaleMessage("myResourcesTitle");
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

        /*
        Add resources through get parameter.
        Implemented for collaboration with an Italian software.
        Currently not used.  
         
        if(null != paramUrl || null != paramTitle || null != paramDescription || null != paramSource || null != paramType)
        {
            if(null == paramUrl || paramUrl.length() == 0)
            {
        	addMessage(FacesMessage.SEVERITY_ERROR, "Missing required param: url");
        	return;
            }
            if(null == paramTitle || paramTitle.length() == 0)
            {
        	addMessage(FacesMessage.SEVERITY_ERROR, "Missing required param: title");
        	return;
            }
        
            resource = new Resource();
            resource.setStorageType(Resource.WEB_RESOURCE);
            resource.setUrl(StringHelper.decodeBase64(paramUrl));
            resource.setTitle(StringHelper.decodeBase64(paramTitle));
            resource.setSource("Internet");
            resource.setLocation("Learnweb");
        
            if(null != paramThumbnail && paramThumbnail.length() != 0)
            {
        	String image = "<img src\"" + StringHelper.decodeBase64(paramThumbnail) + "\" />";
        	resource.setEmbeddedSize1Raw(image);
            }
        
            if(null != paramDescription)
        	resource.setDescription(StringHelper.decodeBase64(paramDescription));
        
            if(null != paramSource)
        	resource.setLocation(StringHelper.decodeBase64(paramSource));
        
            if(null != paramType)
        	resource.setType(StringHelper.decodeBase64(paramType));
        
            try
            {
        	addResource();
        
        	String redirect = UtilBean.getLearnwebBean().getContextUrl() + getTemplateDir() + "/resource.jsf?resource_id=" + resource.getId();
        	getFacesContext().getExternalContext().redirect(redirect);
        
            }
            catch(Exception e)
            {
        	addFatalMessage(e);
            }
        }
        */
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

    public static void main(String[] args) throws SQLException, InterruptedException
    {
        /*
        Resource resource = Learnweb.getInstance().getResourceManager().getResource(190236);
        log.debug(resource);
        
        new CreateThumbnailThread(resource).start();
        
        Thread.sleep(99999999);
        */
    }

    public FileEditorBean getFileEditorBean()
    {
        return fileEditorBean;
    }

    public void setFileEditorBean(FileEditorBean fileEditorBean)
    {
        this.fileEditorBean = fileEditorBean;
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
                resource.setProcessingStarted(true);
                rpm.processResource(resource);
                resource.setProcessingStarted(false);
                resource.save();
            }
            catch(Exception e)
            {
                log.error("Error in CreateThumbnailThread " + e);
            }
        }

    }

}

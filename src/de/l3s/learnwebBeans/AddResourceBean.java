package de.l3s.learnwebBeans;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.validator.ValidatorException;

import org.apache.commons.codec.DecoderException;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import de.l3s.interwebj.AuthorizationInformation.ServiceInformation;
import de.l3s.interwebj.IllegalResponseException;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Resource.OnlineStatus;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
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

    public void doNothing(AjaxBehaviorEvent obj)
    {
	resource.setStorageType(Resource.WEB_RESOURCE);
    }

    public AddResourceBean()
    {
	resource = new Resource();
	resource.setSource("Internet");
	resource.setLocation("Learnweb");
	resource.setStorageType(Resource.FILE_RESOURCE);
	resource.setDeleted(true); // hide the resource from the frontend until it is finally saved
    }

    /*
    public void validateUrl(FacesContext context, UIComponent component, Object value) throws ValidatorException
    {
    
    String urlString = ((String) value).trim();
    
    log.debug("validate Url: " + urlString);
    
    if(!urlString.startsWith("http"))
        urlString = "http://" + urlString;
    
    
        throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "invalid_url"));
    
    }
    
    public void handleUrlChange(AjaxBehaviorEvent event) throws IOException
    {
    log.debug("handleUrlChange");
    log.debug(resource.getUrl());
    
    if(!resource.getUrl().startsWith("http"))
        resource.setUrl("http://" + resource.getUrl());
    
    try
    {
        new URL(resource.getUrl());
    
    }
    catch(MalformedURLException e)
    {
        throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "invalid_url"));
    }
    
    URL url = new URL(resource.getUrl());
    URLExtractor ue = new URLExtractor();
    URLInfo urlinfo = ue.extract(url);
    resource.setTitle(urlinfo.getTitle());
    resource.setDescription(urlinfo.getDescription());
    ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();
    URL img = new URL(urlinfo.getImage());
    try
    {
        rpm.processImage(resource, img.openStream());
    }
    catch(SQLException e)
    {
        resource.setEmbeddedSize1Raw("<img src=\"" + urlinfo.getImage() + "\" width=\"100\" height=\"100\" />");
        e.printStackTrace();
    }
    
    log.debug(resource.getTitle());
    log.debug(resource.getEmbeddedSize1());
    log.debug(resource.getDescription());
    resource.prepareEmbeddedCodes();
    }
    */
    public void clearForm()
    {
	resource = new Resource();
	formStep = 1;
    }

    public void handleFileUpload(FileUploadEvent event)
    {
	try
	{
	    log.debug("Handle File upload");

	    resource.setStorageType(Resource.FILE_RESOURCE);
	    resource.setSource("Desktop");
	    resource.setLocation("Learnweb");
	    resource.setDeleted(true);

	    UploadedFile uploadedFile = event.getFile();

	    ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();

	    log.debug("Get the mime type and extract text if possible");
	    FileInfo info = rpm.getFileInfo(uploadedFile.getInputstream(), uploadedFile.getFileName());

	    log.debug("Create thumbnails");
	    rpm.processFile(resource, uploadedFile.getInputstream(), info);

	    User user = getUser();
	    //resource = user.addResource(resource);	
	    resource.prepareEmbeddedCodes();
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

	    nextStep();
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	}
    }

    public void validateUrl(FacesContext context, UIComponent comp, Object value) throws ValidatorException
    {
	String urlValue = value.toString().trim();
	log.debug("validateUrl: " + urlValue);

	if(!urlValue.startsWith("http"))
	{
	    urlValue = "http://" + urlValue;
	}

	try
	{
	    URL url = new URL(urlValue);
	    URLConnection conn = url.openConnection();
	    conn.connect();
	}
	catch(IOException e)
	{
	    // the connection couldn't be established
	    throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "invalid_url"));
	}
    }

    public Resource getResource()
    {
	return resource;
    }

    public String getStorageType()
    {
	if(resource.getStorageType() == Resource.FILE_RESOURCE)
	    return UtilBean.getLocaleMessage("file");
	else if(resource.getStorageType() == Resource.WEB_RESOURCE)
	    return UtilBean.getLocaleMessage("web");
	else
	    return UtilBean.getLocaleMessage("unknown_storage_type");
    }

    public void addResource()
    {
	try
	{
	    log.debug("addResource; id=" + resource.getId() + "; title=" + resource.getTitle());

	    if(resource.getStorageType() == Resource.FILE_RESOURCE && null == resource.getFile(4))
	    {
		addGrowl(FacesMessage.SEVERITY_ERROR, "Select a file first");
		return;
	    }
	    if(resource.getStorageType() == Resource.WEB_RESOURCE && (resource.getType() == null || resource.getType().isEmpty()))
	    {
		if(!resource.getUrl().startsWith("http"))
		    resource.setUrl("http://" + resource.getUrl());

		resource.setType("text");

		new CreateThumbnailThread(resource).start();
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

	    if(resource.getId() == -1) // a new resource which is not stored in the database yet
		resource = getUser().addResource(resource);
	    else
		resource.save();

	    log(Action.adding_resource, resourceTargetGroupId, resource.getId(), "");

	    addMessage(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());

	    UtilBean.getGroupDetailBean().updateResourcesFromSolr();

	    resource = new Resource();
	    resource.setSource("Internet");
	    resource.setLocation("Learnweb");
	    resource.setStorageType(Resource.FILE_RESOURCE);
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
	resource.setStorageType(Resource.FILE_RESOURCE);
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
		this.resourceTargetFolderId = folder.getFolderId();
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
    }

    public static void main(String[] args) throws SQLException, InterruptedException
    {
	Resource resource = Learnweb.getInstance().getResourceManager().getResource(190236);
	log.debug(resource);

	new CreateThumbnailThread(resource).start();

	Thread.sleep(99999999);
    }

    public static class CreateThumbnailThread extends Thread
    {

	private Resource resource;

	public CreateThumbnailThread(Resource resource)
	{
	    this.resource = resource;
	}

	@Override
	public void run()
	{

	    try
	    {
		log.debug("Create thumbnail for resource " + resource.getId());
		ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
		log.debug("url " + resource.getUrl());
		log.debug("max url " + resource.getMaxImageUrl());
		log.debug("source " + resource.getSource());

		if(resource.getType().equalsIgnoreCase("text") || resource.getType().equalsIgnoreCase("unknown"))
		{
		    FileInfo info = new FileInspector().inspect(FileInspector.openStream(resource.getUrl()), "unknown");

		    if(info.getMimeType().equals("text/html") || info.getMimeType().equals("text/plain") || info.getMimeType().equals("application/xhtml+xml") || info.getMimeType().equals("application/octet-stream") || info.getMimeType().equals("blog-post")
			    || info.getMimeType().equals("application/x-gzip"))
		    {
			resource.setMachineDescription(info.getTextContent());

			rpm.processWebsite(resource);
			resource.setOnlineStatus(OnlineStatus.ONLINE);
			if(resource.getSource() == null)
			    resource.setSource("Internet");

			resource.save();

		    }
		    else if(info.getMimeType().equals("application/pdf"))
		    {
			log.debug("process " + info.getMimeType());
			resource.setMachineDescription(info.getTextContent());

			rpm.processFile(resource, FileInspector.openStream(resource.getUrl()), info);
			resource.save();
		    }
		    else if(info.getMimeType().startsWith("image/"))
		    {
			rpm.processImage(resource, FileInspector.openStream(resource.getUrl()));
			resource.setFormat(info.getMimeType());
			resource.setType("Image");
			resource.save();
		    }
		    else
		    {
			log.error("Can't create thumbnail for mimetype: " + info.getMimeType());
			return;
		    }
		}
		else if(resource.getStorageType() == Resource.WEB_RESOURCE && resource.getMaxImageUrl() != null && resource.getMaxImageUrl().length() > 4)
		{
		    log.debug("Create Thumbnails from: " + resource.getMaxImageUrl());
		    rpm.processImage(resource, FileInspector.openStream(resource.getMaxImageUrl()));
		    resource.save();
		}
		else
		{
		    log.error("Can't create thumbnail. Don't know how to handle resource " + resource.getId());
		    return;
		}
		log.debug("Create thumbnail for resource " + resource.getId() + "; Done");
	    }
	    catch(Exception e)
	    {
		log.error(e);

		resource.setOnlineStatus(OnlineStatus.UNKNOWN); // most probably offline
		try
		{
		    resource.save();
		}
		catch(SQLException e1)
		{
		    log.fatal("can't save resource: " + resource.getId(), e1);
		}
	    }
	}

    }

}

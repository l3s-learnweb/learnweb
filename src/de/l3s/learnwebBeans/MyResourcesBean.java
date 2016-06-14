package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.GroupDetailBean.RPAction;

@ManagedBean
@ViewScoped
public class MyResourcesBean extends ApplicationBean implements Serializable
{
    private final static long serialVersionUID = 5680533799976460331L;
    private final static Logger log = Logger.getLogger(MyResourcesBean.class);

    private List<Resource> resources;
    private List<Resource> resourcesAll;

    private int selectedResourceTargetGroupId;

    private RPAction rightPanelAction = null;
    private Boolean reloadLogs = false;

    private List<Resource> resourcesText = new LinkedList<Resource>();
    private Resource clickedResource;
    private Folder clickedFolder;
    private Folder selectedFolder;
    private String mode = "everything";

    private boolean rootFolder = true;
    private List<Folder> breadcrumb;

    public MyResourcesBean() throws SQLException
    {
	if(getUser() == null) // not logged in
	    return;

	//loadResources();
	breadcrumb = new ArrayList<Folder>();
	clickedResource = new Resource();
    }

    public boolean canDeleteTag(Object tagO) throws SQLException
    {
	if(!(tagO instanceof Tag))
	    return false;

	User user = getUser();
	if(null == user)
	    return false;
	if(user.isAdmin() || user.isModerator())
	    return true;

	Tag tag = (Tag) tagO;
	User owner = clickedResource.getTags().getElementOwner(tag);
	if(user.equals(owner))
	    return true;
	return false;
    }

    /*
    public void addSelectedResource()
    {
    try
    {
        //Resource res = getUser().addResource(clickedResource.clone());
        getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId).addResource(clickedResource, getUser());
        //getLearnweb().getResourceManager().addResourceToGroup(res, getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId), getUser());
    
        log(Action.adding_resource, clickedResource.getId(), selectedResourceTargetGroupId + "");
    
        addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", clickedResource.getTitle());
    
    }
    catch(SQLException e)
    {
        addFatalMessage(e);
    }
    } */

    public void addSelectedResourceToGroup() throws SQLException
    {
	User user = getUser();
	if(null == user)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}

	Resource resource = clickedResource;

	Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(selectedResourceTargetGroupId);

	// add resource to a group if selected
	if(targetGroup != null && targetGroup.getId() > 0)
	{

	    if(resource.getGroupId() == 0)
	    {
		resource.setGroupId(targetGroup.getId());
		resource.save();
	    }
	    else
	    {
		Resource newResource = resource.clone();
		newResource.setGroupId(targetGroup.getId());
		getUser().addResource(newResource);
	    }

	    user.setActiveGroup(targetGroup.getId());
	    addGrowl(FacesMessage.SEVERITY_INFO, "addedResourceToGroup", resource.getTitle(), targetGroup.getTitle());
	}

	log(Action.adding_resource, selectedResourceTargetGroupId, resource.getId(), "");

    }

    public void loadResources() throws SQLException
    {
	resourcesAll = getUser().getResources();
	resourcesText.clear();
	setMode(mode);
    }

    public void editClickedResource() throws SQLException
    {
	clickedResource.save();
    }

    public void onSelect() throws NumberFormatException, SQLException
    {
	ResourceManager rm = Learnweb.getInstance().getResourceManager();
	Resource temp;

	temp = rm.getResource(Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id")));
	setClickedResource(temp);

    }

    public List<Resource> getResources()
    {
	return resources;
    }

    public String getMode()
    {
	return mode;
    }

    public void setMode(String mode)
    {
	this.mode = mode;
	if(mode.equals("everything"))
	    resources = resourcesAll;
	else if(mode.equals("text"))
	    resources = resourcesText;
    }

    public void deleteResource() throws SQLException
    {
	getUser().deleteResource(clickedResource);
	addGrowl(FacesMessage.SEVERITY_INFO, "resource_deleted");
	log(Action.deleting_resource, 0, clickedResource.getId(), clickedResource.getTitle());
	resources.remove(clickedResource);
	clickedResource = new Resource();
	//loadResources();
    }

    public Resource getClickedResource()
    {
	return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
	if(rightPanelAction != RPAction.editResource || this.clickedResource != clickedResource)
	{
	    rightPanelAction = RPAction.viewResource;
	    this.clickedResource = clickedResource;
	}
    }

    public Folder getClickedFolder()
    {
	return clickedFolder;
    }

    private void deleteEntriesFromBreadcrumb()
    {
	ListIterator<Folder> iter = breadcrumb.listIterator();
	boolean delete = false;
	while(iter.hasNext())
	{
	    Folder f = iter.next();
	    if(delete)
	    {
		iter.remove();
		continue;
	    }
	    if(f.equals(clickedFolder))
		delete = true;
	}
    }

    public void setClickedFolder(Folder clickedFolder) throws SQLException
    {
	if(this.clickedFolder != null && this.clickedFolder.equals(clickedFolder))
	{
	    this.rightPanelAction = RPAction.none;
	    setSelectedFolder(this.clickedFolder);
	    this.rootFolder = false;
	    if(breadcrumb.contains(clickedFolder))
		deleteEntriesFromBreadcrumb();
	    else
		breadcrumb.add(selectedFolder);
	}
	else
	{
	    this.rightPanelAction = RPAction.viewFolder;
	    this.clickedFolder = clickedFolder;
	    setSelectedFolder(this.clickedFolder);
	    this.rootFolder = false;
	    if(breadcrumb.contains(clickedFolder))
		deleteEntriesFromBreadcrumb();
	    else
		breadcrumb.add(selectedFolder);
	}
	resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(clickedFolder.getGroupId(), clickedFolder.getFolderId(), getUser().getId());
    }

    public int getSelectedResourceTargetGroupId()
    {
	return selectedResourceTargetGroupId;
    }

    public void setSelectedResourceTargetGroupId(int selectedResourceTargetGroupId)
    {
	this.selectedResourceTargetGroupId = selectedResourceTargetGroupId;
    }

    public RPAction getRightPanelAction()
    {
	return rightPanelAction;
    }

    public void setRightPanelAction(RPAction rightPanelAction)
    {
	this.rightPanelAction = rightPanelAction;
    }

    public void setRightPanelAction(String value)
    {
	try
	{
	    this.rightPanelAction = RPAction.valueOf(value);
	}
	catch(Exception e)
	{
	    this.rightPanelAction = null;
	    log.debug(e);
	}
    }

    public Boolean getReloadLogs()
    {
	return reloadLogs;
    }

    public void setReloadLogs(Boolean reloadLogs)
    {
	this.reloadLogs = reloadLogs;
    }

    public List<Resource> getResourcesAll()
    {
	return resourcesAll;
    }

    public void setResourcesAll(List<Resource> resourcesAll)
    {
	this.resourcesAll = resourcesAll;
    }

    public List<Folder> getFolders() throws SQLException
    {
	if(rootFolder)
	{
	    LinkedList<Folder> folders = new LinkedList<Folder>();
	    folders.add(new Folder(0, 0, "My Private Resources"));
	    folders.addAll(Learnweb.getInstance().getGroupManager().getGroupsForMyResources(getUser().getId()));
	    return folders;
	}
	else
	    return Learnweb.getInstance().getGroupManager().getFolders(getClickedFolder().getGroupId(), getSelectedFolderId());
    }

    public Folder getSelectedFolder()
    {
	return selectedFolder;
    }

    public void setSelectedFolder(Folder folder)
    {
	if(folder != null)
	{
	    this.selectedFolder = folder;
	    this.clickedFolder = selectedFolder;
	    UtilBean.getAddResourceBean().setResourceTargetFolderId(getSelectedFolderId());
	}
    }

    public int getSelectedFolderId()
    {
	return selectedFolder == null || selectedFolder.getFolderId() <= 0 ? 0 : selectedFolder.getFolderId();
    }

    public void setSelectedFolderId(int folderId) throws SQLException
    {
	if(folderId > 0)
	{
	    selectedFolder = getLearnweb().getGroupManager().getFolder(folderId);
	}
    }

    public List<Folder> getBreadcrumb()
    {
	return breadcrumb;
    }
}

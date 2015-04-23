package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class MyResourcesBean extends ApplicationBean implements Serializable
{
    private final static long serialVersionUID = 5680533799976460331L;
    private final static Logger log = Logger.getLogger(MyResourcesBean.class);

    private List<Resource> resources;
    private List<Resource> resourcesAll;
    private String newComment;

    private int selectedResourceTargetGroupId;
    private Boolean newResourceClicked = false;
    private Boolean editResourceClicked = false;
    private Tag selectedTag;
    private String tagName;
    private Comment clickedComment;
    private Boolean reloadLogs = false;

    private List<Resource> resourcesText = new LinkedList<Resource>();
    private List<Resource> resourcesMultimedia = new LinkedList<Resource>();
    private Resource clickedResource;
    private String mode = "everything";
    private int numberOfColumns = 3;

    public MyResourcesBean() throws SQLException
    {
	if(getUser() == null) // not logged in
	    return;

	loadResources();
	Resource temp = new Resource();
	clickedResource = temp;
    }

    public void updateColumns()
    {
	numberOfColumns = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("cols"));

	if(numberOfColumns < 1)
	{
	    log.warn("Tried to set invalid numberOfColumns: " + numberOfColumns);
	    numberOfColumns = 3;
	}
    }

    public int getNumberOfColumns()
    {
	return numberOfColumns;
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

    public void addSelectedResourceLink() throws SQLException
    {
	User user = getUser();
	if(null == user)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}

	Resource newResource = clickedResource;

	// add resource to a group if selected
	if(selectedResourceTargetGroupId != 0)
	{
	    getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId).addResource(newResource, getUser());
	    user.setActiveGroup(selectedResourceTargetGroupId);

	    log(Action.adding_resource, newResource.getId(), selectedResourceTargetGroupId + "");

	    addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", newResource.getTitle());
	}

    }

    public void onDeleteTag()
    {
	try
	{
	    clickedResource.deleteTag(selectedTag);
	    addMessage(FacesMessage.SEVERITY_INFO, "tag_deleted");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public String addTag()
    {
	if(null == getUser())
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return null;
	}

	if(tagName == null || tagName.length() == 0)
	    return null;

	try
	{
	    clickedResource.addTag(tagName, getUser());
	    addGrowl(FacesMessage.SEVERITY_INFO, "tag_added");
	    log(Action.tagging_resource, clickedResource.getId(), tagName);
	    tagName = ""; // clear tag input field 
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_ERROR, "fatal_error");
	}
	return null;
    }

    public void loadResources() throws SQLException
    {
	resourcesAll = getUser().getResources();
	resourcesMultimedia.clear();
	resourcesText.clear();

	/*
	for(Resource resource : resourcesAll)
	{
	    if(resource.isMultimedia())
		resourcesMultimedia.add(resource);
	    else
		resourcesText.add(resource);
	}*/
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

    public void addComment() throws Exception
    {
	clickedResource.addComment(newComment, getUser());
	newComment = "";
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
	else if(mode.equals("multimedia"))
	    resources = resourcesMultimedia;
    }

    public void deleteResource() throws SQLException
    {
	getUser().deleteResource(clickedResource);
	addGrowl(FacesMessage.SEVERITY_INFO, "resource_deleted");
	log(Action.deleting_resource, clickedResource.getId(), clickedResource.getTitle());
	clickedResource = new Resource();
	loadResources();
    }

    public Resource getClickedResource()
    {
	return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
	if(!editResourceClicked || this.clickedResource != clickedResource)
	{
	    editResourceClicked = false;
	    this.clickedResource = clickedResource;
	}
	newResourceClicked = false;
    }

    public int getSelectedResourceTargetGroupId()
    {
	return selectedResourceTargetGroupId;
    }

    public void setSelectedResourceTargetGroupId(int selectedResourceTargetGroupId)
    {
	this.selectedResourceTargetGroupId = selectedResourceTargetGroupId;
    }

    public Boolean getNewResourceClicked()
    {
	return newResourceClicked;
    }

    public void setNewResourceClicked(Boolean newResourceClicked)
    {
	editResourceClicked = false;
	this.newResourceClicked = newResourceClicked;
    }

    public Boolean getEditResourceClicked()
    {
	return editResourceClicked;
    }

    public void setEditResourceClicked(Boolean editResourceClicked)
    {
	newResourceClicked = false;
	this.editResourceClicked = editResourceClicked;
    }

    public Tag getSelectedTag()
    {
	return selectedTag;
    }

    public void setSelectedTag(Tag selectedTag)
    {
	this.selectedTag = selectedTag;
    }

    public void onEditComment()
    {
	try
	{
	    getLearnweb().getResourceManager().saveComment(clickedComment);
	    addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public void onDeleteComment()
    {
	try
	{
	    clickedResource.deleteComment(clickedComment);
	    addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
	    log(Action.deleting_comment, clickedComment.getResourceId(), clickedComment.getId() + "");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public boolean canEditComment(Object commentO) throws Exception
    {
	if(!(commentO instanceof Comment))
	    return false;

	User user = getUser();
	if(null == user)// || true)
	    return false;
	if(user.isAdmin() || user.isModerator())
	    return true;

	Comment comment = (Comment) commentO;
	User owner = comment.getUser();
	if(user.equals(owner))
	    return true;
	return false;
    }

    public Boolean getReloadLogs()
    {
	return reloadLogs;
    }

    public void setReloadLogs(Boolean reloadLogs)
    {
	this.reloadLogs = reloadLogs;
    }

    public Comment getClickedComment()
    {
	return clickedComment;
    }

    public void setClickedComment(Comment clickedComment)
    {
	this.clickedComment = clickedComment;
    }

    public String getTagName()
    {
	return tagName;
    }

    public void setTagName(String tagName)
    {
	this.tagName = tagName;
    }

    public String getNewComment()
    {
	return newComment;
    }

    public void setNewComment(String newComment)
    {
	this.newComment = newComment;
    }

    public List<Resource> getResourcesAll()
    {
	return resourcesAll;
    }

    public void setResourcesAll(List<Resource> resourcesAll)
    {
	this.resourcesAll = resourcesAll;
    }

    public void archiveCurrentVersion()
    {
	getLearnweb().getArchiveUrlManager().addResourceToArchive(clickedResource);
	addGrowl(FacesMessage.SEVERITY_INFO, "addedToArchiveQueue");
    }
}

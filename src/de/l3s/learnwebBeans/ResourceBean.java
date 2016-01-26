package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;
import org.primefaces.event.RateEvent;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class ResourceBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -8834191417574642115L;
    private final static Logger log = Logger.getLogger(ResourceBean.class);
    private int id;
    private Resource resource;
    private String tagName;
    private String commentText;
    private int resourceTargetGroupId;

    private boolean isStarRatingHidden = false;
    private boolean isThumbRatingHidden = false;

    public void loadResource()
    {
	if(null != resource) // resource already loaded
	    return;

	if(id == 0) // no or invalid resource_id
	{

	    id = getParameterInt("resource_id");

	    if(id == 0)
	    {
		addMessage(FacesMessage.SEVERITY_FATAL, "invalid or no resource_id parameter");
		return;
	    }
	}

	try
	{
	    resource = getLearnweb().getResourceManager().getResource(getId());

	    if(null == resource)
	    {
		addMessage(FacesMessage.SEVERITY_FATAL, "Invalid or no resource_id parameter. Maybe the resource was deleted");
		return;
	    }
	    /*
	    	    User user = getUser();
	    	    Organisation org;
	    	    if(null == user) //  not logged in, get organisation "Public"
	    		org = getLearnweb().getOrganisationManager().getOrganisationByTitle("Public");
	    	    else
	    		org = user.getOrganisation();

	    	    isStarRatingHidden = org.getOption(Option.Resource_Hide_Star_rating);
	    	    isThumbRatingHidden = org.getOption(Option.Resource_Hide_Thumb_rating);
	    	    */

	    isStarRatingHidden = false;
	    isThumbRatingHidden = true;

	}
	catch(Exception e)
	{
	    log.error("can't load resource: " + id, e);
	    addFatalMessage(e);
	}
    }

    public void preRenderView(ComponentSystemEvent e)
    {
	loadResource();

    }

    public void handleRate(RateEvent rateEvent)
    {
	if(null == getUser())
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}

	loadResource();

	try
	{
	    if(isRatedByUser())
	    {
		addGrowl(FacesMessage.SEVERITY_FATAL, "You have already rated this resource");
		return;
	    }

	    resource.rate((Integer) rateEvent.getRating(), getUser());
	}
	catch(Exception e)
	{
	    addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
	    e.printStackTrace();
	    return;
	}

	log(Action.rating_resource, resource.getId());

	addGrowl(FacesMessage.SEVERITY_INFO, "resource_rated");
    }

    private void handleThumbRating(int direction)
    {
	loadResource();

	if(null == getUser())
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}

	try
	{
	    if(isThumbRatedByUser())
	    {
		addGrowl(FacesMessage.SEVERITY_FATAL, "You have already rated this resource");
		return;
	    }

	    resource.thumbRate(getUser(), direction);
	}
	catch(Exception e)
	{
	    addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
	    e.printStackTrace();
	    return;
	}

	log(Action.thumb_rating_resource, resource.getId());

	addGrowl(FacesMessage.SEVERITY_INFO, "resource_rated");
    }

    public void onThumbUp()
    {
	handleThumbRating(1);
    }

    public void onThumbDown()
    {
	handleThumbRating(-1);
    }

    public String addTag()
    {
	if(null == getUser())
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return null;
	}

	loadResource();

	if(tagName == null || tagName.length() == 0)
	    return null;

	try
	{
	    resource.addTag(tagName, getUser());
	    addGrowl(FacesMessage.SEVERITY_INFO, "tag_added");
	    log(Action.tagging_resource, resource.getId(), tagName);
	    tagName = ""; // clear tag input field 
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_ERROR, "fatal_error");
	}
	return null;
    }

    public void addComment()
    {
	if(null == getUser())
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}

	loadResource();

	if(commentText == null || commentText.length() == 0)
	    return;

	try
	{
	    Comment comment = resource.addComment(commentText, getUser());
	    log(Action.commenting_resource, resource.getId(), comment.getId() + "");
	    addGrowl(FacesMessage.SEVERITY_INFO, "comment_added");
	    commentText = "";
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public boolean isRatedByUser() throws Exception
    {
	if(getUser() == null || null == resource)
	    return false;

	return resource.isRatedByUser(getUser().getId());
    }

    public boolean isThumbRatedByUser() throws SQLException
    {
	if(getUser() == null || null == resource)
	    return false;

	return resource.isThumbRatedByUser(getUser().getId());
    }

    public String getTagName()
    {
	return tagName;
    }

    public void setTagName(String tagName)
    {
	this.tagName = tagName;
    }

    public int getId()
    {
	return id;
    }

    public void setId(int id)
    {
	this.id = id;
    }

    public Resource getResource()
    {
	return resource;
    }

    public void setResource(Resource resource)
    {
	this.resource = resource;
    }

    public String getCommentText()
    {
	return commentText;
    }

    public void setCommentText(String commentText)
    {
	this.commentText = commentText;
    }

    // resource editing

    public String saveResource()
    {
	log.debug("saveResource()");
	if(null == getUser())
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return null;
	}
	log.debug("saveResource() 2");
	try
	{
	    //loadResource();
	    log.debug("drin" + resource.getTitle());
	    resource.save();

	    log(Action.edit_resource, resource.getId());

	    addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}

	return null;
    }

    public int getResourceTargetGroupId()
    {
	return resourceTargetGroupId;
    }

    public void setResourceTargetGroupId(int resourceTargetGroupId)
    {
	this.resourceTargetGroupId = resourceTargetGroupId;
    }

    public boolean isStarRatingHidden()
    {
	return isStarRatingHidden;
    }

    public boolean isThumbRatingHidden()
    {
	return isThumbRatingHidden;
    }

    public boolean canDeleteTag(Object tagO) throws SQLException
    {
	if(!(tagO instanceof Tag))
	    return false;

	User user = getUser();
	if(null == user)// || true)
	    return false;
	if(user.isAdmin() || user.isModerator())
	    return true;

	Tag tag = (Tag) tagO;
	User owner = resource.getTags().getElementOwner(tag);
	if(user.equals(owner))
	    return true;
	return false;
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

    public void onDeleteTag(Tag tag)
    {
	try
	{
	    resource.deleteTag(tag);
	    addMessage(FacesMessage.SEVERITY_INFO, "tag_deleted");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public void onDeleteComment(Comment comment)
    {
	try
	{
	    resource.deleteComment(comment);
	    addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
	    log(Action.deleting_comment, comment.getResourceId(), comment.getId() + "");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public void onEditComment(Comment comment)
    {
	try
	{
	    getLearnweb().getResourceManager().saveComment(comment);
	    addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }
}

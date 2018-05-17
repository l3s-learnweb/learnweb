package de.l3s.learnweb.beans;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;

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

    public void loadResource()
    {
        if(null != resource) // resource already loaded
            return;

        if(id == 0) // no or invalid resource_id
        {
            // safeId is created to avoid NullPointException
            Integer safeId = getParameterInt("resource_id");

            if(safeId == null || safeId == 0)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "invalid or no resource_id parameter");
                return;
            }

            id = safeId;
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
            log(Action.tagging_resource, resource.getGroupId(), resource.getId(), tagName);
            tagName = ""; // clear tag input field
        }
        catch(Exception e)
        {
            log.error("unhandled error", e);
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
            log(Action.commenting_resource, resource.getGroupId(), resource.getId(), comment.getId());
            addGrowl(FacesMessage.SEVERITY_INFO, "comment_added");
            commentText = "";
        }
        catch(Exception e)
        {
            log.error("unhandled error", e);
            addGrowl(FacesMessage.SEVERITY_FATAL, "fatal_error");
        }
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

            log(Action.edit_resource, resource.getGroupId(), resource.getId());

            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }
        catch(Exception e)
        {
            log.error("unhandled error", e);
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
}

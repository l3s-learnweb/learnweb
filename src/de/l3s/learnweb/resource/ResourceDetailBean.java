package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.PrimeFaces;
import org.primefaces.event.RateEvent;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.User;

// TODO Oleh: rename to resourceAnnotationBean or merge with RightPaneBean (ResourcePaneBean)
@Named
@ViewScoped
public class ResourceDetailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 4911923763255682055L;
    private static final Logger log = Logger.getLogger(ResourceDetailBean.class);

    private static final String hypothesisProxy = "https://via.hypothes.is/";

    private Tag selectedTag;
    private String tagName;
    private Comment clickedComment;
    private String newComment;

    @Inject
    private RightPaneBean rightPaneBean;

    @SuppressWarnings("unused")
    public void setStarRatingRounded(int value)
    {
        // dummy method, is required by p:rating
    }

    public void archiveCurrentVersion()
    {
        boolean addToQueue = true;
        if(rightPaneBean.getClickedResource().getArchiveUrls().size() > 0)
        {
            long timeDifference = (new Date().getTime() - rightPaneBean.getClickedResource().getArchiveUrls().getLast().getTimestamp().getTime()) / 1000;
            addToQueue = timeDifference > 300;
        }

        if(addToQueue)
        {
            String response = getLearnweb().getArchiveUrlManager().addResourceToArchive(rightPaneBean.getClickedResource());
            if(response.equalsIgnoreCase("archive_success"))
                addGrowl(FacesMessage.SEVERITY_INFO, "addedToArchiveQueue");
            else if(response.equalsIgnoreCase("robots_error"))
                addGrowl(FacesMessage.SEVERITY_ERROR, "archiveRobotsMessage");
            else if(response.equalsIgnoreCase("generic_error"))
                addGrowl(FacesMessage.SEVERITY_ERROR, "archiveErrorMessage");
        }
        else
            addGrowl(FacesMessage.SEVERITY_INFO, "archiveWaitMessage");

    }

    public void onDeleteTag()
    {
        try
        {
            rightPaneBean.getClickedResource().deleteTag(selectedTag);
            addMessage(FacesMessage.SEVERITY_INFO, "tag_deleted");
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public String addTag()
    {
        if(null == getUser())
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return null;
        }

        //Limit number of spaces in a tag = 3

        if((StringUtils.countMatches(tagName, " ") > 3) || tagName.contains(",") || tagName.contains("#") || (tagName.length() > 50))
        {
            showTagWarningMessage();

            return null;
        }

        if(tagName == null || tagName.length() == 0)
            return null;

        try
        {
            Resource resource = rightPaneBean.getClickedResource();
            resource.addTag(tagName, getUser());
            addGrowl(FacesMessage.SEVERITY_INFO, "tag_added");
            log(Action.tagging_resource, resource.getGroupId(), resource.getId(), tagName);
            tagName = ""; // clear tag input field
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
        return null;
    }

    /**
     * Recreates the thumbnails of the selected resource
     *
     * @throws SQLException
     */
    public void onUpdateThumbnail() throws SQLException
    {
        try
        {
            User user = getUser();
            if(user == null || !user.isAdmin())
                return;

            // first delete old thumbnails
            FileManager fileManager = getLearnweb().getFileManager();
            Collection<File> files = rightPaneBean.getClickedResource().getFiles().values();
            for(File file : files)
            {
                if(file.getType() == File.TYPE.THUMBNAIL_LARGE || file.getType() == File.TYPE.THUMBNAIL_MEDIUM || file.getType() == File.TYPE.THUMBNAIL_SMALL || file.getType() == File.TYPE.THUMBNAIL_SQUARED || file.getType() == File.TYPE.THUMBNAIL_VERY_SMALL) // number 4 is reserved for the source file
                {
                    log.debug("Delete " + file.getName());
                    fileManager.delete(file);
                }
            }

            ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();
            rpm.processResource(rightPaneBean.getClickedResource());

            rightPaneBean.getClickedResource().save();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    private void showTagWarningMessage() // TODO rishita refactor
    {
        String title = getLocaleMessage("incorrect_tags");

        if(tagName.contains("#"))
        {
            String newTags = tagName.replaceAll("#", " ");
            int countTags = tagName.trim().length() - tagName.trim().replaceAll("#", "").length();
            FacesMessage message = null;
            String text = getLocaleMessage("tags_hashtag");
            if(countTags > 1) //??????
                message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text + newTags);
            else
                message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text + newTags);
            PrimeFaces.current().dialog().showMessageDynamic(message); // duplicate
        }
        else if(tagName.contains(","))
        {
            String text = getLocaleMessage("tags_specialCharacter");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text); // duplicate
            PrimeFaces.current().dialog().showMessageDynamic(message); // duplicate
        }
        else if((StringUtils.countMatches(tagName, " ") > 3))
        {
            String text = getLocaleMessage("tags_spaces");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text); // duplicate
            PrimeFaces.current().dialog().showMessageDynamic(message); // duplicate
        }
        else
        {
            String text = getLocaleMessage("tags_tooLong");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text); // duplicate
            PrimeFaces.current().dialog().showMessageDynamic(message); // duplicate
        }
    }

    public boolean canEditComment(Object commentO) throws SQLException
    {
        if(!(commentO instanceof Comment))
            return false;

        User user = getUser();
        if(null == user)
            return false;
        if(user.isAdmin() || user.isModerator())
            return true;

        Comment comment = (Comment) commentO;
        User owner = comment.getUser();
        if(user.equals(owner))
            return true;
        return false;
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
        User owner = rightPaneBean.getClickedResource().getTags().getElementOwner(tag);
        if(user.equals(owner))
            return true;
        return false;
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
            addErrorMessage(e);
        }
    }

    public void onDeleteComment()
    {
        try
        {
            rightPaneBean.getClickedResource().deleteComment(clickedComment);
            addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
            log(Action.deleting_comment, rightPaneBean.getClickedResource().getGroupId(), clickedComment.getResourceId(), clickedComment.getId());
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void addComment()
    {
        try
        {
            Comment comment = rightPaneBean.getClickedResource().addComment(newComment, getUser());
            log(Action.commenting_resource, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), comment.getId());
            addGrowl(FacesMessage.SEVERITY_INFO, "comment_added");
            newComment = "";
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void setResourceThumbnail()
    {
        try
        {
            String archiveUrl = getParameter("archive_url");
            ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
            ResourceMetadataExtractor rme = Learnweb.getInstance().getResourceMetadataExtractor();

            FileManager fileManager = getLearnweb().getFileManager();
            Collection<File> files = rightPaneBean.getClickedResource().getFiles().values();
            for(File file : files)
            {
                if(file.getType() == File.TYPE.THUMBNAIL_LARGE || file.getType() == File.TYPE.THUMBNAIL_MEDIUM || file.getType() == File.TYPE.THUMBNAIL_SMALL || file.getType() == File.TYPE.THUMBNAIL_SQUARED || file.getType() == File.TYPE.THUMBNAIL_VERY_SMALL) // number 4 is reserved for the source file
                {
                    log.debug("Delete " + file.getName());
                    fileManager.delete(file);
                }
            }

            //Getting mime type
            FileInspector.FileInfo info = rme.getFileInfo(FileInspector.openStream(archiveUrl), rightPaneBean.getClickedResource().getFileName());
            String type = info.getMimeType().substring(0, info.getMimeType().indexOf("/"));
            if(type.equals("application"))
                type = info.getMimeType().substring(info.getMimeType().indexOf("/") + 1);

            if(type.equalsIgnoreCase("pdf"))
            {
                rpm.processPdf(rightPaneBean.getClickedResource(), FileInspector.openStream(archiveUrl));
            }
            else
                rpm.processArchivedVersion(rightPaneBean.getClickedResource(), archiveUrl);

            rightPaneBean.getClickedResource().save();
            log(Action.resource_thumbnail_update, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), "");
            addGrowl(FacesMessage.SEVERITY_INFO, "Successfully updated the thumbnail");
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public Resource getClickedResource()
    {
        return rightPaneBean.getClickedResource();
    }

    // -------------------  Simple getters and setters ---------------------------
    public Tag getSelectedTag()
    {
        return selectedTag;
    }

    public void setSelectedTag(Tag selectedTag)
    {
        this.selectedTag = selectedTag;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public Comment getClickedComment()
    {
        return clickedComment;
    }

    public void setClickedComment(Comment clickedComment)
    {
        this.clickedComment = clickedComment;
    }

    public String getNewComment()
    {
        return newComment;
    }

    public void setNewComment(String newComment)
    {
        this.newComment = newComment;
    }

    public boolean isStarRatedByUser() throws Exception
    {
        if(getUser() == null || null == rightPaneBean.getClickedResource())
            return false;

        return rightPaneBean.getClickedResource().isRatedByUser(getUser().getId());
    }

    public boolean isThumbRatedByUser() throws SQLException
    {
        if(getUser() == null || null == rightPaneBean.getClickedResource())
            return false;

        return rightPaneBean.getClickedResource().isThumbRatedByUser(getUser().getId());
    }

    public void handleRate(RateEvent rateEvent)
    {
        if(null == getUser())
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        try
        {
            if(isStarRatedByUser())
            {
                addGrowl(FacesMessage.SEVERITY_FATAL, "resource_already_rated");
                return;
            }

            rightPaneBean.getClickedResource().rate((Integer) rateEvent.getRating(), getUser());
        }
        catch(Exception e)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.rating_resource, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId());

        addGrowl(FacesMessage.SEVERITY_INFO, "resource_rated");
    }

    private void handleThumbRating(int direction)
    {
        if(null == getUser())
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        try
        {
            if(isThumbRatedByUser())
            {
                addGrowl(FacesMessage.SEVERITY_FATAL, "resource_already_rated");
                return;
            }

            rightPaneBean.getClickedResource().thumbRate(getUser(), direction);
        }
        catch(Exception e)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.thumb_rating_resource, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId());

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

    public String getHypothesisLink()
    {
        return hypothesisProxy + rightPaneBean.getClickedResource().getUrl();
    }

    public RightPaneBean getRightPaneBean()
    {
        return rightPaneBean;
    }

    public void setRightPaneBean(RightPaneBean rightPaneBean)
    {
        this.rightPaneBean = rightPaneBean;
    }
}

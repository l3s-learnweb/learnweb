package de.l3s.learnweb.beans;

import de.l3s.learnweb.*;
import de.l3s.learnweb.solrClient.FileInspector;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RateEvent;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

// TODO Oleh: rename to resourceAnnotationBean or merge with RightPaneBean (ResourcePaneBean)
@ManagedBean
@ViewScoped
public class ResourceDetailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 4911923763255682055L;
    private final static Logger log = Logger.getLogger(ResourceDetailBean.class);

    private final static String hypothesisProxy = "https://via.hypothes.is/";

    private Tag selectedTag;
    private String tagName;
    private Comment clickedComment;
    private String newComment;

    //added by Chloe: to allow addition of new extended metadata
    private String selectedTopcat;
    private String selectedMidcat;
    private String selectedBotcat;
    private String[] selectedLevels;
    private String[] selectedTargets;
    private String[] selectedPurposes;
    private String newBotcat = "";

    @ManagedProperty(value = "#{rightPaneBean}")
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
            addFatalMessage(e);
        }
    }

    public String addTag()
    {

        //Limit of no. of spaces in a tag = 3

        if((StringUtils.countMatches(tagName, " ") > 3) || tagName.contains(",") || tagName.contains("#") || (tagName.length() > 50))
        {
            showTagWarningMessage();

            return null;
        }

        if(null == getUser())
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return null;
        }

        if(tagName == null || tagName.length() == 0)
            return null;

        try
        {
            rightPaneBean.getClickedResource().addTag(tagName, getUser());
            addGrowl(FacesMessage.SEVERITY_INFO, "tag_added");
            log(LogEntry.Action.tagging_resource, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), tagName);
            tagName = ""; // clear tag input field
        }
        catch(Exception e)
        {
            addFatalMessage(e);
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
            addFatalMessage(e);
        }
    }

    private void showTagWarningMessage()
    {
        ResourceBundle bundle = getFacesContext().getApplication().getResourceBundle(getFacesContext(), "msg");
        String title = bundle.getString("incorrect_tags");
        //getLocaleMessage(msgKey, args)
        if(tagName.contains("#"))
        {
            String newTags = tagName.replaceAll("#", " ");
            int countTags = tagName.trim().length() - tagName.trim().replaceAll("#", "").length();
            FacesMessage message = null;
            String text = bundle.getString("tags_hashtag");
            if(countTags > 1)
                message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text + newTags);
            else
                message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text + newTags);
            RequestContext.getCurrentInstance().showMessageInDialog(message);
        }
        else if(tagName.contains(","))
        {
            String text = bundle.getString("tags_specialCharacter");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text);
            RequestContext.getCurrentInstance().showMessageInDialog(message);
        }
        else if((StringUtils.countMatches(tagName, " ") > 3))
        {
            String text = bundle.getString("tags_spaces");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text);
            RequestContext.getCurrentInstance().showMessageInDialog(message);
        }
        else
        {
            String text = bundle.getString("tags_tooLong");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text);
            RequestContext.getCurrentInstance().showMessageInDialog(message);
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
            addFatalMessage(e);
        }
    }

    public void onDeleteComment()
    {
        try
        {
            rightPaneBean.getClickedResource().deleteComment(clickedComment);
            addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
            log(LogEntry.Action.deleting_comment, rightPaneBean.getClickedResource().getGroupId(), clickedComment.getResourceId(), clickedComment.getId() + "");
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void addComment()
    {
        try
        {
            Comment comment = rightPaneBean.getClickedResource().addComment(newComment, getUser());
            log(LogEntry.Action.commenting_resource, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), comment.getId() + "");
            addGrowl(FacesMessage.SEVERITY_INFO, "comment_added");
            newComment = "";
        }
        catch(Exception e)
        {
            addFatalMessage(e);
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
            log(LogEntry.Action.resource_thumbnail_update, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), "");
            addGrowl(FacesMessage.SEVERITY_INFO, "Successfully updated the thumbnail");
        }
        catch(Exception e)
        {
            addFatalMessage(e);
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

        log(LogEntry.Action.rating_resource, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId());

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

        log(LogEntry.Action.thumb_rating_resource, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId());

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

    //methods to add new extended metadata to a resource
    public void addNewMetadata()
    {
        //now move to rightPaneBean.getClickedResource() addNewLevels()
        if(null == getUser())
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        //check if each value is not empty, then pass it to Resource to add new

        //selectedLevels
        if(selectedLevels.length > 0)
        {
            try
            {
                rightPaneBean.getClickedResource().addNewLevels(selectedLevels, getUser());

                String sLevels = "";
                for(int i = 0; i < selectedLevels.length; i++)
                {
                    sLevels += selectedLevels[i] + ";";
                }
                log(LogEntry.Action.adding_yourown_metadata, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), "language levels: " + sLevels);
                selectedLevels = null; // clear lang level field
            }
            catch(Exception e)
            {
                addFatalMessage(e);
            }
        }

        if(selectedTargets.length > 0)
        {
            //now move to rightPaneBean.getClickedResource() addNewTargets()
            try
            {
                rightPaneBean.getClickedResource().addNewTargets(selectedTargets, getUser());

                String sTargets = "";
                for(int i = 0; i < selectedTargets.length; i++)
                {
                    sTargets += selectedTargets[i] + ";";
                }
                log(LogEntry.Action.adding_yourown_metadata, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), "audiences: " + sTargets);
                selectedTargets = null;
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                addFatalMessage(e);
            }

        }

        if(selectedPurposes.length > 0)
        {
            //now move to rightPaneBean.getClickedResource() addNewPurposes()
            try
            {
                rightPaneBean.getClickedResource().addNewPurposes(selectedPurposes, getUser());

                String sPurposes = "";
                for(int i = 0; i < selectedPurposes.length; i++)
                {
                    sPurposes += selectedPurposes[i] + ";";
                }
                log(LogEntry.Action.adding_yourown_metadata, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), "purposes: " + sPurposes);
                selectedPurposes = null;
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                addFatalMessage(e);
            }
        }

        //adding category if any; allow users to add category only when at least top category is valid
        if(!(selectedTopcat.equalsIgnoreCase("Select Category Level 1")))
        {

            if(selectedMidcat.equalsIgnoreCase("Select Category Level 2"))
            {
                selectedMidcat = "x";
                selectedBotcat = "x";
            }

            if((selectedBotcat.equalsIgnoreCase("")) || (selectedBotcat == null))
            {
                selectedBotcat = "x";
            }

            try
            {
                rightPaneBean.getClickedResource().addNewCategory(selectedTopcat, selectedMidcat, selectedBotcat, getUser());

                String sCat = selectedTopcat + "/" + selectedMidcat + "/" + selectedBotcat;
                log(LogEntry.Action.adding_yourown_metadata, rightPaneBean.getClickedResource().getGroupId(), rightPaneBean.getClickedResource().getId(), "category: " + sCat);

            }
            catch(SQLException e)
            {
                addFatalMessage(e);
                return;
            }
        }

        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    //getter and setter for extended metadata variables
    public String getSelectedTopcat()
    {
        return selectedTopcat;
    }

    public void setSelectedTopcat(String selectedTopcat)
    {
        this.selectedTopcat = selectedTopcat;
    }

    public String getSelectedMidcat()
    {
        return selectedMidcat;
    }

    public void setSelectedMidcat(String selectedMidcat)
    {
        this.selectedMidcat = selectedMidcat;
    }

    public String getSelectedBotcat()
    {
        return selectedBotcat;
    }

    public void setSelectedBotcat(String selectedBotcat)
    {
        this.selectedBotcat = selectedBotcat;
    }

    public String[] getSelectedLevels()
    {
        return selectedLevels;
    }

    public void setSelectedLevels(String[] selectedLevels)
    {
        this.selectedLevels = selectedLevels;
    }

    public String[] getSelectedTargets()
    {
        return selectedTargets;
    }

    public void setSelectedTargets(String[] selectedTargets)
    {
        this.selectedTargets = selectedTargets;
    }

    public String[] getSelectedPurposes()
    {
        return selectedPurposes;
    }

    public void setSelectedPurposes(String[] selectedPurposes)
    {
        this.selectedPurposes = selectedPurposes;
    }

    public String getNewBotcat()
    {
        return newBotcat;
    }

    public void setNewBotcat(String newBotcat)
    {
        this.newBotcat = newBotcat;
    }

    public void onOpenExtendedMetadataDialog()
    {
        //log.debug("onOpenExtendedMetadataDialog");
        int groupId = rightPaneBean.getClickedResource() == null ? 0 : rightPaneBean.getClickedResource().getGroupId();
        log(LogEntry.Action.extended_metadata_open_dialog, groupId, 0);
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

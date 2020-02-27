package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.primefaces.PrimeFaces;
import org.primefaces.event.RateEvent;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.archive.TimelineData;
import de.l3s.learnweb.resource.office.FileEditorBean;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class ResourceDetailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 4911923763255682055L;
    private static final Logger log = Logger.getLogger(ResourceDetailBean.class);
    private static final String hypothesisProxy = "https://via.hypothes.is/";

    public enum ViewAction
    {
        none,
        newResource,
        viewResource,
        editResource,
        newFolder,
        editFolder,
        viewFolder,
        newFile
    }

    private Tag selectedTag;
    private String tagName;
    private Comment clickedComment;
    private String newComment;

    private int resourceId = 0; // url param, force resource view

    private ViewAction paneAction = ViewAction.none;
    private AbstractResource clickedAbstractResource;

    @Inject
    private FileEditorBean fileEditorBean;

    @Inject
    private AddResourceBean addResourceBean;

    public void onLoad()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {
            try
            {
                Resource resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);
                if(resource == null)
                {
                    addInvalidParameterMessage("resource_id");
                    return;
                }

                setViewResource(resource);
            }
            catch(Exception e)
            {
                addErrorMessage(e);
            }
        }
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public void editClickedResource() throws SQLException
    {
        if (clickedAbstractResource == null || !clickedAbstractResource.canEditResource(getUser()))
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "resourceNotSelectedOrUserCanNotEditIt");
            return;
        }

        try
        {
            clickedAbstractResource.unlockResource(getUser());
            clickedAbstractResource.save();

            if (clickedAbstractResource instanceof Folder)
            {
                log(Action.edit_folder, clickedAbstractResource.getGroupId(), clickedAbstractResource.getId(), clickedAbstractResource.getTitle());
                addMessage(FacesMessage.SEVERITY_INFO, "folderUpdated", clickedAbstractResource.getTitle());
            }
            else
            {
                log(Action.edit_resource, clickedAbstractResource.getGroupId(), clickedAbstractResource.getId(), clickedAbstractResource.getTitle());
                addMessage(FacesMessage.SEVERITY_INFO, "resourceUpdated", clickedAbstractResource.getTitle());
            }

            setViewResource(clickedAbstractResource);
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void cancelEditClickedResource()
    {
        if (clickedAbstractResource != null)
        {
            clickedAbstractResource.unlockResource(getUser());
            setViewResource(clickedAbstractResource);
        }
    }

    public String getPanelTitle()
    {
        switch(this.paneAction)
        {
            case newResource:
                return UtilBean.getLocaleMessage("upload_resource");
            case viewResource:
                return UtilBean.getLocaleMessage("resource") + " - " + clickedAbstractResource.getTitle();
            case editResource:
                return UtilBean.getLocaleMessage("edit_resource") + " - " + clickedAbstractResource.getTitle();
            case newFolder:
                return UtilBean.getLocaleMessage("create_folder");
            case editFolder:
                return UtilBean.getLocaleMessage("edit_folder");
            case viewFolder:
                return UtilBean.getLocaleMessage("folder") + " - " + clickedAbstractResource.getTitle();
            case newFile:
                return UtilBean.getLocaleMessage("create") + " - " + addResourceBean.getResource().getType().toString();
            default:
                return UtilBean.getLocaleMessage("click_to_view_details");
        }
    }

    public void resetPane()
    {
        clickedAbstractResource = null;
        paneAction = ViewAction.none;
        releaseResourceIfLocked();
    }

    public void releaseResourceIfLocked()
    {
        if (clickedAbstractResource != null && clickedAbstractResource.isEditLocked())
        {
            clickedAbstractResource.unlockResource(getUser());
        }
    }

    public void setViewResource(AbstractResource resource)
    {
        releaseResourceIfLocked();
        setClickedAbstractResource(resource);

        if(resource == null)
        {
            resetPane();
        }
        else if(resource instanceof Folder)
        {
            paneAction = ViewAction.viewFolder;
            log(Action.opening_folder, resource.getGroupId(), resource.getId());
        }
        else
        {
            paneAction = ViewAction.viewResource;
            log(Action.opening_resource, resource.getGroupId(), resource.getId());
        }
    }

    public void setEditResource(AbstractResource resource)
    {
        releaseResourceIfLocked();

        if (!resource.lockResource(getUser()))
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "resourceLockedByAnotherUser", resource.getLockUsername());

            if (resource instanceof Resource)
            {
                log(Action.lock_rejected_edit_resource, resource.getGroupId(), resource.getId());
            }
            else if (resource instanceof Folder)
            {
                log(Action.lock_rejected_edit_folder, resource.getGroupId(), resource.getId());
            }
            return;
        }

        setClickedAbstractResource(resource);

        if(resource instanceof Folder)
        {
            paneAction = ViewAction.editFolder;
            log(Action.opening_folder, resource.getGroupId(), resource.getId());
        }
        else
        {
            paneAction = ViewAction.editResource;
            log(Action.opening_resource, resource.getGroupId(), resource.getId());
        }
    }

    public void editActivityListener()
    {
        if (clickedAbstractResource != null && !clickedAbstractResource.lockerUpdate(getUser()))
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "resourceEditInterrupt");
            setViewResource(clickedAbstractResource);
            PrimeFaces.current().ajax().update(":right_pane");

            if (clickedAbstractResource instanceof Resource)
            {
                log(Action.lock_interrupted_returned_resource, clickedAbstractResource.getGroupId(), clickedAbstractResource.getId());
            }
            else if (clickedAbstractResource instanceof Folder)
            {
                log(Action.lock_interrupted_returned_folder, clickedAbstractResource.getGroupId(), clickedAbstractResource.getId());
            }
        }
    }

    public ViewAction getPaneAction()
    {
        return paneAction;
    }

    public void setPaneAction(ViewAction paneAction)
    {
        this.paneAction = paneAction;
    }

    public boolean isTheResourceClicked(AbstractResource resource)
    {
        return clickedAbstractResource != null && clickedAbstractResource.equals(resource);
    }

    public Folder getClickedFolder()
    {
        if(clickedAbstractResource instanceof Folder)
            return (Folder) clickedAbstractResource;

        return null;
    }

    public Resource getClickedResource()
    {
        if(clickedAbstractResource instanceof Resource)
            return (Resource) clickedAbstractResource;

        return null;
    }

    public AbstractResource getClickedAbstractResource()
    {
        return clickedAbstractResource;
    }

    public void setClickedAbstractResource(AbstractResource resource)
    {
        clickedAbstractResource = resource;

        if(getClickedResource() != null && getClickedResource().isOfficeResource())
            fileEditorBean.fillInFileInfo(getClickedResource());
    }

    /* Archive view utils  */

    /**
     * The method is used from JS in archive_timeline_template.xhtml
     */
    public String getArchiveTimelineJsonData()
    {
        Resource resource = getClickedResource();
        JSONArray highChartsData = new JSONArray();
        try
        {
            List<TimelineData> timelineMonthlyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByMonth(resource.getId(), resource.getUrl());

            for(TimelineData timelineData : timelineMonthlyData)
            {
                JSONArray innerArray = new JSONArray();
                innerArray.put(timelineData.getTimestamp().getTime());
                innerArray.put(timelineData.getNumberOfVersions());
                highChartsData.put(innerArray);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching the archive data aggregated by month for a resource", e);
            addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
        }
        return highChartsData.toString();
    }

    /**
     * The method is used from JS in archive_timeline_template.xhtml
     */
    public String getArchiveCalendarJsonData()
    {
        Resource resource = getClickedResource();
        JSONObject archiveDates = new JSONObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            List<TimelineData> timelineDailyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByDay(resource.getId(), resource.getUrl());
            for(TimelineData timelineData : timelineDailyData)
            {
                JSONObject archiveDay = new JSONObject();
                archiveDay.put("number", timelineData.getNumberOfVersions());
                archiveDay.put("badgeClass", "badge-warning");
                List<ArchiveUrl> archiveUrlsData = getLearnweb().getTimelineManager().getArchiveUrlsByResourceIdAndTimestamp(resource.getId(), timelineData.getTimestamp(), resource.getUrl());
                JSONArray archiveVersions = new JSONArray();
                for(ArchiveUrl archiveUrl : archiveUrlsData)
                {
                    JSONObject archiveVersion = new JSONObject();
                    archiveVersion.put("url", archiveUrl.getArchiveUrl());
                    archiveVersion.put("time", DateFormat.getTimeInstance(DateFormat.MEDIUM, UtilBean.getUserBean().getLocale()).format(archiveUrl.getTimestamp()));
                    archiveVersions.put(archiveVersion);
                }
                archiveDay.put("dayEvents", archiveVersions);
                archiveDates.put(dateFormat.format(timelineData.getTimestamp()), archiveDay);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching the archive data aggregated by day for a resource", e);
            addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
        }
        return archiveDates.toString();
    }

    /**
     * Function to get short week day names for the calendar
     */
    public List<String> getShortWeekDays()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        List<String> dayNames = Arrays.asList(symbols.getShortWeekdays());
        Collections.rotate(dayNames.subList(1, 8), -1);
        return dayNames.subList(1, 8);
    }

    /**
     * Function to localized month names for the calendar
     * The method is used from JS in archive_timeline_template.xhtml
     */
    public String getMonthNames()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        JSONArray monthNames = new JSONArray();
        for(String month : symbols.getMonths()) if(!month.isBlank()) monthNames.put(month);
        return monthNames.toString();
    }

    /**
     * Function to get localized short month names for the timeline
     * The method is used from JS in archive_timeline_template.xhtml
     */
    public String getShortMonthNames()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        JSONArray monthNames = new JSONArray();
        for(String month : symbols.getShortMonths()) if(!month.isBlank()) monthNames.put(month);
        return monthNames.toString();
    }

    @SuppressWarnings("unused")
    public void setStarRatingRounded(int value)
    {
        // dummy method, is required by p:rating
    }

    public void archiveCurrentVersion()
    {
        boolean addToQueue = true;
        if(getClickedResource().getArchiveUrls().size() > 0)
        {
            long timeDifference = (new Date().getTime() - getClickedResource().getArchiveUrls().getLast().getTimestamp().getTime()) / 1000;
            addToQueue = timeDifference > 300;
        }

        if(addToQueue)
        {
            String response = getLearnweb().getArchiveUrlManager().addResourceToArchive(getClickedResource());
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
            getClickedResource().deleteTag(selectedTag);
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
            Resource resource = getClickedResource();
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
            Collection<File> files = getClickedResource().getFiles().values();
            for(File file : files)
            {
                if(file.getType() == File.TYPE.THUMBNAIL_LARGE || file.getType() == File.TYPE.THUMBNAIL_MEDIUM || file.getType() == File.TYPE.THUMBNAIL_SMALL || file.getType() == File.TYPE.THUMBNAIL_SQUARED || file.getType() == File.TYPE.THUMBNAIL_VERY_SMALL) // number 4 is reserved for the source file
                {
                    log.debug("Delete " + file.getName());
                    fileManager.delete(file);
                }
            }

            ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();
            rpm.processResource(getClickedResource());

            getClickedResource().save();
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
        User owner = getClickedResource().getTags().getElementOwner(tag);
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
            getClickedResource().deleteComment(clickedComment);
            addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
            log(Action.deleting_comment, getClickedResource().getGroupId(), clickedComment.getResourceId(), clickedComment.getId());
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
            Comment comment = getClickedResource().addComment(newComment, getUser());
            log(Action.commenting_resource, getClickedResource().getGroupId(), getClickedResource().getId(), comment.getId());
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
            Collection<File> files = getClickedResource().getFiles().values();
            for(File file : files)
            {
                if(file.getType() == File.TYPE.THUMBNAIL_LARGE || file.getType() == File.TYPE.THUMBNAIL_MEDIUM || file.getType() == File.TYPE.THUMBNAIL_SMALL || file.getType() == File.TYPE.THUMBNAIL_SQUARED || file.getType() == File.TYPE.THUMBNAIL_VERY_SMALL) // number 4 is reserved for the source file
                {
                    log.debug("Delete " + file.getName());
                    fileManager.delete(file);
                }
            }

            //Getting mime type
            FileInspector.FileInfo info = rme.getFileInfo(FileInspector.openStream(archiveUrl), getClickedResource().getFileName());
            String type = info.getMimeType().substring(0, info.getMimeType().indexOf("/"));
            if(type.equals("application"))
                type = info.getMimeType().substring(info.getMimeType().indexOf("/") + 1);

            if(type.equalsIgnoreCase("pdf"))
            {
                rpm.processPdf(getClickedResource(), FileInspector.openStream(archiveUrl));
            }
            else
                rpm.processArchivedVersion(getClickedResource(), archiveUrl);

            getClickedResource().save();
            log(Action.resource_thumbnail_update, getClickedResource().getGroupId(), getClickedResource().getId(), "");
            addGrowl(FacesMessage.SEVERITY_INFO, "Successfully updated the thumbnail");
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
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
        if(getUser() == null || null == getClickedResource())
            return false;

        return getClickedResource().isRatedByUser(getUser().getId());
    }

    public boolean isThumbRatedByUser() throws SQLException
    {
        if(getUser() == null || null == getClickedResource())
            return false;

        return getClickedResource().isThumbRatedByUser(getUser().getId());
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

            getClickedResource().rate((Integer) rateEvent.getRating(), getUser());
        }
        catch(Exception e)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.rating_resource, getClickedResource().getGroupId(), getClickedResource().getId());

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

            getClickedResource().thumbRate(getUser(), direction);
        }
        catch(Exception e)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.thumb_rating_resource, getClickedResource().getGroupId(), getClickedResource().getId());

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
        return hypothesisProxy + getClickedResource().getUrl();
    }

    /* Load beans  */

    public FileEditorBean getFileEditorBean()
    {
        return fileEditorBean;
    }

    public void setFileEditorBean(FileEditorBean fileEditorBean)
    {
        this.fileEditorBean = fileEditorBean;
    }

    public AddResourceBean getAddResourceBean()
    {
        return addResourceBean;
    }

    public void setAddResourceBean(AddResourceBean addResourceBean)
    {
        this.addResourceBean = addResourceBean;
    }

}

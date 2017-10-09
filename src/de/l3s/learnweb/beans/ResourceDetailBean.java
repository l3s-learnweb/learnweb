package de.l3s.learnweb.beans;

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
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RateEvent;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Course;
import de.l3s.learnweb.File;
import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.FileManager;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceMetadataExtractor;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.TimelineData;
import de.l3s.learnweb.User;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;

@SuppressWarnings("unchecked")
@ManagedBean(name = "resourceDetailBean")
@ViewScoped
public class ResourceDetailBean extends ApplicationBean implements Serializable
{
    private final static long serialVersionUID = -4468979717844804599L;
    private final static Logger log = Logger.getLogger(ResourceDetailBean.class);

    private int resourceId = 0;
    private Resource clickedResource;
    private Tag selectedTag;
    private String tagName;
    private Comment clickedComment;
    private String newComment;

    private boolean isStarRatingEnabled = false;
    private boolean isThumbRatingEnabled = false;

    //added by Chloe: to allow addition of new extended metadata
    private String selectedTopcat;
    private String selectedMidcat;
    private String selectedBotcat;
    private String[] selectedLevels;
    private String[] selectedTargets;
    private String[] selectedPurposes;

    public ResourceDetailBean() throws SQLException
    {
        clickedResource = new Resource();

        User user = getUser();
        if(user != null)
        {
            Course course = user.getActiveCourse();

            if(course != null)
            {
                isThumbRatingEnabled = course.getOption(Course.Option.Resources_Enable_Thumb_rating);
                isStarRatingEnabled = course.getOption(Course.Option.Resources_Enable_Star_rating);
            }
        }
    }

    public void setStarRatingRounded(int value)
    {
        // dummy method, is required by p:rating
    }

    public void preRenderView(ComponentSystemEvent event)
    {
        if(isAjaxRequest())
        {
            return;
        }

        if(resourceId > 0)
        {
            try
            {
                clickedResource = Learnweb.getInstance().getResourceManager().getResource(resourceId);

                log(Action.opening_resource, clickedResource.getGroupId(), clickedResource.getId(), "");
            }
            catch(SQLException e)
            {
                addFatalMessage(e);
            }
        }
    }

    public String getArchiveTimelineJsonData()
    {
        JSONArray highChartsData = new JSONArray();
        try
        {
            List<TimelineData> timelineMonthlyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByMonth(clickedResource.getId(), clickedResource.getUrl());

            for(TimelineData timelineData : timelineMonthlyData)
            {
                JSONArray innerArray = new JSONArray();
                innerArray.add(timelineData.getTimestamp().getTime());
                innerArray.add(timelineData.getNumberOfVersions());
                highChartsData.add(innerArray);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching the archive data aggregated by month for a resource", e);
            addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
        }
        return highChartsData.toJSONString();
    }

    public String getArchiveCalendarJsonData()
    {
        JSONObject archiveDates = new JSONObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            List<TimelineData> timelineDailyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByDay(clickedResource.getId(), clickedResource.getUrl());
            for(TimelineData timelineData : timelineDailyData)
            {
                JSONObject archiveDay = new JSONObject();
                archiveDay.put("number", timelineData.getNumberOfVersions());
                archiveDay.put("badgeClass", "badge-warning");
                List<ArchiveUrl> archiveUrlsData = getLearnweb().getTimelineManager().getArchiveUrlsByResourceIdAndTimestamp(clickedResource.getId(), timelineData.getTimestamp(), clickedResource.getUrl());
                JSONArray archiveVersions = new JSONArray();
                for(ArchiveUrl archiveUrl : archiveUrlsData)
                {
                    JSONObject archiveVersion = new JSONObject();
                    archiveVersion.put("url", archiveUrl.getArchiveUrl());
                    archiveVersion.put("time", DateFormat.getTimeInstance(DateFormat.MEDIUM, UtilBean.getUserBean().getLocale()).format(archiveUrl.getTimestamp()));
                    archiveVersions.add(archiveVersion);
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
        return archiveDates.toJSONString();
    }

    //Function to get short week day names for the calendar
    public List<String> getShortWeekDays()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        List<String> dayNames = Arrays.asList(symbols.getShortWeekdays());
        Collections.rotate(dayNames.subList(1, 8), -1);
        return dayNames.subList(1, 8);
    }

    //Function to localized month names for the calendar 
    public String getMonthNames()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        JSONArray monthNames = new JSONArray();
        for(String month : symbols.getMonths())
        {
            if(!month.equals(""))
                monthNames.add(month);
        }

        return monthNames.toJSONString();
    }

    //Function to get localized short month names for the timeline
    public String getShortMonthNames()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        JSONArray monthNames = new JSONArray();
        for(String month : symbols.getShortMonths())
        {
            if(!month.equals(""))
                monthNames.add(month);
        }

        return monthNames.toJSONString();
    }

    public void archiveCurrentVersion()
    {
        boolean addToQueue = true;
        if(clickedResource.getArchiveUrls().size() > 0)
        {
            long timeDifference = (new Date().getTime() - clickedResource.getArchiveUrls().getLast().getTimestamp().getTime()) / 1000;
            addToQueue = timeDifference > 300;
        }

        if(addToQueue)
        {
            String response = getLearnweb().getArchiveUrlManager().addResourceToArchive(clickedResource);
            if(response.equalsIgnoreCase("archive_success"))
                addGrowl(FacesMessage.SEVERITY_INFO, "addedToArchiveQueue");
            else if(response.equalsIgnoreCase("robots_error"))
                addGrowl(FacesMessage.SEVERITY_INFO, "archiveRobotsMessage");
        }
        else
            addGrowl(FacesMessage.SEVERITY_INFO, "archiveWaitMessage");

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
            clickedResource.addTag(tagName, getUser());
            addGrowl(FacesMessage.SEVERITY_INFO, "tag_added");
            log(Action.tagging_resource, clickedResource.getGroupId(), clickedResource.getId(), tagName);
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
            Collection<File> files = clickedResource.getFiles().values();
            for(File file : files)
            {
                if(file.getType() == TYPE.THUMBNAIL_LARGE || file.getType() == TYPE.THUMBNAIL_MEDIUM || file.getType() == TYPE.THUMBNAIL_SMALL || file.getType() == TYPE.THUMBNAIL_SQUARD || file.getType() == TYPE.THUMBNAIL_VERY_SMALL) // number 4 is reserved for the source file
                {
                    log.debug("Delete " + file.getName());
                    fileManager.delete(file);
                }
            }

            if(clickedResource.getStorageType() == Resource.FILE_RESOURCE)
            {
                ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();
                log.debug("Get the mime type and extract text if possible");
                FileInfo info = rpm.getFileInfo(FileInspector.openStream(clickedResource.getUrl()), clickedResource.getFileName());

                log.debug("Create thumbnails");
                rpm.processFile(clickedResource, FileInspector.openStream(clickedResource.getUrl()), info);
            }
            else
            {
                ResourceMetadataExtractor extractor = new ResourceMetadataExtractor(clickedResource);
                extractor.makePreview();
            }
            clickedResource.save();
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
        User owner = clickedResource.getTags().getElementOwner(tag);
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
            clickedResource.deleteComment(clickedComment);
            addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
            log(Action.deleting_comment, clickedResource.getGroupId(), clickedComment.getResourceId(), clickedComment.getId() + "");
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
            Comment comment = clickedResource.addComment(newComment, getUser());
            log(Action.commenting_resource, clickedResource.getGroupId(), clickedResource.getId(), comment.getId() + "");
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
            rpm.processArchivedVersion(clickedResource, archiveUrl);
            clickedResource.save();
            log(Action.resource_thumbnail_update, clickedResource.getGroupId(), clickedResource.getId(), "");
            addGrowl(FacesMessage.SEVERITY_INFO, "Successfully updated the thumbnail");
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void selectResource()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String resourceIdStr = params.get("resourceId");

        try
        {
            int resourceId = Integer.parseInt(resourceIdStr);
            Resource res = Learnweb.getInstance().getResourceManager().getResource(resourceId);

            this.setClickedResource(res);
        }
        catch(Exception e)
        {

        }
    }

    // -------------------  Simple getters and setters ---------------------------

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public Resource getClickedResource()
    {
        return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
        if(clickedResource != null && clickedResource.getType().equals("folder"))
        {
            this.clickedResource = new Resource();
        }
        else
        {
            this.clickedResource = clickedResource;
        }
    }

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
        if(getUser() == null || null == clickedResource)
            return false;

        return clickedResource.isRatedByUser(getUser().getId());
    }

    public boolean isThumbRatedByUser() throws SQLException
    {
        if(getUser() == null || null == clickedResource)
            return false;

        return clickedResource.isThumbRatedByUser(getUser().getId());
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

            clickedResource.rate((Integer) rateEvent.getRating(), getUser());
        }
        catch(Exception e)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.rating_resource, clickedResource.getGroupId(), clickedResource.getId());

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

            clickedResource.thumbRate(getUser(), direction);
        }
        catch(Exception e)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.thumb_rating_resource, clickedResource.getGroupId(), clickedResource.getId());

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

    public boolean isStarRatingEnabled()
    {
        return isStarRatingEnabled;
    }

    public boolean isThumbRatingEnabled()
    {
        return isThumbRatingEnabled;
    }

    //methods to add new extended metadata to a resource
    public void addNewMetadata()
    {
        //now move to clickedResource addNewLevels()
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
                clickedResource.addNewLevels(selectedLevels, getUser());
                addGrowl(FacesMessage.SEVERITY_INFO, "langlevels_added"); // I would not add one message for each input field

                //log(Action.tagging_resource, clickedResource.getGroupId(), clickedResource.getId(), tagName);
                selectedLevels = null; // clear lang level field 
            }
            catch(Exception e)
            {
                addFatalMessage(e);
            }
        }
        else
        {
            return;
        }

        if(selectedTargets.length > 0)
        {
            //now move to clickedResource addNewTargets()
        }

        if(selectedPurposes.length > 0)
        {
            //now move to clickedResource addNewPurposes()
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

}

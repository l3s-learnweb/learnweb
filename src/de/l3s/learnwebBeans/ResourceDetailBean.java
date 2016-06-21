package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.TimelineData;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@SuppressWarnings("unchecked")
@ManagedBean
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

    public ResourceDetailBean()
    {
	clickedResource = new Resource();
    }

    public void preRenderView(ComponentSystemEvent e)
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
	    catch(SQLException e1)
	    {
		log.error("Can't view resource: " + resourceId, e1);
		addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
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
}

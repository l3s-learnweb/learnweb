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

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Comment;
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
    private static final long serialVersionUID = -4468979717844804599L;
    private final static Logger log = Logger.getLogger(ResourceDetailBean.class);
    private int resourceId = 0;
    private Resource clickedResource;
    private Tag selectedTag;
    private String tagName;
    private Comment clickedComment;
    private String newComment;

    public String getHighChartsJsonData()
    {
	JSONArray highChartsData = new JSONArray();
	try
	{
	    List<TimelineData> timelineMonthlyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByMonth(clickedResource.getId());

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

    public String getCalendarJsonData()
    {
	JSONObject archiveDates = new JSONObject();
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	try
	{
	    List<TimelineData> timelineDailyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByDay(clickedResource.getId());
	    for(TimelineData timelineData : timelineDailyData)
	    {
		JSONObject archiveDay = new JSONObject();
		archiveDay.put("number", timelineData.getNumberOfVersions());
		archiveDay.put("badgeClass", "badge-warning");
		List<ArchiveUrl> archiveUrlsData = getLearnweb().getTimelineManager().getArchiveUrlsByResourceIdAndTimestamp(clickedResource.getId(), timelineData.getTimestamp());
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
	    monthNames.add(month);
	}
	monthNames.remove(""); //To remove empty string from the array
	return monthNames.toJSONString();
    }

    public String getShortMonthNames()
    {
	DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
	JSONArray monthNames = new JSONArray();
	for(String month : symbols.getShortMonths())
	{
	    monthNames.add(month);
	}
	monthNames.remove(""); //To remove empty string from the array
	return monthNames.toJSONString();
    }

    public void archiveCurrentVersion()
    {
	boolean addToQueue = true;
	try
	{
	    if(clickedResource.getArchiveUrls().size() > 0)
	    {
		long timeDifference = (new Date().getTime() - clickedResource.getArchiveUrls().getLast().getTimestamp().getTime()) / 1000;
		addToQueue = timeDifference > 300;
	    }

	    if(addToQueue)
	    {
		getLearnweb().getArchiveUrlManager().addResourceToArchive(clickedResource);
		addGrowl(FacesMessage.SEVERITY_INFO, "addedToArchiveQueue");
	    }
	    else
		addGrowl(FacesMessage.SEVERITY_INFO, "archiveWaitMessage");
	}
	catch(SQLException e)
	{
	    log.error("Error while fetching the archive urls from a resource", e);
	    addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
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

    public boolean canEditComment(Object commentO) throws SQLException
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

    public void addComment()
    {
	try
	{
	    Comment comment = clickedResource.addComment(newComment, getUser());
	    log(Action.commenting_resource, clickedResource.getId(), comment.getId() + "");
	    addGrowl(FacesMessage.SEVERITY_INFO, "comment_added");
	    newComment = "";
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
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

    public Resource getClickedResource()
    {
	return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
	this.clickedResource = clickedResource;
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

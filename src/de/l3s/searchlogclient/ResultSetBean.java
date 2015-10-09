package de.l3s.searchlogclient;

import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.codec.digest.DigestUtils;

import com.sun.jersey.api.client.ClientHandlerException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.AddResourceBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.searchlogclient.Actions.ACTION;
import de.l3s.searchlogclient.jaxb.CommentonSearch;
import de.l3s.searchlogclient.jaxb.QueryLog;
import de.l3s.searchlogclient.jaxb.ResourceLog;
import de.l3s.searchlogclient.jaxb.ResultsetFeed;
import de.l3s.searchlogclient.jaxb.SharedResultset;
import de.l3s.searchlogclient.jaxb.Tag;
import de.l3s.searchlogclient.jaxb.ViewingTime;

@ManagedBean
@SessionScoped
public class ResultSetBean extends ApplicationBean
{

    private int resultSetId;
    private String resultSetIdMd5;
    private String resultSetView;
    private String resultsetfilter;
    private LinkedList<ResourceDecorator> resources;
    private Resource selectedResource;
    private int selectedResourceTargetGroupId;
    private ArrayList<ResourceLog> clickedResources;
    private ArrayList<ResourceLog> savedResources;
    private LinkedList<ResourceLog> clickedResourcesList;
    private LinkedList<ResourceLog> savedResourcesList;
    private ArrayList<CommentonSearch> searchComments;
    private HashMap<String, ResourceLog> resourceClickHashMap;
    private HashMap<String, ResourceLog> resourceSavedHashMap;
    private HashMap<String, CommentonSearch> searchCommentsHashMap;
    private SearchLogClient searchLogClient;
    private String queryDate;
    private String query;
    private ArrayList<Timeline> timeline;
    private String newSearchComment;
    private Tag selectedTag;
    private String tagName;
    private Date startTime;
    private Date endTime;
    private String userNameToShareWith;

    public ResultSetBean()
    {
	resultsetfilter = "all_resources";
	newSearchComment = "";
	searchLogClient = Learnweb.getInstance().getSearchlogClient();
	resourceClickHashMap = new HashMap<String, ResourceLog>();
	resourceSavedHashMap = new HashMap<String, ResourceLog>();
	searchCommentsHashMap = new HashMap<String, CommentonSearch>();
	timeline = new ArrayList<Timeline>();
	setResultSetView("image");
    }

    public void preRenderView()
    {
	timeline.clear();

	if(!FacesContext.getCurrentInstance().isPostback())
	{
	    resultsetfilter = "all_resources";
	    resources = null;
	    clickedResourcesList = null;
	    savedResourcesList = null;
	    clickedResources = null;
	    savedResources = null;
	    searchComments = null;
	    resourceClickHashMap.clear();
	    resourceSavedHashMap.clear();
	    searchCommentsHashMap.clear();
	    int userId = getUser() == null ? -1 : getUser().getId();
	    try
	    {
		searchLogClient.getRecentQuery(userId);
	    }
	    catch(ClientHandlerException e)
	    {
		addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
	    }
	}
    }

    public String resultSetRedirect()
    {
	return getTemplateDir() + "/view_resultset.xhtml?resultsetid=" + DigestUtils.md5Hex(Integer.toString(resultSetId)) + "&amp;faces-redirect=true";
    }

    public LinkedList<ResourceDecorator> getResources()
    {

	try
	{
	    if(resources == null)
	    {
		if(resultsetfilter.equals("") || resultsetfilter.equals("all_resources"))
		    resources = searchLogClient.getResourcesByResultSetId(resultSetId);
		else if(resultsetfilter.equals("resource_click") || resultsetfilter.equals("resource_saved") || resultsetfilter.equals("resources_not_clicked"))
		    resources = searchLogClient.getResourcesByResultSetIdAndAction(resultSetId, resultsetfilter);
	    }
	    if(resources.size() == 0 && FacesContext.getCurrentInstance().getMessageList().size() == 0)
		addMessage(FacesMessage.SEVERITY_INFO, "There are no resources for the action '" + resultsetfilter + "' to be displayed");
	    return resources;
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	    return null;
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	    return null;
	}
    }

    /*
    public ArrayList<ResourceLog> getClickedResources() {
    	if(clickedResources == null)
    		clickedResources = searchLogClient.getResourcesLogByResultsetIdAndAction(resultSetId, "resource_click");
    	return clickedResources;
    }

    public ArrayList<ResourceLog> getSavedResources() {
    	if(savedResources == null)
    		savedResources = searchLogClient.getResourcesLogByResultsetIdAndAction(resultSetId, "resource_saved");
    	return savedResources;
    }
    */
    public LinkedList<ResourceLog> getClickedResourcesList()
    {
	try
	{
	    clickedResourcesList = searchLogClient.getResourceClickList();

	    for(ResourceLog clickedResource : clickedResourcesList)
	    {
		for(ViewingTime viewTime : searchLogClient.getViewingTimeList())
		{
		    if(clickedResource.getResultsetId() == viewTime.getResultsetId() && clickedResource.getResourceRank() == viewTime.getResourceRank())
		    {
			clickedResource.setViewTime((viewTime.getEndTime().getTime() - viewTime.getStartTime().getTime()) / 1000 % 60);
		    }
		}
	    }
	    return clickedResourcesList;
	}
	catch(ClientHandlerException e)
	{
	    addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
	    return null;
	}
	catch(RuntimeException e)
	{
	    addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
	    return null;
	}

    }

    public LinkedList<ResourceLog> getSavedResourcesList()
    {
	try
	{
	    savedResourcesList = searchLogClient.getResourceSavedList();

	    for(ResourceLog savedResource : savedResourcesList)
	    {
		for(ViewingTime viewTime : searchLogClient.getViewingTimeList())
		{
		    if(savedResource.getResultsetId() == viewTime.getResultsetId() && savedResource.getResourceRank() == viewTime.getResourceRank())
		    {
			savedResource.setViewTime((viewTime.getEndTime().getTime() - viewTime.getStartTime().getTime()) / 1000 % 60);
		    }
		}
	    }
	    return savedResourcesList;
	}
	catch(ClientHandlerException e)
	{
	    addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
	    return null;
	}
	catch(RuntimeException e)
	{
	    addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
	    return null;
	}
    }

    public String getResultSetView()
    {
	return resultSetView;
    }

    public void setResultSetView(String resultSetView)
    {
	if(resultSetView.equals("image"))
	    this.resultSetView = "float";
	else if(resultSetView.equals("video"))
	    this.resultSetView = "grid";
	else if(resultSetView.equals("web"))
	    this.resultSetView = "list";
    }

    public int getResultSetId()
    {
	return resultSetId;
    }

    public void setResultSetId(int resultSetId)
    {
	try
	{
	    this.resultSetId = resultSetId;
	    searchLogClient.postResourceLog();
	    searchLogClient.passUpdateResultset();
	    searchLogClient.setResultsetTags(null);
	    searchLogClient.flushLists();
	    searchLogClient.setResultsetid(resultSetId);
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	}
    }

    public String getResultsetfilter()
    {
	return resultsetfilter;
    }

    public void setResultsetfilter(String resultsetfilter)
    {
	this.resultsetfilter = resultsetfilter;
    }

    public void selectFilter()
    {
	resources = null;
    }

    public String getQuery()
    {
	return query;
    }

    public void setQuery(String query)
    {
	this.query = query;
    }

    public String getQueryDate()
    {
	return queryDate;
    }

    public void setQueryDate(String queryDate)
    {
	SimpleDateFormat stringDate = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	try
	{
	    Date queryTimestamp = dateFormatter.parse(queryDate);
	    this.queryDate = stringDate.format(queryTimestamp);
	}
	catch(ParseException e)
	{
	    this.queryDate = queryDate;
	}
    }

    public void addSelectedResource()
    {
	Date date = new Date();
	User user = getUser();
	if(null == user)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}
	try
	{
	    Resource newResource;
	    boolean createNewResource = false;

	    if(selectedResource.getId() == -1) // resource is not yet stored at fedora
	    {
		newResource = selectedResource;
		createNewResource = true;

	    }
	    else
		// create a copy 
		newResource = selectedResource.clone();

	    newResource.setQuery(query);
	    newResource = user.addResource(newResource);

	    // add resource to a group if selected
	    if(selectedResourceTargetGroupId != 0)
	    {
		getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId).addResource(newResource, getUser());
		user.setActiveGroup(selectedResourceTargetGroupId);
	    }

	    if(createNewResource)
		new AddResourceBean.CreateThumbnailThread(newResource).start();

	    int tempresourceId = searchLogClient.getResourceIdByUrl(newResource.getUrl());
	    searchLogClient.saveResourceLog(user.getId(), date, ACTION.resource_saved, newResource.getUrl(), tempresourceId, newResource.getTitle(), newResource.getSource());
	    searchLogClient.addResourceSavedList(tempresourceId, newResource.getId());

	    log(Action.adding_resource, selectedResourceTargetGroupId, newResource.getId(), "");

	    addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", newResource.getTitle());
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
	}

    }

    /**
     * This method logs a resource click event.
     */
    public void logResourceOpened()
    {
	try
	{
	    startTime = new Date();
	    int tempResourceId = getParameterInt("resource_id");
	    Resource resource = getResourceByTempId(tempResourceId);

	    if(null == resource)
		throw new InvalidParameterException("unknown resource id:" + tempResourceId);

	    boolean storedInLearnweb = resource.getId() > 0;
	    int userId = getUser() == null ? -1 : getUser().getId(); // search can be anonymous

	    searchLogClient.saveResourceLog(userId, startTime, ACTION.resource_click, resource.getUrl(), tempResourceId, resource.getTitle(), resource.getSource());
	    System.out.println("userId:" + userId + "; tempId:" + tempResourceId + "; realId:" + resource.getId() + " stored in Learnweb:" + storedInLearnweb);
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	}
    }

    /**
     * This method keeps track of the end of the viewing time for a particular resource
     */
    public void logEndTime()
    {
	endTime = new Date();
	int tempResourceId = getParameterInt("resource_id");
	Resource resource = getResourceByTempId(tempResourceId);

	if(null == resource)
	    throw new InvalidParameterException("unknown resource id:" + tempResourceId);
	try
	{
	    searchLogClient.passViewingTime(tempResourceId, startTime, endTime);
	    //searchlogClient.passBatchViewingTime(Integer.parseInt(tempResourceId), startTime, endTime,getSessionId());
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	}
    }

    /**
     * This method returns the resource corresponding to a given temporary resource ID.
     * 
     * @param tempResourceId
     * @return
     */
    public Resource getResourceByTempId(int tempResourceId)
    {
	Resource resource = null;
	for(ResourceDecorator tempResource : resources)
	{
	    if(tempResource.getTempId() == tempResourceId)
	    {
		resource = tempResource.getResource();
	    }
	}
	return resource;
    }

    public Resource getSelectedResource()
    {
	return selectedResource;
    }

    public void setSelectedResource()
    {
	try
	{
	    int tempResourceId = getParameterInt("resource_id");

	    Resource resource = getResourceByTempId(tempResourceId);

	    if(null == resource)
		throw new InvalidParameterException("unknown resource id:" + tempResourceId);

	    setSelectedResource(resource);
	}
	catch(Throwable e)
	{
	    e.printStackTrace();
	}
    }

    public void setSelectedResource(Resource selectedResource)
    {
	this.selectedResource = selectedResource;
    }

    public int getSelectedResourceTargetGroupId()
    {
	return selectedResourceTargetGroupId;
    }

    public void setSelectedResourceTargetGroupId(int selectedResourceTargetGroupId)
    {
	this.selectedResourceTargetGroupId = selectedResourceTargetGroupId;
    }

    public ArrayList<Timeline> getTimeline()
    {

	ArrayList<String> datesList = new ArrayList<String>();
	ArrayList<String> timestampList = new ArrayList<String>();
	SimpleDateFormat historyDate = new SimpleDateFormat("MMM dd, yyyy");
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
	Date queryDate = null;
	String history_queryDate = "";
	String resourceLogTimestamp, commentTimestamp;
	try
	{
	    if(resultsetfilter.equals("all_resources") || resultsetfilter.equals("resource_click"))
	    {
		if(clickedResources == null)
		    clickedResources = searchLogClient.getResourcesLogByResultsetIdAndAction(resultSetId, "resource_click");
		for(ResourceLog clickedResource : clickedResources)
		{
		    resourceLogTimestamp = clickedResource.getTimestamp();
		    queryDate = dateFormatter.parse(resourceLogTimestamp);
		    history_queryDate = historyDate.format(queryDate);

		    timestampList.add(resourceLogTimestamp);

		    if(!datesList.contains(history_queryDate))
		    {
			datesList.add(history_queryDate);
		    }

		    resourceClickHashMap.put(resourceLogTimestamp, clickedResource);
		}
	    }

	    if(resultsetfilter.equals("all_resources") || resultsetfilter.equals("resource_saved"))
	    {
		if(savedResources == null)
		    savedResources = searchLogClient.getResourcesLogByResultsetIdAndAction(resultSetId, "resource_saved");
		for(ResourceLog savedResource : savedResources)
		{
		    resourceLogTimestamp = savedResource.getTimestamp();
		    queryDate = dateFormatter.parse(resourceLogTimestamp);
		    history_queryDate = historyDate.format(queryDate);
		    timestampList.add(resourceLogTimestamp);

		    if(!datesList.contains(history_queryDate))
		    {
			datesList.add(history_queryDate);
		    }

		    resourceSavedHashMap.put(resourceLogTimestamp, savedResource);
		}
	    }

	    if(resultsetfilter.equals("all_resources"))
	    {
		if(searchComments == null)
		    searchComments = searchLogClient.getSearchCommentsByResultsetId(resultSetId);
		for(CommentonSearch comment : searchComments)
		{
		    commentTimestamp = comment.getTimestamp();
		    queryDate = dateFormatter.parse(commentTimestamp);
		    history_queryDate = historyDate.format(queryDate);
		    timestampList.add(commentTimestamp);

		    comment.setTime(timeFormatter.format(queryDate));

		    if(!datesList.contains(history_queryDate))
		    {
			datesList.add(history_queryDate);
		    }

		    searchCommentsHashMap.put(commentTimestamp, comment);
		}
	    }

	    Collections.sort(datesList);
	    Collections.reverse(datesList);
	    Collections.sort(timestampList);

	    for(String date : datesList)
	    {
		Timeline timelinedate = new Timeline();
		timelinedate.setDate(date);

		for(String timestamp : timestampList)
		{
		    queryDate = dateFormatter.parse(timestamp);
		    history_queryDate = historyDate.format(queryDate);
		    if(history_queryDate.equals(date))
		    {
			timelinedate.getTimestamps().add(timestamp);
		    }
		}
		timeline.add(timelinedate);
	    }

	}
	catch(ParseException e)
	{
	    e.printStackTrace();
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	    return null;
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	    return null;
	}

	return timeline;
    }

    public boolean isResourceClicked(String timestamp)
    {
	return resourceClickHashMap.containsKey(timestamp);
    }

    public ResourceLog getResourceClicked(String timestamp)
    {
	return resourceClickHashMap.get(timestamp);
    }

    public boolean isResourceSaved(String timestamp)
    {
	return resourceSavedHashMap.containsKey(timestamp);
    }

    public ResourceLog getResourceSaved(String timestamp)
    {
	return resourceSavedHashMap.get(timestamp);
    }

    public boolean isSearchComment(String timestamp)
    {
	return searchCommentsHashMap.containsKey(timestamp);
    }

    public CommentonSearch getSearchComment(String timestamp)
    {
	return searchCommentsHashMap.get(timestamp);
    }

    public void addSearchComment()
    {

	Date searchCommentTime = new Date();

	SimpleDateFormat dateToTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat dateToTime = new SimpleDateFormat("HH:mm:ss");

	int userId = getUser() == null ? -1 : getUser().getId(); // search can be anonymous
	if(userId > 0)
	{

	    try
	    {
		searchLogClient.passSearchComment(newSearchComment, userId, dateToTimestamp.format(searchCommentTime), getUser().getUsername(), dateToTime.format(searchCommentTime));
		newSearchComment = "";
	    }
	    catch(ClientHandlerException e)
	    {
		System.out.println("Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		System.out.println(e.getMessage());
	    }

	}

    }

    public LinkedList<CommentonSearch> getSearchComments()
    {
	try
	{
	    return searchLogClient.getSearchCommentsList();
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	    return null;
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	    return null;
	}
    }

    public String getNewSearchComment()
    {
	return newSearchComment;
    }

    public void setNewSearchComment(String newSearchComment)
    {
	this.newSearchComment = newSearchComment;
    }

    public void addTag()
    {
	int userId = getUser() == null ? -1 : getUser().getId(); // search can be anonymous
	if(userId > 0)
	{
	    try
	    {
		searchLogClient.addToTagList(tagName, userId, "resultsetTags");
	    }
	    catch(ClientHandlerException e)
	    {
		addGrowl(FacesMessage.SEVERITY_INFO, "searchTrackerDown");
	    }
	    catch(RuntimeException e)
	    {
		addGrowl(FacesMessage.SEVERITY_INFO, e.getMessage());
	    }
	}
    }

    public void onDeleteTag()
    {
	try
	{
	    searchLogClient.removeFromTagList(selectedTag, "resultsetTags");
	}
	catch(ClientHandlerException e)
	{
	    addGrowl(FacesMessage.SEVERITY_INFO, "searchTrackerDown");
	}
	catch(RuntimeException e)
	{
	    addGrowl(FacesMessage.SEVERITY_INFO, e.getMessage());
	}
    }

    public String getTagName()
    {
	String tempTagName = tagName;
	tagName = "";
	return tempTagName;
    }

    public void setTagName(String tagName)
    {
	this.tagName = tagName;
    }

    public Tag getSelectedTag()
    {
	return selectedTag;
    }

    public void setSelectedTag(Tag selectedTag)
    {
	this.selectedTag = selectedTag;
    }

    public ArrayList<Tag> getTags()
    {
	try
	{
	    return searchLogClient.getResultsetTags();
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	    return null;
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	    return null;
	}
    }

    public ArrayList<String> completetags(String query)
    {
	int userId = (getUser() == null ? -1 : getUser().getId());
	try
	{
	    ArrayList<Tag> tags = searchLogClient.getTagsByUserId(userId);
	    ArrayList<String> tagNames = new ArrayList<String>();
	    for(Tag tag : tags)
	    {
		String tagName = tag.getName();
		if(tagName.toLowerCase().startsWith(query))
		{
		    tagNames.add(tagName);
		}
	    }
	    return tagNames;
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	    return null;
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	    return null;
	}
    }

    public String getUserNameToShareWith()
    {
	return userNameToShareWith;
    }

    public void setUserNameToShareWith(String userNameToShareWith)
    {
	this.userNameToShareWith = userNameToShareWith;
    }

    public void shareResultset()
    {
	int userId = getUser().getId();
	try
	{

	    int userIdToShareWith = getLearnweb().getUserManager().getUserIdByUsername(userNameToShareWith);
	    if(userIdToShareWith > 0)
		searchLogClient.postShareResultset(userId, userIdToShareWith);

	    addGrowl(FacesMessage.SEVERITY_INFO, "Shared Resultset With", getUser().getUsername());
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}
    }

    public ArrayList<ResultsetFeed> viewResultsets()
    {
	if(getUser() == null)
	    return null;

	int userId = getUser().getId();
	ArrayList<ResultsetFeed> resultsetFeed = new ArrayList<ResultsetFeed>();
	try
	{
	    ArrayList<SharedResultset> sharedResultsets = searchLogClient.getSharedResultsetsByUserId(userId);

	    for(SharedResultset sharedResultset : sharedResultsets)
	    {
		User user = null;
		try
		{
		    user = getLearnweb().getUserManager().getUser(sharedResultset.getUserSharing());
		}
		catch(SQLException e)
		{
		    e.printStackTrace();
		}
		resultsetFeed.add(new ResultsetFeed(sharedResultset.getUserSharing(), sharedResultset.getResultsetId(), sharedResultset.getMd5value(), sharedResultset.getQuery(), sharedResultset.getQueryTimestamp(), user));
	    }

	}
	catch(NullPointerException e)
	{
	    System.out.println("Search Tracker service is down");
	    return null;
	}
	catch(ClientHandlerException e)
	{
	    addGrowl(FacesMessage.SEVERITY_INFO, "searchTrackerDown");
	    return null;
	}
	catch(RuntimeException e)
	{
	    addGrowl(FacesMessage.SEVERITY_INFO, e.getMessage());
	    return null;
	}

	return resultsetFeed;
    }

    public String getResultSetIdMd5()
    {
	return resultSetIdMd5;
    }

    public void setResultSetIdMd5(String resultSetIdMd5)
    {
	this.resultSetIdMd5 = resultSetIdMd5;
	try
	{
	    QueryLog resultsetInfo = searchLogClient.getResultsetIdFromMd5Value(resultSetIdMd5);
	    this.resultSetId = resultsetInfo.getResultsetId();
	    setResultSetView(resultsetInfo.getSearchType());
	    setQueryDate(resultsetInfo.getTimestamp());
	    setQuery(resultsetInfo.getQuery());
	}
	catch(ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    System.out.println(e.getMessage());
	}
    }
}

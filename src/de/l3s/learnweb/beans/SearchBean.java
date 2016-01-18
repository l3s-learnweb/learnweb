package de.l3s.learnweb.beans;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.sun.jersey.api.client.ClientHandlerException;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.FactSheet;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.Search;
import de.l3s.learnweb.SearchFilters;
import de.l3s.learnweb.SearchFilters.Filter;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.AddResourceBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.searchlogclient.Actions.ACTION;
import de.l3s.searchlogclient.SearchLogClient;

@ManagedBean
@SessionScoped
public class SearchBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8540469716342051138L;
    private static final Logger log = Logger.getLogger(SearchBean.class);

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Values from views stored here
    private String query = "";
    private String queryMode;
    private String queryFilters;
    private int page;

    private Search search;
    private InterWeb interweb;
    private SearchFilters searchFilters;

    private Resource selectedResource;
    private int selectedResourceTargetGroupId;

    private FactSheet graph = new FactSheet();
    private Search images;
    private MODE searchMode;
    private String view = "float"; // float, grid or list

    private boolean graphLoaded = false;
    private int counter = 0;

    /* For logging */
    private boolean logEnabled; //Only carry out search log functions if user is logged in
    private Date startTime; //To log when viewing time is started for a resource
    private Date endTime; //To log when viewing time is ended for a resource
    private transient SearchLogClient searchLogClient;
    private long batchrsStartTime; //To keep track of the start time for batch update of resultSet
    private long batchrsTimeout; //To keep track of timeout for batch update of resultSet
    boolean historyResourcesRetrieved; //To keep track if the previous resultSet resources have already been retrieved 
    HashSet<String> historyResources; //Stores resource URLs from a previous resultSet
    private int resultsetId; //For getting the result set ID of the past query posted for comparison of resultsets
    private int resultsetViewId;

    public SearchBean()
    {
	interweb = getLearnweb().getInterweb();
	searchMode = MODE.image; // default search mode
	queryMode = getPreference("SEARCH_ACTION", "web");

	searchFilters = new SearchFilters();
	searchFilters.setLanguageFilter(UtilBean.getUserBean().getLocaleCode());

	logEnabled = false;
	historyResources = new HashSet<String>();
    }

    public void preRenderView() throws SQLException
    {
	if(isAjaxRequest())
	{
	    return;
	}

	if(queryMode != null)
	{
	    String modeTemp = queryMode;
	    queryMode = null;

	    if(modeTemp.equals("web"))
		onSearchText();
	    else if(modeTemp.equals("image"))
		onSearchImage();
	    else if(modeTemp.equals("video"))
		onSearchVideo();
	}

	getFacesContext().getExternalContext().setResponseCharacterEncoding("UTF-8");
	// stop caching (back button problem)
	HttpServletResponse response = (HttpServletResponse) getFacesContext().getExternalContext().getResponse();

	response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
	response.setDateHeader("Expires", 0); // Proxies.
    }

    // -------------------------------------------------------------------------

    public String onSearchVideo()
    {
	searchMode = MODE.video;
	setView("grid");
	return onSearch();
    }

    public String onSearchImage()
    {
	searchMode = MODE.image;
	setView("float");
	return onSearch();
    }

    public String onSearchText()
    {
	searchMode = MODE.web;
	setView("list");
	return onSearch();
    }

    public String onSearch()
    {
	Date tempDate = new Date(); //For getting the query timestamp 
	String onSearchTimestamp = DEFAULT_DATE_FORMAT.format(tempDate);
	searchLogClient = getLearnweb().getSearchlogClient();

	int userId = getUser() == null ? -1 : getUser().getId();
	if(userId > 0)
	{
	    logEnabled = true;
	}

	// search if a query is given and (it was not searched before or the query or searchmode has been changed)
	if(!isEmpty(query) && (null == search || !query.equals(search.getQuery()) || searchMode != search.getMode() || !StringUtils.equals(queryFilters, searchFilters.getFiltersString())))
	{

	    if(null != search)
		search.stop();

	    setPreference("search_action", searchMode.name());

	    historyResourcesRetrieved = false;

	    long start = System.currentTimeMillis();
	    try
	    {
		//Posting the batch of resources stored part of the result set corresponding to the previous query
		searchLogClient.pushBatchResultsetList();
		searchLogClient.postResourceLog();
		searchLogClient.passUpdateResultset();
		searchLogClient.pushTagList();

		//Logs the query posted by the user along with the time stamp, sessionId, groupId and search type.
		searchLogClient.passUserQuery(query, searchMode.toString(), userId, getUser().getActiveGroupId(), getSessionId(), onSearchTimestamp);
		searchLogClient.changeTagNamesListResultsetIds();
	    }
	    catch(ClientHandlerException e)
	    {
		System.out.println("Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		System.out.println(e.getMessage());
	    }

	    batchrsStartTime = new Date().getTime();

	    log.debug("Search log client braucht " + (System.currentTimeMillis() - start) + "ms");

	    page = 1;
	    searchFilters.setFiltersFromString(queryFilters);
	    search = new Search(interweb, query, searchFilters, getUser());
	    search.setMode(searchMode);
	    LinkedList<ResourceDecorator> res = search.getResourcesByPage(1);

	    queryFilters = null;

	    try
	    {
		searchLogClient.saveSERP(1, searchMode, res);
	    }
	    catch(ClientHandlerException e)
	    {
		System.out.println("Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		System.out.println(e.getMessage());
	    }
	    graphLoaded = false;

	    log(Action.searching, 0, query);
	}

	return "/lw/search.xhtml?faces-redirect=true";
    }

    // TODO We use it somewhere?
    //For comparing the resources in the current result set with another result set from a similar query in the past
    public String compareHistoryResources()
    {
	HashSet<String> searched = new HashSet<String>();
	if(resultsetId > 0)
	{
	    historyResources.clear();
	    try
	    {
		historyResources.addAll(searchLogClient.getResourceUrlsByResultsetId(resultsetId));
	    }
	    catch(ClientHandlerException e)
	    {
		System.out.println("Search Tracker service is down");
		return null;
	    }
	    catch(RuntimeException e)
	    {
		addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
		return null;
	    }
	    if(historyResources.size() > 0)
		historyResourcesRetrieved = true;
	    resultsetId = 0;
	}

	int historyResourcesSize = historyResources.size();
	int resourceCount = 0;
	for(ResourceDecorator resource : search.getResources())
	{
	    resource.setNewResource(false);
	    ++resourceCount;
	}

	// TODO does this really make sense?
	// shouldn't you check if a resource with the same url was part of the old resultset to check if a resource is new?
	while(resourceCount < historyResourcesSize)
	{
	    ++page;
	    LinkedList<ResourceDecorator> newResources = search.getResourcesByPage(page);
	    for(ResourceDecorator resource : newResources)
	    {
		if(resourceCount > historyResourcesSize)
		    break;

		resource.setNewResource(false);
		++resourceCount;
	    }
	}

	if(historyResourcesRetrieved)
	{
	    for(ResourceDecorator newResource : search.getResources())
	    {
		if(searched.size() == historyResources.size())
		{
		    historyResourcesRetrieved = false;
		    break;
		}
		else
		{
		    if(!searched.contains(newResource.getUrl()))
			searched.add(newResource.getUrl());

		    if(!historyResources.contains(newResource.getUrl()))
		    {
			newResource.setNewResource(true);
		    }
		}

	    }
	}
	return getTemplateDir() + "/search.xhtml?faces-redirect=true";
    }

    public LinkedList<ResourceDecorator> getNextPage()
    {
	if(!isSearched())
	    return null;

	log.debug("getNextPage");

	batchrsTimeout = new Date().getTime() - batchrsStartTime;
	LinkedList<ResourceDecorator> newResources = search.getResourcesByPage(page);

	if(logEnabled)
	{
	    try
	    {
		//Saves the resources returned for a page 
		searchLogClient.saveSERP(page, searchMode, newResources);
		//Saves the batch resources after a timeout of 10 minutes
		if(batchrsTimeout > 600000)
		{
		    searchLogClient.pushBatchResultsetList();
		    searchLogClient.postResourceLog();
		    searchLogClient.passUpdateResultset();
		    batchrsStartTime = new Date().getTime();
		}
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
	return newResources;
    }

    // -------------------------------------------------------------------------

    public void addSelectedResource()
    {
	Date date = new Date(); // To get the time stamp as to when the resource was saved.
	User user = getUser();
	if(null == user)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}

	try
	{
	    Resource newResource;

	    if(selectedResource.getId() == -1) // resource is not yet stored at the database
		newResource = selectedResource;
	    else
		// create a copy 
		newResource = selectedResource.clone();

	    newResource.setQuery(query);
	    newResource.setEmbeddedSize1Raw("");

	    // add resource to a group if selected
	    if(selectedResourceTargetGroupId != 0)
	    {
		newResource.setGroupId(selectedResourceTargetGroupId);
		user.setActiveGroup(selectedResourceTargetGroupId);
	    }

	    newResource = user.addResource(newResource);

	    // create thumbnails for the resource
	    if(newResource.getThumbnail2() == null || newResource.getThumbnail2().getFileId() == 0)
		new AddResourceBean.CreateThumbnailThread(newResource).start();

	    //Logs when a resource has been saved by the user to LearnWeb
	    if(logEnabled)
	    {
		try
		{
		    int tempresourceId = searchLogClient.getResourceIdByUrl(newResource.getUrl());
		    searchLogClient.saveResourceLog(user.getId(), date, ACTION.resource_saved, newResource.getUrl(), tempresourceId, newResource.getTitle(), newResource.getSource());
		    searchLogClient.addResourceSavedList(tempresourceId, newResource.getId());
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
	    user.setActiveGroup(selectedResourceTargetGroupId);
	    log(Action.adding_resource, newResource.getId());

	    // add query as tag 
	    //newResource.addTag(query, user);

	    addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", newResource.getTitle());
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
	}

    }

    // -------------------------------------------------------------------------
    /**
     * This method logs a resource click event.
     */
    public void logResourceOpened()
    {
	try
	{
	    startTime = new Date(); //Recording the beginning of viewing time for a resource.
	    int tempResourceId = getParameterInt("resource_id");

	    Resource resource = search.getResourceByTempId(tempResourceId);

	    if(null == resource)
		throw new InvalidParameterException("unknown resource id:" + tempResourceId);

	    int userId = getUser() == null ? -1 : getUser().getId(); // search can be anonymous
	    if(logEnabled)
	    {
		try
		{
		    searchLogClient.saveResourceLog(userId, startTime, ACTION.resource_click, resource.getUrl(), tempResourceId, resource.getTitle(), resource.getSource());
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
	catch(Throwable e)
	{
	    e.printStackTrace();
	}
    }

    /**
     * This method keeps track of the end of the viewing time for a particular resource
     */
    public void logEndTime()
    {
	endTime = new Date();
	String tempResourceId = getParameter("resource_id");
	Resource resource = search.getResourceByTempId(Integer.parseInt(tempResourceId));

	if(null == resource)
	    throw new InvalidParameterException("unknown resource id:" + tempResourceId);

	try
	{
	    searchLogClient.passViewingTime(Integer.parseInt(tempResourceId), startTime, endTime);
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

    // -------------------------------------------------------------------------

    /**
     * True if a the user has started a search request
     * 
     * @return
     */
    public boolean isSearched()
    {
	return search != null;
    }

    public void loadNextPage()
    {
	page++;
    }

    public void loadInterwebCounts()
    {
	if(search != null)
	{
	    search.getResourcesByPage(2);
	}

	log.info("loadInterwebCounts");
    }

    public List<Filter> getAvailableFilters()
    {
	return searchFilters.getAvailableFilters();
    }

    public String getSearchFilters()
    {
	return searchFilters.getFiltersString();
    }

    public String getSearchMode()
    {
	return searchMode.name();
    }

    public Search getSearch()
    {
	return search;
    }

    public int getPage()
    {
	return page;
    }

    public String getQuery()
    {
	return query;
    }

    public void setQuery(String query)
    {
	this.query = query;
    }

    public String getQueryMode()
    {
	return queryMode;
    }

    public void setQueryMode(String queryMode)
    {
	if(queryMode != null && !queryMode.isEmpty())
	{
	    this.queryMode = queryMode;
	}
    }

    public String getQueryFilters()
    {
	return queryFilters;
    }

    public void setQueryFilters(String queryFilters)
    {
	this.queryFilters = queryFilters;
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

	    Resource resource = search.getResourceByTempId(tempResourceId);

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
	Date timestamp = new Date(); //To record when the resource dialog box is opened.
	System.out.println(selectedResource.getTitle());
	this.selectedResource = selectedResource;

	//logs a resource dialog open event
	if(logEnabled)
	{
	    try
	    {
		int userId = getUser() == null ? -1 : getUser().getId();
		int tempResourceId = searchLogClient.getResourceIdByUrl(selectedResource.getUrl());
		searchLogClient.saveResourceLog(userId, timestamp, ACTION.resource_dialog_open, selectedResource.getUrl(), tempResourceId, selectedResource.getTitle(), selectedResource.getSource());
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

    public int getSelectedResourceTargetGroupId()
    {
	return selectedResourceTargetGroupId;
    }

    public void setSelectedResourceTargetGroupId(int selectedResourceTargetGroupId)
    {
	this.selectedResourceTargetGroupId = selectedResourceTargetGroupId;
    }

    public String getView()
    {
	return view;
    }

    public void setView(String view)
    {
	this.view = view;
    }

    public FactSheet getGraph()
    {
	return graph;
    }

    public boolean isGraphLoaded()
    {
	return graphLoaded;
    }

    public Search getImages()
    {
	return images;
    }

    public int getCounter()
    {
	return counter++;
    }

    public HashSet<String> getHistoryResources()
    {
	return historyResources;
    }

    public int getResultsetId()
    {
	return resultsetId;
    }

    public void setResultsetId(int resultsetId)
    {
	this.resultsetViewId = resultsetId;
	this.resultsetId = resultsetId;
    }

    public int getResultsetViewId()
    {
	return resultsetViewId;
    }

    public void updateSearchResources()
    {
	try
	{
	    searchLogClient.pushBatchResultsetList();
	    searchLogClient.postResourceLog();
	    searchLogClient.passUpdateResultset();
	    searchLogClient.flushLists();
	    for(ResourceDecorator resource : search.getResources())
	    {
		resource.setNewResource(false);
	    }
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

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException
    {
	inputStream.defaultReadObject();

	// restore transient objects
	searchLogClient = getLearnweb().getSearchlogClient();
    }

    public void generateKnowledgeGraph()
    {
	try
	{
	    if(query == null || query.length() < 2)
		return;

	    graph = new FactSheet(query);
	    if(graph.getEntities().size() == 0)
	    {
		log.info("No DBPedia entry found for: " + query);
		return;
	    }
	    String label = graph.getEntities().get(0).getLabel();

	    images = new Search(getLearnweb().getInterweb(), label, new SearchFilters(), getUser());
	    images.setMode(MODE.image);
	    //images.setService(SERVICE.Ipernity, SERVICE.Flickr);
	    images.setResultsPerService(10);
	    images.getResourcesByPage(1);
	    //images.getResourcesByPage(2);			

	    graphLoaded = true;
	}
	catch(Exception e)
	{
	    log.error("Can't create FactSheet", e);
	}
    }
}

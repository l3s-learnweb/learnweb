package de.l3s.learnweb.beans;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import com.sun.jersey.api.client.ClientHandlerException;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.FactSheet;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.Search;
import de.l3s.learnweb.Search.GroupedResources;
import de.l3s.learnweb.SearchFilters;
import de.l3s.learnweb.SearchFilters.FILTERS;
import de.l3s.learnweb.SearchFilters.Filter;
import de.l3s.learnweb.SearchFilters.FilterItem;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.SearchFilters.SERVICE;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.AddResourceBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.searchlogclient.Actions.ACTION;
import de.l3s.searchlogclient.SearchLogClient;

@ManagedBean
@ViewScoped
public class SearchBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8540469716342051138L;
    private static final Logger log = Logger.getLogger(SearchBean.class);

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Values from views stored here
    private String query = "";
    private String queryMode;
    private String queryService;
    private String queryFilters;
    private int page;

    private Search search;
    private InterWeb interweb;
    private SearchFilters searchFilters;

    private Search metaSearch;
    private SearchFilters metaFilters;

    private Resource selectedResource;
    private TreeNode selectedNode;
    private int selectedResourceTargetGroupId = 0;
    private int selectedResourceTargetFolderId = 0;

    private FactSheet graph = new FactSheet();
    private Search images;
    private MODE searchMode;
    private SERVICE searchService;
    private String view = "float"; // float, grid or list

    private boolean graphLoaded = false;
    private int counter = 0;

    private int minResourcesPerGroup = 2;

    /* For logging */
    private boolean logEnabled; //Only carry out search log functions if user is logged in
    private Date startTime; //To log when viewing time is started for a resource
    private Date endTime; //To log when viewing time is ended for a resource
    private transient SearchLogClient searchLogClient;
    // private long batchrsStartTime; //To keep track of the start time for batch update of resultSet
    // private long batchrsTimeout; //To keep track of timeout for batch update of resultSet
    boolean historyResourcesRetrieved; //To keep track if the previous resultSet resources have already been retrieved 
    HashSet<String> historyResources; //Stores resource URLs from a previous resultSet
    private int resultsetId; //For getting the result set ID of the past query posted for comparison of resultsets
    private int resultsetViewId;

    private List<GroupedResources> resourcesGroupedBySource = null;
    private List<FilterItem> availableSources = null;

    public SearchBean()
    {
	interweb = getLearnweb().getInterweb();
	searchMode = MODE.image; // default search mode
	queryMode = getPreference("SEARCH_ACTION", "text");

	searchFilters = new SearchFilters();
	searchFilters.setLanguageFilter(UtilBean.getUserBean().getLocaleCode());

	metaFilters = new SearchFilters();

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

	    if(modeTemp.equals("text") || modeTemp.equals("web"))
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
	searchMode = MODE.text;
	setView("list");
	return onSearch();
    }

    public String onSearch()
    {
	Date tempDate = new Date(); //For getting the query timestamp 
	String onSearchTimestamp = DEFAULT_DATE_FORMAT.format(tempDate);
	searchLogClient = getLearnweb().getSearchlogClient();

	int userId = getUser() == null ? -1 : getUser().getId();

	/* TODO enable when log service is working again
	if(userId > 0)
	{
	    logEnabled = true;
	}
	*/
	// search if a query is given and (it was not searched before or the query or searchmode has been changed)
	if(!isEmpty(query) && (null == search || !query.equals(search.getQuery()) || searchMode != search.getMode() || !queryService.equals(searchService.name()) || !StringUtils.equals(queryFilters, searchFilters.getFiltersString())))
	{
	    if(null != search)
		search.stop();

	    setSearchService(queryService);

	    setPreference("SEARCH_ACTION", searchMode.name());
	    setPreference("SEARCH_SERVICE_" + searchMode.name().toUpperCase(), searchService.name());

	    historyResourcesRetrieved = false;

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
		log.debug("Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		log.debug("Search log failed: " + e.getMessage());
	    }

	    page = 1;
	    search = new Search(interweb, query, searchFilters, getUser());
	    search.setMode(searchMode);
	    searchFilters.setFiltersFromString(queryFilters);
	    searchFilters.setFilter(FILTERS.service, searchService);

	    LinkedList<ResourceDecorator> res = search.getResourcesByPage(1);

	    resourcesGroupedBySource = null;
	    availableSources = null;

	    queryFilters = null;

	    try
	    {
		searchLogClient.saveSERP(1, searchMode, res);
	    }
	    catch(ClientHandlerException e)
	    {
		log.debug("Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		log.debug(e.getMessage());
	    }
	    graphLoaded = false;

	    log(Action.searching, 0, 0, query);
	}

	return "/lw/search.xhtml?faces-redirect=true";
    }

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
		log.debug("Search Tracker service is down");
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

	//log.debug("getNextPage");

	// don't log anything here.
	// this method will be called multiple times for each page

	return search.getResourcesByPage(page);
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
	    {
		newResource = selectedResource;
		if(newResource.getSource().equalsIgnoreCase("Bing")) //resource which is already saved in database already has wayback captures stored
		    getLearnweb().getWaybackCapturesLogger().logWaybackCaptures(newResource);
	    }
	    else
		// create a copy 
		newResource = selectedResource.clone();

	    newResource.setQuery(query);
	    newResource.setEmbeddedSize1Raw("");
	    //These metadata entries are not required while storing resource at the database
	    newResource.getMetadata().remove("first_timestamp");
	    newResource.getMetadata().remove("last_timestamp");

	    log.debug("Add resource); group: " + selectedResourceTargetGroupId + "; folder: " + selectedResourceTargetFolderId);

	    // add resource to a group if selected
	    if(selectedResourceTargetGroupId != 0)
	    {
		newResource.setGroupId(selectedResourceTargetGroupId);
		user.setActiveGroup(selectedResourceTargetGroupId);

		if(selectedResourceTargetFolderId != 0)
		{
		    newResource.setFolderId(selectedResourceTargetFolderId);
		}
	    }
	    else
		newResource.setGroupId(selectedResourceTargetGroupId); //This is for resources to be added to MyResources

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
		    log.debug("Search Tracker service is down");
		}
		catch(RuntimeException e)
		{
		    log.debug(e.getMessage());
		}
	    }
	    user.setActiveGroup(selectedResourceTargetGroupId);
	    log(Action.adding_resource, selectedResourceTargetGroupId, newResource.getId(), "");

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
	if(!logEnabled)
	    return;

	try
	{
	    startTime = new Date(); //Recording the beginning of viewing time for a resource.
	    int tempResourceId = getParameterInt("resource_id");

	    Resource resource = search.getResourceByTempId(tempResourceId);

	    if(null == resource)
		throw new InvalidParameterException("unknown resource id:" + tempResourceId);

	    int userId = getUser() == null ? -1 : getUser().getId(); // search can be anonymous

	    try
	    {
		searchLogClient.saveResourceLog(userId, startTime, ACTION.resource_click, resource.getUrl(), tempResourceId, resource.getTitle(), resource.getSource());
	    }
	    catch(ClientHandlerException e)
	    {
		log.debug("Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		log.debug(e.getMessage());
	    }

	}
	catch(Throwable e)
	{
	    e.printStackTrace();
	}
    }

    public void logQuerySuggestion()
    {
	String query = getParameter("query");
	String suggestions = getParameter("suggestions");
	String market = getParameter("market");

	//log.debug("query: " + query + "; suggestions: " + suggestions + "; market: " + market);

	getLearnweb().getSuggestionLogger().log(query, market, suggestions, getSessionId(), getUser());
    }

    /**
     * This method keeps track of the end of the viewing time for a particular resource
     */
    public void logEndTime()
    {
	if(!logEnabled)
	    return;

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
	    log.debug("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    log.debug(e.getMessage());
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
    }

    public List<Filter> getAvailableFilters()
    {
	FILTERS[] except = { FILTERS.service };
	return searchFilters.getAvailableFilters(except);
    }

    public List<FilterItem> getAvailableSources()
    {
	if(availableSources == null || availableSources.size() == 0)
	{
	    availableSources = metaFilters.getAvailableSources(searchService);
	    Collections.sort(availableSources);
	}
	return availableSources;
    }

    public SERVICE getSearchService()
    {
	return searchService;
    }

    private void setSearchService(String service)
    {
	try
	{
	    if(service == null)
		throw new IllegalArgumentException();
	    searchService = SERVICE.valueOf(service);
	}
	catch(Exception e)
	{
	    if(searchMode == MODE.text)
		searchService = SERVICE.valueOf(getPreference("SEARCH_SERVICE_TEXT", "bing"));
	    else if(searchMode == MODE.image)
		searchService = SERVICE.valueOf(getPreference("SEARCH_SERVICE_IMAGE", "flickr"));
	    else if(searchMode == MODE.video)
		searchService = SERVICE.valueOf(getPreference("SEARCH_SERVICE_VIDEO", "youtube"));
	}

	queryService = searchService.name();
    }

    public Long getTotalFromCurrentService()
    {
	return searchFilters.getTotalResults() - search.getRemovedResourceCount();
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

    public String getQueryService()
    {
	return queryService;
    }

    public void setQueryService(String queryService)
    {
	if(queryService != null && !queryService.isEmpty())
	{
	    this.queryService = queryService;
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
	log.debug("getSelectedResource");
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
	    log.fatal(e);
	}
    }

    public void setSelectedResource(Resource selectedResource)
    {
	Date timestamp = new Date(); //To record when the resource dialog box is opened.
	log.debug("Selected resource: " + selectedResource.getTitle());
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
		log.debug("Search Tracker service is down");
	    }
	    catch(RuntimeException e)
	    {
		log.debug(e.getMessage());
	    }
	}

    }

    public TreeNode getSelectedNode()
    {
	return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode)
    {
	this.selectedNode = selectedNode;
    }

    public void onNodeSelect(NodeSelectEvent event)
    {
	String type = event.getTreeNode().getType();

	if(type.equals("group"))
	{
	    Group group = (Group) event.getTreeNode().getData();
	    if(group != null)
	    {
		selectedResourceTargetGroupId = group.getId();
		selectedResourceTargetFolderId = 0;
	    }
	}
	else if(type.equals("folder"))
	{
	    Folder folder = (Folder) event.getTreeNode().getData();
	    if(folder != null)
	    {
		selectedResourceTargetGroupId = folder.getGroupId();
		selectedResourceTargetFolderId = folder.getFolderId();
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

    public int getSelectedResourceTargetFolderId()
    {
	return selectedResourceTargetFolderId;
    }

    public void setSelectedResourceTargetFolderId(int selectedResourceTargetFolderId)
    {
	this.selectedResourceTargetFolderId = selectedResourceTargetFolderId;
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
	    log.debug("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    log.debug(e.getMessage());
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

	    SearchFilters filter = new SearchFilters();
	    images = new Search(getLearnweb().getInterweb(), label, filter, getUser());
	    images.setMode(MODE.image);
	    filter.setFilter(FILTERS.service, SERVICE.bing);

	    //images.setService(SERVICE.Ipernity, SERVICE.Flickr);
	    images.setResultsPerService(10);
	    images.getResourcesByPage(1);

	    /*
	    LinkedList<ResourceDecorator> resources = images.getResources();
	    
	    for(ResourceDecorator resource : resources)
	    System.out.println(resource.getSource());
	    //images.getResourcesByPage(2);			
	    */
	    graphLoaded = true;
	}
	catch(Exception e)
	{
	    log.error("Can't create FactSheet", e);
	}
    }

    /**
     * Formats wayback date format to short DateFormat.
     */
    public String formatDate(String timestamp) throws ParseException
    {
	DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, UtilBean.getUserBean().getLocale());
	DateFormat waybackDf = new SimpleDateFormat("yyyyMMddhhmmss");
	if(timestamp == null)
	    return null;

	return df.format(waybackDf.parse(timestamp));
    }

    public List<GroupedResources> getResourcesGroupedBySource()
    {
	if((resourcesGroupedBySource == null || resourcesGroupedBySource.isEmpty()) && StringUtils.isNotEmpty(query))
	{
	    metaSearch = new Search(interweb, query, metaFilters, getUser());
	    metaSearch.setMode(searchMode);
	    metaSearch.setResultsPerService(20);
	    metaSearch.setConfigGroupResultsByField("location");
	    metaSearch.setConfigResultsPerGroup(10);
	    metaSearch.getResourcesByPage(2);
	    resourcesGroupedBySource = metaSearch.getResourcesGroupedBySource(this.minResourcesPerGroup);
	    Collections.sort(resourcesGroupedBySource);
	}
	return resourcesGroupedBySource;
    }
}

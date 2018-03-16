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

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.FactSheet;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Resource.ResourceType;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.ResourceMetadataExtractor;
import de.l3s.learnweb.Search;
import de.l3s.learnweb.Search.GroupedResources;
import de.l3s.learnweb.SearchFilters;
import de.l3s.learnweb.SearchFilters.FILTERS;
import de.l3s.learnweb.SearchFilters.Filter;
import de.l3s.learnweb.SearchFilters.FilterItem;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.SearchFilters.SERVICE;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.AddResourceBean.CreateThumbnailThread;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;

@ManagedBean
@ViewScoped
public class SearchBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8540469716342051138L;
    private static final Logger log = Logger.getLogger(SearchBean.class);

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

    private final static int minResourcesPerGroup = 2;

    /* For logging */
    // private boolean logEnabled; //Only carry out search log functions if user is logged in
    //private Date startTime; //To log when viewing time is started for a resource
    // private Date endTime; //To log when viewing time is ended for a resource
    //private transient SearchLogClient searchLogClient;
    // private long batchrsStartTime; //To keep track of the start time for batch update of resultSet
    // private long batchrsTimeout; //To keep track of timeout for batch update of resultSet
    boolean historyResourcesRetrieved; //To keep track if the previous resultSet resources have already been retrieved
    HashSet<String> historyResources; //Stores resource URLs from a previous resultSet
    //private int resultsetId; //For getting the result set ID of the past query posted for comparison of resultsets
    //private int resultsetViewId;

    private List<GroupedResources> resourcesGroupedBySource = null;
    private List<FilterItem> availableSources = null;
    private Integer selectedResourceTempId; // temp Id of the selected resource. Necessary because Interweb results have no unique id

    public SearchBean()
    {
        interweb = getLearnweb().getInterweb();
        searchMode = MODE.image; // default search mode
        queryMode = getPreference("SEARCH_ACTION", "text");

        searchFilters = new SearchFilters();

        metaFilters = new SearchFilters();

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
        // search if a query is given and (it was not searched before or the query or searchmode has been changed)
        if(!isEmpty(query) && (null == search || !query.equals(search.getQuery()) || searchMode != search.getMode() || !queryService.equals(searchService.name()) || !StringUtils.equals(queryFilters, searchFilters.getFiltersString())))
        {
            if(null != search)
                search.stop();

            setSearchService(queryService);

            setPreference("SEARCH_ACTION", searchMode.name());
            setPreference("SEARCH_SERVICE_" + searchMode.name().toUpperCase(), searchService.name());

            page = 1;

            search = new Search(interweb, query, searchFilters, getUser());
            search.setMode(searchMode);
            searchFilters.setFiltersFromString(queryFilters);
            searchFilters.setFilter(FILTERS.service, searchService);
            searchFilters.setLanguageFilter(UtilBean.getUserBean().getLocaleCode());

            search.logQuery(query, searchMode, searchService, searchFilters.getLanguageFilter(), queryFilters, getUser());
            search.getResourcesByPage(1); // load first page

            log(Action.searching, 0, search.getId(), query);

            resourcesGroupedBySource = null;
            availableSources = null;
            queryFilters = null;
            graphLoaded = false;
        }

        return "/lw/search.xhtml?faces-redirect=true";
    }

    //For comparing the resources in the current result set with another result set from a similar query in the past
    /*public String compareHistoryResources()
    {
        HashSet<String> searched = new HashSet<String>();
        if(resultsetId > 0)
        {
            historyResources.clear();
            try
            {
    
                //historyResources.addAll(getSearchLogClient().getResourceUrlsByResultsetId(resultsetId));
            }
            catch(Exception e)
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
    }*/

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
            {
                // create a copy
                newResource = selectedResource.clone();
            }

            newResource.setQuery(query);
            //These metadata entries are not required while storing resource at the database
            newResource.getMetadata().remove("first_timestamp");
            newResource.getMetadata().remove("last_timestamp");

            log.debug("Add resource); group: " + selectedResourceTargetGroupId + "; folder: " + selectedResourceTargetFolderId);

            // add resource to a group if selected
            newResource.setGroupId(selectedResourceTargetGroupId);
            newResource.setFolderId(selectedResourceTargetFolderId);
            user.setActiveGroup(selectedResourceTargetGroupId);

            newResource = user.addResource(newResource);

            // create thumbnails for the resource
            if(newResource.getThumbnail2() == null || newResource.getThumbnail2().getFileId() == 0)
                new AddResourceBean.CreateThumbnailThread(newResource).start();

            // we need to check whether a Bing result is a PDF, Word or other document
            if(newResource.getOriginalResourceId() == 0 && (newResource.getType().equals(ResourceType.website) || newResource.getType().equals(ResourceType.text)) && newResource.getSource().equals("Bing"))
            {
                log.debug("Extracting info from given url...");
                ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();
                FileInfo fileInfo = rme.getFileInfo(newResource.getUrl());
                rme.processFileResource(newResource, fileInfo);
            }

            log.debug("Creating thumbnails from given url...");
            Thread createThumbnailThread = new CreateThumbnailThread(newResource);
            createThumbnailThread.start();

            search.logResourceSaved(selectedResourceTempId, getUser(), newResource.getId());
            log(Action.adding_resource, selectedResourceTargetGroupId, newResource.getId(), search.getId() + " - " + selectedResourceTempId);

            addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", newResource.getTitle());
        }
        catch(Exception e)
        {
            addFatalMessage(e);
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
            int tempResourceId = getParameterInt("resource_id");

            search.logResourceClicked(tempResourceId, getUser());
        }
        catch(Throwable e)
        {
            log.error("Can't log resource opened event", e);
        }
    }

    public void logQuerySuggestion()
    {
        try
        {
            String query = getParameter("query");
            String suggestions = getParameter("suggestions");
            String market = getParameter("market");

            getLearnweb().getSuggestionLogger().log(query, market, suggestions, getSessionId(), getUser());
        }
        catch(Throwable e)
        {
            log.error("Can't log query suggestion", e);
        }
    }

    /**
     * This method keeps track of the end of the viewing time for a particular resource
     */
    public void logEndTime()
    {
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
        return selectedResource;
    }

    public void setSelectedResource()
    {
        try
        {
            selectedResourceTempId = getParameterInt("resource_id");

            Resource resource = search.getResourceByTempId(selectedResourceTempId);

            if(null == resource)
                throw new InvalidParameterException("unknown resource id:" + selectedResourceTempId);

            setSelectedResource(resource);
        }
        catch(Throwable e)
        {
            log.fatal(e);
        }
    }

    public void setSelectedResource(Resource selectedResource)
    {
        this.selectedResource = selectedResource;
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
                selectedResourceTargetFolderId = folder.getId();
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

    /*public HashSet<String> getHistoryResources()
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
    }*/

    /**
     * Used by old search result history
     */
    public void updateSearchResources()
    {
        for(ResourceDecorator resource : search.getResources())
        {
            resource.setNewResource(false);
        }
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException
    {
        inputStream.defaultReadObject();
    }

    public void generateKnowledgeGraph()
    {
        try
        {
            if(query == null || query.length() < 2)
                return;

            /*
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


            graphLoaded = true;
            */
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
            resourcesGroupedBySource = metaSearch.getResourcesGroupedBySource(minResourcesPerGroup);
            Collections.sort(resourcesGroupedBySource);
        }
        return resourcesGroupedBySource;
    }
}

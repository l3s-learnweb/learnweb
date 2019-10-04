package de.l3s.learnweb.resource.search;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.AddResourceBean.CreateThumbnailThread;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.ResourceType;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceMetadataExtractor;
import de.l3s.learnweb.resource.SERVICE;
import de.l3s.learnweb.resource.search.Search.GroupedResources;
import de.l3s.learnweb.resource.search.SearchFilters.FILTERS;
import de.l3s.learnweb.resource.search.SearchFilters.Filter;
import de.l3s.learnweb.resource.search.SearchFilters.FilterItem;
import de.l3s.learnweb.resource.search.SearchFilters.MODE;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class SearchBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8540469716342051138L;
    private static final Logger log = Logger.getLogger(SearchBean.class);

    // Values from views are stored here
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

    private ResourceDecorator selectedResource;
    private TreeNode selectedNode;
    private int selectedResourceTargetGroupId = 0;
    private int selectedResourceTargetFolderId = 0;

    private Search images;
    private MODE searchMode;
    private SERVICE searchService;
    private String view = "float"; // float, grid or list

    private int counter = 0;

    private final static int minResourcesPerGroup = 2;

    private List<GroupedResources> resourcesGroupedBySource = null;
    private List<FilterItem> availableSources = null;

    public SearchBean()
    {
        log.debug("SearchBean()");

        interweb = getLearnweb().getInterweb();
        searchMode = MODE.image; // default search mode
        queryMode = getPreference("SEARCH_ACTION", "text");

        searchFilters = new SearchFilters();

        metaFilters = new SearchFilters();

        //historyResources = new HashSet<String>();
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
        forceRevalidation();
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
        }

        return "/lw/search.xhtml?faces-redirect=true";
    }

    public LinkedList<ResourceDecorator> getNextPage()
    {
        if(!isSearched())
            return null;

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
                newResource = selectedResource.getResource();
                if(newResource.getSource().equals(SERVICE.bing)) //resource which is already saved in database already has wayback captures stored
                    getLearnweb().getWaybackCapturesLogger().logWaybackCaptures(newResource);
            }
            else
            {
                // create a copy
                newResource = selectedResource.getResource().clone();
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

            // we need to check whether a Bing result is a PDF, Word or other document
            if(newResource.getOriginalResourceId() == 0 && (newResource.getType().equals(ResourceType.website) || newResource.getType().equals(ResourceType.text)) && newResource.getSource().equals(SERVICE.bing))
            {
                log.debug("Extracting info from given url...");
                ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();
                FileInfo fileInfo = rme.getFileInfo(newResource.getUrl());
                rme.processFileResource(newResource, fileInfo);
            }

            newResource = user.addResource(newResource);

            log.debug("Creating thumbnails from given url...");
            Thread createThumbnailThread = new CreateThumbnailThread(newResource);
            createThumbnailThread.start();

            search.logResourceSaved(selectedResource.getRank(), getUser(), newResource.getId());
            log(Action.adding_resource, selectedResourceTargetGroupId, newResource.getId(), search.getId() + " - " + selectedResource.getRank());

            addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", newResource.getTitle());
        }
        catch(Exception e)
        {
            addErrorMessage(e);
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
        if(StringUtils.isNotEmpty(queryMode))
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
        if(StringUtils.isNotEmpty(queryService))
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

    public ResourceDecorator getSelectedResource()
    {
        return selectedResource;
    }

    public void setSelectedResource(ResourceDecorator decoratedResource)
    {
        this.selectedResource = decoratedResource;
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

        // TODO Dupe
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

    public Search getImages()
    {
        return images;
    }

    public int getCounter()
    {
        return counter++;
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException
    {
        inputStream.defaultReadObject();
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
            metaSearch.getResourcesByPage(2); // fetch resources
            resourcesGroupedBySource = metaSearch.getResourcesGroupedBySource(minResourcesPerGroup);
            Collections.sort(resourcesGroupedBySource);
        }
        return resourcesGroupedBySource;
    }
}

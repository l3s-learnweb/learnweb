package de.l3s.learnweb.resource.search;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;
import org.primefaces.PrimeFaces;

import de.l3s.interwebj.client.InterWeb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.SelectLocationBean;
import de.l3s.learnweb.resource.archive.WaybackCapturesLogger;
import de.l3s.learnweb.resource.search.Search.GroupedResources;
import de.l3s.learnweb.resource.search.filters.Filter;
import de.l3s.learnweb.resource.search.filters.FilterType;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

@Named
@ViewScoped
public class SearchBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 8540469716342051138L;
    private static final Logger log = LogManager.getLogger(SearchBean.class);

    private static final int MIN_RESOURCES_PER_GROUP = 2;

    // Values from views are stored here
    private String query = "";
    private String queryMode;
    private String queryService;
    private String queryFilters;
    private int page;

    private Search search;
    private InterWeb interweb;
    private SearchFilters searchFilters;

    private ResourceDecorator selectedResource;

    private SearchMode searchMode;
    private ResourceService searchService;
    private String view = "float"; // float, grid or list

    private int counter = 0;
    private List<GroupedResources> resourcesGroupedBySource;

    @PostConstruct
    public void init() {
        interweb = getLearnweb().getInterweb();
        searchFilters = new SearchFilters(SearchMode.text);
    }

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());
        log.debug("mode/action: {}; filter: {} - service: {}; query:{}", queryMode, queryFilters, queryService, query);

        if (null == queryMode) {
            queryMode = getPreference("SEARCH_ACTION", "text");
        }

        if ("text".equals(queryMode) || "web".equals(queryMode)) {
            searchMode = SearchMode.text;
            setView("list");
        } else if ("image".equals(queryMode)) {
            searchMode = SearchMode.image;
            setView("float");
        } else if ("video".equals(queryMode)) {
            searchMode = SearchMode.video;
            setView("grid");
        }

        onSearch();

        Servlets.setNoCacheHeaders(Faces.getResponse());
    }

    // -------------------------------------------------------------------------

    public String onSearch() {
        // search if a query is given and (it was not searched before or the query or search mode has been changed)
        if (!StringUtils.isEmpty(query) && (null == search || !query.equals(search.getQuery()) || searchMode != search.getMode() || !queryService.equals(searchService.name()))) {
            if (null != search) {
                search.stop();
            }

            setSearchService(queryService);

            setPreference("SEARCH_ACTION", searchMode.name());
            setPreference("SEARCH_SERVICE_" + searchMode.name().toUpperCase(), searchService.name());

            page = 1;

            search = new Search(interweb, query, searchFilters, getUser());
            search.setMode(searchMode);
            searchFilters.setFilters(queryFilters);
            searchFilters.setFilter(FilterType.service, searchService.name());
            searchFilters.setFilter(FilterType.language, getUserBean().getLocaleCode());

            if (config().isCollectSearchHistory()) {
                search.logQuery(query, searchService, searchFilters.getFilterValue(FilterType.language), queryFilters);
            }

            search.getResourcesByPage(1); // load first page

            log(Action.searching, 0, search.getId(), query);

            resourcesGroupedBySource = null;
        }

        return "/lw/search.xhtml?faces-redirect=true";
    }

    public List<ResourceDecorator> getNextPage() {
        if (!isSearched()) {
            return null;
        }

        // don't log anything here.
        // this method will be called multiple times for each page
        return search.getResourcesByPage(page);
    }

    // -------------------------------------------------------------------------

    public void addSelectedResource() {
        User user = getUser();
        if (null == user) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        Resource newResource = null;
        try {
            if (selectedResource.getId() == 0) { // resource is not yet stored at the database
                newResource = selectedResource.getResource();
                if (newResource.getService() == ResourceService.bing) { // resource which is already saved in database already has wayback captures stored
                    Beans.getInstance(WaybackCapturesLogger.class).logWaybackCaptures(newResource);
                }
            } else {
                // create a copy
                newResource = new Resource(selectedResource.getResource());
            }

            newResource.setQuery(query);
            // These metadata entries are not required while storing resource at the database
            newResource.getMetadata().remove("first_timestamp");
            newResource.getMetadata().remove("last_timestamp");

            // add resource to a group if selected
            SelectLocationBean selectLocationBean = Beans.getInstance(SelectLocationBean.class);
            newResource.setGroup(selectLocationBean.getTargetGroup());
            newResource.setFolder(selectLocationBean.getTargetFolder());
            newResource.setUser(user);

            log.debug("Add resource; group: {}; folder: {}", newResource.getGroupId(), newResource.getFolderId());

            // we need to check whether a Bing result is a PDF, Word or other document
            if (newResource.getOriginalResourceId() == 0 && newResource.getService() == ResourceService.bing
                && (newResource.getType() == ResourceType.website || newResource.getType() == ResourceType.text)) {
                log.debug("Extracting info from given url...");
                FileInfo fileInfo = getLearnweb().getResourceMetadataExtractor().getFileInfo(newResource.getUrl());
                getLearnweb().getResourceMetadataExtractor().processFileResource(newResource, fileInfo);
            }

            newResource.save();

            log.debug("Creating thumbnails from given url...");
            Thread createThumbnailThread = new ResourcePreviewMaker.CreateThumbnailThread(newResource);
            createThumbnailThread.start();

            if (search != null) {
                search.logResourceSaved(selectedResource.getRank(), getUser(), newResource.getId());
                log(Action.adding_resource, newResource.getGroupId(), newResource.getId(), search.getId() + " - " + selectedResource.getRank());
            }

            addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", newResource.getTitle());
        } catch (RuntimeException | IOException e) {
            String details = "resource: " + newResource + "; selectedResource: " + selectedResource;
            addErrorMessage(details, e);
        }
    }

    public void commandGetResourceDetails() {
        Map<String, String> params = Faces.getRequestParameterMap();
        int rank = Integer.parseInt(params.get("resourceRank"));

        ResourceDecorator resource = search.getResourceByRank(rank);
        setSelectedResource(resource);

        PrimeFaces.current().ajax().addCallbackParam("slideIndex", params.get("slideIndex"));
        PrimeFaces.current().ajax().addCallbackParam("resourceRank", resource.getRank());

        if (resource.getEmbeddedCode() != null) {
            PrimeFaces.current().ajax().addCallbackParam("embeddedCode", resource.getEmbeddedCode());
        } else {
            PrimeFaces.current().ajax().addCallbackParam("embeddedCode", "<img src=\"" + resource.getResource().getThumbnailLargest() + "\" />");
        }
    }

    public void commandOnResourceSelect() {
        Map<String, String> params = Faces.getRequestParameterMap();
        int rank = Integer.parseInt(params.get("resourceRank"));

        ResourceDecorator resource = search.getResourceByRank(rank);
        setSelectedResource(resource);
    }

    /**
     * This method logs a resource click event.
     */
    public void commandOnResourceClick() {
        try {
            Map<String, String> params = Faces.getRequestParameterMap();
            int tempResourceId = Integer.parseInt(params.get("resourceId"));

            search.logResourceClicked(tempResourceId, getUser());
        } catch (Exception e) {
            log.error("Can't log resource opened event", e);
        }
    }

    /**
     * True if a the user has started a search request.
     */
    public boolean isSearched() {
        return search != null;
    }

    public void commandLoadNextPage() {
        page++;
    }

    public Collection<Filter> getAvailableFilters() {
        FilterType[] except = {FilterType.service};
        return searchFilters.getAvailableFilters(except);
    }

    public ResourceService getSearchService() {
        return searchService;
    }

    private void setSearchService(String service) {
        try {
            if (service == null) {
                throw new IllegalArgumentException();
            }
            searchService = ResourceService.valueOf(service);
        } catch (Exception e) {
            if (searchMode == SearchMode.text) {
                searchService = ResourceService.valueOf(getPreference("SEARCH_SERVICE_TEXT", "bing"));
            } else if (searchMode == SearchMode.image) {
                searchService = ResourceService.valueOf(getPreference("SEARCH_SERVICE_IMAGE", "flickr"));
            } else if (searchMode == SearchMode.video) {
                searchService = ResourceService.valueOf(getPreference("SEARCH_SERVICE_VIDEO", "youtube"));
            }
        }

        queryService = searchService.name();
    }

    public Long getTotalFromCurrentService() {
        return searchFilters.getTotalResults() - search.getRemovedResourceCount();
    }

    public SearchFilters getSearchFilters() {
        return searchFilters;
    }

    public String getSearchMode() {
        return searchMode.name();
    }

    public String createFilterUrl(FilterType filterType, String value) {
        StringBuilder sb = new StringBuilder();

        int next = -1;
        if (StringUtils.isNotBlank(queryFilters)) {
            boolean startWith = queryFilters.startsWith(filterType.name() + ':');
            int index = startWith ? 0 : queryFilters.indexOf(',' + filterType.name() + ':');
            next = queryFilters.indexOf(',', index + 1);

            if (index > 0) {
                sb.append(queryFilters, 0, index);
            } else if (index == -1) {
                sb.append(queryFilters);
                next = -1;
            }
        }

        if (StringUtils.isNotBlank(value)) {
            if (sb.length() != 0) {
                sb.append(':');
            }
            sb.append(filterType.name()).append(':').append(filterType.isEncodeBase64() ? StringHelper.encodeBase64(value) : value);
        }

        if (next != -1) {
            sb.append(queryFilters, next, queryFilters.length());
        }

        return sb.toString();
    }

    public Search getSearch() {
        return search;
    }

    public int getPage() {
        return page;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(String queryMode) {
        if (StringUtils.isNotEmpty(queryMode)) {
            this.queryMode = queryMode;
        }
    }

    public String getQueryService() {
        return queryService;
    }

    public void setQueryService(String queryService) {
        if (StringUtils.isNotEmpty(queryService)) {
            this.queryService = queryService;
        }
    }

    public String getQueryFilters() {
        return queryFilters;
    }

    public void setQueryFilters(String queryFilters) {
        this.queryFilters = queryFilters;
    }

    public ResourceDecorator getSelectedResource() {
        return selectedResource;
    }

    public void setSelectedResource(ResourceDecorator decoratedResource) {
        this.selectedResource = decoratedResource;
    }

    public String getView() {
        return view;
    }

    private void setView(String view) {
        this.view = view;
    }

    public int getCounter() {
        return counter++;
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
    }

    public List<GroupedResources> getResourcesGroupedBySource() {
        if (StringUtils.isNoneBlank(query) && (resourcesGroupedBySource == null || resourcesGroupedBySource.isEmpty())) {
            SearchFilters searchFilters = new SearchFilters(searchMode);
            Search metaSearch = new Search(interweb, query, searchFilters, getUser());
            metaSearch.setMode(searchMode);
            metaSearch.setResultsPerService(32);
            metaSearch.setConfigGroupResultsByField("source");
            metaSearch.setConfigResultsPerGroup(10);
            searchFilters.setFilter(FilterType.language, getUserBean().getLocaleCode());
            metaSearch.getResourcesByPage(2); // fetch resources
            resourcesGroupedBySource = metaSearch.getResourcesGroupedBySource(MIN_RESOURCES_PER_GROUP, searchService);
            Collections.sort(resourcesGroupedBySource);
        }
        return resourcesGroupedBySource;
    }
}

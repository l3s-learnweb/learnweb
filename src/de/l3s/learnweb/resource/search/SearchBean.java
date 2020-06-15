package de.l3s.learnweb.resource.search;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.primefaces.PrimeFaces;

import de.l3s.interwebj.client.InterWeb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceMetadataExtractor;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.SelectLocationBean;
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
    private final InterWeb interweb;
    private final SearchFilters searchFilters;

    private ResourceDecorator selectedResource;

    private SearchMode searchMode;
    private ResourceService searchService;
    private String view = "float"; // float, grid or list

    private int counter = 0;
    private List<GroupedResources> resourcesGroupedBySource;

    public SearchBean() {
        interweb = getLearnweb().getInterweb();
        searchFilters = new SearchFilters(SearchMode.text);
    }

    public void onLoad() {
        log.debug("mode/action: " + queryMode + "; filter: " + queryFilters + " - service: " + queryService + "; query:" + query);

        if (isAjaxRequest() || !isLoggedIn()) {
            return;
        }

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

        forceRevalidation();
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

            search.logQuery(query, searchService, searchFilters.getFilterValue(FilterType.language), queryFilters);
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

        try {
            Resource newResource;

            if (selectedResource.getId() == -1) { // resource is not yet stored at the database
                newResource = selectedResource.getResource();
                if (newResource.getSource() == ResourceService.bing) { // resource which is already saved in database already has wayback captures stored
                    getLearnweb().getWaybackCapturesLogger().logWaybackCaptures(newResource);
                }
            } else {
                // create a copy
                newResource = selectedResource.getResource().clone();
            }

            newResource.setQuery(query);
            // These metadata entries are not required while storing resource at the database
            newResource.getMetadata().remove("first_timestamp");
            newResource.getMetadata().remove("last_timestamp");

            // add resource to a group if selected
            SelectLocationBean selectLocationBean = Beans.getInstance(SelectLocationBean.class);
            newResource.setGroup(selectLocationBean.getTargetGroup());
            newResource.setFolder(selectLocationBean.getTargetFolder());

            log.debug("Add resource); group: " + newResource.getGroupId() + "; folder: " + newResource.getFolderId());

            // we need to check whether a Bing result is a PDF, Word or other document
            if (newResource.getOriginalResourceId() == 0 && (newResource.getType() == ResourceType.website || newResource.getType() == ResourceType.text) && newResource.getSource() == ResourceService.bing) {
                log.debug("Extracting info from given url...");
                ResourceMetadataExtractor rme = getLearnweb().getResourceMetadataExtractor();
                FileInfo fileInfo = rme.getFileInfo(newResource.getUrl());
                rme.processFileResource(newResource, fileInfo);
                // TODO check if it would be better to use rme.processWebResource

            }

            newResource = user.addResource(newResource);

            log.debug("Creating thumbnails from given url...");
            Thread createThumbnailThread = new ResourcePreviewMaker.CreateThumbnailThread(newResource);
            createThumbnailThread.start();

            search.logResourceSaved(selectedResource.getRank(), getUser(), newResource.getId());
            log(Action.adding_resource, newResource.getGroupId(), newResource.getId(), search.getId() + " - " + selectedResource.getRank());

            addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", newResource.getTitle());
        } catch (RuntimeException | IOException | SQLException e) {
            addErrorMessage(e);
        }
    }

    public void getResourceDetailsCommand() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        int rank = Integer.parseInt(params.get("resourceRank"));

        ResourceDecorator resource = search.getResourceByRank(rank);
        setSelectedResource(resource);

        PrimeFaces.current().ajax().addCallbackParam("slideIndex", params.get("slideIndex"));
        PrimeFaces.current().ajax().addCallbackParam("resourceRank", resource.getRank());

        if (resource.getEmbedded() != null) {
            PrimeFaces.current().ajax().addCallbackParam("embeddedCode", resource.getEmbedded());
        } else {
            PrimeFaces.current().ajax().addCallbackParam("embeddedCode", resource.getResource().getLargestThumbnail().getHtml());
        }
    }

    public void selectResourceCommand() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        int rank = Integer.parseInt(params.get("resourceRank"));

        ResourceDecorator resource = search.getResourceByRank(rank);
        setSelectedResource(resource);
    }

    // -------------------------------------------------------------------------

    /**
     * This method logs a resource click event.
     */
    public void logResourceOpened() {
        try {
            int tempResourceId = getParameterInt("resource_id");

            search.logResourceClicked(tempResourceId, getUser());
        } catch (Exception e) {
            log.error("Can't log resource opened event", e);
        }
    }

    // -------------------------------------------------------------------------

    /**
     * True if a the user has started a search request.
     */
    public boolean isSearched() {
        return search != null;
    }

    public void loadNextPage() {
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
            if (sb.length() > 0) {
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
            metaSearch.setConfigGroupResultsByField("location");
            metaSearch.setConfigResultsPerGroup(10);
            searchFilters.setFilter(FilterType.language, getUserBean().getLocaleCode());
            metaSearch.getResourcesByPage(2); // fetch resources
            resourcesGroupedBySource = metaSearch.getResourcesGroupedBySource(MIN_RESOURCES_PER_GROUP, searchService);
            Collections.sort(resourcesGroupedBySource);
        }
        return resourcesGroupedBySource;
    }
}

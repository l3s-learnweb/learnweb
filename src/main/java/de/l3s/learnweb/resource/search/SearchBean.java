package de.l3s.learnweb.resource.search;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DialogFrameworkOptions;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.l3s.interweb.client.Interweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.HttpException;
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
import de.l3s.learnweb.resource.web.WebResource;
import de.l3s.learnweb.searchhistory.PKGraphDao;
import de.l3s.learnweb.searchhistory.JsonSharedObject;
import de.l3s.learnweb.searchhistory.PKGraph;
import de.l3s.learnweb.searchhistory.RecognisedEntity;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.DbpediaSpotlightService;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

@Named
@ViewScoped
public class SearchBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 8540469716342051138L;
    private static final Logger log = LogManager.getLogger(SearchBean.class);

    private static final int MIN_RESOURCES_PER_GROUP = 2;
    private static final int RESULT_LIMIT = 32;

    // Values from views are stored here
    private String query = "";
    private String queryMode;
    private String queryService;
    private String queryFilters;
    private int page;

    private Search search;
    private Interweb interweb;
    private SearchFilters searchFilters;

    private ResourceDecorator selectedResource;

    private SearchMode searchMode;
    private ResourceService searchService;
    private String view = "float"; // float, grid or list

    private int counter = 0;
    private List<GroupedResources> resourcesGroupedBySource;
    private Boolean isUserActive;
    private List<Boolean> snippetClicked;
    private List<String> recommendations;
    private boolean showRecommendations;
    private boolean showRelatedQueries;

    private transient String edurecRequest;
    private transient String suggestedEntries;

    @Inject
    private PKGraphDao pkGraphDao;
    @Inject
    private DbpediaSpotlightService dbpediaSpotlightService;

    @PostConstruct
    public void init() {
        interweb = getLearnweb().getInterweb();
        searchFilters = new SearchFilters(SearchMode.text);
    }

    public void onLoad() throws InterruptedException {
        BeanAssert.authorized(isLoggedIn());
        log.debug("mode/action: {}; filter: {} - service: {}; query:{}", queryMode, queryFilters, queryService, query);

        if (null == queryMode) {
            queryMode = getPreference("SEARCH_ACTION", getUser().getOrganisation().getDefaultSearchMode().name());
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
        showRecommendations = !getUser().getOrganisation().getOption(Organisation.Option.Search_Disable_recommendations);
        showRelatedQueries = !getUser().getOrganisation().getOption(Organisation.Option.Search_Disable_related_searches);

        isUserActive = false;
        snippetClicked = new ArrayList<>();
        for (int i = 0; i < RESULT_LIMIT; i++) {
            snippetClicked.add(false);
        }
    }

    // -------------------------------------------------------------------------

    private String onSearch() throws InterruptedException {
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
                try {
                    search.logQuery(query, searchService, searchFilters.getFilterValue(FilterType.language), queryFilters);
                } catch (Exception e) {
                    log.error("Could not log search query", e);
                }
            }

            search.getResourcesByPage(1); // load first page

            log(Action.searching, 0, search.getId(), query);

            resourcesGroupedBySource = null;

            if (showRecommendations) {
                createSearchRecommendation();
            }
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
                    Beans.getInstance(WaybackCapturesLogger.class).logWaybackCaptures((WebResource) newResource);
                }
            } else {
                // create a copy
                newResource = selectedResource.getResource().cloneResource();
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
            throw new HttpException("Failed to store resource: " + newResource + "; selectedResource: " + selectedResource, e);
        }
    }

    public void commandGetResourceDetails() {
        Map<String, String> params = Faces.getRequestParameterMap();
        int rank = Integer.parseInt(params.get("resourceRank"));

        ResourceDecorator resource = search.getResourceByRank(rank);
        setSelectedResource(resource);

        PrimeFaces.current().ajax().addCallbackParam("slideIndex", params.get("slideIndex"));
        PrimeFaces.current().ajax().addCallbackParam("resourceRank", resource.getRank());

        if (resource.getEmbeddedCode() != null && getUserBean().isVideoPreviewEnabled()) {
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

    public void suggestQueries() throws IOException, InterruptedException {
        DialogFrameworkOptions options = DialogFrameworkOptions.builder()
            .modal(true)
            .draggable(false)
            .resizable(false)
            .closeOnEscape(true)
            .onHide("const f = $('#navbar_form\\\\:searchfield'); if (f) {f.data.bypass=1};")
            .build();

        List<String> suggestedBing = getBingSuggestQueries(query);
        List<String> suggestedEduRec = getEduRecSuggestQueries(query);
        log.debug("Suggested queries: bing: {}, edurec: {}", suggestedBing, suggestedEduRec);

        final List<SuggestedQuery> queries = new ArrayList<>();
        if (suggestedBing != null) {
            int index = 1;
            suggestedBing = suggestedBing.subList(0, Math.min(5, suggestedBing.size()));
            for (String query : suggestedBing) {
                queries.add(new SuggestedQuery(index++, "bing", query));
            }
        }
        if (suggestedEduRec != null) {
            int index = 101;
            suggestedEduRec = suggestedEduRec.subList(0, Math.min(5, suggestedEduRec.size()));
            for (String query : suggestedEduRec) {
                queries.add(new SuggestedQuery(index++, "edurec", query));
            }
        }
        Collections.shuffle(queries);
        final List<SuggestedQuery> randQueries = new ArrayList<>();
        int index = 1;
        for (SuggestedQuery query : queries) {
            randQueries.add(new SuggestedQuery(query.id(), index++, query.source(), query.query()));
        }
        suggestedEntries = new Gson().toJson(randQueries);

        FacesContext.getCurrentInstance().getExternalContext().getFlash().put("queries", randQueries);
        PrimeFaces.current().dialog().openDynamic("/dialogs/suggestQueries.jsf", options, null);
    }

    public void onSuggestedQuerySelected(SelectEvent<SuggestedQuery> event) {
        SuggestedQuery query = event.getObject();
        log.debug("Selected suggested query: {}", query);
        pkGraphDao.insertSuggestedQuery(getUser(), getQuery(), query.query(), query.source(), query.index(), suggestedEntries, edurecRequest);

        Faces.redirect("/lw/search.jsf?action=" + queryMode + "&service=" + queryService + "&query=" + URLEncoder.encode(query.query(), StandardCharsets.UTF_8));
    }

    private List<String> getBingSuggestQueries(String query) throws IOException, InterruptedException {
        final URI requestUri = URI.create("https://api.bing.com/osjson.aspx?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder().GET().header("Content-type", "application/json").uri(requestUri).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            JsonArray jsonObject = JsonParser.parseString(response.body()).getAsJsonArray();
            JsonArray jsonArray = jsonObject.get(1).getAsJsonArray();
            List<String> queries = new ArrayList<>();
            for (JsonElement jsonElement : jsonArray) {
                queries.add(jsonElement.getAsString());
            }
            return queries;
        }

        return null;
    }

    private List<String> getEduRecSuggestQueries(String query) throws IOException, InterruptedException {
        final String requestUrl = "https://edurec.kevinhaller.dev/recommend/5/items";

        JsonArray nodesArray = new JsonArray();
        JsonObject recordObject = new JsonObject();
        recordObject.add("nodes", nodesArray);
        JsonObject rootObject = new JsonObject();
        rootObject.add("record", recordObject);

        PKGraph pkg = getUserBean().getUserPkg();
        JsonSharedObject request = pkg.prepareCollabRec(10, 10);
        if (request != null) {
            for (JsonSharedObject.Entity entity : request.getEntities()) {
                JsonObject graphNode = new JsonObject();
                graphNode.addProperty("uri", entity.getUri());
                graphNode.addProperty("query", entity.getQuery());
                graphNode.addProperty("weight", entity.getWeight());
                nodesArray.add(graphNode);
            }
        }
        edurecRequest = new Gson().toJson(rootObject);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.uri(URI.create(requestUrl));
        requestBuilder.header("Content-Type", "application/json");
        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(edurecRequest));

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        JsonObject jsonRoot = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray suggestions = jsonRoot.getAsJsonArray("list");

        List<String> queries = new ArrayList<>();
        for (JsonElement suggestion : suggestions) {
            final String value = suggestion.getAsJsonObject().get("iri").getAsString();
            queries.add(value.substring(value.lastIndexOf('/') + 1).replace('_', ' '));
        }

        return queries;
    }

    /**
     * This method logs a resource click event.
     */
    public void commandOnResourceClick() {
        try {
            Map<String, String> params = Faces.getRequestParameterMap();
            int tempResourceId = Integer.parseInt(params.get("resourceId"));
            isUserActive = true;
            if (!snippetClicked.get(search.getResources().indexOf(search.getResources().get(tempResourceId)))) {
                Resource resource = search.getResources().get(tempResourceId).getResource();
                List<RecognisedEntity> recognisedEntities = dbpediaSpotlightService.storeStreamAndExtractEntities(getUser(), "web", search.getId(), resource.getUrl());
                int inputId = dbpediaSpotlightService.storeEntities(getSessionId(), getUser(), recognisedEntities);

                getUserBean().getUserPkg().addRdfStatement("schema:WebPage/" + inputId, "schema:title", resource.getTitle(), "literal");
                getUserBean().getUserPkg().addRdfStatement("schema:WebPage/" + inputId, "schema:url", resource.getUrl(), "resource");
                getUserBean().getUserPkg().addRdfStatement("SearchQuery/" + search.getId(), "generatesResult", "schema:WebPage/" + inputId, "resource");
            }
            snippetClicked.set(search.getResources().indexOf(search.getResources().get(tempResourceId)), true);
            search.logResourceClicked(tempResourceId, getUser());
        } catch (Exception e) {
            log.error("Can't log resource opened event", e);
        }
    }

    /**
     * Calls when the user unloads the page.
     * If the user is active searching, call the dbpedia-spotlight recognition on: query, web and all snippets
     */
    @PreDestroy
    public void destroy() throws Exception {
        if (isUserActive) {
            List<RecognisedEntity> queryEntities = dbpediaSpotlightService.storeStreamAndExtractEntities(getUser(), "query", search.getId(), search.getQuery());
            dbpediaSpotlightService.storeEntities(getSessionId(), getUser(), queryEntities);

            for (ResourceDecorator snippet : search.getResources()) {
                String s = snippet.getTitle().split("\\|")[0].split("-")[0];
                String type = snippetClicked.get(search.getResources().indexOf(snippet)) ? "snippet_clicked" : "snippet_not_clicked";

                List<RecognisedEntity> snippetEntities = dbpediaSpotlightService.storeStreamAndExtractEntities(getUser(), type, search.getId(), "<title>" + s + "</title> " + snippet.getDescription());
                int inputId = dbpediaSpotlightService.storeEntities(getSessionId(), getUser(), snippetEntities);

                getUserBean().getUserPkg().addRdfStatement("Snippet/" + inputId, "schema:title", s, "literal");
                getUserBean().getUserPkg().addRdfStatement("Snippet/" + inputId, "schema:url", snippet.getUrl(), "literal");
                getUserBean().getUserPkg().addRdfStatement("SearchQuery/" + search.getId(), "generatesResult", "Snippet/" + inputId, "resource");
            }

            // getUserBean().getUserPkg().removeDuplicatingNodesAndLinks();
            // Update one for recommendation, one for collabGraph which marks the user's active state.
            // FIXME: it seems we hardcode to use only the first group
            // getUserBean().getUserPkg().createSharedObject(getUser(), getUser().getGroups().get(0).getId(), 5, false, "recommendation");
        }
    }

    /**
     * Create a small recommender system for the current search query.
     * Find the top 3 entities from other users in shared object form, exclude the results from this user, based on
     * their weight in Pkg.
    */
    private void createSearchRecommendation() {
        //Initialization
        recommendations = new ArrayList<>();
        List<JsonSharedObject> sharedObjects =  pkGraphDao.findObjectsByUserId(getUser().getId(), "recommendation");
        if (sharedObjects == null) {
            return;
        }
        Map<String, Double> entityRank = new HashMap<>();
        List<JsonSharedObject.Entity> chosenEntities = new ArrayList<>();

        for (JsonSharedObject sharedObject : sharedObjects) {
            if (sharedObject.getUser().getId() != getUser().getId()) {
                for (JsonSharedObject.Entity entity : sharedObject.getEntities()) {
                    if (chosenEntities.stream().anyMatch(s -> s.getQuery().equals(entity.getQuery()))) {
                        chosenEntities.stream()
                            .filter(s -> s.getQuery().equals(entity.getQuery()))
                            .findFirst()
                            .filter(s -> s.getWeight() < entity.getWeight())
                            .ifPresent(s -> s.setWeight(entity.getWeight()));
                    } else {
                        chosenEntities.add(entity);
                    }
                }
            }
        }
        for (JsonSharedObject sharedObject : sharedObjects) {
            if (sharedObject.getUser().getId() == getUser().getId()) {
                for (JsonSharedObject.Entity entity : sharedObject.getEntities()) {
                    chosenEntities.removeIf(s -> (Objects.equals(s.getUri(), entity.getUri())));
                }
            }
        }
        for (JsonSharedObject.Entity entity : chosenEntities) {
            entityRank.put(entity.getQuery(), entity.getWeight());
        }

        //entries list will be used to store and sort the entities based on their weights
        List<Map.Entry<String, Double>> entries = new ArrayList<>(entityRank.entrySet());
        //No entries are found then we don't need to display the results
        if (entries.isEmpty()) {
            return;
        }
        entries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        //Get the first 3 entities of the results, or the whole results if entries have less than 3 elements
        recommendations = entries.stream().map(entry -> entry.getKey()).collect(Collectors.toList()).subList(0, Math.min(3, entries.size()));
    }

    /**
     * True if the user has started a search request.
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
            String prefService = switch (searchMode) {
                case text -> "SEARCH_SERVICE_TEXT";
                case image -> "SEARCH_SERVICE_IMAGE";
                case video -> "SEARCH_SERVICE_VIDEO";
                case group -> null;
            };
            String defService = switch (searchMode) {
                case text -> getUser().getOrganisation().getDefaultSearchServiceText().name();
                case image -> getUser().getOrganisation().getDefaultSearchServiceImage().name();
                case video -> getUser().getOrganisation().getDefaultSearchServiceVideo().name();
                case group -> "learnweb";
            };
            searchService = ResourceService.valueOf(isShowAlternativeSources() ? getPreference(prefService, defService) : defService);
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
            if (!sb.isEmpty()) {
                sb.append(':');
            }
            sb.append(filterType.name()).append(':').append(filterType.isEncodeBase64() ? StringHelper.encodeBase64(value) : value);
        }

        if (next != -1) {
            sb.append(queryFilters, next, queryFilters.length());
        }

        return sb.toString();
    }

    public List<String> getRecommendations() {
        return recommendations;
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

    public boolean isShowAlternativeSources() {
        return !getUser().getOrganisation().getOption(Organisation.Option.Search_Disable_alternative_sources);
    }

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
    }

    public List<GroupedResources> getResourcesGroupedBySource() {
        if (StringUtils.isNoneBlank(query) && (resourcesGroupedBySource == null || resourcesGroupedBySource.isEmpty())) {
            SearchFilters searchFilters = new SearchFilters(searchMode);
            Search metaSearch = new Search(interweb, query, searchFilters, getUser());
            metaSearch.setMode(searchMode);
            metaSearch.setConfigGroupResultsByField("source");
            metaSearch.setConfigResultsPerGroup(10);
            searchFilters.setFilter(FilterType.language, getUserBean().getLocaleCode());
            metaSearch.getResourcesByPage(2); // fetch resources
            resourcesGroupedBySource = metaSearch.getResourcesGroupedBySource(MIN_RESOURCES_PER_GROUP, searchService);
            Collections.sort(resourcesGroupedBySource);
        }
        return resourcesGroupedBySource;
    }

    public boolean isShowRecommendations() {
        return showRecommendations;
    }

    public boolean isShowRelatedQueries() {
        return showRelatedQueries;
    }
}

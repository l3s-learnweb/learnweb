package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class SolrSearch implements Serializable {
    @Serial
    private static final long serialVersionUID = 6623209570091677070L;
    private static final Logger log = LogManager.getLogger(SolrSearch.class);

    private static final int DEFAULT_GROUP_RESULTS_LIMIT = 2;
    private static final int DEFAULT_RESULTS_LIMIT = 8;

    private final int userId;
    private final boolean onlyOwned;
    private String query;

    // search filters
    private String filterType = ""; // image, video or web
    private String filterService = ""; // Bing, Flickr, YouTube, Vimeo, SlideShare, Ipernity, TED, Learnweb ...
    private String filterDateFrom = "";
    private String filterDateTo = "";
    private String filterCollector = "";
    private String filterAuthor = "";
    private String filterCoverage = "";
    private String filterPublisher = "";
    private String filterLanguageLevel = "";
    private String filterYellTarget = "";
    private String filterYellPurpose = "";
    private String filterTags = "";
    private String filterLanguage = ""; // for example en_US

    // query
    private List<Integer> filterGroupIds;
    private Integer filterFolderId;
    private boolean filterFolderIncludeChild = true;

    private String orderField = "timestamp";
    private ORDER orderDirection = ORDER.desc;

    private int resultsPerPage = DEFAULT_RESULTS_LIMIT;
    private boolean skipResourcesWithoutThumbnails = true;

    private String[] facetFields;
    private String[] facetQueries;

    private String groupResultsByField;
    private int groupResultsLimit = DEFAULT_GROUP_RESULTS_LIMIT;

    // results
    private long resultsFound;
    private List<FacetField> resultsFacetFields;
    private Map<String, Integer> resultsFacetQuery;

    public SolrSearch(String query, User user, boolean onlyOwned) {
        this.userId = user == null ? 0 : user.getId();
        this.onlyOwned = onlyOwned;

        this.query = query;
        String newQuery = removeMyGroupQuery(query);
        if (!query.equals(newQuery)) {
            this.query = newQuery;
            if (user != null && user.getGroups() != null) {
                this.filterGroupIds = user.getGroups().stream().map(Group::getId).collect(Collectors.toList());
            }
        }
    }

    protected int getUserId() {
        return userId;
    }

    protected String getQuery() {
        return query;
    }

    protected List<Integer> getFilterGroupIds() {
        return filterGroupIds;
    }

    protected String getFilterLanguage() {
        return filterLanguage;
    }

    public void setFilterLanguage(String filterLanguage) {
        this.filterLanguage = filterLanguage;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public void setFilterService(String filterService) {
        this.filterService = filterService;
    }

    public void setFilterDateFrom(String date) {
        this.filterDateFrom = date;
    }

    public void setFilterDateTo(String date) {
        this.filterDateTo = date;
    }

    public void setFilterCollector(String collector) {
        this.filterCollector = collector;
    }

    public void setFilterAuthor(String author) {
        this.filterAuthor = author;
    }

    public void setFilterCoverage(String coverage) {
        this.filterCoverage = coverage;
    }

    public void setFilterPublisher(String publisher) {
        this.filterPublisher = publisher;
    }

    public void setFilterLanguageLevel(final String filterLanguageLevel) {
        this.filterLanguageLevel = filterLanguageLevel;
    }

    public void setFilterYellTarget(final String filterYellTarget) {
        this.filterYellTarget = filterYellTarget;
    }

    public void setFilterYellPurpose(final String filterYellPurpose) {
        this.filterYellPurpose = filterYellPurpose;
    }

    public void setFilterTags(String tags) {
        this.filterTags = tags;
    }

    public void clearAllFilters() {
        this.facetFields = null;
        this.facetQueries = null;
        if (null != filterGroupIds) {
            this.filterGroupIds.clear();
        }
        this.filterLanguage = "";
        this.filterService = "";
        this.filterType = "";
        this.filterDateFrom = "";
        this.filterDateTo = "";
        this.filterCollector = "";
        this.filterAuthor = "";
        this.filterCoverage = "";
        this.filterPublisher = "";
        this.filterLanguageLevel = "";
        this.filterYellTarget = "";
        this.filterYellPurpose = "";
        this.filterTags = "";
    }

    public void setFilterGroups(Integer... filterGroupIds) {
        this.filterGroupIds = new ArrayList<>();
        Collections.addAll(this.filterGroupIds, filterGroupIds);
    }

    public void setFilterFolder(Integer folderId, boolean isIncludeChild) {
        this.filterFolderId = folderId;
        this.filterFolderIncludeChild = isIncludeChild;
    }

    public void setOrder(String field, ORDER direction) {
        this.orderField = field;
        this.orderDirection = direction;
    }

    public Integer getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    public void setSkipResourcesWithoutThumbnails(boolean skipResourcesWithoutThumbnails) {
        this.skipResourcesWithoutThumbnails = skipResourcesWithoutThumbnails;
    }

    public void setGroupResultsByField(String groupResultsByField) {
        this.groupResultsByField = groupResultsByField;
    }

    public void setGroupResultsLimit(int groupResultsLimit) {
        this.groupResultsLimit = groupResultsLimit;
    }

    public void setFacetFields(String... facetFields) {
        this.facetFields = facetFields;
    }

    public void setFacetQueries(String... facetQueries) {
        this.facetQueries = facetQueries;
    }

    private QueryResponse getQueryResourcesByPage(int page) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.set("qt", "/LearnwebQuery");

        if (filterGroupIds != null && !filterGroupIds.isEmpty()) {
            solrQuery.addFilterQuery("groupId : (" + StringUtils.join(filterGroupIds, " OR ") + ")");

            if (onlyOwned || (filterGroupIds.size() == 1 && filterGroupIds.contains(0))) {
                solrQuery.addFilterQuery("ownerUserId: " + userId); // show only resources of the user
            }
        } else {
            solrQuery.addFilterQuery("groupId: [* TO *] OR ownerUserId: " + userId); // hide private resources
        }

        if (filterFolderId != null) {
            applyFolder(solrQuery, filterFolderId, filterFolderIncludeChild);
        }

        applySearchFilters(solrQuery);

        if (groupResultsByField != null) {
            solrQuery.set("group", "true");
            solrQuery.set("group.field", groupResultsByField);
            solrQuery.set("group.limit", groupResultsLimit);
            solrQuery.set("group.main", "true");
        }

        if (orderField != null) {
            solrQuery.addSort(orderField, orderDirection);
        }

        solrQuery.setStart((page - 1) * resultsPerPage);
        solrQuery.setRows(resultsPerPage);

        // for snippets
        solrQuery.setHighlight(true);
        solrQuery.addHighlightField("title");
        solrQuery.addHighlightField("description");
        solrQuery.addHighlightField("comments");
        solrQuery.addHighlightField("machineDescription");
        solrQuery.setHighlightSnippets(1); // number of snippets per field per resource
        solrQuery.setHighlightFragsize(200); // size of per snippet
        solrQuery.setParam("f.title.hl.fragsize", "0"); // size of snippet from title, 0 means return the whole field as snippet
        solrQuery.setHighlightSimplePre("<strong>");
        solrQuery.setHighlightSimplePost("</strong>");

        if (facetFields != null || facetQueries != null) {
            applyFacets(solrQuery, facetFields, facetQueries);
        }

        // log.debug("solr query: " + solrQuery);

        Http2SolrClient server = Learnweb.getInstance().getSolrClient().getHttpSolrClient();
        return server.query(solrQuery);
    }

    private void applySearchFilters(final SolrQuery solrQuery) {
        if (!filterService.isEmpty()) {
            solrQuery.addFilterQuery("source : " + filterService);
        }

        if (!filterType.isEmpty()) {
            if ("web".equalsIgnoreCase(filterType)) {
                solrQuery.addFilterQuery("-type : (image OR video)");
                solrQuery.add("bq", "description:*^9+description:*^9"); // boost results which have a title and description
            } else if ("other".equalsIgnoreCase(filterType)) {
                solrQuery.addFilterQuery("-type : (text OR image OR video OR pdf)");
            } else {
                solrQuery.addFilterQuery("type : " + filterType);
            }
        }

        if (!filterDateFrom.isEmpty()) {
            if (filterDateTo.isEmpty()) {
                solrQuery.addFilterQuery("timestamp : [" + filterDateFrom + " TO NOW]");
            } else {
                solrQuery.addFilterQuery("timestamp : [" + filterDateFrom + " TO " + filterDateTo + "]");
            }
        } else if (!filterDateTo.isEmpty()) {
            solrQuery.addFilterQuery("timestamp : [* TO " + filterDateTo + "]");
        }

        if (!filterCollector.isEmpty()) {
            solrQuery.addFilterQuery("collector_s : \"" + filterCollector + "\"");
        }

        if (!filterAuthor.isEmpty()) {
            solrQuery.addFilterQuery("author_s : \"" + filterAuthor + "\"");
        }

        if (!filterCoverage.isEmpty()) {
            solrQuery.addFilterQuery("coverage_s : \"" + filterCoverage + "\"");
        }

        if (!filterPublisher.isEmpty()) {
            solrQuery.addFilterQuery("publisher_s : \"" + filterPublisher + "\"");
        }

        if (!filterLanguageLevel.isEmpty()) {
            solrQuery.addFilterQuery("language_level_ss : \"" + filterLanguageLevel + "\"");
        }

        if (!filterYellTarget.isEmpty()) {
            solrQuery.addFilterQuery("yell_target_ss : \"" + filterYellTarget + "\"");
        }

        if (!filterYellPurpose.isEmpty()) {
            solrQuery.addFilterQuery("yell_purpose_ss : \"" + filterYellPurpose + "\"");
        }

        if (!filterTags.isEmpty()) {
            solrQuery.addFilterQuery("tags : \"" + filterTags + "\"");
        }

        if (!filterLanguage.isEmpty()) {
            solrQuery.addFilterQuery("language : " + filterLanguage);
        }
    }

    public List<ResourceDecorator> getResourcesByPage(int page) throws IOException, SolrServerException {
        List<ResourceDecorator> resources = new LinkedList<>();

        QueryResponse queryResponse = getQueryResourcesByPage(page);
        if (queryResponse != null) {
            List<ResourceDocument> resourceDocuments = queryResponse.getBeans(ResourceDocument.class);

            int resourceRank = (page - 1) * resultsPerPage;
            int skippedResources = 0;
            for (ResourceDocument resourceDocument : resourceDocuments) {
                int resourceId = SolrClient.extractId(resourceDocument.getId());
                Optional<Resource> resource = Learnweb.dao().getResourceDao().findById(resourceId);

                if (resource.isEmpty()) {
                    log.warn("could not find resource with id:{}", resourceDocument.getId());
                    continue;
                }

                if (skipResourcesWithoutThumbnails && resource.get().getThumbnailMedium() == null
                    && (resource.get().getType() == ResourceType.image || resource.get().getType() == ResourceType.video)) {
                    skippedResources++;
                    continue;
                }

                resources.add(createResourceDecorator(resource.get(), resourceRank, queryResponse.getHighlighting().get(resourceDocument.getId())));
                resourceRank += 1;
            }

            if (skippedResources != 0) {
                log.error("{} video/image resources have no thumbnails and were skipped", skippedResources);
            }

            resultsFound = queryResponse.getResults().getNumFound();
            resultsFacetFields = queryResponse.getFacetFields();
            resultsFacetQuery = queryResponse.getFacetQuery();
        }

        return resources;
    }

    public long getResultsFound() {
        return resultsFound;
    }

    public List<FacetField> getResultsFacetFields() {
        return resultsFacetFields;
    }

    public Map<String, Integer> getResultsFacetQuery() {
        return resultsFacetQuery;
    }

    private static String removeMyGroupQuery(final String query) {
        Pattern pattern = Pattern.compile("groups\\s*:\\s*my\\s*");
        Matcher matcher = pattern.matcher(query.toLowerCase(Locale.ENGLISH));
        if (matcher.find()) {
            String newQuery = "";
            int start = matcher.start();
            if (start != 0) {
                newQuery = query.substring(0, start);
            }
            newQuery += query.substring(matcher.end());
            return newQuery;
        } else {
            return query;
        }
    }

    private static void applyFolder(final SolrQuery solrQuery, final Integer folderId, final boolean isIncludeChild) {
        if (folderId == null || folderId == 0) {
            if (!isIncludeChild) {
                // only from root directory: path field not exists or equals to "/"
                solrQuery.addFilterQuery("(*:* NOT path:[* TO *]) OR path: \"/\"");
            }
            // else: root folder and all subfolders - no query needed
        } else {
            if (isIncludeChild) {
                // certain folder and subfolders: the path includes or ends with the folderId
                solrQuery.addFilterQuery("path : (*/" + folderId + "/* OR */" + folderId + ")");
            } else {
                // only from certain folder without subfolders: the path should ends with the folderId
                solrQuery.addFilterQuery("path : */" + folderId);
            }

        }
    }

    private static void applyFacets(final SolrQuery solrQuery, final String[] facetFields, final String[] facetQueries) {
        solrQuery.setFacet(true);
        if (facetFields != null) {
            solrQuery.addFacetField(facetFields);
        }

        if (facetQueries != null && facetQueries.length > 0) {
            for (String facetQuery : facetQueries) {
                solrQuery.addFacetQuery(facetQuery);
            }
        }

        solrQuery.setFacetLimit(30);
        solrQuery.setFacetSort("count");
        solrQuery.setFacetMinCount(1);
    }

    private static ResourceDecorator createResourceDecorator(final Resource resource, final int resRank, final Map<String, List<String>> documentSnippets) {
        final ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        final StringBuilder snippet = new StringBuilder();

        decoratedResource.setRank(resRank);
        if (documentSnippets.get("title") != null) {
            decoratedResource.setTitle(documentSnippets.get("title").get(0));
        }
        if (documentSnippets.get("description") != null) {
            snippet.append(documentSnippets.get("description").get(0));
        }
        if (snippet.length() < 150 && documentSnippets.get("comments") != null) {
            snippet.append(documentSnippets.get("comments").get(0));
        }
        if (snippet.length() < 150 && documentSnippets.get("machineDescription") != null) {
            snippet.append(documentSnippets.get("machineDescription").get(0));
        }

        // still no real snippet => use description
        if (snippet.length() < 40 && null != resource.getDescription()) {
            snippet.append(StringHelper.shortnString(Jsoup.clean(resource.getDescription(), Safelist.none()), 230));
        }

        String oneLineSnippet = StringHelper.removeNewLines(snippet.toString());
        oneLineSnippet = StringHelper.trimNotAlphabetical(oneLineSnippet);
        if (!oneLineSnippet.isEmpty()) {
            decoratedResource.setSnippet(oneLineSnippet);
        }
        return decoratedResource;
    }
}

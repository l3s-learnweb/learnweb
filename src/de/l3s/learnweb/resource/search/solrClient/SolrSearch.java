package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.l3s.learnweb.resource.ResourceType;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class SolrSearch implements Serializable
{
    private static final long serialVersionUID = 6623209570091677070L;
    private static final Logger log = Logger.getLogger(SolrSearch.class);

    private static final int DEFAULT_GROUP_RESULTS_LIMIT = 2;
    private static final int DEFAULT_RESULTS_LIMIT = 8;

    private int userId;
    private String query;

    // search filters
    private String filterType = ""; // image, video or web
    private String filterLocation = ""; // Bing, Flickr, YouTube, Vimeo, SlideShare, Ipernity, TED, Learnweb ...
    private String filterDateFrom = "";
    private String filterDateTo = "";
    private String filterCollector = "";
    private String filterAuthor = "";
    private String filterCoverage = "";
    private String filterPublisher = "";
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
    private transient QueryResponse queryResponse;

    public SolrSearch(String query, User user)
    {
        this.userId = user == null ? 0 : user.getId();

        this.query = query;
        String newQuery = removeMyGroupQuery(query);
        if(!query.equals(newQuery))
        {
            this.query = newQuery;
            try
            {
                if(user != null && user.getGroups() != null)
                {
                    this.filterGroupIds = user.getGroups().stream().map(Group::getId).collect(Collectors.toList());
                }
            }
            catch(SQLException e)
            {
                log.error("Could not retrieve users group", e);
            }
        }
    }

    private static String removeMyGroupQuery(final String query)
    {
        Pattern pattern = Pattern.compile("groups\\s*:\\s*my\\s*");
        Matcher matcher = pattern.matcher(query.toLowerCase(Locale.ENGLISH));
        if(matcher.find())
        {
            String newQuery = "";
            int start = matcher.start();
            if(start != 0) newQuery = query.substring(0, start);
            newQuery = newQuery.concat(query.substring(matcher.end()));
            return newQuery;
        }
        else return query;
    }

    public void setFilterLanguage(String filterLanguage)
    {
        this.filterLanguage = filterLanguage;
    }

    public void setFilterType(String filterType)
    {
        this.filterType = filterType;
    }

    public void setFilterLocation(String filterLocation)
    {
        this.filterLocation = filterLocation;
    }

    public void setFilterDateFrom(String date)
    {
        this.filterDateFrom = date;
    }

    public void setFilterDateTo(String date)
    {
        this.filterDateTo = date;
    }

    public void setFilterCollector(String collector)
    {
        this.filterCollector = collector;
    }

    public void setFilterAuthor(String author)
    {
        this.filterAuthor = author;
    }

    public void setFilterCoverage(String coverage)
    {
        this.filterCoverage = coverage;
    }

    public void setFilterPublisher(String publisher)
    {
        this.filterPublisher = publisher;
    }

    public void setFilterTags(String tags)
    {
        this.filterTags = tags;
    }

    public void clearAllFilters()
    {
        this.facetFields = null;
        this.facetQueries = null;
        if(null != filterGroupIds)
            this.filterGroupIds.clear();
        this.filterLanguage = "";
        this.filterLocation = "";
        this.filterType = "";
        this.filterDateFrom = "";
        this.filterDateTo = "";
        this.filterCollector = "";
        this.filterAuthor = "";
        this.filterCoverage = "";
        this.filterPublisher = "";
        this.filterTags = "";
    }

    public void setFilterGroups(Integer... filterGroupIds)
    {
        this.filterGroupIds = new ArrayList<>();
        Collections.addAll(this.filterGroupIds, filterGroupIds);
    }

    public void setFilterFolder(Integer folderId, boolean isIncludeChild)
    {
        this.filterFolderId = folderId;
        this.filterFolderIncludeChild = isIncludeChild;
    }

    public void setOrder(String field, ORDER direction)
    {
        this.orderField = field;
        this.orderDirection = direction;
    }

    public Integer getResultsPerPage()
    {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage)
    {
        this.resultsPerPage = resultsPerPage;
    }

    public void setSkipResourcesWithoutThumbnails(boolean skipResourcesWithoutThumbnails)
    {
        this.skipResourcesWithoutThumbnails = skipResourcesWithoutThumbnails;
    }

    public void setGroupResultsByField(String groupResultsByField)
    {
        this.groupResultsByField = groupResultsByField;
    }

    public void setGroupResultsLimit(int groupResultsLimit)
    {
        this.groupResultsLimit = groupResultsLimit;
    }

    public void setFacetFields(String... facetFields)
    {
        this.facetFields = facetFields;
    }

    public void setFacetQueries(String... facetQueries)
    {
        this.facetQueries = facetQueries;
    }

    private QueryResponse getQueryResourcesByPage(int page) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.set("qt", "/LearnwebQuery");

        if(filterGroupIds != null && !filterGroupIds.isEmpty())
        {
            solrQuery.addFilterQuery("groupId : (" + StringUtils.join(filterGroupIds, " OR ") + ")");
        }
        else
        {
            solrQuery.addFilterQuery("groupId: [* TO *] OR ownerUserId: " + userId); // hide private resources
        }

        if(filterFolderId != null)
        {
            applyFolder(solrQuery, filterFolderId, filterFolderIncludeChild);
        }

        applySearchFilters(solrQuery);

        if(groupResultsByField != null)
        {
            solrQuery.set("group", "true");
            solrQuery.set("group.field", groupResultsByField);
            solrQuery.set("group.limit", groupResultsLimit);
            solrQuery.set("group.main", "true");
        }

        if(orderField != null)
        {
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

        if(facetFields != null || facetQueries != null)
        {
            applyFacets(solrQuery, facetFields, facetQueries);
        }

        // log.debug("solr query: " + solrQuery);

        HttpSolrClient server = Learnweb.getInstance().getSolrClient().getSolrServer();
        return server.query(solrQuery);
    }

    private static void applyFolder(final SolrQuery solrQuery, final Integer folderId, final boolean isIncludeChild)
    {
        if(folderId == null || folderId == 0)
        {
            if(!isIncludeChild)
            {
                // only from root directory: path field not exists or equals to "/"
                solrQuery.addFilterQuery("(*:* NOT path:[* TO *]) OR path: \"/\"");
            }
            // else: root folder and all subfolders - no query needed
        }
        else
        {
            if(isIncludeChild)
            {
                // certain folder and subfolders: the path includes or ends with the folderId
                solrQuery.addFilterQuery("path : (*/" + folderId + "/* OR */" + folderId + ")");
            }
            else
            {
                // only from certain folder without subfolders: the path should ends with the folderId
                solrQuery.addFilterQuery("path : */" + folderId);
            }

        }
    }

    private void applySearchFilters(final SolrQuery solrQuery)
    {
        if(0 != filterLocation.length())
            solrQuery.addFilterQuery("location : " + filterLocation);

        if(0 != filterType.length())
        {
            if("web".equalsIgnoreCase(filterType))
            {
                solrQuery.addFilterQuery("-type : (image OR video)");
                solrQuery.add("bq", "description:*^9+description:*^9"); // boost results which have a title and description
            }
            else
            {
                solrQuery.addFilterQuery("type : " + filterType);
            }
        }

        if(0 != filterDateFrom.length())
        {
            if(0 != filterDateTo.length())
                solrQuery.addFilterQuery("timestamp : [" + filterDateFrom + " TO " + filterDateTo + "]");
            else
                solrQuery.addFilterQuery("timestamp : [" + filterDateFrom + " TO NOW]");
        }
        else if(0 != filterDateTo.length())
        {
            solrQuery.addFilterQuery("timestamp : [* TO " + filterDateTo + "]");
        }

        if(0 != filterCollector.length())
        {
            solrQuery.addFilterQuery("collector_s : \"" + filterCollector + "\"");
        }

        if(0 != filterAuthor.length())
        {
            solrQuery.addFilterQuery("author_s : \"" + filterAuthor + "\"");
        }

        if(0 != filterCoverage.length())
        {
            solrQuery.addFilterQuery("coverage_s : \"" + filterCoverage + "\"");
        }

        if(0 != filterPublisher.length())
        {
            solrQuery.addFilterQuery("publisher_s : \"" + filterPublisher + "\"");
        }

        if(0 != filterTags.length())
        {
            solrQuery.addFilterQuery("tags : \"" + filterTags + "\"");
        }

        if(0 != filterLanguage.length())
        {
            solrQuery.addFilterQuery("language : " + filterLanguage);
        }
    }

    private static void applyFacets(final SolrQuery solrQuery, final String[] facetFields, final String[] facetQueries)
    {
        solrQuery.setFacet(true);
        if(facetFields != null)
        {
            solrQuery.addFacetField(facetFields);
        }

        if(facetQueries != null && facetQueries.length > 0)
        {
            for(String facetQuery : facetQueries)
            {
                solrQuery.addFacetQuery(facetQuery);
            }
        }

        solrQuery.setFacetLimit(30);
        solrQuery.setFacetSort("count");
        solrQuery.setFacetMinCount(1);
    }

    public List<ResourceDecorator> getResourcesByPage(int page) throws SQLException, IOException, SolrServerException
    {
        List<ResourceDecorator> resources = new LinkedList<>();

        this.queryResponse = getQueryResourcesByPage(page);
        if(queryResponse != null)
        {
            List<ResourceDocument> resourceDocuments = queryResponse.getBeans(ResourceDocument.class);

            ResourceManager resourceManager = Learnweb.getInstance().getResourceManager();

            int skippedResources = 0;
            for(ResourceDocument resourceDocument : resourceDocuments)
            {
                int resourceId = extractResourceId(resourceDocument.getId());
                Resource resource = resourceManager.getResource(resourceId);

                if(resource == null)
                {
                    log.warn("could not find resource with id:" + resourceDocument.getId());
                    continue;
                }

                if(skipResourcesWithoutThumbnails && resource.getThumbnail2() == null &&
                    (resource.getType() == ResourceType.image || resource.getType() == ResourceType.video))
                {
                    skippedResources++;
                    continue;
                }

                resources.add(createResourceDecorator(resource, queryResponse.getHighlighting().get(resourceDocument.getId())));
            }

            if(skippedResources > 0)
            {
                log.error(skippedResources + " video/image resources have no thumbnails and were skipped");
            }
        }

        return resources;
    }

    private static ResourceDecorator createResourceDecorator(final Resource resource, final Map<String, List<String>> documentSnippets)
    {
        final ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        StringBuilder snippet = new StringBuilder();

        if(documentSnippets.get("title") != null) decoratedResource.setTitle(documentSnippets.get("title").get(0));
        if(documentSnippets.get("description") != null) snippet.append(documentSnippets.get("description").get(0));
        if(snippet.length() < 150 && documentSnippets.get("comments") != null) snippet.append(documentSnippets.get("comments").get(0));
        if(snippet.length() < 150 && documentSnippets.get("machineDescription") != null) snippet.append(documentSnippets.get("machineDescription").get(0));

        // still no real snippet => use description
        if(snippet.length() < 40 && null != resource.getDescription())
        {
            snippet.append(StringHelper.shortnString(Jsoup.clean(resource.getDescription(), Whitelist.none()), 230));
        }

        String oneLineSnippet = StringHelper.removeNewLines(snippet.toString());
        oneLineSnippet = StringHelper.trimNotAlphabetical(oneLineSnippet);
        if(oneLineSnippet.length() != 0) decoratedResource.setSnippet(oneLineSnippet);
        return decoratedResource;
    }

    public QueryResponse getQueryResponse()
    {
        return queryResponse;
    }

    private static int extractResourceId(String id)
    {
        try
        {
            return Integer.parseInt(id.substring(2));
        }
        catch(NumberFormatException e)
        {
            log.error("SolrSearch, NumberFormatException: " + e.getMessage());
            return -1;
        }
    }
}

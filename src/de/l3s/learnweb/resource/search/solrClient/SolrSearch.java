package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class SolrSearch implements Serializable
{
    private static final long serialVersionUID = 6623209570091677070L;
    final static Logger log = Logger.getLogger(SolrSearch.class);

    private String query;
    private SolrQuery solrQuery;

    private Integer resultsPerPage = 8;
    private Integer resultsPerGroup = 2;
    private String facetFields[] = null;
    private String facetQueries[] = null;
    private String filterLanguage = ""; // for example en_US
    private String filterType = ""; // image, video or web
    private String filterSource = ""; // Bing, Flickr, YouTube, Vimeo, SlideShare, Ipernity, TED, Desktop ...
    private String filterLocation = ""; // Bing, Flickr, YouTube, Vimeo, SlideShare, Ipernity, TED, Learnweb ...
    private String filterFormat = ""; // for example: application/pdf
    private String filterDateFrom = "";
    private String filterDateTo = "";
    private String filterCollector = "";
    private String filterAuthor = "";
    private String filterCoverage = "";
    private String filterPublisher = "";
    private String filterTags = "";
    private String filterPath = "";
    private List<Integer> filterGroupIds;
    private String sorting;
    private String groupField = "";

    protected long totalResults = -1;
    private String filterGroupStr = "";
    private int userId;
    private boolean skipResourcesWithoutThumbnails = true;
    private List<FacetField> facetFieldsResult = null;
    private Map<String, Integer> facetQueriesResult = null;

    public SolrSearch(String query, User user)
    {
        this.query = query;
        String newQuery = removeMyGroupQuery(query);
        if(!query.equals(newQuery))
        {
            this.query = newQuery;
            try
            {
                if(user != null && user.getGroups() != null)
                {
                    this.filterGroupIds = new LinkedList<>();
                    for(Group group : user.getGroups())
                        this.filterGroupIds.add(group.getId());
                }
            }
            catch(SQLException e)
            {
                log.error("Could not retrieve users group", e);
            }
        }
        this.userId = user == null ? 0 : user.getId();
    }

    public void setResultsPerPage(Integer configResultsPerPage)
    {
        this.resultsPerPage = configResultsPerPage;
    }

    public void setResultsPerGroup(Integer resultsPerGroup)
    {
        this.resultsPerGroup = resultsPerGroup;
    }

    public void setFacetFields(String... facetFields)
    {
        this.facetFields = facetFields;
    }

    public void setFacetQueries(String... facetQueries)
    {
        this.facetQueries = facetQueries;
    }

    public Integer getResultsPerPage()
    {
        return resultsPerPage;
    }

    /**
     * The language resources should be in
     * 
     * @param filterLanguage
     */
    public void setFilterLanguage(String filterLanguage)
    {
        this.filterLanguage = filterLanguage;
    }

    /**
     * 
     * @param filterType image, video or web
     */
    public void setFilterType(String filterType)
    {
        this.filterType = filterType;
    }

    public void setFilterSource(String filterSource)
    {
        this.filterSource = filterSource;
    }

    public void setFilterLocation(String filterLocation)
    {
        this.filterLocation = filterLocation;
    }

    public void setFilterFormat(String filterFormat)
    {
        this.filterFormat = filterFormat;
    }

    public void setFilterDateFrom(String date)
    {
        this.filterDateFrom = date;
    }

    public void setFilterDateTo(String date)
    {
        this.filterDateTo = date;
    }

    /**
     * Either provide a list of groups
     * 
     * @param filterGroups
     */
    public void setFilterGroups(List<Group> filterGroups)
    {
        this.filterGroupIds = new LinkedList<>();

        for(Group group : filterGroups) // get ids of the groups
        {
            filterGroupIds.add(group.getId());
        }
    }

    /**
     * Or directly provide the list of group ids to search in
     * 
     * @param filterGroupIds
     */
    public void setFilterGroups(Integer... filterGroupIds)
    {
        this.filterGroupIds = new LinkedList<>();
        Collections.addAll(this.filterGroupIds, filterGroupIds);
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

    public void setFilterFolder(int folderId, boolean isIncludeChild)
    {
        setFilterFolder(Integer.toString(folderId), isIncludeChild);
    }

    public void setFilterFolder(String folder, boolean isIncludeChild)
    {
        if(folder != null)
        {
            if(folder.startsWith("0"))
            {
                if(isIncludeChild)
                {
                    // skip this filter, because it is root directory and all subdirectories
                    this.filterPath = "";
                }
                else
                {
                    // Where field not exists or equal to "/"
                    this.filterPath = "(*:* NOT path:*) || path: \"/\"";
                }
            }
            else
            {
                if(isIncludeChild)
                {
                    this.filterPath = "path : (*/" + folder + "/* OR */" + folder + ")";
                }
                else
                {
                    this.filterPath = "path : */" + folder;
                }

            }
        }
        else
        {
            this.filterPath = "";
        }
    }

    public void setGroupField(String groupField)
    {
        this.groupField = groupField;
    }

    public void clearAllFilters()
    {
        this.facetFields = null;
        this.facetQueries = null;
        this.facetFieldsResult = null;
        this.facetQueriesResult = null;
        this.filterFormat = "";
        if(null != filterGroupIds)
            this.filterGroupIds.clear();
        this.filterGroupStr = "";
        this.filterLanguage = "";
        this.filterLocation = "";
        this.filterSource = "";
        this.filterType = "";
        this.filterDateFrom = "";
        this.filterDateTo = "";
        this.filterCollector = "";
        this.filterAuthor = "";
        this.filterCoverage = "";
        this.filterPublisher = "";
        this.filterTags = "";
        this.filterPath = "";
    }

    public long getTotalResultCount()
    {
        return totalResults;
    }

    public List<FacetField> getFacetFields()
    {
        if(facetFieldsResult == null)
        {
            getFaced();
        }

        List<FacetField> ff = facetFieldsResult;
        this.facetFieldsResult = null;
        return ff;
    }

    public Map<String, Integer> getFacetQueries()
    {
        if(facetQueriesResult == null)
        {
            getFaced();
        }

        Map<String, Integer> fq = facetQueriesResult;
        this.facetQueriesResult = null;
        return fq;
    }

    public void setSkipResourcesWithoutThumbnails(boolean skipResourcesWithoutThumbnails)
    {
        this.skipResourcesWithoutThumbnails = skipResourcesWithoutThumbnails;
    }

    private QueryResponse getSolrResourcesByPage(int page) throws SQLException, SolrServerException, IOException
    {

        //set SolrQuery
        solrQuery = new SolrQuery(query);
        solrQuery.set("qt", "/LearnwebQuery");
        if(0 != filterLanguage.length())
            solrQuery.addFilterQuery("language : " + filterLanguage);
        if(0 != filterType.length())
        {
            if(filterType.equalsIgnoreCase("web"))
            {
                solrQuery.addFilterQuery("-type : image");
                solrQuery.addFilterQuery("-type : video");

                solrQuery.add("bq", "description:*^9+description:*^9"); // boost results which have a title and description		
            }
            else if(filterType.equalsIgnoreCase("other"))
            {
                solrQuery.addFilterQuery("-type : text");
                solrQuery.addFilterQuery("-type : image");
                solrQuery.addFilterQuery("-type : video");
                solrQuery.addFilterQuery("-type : pdf");
            }
            else
                solrQuery.addFilterQuery("type : " + filterType);
        }
        if(0 != filterSource.length())
            solrQuery.addFilterQuery("source : " + filterSource);
        if(0 != filterLocation.length())
            solrQuery.addFilterQuery("location : " + filterLocation);
        if(0 != filterFormat.length())
            solrQuery.addFilterQuery("format : " + filterFormat);
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

        if(0 != filterPath.length())
        {
            solrQuery.addFilterQuery(filterPath);
        }

        if(null != filterGroupIds)
        {
            filterGroupStr = "";
            for(Integer groupId : filterGroupIds)
            {
                if(0 == filterGroupStr.length())
                    filterGroupStr = "groupId : " + groupId.toString();
                else
                    filterGroupStr += " OR groupId : " + groupId.toString();
            }
            solrQuery.addFilterQuery(filterGroupStr);
        }

        if(0 != groupField.length())
        {
            solrQuery.set("group", "true");
            solrQuery.set("group.field", groupField);
            solrQuery.set("group.limit", this.resultsPerGroup);
            solrQuery.set("group.main", "true");
        }

        if(null != sorting) // TODO implement 
        {
            solrQuery.addSort("timestamp", ORDER.desc);
        }
        solrQuery.addFilterQuery("-(id:r_* AND -(groupId:* OR ownerUserId:" + userId + "))"); // hide private resources
        // better use?  solrQuery.addFilterQuery("groupId:* OR ownerUserId:" + userId + ""); // hide private resources

        solrQuery.setStart((page - 1) * resultsPerPage);
        solrQuery.setRows(resultsPerPage);

        //for snippets
        solrQuery.setHighlight(true);
        solrQuery.addHighlightField("title");
        solrQuery.addHighlightField("description");
        solrQuery.addHighlightField("comments");
        solrQuery.addHighlightField("machineDescription");
        solrQuery.setHighlightSnippets(1); // number of snippets per field per resource
        solrQuery.setHighlightFragsize(200); //size of per snippet
        solrQuery.setParam("f.title.hl.fragsize", "0");//size of snippet from title, 0 means return the whole field as snippet 
        solrQuery.setHighlightSimplePre("<strong>");
        solrQuery.setHighlightSimplePost("</strong>");

        solrQuery.setFacet(true);
        if(facetFields != null)
        {
            solrQuery.addFacetField(facetFields);
        }
        if(facetQueries != null && facetQueries.length > 0)
        {
            for(String query : facetQueries)
            {
                solrQuery.addFacetQuery(query);
            }
        }
        solrQuery.setFacetLimit(20); // TODO set to -1 to show all facets (implement "more" button on frontend)
        solrQuery.setFacetSort("count");
        solrQuery.setFacetMinCount(1);

        //        log.debug("solr query: " + solrQuery);

        //get solrServer
        HttpSolrClient server = Learnweb.getInstance().getSolrClient().getSolrServer();

        //get response
        return server.query(solrQuery);
    }

    /**
     * Execute query and set facet results
     * 
     * @throws SQLException
     * @throws SolrServerException
     */
    public void getFaced()
    {
        try
        {
            QueryResponse response = getSolrResourcesByPage(1);
            if(response != null)
            {
                totalResults = response.getResults().getNumFound();
                facetFieldsResult = response.getFacetFields();
                facetQueriesResult = response.getFacetQuery();
            }
        }
        catch(SQLException | IOException | SolrServerException e)
        {
            log.fatal("Couldn't read faced fields from Solr", e);
        }
    }

    /**
     * Returns null of nothing found
     * 
     * @param page
     * @return
     * @throws SolrServerException
     * @throws SQLException
     */
    public List<ResourceDecorator> getResourcesByPage(int page) throws SQLException, IOException, SolrServerException
    {
        List<ResourceDecorator> resources = new LinkedList<>();

        ResourceManager resourceManager = Learnweb.getInstance().getResourceManager();
        QueryResponse response = getSolrResourcesByPage(page);

        if(response != null)
        {
            totalResults = response.getResults().getNumFound();
            facetFieldsResult = response.getFacetFields();
            facetQueriesResult = response.getFacetQuery();
        }

        // SolrDocumentList docs = response.getResults(); // to get the score
        List<SolrResourceBean> solrResources = response.getBeans(SolrResourceBean.class);
        Map<String, Map<String, List<String>>> highlight = response.getHighlighting();

        List<String> snippets = new LinkedList<>();

        int skippedResources = 0;

        for(SolrResourceBean solrResource : solrResources)
        {
            //print solr scores for each returned result from solr
            //log.debug(docs.get(i).getFieldValue("score"));
            Resource resource = null;

            if(solrResource.getId().startsWith("r_")) // a "real" Learnweb resource
            {
                int resourceId = extractId(solrResource.getId());
                resource = resourceManager.getResource(resourceId);

                if(null == resource)
                {
                    log.warn("could not find resource with id:" + solrResource.getId());
                    continue;
                }
            }
            else
            { // cached resources
                log.fatal("Cached resources are disabled. This should never happen. Solr is in a corrupted state.");
                /*		
                		resource = new Resource();
                		resource.setUrl(solrResource.getId());
                		resource.setTitle(solrResource.getTitle());
                		resource.setSource(solrResource.getSource());
                		resource.setDescription(solrResource.getDescription());
                		resource.setLocation(solrResource.getLocation());
                		resource.setType(solrResource.getType());
                		resource.setFormat(solrResource.getFormat());
                		//resource.setLanguage(solrResource.getLanguage());
                		resource.setAuthor(solrResource.getAuthor());
                		resource.setMachineDescription(solrResource.getMachineDescription());
                		resource.setEmbeddedRaw(solrResource.getEmbeddedCode());
                		resource.setThumbnail2(new Thumbnail(solrResource.getThumbnailUrl2(), solrResource.getThumbnailWidth2(), solrResource.getThumbnailHeight2()));
                		resource.setThumbnail3(new Thumbnail(solrResource.getThumbnailUrl3(), solrResource.getThumbnailWidth3(), solrResource.getThumbnailHeight3()));
                		resource.setThumbnail4(new Thumbnail(solrResource.getThumbnailUrl4(), solrResource.getThumbnailWidth4(), solrResource.getThumbnailHeight4()));
                */
            }

            if(resource.getType() == null || resource.getTitle() == null || resource.getUrl() == null)
            {
                log.error("missing mandatory field url, title or type " + resource);
                continue;
            }

            if(skipResourcesWithoutThumbnails && (resource.getType().equals(Resource.ResourceType.image) || resource.getType().equals(Resource.ResourceType.video)) && resource.getThumbnail2() == null)
            {
                skippedResources++;
                continue;
            }

            ResourceDecorator decoratedResource = new ResourceDecorator(resource);
            resources.add(decoratedResource);

            Map<String, List<String>> resourceSnippets = highlight.get(solrResource.getId());
            snippets.clear();
            StringBuilder snippet = new StringBuilder();

            if(null != resourceSnippets.get("title"))
                decoratedResource.setTitle(resourceSnippets.get("title").get(0));

            if(null != resourceSnippets.get("description"))
                snippet.append(resourceSnippets.get("description").get(0));

            if(snippet.length() < 150)
            {
                if(null != resourceSnippets.get("comments"))
                    snippet.append(resourceSnippets.get("comments").get(0));
            }
            if(snippet.length() < 150)
            {
                if(null != resourceSnippets.get("machineDescription"))
                    snippet.append(resourceSnippets.get("machineDescription").get(0));
            }

            if(snippet.length() < 40) // still no real snippet => use description
            {
                if(null != resource.getDescription())
                    snippet.append(Jsoup.clean(StringHelper.shortnString(resource.getDescription(), 180), Whitelist.none()));
            }

            String oneLineSnippets = snippet.toString().replaceAll("\n", " ");
            Pattern pattern = Pattern.compile("[^<\"\'a-zA-Z]+");
            Matcher matcher = pattern.matcher(oneLineSnippets);
            if(matcher.lookingAt())
            {
                oneLineSnippets = oneLineSnippets.substring(matcher.end());
            }

            if(oneLineSnippets.length() != 0)
                decoratedResource.setSnippet(oneLineSnippets);
        }

        if(skippedResources > 0)
        {
            log.error(skippedResources + " video/image resource had no thumbnail and were skipped");
        }

        return resources;
    }

    public void setSort(String sort)
    {
        this.sorting = sort;
    }

    private int extractId(String id)
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

    private String removeMyGroupQuery(String query)
    {
        String newQuery = "";
        Pattern pattern = Pattern.compile("groups\\s*:\\s*my\\s*");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if(matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();
            if(start != 0)
                newQuery = query.substring(0, start);
            newQuery = newQuery.concat(query.substring(end, query.length()));
            return newQuery;
        }
        else
            return query;
    }

    public static class SearchPaginator extends AbstractPaginator
    {
        private final static long serialVersionUID = 3823389610985272265L;
        private final SolrSearch search;

        private List<FacetField> facetFieldsResult = null;
        private Map<String, Integer> facetQueriesResult = null;

        public SearchPaginator(SolrSearch search)
        {
            super(search.getResultsPerPage());
            this.search = search;
        }

        @Override
        public synchronized List<ResourceDecorator> getCurrentPage() throws SQLException, IOException, SolrServerException
        {
            if(getCurrentPageCache() != null)
                return getCurrentPageCache();

            List<ResourceDecorator> results = search.getResourcesByPage(getPageIndex() + 1);
            setTotalResults((int) search.getTotalResultCount());
            facetFieldsResult = search.getFacetFields();
            facetQueriesResult = search.getFacetQueries();

            setCurrentPageCache(results);

            return results;
        }

        public List<FacetField> getFacetFields() throws SQLException, IOException, SolrServerException
        {
            if(facetFieldsResult == null)
            {
                getCurrentPage();
            }

            return facetFieldsResult;
        }

        public Map<String, Integer> getFacetQueries() throws SQLException, IOException, SolrServerException
        {
            if(facetQueriesResult == null)
            {
                getCurrentPage();
            }

            return facetQueriesResult;
        }
    }

}

package de.l3s.learnweb.resource.search;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.interwebj.IllegalResponseException;
import de.l3s.interwebj.InterWeb;
import de.l3s.interwebj.SearchQuery;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.filters.FilterType;
import de.l3s.learnweb.resource.search.solrClient.SolrSearch;
import de.l3s.learnweb.user.User;

public class Search implements Serializable
{
    private static final long serialVersionUID = -2405235188000105509L;
    private static final Logger log = LogManager.getLogger(Search.class);

    private final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // SimpleDateFormat isn't thread save don't make it static again
    private final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private String query;
    private SearchMode configMode;
    private Integer configResultsPerService = 8;
    private String configGroupResultsByField = null;
    private Integer configResultsPerGroup = 2;

    // all resources
    private LinkedList<ResourceDecorator> resources = new LinkedList<>();

    // resources grouped by pages
    private int temporaryId = 1;
    private HashMap<Integer, LinkedList<ResourceDecorator>> pages = new HashMap<>();
    private HashMap<Integer, ResourceDecorator> rankIndex = new HashMap<>(); // makes it possible to retrieve resources by its rank
    private TreeSet<String> urlHashMap = new TreeSet<>(); // used to make sure that resources with the same url appear only once in the search results

    private int userId;
    private InterWeb interweb;
    private SolrSearch solrSearch;
    private SearchFilters searchFilters;

    private boolean hasMoreResults = true;
    private boolean hasMoreLearnwebResults = true;
    private boolean hasMoreInterwebResults = true;

    private int removedResourceCount = 0;
    private int interwebPageOffset = 0;
    private boolean stopped;
    private transient SearchLogManager searchLogger;
    private int searchId;
    private User user;

    public Search(InterWeb interweb, String query, SearchFilters sf, User user)
    {
        this.interweb = interweb;
        this.query = query;
        this.searchFilters = sf;
        this.userId = (null == user) ? -1 : user.getId();
        this.user = user;
        this.solrSearch = new SolrSearch(query, user);

        if(query.startsWith("source:") || query.startsWith("location:") || query.startsWith("groups:") || query.startsWith("title:"))
        {
            hasMoreInterwebResults = false;
        }
    }

    private LinkedList<ResourceDecorator> doSearch(int page)
    {

        LinkedList<ResourceDecorator> newResources = new LinkedList<>();

        log.debug("Search page " + page + " for: " + query);

        try
        {
            if(hasMoreResults && !stopped)
            {
                if(hasMoreLearnwebResults && !searchFilters.isLearnwebSearchEnabled())
                {
                    hasMoreLearnwebResults = false;
                }

                if(hasMoreInterwebResults && !searchFilters.isInterwebSearchEnabled())
                {
                    hasMoreInterwebResults = false;
                }

                // get results from LearnWeb
                if(hasMoreLearnwebResults)
                {
                    newResources.addAll(getLearnwebResults(page));
                }

                // get results from InterWeb
                if(hasMoreInterwebResults)
                {
                    // on the first page get results from Interweb, only when Learnweb does not return results
                    if(page == 1 && hasMoreLearnwebResults)
                        interwebPageOffset = -1; // no interweb results were requested. so we have to request page 1 when Learnweb shows the next page
                    else
                        newResources.addAll(getInterwebResults(page + interwebPageOffset));
                }

                if(!hasMoreInterwebResults && !hasMoreLearnwebResults)
                    hasMoreResults = false;

                resources.addAll(newResources);
                pages.put(page, newResources);
            }
            else if(stopped)
            {
                hasMoreResults = false;
            }
        }
        catch(Exception e)
        {
            log.fatal("error during search", e);
        }

        logResources(newResources, page);

        return newResources;
    }

    /**
     * Load resources from SOLR
     *
     * @param page
     * @return
     * @throws SQLException
     * @throws SolrServerException
     */
    private LinkedList<ResourceDecorator> getLearnwebResults(int page) throws SQLException, IOException, SolrServerException
    {
        //long start = System.currentTimeMillis();

        // Setup filters
        if(page == 1)
        {
            this.solrSearch.setFacetFields(searchFilters.getFacetFields());
            if(!searchFilters.isInterwebSearchEnabled())
                this.solrSearch.setFacetQueries(searchFilters.getFacetQueries());
        }

        this.solrSearch.setFilterType(configMode == SearchMode.text ? "web" : configMode.name());
        if(searchFilters.getServiceFilter() != null)
        {
            this.solrSearch.setFilterLocation(searchFilters.getServiceFilter());
            this.solrSearch.setResultsPerPage(configResultsPerService * 4);
        }
        else
        {
            this.solrSearch.setResultsPerPage(configResultsPerService);
        }

        if(this.configGroupResultsByField != null)
        {
            this.solrSearch.setGroupResultsByField(this.configGroupResultsByField);
            this.solrSearch.setGroupResultsLimit(this.configResultsPerGroup);
        }

        if(searchFilters.getDateFromFilterAsString() != null)
            this.solrSearch.setFilterDateFrom(SOLR_DATE_FORMAT.format(searchFilters.getDateFromFilter()));
        if(searchFilters.getDateToFilterAsString() != null)
            this.solrSearch.setFilterDateTo(SOLR_DATE_FORMAT.format(searchFilters.getDateToFilter()));
        if(searchFilters.getGroupFilter() != null)
            this.solrSearch.setFilterGroups(Integer.parseInt(searchFilters.getGroupFilter()));
        if(searchFilters.getCollectorFilter() != null)
            this.solrSearch.setFilterCollector(searchFilters.getCollectorFilter());
        if(searchFilters.getAuthorFilter() != null)
            this.solrSearch.setFilterAuthor(searchFilters.getAuthorFilter());
        if(searchFilters.getCoverageFilter() != null)
            this.solrSearch.setFilterCoverage(searchFilters.getCoverageFilter());
        if(searchFilters.getPublisherFilter() != null)
            this.solrSearch.setFilterPublisher(searchFilters.getPublisherFilter());
        if(searchFilters.getTagsFilter() != null)
            this.solrSearch.setFilterTags(searchFilters.getTagsFilter());

        List<ResourceDecorator> learnwebResources = solrSearch.getResourcesByPage(page);

        //log.debug("Solr returned " + learnwebResources.size() + " results in " + (System.currentTimeMillis() - start) + " ms");

        if(stopped)
            return null;

        if(page == 1)
        {
            searchFilters.putResourceCounter(solrSearch.getQueryResponse().getFacetFields());
            if(!searchFilters.isInterwebSearchEnabled())
            {
                searchFilters.putResourceCounter(solrSearch.getQueryResponse().getFacetQuery());
            }

            searchFilters.setTotalResultsLearnweb(solrSearch.getQueryResponse().getResults().getNumFound());
        }

        if(learnwebResources.isEmpty())
            hasMoreLearnwebResults = false;

        int privateResourceCount = 0; // number of resources that match the query but will not be displayed to the user
        int duplicatedUrlCount = 0; // number of resources that already displayed to the user
        int notSatisfyFiltersCount = 0; // number of resources that not satisfy filters like video duration or image size

        LinkedList<ResourceDecorator> newResources = new LinkedList<>();

        for(ResourceDecorator decoratedResource : learnwebResources)
        {
            Resource resource = decoratedResource.getResource();

            if(resource.getId() > 0 && resource.getGroupId() == 0 && resource.getUserId() != userId)
            {
                // the resource is stored in learnweb, belongs to no group and the current user is not the owner
                // of the resource. So he is not allowed to view the resource
                privateResourceCount++;
                continue;
            }

            // check if an other resource with the same url exists
            // Yovisto urls are not unique in this case we use the file url
            if(!urlHashMap.add(StringUtils.isNotBlank(resource.getFileUrl()) ? resource.getFileUrl() : resource.getUrl()))
            {
                duplicatedUrlCount++;
                continue;
            }

            if(!searchFilters.checkAfterLoadFilters(decoratedResource))
            {
                notSatisfyFiltersCount++;
                continue;
            }

            decoratedResource.setRank(temporaryId);

            rankIndex.put(temporaryId, decoratedResource);
            temporaryId++;
            //Learnweb.getInstance().getArchiveUrlManager().checkWaybackCaptures(decoratedResource);
            newResources.add(decoratedResource);
        }

        if(notSatisfyFiltersCount > 0 || privateResourceCount > 0 || duplicatedUrlCount > 0)
        {
            this.removedResourceCount += duplicatedUrlCount + privateResourceCount + notSatisfyFiltersCount;
            log.debug("Filtered " + notSatisfyFiltersCount + " resources and skipped " + privateResourceCount + " private resources, " + duplicatedUrlCount + " duplicated resources");
        }

        return newResources;
    }

    private LinkedList<ResourceDecorator> getInterwebResults(int page) throws IOException, IllegalResponseException
    {
        long start = System.currentTimeMillis();

        // Setup filters
        TreeMap<String, String> params = new TreeMap<>();
        params.put("media_types", configMode.name());
        params.put("page", Integer.toString(page));
        params.put("timeout", "50");

        if(searchFilters.getServiceFilter() != null)
        {
            params.put("services", searchFilters.getServiceFilter());
            params.put("number_of_results", String.valueOf(configResultsPerService * 4));
        }
        else
        {
            if(configMode == SearchMode.text)
            {
                params.put("services", "Bing");
            }
            else if(configMode == SearchMode.image)
            {
                params.put("services", "Flickr,Bing,Ipernity");
            }
            else if(configMode == SearchMode.video)
            {
                params.put("services", "YouTube,Vimeo");
            }
            params.put("number_of_results", configResultsPerService.toString());
        }

        if(searchFilters.getDateFromFilterAsString() != null)
            params.put("date_from", DEFAULT_DATE_FORMAT.format(searchFilters.getDateFromFilter()));

        if(searchFilters.getDateToFilterAsString() != null)
            params.put("date_to", DEFAULT_DATE_FORMAT.format(searchFilters.getDateToFilter()));

        if(searchFilters.getLanguageFilter() != null)
            params.put("language", searchFilters.getLanguageFilter());

        SearchQuery interwebResponse = interweb.search(query, params);
        List<ResourceDecorator> interwebResults = interwebResponse.getResults();
        log.debug("Interweb returned " + interwebResults.size() + " results in " + (System.currentTimeMillis() - start) + " ms");

        if(stopped)
            return null;

        if(page == 1)
        {
            searchFilters.putResourceCounter(FilterType.service, interwebResponse.getResultCountPerService(), true);
            searchFilters.setTotalResultsInterweb(interwebResponse.getTotalResultCount());
        }

        if(interwebResults.isEmpty())
            hasMoreInterwebResults = false;

        int duplicatedUrlCount = 0; // number of resources that already displayed to the user
        int notSatisfyFiltersCount = 0; // number of resources that not satisfy filters like video duration or image size

        LinkedList<ResourceDecorator> newResources = new LinkedList<>();

        for(ResourceDecorator decoratedResource : interwebResults)
        {
            if(null == decoratedResource.getUrl())
            {
                log.warn("url is null: " + decoratedResource);
                continue;
            }
            // check if an other resource with the same url exists
            if(!urlHashMap.add(decoratedResource.getUrl()))
            {
                duplicatedUrlCount++;
                continue;
            }

            if(!searchFilters.checkAfterLoadFilters(decoratedResource))
            {
                notSatisfyFiltersCount++;
                continue;
            }

            decoratedResource.setRank(temporaryId);

            rankIndex.put(temporaryId, decoratedResource);
            temporaryId++;

            if(configMode == SearchMode.text)
                Learnweb.getInstance().getArchiveUrlManager().checkWaybackCaptures(decoratedResource);
            newResources.add(decoratedResource);
        }

        if(notSatisfyFiltersCount > 0 || duplicatedUrlCount > 0)
        {
            this.removedResourceCount += duplicatedUrlCount + notSatisfyFiltersCount;
            log.debug("Filtered " + notSatisfyFiltersCount + " resources and skipped " + duplicatedUrlCount + " duplicated resources");
        }

        return newResources;
    }

    public String getQuery()
    {
        return query;
    }

    public void setMode(SearchMode searchMode)
    {
        this.configMode = searchMode;
        searchFilters.setMode(searchMode);
    }

    public SearchMode getMode()
    {
        return configMode;
    }

    public void setResultsPerService(Integer configResultsPerService)
    {
        this.configResultsPerService = configResultsPerService;
    }

    public Integer getResultsPerService()
    {
        return this.configResultsPerService;
    }

    public String getConfigGroupResultsByField()
    {
        return configGroupResultsByField;
    }

    public void setConfigGroupResultsByField(String configGroupResultsByField)
    {
        this.configGroupResultsByField = configGroupResultsByField;
    }

    public Integer getConfigResultsPerGroup()
    {
        return configResultsPerGroup;
    }

    public void setConfigResultsPerGroup(Integer configResultsPerGroup)
    {
        this.configResultsPerGroup = configResultsPerGroup;
    }

    public int getRemovedResourceCount()
    {
        return this.removedResourceCount;
    }

    /**
     * @return All resources that have been loaded until now. So you have to use get getResourcesByPage first
     */
    public LinkedList<ResourceDecorator> getResources()
    {
        return resources;
    }

    /**
     * @return All resources that have been loaded
     */
    public List<GroupedResources> getResourcesGroupedBySource(Integer limit, final ResourceService searchService)
    {
        List<GroupedResources> resourcesByGroups = new ArrayList<>();

        for(ResourceDecorator res : resources)
        {
            GroupedResources resGroup = new GroupedResources();
            resGroup.setGroupName(res.getLocation());

            //log.debug("getResourcesGroupedBySource: resource " + res.getId() + " location " + res.getLocation());

            if(res.getLocation().equalsIgnoreCase(searchService.name()))
                continue;

            if(resourcesByGroups.contains(resGroup))
            {
                resGroup = resourcesByGroups.get(resourcesByGroups.indexOf(resGroup));
                if(resGroup.getResources().size() < limit)
                {
                    resGroup.addResource(res);
                }
            }
            else
            {
                resGroup.setTotalResources(searchFilters.getTotalResources(FilterType.service, resGroup.getGroupAlias()).intValue());
                resGroup.addResource(res);
                resourcesByGroups.add(resGroup);
            }
        }

        return resourcesByGroups;
    }

    /**
     * May return null.
     *
     * @param page
     * @return
     */
    public synchronized LinkedList<ResourceDecorator> getResourcesByPage(int page)
    {
        if(page == 2)
            getResourcesByPage(1);

        if(page > 50)
        {
            log.error("Requested more than 50 pages; Stopping", new Exception());
            return null;
        }

        // log.debug("called doSearch for page: " + page);
        LinkedList<ResourceDecorator> res = pages.get(page);

        if(null == res)
            return doSearch(page);

        return res;
    }

    public boolean isHasMoreResults()
    {
        return hasMoreResults;
    }

    public ResourceDecorator getResourceByRank(int rank)
    {
        return rankIndex.get(rank);
    }

    public void stop()
    {
        this.stopped = true;
    }

    public class GroupedResources implements Serializable, Comparable<GroupedResources>
    {
        private static final long serialVersionUID = -1060339894351517966L;

        String groupName;
        String groupAlias;
        Integer totalResources;
        LinkedList<ResourceDecorator> resources;

        @Override
        public boolean equals(final Object o)
        {
            if(this == o)
                return true;
            if(o == null || getClass() != o.getClass())
                return false;
            final GroupedResources that = (GroupedResources) o;
            return Objects.equals(groupAlias, that.groupAlias);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(groupAlias);
        }

        @Override
        public int compareTo(GroupedResources another)
        {
            if(this.getTotalResources() > another.getTotalResources())
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }

        public String getGroupName()
        {
            return groupName;
        }

        public void setGroupName(String groupName)
        {
            try
            {
                ResourceService src = ResourceService.parse(groupName);
                this.groupAlias = src.name();
                this.groupName = src.toString();
            }
            catch(IllegalArgumentException e)
            {
                this.groupAlias = groupName.toLowerCase();
                this.groupName = groupName;
            }
        }

        public String getGroupAlias()
        {
            return groupAlias;
        }

        public Integer getTotalResources()
        {
            return totalResources > resources.size() ? totalResources : resources.size();
        }

        public void setTotalResources(Integer totalResources)
        {
            this.totalResources = totalResources;
        }

        public LinkedList<ResourceDecorator> getResources()
        {
            return resources;
        }

        public void setResources(LinkedList<ResourceDecorator> resources)
        {
            this.resources = resources;
        }

        public void addResource(ResourceDecorator resources)
        {
            if(this.resources == null)
            {
                this.resources = new LinkedList<>();
            }

            this.resources.add(resources);
        }
    }

    /**
     *
     * @return Unique id is generated when the query has been logged by logQuery()
     */
    public int getId()
    {
        return searchId;
    }

    public void logQuery(String query, ResourceService searchService, String language, String queryFilters)
    {
        searchId = getSearchLogger().logQuery(query, getMode(), searchService, language, queryFilters, this.user);
    }

    private SearchLogManager getSearchLogger()
    {
        if(searchLogger == null)
            searchLogger = Learnweb.getInstance().getSearchLogManager();

        return searchLogger;
    }

    private void logResources(List<ResourceDecorator> resources, int pageId)
    {
        /*if(searchId > 0) // log resources only when logQuery() was called before; This isn't the case on the group search page
            getSearchLogger().logResources(searchId, resources);*/

        //call the method to fetch the html of the logged resources
        //only if search_mode='text' and userId is admin/specificUser
        if(searchId > 0)
            getSearchLogger().logResources(searchId, resources, pageId);
    }

    public void logResourceClicked(int rank, User user)
    {
        getSearchLogger().logResourceClicked(searchId, rank, user);
    }

    /**
     * @param rank
     * @param user
     * @param newResourceId Id of the new stored resource
     */
    public void logResourceSaved(int rank, User user, int newResourceId)
    {
        getSearchLogger().logResourceSaved(searchId, rank, user, newResourceId);
    }
}

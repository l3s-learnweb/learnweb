package de.l3s.learnweb;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.interwebj.IllegalResponseException;
import de.l3s.interwebj.InterWeb;
import de.l3s.interwebj.SearchQuery;
import de.l3s.learnweb.SearchFilters.FILTERS;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.util.StringHelper;

public class Search implements Serializable
{
    private static final long serialVersionUID = -2405235188000105509L;
    final static Logger log = Logger.getLogger(Search.class);

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private String query;
    private MODE configMode;
    private Integer configResultsPerService = 8;

    // all resources
    private LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();

    // resources grouped by pages
    private int temporaryId = 1;
    private HashMap<Integer, LinkedList<ResourceDecorator>> pages = new HashMap<Integer, LinkedList<ResourceDecorator>>();
    private HashMap<Integer, Resource> tempIdIndex = new HashMap<Integer, Resource>(); // makes it possible to retrieve resources by its tempId
    private TreeSet<String> urlHashMap = new TreeSet<String>(); // used to make sure that resources with the same url appear only once in the search results

    private int userId;
    private InterWeb interweb;
    private SolrSearch solrSearch;
    private SearchFilters searchFilters;

    private boolean hasMoreResults = true;
    private boolean hasMoreLearnwebResults = true;
    private boolean hasMoreInterwebResults = true;

    private int interwebPageOffset = 0;
    private boolean stopped;

    public Search(InterWeb interweb, String query, SearchFilters sf, User user)
    {
	this.interweb = interweb;
	this.query = query;
	this.searchFilters = sf;
	this.userId = (null == user) ? -1 : user.getId();
	this.solrSearch = new SolrSearch(query, user);

	if(query.startsWith("source:") || query.startsWith("location:") || query.startsWith("groups:") || query.startsWith("title:"))
	{
	    hasMoreInterwebResults = false;
	}
    }

    private LinkedList<ResourceDecorator> doSearch(int page)
    {

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

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
    private LinkedList<ResourceDecorator> getLearnwebResults(int page) throws SQLException, SolrServerException
    {
	long start = System.currentTimeMillis();

	// Setup filters
	if(page == 1)
	{
	    this.solrSearch.setFacetFields(searchFilters.getFacetFields());
	    if(!searchFilters.isInterwebSearchEnabled())
		this.solrSearch.setFacetQueries(searchFilters.getFacetQueries());
	}

	this.solrSearch.setFilterType(configMode.name());
	if(searchFilters.getServiceFilter() != null)
	{
	    this.solrSearch.setFilterLocation(searchFilters.getServiceFilter());
	    this.solrSearch.setResultsPerPage(configResultsPerService * 4);
	}
	else
	{
	    this.solrSearch.setResultsPerPage(configResultsPerService);
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
	log.debug("Solr returned " + learnwebResources.size() + " results in " + (System.currentTimeMillis() - start) + " ms");

	if(stopped)
	    return null;

	if(page == 1)
	{
	    searchFilters.putResourceCounter(solrSearch.getFacetFields());
	    if(!searchFilters.isInterwebSearchEnabled())
	    {
		searchFilters.putResourceCounter(solrSearch.getFacetQueries());
	    }
	}

	if(learnwebResources.size() == 0)
	    hasMoreLearnwebResults = false;

	int privateResourceCount = 0; // number of resources that match the query but will not be displayed to the user
	int duplicatedUrlCount = 0; // number of resources that already displayed to the user
	int notSatisfyFiltersCount = 0; // number of resources that not satisfy filters like video duration or image size

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

	for(ResourceDecorator decoratedResource : learnwebResources)
	{
	    Resource resource = decoratedResource.getResource();

	    if(resource.getId() > 0 && resource.getGroupId() == 0 && resource.getOwnerUserId() != userId)
	    {
		// the resource is stored in learnweb, belongs to no group and the current user is not the owner 
		// of the resource. So he is not allowed to view the resource
		privateResourceCount++;
		continue;
	    }

	    // check if an other resource with the same url exists
	    // Yovisto urls are not unique in this case we use the file url
	    if(!urlHashMap.add(!StringHelper.empty(resource.getFileUrl()) ? resource.getFileUrl() : resource.getUrl()))
	    {
		duplicatedUrlCount++;
		continue;
	    }

	    if(!searchFilters.checkAfterLoadFilters(decoratedResource))
	    {
		notSatisfyFiltersCount++;
		continue;
	    }

	    decoratedResource.setTempId(temporaryId);

	    tempIdIndex.put(temporaryId, decoratedResource.getResource());
	    temporaryId++;
	    //Learnweb.getInstance().getArchiveUrlManager().checkWaybackCaptures(decoratedResource);
	    newResources.add(decoratedResource);
	}

	if(notSatisfyFiltersCount > 0 || privateResourceCount > 0 || duplicatedUrlCount > 0)
	    log.debug("Filtered " + notSatisfyFiltersCount + " resources and skipped " + privateResourceCount + " private resources, " + duplicatedUrlCount + " dublicated resources");

	return newResources;
    }

    private LinkedList<ResourceDecorator> getInterwebResults(int page) throws IOException, IllegalResponseException
    {
	// TODO: If have problems implemented it here (I don't understand why we do it)
	/* do // we have loop here when stopped = true
	{
		newResources.addAll(getInterwebResults(page + interwebPageOffset));
		if(newResources.size() > 0 || !hasMoreInterwebResults)
			break;
		interwebPageOffset++;
	}
	while(true); */

	long start = System.currentTimeMillis();

	// Setup filters
	TreeMap<String, String> params = new TreeMap<String, String>();
	params.put("media_types", configMode.getInterwebName());
	params.put("page", Integer.toString(page));
	params.put("timeout", "50");

	if(searchFilters.getServiceFilter() != null)
	{
	    params.put("services", searchFilters.getServiceFilter());
	    params.put("number_of_results", String.valueOf(configResultsPerService * 4));
	}
	else
	{
	    if(configMode == MODE.web)
	    {
		params.put("services", "Bing");
	    }
	    else if(configMode == MODE.image)
	    {
		params.put("services", "Flickr,Bing,Ipernity");
	    }
	    else if(configMode == MODE.video)
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
	    searchFilters.putResourceCounter(FILTERS.service, interwebResponse.getResultCountPerService(), true);
	}

	if(interwebResults.size() == 0)
	    hasMoreInterwebResults = false;

	int duplicatedUrlCount = 0; // number of resources that already displayed to the user
	int notSatisfyFiltersCount = 0; // number of resources that not satisfy filters like video duration or image size

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

	for(ResourceDecorator decoratedResource : interwebResults)
	{
	    if(null == decoratedResource.getUrl())
	    {
		log.warn("url is null: " + decoratedResource.toString());
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

	    decoratedResource.setTempId(temporaryId);

	    tempIdIndex.put(temporaryId, decoratedResource.getResource());
	    temporaryId++;
	    Learnweb.getInstance().getArchiveUrlManager().checkWaybackCaptures(decoratedResource);
	    newResources.add(decoratedResource);
	}

	if(notSatisfyFiltersCount > 0 || duplicatedUrlCount > 0)
	    log.debug("Filtered " + notSatisfyFiltersCount + " resources and skipped " + duplicatedUrlCount + " dublicated resources");

	return newResources;
    }

    public String getQuery()
    {
	return query;
    }

    public void setMode(MODE searchMode)
    {
	this.configMode = searchMode;
	searchFilters.setMode(searchMode);
    }

    public MODE getMode()
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

    /**
     * 
     * @return All resources that have been loaded
     */
    public LinkedList<ResourceDecorator> getResources()
    {
	return resources;
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
	    log.fatal("Requested more than 50 pages", new Exception());
	    return null;
	}

	//	log.debug("called doSearch for page: " + page);
	LinkedList<ResourceDecorator> res = pages.get(page);

	if(null == res)
	    return doSearch(page);

	return res;
    }

    public boolean isHasMoreResults()
    {
	return hasMoreResults;
    }

    public Resource getResourceByTempId(int tempId)
    {
	return tempIdIndex.get(tempId);
    }

    public void stop()
    {
	this.stopped = true;
    }
}

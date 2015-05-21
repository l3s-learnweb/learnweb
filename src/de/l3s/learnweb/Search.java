package de.l3s.learnweb;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.interwebj.IllegalResponseException;
import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.util.StringHelper;

public class Search implements Serializable
{
    private static final long serialVersionUID = -2405235188000105509L;
    final static Logger log = Logger.getLogger(Search.class);

    public enum SERVICE
    {
	Bing,
	Flickr,
	YouTube,
	Vimeo,
	SlideShare,
	Ipernity,
	TED,
	Learnweb
    };

    public enum MEDIA
    {
	image,
	presentation,
	text,
	video
    };

    public enum MODE
    {
	image,
	web,
	video,
	multimedia
    };

    private String query;
    private List<String> configMedia = new ArrayList<String>(Arrays.asList("image", "presentation", "video")); // the media types to search for
    private List<String> configService = new ArrayList<String>(Arrays.asList("Flickr", "YouTube", "Vimeo", "SlideShare", "Ipernity")); // the services to search in
    private Integer configResultsPerService = 8;
    private String configLanguage;

    // all resources
    private LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();

    // resources grouped by pages
    private HashMap<Integer, LinkedList<ResourceDecorator>> pages = new HashMap<Integer, LinkedList<ResourceDecorator>>();
    private HashMap<Integer, Resource> tempIdIndex = new HashMap<Integer, Resource>(); // makes it possible to retrieve resources by its tempId
    private int temporaryId = 1;
    private TreeSet<String> urlHashMap = new TreeSet<String>();
    private InterWeb interweb;
    private MODE mode;
    private boolean hasMoreResults = true;
    private boolean hasMoreLearnwebResults = true;
    private boolean hasMoreInterwebResults = true;
    private SolrSearch solrSearch;
    private int userId;

    private int interwebPageOffset = 0;
    private boolean stopped;

    public Search(InterWeb interweb, String query, User user)
    {
	urlHashMap.add("http://vimeo.com/735450"); // a really stupid video, that appears in many search results

	this.query = query;
	this.interweb = interweb;
	this.userId = (null == user) ? -1 : user.getId();
	this.solrSearch = new SolrSearch(query, user);

	if(query.startsWith("source:"))
	    hasMoreInterwebResults = false; // disable interweb for advanced search queries
    }

    private LinkedList<ResourceDecorator> doSearch(int page)
    {

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

	log.debug("Search page " + page + " for: " + query);

	try
	{

	    if(page != 1 && hasMoreInterwebResults)
	    {
		do
		{
		    newResources.addAll(getInterwebResults(page + interwebPageOffset));

		    if(newResources.size() > 0 || !hasMoreInterwebResults)
			break;

		    interwebPageOffset++;
		}
		while(true);
	    }

	    // get results from Learnweb
	    if(hasMoreLearnwebResults)
	    {
		newResources.addAll(getLearnwebResults(page));
	    }

	    if(page == 1) // on the first page get results from Interweb, only when Learnweb does not return results
	    {
		if(!hasMoreLearnwebResults)
		    newResources = getInterwebResults(page);
		else
		    interwebPageOffset = -1; // no interweb results were requested. so we have to request page 1 when Learnweb shows the next page
	    }

	    if(!hasMoreInterwebResults && !hasMoreLearnwebResults)
		hasMoreResults = false;

	    resources.addAll(newResources);
	    pages.put(page, newResources);
	}
	catch(Exception e)
	{
	    log.fatal("error during search", e);
	}

	return newResources;
    }

    private LinkedList<ResourceDecorator> getLearnwebResults(int page) throws SQLException, SolrServerException
    {
	long start = System.currentTimeMillis();

	List<ResourceDecorator> learnwebResources = solrSearch.getResourcesByPage(page);

	if(stopped)
	    return null;

	log.debug("Solr returned " + learnwebResources.size() + " results in " + (System.currentTimeMillis() - start) + "ms");

	if(learnwebResources.size() == 0)
	    hasMoreLearnwebResults = false;

	int privateResourceCount = 0; // number of resources that match the query but will not be displayed to the user

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

	// copy results
	for(ResourceDecorator decoratedResource : learnwebResources)
	{
	    Resource resource = decoratedResource.getResource();

	    if(resource.getId() > 0 && resource.getGroups().size() == 0 && resource.getOwnerUserId() != userId)
	    {
		// the resource is stored in learnweb, belongs to no group and the current user is not the owner 
		// of the resource. So he is not allowed to view the resource
		//System.out.println(resource);
		privateResourceCount++;
		continue;
	    }

	    // check if an other resource with the same url exists
	    if(!urlHashMap.add(resource.getUrl()))
		continue;

	    decoratedResource.setTempId(temporaryId);

	    tempIdIndex.put(temporaryId, resource);
	    temporaryId++;

	    newResources.add(decoratedResource);
	}

	if(privateResourceCount > 0)
	    log.error("Skipped " + privateResourceCount + " private resources");

	return newResources;
    }

    private LinkedList<ResourceDecorator> getInterwebResults(int page) throws IOException, IllegalResponseException
    {
	if(configService.size() == 0 || stopped)
	{
	    hasMoreInterwebResults = false;
	    return new LinkedList<ResourceDecorator>();
	}

	TreeMap<String, String> params = new TreeMap<String, String>();
	params.put("media_types", StringHelper.implode(configMedia, ","));
	params.put("services", StringHelper.implode(configService, ","));
	params.put("number_of_results", configResultsPerService.toString());
	params.put("page", Integer.toString(page));
	params.put("timeout", "50");

	if(configLanguage != null)
	    params.put("language", configLanguage);

	List<ResourceDecorator> interwebResults = interweb.search(query, params);

	if(stopped)
	    return new LinkedList<ResourceDecorator>();

	if(interwebResults.size() == 0)
	    hasMoreInterwebResults = false;

	log.debug("Interweb returned " + interwebResults.size() + " results");

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

	// copy results
	for(ResourceDecorator decoratedResource : interwebResults)
	{
	    // check if an other resource with the same url exists
	    if(!urlHashMap.add(decoratedResource.getUrl()))
		continue;

	    decoratedResource.setTempId(temporaryId);

	    tempIdIndex.put(temporaryId, decoratedResource.getResource());
	    temporaryId++;

	    newResources.add(decoratedResource);
	}

	/*
	if(page < 4) // cache the best new results
	    SolrCache.getInstance().cacheResources(newResources);
	*/

	return newResources;
    }

    public String getQuery()
    {
	return query;
    }

    /**
     * This function sets the default media and service options for the defined search mode
     * 
     * @param mode
     */
    public void setMode(MODE searchMode)
    {
	this.solrSearch.setFilterType(searchMode.name());

	this.mode = searchMode;
	switch(searchMode)
	{
	case image:
	    setMedia(MEDIA.image);
	    setService(SERVICE.Bing, SERVICE.Flickr, SERVICE.Ipernity); // , SERVICE.SlideShare  
	    break;
	case web:
	    setMedia(MEDIA.text);
	    setService(SERVICE.Bing);
	    break;
	case video:
	    setMedia(MEDIA.video);
	    setService(SERVICE.YouTube, SERVICE.Vimeo); // , SERVICE.Flickr , SERVICE.SlideShare
	    break;
	case multimedia:
	    setMedia(MEDIA.video, MEDIA.image, MEDIA.text, MEDIA.presentation);
	    setService(SERVICE.Flickr, SERVICE.YouTube, SERVICE.Vimeo, SERVICE.SlideShare, SERVICE.Ipernity);
	    break;
	default:
	    throw new IllegalArgumentException("unknown searchmode: " + searchMode);
	}
    }

    public MODE getMode()
    {
	return mode;
    }

    public void setMedia(MEDIA... args)
    {
	configMedia.clear();
	for(MEDIA media : args)
	    configMedia.add(media.name());
    }

    public void setService(SERVICE... args)
    {
	configService.clear();
	for(SERVICE service : args)
	    configService.add(service.name());
    }

    public void setResultsPerService(Integer configResultsPerService)
    {
	this.configResultsPerService = configResultsPerService;
	this.solrSearch.setResultsPerPage(configResultsPerService);
    }

    /**
     * Search config:
     * The language resources should be in
     * 
     * @param configLanguage
     */
    public void setLanguage(String configLanguage)
    {
	this.configLanguage = configLanguage;
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

	if(page > 50)
	{
	    log.fatal("requested more than 50 pages", new Exception());
	    return null;
	}

	log.debug("called doSearch" + page);
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

    /*
    //dataTypeCount example {Video:5, Audio:8, ....}
    private HashMap<String, Integer> dataTypeCount = new HashMap<String, Integer>();
    private HashMap<String, Integer> sourceCount = new HashMap<String, Integer>();
    
    private void countData(Resource res)
    {
    	String type = StringHelper.ucFirst(res.getType());
    	if(type.length() > 12)
    		type = type.substring(0, 11) +"...";
    	
    	Integer count = dataTypeCount.get(type);
    	if (null == count)
    	{
    		dataTypeCount.put(type, 1);
    		if(configPage == 1)
    			typeFilterInput.add(type); // set this value preselected 
    	}
    	else dataTypeCount.put(type, count + 1);
    	
    	String source = res.getSource();
    	if(source.length() < 3)
    		return;
    	count = sourceCount.get(source);
    	if (null == count)
    	{
    		sourceCount.put(source, 1);
    		if(configPage == 1)
    			sourceFilterInput.add(source);
    	}
    	else sourceCount.put(source, count + 1);
    }
    */
}

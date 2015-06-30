package de.l3s.learnweb;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.interwebj.IllegalResponseException;
import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.util.StringHelper;

public class Search implements Serializable
{
    private static final long serialVersionUID = -2405235188000105509L;
    final static Logger log = Logger.getLogger(Search.class);

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public enum SERVICE
    {
	Bing,
	Flickr,
	YouTube,
	Vimeo,
	SlideShare,
	Ipernity,
	TED, // stored in SOLR
	TEDx, // stored in SOLR
	Loro, // stored in SOLR
	Yovisto, //  stored in SOLR
	LearnWeb // stored in SOLR
	;
	/*
		public String getCustomName()
		{
		    if(this == null)
			return UtilBean.getLocaleMessage("any_service");
		    return this.name();
		    /*
		    switch(this)
		    {
		    case Bing:
		    return "Bing";
		    case Flickr:
		    return "Flickr";
		    case YouTube:
		    return "YouTube";
		    case Vimeo:
		    return "Vimeo";
		    case SlideShare:
		    return "SlideShare";
		    case Ipernity:
		    return "Ipernity";
		    case TED:
		    return "TED";
		    case TEDx:
		    return "TEDx";
		    case Loro:
		    return "Loro";
		    case Yovisto:
		    return "Yovisto";
		    case Learnweb:
		    return "LearnWeb";
		    default:
		    return UtilBean.getLocaleMessage("any_service");
		    
		    }* /
		}
	    */

	public boolean isLearnwebSource()
	{
	    switch(this)
	    {
	    case TED:
		return true;
	    case TEDx:
		return true;
	    case Loro:
		return true;
	    case Yovisto:
		return true;
	    case LearnWeb:
		return true;
	    default:
		return false;
	    }
	}
    };

    public enum DATE
    {
	d, // day
	w, // week (7 days)
	m, // month
	y // year
	;

	public String getCustomName()
	{
	    switch(this)
	    {
	    case d:
		return UtilBean.getLocaleMessage("past_24_hours");
	    case w:
		return UtilBean.getLocaleMessage("past_week");
	    case m:
		return UtilBean.getLocaleMessage("past_month");
	    case y:
		return UtilBean.getLocaleMessage("past_year");
	    default:
		return UtilBean.getLocaleMessage("any_time");
	    }
	}

	@Override
	public String toString()
	{
	    Calendar cal = Calendar.getInstance();
	    switch(this)
	    {
	    case d:
		cal.add(Calendar.DATE, -1);
		break;
	    case w:
		cal.add(Calendar.DATE, -7);
		break;
	    case m:
		cal.add(Calendar.MONTH, -1);
		break;
	    case y:
		cal.add(Calendar.YEAR, -1);
		break;
	    default:
		return null;
	    }

	    return DEFAULT_DATE_FORMAT.format(cal.getTime());
	}
    }

    public enum SIZE
    {
	small,
	medium,
	large,
	extraLarge;

	public String getCustomName()
	{
	    switch(this)
	    {
	    case small:
		return UtilBean.getLocaleMessage("small");
	    case medium:
		return UtilBean.getLocaleMessage("medium");
	    case large:
		return UtilBean.getLocaleMessage("large");
	    case extraLarge:
		return UtilBean.getLocaleMessage("extra_large");
	    default:
		return UtilBean.getLocaleMessage("any_size");
	    }
	}

	public int getMaxWidth()
	{
	    switch(this)
	    {
	    case small:
		return 150;
	    case medium:
		return 600;
	    case large:
		return 1200;
	    case extraLarge:
		return 0;
	    default:
		return 0;
	    }
	}

	public int getMinWidth()
	{
	    switch(this)
	    {
	    case small:
		return 0;
	    case medium:
		return 150;
	    case large:
		return 600;
	    case extraLarge:
		return 1200;
	    default:
		return 0;
	    }
	}
    }

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
	video
    };

    private String query;
    private List<String> configMedia = new ArrayList<String>(Arrays.asList("image", "presentation", "video")); // the media types to search for
    private List<String> configService = new ArrayList<String>(Arrays.asList("Flickr", "YouTube", "Vimeo", "SlideShare", "Ipernity")); // the services to search in
    private DATE configDate;
    private SIZE configSize;
    private Integer configResultsPerService = 8;
    private Integer configResultsPerOneService = 40;
    private String configLanguage;

    // all resources
    private LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();

    // resources grouped by pages
    private HashMap<Integer, LinkedList<ResourceDecorator>> pages = new HashMap<Integer, LinkedList<ResourceDecorator>>();
    private HashMap<Integer, Resource> tempIdIndex = new HashMap<Integer, Resource>(); // makes it possible to retrieve resources by its tempId
    private int temporaryId = 1;
    private TreeSet<String> urlHashMap = new TreeSet<String>(); // used to make sure that resources with the same url appear only once in the search results
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
	this.query = query;
	this.interweb = interweb;
	this.userId = (null == user) ? -1 : user.getId();
	this.solrSearch = new SolrSearch(query, user);

	if(query.startsWith("source:"))
	    hasMoreInterwebResults = false;
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
		if(!hasMoreLearnwebResults && hasMoreInterwebResults)
		    newResources = getInterwebResults(page);
		else
		    interwebPageOffset = -1; // no interweb results were requested. so we have to request page 1 when Learnweb shows the next page
	    }

	    if(!hasMoreInterwebResults && !hasMoreLearnwebResults)
		hasMoreResults = false;

	    resources.addAll(newResources);

	    // Check size filter for image search
	    if(configMedia.size() == 1 && configMedia.get(0).equals("image") && configSize != null)
	    {
		for(Iterator<ResourceDecorator> it = resources.iterator(); it.hasNext();)
		{
		    int width = it.next().getThumbnail4().getWidth(), minWidth = configSize.getMinWidth(), maxWidth = configSize.getMaxWidth();

		    if(minWidth > width || (maxWidth != 0 && width > maxWidth))
		    {
			it.remove();
		    }
		}
	    }

	    pages.put(page, newResources);
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

	log.debug(StringHelper.implode(configService, ","));
	this.solrSearch.setFilterSource(StringHelper.implode(configService, ","));
	this.solrSearch.setResultsPerPage(configService.size() > 1 ? configResultsPerService : configResultsPerOneService);
	this.solrSearch.setFilterType(mode.name());
	List<ResourceDecorator> learnwebResources = solrSearch.getResourcesByPage(page);

	if(stopped)
	    return null;

	log.debug("Solr returned " + learnwebResources.size() + " results in " + (System.currentTimeMillis() - start) + "ms");

	if(learnwebResources.size() == 0)
	    hasMoreLearnwebResults = false;

	int privateResourceCount = 0; // number of resources that match the query but will not be displayed to the user
	int duplicatedUrlCount = 0;

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

	// copy results
	for(ResourceDecorator decoratedResource : learnwebResources)
	{
	    Resource resource = decoratedResource.getResource();

	    if(resource.getId() > 0 && resource.getGroups().size() == 0 && resource.getOwnerUserId() != userId)
	    {
		// the resource is stored in learnweb, belongs to no group and the current user is not the owner 
		// of the resource. So he is not allowed to view the resource
		privateResourceCount++;
		continue;
	    }

	    // check if an other resource with the same url exists; Yovisto urls are not unique in this case we use the file url
	    String url = !StringHelper.empty(resource.getFileUrl()) ? resource.getFileUrl() : resource.getUrl();

	    if(!urlHashMap.add(url))
	    {
		duplicatedUrlCount++;
		continue;
	    }

	    decoratedResource.setTempId(temporaryId);

	    tempIdIndex.put(temporaryId, resource);
	    temporaryId++;

	    newResources.add(decoratedResource);
	}

	if(privateResourceCount > 0 || duplicatedUrlCount > 0)
	    log.error("Skipped " + privateResourceCount + " private resources and " + duplicatedUrlCount + " dublicated resources");

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
	params.put("number_of_results", configService.size() > 1 ? configResultsPerService.toString() : configResultsPerOneService.toString());
	params.put("page", Integer.toString(page));
	params.put("timeout", "50");

	if(configDate != null)
	{
	    params.put("date_from", configDate.toString());
	}

	if(configLanguage != null)
	{
	    params.put("language", configLanguage);
	}

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
	this.mode = searchMode;

	switch(searchMode)
	{
	case image:
	    setMedia(MEDIA.image);
	    setService(SERVICE.Bing, SERVICE.Flickr, SERVICE.Ipernity, SERVICE.Loro, SERVICE.LearnWeb); // , SERVICE.SlideShare  
	    break;
	case web:
	    setMedia(MEDIA.text);
	    setService(SERVICE.Bing, SERVICE.LearnWeb, SERVICE.Loro, SERVICE.SlideShare);
	    break;
	case video:
	    setMedia(MEDIA.video);
	    setService(SERVICE.YouTube, SERVICE.Vimeo, SERVICE.Loro, SERVICE.TED, SERVICE.Yovisto, SERVICE.LearnWeb); // , SERVICE.Flickr , SERVICE.SlideShare
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
	{
	    configMedia.add(media.name());
	}
    }

    public void setService(SERVICE... args)
    {
	configService.clear();

	if(args.length == 1)
	{
	    for(SERVICE service : args)
	    {
		if(service.isLearnwebSource())
		{
		    hasMoreInterwebResults = false; // disable interweb
		}
		/*else
		{
		    hasMoreLearnwebResults = false; // disable learnweb
		}*/

		configService.add(service.name());
	    }
	}
	else
	{
	    for(SERVICE service : args)
	    {
		configService.add(service.name());
	    }
	}
    }

    public void setDate(DATE date)
    {
	this.configDate = date;
    }

    public void setSize(SIZE size)
    {
	this.configSize = size;
    }

    public void setResultsPerService(Integer configResultsPerService)
    {
	this.configResultsPerService = configResultsPerService;
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

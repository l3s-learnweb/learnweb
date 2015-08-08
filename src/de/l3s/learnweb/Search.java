package de.l3s.learnweb;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField.Count;

import de.l3s.interwebj.IllegalResponseException;
import de.l3s.interwebj.InterWeb;
import de.l3s.interwebj.SearchQuery;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.util.StringHelper;

public class Search implements Serializable
{
    private static final long serialVersionUID = -2405235188000105509L;
    final static Logger log = Logger.getLogger(Search.class);

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public enum SERVICE
    {
	Bing,
	Flickr,
	YouTube,
	Vimeo,
	Ipernity,
	TED, // stored in SOLR
	TEDx, // stored in SOLR
	Loro, // stored in SOLR
	Yovisto, //  stored in SOLR
	LearnWeb, // stored in SOLR
	ArchiveIt// stored in SOLR
	;

	public boolean isLearnwebSource()
	{
	    switch(this)
	    {
	    case TED:
	    case TEDx:
	    case Loro:
	    case Yovisto:
	    case LearnWeb:
	    case ArchiveIt:
		return true;
	    default:
		return false;
	    }
	}

	public String getCustomName()
	{
	    switch(this)
	    {
	    case ArchiveIt:
		return UtilBean.getLocaleMessage("Archive-It");
	    default:
		return this.name();
	    }
	}

	public static SERVICE[] getWebServices()
	{
	    return new SERVICE[] { SERVICE.Bing, SERVICE.Loro, SERVICE.ArchiveIt, SERVICE.LearnWeb };
	}

	public static SERVICE[] getImageServices()
	{
	    return new SERVICE[] { SERVICE.Bing, SERVICE.Flickr, SERVICE.Ipernity, SERVICE.Loro, SERVICE.LearnWeb };
	}

	public static SERVICE[] getVideoServices()
	{
	    return new SERVICE[] { SERVICE.YouTube, SERVICE.Vimeo, SERVICE.Loro, SERVICE.TED, SERVICE.TEDx, SERVICE.Yovisto, SERVICE.LearnWeb };
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

	public Date getDate()
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

	    return cal.getTime();
	}

	@Override
	public String toString()
	{
	    return DEFAULT_DATE_FORMAT.format(this.getDate());
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

    public enum DURATION
    {
	s,
	m,
	l;

	public String getCustomName()
	{
	    switch(this)
	    {
	    case s:
		return UtilBean.getLocaleMessage("short");
	    case m:
		return UtilBean.getLocaleMessage("medium");
	    case l:
		return UtilBean.getLocaleMessage("long");
	    default:
		return UtilBean.getLocaleMessage("any_duration");
	    }
	}

	public int getMaxDuration()
	{
	    switch(this)
	    {
	    case s:
		return 300;
	    case m:
		return 1200;
	    case l:
		return 0;
	    default:
		return 0;
	    }
	}

	public int getMinDuration()
	{
	    switch(this)
	    {
	    case s:
		return 0;
	    case m:
		return 300;
	    case l:
		return 1200;
	    default:
		return 0;
	    }
	}
    }

    public enum MODE
    {
	image,
	web,
	video;

	public String getInterwebName()
	{
	    switch(this)
	    {
	    case web:
		return "text";
	    default:
		return this.name();
	    }
	}
    };

    private String query;
    private MODE configMode;
    private DATE configDate;
    private SIZE configSize;
    private DURATION configDuration;
    private String configGroup;
    private Integer configResultsPerService = 8;
    private Integer configResultsPerOneService = 40;
    private String configLanguage;
    private List<SERVICE> configService = new ArrayList<SERVICE>(Arrays.asList(SERVICE.values())); // the services to search in

    // all resources
    private LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();
    private Map<String, Long> resultsCountPerService = new HashMap<String, Long>();
    private List<Count> resultsCountPerGroup;

    // resources grouped by pages
    private int temporaryId = 1;
    private HashMap<Integer, LinkedList<ResourceDecorator>> pages = new HashMap<Integer, LinkedList<ResourceDecorator>>();
    private HashMap<Integer, Resource> tempIdIndex = new HashMap<Integer, Resource>(); // makes it possible to retrieve resources by its tempId
    private TreeSet<String> urlHashMap = new TreeSet<String>(); // used to make sure that resources with the same url appear only once in the search results

    private int userId;
    private InterWeb interweb;
    private SolrSearch solrSearch;

    private boolean hasMoreResults = true;
    private boolean hasMoreLearnwebResults = true;
    private boolean hasMoreInterwebResults = true;

    private int interwebPageOffset = 0;
    private boolean stopped;

    public Search(InterWeb interweb, String query, User user)
    {
	this.interweb = interweb;
	this.query = query;
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
	    if(configService.size() == 0 || stopped)
	    {
		hasMoreResults = false;
	    }
	    else if(hasMoreResults)
	    {
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
	this.solrSearch.setFilterLocation(getServicesForLearnweb(configService));
	this.solrSearch.setResultsPerPage(configService.size() > 1 ? configResultsPerService : configResultsPerOneService);
	this.solrSearch.setFilterType(configMode.name());
	if(configDate != null)
	    this.solrSearch.setFilterDateFrom(SOLR_DATE_FORMAT.format(configDate.getDate()));
	if(configGroup != null)
	    this.solrSearch.setFilterGroups(Integer.parseInt(configGroup));

	List<ResourceDecorator> learnwebResources = solrSearch.getResourcesByPage(page);
	log.debug("Solr returned " + learnwebResources.size() + " results in " + (System.currentTimeMillis() - start) + " ms");

	if(stopped)
	    return null;

	//resultsCountPerGroup.putAll(solrSearch.getResultCountPerGroup());  
	resultsCountPerGroup = solrSearch.getResultCountPerGroup();

	if(page == 1)
	    resultsCountPerService.putAll(solrSearch.getResultCountPerService());

	if(learnwebResources.size() == 0)
	    hasMoreLearnwebResults = false;

	int privateResourceCount = 0; // number of resources that match the query but will not be displayed to the user
	int duplicatedUrlCount = 0; // number of resources that already displayed to the user
	int notSatisfyFiltersCount = 0; // number of resources that not satisfy filters like video duration or image size

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

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

	    // check if an other resource with the same url exists
	    // Yovisto urls are not unique in this case we use the file url
	    if(!urlHashMap.add(!StringHelper.empty(resource.getFileUrl()) ? resource.getFileUrl() : resource.getUrl()))
	    {
		duplicatedUrlCount++;
		continue;
	    }

	    if(!checkAfterLoadFilters(decoratedResource))
	    {
		notSatisfyFiltersCount++;
		continue;
	    }

	    decoratedResource.setTempId(temporaryId);

	    tempIdIndex.put(temporaryId, decoratedResource.getResource());
	    temporaryId++;

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
	params.put("services", getServicesForInterweb(configService));
	params.put("number_of_results", configService.size() > 1 ? configResultsPerService.toString() : configResultsPerOneService.toString());
	params.put("media_types", configMode.getInterwebName());
	params.put("page", Integer.toString(page));
	params.put("timeout", "50");
	if(configDate != null)
	    params.put("date_from", configDate.toString());
	if(configLanguage != null)
	    params.put("language", configLanguage);

	SearchQuery interwebResponse = interweb.search(query, params);
	List<ResourceDecorator> interwebResults = interwebResponse.getResults();
	log.debug("Interweb returned " + interwebResults.size() + " results in " + (System.currentTimeMillis() - start) + " ms");

	if(stopped)
	    return null;

	if(page == 1)
	    resultsCountPerService.putAll(interwebResponse.getResultCountAtService());

	if(interwebResults.size() == 0)
	    hasMoreInterwebResults = false;

	int duplicatedUrlCount = 0; // number of resources that already displayed to the user
	int notSatisfyFiltersCount = 0; // number of resources that not satisfy filters like video duration or image size

	LinkedList<ResourceDecorator> newResources = new LinkedList<ResourceDecorator>();

	for(ResourceDecorator decoratedResource : interwebResults)
	{
	    // check if an other resource with the same url exists
	    if(!urlHashMap.add(decoratedResource.getUrl()))
	    {
		duplicatedUrlCount++;
		continue;
	    }

	    if(!checkAfterLoadFilters(decoratedResource))
	    {
		notSatisfyFiltersCount++;
		continue;
	    }

	    decoratedResource.setTempId(temporaryId);

	    tempIdIndex.put(temporaryId, decoratedResource.getResource());
	    temporaryId++;

	    newResources.add(decoratedResource);
	}

	if(notSatisfyFiltersCount > 0 || duplicatedUrlCount > 0)
	    log.debug("Filtered " + notSatisfyFiltersCount + " resources and skipped " + duplicatedUrlCount + " dublicated resources");

	return newResources;
    }

    /**
     * Select only services which stored in Solr
     * 
     * @param listService
     * @return String
     */
    private String getServicesForLearnweb(List<SERVICE> listService)
    {
	String learnweb = "";
	for(SERVICE service : listService)
	{
	    if(service.isLearnwebSource())
	    {
		learnweb += service.getCustomName() + ",";
	    }
	}

	return StringHelper.removeLastComma(learnweb);
    }

    /**
     * Select only services which available in InterWeb
     * 
     * @param listService
     * @return String
     */
    private String getServicesForInterweb(List<SERVICE> listService)
    {
	String interweb = "";
	for(SERVICE service : listService)
	{
	    if(!service.isLearnwebSource())
	    {
		interweb += service.getCustomName() + ",";
	    }
	}

	return StringHelper.removeLastComma(interweb);
    }

    /**
     * Check filters like image width and video duration
     * 
     * @param res
     * @return boolean
     */
    private boolean checkAfterLoadFilters(ResourceDecorator res)
    {
	//String type = res.getResource().getType(); // text Image Video
	if(configSize != null && res.getResource().getType().equals("Image"))
	{
	    int width = res.getThumbnail4().getWidth(), minWidth = configSize.getMinWidth(), maxWidth = configSize.getMaxWidth();

	    if(minWidth > width || (maxWidth != 0 && width > maxWidth))
	    {
		return false;
	    }
	}

	if(configDuration != null && res.getResource().getType().equals("Video"))
	{
	    int duration = res.getResource().getDuration(), minDuration = configDuration.getMinDuration(), maxDuration = configDuration.getMaxDuration();

	    if(minDuration > duration || (maxDuration != 0 && duration > maxDuration))
	    {
		return false;
	    }
	}

	return true;
    }

    public String getQuery()
    {
	return query;
    }

    /**
     * This function sets the default media and service options for the defined search mode
     * 
     * @param configMode
     */
    public void setMode(MODE searchMode)
    {
	this.configMode = searchMode;

	switch(searchMode)
	{
	case web:
	    setService(SERVICE.getWebServices());
	    break;
	case image:
	    setService(SERVICE.getImageServices());
	    break;
	case video:
	    setService(SERVICE.getVideoServices());
	    break;
	default:
	    throw new IllegalArgumentException("unknown searchmode: " + searchMode);
	}
    }

    public MODE getMode()
    {
	return configMode;
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
		else
		{
		    hasMoreLearnwebResults = false; // disable learnweb
		}

		configService.add(service);
	    }
	}
	else
	{
	    for(SERVICE service : args)
	    {
		configService.add(service);
	    }
	}
    }

    public List<SERVICE> getService()
    {
	return configService;
    }

    public void setFilterByDate(DATE date)
    {
	this.configDate = date;
    }

    public DATE getDate()
    {
	return configDate;
    }

    public void setFilterBySize(SIZE size)
    {
	this.configSize = size;
    }

    public SIZE getSize()
    {
	return configSize;
    }

    public void setFilterByDuration(DURATION duration)
    {
	this.configDuration = duration;
    }

    public DURATION getDuration()
    {
	return configDuration;
    }

    public void setFilterByGroup(String group)
    {
	this.hasMoreInterwebResults = false;
	this.configGroup = group;
    }

    public String getGroup()
    {
	return configGroup;
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
	if(page == 2)
	    getResourcesByPage(1);

	if(page > 50)
	{
	    log.fatal("Requested more than 50 pages", new Exception());
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

    public Map<String, Long> getResultsCountPerService()
    {
	return resultsCountPerService;
    }

    public List<Count> getResultsCountPerGroup()
    {
	return resultsCountPerGroup;
    }

    public void stop()
    {
	this.stopped = true;
    }
}

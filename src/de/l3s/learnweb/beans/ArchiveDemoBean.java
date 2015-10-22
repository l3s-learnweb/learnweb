package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.archivedemo.ArchiveSearchManager;
import de.l3s.archivedemo.Query;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.util.StringHelper;

@ManagedBean
@SessionScoped
public class ArchiveDemoBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -8426331759352561208L;
    private static final Logger log = Logger.getLogger(ArchiveDemoBean.class);
    private static final int MAX_API_ERRORS = 3;

    private String queryString;
    private List<ResourceDecorator> resources = new LinkedList<>();
    private int page = 1;
    private List<ResourceDecorator> resourcesRaw;
    private Map<Integer, LinkedList<ResourceDecorator>> pages = Collections.synchronizedMap(new HashMap<Integer, LinkedList<ResourceDecorator>>());
    private int processedResources = 0;
    private int addedResources = 0;
    private int waybackAPIerrors = 0;
    private SimpleDateFormat waybackDateFormat;
    private String market;
    private List<String> relatedEntities;
    private final String sessionId;
    private int queryId;

    public ArchiveDemoBean() throws SQLException
    {
	HttpSession session = (HttpSession) getFacesContext().getExternalContext().getSession(true);
	sessionId = session.getId();
	waybackDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
	market = UtilBean.getUserBean().getLocaleAsString().replace("_", "-");

	System.out.println("market:" + market);
	if(market.equalsIgnoreCase("de"))
	    market = "de-DE";
	else if(market.equalsIgnoreCase("en"))
	    market = "en-US";

	System.out.println("market:" + market);
    }

    public void preRenderView() throws SQLException, UnsupportedEncodingException
    {
	if(isAjaxRequest())
	{
	    return;
	}
	if(queryString != null)
	    onSearch();

	getFacesContext().getExternalContext().setResponseCharacterEncoding("UTF-8");

	// stop caching (back button problem)
	HttpServletResponse response = (HttpServletResponse) getFacesContext().getExternalContext().getResponse();

	response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
	response.setDateHeader("Expires", 0); // Proxies.
    }

    public String formatDate(String timestamp) throws ParseException
    {
	DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, UtilBean.getUserBean().getLocale());
	DateFormat waybackDf = new SimpleDateFormat("yyyyMMddhhmmss");
	if(timestamp == null)
	    return "01.01.70";

	return df.format(waybackDf.parse(timestamp));
    }

    public String onSearch() throws SQLException
    {
	// reset values
	page = 1;
	pages.clear();
	processedResources = 0;
	addedResources = 0;
	waybackAPIerrors = 0;

	queryString = StringHelper.urlDecode(queryString);
	Query q = getLearnweb().getArchiveSearchManager().getQueryByQueryString(market, queryString);

	if(q == null)
	{
	    log.debug("No query found for: " + queryString + "; market: " + market);
	    addMessage(FacesMessage.SEVERITY_ERROR, "ArchiveSearch.select_suggested_entity");
	    resourcesRaw = null;
	    resources.clear();
	    relatedEntities = null;
	    queryId = -1;
	    return null;
	}

	queryId = q.getId(); // necessary for click log

	resourcesRaw = q.getResults();
	resources = getNextPage();

	if(resourcesRaw.size() == 0)
	    addMessage(FacesMessage.SEVERITY_ERROR, "No archived URLs found");

	ArchiveSearchManager archiveManager = getLearnweb().getArchiveSearchManager();

	long start = System.currentTimeMillis();
	// load related entities
	//TODO
	if(false) // true == simple related entities from database only
	    relatedEntities = archiveManager.getQueryCompletions(market, queryString, 10);
	else
	{
	    try
	    {
		relatedEntities = archiveManager.getNewQuerySuggestions(market, queryString, 10);
		if(relatedEntities.size() == 0)
		    relatedEntities = archiveManager.getQuerySuggestions(market, queryString, 10);
	    }
	    catch(SolrServerException | IOException e)
	    {
		log.error("Can't get related entities", e);
	    }
	}
	log.debug("Loaded related entities in " + (System.currentTimeMillis() - start) + "ms");

	archiveManager.logQuery(queryString, sessionId, market);

	return "/archive/search.xhtml?query=" + StringHelper.urlEncode(queryString) + "&amp;faces-redirect=true";
    }

    public List<String> completeQuery(String query) throws SQLException
    {
	return getLearnweb().getArchiveSearchManager().getQueryCompletions(market, query, 20);
    }

    public synchronized LinkedList<ResourceDecorator> getNextPage() throws NumberFormatException, SQLException
    {
	if(waybackAPIerrors > MAX_API_ERRORS)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "Archive.org API does not respond");
	    return null;
	}
	if(queryId == -1)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "ArchiveSearch.select_suggested_entity");
	    return null;
	}

	ArchiveSearchManager archiveSearchManager = getLearnweb().getArchiveSearchManager();

	//log.debug("getPage " + page);

	LinkedList<ResourceDecorator> resourcePage = pages.get(page);

	if(null == resourcePage && resourcesRaw != null)
	{
	    final int resourcesPerPage = (page < 3) ? 5 : 10; // display at most 5 resources on the first two pages
	    int minCrawlTime = (int) (new Date().getTime() / 1000) - 86400 * 10; // the latest valid crawl date
	    resourcePage = new LinkedList<ResourceDecorator>();

	    for(int requests = 0; processedResources < resourcesRaw.size(); processedResources++)
	    {
		ResourceDecorator resource = resourcesRaw.get(processedResources);

		int crawlTime = 0, captures = 0;

		if(resource.getMetadataValue("url_captures") != null) // the capture dates have been crawled before
		{
		    crawlTime = Integer.parseInt(resource.getMetadataValue("crawl_time"));
		    captures = Integer.parseInt(resource.getMetadataValue("url_captures"));
		}

		//System.out.println("rank: " + resource.getRankAtService() + "; " + captures + "; " + minCrawlTime + "; " + resource.getTitle());

		if(crawlTime < minCrawlTime) //&& waybackAPIerrors < MAX_API_ERRORS)
		{
		    captures = 0;
		    requests++;
		    String url = resource.getUrl().substring(resource.getUrl().indexOf("//") + 2); // remove leading http://

		    Date lastCapture = null, firstCapture = getFirstCaptureDate(url);

		    if(firstCapture != null)
		    {
			lastCapture = getLastCaptureDate(url);

			if(lastCapture != null)
			{
			    resource.getResource().setMetadataValue("first_timestamp", waybackDateFormat.format(firstCapture));
			    resource.getResource().setMetadataValue("last_timestamp", waybackDateFormat.format(lastCapture));
			    captures = 1; // one capture date -> at least one capture
			}
		    }

		    if(waybackAPIerrors == 0)
			archiveSearchManager.cacheCaptureCount(Integer.parseInt(resource.getMetadataValue("query_id")), resource.getRankAtService(), firstCapture, lastCapture, captures);
		}

		if(captures == 0)
		    continue; // no captures found -> don't display this resource

		resource.setTempId(++addedResources);

		resourcePage.add(resource);

		// return the result when to many errors occurred or enough results were found
		if(waybackAPIerrors > MAX_API_ERRORS)
		{
		    addMessage(FacesMessage.SEVERITY_ERROR, "Archive.org API does not respond");
		    break;
		}
		if(resourcesPerPage == resourcePage.size() || resourcePage.size() > 0 && requests >= 2)
		    break;
	    }
	    processedResources++;

	    pages.put(page, resourcePage);
	    resources.addAll(resourcePage);
	}

	return resourcePage;

    }

    public void logClick()
    {
	int rank = getParameterInt("rank");
	int type = getParameterInt("type");

	log.debug(rank + " - " + type);

	getLearnweb().getArchiveSearchManager().logClick(queryId, rank, type, sessionId);

    }

    public void loadNextPage()
    {
	page++;
    }

    public Date getFirstCaptureDate(String url)
    {
	//log.debug("getFirstCaptureDate: " + url);
	return getCaptureDate(url, 1);
    }

    public Date getLastCaptureDate(String url)
    {
	return getCaptureDate(url, -1);
    }

    private Date getCaptureDate(String url, int limit)
    {
	//log.debug("getCaptureDate");
	String response;
	try
	{
	    response = IOUtils.toString(new URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp&limit=" + limit));

	    if(response.trim().length() == 0)
		return null;

	    return waybackDateFormat.parse(response);
	}
	catch(MalformedURLException e)
	{
	    throw new RuntimeException(e);
	}
	catch(ParseException | IOException e)
	{
	    if(e.getMessage().contains("HTTP response code: 403")) // blocked by robots
		return null;

	    log.error("wayback api error", e);
	    waybackAPIerrors++;
	}

	return null;
    }

    public String getMarket()
    {
	return market;
    }

    public String setMarket(String market)
    {
	this.market = market;

	return UtilBean.getUserBean().setLocaleCode(market.substring(0, 2));
    }

    public String getQuery()
    {
	return queryString;
    }

    public void setQuery(String query)
    {
	this.queryString = query;
    }

    public List<ResourceDecorator> getResources()
    {
	return resources;
    }

    public List<String> getRelatedEntities()
    {
	//log.debug("getRelatedEntities");

	return relatedEntities;
    }

}

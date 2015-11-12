package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dom4j.DocumentException;

import de.l3s.archivedemo.ArchiveSearchManager;
import de.l3s.archivedemo.BingAzure;
import de.l3s.archivedemo.CDXClient;
import de.l3s.archivedemo.Query;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@ViewScoped
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
    private String market;
    private final String sessionId;
    private List<String> relatedEntities;
    private boolean relatedEntitiesFromSolr = false; // indicates whether the related entities are loaded from solr

    private transient CDXClient cdxClient = null;

    public ArchiveDemoBean() throws SQLException
    {
	HttpSession session = (HttpSession) getFacesContext().getExternalContext().getSession(true);
	sessionId = session.getId();
	market = UtilBean.getUserBean().getLocaleAsString().replace("_", "-");

	if(market.equalsIgnoreCase("de"))
	    market = "de-DE";
	else if(market.equalsIgnoreCase("en"))
	    market = "en-US";
    }

    public void preRenderView() throws SQLException, DocumentException, IOException
    {
	if(isAjaxRequest())
	{
	    return;
	}

	log.debug("pre render: " + queryString);
	if(queryString != null)
	    search();

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
	return "/archive/search.xhtml?includeViewParams=true&amp;faces-redirect=true"; // query=" + StringHelper.urlEncode(queryString) + "
    }

    private void search() throws SQLException, DocumentException, IOException
    {
	// reset values
	page = 1;
	pages.clear();
	getCDXClient().resetAPICounters();
	processedResources = 0;
	addedResources = 0;
	relatedEntities = null;

	//queryString = StringHelper.urlDecode(queryString);
	Query q = getLearnweb().getArchiveSearchManager().getQueryByQueryString(market, queryString);

	if(q == null)
	{
	    log.debug("No cached query found for: " + queryString + "; market: " + market);

	    q = new Query();
	    q.setQueryString(queryString);
	    q.setRequestedResultCount(50);
	    q.setMarket(market);

	    BingAzure bing = new BingAzure();
	    bing.search(q, "web");

	    if(q.getLoadedResultCount() == 0)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "ArchiveSearch.select_suggested_entity");
		resourcesRaw = null;
		resources.clear();
		return;
	    }
	}

	resourcesRaw = q.getResults();
	resources = getNextPage();

	if(resourcesRaw.size() == 0)
	    addMessage(FacesMessage.SEVERITY_ERROR, "No archived URLs found");

	ArchiveSearchManager archiveManager = getLearnweb().getArchiveSearchManager();

	archiveManager.logQuery(queryString, sessionId, market);
    }

    public List<String> completeQuery(String query) throws SQLException
    {
	return getLearnweb().getArchiveSearchManager().getQueryCompletions(market, query, 20);
    }

    public synchronized LinkedList<ResourceDecorator> getNextPage() throws NumberFormatException, SQLException
    {
	if(getCDXClient().getWaybackAPIerrors() > MAX_API_ERRORS)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "Archive.org API does not respond");
	    return null;
	}
	/*
	if(queryId == -1)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "ArchiveSearch.select_suggested_entity");
	    return null;
	}
	*/

	LinkedList<ResourceDecorator> resourcePage = pages.get(page);

	if(null == resourcePage && resourcesRaw != null)
	{
	    final int resourcesPerPage = (page < 3) ? 5 : 10; // display at most 5 resources on the first two pages
	    resourcePage = new LinkedList<ResourceDecorator>();

	    for(; processedResources < resourcesRaw.size(); processedResources++)
	    {
		ResourceDecorator resource = resourcesRaw.get(processedResources);

		if(!getCDXClient().isArchived(resource))
		    continue; // no captures found -> don't display this resource

		resource.setTempId(++addedResources);

		resourcePage.add(resource);

		// return the result when to many errors occurred or enough results were found
		if(getCDXClient().getWaybackAPIerrors() > MAX_API_ERRORS)
		{
		    addMessage(FacesMessage.SEVERITY_ERROR, "Archive.org API does not respond");
		    break;
		}
		if(resourcesPerPage == resourcePage.size() || resourcePage.size() > 0 && cdxClient.getWaybackAPIrequests() >= 2)
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

	getLearnweb().getArchiveSearchManager().logClick(queryString, getMarket(), rank, type, sessionId);
    }

    public void logRelatedEntityClick()
    {
	String relatedEntity = getParameter("relatedEntity");
	int rank = getParameterInt("rank");
	String method = relatedEntitiesFromSolr ? "solr" : "wiki_link";

	getLearnweb().getArchiveSearchManager().logRelatedEntityClick(queryString, sessionId, getMarket(), relatedEntity, rank, method);
    }

    public void loadNextPage()
    {
	page++;
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

    public void loadRelatedEntities() throws SQLException
    {
	if(relatedEntities == null)
	{
	    long start = System.currentTimeMillis();
	    // load related entities

	    ArchiveSearchManager archiveManager = getLearnweb().getArchiveSearchManager();
	    try
	    {
		relatedEntities = archiveManager.getQuerySuggestionsWikiLink(market, queryString, 10);
		if(relatedEntities.size() == 0)
		{
		    relatedEntities = archiveManager.getQuerySuggestionsSOLR(market, queryString, 10);
		    relatedEntitiesFromSolr = true;
		}
		else
		    relatedEntitiesFromSolr = false;
	    }
	    catch(SolrServerException | IOException e)
	    {
		log.error("Can't get related entities", e);
	    }

	    log.debug("Loaded related entities in " + (System.currentTimeMillis() - start) + "ms");
	}
    }

    public List<String> getRelatedEntities()
    {
	return relatedEntities;
    }

    public boolean isFromSolr()
    {
	return relatedEntitiesFromSolr;
    }

    private CDXClient getCDXClient()
    {
	if(null == cdxClient)
	{
	    Calendar minCrawlTime = Calendar.getInstance();
	    minCrawlTime.add(Calendar.DATE, -30);
	    cdxClient = new CDXClient(minCrawlTime.getTime());
	}
	return cdxClient;
    }

}

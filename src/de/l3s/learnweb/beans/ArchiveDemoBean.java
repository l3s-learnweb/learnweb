package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.servlet.http.HttpServletResponse;

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
    private String queryString;
    private List<ResourceDecorator> resources = new LinkedList<>();
    private int page = 1;
    private List<ResourceDecorator> resourcesRaw;
    private HashMap<Integer, LinkedList<ResourceDecorator>> pages = new HashMap<Integer, LinkedList<ResourceDecorator>>();
    private int processedResources = 0;
    private int addedResources = 0;
    private int waybackAPIerrors = 0;
    private ArchiveSearchManager archiveSearchManager;
    private SimpleDateFormat waybackDateFormat;

    public ArchiveDemoBean() throws SQLException
    {
	archiveSearchManager = getLearnweb().getArchiveSearchManager();
	waybackDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
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

    public String onSearch() throws SQLException, UnsupportedEncodingException
    {
	queryString = URLDecoder.decode(queryString, "UTF-8");
	Query q = getLearnweb().getArchiveSearchManager().getQueryByQueryString(queryString);

	if(q == null)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "ArchiveSearch.select_suggested_entity");
	    resources = null;
	    return null;
	}

	// reset values
	page = 1;
	pages.clear();
	processedResources = 0;
	addedResources = 0;
	waybackAPIerrors = 0;

	resourcesRaw = q.getResults();
	resources = getNextPage();

	if(resourcesRaw.size() == 0)
	    addMessage(FacesMessage.SEVERITY_ERROR, "No archived URLs found");

	return "/archive/search.xhtml?query=" + URLEncoder.encode(queryString, "UTF-8") + "&amp;faces-redirect=true";
    }

    public List<String> completeQuery(String query) throws SQLException
    {
	return getLearnweb().getArchiveSearchManager().getQueryCompletions(query, 20);
    }

    public List<String> suggestQueries() throws SQLException, SolrServerException, IOException
    {
	if(queryString == null || queryString == "")
	    return null;

	//return getLearnweb().getArchiveSearchManager().getQueryCompletions(queryString, 10);

	//TODO
	return getLearnweb().getArchiveSearchManager().getQuerySuggestions(queryString, 10);
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

    public synchronized LinkedList<ResourceDecorator> getNextPage() throws NumberFormatException, SQLException
    {
	log.debug("getNextPage");

	LinkedList<ResourceDecorator> newResources = pages.get(page);

	if(null == newResources && resourcesRaw != null)
	{
	    newResources = new LinkedList<ResourceDecorator>();

	    for(int requests = 0; processedResources < resourcesRaw.size() && requests < 2; processedResources++)
	    {
		ResourceDecorator resource = resourcesRaw.get(processedResources);

		if(resource.getMetadataValue("url_captures") == null)
		{
		    int captures = 0;
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

		    archiveSearchManager.cacheCaptureCount(Integer.parseInt(resource.getMetadataValue("query_id")), resource.getRankAtService(), firstCapture, lastCapture, captures);

		    if(captures == 0)
			continue; // no captures found -> don't display this resource
		}

		resource.setTempId(++addedResources);

		//System.out.println(resource.getTempId() + " - " + resource.getTitle());

		newResources.add(resource);

		if(page == 1 && processedResources == 10)
		    break;
	    }

	    pages.put(page, newResources);
	    resources.addAll(newResources);
	}

	return newResources;

    }

    public void loadNextPage()
    {
	page++;
    }

    public Date getFirstCaptureDate(String url)
    {
	return getCaptureDate(url, 1);
    }

    public Date getLastCaptureDate(String url)
    {
	return getCaptureDate(url, -1);
    }

    private Date getCaptureDate(String url, int limit)
    {
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

	    if(waybackAPIerrors > 10)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "Archive.org API does not respond");
	    }
	}

	return null;
    }
}

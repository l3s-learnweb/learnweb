package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.archivedemo.Query;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@SessionScoped
public class ArchiveDemoBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -8426331759352561208L;
    private static final Logger log = Logger.getLogger(ArchiveDemoBean.class);
    private String queryString;
    private List<ResourceDecorator> resources;

    public ArchiveDemoBean() throws SQLException
    {

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
	DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
	DateFormat waybackDf = new SimpleDateFormat("yyyyMMddhhmmss");
	if(timestamp == null)
	    return "01.01.70";

	return df.format(waybackDf.parse(timestamp));
    }

    public String onSearch() throws SQLException, UnsupportedEncodingException
    {
	log.debug("Query: " + queryString);
	queryString = URLDecoder.decode(queryString, "UTF-8");
	Query q = getLearnweb().getArchiveSearchManager().getQueryByQueryString(queryString);

	if(q == null)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "ArchiveSearch.select_suggested_entity");
	    resources = null;
	    return null;
	}

	resources = q.getResults();
	if(resources.size() == 0)
	    addMessage(FacesMessage.SEVERITY_ERROR, "No archived URLs found");

	return "/archive/search.xhtml?query=" + URLEncoder.encode(queryString, "UTF-8") + "&amp;faces-redirect=true";
    }

    public List<String> completeQuery(String query) throws SQLException
    {
	return getLearnweb().getArchiveSearchManager().getQueryCompletions(query, 20);
    }

    public List<String> suggestQueries() throws SQLException, SolrServerException, IOException
    {
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

}

package de.l3s.learnweb.solrClient;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.User;

public class SolrSearch implements Serializable
{
    private static final long serialVersionUID = 6623209570091677070L;
    final static Logger log = Logger.getLogger(SolrSearch.class);

    private String query;
    private SolrQuery solrQuery;

    private Integer resultsPerPage = 8;
    private String filterLanguage = ""; // for example en_US
    private String filterType = ""; // image, video or web
    private String filterSource = ""; // Bing, Flickr, YouTube, Vimeo, SlideShare, Ipernity, TED, Desktop ...
    private String filterLocation = ""; // Bing, Flickr, YouTube, Vimeo, SlideShare, Ipernity, TED, Learnweb ...
    private String filterFormat = ""; // for example: application/pdf
    private List<Integer> filterGroupIds;

    private String filterGroupStr = "";
    private int userId;

    public SolrSearch(String query, User user)
    {
	this.query = query;
	String newquery = removeMyGroupQuery(query);
	if(!query.equals(newquery))
	{
	    this.query = newquery;
	    try
	    {
		if(user != null && user.getGroups() != null)
		{
		    this.filterGroupIds = new LinkedList<Integer>();
		    for(Group group : user.getGroups())
			this.filterGroupIds.add(group.getId());
		}
	    }
	    catch(SQLException e)
	    {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	this.userId = user == null ? 0 : user.getId();
    }

    public void setResultsPerPage(Integer configResultsPerPage)
    {
	this.resultsPerPage = configResultsPerPage;
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

    /**
     * Either provide a list of groups
     * 
     * @param filterGroups
     */
    public void setFilterGroups(List<Group> filterGroups)
    {
	this.filterGroupIds = new LinkedList<Integer>();

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
	this.filterGroupIds = new LinkedList<Integer>();

	for(Integer id : filterGroupIds) // get ids of the groups
	{
	    this.filterGroupIds.add(id);
	}
    }

    public void clearAllFilters()
    {
	this.filterFormat = "";
	if(null != filterGroupIds)
	    this.filterGroupIds.clear();
	this.filterGroupStr = "";
	this.filterLanguage = "";
	this.filterLocation = "";
	this.filterSource = "";
	this.filterType = "";
    }

    /**
     * 
     * @return How many results the current search will return overall
     * @throws SolrServerException
     * @throws SQLException
     */
    public long getTotalResultCount() throws SQLException, SolrServerException
    {
	QueryResponse rsp = getSolrResourcesByPage(1);
	if(rsp != null)
	    return rsp.getResults().getNumFound();
	else
	    return -1;
    }

    private QueryResponse getSolrResourcesByPage(int page) throws SQLException, SolrServerException
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

	if(null != filterGroupIds)
	{
	    filterGroupStr = "";
	    for(Integer groupId : filterGroupIds)
	    {
		// TODO this filter should also work with groups
		if(0 == filterGroupStr.length())
		    filterGroupStr = "groups : " + groupId.toString();
		else
		{
		    filterGroupStr += " OR groups : " + groupId.toString();
		}
	    }
	    solrQuery.addFilterQuery(filterGroupStr);
	}
	solrQuery.addFilterQuery("-(id:r_* AND -(groups:* OR ownerUserId:" + userId + "))"); // hide private resources
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

	//log.debug(solrQuery);

	//get solrServer
	SolrServer server = Learnweb.getInstance().getSolrClient().getSolrServer();

	//get response
	return server.query(solrQuery);
    }

    /**
     * Returns null of nothing found
     * 
     * @param page
     * @return
     * @throws SolrServerException
     * @throws SQLException
     */
    public List<ResourceDecorator> getResourcesByPage(int page) throws SQLException, SolrServerException
    {
	List<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();

	ResourceManager resourceManager = Learnweb.getInstance().getResourceManager();
	QueryResponse response = getSolrResourcesByPage(page);
	// SolrDocumentList docs = response.getResults(); // to get the score
	List<SolrResourceBean> solrResources = response.getBeans(SolrResourceBean.class);
	Map<String, Map<String, List<String>>> highlight = response.getHighlighting();

	List<String> snippets = new LinkedList<String>();

	int skippedResources = 0;

	for(int i = 0; i < solrResources.size(); i++)
	{
	    //print solr scores for each returned result from solr
	    //System.out.println(docs.get(i).getFieldValue("score"));
	    SolrResourceBean solrResource = solrResources.get(i);

	    Resource resource = null;

	    if(solrResource.getId().startsWith("r_")) // a "real" Learnweb resource
	    {
		int resourceId = extractId(solrResource.getId());
		resource = resourceManager.getResource(resourceId);

		if(null == resource)
		{
		    log.fatal("could not find resource with id:" + solrResource.getId());
		    continue;
		}
	    }
	    else
	    { // cached resources
		log.fatal("Cached resources are dissabled. This should never happen. Solr is in a corrupted state.");
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
				resource.setThumbnail2(new Thumbnail(solrResource.getThumbnailUrl2(), solrResource.getThumnailWidth2(), solrResource.getThumbnailHeight2()));
				resource.setThumbnail3(new Thumbnail(solrResource.getThumbnailUrl3(), solrResource.getThumnailWidth3(), solrResource.getThumbnailHeight3()));
				resource.setThumbnail4(new Thumbnail(solrResource.getThumbnailUrl4(), solrResource.getThumnailWidth4(), solrResource.getThumbnailHeight4()));
		*/
	    }

	    if(resource.getType() == null || resource.getTitle() == null || resource.getUrl() == null)
	    {
		log.error("missing mandatory field url, title or type " + resource);
		continue;
	    }

	    if((resource.getType().equals("Image") || resource.getType().equals("Video")) && resource.getThumbnail2() == null)
	    {
		skippedResources++;
		continue;
	    }

	    ResourceDecorator decoratedResoure = new ResourceDecorator(resource);
	    resources.add(decoratedResoure);

	    Map<String, List<String>> resourceSnippets = highlight.get(solrResource.getId());
	    snippets.clear();
	    StringBuilder snippet = new StringBuilder();

	    if(null != resourceSnippets.get("title"))
		decoratedResoure.setTitle(resourceSnippets.get("title").get(0));

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

	    String oneLineSnippets = snippet.toString().replaceAll("\n", " ");
	    Pattern pattern = Pattern.compile("[^\"\'a-zA-Z]+");
	    Matcher matcher = pattern.matcher(oneLineSnippets);
	    if(matcher.lookingAt())
	    {
		oneLineSnippets = oneLineSnippets.substring(matcher.end());
	    }

	    if(oneLineSnippets.length() != 0)
		decoratedResoure.setSnippet(oneLineSnippets);
	}

	if(skippedResources > 0)
	    log.error(skippedResources + " video/image resource had no thumbnail and were skipped");

	return resources;
    }

    private int extractId(String id)
    {
	try
	{
	    return Integer.parseInt(id.substring(2));
	}
	catch(NumberFormatException e)
	{
	    System.err.println("SolrSearch, NumberFormatException: " + e.getMessage());
	    return -1;
	}
    }

    private String removeMyGroupQuery(String query)
    {
	String newquery = "";
	Pattern pattern = Pattern.compile("groups\\s*:\\s*my\\s+");
	Matcher matcher = pattern.matcher(query.toLowerCase());
	if(matcher.find())
	{
	    int start = matcher.start();
	    int end = matcher.end();
	    if(start != 0)
		newquery = query.substring(0, start);
	    newquery = newquery.concat(query.substring(end, query.length()));
	    return newquery;
	}
	else
	    return query;
    }

    public static void main(String[] args) throws SQLException, SolrServerException, IOException
    {
	Learnweb.getInstance().getSolrClient().deleteOldCachedResource();
	/*
	User user = new User();
	user.setId(2376);
	System.out.println(user.getGroups());
	SolrSearch search = new SolrSearch("groups:my bike", user);
	//search.setFilterType("web");
	//search.setFilterGroups(128, 151);
	// location, source and language are not set. this means they do not matter for this search

	// get the first 8 (resultsPerPage) search results 
	List<ResourceDecorator> page1 = search.getResourcesByPage(1);

	// get the next 8 search results 
	//List<ResourceDecorator> page2 = search.getResourcesByPage(2);

	// this is only an example it. This search will not necessarily return results
	*/
    }
}

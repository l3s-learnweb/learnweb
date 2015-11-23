package de.l3s.learnweb.solrClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.Tag;

public class SolrClient
{
    private final static Logger log = Logger.getLogger(SolrClient.class);

    private static SolrClient instance = null;

    private final String serverUrl;
    private final SolrServer server;

    private SolrClient(Learnweb learnweb)
    {
	instance = this;
	serverUrl = learnweb.getProperties().getProperty("SOLR_SERVER_URL"); // see /Learnweb/Resources/de/l3s/learnweb/config/learnweb.properties
	server = new HttpSolrServer(serverUrl);
	//server = new HttpSolrServer("http://prometheus.kbs.uni-hannover.de:8983/solr");
    }

    public static SolrClient getInstance(Learnweb learnweb)
    {
	if(null == instance)
	    return new SolrClient(learnweb);

	return instance;
    }

    public SolrServer getSolrServer()
    {
	return server;
    }

    /**
     * Add a new resource to the Solr index.
     * 
     * @param resource
     * @throws SQLException
     * @throws SolrServerException
     * @throws IOException
     */
    public void indexResource(Resource resource) throws SQLException, IOException, SolrServerException
    {

	if(resource.getUrl().indexOf("localhost") != -1)
	{
	    log.warn("Skip local resource with ID : " + resource.getId());
	    return;
	}
	SolrResourceBean solrResource = new SolrResourceBean(resource);

	log.debug("index resource: " + resource.getId() + " " + solrResource.getDescription());

	server.addBean(solrResource);
	server.commit();
    }

    public void indexDecoratedResource(ResourceDecorator decoratedResource) throws SQLException, SolrServerException, IOException
    {
	server.addBean(new SolrResourceBean(decoratedResource));
	server.commit();
    }

    public void indexDecoratedResources(List<ResourceDecorator> decoratedResources)
    {
	try
	{
	    List<SolrResourceBean> solrResources = new LinkedList<SolrResourceBean>();
	    for(ResourceDecorator decoratedResource : decoratedResources)
	    {
		Resource resource = decoratedResource.getResource();

		if((resource.getType().equals("Image") || resource.getType().equals("Video")) && resource.getThumbnail2() == null)
		{
		    log.error("will not index a video/image resource without thumbnail: " + resource.toString());
		}

		solrResources.add(new SolrResourceBean(decoratedResource));
	    }
	    server.addBeans(solrResources);
	    server.commit();

	    log.debug("Indexed " + decoratedResources.size() + " resources for caching");
	}
	catch(Throwable t)
	{
	    log.fatal("error during indexing cache resource", t);
	}

    }

    /**
     * This function will be called after the resource meta data changed
     * 
     * @param resource
     * @throws SolrServerException
     * @throws IOException
     * @throws SQLException
     */
    public void reIndexResource(Resource resource)
    {
	try
	{
	    indexResource(resource);
	}
	catch(Throwable t)
	{
	    log.fatal("Couldn't reindex resource", t);
	}
    }

    public void deleteFromIndex(int resourceId) throws SolrServerException, IOException
    {
	server.deleteById("r_" + resourceId);
	server.commit();
    }

    /**
     * This function delete all learnweb resources(with id starts with r_ ) from solr
     * 
     * @throws SolrServerException
     * @throws IOException
     */
    public void deleteAllFromIndex() throws SolrServerException, IOException
    {
	server.deleteByQuery("id:r_*");
	server.commit();
    }

    public void deleteOldCachedResource() throws SolrServerException, IOException
    {
	//delete cached resources which are indexed before the start of yesterday

	/* " * TO NOW-1DAY/DAY " means time before the start of yesterday
	 * " * TO NOW-1MONTH/DAY " means time before the start of one month ago
	 * /HOUR : Round to the start of the current hour
	 * /DAY : Round to the start of the current day
	 * -1DAY : Exactly 1 day prior to now
	 * -1MONTH : Exactly 1 month prior to now
	 * +2YEARS : Exactly two years in the future from now
	 */

	//server.deleteByQuery("timestamp : [ * TO NOW-1DAY/DAY] AND -id:r_*");
	server.deleteByQuery("-id:r_*");
	server.commit();
    }

    /**
     * This method will be called when a comment was added to an existing (and already indexed) resource
     * This function should be called after the comment has been added to the resource
     * 
     * @param comment
     * @throws SQLException
     * @throws Exception
     */
    public void indexComment(Comment comment) throws SQLException
    {
	Resource resource = comment.getResource(); // the resource to which the comment was added
	reIndexResource(resource);
    }

    /**
     * This method will be called when a tag was added to an existing (and already indexed) resource
     * This function should be called after the tag has been added to the resource
     * 
     * @param tag
     * @param resource The resource to which the tag was added
     * @throws SQLException
     * @throws SolrServerException
     * @throws IOException
     */
    public void indexTag(Tag tag, Resource resource)
    {
	reIndexResource(resource);
    }

    /**
     * This function should be called after the tag has been deleted from the resource
     * 
     * @param tag
     * @param resource
     * @throws Exception
     */
    public void deleteFromIndex(Tag tag, Resource resource) throws Exception
    {
	reIndexResource(resource);
    }

    /**
     * This function should be called after the comment has been deleted from the resource
     * 
     * @param comment
     * @throws Exception
     */
    public void deleteFromIndex(Comment comment) throws Exception
    {
	Resource resource = comment.getResource();

	reIndexResource(resource);
    }

    public List<Integer> findResourcesByUrl(String url) throws SolrServerException
    {
	List<Integer> ids = new LinkedList<Integer>();
	SolrQuery solrQuery = new SolrQuery();
	solrQuery.setQuery("url:\"" + url + "\"");
	solrQuery.addFilterQuery("id:r_*");
	solrQuery.setStart(0);
	solrQuery.setRows(Integer.MAX_VALUE);
	solrQuery.setFields("id");
	QueryResponse result = server.query(solrQuery);
	if(null != result)
	{
	    for(SolrDocument doc : result.getResults())
		ids.add(extractId((String) doc.getFieldValue("id")));
	    return ids;
	}
	else
	    return null;
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

    public static void main(String[] args) throws SQLException, IOException, SolrServerException
    {

	//indexOneResource(67069);
	//indexOneResource(72364);
	//deleteOneResource(72364);
	//deleteOneResource(67069);

	//deleteAllResource();
	//indexAllResources();

	/*
	Learnweb learnweb = Learnweb.getInstance();
	SolrClient indexer = new SolrClient(learnweb);
	
	indexer.server.deleteByQuery("*:*");
	indexer.server.commit();*
	*/
	SolrClient.indexAllResources();
	//SolrClient.deleteInvalidEntries();

    }

    public static void deleteInvalidEntries() throws SQLException, SolrServerException, IOException
    {
	Learnweb learnweb = Learnweb.getInstance();
	SolrClient indexer = learnweb.getSolrClient();

	List<Integer> invalidIds = learnweb.getResourceManager().getInvalidResourceIds();

	for(int id : invalidIds)
	{
	    System.out.println("delete: " + id);
	    indexer.deleteFromIndex(id);
	}
    }

    /**
     * Index all resources
     * 
     * @param args
     * @throws SQLException
     * @throws SolrServerException
     * @throws IOException
     */
    public static void indexAllResources() throws SQLException, IOException, SolrServerException
    {
	Learnweb learnweb = Learnweb.getInstance();
	SolrClient indexer = learnweb.getSolrClient();

	for(int i = 0; i < 1; i++)
	{

	    //List<Resource> resources = learnweb.getResourceManager().getResourcesAll(i, 1000); // loads all resources (very slow)

	    List<Resource> resources = learnweb.getGroupManager().getGroupById(118).getResources();

	    log.debug("page: " + i);

	    for(Resource resource : resources)
	    {

		/*
		File file = resource.getFile(4);
		
		if(file != null && file.getUrl().startsWith("http://learnweb.l3s.uni-hannover.de")) // resource has an attached file
		{
		
		FileInspector inspector = new FileInspector();
		FileInfo info = inspector.inspect((new URL(file.getUrl())).openStream(), file.getName());
		
		System.out.println(info);
		
		if(info.getTextContent() != null)
		{
		resource.setMachineDescription(info.getTextContent());
		resource.save();
		
		System.out.println("saved description ");
		}
		
		}*/
		log.debug("Process resource: " + resource.getId());

		indexer.reIndexResource(resource);
	    }
	}

    }

}

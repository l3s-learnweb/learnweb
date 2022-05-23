package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.Tag;

public class SolrClient
{
    private final static Logger log = Logger.getLogger(SolrClient.class);

    private static SolrClient instance = null;

    private final String serverUrl;
    private final Http2SolrClient server;

    private Learnweb learnweb;

    private SolrClient(Learnweb learnweb)
    {
        instance = this;
        this.serverUrl = learnweb.getProperties().getProperty("SOLR_SERVER_URL");
        this.server = new Http2SolrClient.Builder(serverUrl).build();
        this.learnweb = learnweb;
    }

    public static SolrClient getInstance(Learnweb learnweb)
    {
        if(null == instance)
            return new SolrClient(learnweb);

        return instance;
    }

    public Http2SolrClient getSolrServer()
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
        SolrResourceBean solrResource = new SolrResourceBean(resource);

        log.debug("index resource: " + resource.getId());

        server.addBean(solrResource);
        server.commit();
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
            if(resource.isDeleted())
                deleteFromIndex(resource.getId());
            else
                indexResource(resource);
        }
        catch(Throwable t)
        {
            log.fatal("Couldn't reindex resource " + resource.toString(), t);
        }
    }

    public void deleteFromIndex(int resourceId) throws SolrServerException, IOException
    {
        server.deleteById("r_" + resourceId);
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
    public void deleteFromIndex(Tag tag, Resource resource)
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

    public List<Integer> findResourcesByUrl(String url) throws SolrServerException, IOException
    {
        List<Integer> ids = new LinkedList<>();
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

    public long countResources(String query) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields("id");
        QueryResponse result = server.query(solrQuery);
        if(null != result)
        {
            return result.getResults().getNumFound();
        }
        else
            return -1;
    }

    private int extractId(String id)
    {
        try
        {
            return Integer.parseInt(id.substring(2));
        }
        catch(NumberFormatException e)
        {
            log.error("SolrSearch, NumberFormatException: " + e.getMessage());
            return -1;
        }
    }

    public List<String> getAutoCompletion(String field, String query) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.setFacet(true);
        solrQuery.addFacetField(field);
        solrQuery.setFacetPrefix(query);
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(10);
        solrQuery.setRows(0);

        //get response
        QueryResponse response = getSolrServer().query(solrQuery);
        FacetField facetFieldsResult = response.getFacetFields().get(0);

        List<String> suggestions = new ArrayList<>(10);

        for(Count entry : facetFieldsResult.getValues())
        {
            suggestions.add(entry.getName());
        }

        return suggestions;
    }

    /**
     * Drop index
     *
     * @throws SQLException
     * @throws SolrServerException
     * @throws IOException
     */
    public void deleteAllResource() throws SQLException, IOException, SolrServerException
    {
        server.deleteByQuery("*:*");
        server.commit();
    }

    /**
     * Index all resources
     *
     * @throws SQLException
     * @throws SolrServerException
     * @throws IOException
     */
    public void indexAllResources(Consumer<Integer> progressCallback) throws SQLException, IOException, SolrServerException
    {
        final int batchSize = 100;
        int indexedResources = 0;
        ResourceManager resourceManager = learnweb.getResourceManager();
        resourceManager.setReindexMode(true);

        final int totalResources = resourceManager.getResourceCount();
        Collection<SolrResourceBean> solrResourceBeans = new ArrayList<>(batchSize);

        for(int i = 0;; i++)
        {
            log.debug("Load page: " + i);
            List<Resource> resources = resourceManager.getResourcesAll(i, batchSize);

            if(resources.size() == 0)
            {
                log.debug("finished: last page");
                break;
            }

            log.debug("Process page: " + i);

            for(Resource resource : resources)
            {
                solrResourceBeans.add(new SolrResourceBean(resource));
            }

            log.debug("Storing page: " + i);
            UpdateResponse response = server.addBeans(solrResourceBeans);
            if(response.getStatus() != 0)
                throw new SolrServerException("invalid response code: " + response.getStatus() + "; desc: " + response);

            server.commit();
            indexedResources += solrResourceBeans.size();
            solrResourceBeans.clear();

            int progress = (indexedResources * 100) / totalResources;
            progressCallback.accept(progress);
            log.debug("Processed: " + progress + "%");
        }
    }

}

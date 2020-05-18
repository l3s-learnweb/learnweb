package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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

public final class SolrClient {
    private static final Logger log = LogManager.getLogger(SolrClient.class);

    private static SolrClient instance;

    private final String serverUrl;
    private final HttpSolrClient server;

    private final Learnweb learnweb;

    private SolrClient(Learnweb learnweb) {
        this.serverUrl = learnweb.getProperties().getProperty("SOLR_SERVER_URL");
        this.server = new HttpSolrClient.Builder(serverUrl).build();
        this.learnweb = learnweb;
    }

    public HttpSolrClient getSolrServer() {
        return server;
    }

    /**
     * Add a new resource to the Solr index.
     */
    public void indexResource(Resource resource) throws SQLException, IOException, SolrServerException {
        log.debug("index resource: " + resource.getId());

        server.addBean(new ResourceDocument(resource));
        server.commit();
    }

    /**
     * This function will be called after the resource meta data changed.
     */
    public void reIndexResource(Resource resource) {
        try {
            if (resource.isDeleted()) {
                deleteFromIndex(resource.getId());
            } else {
                indexResource(resource);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Couldn't reindex resource " + resource.getId(), t);
            //log.fatal("Couldn't reindex resource " + resource.toString(), t);
        }
    }

    /**
     * This method will be called when a comment was added to an existing (and already indexed) resource.
     * This function should be called after the comment has been added to the resource.
     */
    public void indexComment(Comment comment) throws SQLException {
        Resource resource = comment.getResource(); // the resource to which the comment was added
        reIndexResource(resource);
    }

    /**
     * This method will be called when a tag was added to an existing (and already indexed) resource.
     * This function should be called after the tag has been added to the resource.
     *
     * @param resource The resource to which the tag was added
     */
    public void indexTag(Tag tag, Resource resource) {
        reIndexResource(resource);
    }

    public void deleteFromIndex(int resourceId) throws SolrServerException, IOException {
        server.deleteById("r_" + resourceId);
        server.commit();
    }

    /**
     * This function should be called after the tag has been deleted from the resource.
     */
    public void deleteFromIndex(Tag tag, Resource resource) {
        reIndexResource(resource);
    }

    /**
     * This function should be called after the comment has been deleted from the resource.
     */
    public void deleteFromIndex(Comment comment) throws Exception {
        Resource resource = comment.getResource();

        reIndexResource(resource);
    }

    public List<Integer> findResourcesByUrl(String url) throws SolrServerException, IOException {
        List<Integer> ids = new LinkedList<>();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("url:\"" + url + "\"");
        solrQuery.addFilterQuery("id:r_*");
        solrQuery.setStart(0);
        solrQuery.setRows(Integer.MAX_VALUE);
        solrQuery.setFields("id");
        QueryResponse result = server.query(solrQuery);
        if (null != result) {
            for (SolrDocument doc : result.getResults()) {
                ids.add(extractId((String) doc.getFieldValue("id")));
            }
            return ids;
        } else {
            return null;
        }
    }

    public long countResources(String query) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields("id");
        QueryResponse result = server.query(solrQuery);
        if (null != result) {
            return result.getResults().getNumFound();
        } else {
            return -1;
        }
    }

    private int extractId(String id) {
        try {
            return Integer.parseInt(id.substring(2));
        } catch (NumberFormatException e) {
            log.error("SolrSearch, NumberFormatException: " + e.getMessage());
            return -1;
        }
    }

    public List<String> getAutoCompletion(String field, String query) throws SolrServerException, IOException {
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

        for (Count entry : facetFieldsResult.getValues()) {
            suggestions.add(entry.getName());
        }

        return suggestions;
    }

    /**
     * Drop index.
     */
    public void deleteAllResource() throws SQLException, IOException, SolrServerException {
        server.deleteByQuery("*:*");
        server.commit();
    }

    /**
     * Index all resources.
     */
    public void indexAllResources() throws SQLException, IOException, SolrServerException {
        final int batchSize = 1000;
        ResourceManager resourceManager = learnweb.getResourceManager();
        resourceManager.setReindexMode(true);

        Collection<ResourceDocument> resourceDocuments = new ArrayList<>(batchSize);
        //long sendResources = 0;

        for (int i = 0; true; i++) {
            log.debug("Load page: " + i);
            List<Resource> resources = resourceManager.getResourcesAll(i, batchSize);

            if (resources.isEmpty()) {
                log.debug("finished: last page");
                break;
            }

            log.debug("Process page: " + i);

            resourceDocuments.clear();

            for (Resource resource : resources) {
                resourceDocuments.add(new ResourceDocument(resource));
                //sendResources++;
            }

            UpdateResponse response = server.addBeans(resourceDocuments);
            if (response.getStatus() != 0) {
                throw new RuntimeException("invalid response code: " + response.getStatus() + "; desc: " + response);
            }

            server.commit();

            /*
            long indexedResources = countResources("*:*");

            if(sendResources != indexedResources)
                throw new RuntimeException(sendResources + " - " + indexedResources);
            */
        }
    }

    public static SolrClient getInstance(Learnweb learnweb) {
        if (null == instance) {
            instance = new SolrClient(learnweb);
        }

        return instance;
    }

}

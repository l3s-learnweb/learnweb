package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import de.l3s.learnweb.resource.Resource;

public final class SolrClient {
    private static final Logger log = LogManager.getLogger(SolrClient.class);

    private final HttpSolrClient httpSolrClient;
    private final boolean disabled;

    public SolrClient(String solrServerUrl) {
        this.disabled = StringUtils.isEmpty(solrServerUrl);
        this.httpSolrClient = this.disabled ? null : new HttpSolrClient.Builder(solrServerUrl).build();

        if (disabled) {
            log.warn("SolrClient is DISABLED!");
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public HttpSolrClient getHttpSolrClient() {
        return httpSolrClient;
    }

    /**
     * Add a new resource to the Solr index.
     */
    public void indexResource(Resource resource) throws IOException, SolrServerException {
        log.debug("index resource: {}", resource.getId());

        if (!disabled) {
            httpSolrClient.addBean(new ResourceDocument(resource));
            httpSolrClient.commit();
        }
    }

    /**
     * Remove a resource from the Solr index.
     */
    public void deleteResource(Resource resource) throws SolrServerException, IOException {
        log.debug("delete resource: {}", resource.getId());

        if (!disabled) {
            httpSolrClient.deleteById("r_" + resource.getId());
            httpSolrClient.commit();
        }
    }

    /**
     * This function will be called after the resource meta data changed.
     */
    public void reIndexResource(Resource resource) {
        try {
            if (resource.isDeleted()) {
                deleteResource(resource);
            } else {
                indexResource(resource);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Couldn't reindex resource " + resource.getId(), t);
            //log.fatal("Couldn't reindex resource " + resource.toString(), t);
        }
    }

    public List<Integer> findResourcesByUrl(String url) throws SolrServerException, IOException {
        List<Integer> ids = new LinkedList<>();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("url:\"" + url + "\"");
        solrQuery.addFilterQuery("id:r_*");
        solrQuery.setStart(0);
        solrQuery.setRows(Integer.MAX_VALUE);
        solrQuery.setFields("id");
        QueryResponse result = httpSolrClient.query(solrQuery);
        if (null != result) {
            for (SolrDocument doc : result.getResults()) {
                ids.add(extractId((String) doc.getFieldValue("id")));
            }
            return ids;
        } else {
            return null;
        }
    }

    public long countResources(String query) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.addFilterQuery("id:r_*");
        solrQuery.setFields("id");

        try {
            QueryResponse result = httpSolrClient.query(solrQuery);
            return result.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            return -1;
        }
    }

    static int extractId(String id) {
        try {
            return Integer.parseInt(id.substring(2));
        } catch (NumberFormatException e) {
            log.error("SolrSearch, NumberFormatException: {}", e.getMessage());
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
        QueryResponse response = httpSolrClient.query(solrQuery);
        FacetField facetFieldsResult = response.getFacetFields().get(0);

        List<String> suggestions = new ArrayList<>(10);

        for (Count entry : facetFieldsResult.getValues()) {
            suggestions.add(entry.getName());
        }

        return suggestions;
    }
}

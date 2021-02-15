package de.l3s.maintenance.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.search.solrClient.ResourceDocument;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.maintenance.MaintenanceTask;

public class ReindexResources extends MaintenanceTask {

    private ResourceDao resourceDao;
    private SolrClient solrClient;

    @Override
    protected void init() {
        resourceDao = getLearnweb().getDaoProvider().getResourceDao();
        solrClient = getLearnweb().getSolrClient();
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        // Reindex resources of single user
        //final List<Resource> resources = learnweb.getResourceManager().getResourcesByUserId(9289);

        // Reindex a single resource
        //solr.reIndexResource(learnweb.getResourceManager().getResource(219673));

        // Reindex resources of a group
        // groupDao.findById(1401).get().getResources().forEach(solr::reIndexResource);

        /* Reindex all resources */
        deleteAllResource();
        indexAllResources();

        log.debug("All tasks completed.");
    }

    /**
     * Drop index.
     */
    public void deleteAllResource() throws IOException, SolrServerException {
        solrClient.getHttpSolrClient().deleteByQuery("*:*");
        solrClient.getHttpSolrClient().commit();
    }

    /**
     * Index all resources.
     */
    public void indexAllResources() throws IOException, SolrServerException {
        final int batchSize = 1000;

        Collection<ResourceDocument> resourceDocuments = new ArrayList<>(batchSize);
        //long sendResources = 0;

        for (int i = 0; true; i++) {
            log.debug("Load page: {}", i);
            List<Resource> resources = resourceDao.findAll(i, i * batchSize);

            if (resources.isEmpty()) {
                log.debug("finished: last page");
                break;
            }

            log.debug("Process page: {}", i);

            resourceDocuments.clear();

            for (Resource resource : resources) {
                resourceDocuments.add(new ResourceDocument(resource));
                //sendResources++;
            }

            UpdateResponse response = solrClient.getHttpSolrClient().addBeans(resourceDocuments);
            if (response.getStatus() != 0) {
                throw new IllegalStateException("invalid response code: " + response.getStatus() + "; desc: " + response);
            }

            solrClient.getHttpSolrClient().commit();

            /*
            long indexedResources = countResources("*:*");

            if(sendResources != indexedResources)
                throw new RuntimeException(sendResources + " - " + indexedResources);
            */
        }
    }

    public static void main(String[] args) {
        new ReindexResources().start(args);
    }
}

package de.l3s.maintenance.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.search.solrClient.ResourceDocument;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.maintenance.MaintenanceTask;

public class ReindexResources extends MaintenanceTask {

    private final ResourceDao resourceDao;
    private final SolrClient solrClient;
    private final Consumer<Integer> progressCallback;

    private ReindexResources() {
        progressCallback = integer -> {};
        resourceDao = getLearnweb().getDaoProvider().getResourceDao();
        solrClient = getLearnweb().getSolrClient();
    }

    public ReindexResources(Learnweb learnweb, final Consumer<Integer> callback) {
        super(learnweb);

        progressCallback = callback;
        resourceDao = getLearnweb().getDaoProvider().getResourceDao();
        solrClient = getLearnweb().getSolrClient();
    }

    @Override
    public void run(final boolean dryRun) throws IOException, SolrServerException {
        // Reindex resources of single user
        //final List<Resource> resources = learnweb.getResourceManager().getResourcesByUserId(9289);

        // Reindex a single resource
        //solr.reIndexResource(learnweb.getResourceManager().getResource(219673));

        // Reindex resources of a group
        // groupDao.findById(1401).get().getResources().forEach(solr::reIndexResource);

        /* Reindex all resources */
        deleteAllResource();
        progressCallback.accept(0);
        indexAllResources();
        progressCallback.accept(100);

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
        final int totalResources = resourceDao.countUndeleted();
        int indexedResources = 0;

        final int batchSize = Math.min(1000, totalResources);

        List<ResourceDocument> resourceDocuments = new ArrayList<>(batchSize);

        for (int i = 0; true; i++) {
            log.debug("Load page: {}", i);
            List<Resource> resources = resourceDao.findAll(batchSize, i * batchSize);

            if (resources.isEmpty()) {
                log.debug("finished: last page");
                break;
            }

            log.debug("Process page: {}", i);

            for (Resource resource : resources) {
                resourceDocuments.add(new ResourceDocument(resource));
            }

            UpdateResponse response = solrClient.getHttpSolrClient().addBeans(resourceDocuments);
            if (response.getStatus() != 0) {
                throw new IllegalStateException("invalid response code: " + response.getStatus() + "; desc: " + response);
            }

            solrClient.getHttpSolrClient().commit();
            indexedResources += resourceDocuments.size();
            progressCallback.accept((indexedResources * 100) / totalResources);

            resourceDocuments.clear();
        }
    }

    public static void main(String[] args) {
        new ReindexResources().start(args);
    }
}

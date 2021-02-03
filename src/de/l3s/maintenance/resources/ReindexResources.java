package de.l3s.maintenance.resources;

import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.maintenance.MaintenanceTask;

public class ReindexResources extends MaintenanceTask {

    @Override
    protected void run(final boolean dryRun) throws Exception {
        SolrClient solr = getLearnweb().getSolrClient();
        getLearnweb().getResourceManager().setReindexMode(true);

        // Reindex resources of single user
        //final List<Resource> resources = learnweb.getResourceManager().getResourcesByUserId(9289);

        // Reindex a single resource
        //solr.reIndexResource(learnweb.getResourceManager().getResource(219673));

        // Reindex resources of a group
        // learnweb.getGroupManager().getGroupById(1401).getResources().forEach(solr::reIndexResource);

        /* Reindex all resources */
        solr.deleteAllResource();
        solr.indexAllResources();

        log.debug("All tasks completed.");
    }

    public static void main(String[] args) {
        new ReindexResources().start(args);
    }
}

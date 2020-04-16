package de.l3s.learnweb.tasks.resources;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;

public class ReindexAllResources
{
    private static final Logger log = LogManager.getLogger(ReindexAllResources.class);

    public static void main(String[] args) throws SQLException, IOException, SolrServerException, ClassNotFoundException
    {
        Learnweb learnweb = Learnweb.createInstance();
        SolrClient solr = learnweb.getSolrClient();

        learnweb.getResourceManager().setReindexMode(true);
        // Reindex resources of single user
        //final List<Resource> resources = learnweb.getResourceManager().getResourcesByUserId(9289);

        // Reindex a single resource
        //solr.reIndexResource(learnweb.getResourceManager().getResource(219673));

        // Reindex resources of a group
        learnweb.getGroupManager().getGroupById(1401).getResources().forEach(solr::reIndexResource);

        /* Reindex all resources */
        // solr.deleteAllResource();
        //solr.indexAllResources();

        learnweb.onDestroy();

        log.debug("All tasks completed.");
        System.exit(0);
    }
}

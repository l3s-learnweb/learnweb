package de.l3s.learnweb.tasks.resources;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;

public class ReindexAllResources
{
    private static final Logger log = Logger.getLogger(ReindexAllResources.class);

    public static void main(String[] args) throws SQLException, IOException, SolrServerException, ClassNotFoundException
    {
        Learnweb learnweb = Learnweb.createInstance();
        SolrClient solr = learnweb.getSolrClient();

        /* Reindex resources of single user */
        // final List<Resource> resources = learnweb.getResourceManager().getResourcesByUserId(9289);
        // log.debug("Found " + resources.size() + " resources.");
        // resources.forEach(solr::reIndexResource);

        /* Reindex all resources */
        // solr.deleteAllResource();
        solr.indexAllResources();

        learnweb.onDestroy();

        log.debug("All tasks completed.");
        System.exit(0);
    }
}

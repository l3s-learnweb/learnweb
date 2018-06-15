package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;

public class ReindexAllResources
{
    private final static Logger log = Logger.getLogger(ReindexAllResources.class);

    public static void main(String[] args) throws SQLException, IOException, SolrServerException, ClassNotFoundException
    {

        Learnweb learnweb = Learnweb.createInstance(null);
        SolrClient solr = learnweb.getSolrClient();

        //solr.deleteAllResource();
        solr.indexAllResources();

        learnweb.onDestroy();

        log.debug("All tasks completed.");
    }
}

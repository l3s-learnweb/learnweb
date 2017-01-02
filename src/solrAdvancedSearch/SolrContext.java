package solrAdvancedSearch;

import java.io.InputStream;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

public class SolrContext
{
    private final static String URL = "";
    //private final static CommonsHttpSolrServer server = null;
    private static HttpSolrServer server = null;
    //ContentHandler textHandler = new BodyContentHandler();
    //Metadata metadata = new Metadata();
    //ParseContext context = new ParseContext();
    InputStream input = null;
    static
    {
        try
        {
            server = new HttpSolrServer(URL);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static SolrServer getServer()
    {
        return server;
    }

}

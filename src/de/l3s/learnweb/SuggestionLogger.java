package de.l3s.learnweb;

import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.l3s.util.StringHelper;

public class SuggestionLogger
{
    private static final Logger log = Logger.getLogger(SuggestionLogger.class);

    private final static Container LAST_ENTRY = new Container("", "", "", "", null); // this element indicates that the consumer thread should stop
    private final Learnweb learnweb;
    private final LinkedBlockingQueue<Container> queue;
    private final Thread consumerThread;

    public SuggestionLogger(Learnweb learnweb)
    {
        super();
        this.learnweb = learnweb;
        this.queue = new LinkedBlockingQueue<Container>();
        this.consumerThread = new Thread(new Consumer());
        this.consumerThread.start();
    }

    public void log(String query, String market, String suggestionBing, String sessionId, User user)
    {
        try
        {
            queue.put(new Container(query, market, suggestionBing, sessionId, user));
        }
        catch(InterruptedException e)
        {
            log.fatal("Couldn't log suggestion", e);
        }
    }

    public void stop()
    {
        try
        {
            queue.put(LAST_ENTRY);
            consumerThread.join();
        }
        catch(Exception e)
        {
            log.fatal("Couldn't stop suggestion logger", e);
        }
    }

    private class Consumer implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    Container container = queue.take();

                    if(container == LAST_ENTRY) // stop method was called
                        break;

                    String suggestionsGoogle = null;
                    try
                    {
                        suggestionsGoogle = googleSuggestion(container.market, container.query);
                    }
                    catch(Exception e)
                    {
                        log.fatal("Couldn't get google suggestion for: " + container.query, e);
                    }

                    try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `lw_log_suggestions` (`query`, `market`, `timestamp`, `suggestions_bing`, `suggestions_google`, session_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)");)
                    {
                        insert.setString(1, container.query);
                        insert.setString(2, container.market);
                        insert.setTimestamp(3, new Timestamp(container.timestamp));
                        insert.setString(4, container.suggestionBing);
                        insert.setString(5, suggestionsGoogle);
                        insert.setString(6, container.sessionId);
                        insert.setInt(7, container.userId);
                        insert.executeUpdate();
                    }
                    catch(SQLException e)
                    {
                        log.fatal("Couldn't log suggestion: " + container, e);
                    }
                }

                log.debug("Suggestion logger was stopped");
            }
            catch(InterruptedException e)
            {
                log.fatal("Suggestion logger crashed", e);
            }
        }

        private String googleSuggestion(String market, String query)
        {
            String suggestion = "";
            if(market.length() > 2)
            {
                market = market.substring(0, 2);
            }
            String suggestorUrl = "http://suggestqueries.google.com/complete/search?output=toolbar&hl=" + market + "&q=" + StringHelper.urlEncode(query);
            try
            {
                Client client = Client.create();
                WebResource webResource = client.resource(suggestorUrl);
                ClientResponse response = webResource.get(ClientResponse.class);
                String xml = response.getEntity(String.class);

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = null;
                try
                {
                    db = dbf.newDocumentBuilder();
                }
                catch(ParserConfigurationException e)
                {
                    log.error(e);
                    return "";
                }
                Document doc = null;
                try
                {
                    try
                    {
                        doc = db.parse(new InputSource(new StringReader(xml)));
                    }
                    catch(IOException e)
                    {
                        log.error(e);
                        return "";
                    }
                }
                catch(SAXException e)
                {
                    log.error("Exception in parsing xml doc of google suggestion with query: " + query + " in language: " + market, e);
                    log.error("XML causing exception : " + xml);
                    return "";
                }
                NodeList getData = null;
                try
                {
                    getData = doc.getElementsByTagName("suggestion");
                }
                catch(NullPointerException e)
                {
                    log.error("Can not retrieve suggestions for query: " + query, e);
                    return "";
                }
                for(int i = 0; i < getData.getLength(); i++)
                {
                    Node currentSuggestion = getData.item(i);

                    suggestion += currentSuggestion.getAttributes().getNamedItem("data").getNodeValue();
                    suggestion += ", ";
                }
                if(suggestion.lastIndexOf(",") != -1)
                    suggestion = suggestion.substring(0, suggestion.lastIndexOf(","));
            }
            catch(NullPointerException e)
            {
                log.error("Google suggestions not retrieved for query: " + query, e);
                return "";
            }

            return suggestion;
        }
    }

    private static class Container
    {
        private String query;
        private String market;
        private String suggestionBing;
        private long timestamp;
        private String sessionId;
        private int userId;

        public Container(String query, String market, String suggestionBing, String sessionId, User user)
        {
            super();
            this.query = query;
            this.market = market;
            this.suggestionBing = suggestionBing;
            this.timestamp = System.currentTimeMillis();
            this.sessionId = sessionId;
            this.userId = user == null ? 0 : user.getId();
        }

        @Override
        public String toString()
        {
            return "Container [query=" + query + ", market=" + market + ", suggestionBing=" + suggestionBing + ", timestamp=" + timestamp + "]";
        }

    }

}

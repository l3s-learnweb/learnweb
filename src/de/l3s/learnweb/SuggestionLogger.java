package de.l3s.learnweb;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
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
import org.xml.sax.SAXException;

public class SuggestionLogger
{
    private static final Logger log = Logger.getLogger(SuggestionLogger.class);

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
	    queue.put(null);
	}
	catch(InterruptedException e)
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
		    Container container;

		    container = queue.take();

		    if(container == null) // stop method was called
			break;

		    String suggestionsGoogle = googleLogger(container.market, container.query);

		    try
		    {
			PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `lw_log_suggestions` (`query`, `market`, `timestamp`, `suggestions_bing`, `suggestions_google`, session_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
			insert.setString(1, container.query);
			insert.setString(2, container.market);
			insert.setTimestamp(3, new Timestamp(container.timestamp));
			insert.setString(4, container.suggestionBing);
			insert.setString(5, suggestionsGoogle);
			insert.setString(6, container.sessionId);
			insert.setInt(7, container.userId);
			insert.executeUpdate();

			//log.debug("Logged suggestion: " + container);
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

	private String googleLogger(String market, String query)
	{
	    String suggestion = "";
	    if(market.length() > 2)
	    {
		market = market.substring(0, 2);
	    }
	    String suggestorUrl = "http://suggestqueries.google.com/complete/search?output=toolbar&hl=" + market + "&q=" + query;
	    URL queryUrl = null;
	    try
	    {
		try
		{
		    queryUrl = new URL(suggestorUrl);
		}
		catch(MalformedURLException e)
		{
		    log.error("Error in URL formation of Google suggest query", e);
		    return "";
		}

		HttpURLConnection connection = null;
		try
		{
		    connection = (HttpURLConnection) queryUrl.openConnection();
		}
		catch(IOException e)
		{
		    log.error("Error in establishing connection with Google suggest query URL", e);
		    return "";
		}
		try
		{
		    connection.setRequestMethod("GET");
		}
		catch(ProtocolException e)
		{
		    log.error(e);
		    return "";
		}
		connection.setRequestProperty("Accept", "application/xml");

		InputStream xml = null;
		try
		{
		    xml = connection.getInputStream();
		}
		catch(IOException e)
		{
		    log.error("IO exception in getting google suggestions in xml form", e);
		    return "";
		}
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
			doc = db.parse(xml, "UTF-8");
		    }
		    catch(IOException e)
		    {
			log.error(e);
		    }
		}
		catch(SAXException e)
		{
		    log.error("Exception in parsing xml doc of google suggestion", e);
		}
		NodeList getData = doc.getElementsByTagName("suggestion");
		for(int i = 0; i < getData.getLength(); i++)
		{
		    Node currentSuggestion = getData.item(i);

		    suggestion += currentSuggestion.getAttributes().getNamedItem("data").getNodeValue();
		    suggestion += ", ";
		}
		suggestion.substring(0, suggestion.lastIndexOf(","));
	    }
	    catch(NullPointerException e)
	    {
		log.error("Google suggestions not retrieved", e);
		return "";
	    }

	    return suggestion;
	}
    }

    private class Container
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

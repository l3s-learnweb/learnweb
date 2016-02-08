package de.l3s.learnweb;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
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

    public void log(String query, String market, String suggestionBing)
    {
	try
	{
	    queue.put(new Container(query, market, suggestionBing));
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
			PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `lw_log_suggestions` (`query`, `market`, `timestamp`, `suggestions_bing`, `suggestions_google`) VALUES (?, ?, ?, ?, ?)");
			insert.setString(1, container.query);
			insert.setString(2, container.market);
			insert.setTimestamp(3, new Timestamp(container.timestamp));
			insert.setString(4, container.suggestionBing);
			insert.setString(5, suggestionsGoogle);
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
	    if(market.length() > 2)
	    {
		market = market.substring(0, 2);
	    }
	    String suggestorUrl = "http://suggestqueries.google.com/complete/search?output=toolbar&hl=" + market + "&q=" + query;
	    URL queryUrl = null;
	    try
	    {
		queryUrl = new URL(suggestorUrl);
	    }
	    catch(MalformedURLException e)
	    {
		log.error("Error in URL formation of Google suggest query", e);
	    }

	    HttpURLConnection connection = null;
	    try
	    {
		connection = (HttpURLConnection) queryUrl.openConnection();
	    }
	    catch(IOException e)
	    {
		log.error("Error in establishing connection with Google suggest query URL", e);
	    }
	    try
	    {
		connection.setRequestMethod("GET");
	    }
	    catch(ProtocolException e)
	    {
		log.error(e);
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
	    }
	    Document doc = null;
	    try
	    {
		doc = db.parse(xml);
	    }
	    catch(SAXException e)
	    {
		log.error("Exception in parsing xml doc of google suggestion", e);
	    }
	    catch(IOException e)
	    {
		log.error("Exception in parsing xml doc of google suggestion", e);
	    }
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = null;
	    try
	    {
		transformer = tf.newTransformer();
	    }
	    catch(TransformerConfigurationException e2)
	    {
		log.error(e2);
	    }
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    StringWriter writer = new StringWriter();
	    try
	    {
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
	    }
	    catch(TransformerException e1)
	    {
		log.error(e1);
	    }
	    String output = writer.getBuffer().toString().trim();
	    ArrayList<String> queries = new ArrayList<String>();
	    while(output.contains("=\""))
	    {
		int subStringIndex = output.indexOf("=\"", 0);
		output = output.substring(subStringIndex + 2, output.length() - 1);

		queries.add(output.substring(0, output.indexOf("\"")));
	    }
	    String suggestion = StringUtils.join(queries).replaceAll("\\[|\\]", "");
	    return suggestion;
	}
    }

    private class Container
    {
	String query;
	String market;
	String suggestionBing;
	long timestamp;

	public Container(String query, String market, String suggestionBing)
	{
	    super();
	    this.query = query;
	    this.market = market;
	    this.suggestionBing = suggestionBing;
	    this.timestamp = System.currentTimeMillis();
	}

	@Override
	public String toString()
	{
	    return "Container [query=" + query + ", market=" + market + ", suggestionBing=" + suggestionBing + ", timestamp=" + timestamp + "]";
	}

    }

}

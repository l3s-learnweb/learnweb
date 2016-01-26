package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

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

		    // TODO get Google suggestions for container.query, container.market
		    String suggestionsGoogle = "";

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

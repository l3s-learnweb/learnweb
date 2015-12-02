package de.l3s.archiveSearch;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import de.l3s.archivedemo.ArchiveSearchManager;
import de.l3s.archivedemo.BingAzure;
import de.l3s.archivedemo.CDXClient;
import de.l3s.archivedemo.Query;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.ResourceDecorator;

/**
 * Request 1000 results for selected queries to compare with Tarcicios results
 * "Angela Merkel 2009", "Angelina Jolie", "Mark Zuckerberg", "Star Wars", "Barack Obama"
 * 
 * @author Kemkes
 *
 */
public class TaskTarcisioCompare
{
    private final static Logger log = Logger.getLogger(TaskTarcisioCompare.class);

    public static void main(String[] args) throws DocumentException, IOException, SQLException
    {
	Calendar minCrawlTime = Calendar.getInstance();
	minCrawlTime.add(Calendar.DATE, -60);
	final CDXClient cdxClient = new CDXClient(minCrawlTime.getTime());

	ArchiveSearchManager archiveManager = Learnweb.getInstance().getArchiveSearchManager();

	BingAzure bing = new BingAzure();
	//
	String[] queries = { "Angela Merkel 2009", "Angelina Jolie", "Mark Zuckerberg", "Star Wars", "Barack Obama" };

	for(String queryString : queries)
	{
	    Query query;
	    log.debug("Process query: " + queryString);

	    query = archiveManager.getQueryByQueryString("de-DE", queryString);

	    if(query == null || query.getRequestedResultCount() != 1000)
	    {
		log.debug("Search at bing");
		query = new Query();
		query.setQueryString(queryString);
		query.setDisableQueryAlterations(false);
		query.setMarket("de-DE");
		query.setRequestedResultCount(1000);

		bing.search(query, "web");

		log.debug("found results: " + query.getLoadedResultCount());
	    }
	    else
		log.debug("Use cached query: " + query.getId());

	    List<ResourceDecorator> results = query.getResults();
	    for(ResourceDecorator resource : results)
	    {
		int retries = 0;
		boolean isArchived = false;
		do
		{
		    isArchived = cdxClient.isArchived(resource);
		    cdxClient.resetAPICounters();
		    retries++;
		}
		while(cdxClient.getWaybackAPIerrors() > 0 && retries < 5); // retry 5 times

		log.debug(isArchived + " - " + resource.getUrl());

	    }

	}
    }

}

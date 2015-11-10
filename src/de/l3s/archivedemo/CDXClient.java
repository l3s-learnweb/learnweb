package de.l3s.archivedemo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.util.StringHelper;

public class CDXClient
{
    private static final Logger log = Logger.getLogger(CDXClient.class);

    private int waybackAPIerrors = 0;
    private int waybackAPIrequests = 0;

    private SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
    private int minCrawlTime;
    private ArchiveSearchManager archiveSearchManager;

    /**
     * 
     * @param minCrawlTime results which have been crawled before this date must be crawled again
     */
    public CDXClient(Date minCrawlTime)
    {
	this.minCrawlTime = (int) (minCrawlTime.getTime() / 1000);
	this.archiveSearchManager = Learnweb.getInstance().getArchiveSearchManager();
    }

    /**
     * This method checks whether a URL is archived. If yes it updates the first_timestamp and last_timestamp fields of the resource's meta data
     * 
     * @param resource
     * @return
     * @throws SQLException
     * @throws NumberFormatException
     */
    public boolean isArchived(ResourceDecorator resource) throws NumberFormatException, SQLException
    {
	int crawlTime = 0, captures = 0;

	if(resource.getMetadataValue("url_captures") != null) // the capture dates have been crawled before
	{
	    crawlTime = Integer.parseInt(resource.getMetadataValue("crawl_time"));
	    captures = Integer.parseInt(resource.getMetadataValue("url_captures"));
	}

	//System.out.println("rank: " + resource.getRankAtService() + "; " + captures + "; " + minCrawlTime + "; " + resource.getTitle());

	if(crawlTime < minCrawlTime) //&& waybackAPIerrors < MAX_API_ERRORS)
	{
	    captures = 0;
	    waybackAPIrequests++;
	    String url = resource.getUrl().substring(resource.getUrl().indexOf("//") + 2); // remove leading http(s)://

	    Date lastCapture = null, firstCapture = getFirstCaptureDate(url);

	    if(firstCapture != null)
	    {
		lastCapture = getLastCaptureDate(url);

		if(lastCapture != null)
		{
		    resource.getResource().setMetadataValue("first_timestamp", waybackDateFormat.format(firstCapture));
		    resource.getResource().setMetadataValue("last_timestamp", waybackDateFormat.format(lastCapture));
		    captures = 1; // one capture date -> at least one capture
		}
	    }

	    if(waybackAPIerrors == 0)
		archiveSearchManager.cacheCaptureCount(Integer.parseInt(resource.getMetadataValue("query_id")), resource.getRankAtService(), firstCapture, lastCapture, captures);
	}

	return captures > 0;
    }

    public Date getFirstCaptureDate(String url)
    {
	return getCaptureDate(url, 1);
    }

    public Date getLastCaptureDate(String url)
    {
	return getCaptureDate(url, -1);
    }

    private Date getCaptureDate(String url, int limit)
    {
	String response;
	try
	{
	    response = IOUtils.toString(new URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp&limit=" + limit));

	    if(response.trim().length() == 0)
		return null;

	    return waybackDateFormat.parse(response);
	}
	catch(MalformedURLException e)
	{
	    throw new RuntimeException(e);
	}
	catch(ParseException | IOException e)
	{
	    if(e.getMessage().contains("HTTP response code: 403")) // blocked by robots
		return null;

	    log.error("wayback api error: " + e.getMessage());
	    waybackAPIerrors++;
	}

	return null;
    }

    public int getWaybackAPIrequests()
    {
	return waybackAPIrequests;
    }

    /**
     * Set the request and error counter to 0
     * 
     * @param waybackAPIrequests
     */
    public void resetAPICounters()
    {
	this.waybackAPIrequests = 0;
	this.waybackAPIrequests = 0;
    }

    public int getWaybackAPIerrors()
    {
	return waybackAPIerrors;
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub

    }

}

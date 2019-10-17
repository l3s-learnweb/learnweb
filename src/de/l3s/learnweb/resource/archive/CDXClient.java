package de.l3s.learnweb.resource.archive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.util.Misc;
import de.l3s.util.StringHelper;

public class CDXClient
{
    private static final Logger log = Logger.getLogger(CDXClient.class);

    private int waybackAPIerrors = 0;
    private int waybackAPIrequests = 0;

    private SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    //private ArchiveSearchManager archiveSearchManager;

    public CDXClient()
    {

    }

    /**
     * This method checks whether a URL is archived. If yes it updates the first_timestamp and last_timestamp fields of the resource's meta data
     *
     * @param resource
     * @return
     * @throws SQLException
     * @throws NumberFormatException
     * @throws IOException
     * @throws ParseException
     */
    public boolean isArchived(ResourceDecorator resource) throws NumberFormatException, SQLException, ParseException, IOException
    {
        //int crawlTime = 0, captures = 0;

        /*if(resource.getMetadataValue("url_captures") != null) // the capture dates have been crawled before
        {
            crawlTime = Integer.parseInt(resource.getMetadataValue("crawl_time"));
            captures = Integer.parseInt(resource.getMetadataValue("url_captures"));
        }
        
        
        if(crawlTime < minCrawlTime) //&& waybackAPIerrors < MAX_API_ERRORS)
        {*/
        //log.debug("Execute Wayback query for: " + resource.getUrl());
        //	log.debug("Getting first and last capture info for: " + resource.getUrl());
        int captures = 0;
        waybackAPIrequests++;
        //int oldwaybackAPIerrors = waybackAPIerrors;
        String url = resource.getUrl().substring(resource.getUrl().indexOf("//") + 2); // remove leading http(s)://

        Date lastCapture, firstCapture = getFirstCaptureDate(url);

        if(firstCapture != null)
        {
            lastCapture = getLastCaptureDate(url);

            if(lastCapture != null)
            {
                resource.getResource().setMetadataValue("first_timestamp", waybackDateFormat.format(firstCapture));
                resource.getResource().setMetadataValue("last_timestamp", waybackDateFormat.format(lastCapture));
                captures = 1; // one capture date -> at least one capture
                Learnweb.getInstance().getWaybackCapturesLogger().logWaybackUrl(resource.getUrl(), firstCapture.getTime(), lastCapture.getTime());
                //		log.debug("URL:" + url + "; First Capture:" + firstCapture + "; Last Capture:" + lastCapture);
            }
        }

        /*
        if(oldwaybackAPIerrors == waybackAPIerrors)
        archiveSearchManager.cacheCaptureCount(Integer.parseInt(resource.getMetadataValue("query_id")), resource.getRankAtService(), firstCapture, lastCapture, captures);
        }
        */
        return captures > 0;
    }

    public Date getFirstCaptureDate(String url) throws ParseException, IOException
    {
        return getCaptureDate(url, 1);
    }

    public Date getLastCaptureDate(String url) throws ParseException, IOException
    {
        return getCaptureDate(url, -1);
    }

    private Date getCaptureDate(String url, int limit) throws ParseException, IOException
    {
        for(int retry = 2; retry >= 0; retry--) // retry 2 times if we get retrieve "Unexpected end of file from server"
        {
            try
            {
                URLConnection connection = new java.net.URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp&limit=" + limit).openConnection();
                connection.setRequestProperty("User-Agent", "L3S-CDX-Client/1.0 (User=https://learnweb.l3s.uni-hannover.de/)");

                StringBuilder sb = new StringBuilder();
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while((inputLine = in.readLine()) != null)
                    sb.append(inputLine);
                in.close();

                String response = sb.toString();
                if(response.trim().length() == 0)
                    return null;

                synchronized(this)
                {
                    return waybackDateFormat.parse(response);
                }
            }
            catch(MalformedURLException e)
            {
                log.error("Can't check records of URL: " + url + "; Limit: " + limit, e);
                return null;
            }
            catch(ParseException | IOException e)
            {
                String msg = e.getMessage();
                if(msg.contains("HTTP response code: 403")) // blocked by robots
                    return null;
                else if((msg.equals("Unexpected end of file from server") || msg.startsWith("Server returned HTTP response code: 50")) && retry > 0) // hit request limit
                {
                    Misc.sleep(600);
                    log.debug("To many api requests => Sleep a while");
                }
                else
                    throw e;
            }
        }
        throw new IllegalStateException();
    }

    /* simpler implementation
     *
    private Date getCaptureDate(String url, int limit)
    {
        String response;
        try
        {
            response = IOUtils.toString(new URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp&limit=" + limit), "UTF-8");

            if(response.trim().length() == 0)
            {
                //log.debug("No Captures for: " + url);
                return null;
            }

            return waybackDateFormat.parse(response);
        }
        catch(MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
        catch(ParseException | IOException e)
        {
            if(e.getMessage().contains("HTTP response code: 403")) // blocked by robots
            {
                //log.error("wayback api error:" + e.getMessage());
                return null;
            }

            log.warn("wayback api error: " + e.getMessage());
            waybackAPIerrors++;
        }

        return null;
    }
    */

    public List<Long> getCaptures(String url)
    {
        List<String> response;
        try
        {

            url = url.substring(url.indexOf("//") + 2); // remove leading http(s)://
            log.debug("Getting wayback captures for: " + url);
            response = IOUtils.readLines(new URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp").openStream(), "UTF-8");

            if(response == null || response.isEmpty())
            {
                log.debug("No Captures for: " + url);
                return null;
            }

            List<Long> timestamps = new LinkedList<>();
            for(String s : response)
            {
                timestamps.add(waybackDateFormat.parse(s).getTime());
            }
            log.debug("Fetched " + timestamps.size() + " captures for: " + url);
            return timestamps;
        }
        catch(MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
        catch(ParseException | IOException e)
        {
            if(e.getMessage().contains("HTTP response code: 403")) // blocked by robots
            {
                log.error("wayback api error:" + e.getMessage());
                return null;
            }
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
     */
    public void resetAPICounters()
    {
        this.waybackAPIrequests = 0;
        this.waybackAPIerrors = 0;
    }

    public int getWaybackAPIerrors()
    {
        return waybackAPIerrors;
    }

}

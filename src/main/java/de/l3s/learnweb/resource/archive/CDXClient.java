package de.l3s.learnweb.resource.archive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;

import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.util.Misc;
import de.l3s.util.StringHelper;

public class CDXClient {
    private static final Logger log = LogManager.getLogger(CDXClient.class);
    private final DateTimeFormatter waybackDateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private int waybackApiErrors = 0;
    private int waybackApiRequests = 0;

    /**
     * This method checks whether a URL is archived. If yes it updates the first_timestamp and last_timestamp fields of the resource's metadata
     */
    public boolean isArchived(ResourceDecorator resource) throws NumberFormatException, IOException {
        //int crawlTime = 0, captures = 0;

        /*if(resource.getMetadataValue("url_captures") != null) { // the capture dates have been crawled before
            crawlTime = Integer.parseInt(resource.getMetadataValue("crawl_time"));
            captures = Integer.parseInt(resource.getMetadataValue("url_captures"));
        }

        if(crawlTime < minCrawlTime) //&& waybackApiErrors < MAX_API_ERRORS) {*/
        //log.debug("Execute Wayback query for: " + resource.getUrl());
        //log.debug("Getting first and last capture info for: " + resource.getUrl());
        int captures = 0;
        waybackApiRequests++;
        //int oldWaybackApiErrors = waybackApiErrors;
        String url = resource.getUrl().substring(resource.getUrl().indexOf("//") + 2); // remove leading http(s)://

        LocalDateTime lastCapture;
        LocalDateTime firstCapture = getFirstCaptureDate(url);

        if (firstCapture != null) {
            lastCapture = getLastCaptureDate(url);

            if (lastCapture != null) {
                resource.getResource().setMetadataValue("first_timestamp", waybackDateFormat.format(firstCapture));
                resource.getResource().setMetadataValue("last_timestamp", waybackDateFormat.format(lastCapture));
                captures = 1; // one capture date -> at least one capture
                Beans.getInstance(WaybackCapturesLogger.class).logWaybackUrl(resource.getUrl(), firstCapture, lastCapture);
                // log.debug("URL:" + url + "; First Capture:" + firstCapture + "; Last Capture:" + lastCapture);
            }
        }

        return captures > 0;
    }

    public LocalDateTime getFirstCaptureDate(String url) throws IOException {
        return getCaptureDate(url, 1);
    }

    public LocalDateTime getLastCaptureDate(String url) throws IOException {
        return getCaptureDate(url, -1);
    }

    private LocalDateTime getCaptureDate(String url, int limit) throws IOException {
        for (int retry = 2; retry >= 0; retry--) { // retry 2 times if we get retrieve "Unexpected end of file from server"
            try {
                URLConnection connection = new java.net.URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp&limit=" + limit).openConnection();
                connection.setRequestProperty("User-Agent", "L3S-CDX-Client/1.0 (User=https://learnweb.l3s.uni-hannover.de/)");

                String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
                if (response.trim().isEmpty()) {
                    return null;
                }

                return LocalDateTime.parse(response, waybackDateFormat);
            } catch (MalformedURLException e) {
                log.error("Can't check records of URL: {}; Limit: {}", url, limit, e);
                return null;
            } catch (IOException e) {
                String msg = e.getMessage();
                if (msg.contains("HTTP response code: 403")) {
                    return null; // blocked by robots
                } else if (("Unexpected end of file from server".equals(msg) || msg.startsWith("Server returned HTTP response code: 50")) && retry > 0) {
                    Misc.sleep(600); // hit request limit
                    log.debug("To many api requests => Sleep a while");
                } else {
                    throw e;
                }
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

    public List<LocalDateTime> getCaptures(String url) {
        List<String> response;
        try {

            url = url.substring(url.indexOf("//") + 2); // remove leading http(s)://
            log.debug("Getting wayback captures for: {}", url);
            response = IOUtils.readLines(new URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp").openStream(), StandardCharsets.UTF_8);

            if (response.isEmpty()) {
                log.debug("No Captures for: {}", url);
                return null;
            }

            List<LocalDateTime> timestamps = new LinkedList<>();
            for (String s : response) {
                timestamps.add(LocalDateTime.parse(s, waybackDateFormat));
            }
            log.debug("Fetched {} captures for: {}", timestamps.size(), url);
            return timestamps;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            if (e.getMessage().contains("HTTP response code: 403")) { // blocked by robots
                log.error("wayback api error:{}", e.getMessage());
                return null;
            }
            log.error("wayback api error: {}", e.getMessage());
            waybackApiErrors++;
        }

        return null;
    }

    public int getWaybackApiRequests() {
        return waybackApiRequests;
    }

    /**
     * Set the request and error counter to 0.
     */
    public void resetAPICounters() {
        this.waybackApiRequests = 0;
        this.waybackApiErrors = 0;
    }

    public int getWaybackApiErrors() {
        return waybackApiErrors;
    }

}

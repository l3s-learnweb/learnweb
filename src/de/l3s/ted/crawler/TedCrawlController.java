package de.l3s.ted.crawler;

import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class TedCrawlController implements Runnable
{
    public static final String CRAWL_STORAGE_FOLDER = "/home/learnweb_user/TED_Crawl/";

    @Override
    public void run()
    {
	try
	{
	    int numberOfCrawlers = 20;
	    String lastBrowsingPage;
	    Document doc = Jsoup.parse(new URL("http://www.ted.com/talks?page=1"), 10000);
	    Element lastBrowsingPageel = doc.select("a.pagination__item:nth-child(13)").first();
	    lastBrowsingPage = lastBrowsingPageel.text();
	    int lastBrowsing = Integer.parseInt(lastBrowsingPage);

	    CrawlConfig config = new CrawlConfig();
	    config.setCrawlStorageFolder(CRAWL_STORAGE_FOLDER);

	    /*
	     * Be polite: Make sure that we don't send more than 1 request per
	     * second (1000 milliseconds between requests).
	     */
	    config.setPolitenessDelay(3000);
	    config.setIncludeHttpsPages(true);

	    config.setMaxDepthOfCrawling(3);

	    config.setMaxPagesToFetch(-1);
	    config.setResumableCrawling(false);

	    PageFetcher pageFetcher = new PageFetcher(config);
	    RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
	    robotstxtConfig.setEnabled(false);
	    RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);

	    CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

	    for(int i = 1; i < lastBrowsing; i++)
		controller.addSeed("http://www.ted.com/talks?page=" + i);

	    controller.start(TedCrawler.class, numberOfCrawlers);
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
    }
}

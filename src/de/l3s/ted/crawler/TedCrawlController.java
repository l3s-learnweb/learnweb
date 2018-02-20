package de.l3s.ted.crawler;

import org.apache.log4j.Logger;

public class TedCrawlController implements Runnable
{
    public static final String CRAWL_STORAGE_FOLDER = "/home/learnweb_user/TED_Crawl/";
    private static final Logger log = Logger.getLogger(TedCrawlController.class);

    @Override
    public void run()
    {
        /*try
        {
            int numberOfCrawlers = 20;
            String lastBrowsingPage;
            Document doc = Jsoup.parse(new URL("http://www.ted.com/talks?page=1"), 10000);
            Element lastBrowsingPageel = doc.select("a.pagination__item:nth-child(13)").first();
            lastBrowsingPage = lastBrowsingPageel.text();
            int lastBrowsing = Integer.parseInt(lastBrowsingPage);
        
            CrawlConfig config = new CrawlConfig();
            config.setCrawlStorageFolder(CRAWL_STORAGE_FOLDER);
        
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
            log.error("unhandled error", e);
        }*/
    }
}

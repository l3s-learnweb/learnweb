package de.l3s.learnweb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.SearchFilters.SERVICE;
import de.l3s.learnweb.WaybackUrlManager.UrlRecord;
import de.l3s.searchHistoryTest.SolrEdgeComputeForEachSession;
import de.l3s.util.StringHelper;
import de.l3s.util.URL;

public class SearchLogManager
{
    private static final Logger log = Logger.getLogger(SearchLogManager.class);
    private static final String QUERY_COLUMNS = "`query`, `mode`, `service`, `language`, `filters`, `user_id`, `timestamp`";
    private static final String RESOURCE_COLUMNS = "`search_id`, `rank`, `resource_id`, `url`, `title`, `description`, `thumbnail_url`, `thumbnail_height`, `thumbnail_width`";
    private static final String ACTION_COLUMNS = "`search_id`, `rank`, `user_id`, `action`, `timestamp`";
    private static final String LAST_ENTRY = "last_entry"; // this element indicates that the consumer thread should stop

    //Queue to hold the URLs whose HTML needs to be logged
    private final LinkedBlockingQueue<String> queue;
    private final Thread consumerThread;

    //Queue to hold the search ids that needs to be annotated using yahooFEL
    private LinkedBlockingQueue<String> searchIdQueue;
    private Thread felAnnotationConsumerThread;
    private String felAnnotatorPath;
    private boolean felAnnotate = false;

    public enum LOG_ACTION
    {
        resource_clicked,
        resource_saved
    };

    private final Learnweb learnweb;

    public SearchLogManager(Learnweb learnweb)
    {
        super();
        this.learnweb = learnweb;
        this.queue = new LinkedBlockingQueue<String>();
        this.consumerThread = new Thread(new Consumer());
        this.consumerThread.start();

        this.felAnnotatorPath = learnweb.getProperties().getProperty("FEL_ANNOTATOR_PATH", "");
        System.out.println("felAnnotatorPath: " + felAnnotatorPath);
        File felJarFile = new File(felAnnotatorPath);
        if(felJarFile.exists())
        {
            this.searchIdQueue = new LinkedBlockingQueue<String>();
            this.felAnnotationConsumerThread = new Thread(new FELAnnotationConsumer());
            this.felAnnotationConsumerThread.start();
            felAnnotate = true;
        }
        else
            log.error("Couldn't load FEL Annotator jar");

    }

    protected int logQuery(String query, MODE searchMode, SERVICE searchService, String language, String searchFilters, User user)
    {
        try
        {
            int userId = user == null ? 0 : user.getId();

            if(searchFilters == null)
                searchFilters = "";
            else if(searchFilters.length() > 1000)
                searchFilters = searchFilters.substring(0, 1000);

            //log.debug("log: " + query + " _ " + searchMode + " _ " + searchFilters + " _ " + userId + " _ ");

            PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_query` (" + QUERY_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);", Statement.RETURN_GENERATED_KEYS);
            insert.setString(1, query);
            insert.setString(2, searchMode.name());
            insert.setString(3, searchService.name());
            insert.setString(4, language);
            insert.setString(5, searchFilters);
            insert.setInt(6, userId);
            insert.executeUpdate();

            ResultSet rs = insert.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            int searchId = rs.getInt(1);

            insert.close();

            return searchId;
        }
        catch(Exception e)
        {
            log.error("Could not log query=" + query + "; mode=" + searchMode + "; filters=" + searchFilters + "; user=" + user + ";", e);
        }
        return -1;
    }

    protected void logResources(int searchId, List<ResourceDecorator> resources, boolean logHTML, int pageId)
    {
        if(resources.size() == 0)
        {
            log.warn("No resources to log for searchId=" + searchId);
            return;
        }
        if(searchId < 0) // failed to log query, no need to log resources
            return;

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `learnweb_large`.`sl_resource` (" + RESOURCE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");)
        {
            for(ResourceDecorator decoratedResource : resources)
            {
                insert.setInt(1, searchId);
                insert.setInt(2, decoratedResource.getTempId());
                if(decoratedResource.getResource().getId() > 0) // resource is stored in Learnweb, we do not need to save the title or description
                {
                    insert.setInt(3, decoratedResource.getResource().getId());
                    insert.setNull(4, Types.VARCHAR);
                    insert.setNull(5, Types.VARCHAR);
                    insert.setNull(6, Types.VARCHAR);
                    insert.setNull(7, Types.VARCHAR);
                    insert.setInt(8, 0);
                    insert.setInt(9, 0);
                }
                else // no learnweb resource -> store title URL and description
                {
                    insert.setNull(3, Types.INTEGER);
                    insert.setString(4, decoratedResource.getUrl());
                    insert.setString(5, StringHelper.shortnString(decoratedResource.getTitle(), 250));
                    insert.setString(6, StringHelper.shortnString(decoratedResource.getDescription(), 1000));

                    Thumbnail thumbnail = decoratedResource.getThumbnail2();
                    if(thumbnail != null)
                    {
                        insert.setString(7, thumbnail.getUrl());
                        insert.setInt(8, thumbnail.getHeight() > 65535 ? 65535 : thumbnail.getHeight());
                        insert.setInt(9, thumbnail.getWidth() > 65535 ? 65535 : thumbnail.getWidth());
                    }
                    else
                    {
                        insert.setNull(7, Types.VARCHAR);
                        insert.setInt(8, 0);
                        insert.setInt(9, 0);
                    }
                }
                insert.addBatch();

                try
                {
                    if(logHTML && !queue.contains(decoratedResource.getUrl()))
                        queue.put(decoratedResource.getUrl());
                }
                catch(InterruptedException e)
                {
                    log.error("Couldn't log html for url: " + decoratedResource.getUrl(), e);
                }
            }

            insert.executeBatch();
            if(felAnnotate)
            {
                if(pageId == 1 && resources.size() >= 10)
                    searchIdQueue.put(Integer.toString(searchId));
                else if(pageId == 2)
                    searchIdQueue.put(Integer.toString(searchId));
            }
        }
        catch(Exception e)
        {
            log.error("Could not log resources of searchId=" + searchId, e);
        }
    }

    protected void logResourceClicked(int searchId, int rank, User user)
    {
        logAction(searchId, rank, user, LOG_ACTION.resource_clicked);
    }

    /**
     *
     * @param searchId
     * @param rank
     * @param user
     * @param newResourceId Id of the new stored resource
     */
    protected void logResourceSaved(int searchId, int rank, User user, int newResourceId)
    {
        logAction(searchId, rank, user, LOG_ACTION.resource_saved);
    }

    private void logAction(int searchId, int rank, User user, LOG_ACTION action)
    {
        try
        {
            int userId = user == null ? 0 : user.getId();

            PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `learnweb_large`.`sl_action` (" + ACTION_COLUMNS + ") VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)");
            insert.setInt(1, searchId);
            insert.setInt(2, rank);
            insert.setInt(3, userId);
            insert.setString(4, action.name());
            insert.executeUpdate();
            insert.close();
        }
        catch(Exception e)
        {
            log.error("Could not log resources action of searchId=" + searchId + "; rank=" + rank + "; user=" + user + "; action=" + action + ";", e);
        }
    }

    public void stop()
    {
        try
        {
            queue.put(LAST_ENTRY);
            consumerThread.join();

            log.debug("SearchLogManager url html fetcher thread was stopped");

            if(felAnnotate)
            {
                searchIdQueue.put(LAST_ENTRY);
                felAnnotationConsumerThread.join();

                log.debug("SearchLogManager fel annotation thread was stopped");
            }
        }
        catch(InterruptedException e)
        {
            log.fatal("Couldn't stop SearchLog manager html fetcher thread", e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean checkRelatedEntitiesForSearch(String searchId)
    {
        boolean relatedEntitiesExists = false;
        try
        {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM learnweb_large.sl_query_entities WHERE search_id = ?");
            pStmt.setInt(1, Integer.parseInt(searchId));
            ResultSet rs = pStmt.executeQuery();
            if(rs.next())
                relatedEntitiesExists = true;
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving related entities for search id: " + searchId, e);
        }
        return relatedEntitiesExists;
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
                    String url;

                    url = queue.take();

                    if(url == LAST_ENTRY) // stop method was called
                        break;

                    try
                    {
                        String asciiUrl = new URL(url).toString();

                        Date crawlTime;
                        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT url_id, crawl_time FROM `learnweb_large`.`sl_resource_html` WHERE url = ? ORDER BY crawl_time DESC LIMIT 1");
                        select.setString(1, asciiUrl);
                        ResultSet rs = select.executeQuery();
                        if(rs.next())
                        {
                            crawlTime = rs.getDate(2);
                            long timeSinceLastCrawl = System.currentTimeMillis() - crawlTime.getTime();
                            if(timeSinceLastCrawl < TimeUnit.DAYS.toMillis(1))
                                continue;
                        }

                        UrlRecord urlRecord = Learnweb.getInstance().getWaybackUrlManager().getHtmlContent(url);

                        PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_resource_html` (`url`, `html`, `crawl_time`, `status_code`) VALUES (?, ?, ?, ?)");

                        insert.setString(1, urlRecord.getUrl().toString());
                        insert.setString(2, urlRecord.getContent());
                        insert.setTimestamp(3, new java.sql.Timestamp(urlRecord.getStatusCodeDate().getTime()));
                        insert.setInt(4, urlRecord.getStatusCode());
                        insert.executeUpdate();
                    }
                    catch(SQLException e)
                    {
                        log.error("Exception while inserting the url record into database: " + url, e);
                    }
                    catch(IOException e)
                    {
                        log.error("Exception while retrieving html for url:" + url, e);
                    }
                    catch(URISyntaxException e)
                    {
                        log.error("Invalid url error: " + url, e);
                    }

                }

                log.debug("Search logger thread for logging html was stopped");
            }
            catch(InterruptedException e)
            {
                log.error("Search logger thread for logging html has crashed", e);
            }
        }
    }

    private class FELAnnotationConsumer implements Runnable
    {
        SolrEdgeComputeForEachSession seces = new SolrEdgeComputeForEachSession();

        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    String searchId;

                    searchId = searchIdQueue.take();

                    if(searchId == LAST_ENTRY) //stop method was called
                        break;

                    boolean relatedEntitiesExist = checkRelatedEntitiesForSearch(searchId);
                    if(!relatedEntitiesExist)
                    {
                        try
                        {
                            Process proc = Runtime.getRuntime().exec("java -Xmx4g -jar " + felAnnotatorPath + " " + searchId);

                            InputStream in = proc.getInputStream();
                            InputStream err = proc.getErrorStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(in));
                            String line;
                            while((line = br.readLine()) != null)
                                log.debug(line);
                            br.close();

                            BufferedReader brError = new BufferedReader(new InputStreamReader(err));
                            while((line = brError.readLine()) != null)
                                log.error("Error during FEL annotation process for search id: " + searchId + ";\n" + line);
                            brError.close();
                        }
                        catch(IOException e)
                        {
                            log.error("Error while annotating search id: " + searchId);
                        }
                        seces.insertEdgesForSessionGivenSearchId(searchId);
                    }
                }

                log.debug("SearchLogger FEL annotation thread for annotating search ids has stopped");
            }
            catch(InterruptedException e)
            {
                log.error("SearchLogger FEL annotation thread for annotating search ids has crashed", e);
            }
        }
    }
}

package de.l3s.learnweb.resource.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.learnweb.resource.search.SearchFilters.MODE;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class SearchLogManager
{
    private static final Logger log = Logger.getLogger(SearchLogManager.class);
    private static final String QUERY_COLUMNS = "`query`, `mode`, `service`, `language`, `filters`, `user_id`, `timestamp`";
    private static final String QUERY_COLUMNS_FOR_GROUP = "`group_id`, `query`, `mode`, `service`, `language`, `filters`, `user_id`, `timestamp`";
    private static final String RESOURCE_COLUMNS = "`search_id`, `rank`, `resource_id`, `url`, `title`, `description`, `thumbnail_url`, `thumbnail_height`, `thumbnail_width`";
    private static final String ACTION_COLUMNS = "`search_id`, `rank`, `user_id`, `action`, `timestamp`";
    private static final String LAST_ENTRY = "last_entry"; // this element indicates that the consumer thread should stop

    //Queue to hold the search ids that needs to be annotated using yahooFEL
    private LinkedBlockingQueue<String> searchIdQueue;
    private Thread felAnnotationConsumerThread;
    private String felAnnotatorPath;
    private boolean felAnnotate = false;

    public enum LOG_ACTION
    {
        resource_clicked,
        resource_saved
    }

    private final Learnweb learnweb;

    public SearchLogManager(Learnweb learnweb)
    {
        super();
        this.learnweb = learnweb;
        this.felAnnotatorPath = learnweb.getProperties().getProperty("FEL_ANNOTATOR_PATH", "");

        if(StringUtils.isEmpty(felAnnotatorPath))
            log.warn("'FEL_ANNOTATOR_PATH' not set in properties. Feature disabled");
        else
        {
            File felJarFile = new File(felAnnotatorPath);
            if(felJarFile.exists())
            {
                this.searchIdQueue = new LinkedBlockingQueue<>();
                /*
                this.felAnnotationConsumerThread = new Thread(new FELAnnotationConsumer());
                this.felAnnotationConsumerThread.start();
                
                felAnnotate = true;
                */

                log.debug("FEL Annotator disabled"); // temporarily or for ever?
            }
            else
                log.error("Couldn't load FEL Annotator jar");
        }
    }

    public int logQuery(String query, MODE searchMode, ResourceService searchService, String language, String searchFilters, User user)
    {
        int userId = user == null ? 0 : user.getId();

        if(searchFilters == null)
            searchFilters = "";
        else if(searchFilters.length() > 1000)
            searchFilters = searchFilters.substring(0, 1000);

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_query` (" + QUERY_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);", Statement.RETURN_GENERATED_KEYS))
        {
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

    public int logGroupQuery(final Group group, final String query, String searchFilters, final String language, final User user)
    {
        int userId = user == null ? 0 : user.getId();

        if(searchFilters == null)
            searchFilters = "";
        else if(searchFilters.length() > 1000)
            searchFilters = searchFilters.substring(0, 1000);

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_query` (" + QUERY_COLUMNS_FOR_GROUP + ") VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);", Statement.RETURN_GENERATED_KEYS))
        {
            insert.setInt(1, group.getId());
            insert.setString(2, query);
            insert.setString(3, MODE.group.name());
            insert.setString(4, ResourceService.learnweb.name());
            insert.setString(5, language);
            insert.setString(6, searchFilters);
            insert.setInt(7, userId);
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
            log.error("Could not log query=" + query + "; filters=" + searchFilters + "; group title=" + group.getTitle() + "; user=" + user + ";", e);
        }
        return -1;
    }

    public void logResources(int searchId, List<ResourceDecorator> resources, int pageId)
    {
        if(resources.size() == 0)
        {
            log.warn("No resources to log for searchId=" + searchId);
            return;
        }
        if(searchId < 0) // failed to log query, no need to log resources
            return;

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `learnweb_large`.`sl_resource` (" + RESOURCE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"))
        {
            for(ResourceDecorator decoratedResource : resources)
            {
                insert.setInt(1, searchId);
                insert.setInt(2, decoratedResource.getRank());
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

    private boolean checkRelatedEntitiesForSearch(String searchId)
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

    private class FELAnnotationConsumer implements Runnable
    {

        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    String searchId;

                    searchId = searchIdQueue.take();

                    if(Objects.equals(searchId, LAST_ENTRY)) //stop method was called
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
                        learnweb.getSearchSessionEdgeComputator().insertEdgesForSessionGivenSearchId(searchId);
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

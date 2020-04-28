package de.l3s.learnweb.resource.search;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class SearchLogManager
{
    private static final Logger log = LogManager.getLogger(SearchLogManager.class);
    private static final String QUERY_COLUMNS = "`query`, `mode`, `service`, `language`, `filters`, `user_id`, `timestamp`";
    private static final String QUERY_COLUMNS_FOR_GROUP = "`group_id`, `query`, `mode`, `service`, `language`, `filters`, `user_id`, `timestamp`";
    private static final String RESOURCE_COLUMNS = "`search_id`, `rank`, `resource_id`, `url`, `title`, `description`, `thumbnail_url`, `thumbnail_height`, `thumbnail_width`";
    private static final String ACTION_COLUMNS = "`search_id`, `rank`, `user_id`, `action`, `timestamp`";
    private static final String LAST_ENTRY = "last_entry"; // this element indicates that the consumer thread should stop

    public enum LOG_ACTION
    {
        resource_clicked,
        resource_saved
    }

    private final Learnweb learnweb;

    public SearchLogManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public int logQuery(String query, SearchMode searchMode, ResourceService searchService, String language, String searchFilters, User user)
    {
        int userId = user == null ? 0 : user.getId();

        if(searchFilters == null)
            searchFilters = "";
        else if(searchFilters.length() > 1000)
            searchFilters = searchFilters.substring(0, 1000);

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_query` (" + QUERY_COLUMNS + ", learnweb_version) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 3);", Statement.RETURN_GENERATED_KEYS))
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

    public int logGroupQuery(final int groupId, final String query, String searchFilters, final String language, final int userId)
    {
        if(searchFilters == null)
            searchFilters = "";
        else if(searchFilters.length() > 1000)
            searchFilters = searchFilters.substring(0, 1000);

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_query` (" + QUERY_COLUMNS_FOR_GROUP + ", learnweb_version) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 3);", Statement.RETURN_GENERATED_KEYS))
        {
            insert.setInt(1, groupId);
            insert.setString(2, query);
            insert.setString(3, SearchMode.group.name());
            insert.setString(4, ResourceService.learnweb.name());
            insert.setString(5, language == null ? "" : language);
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
            log.error("Could not log query=" + query + "; filters=" + searchFilters + "; group=" + groupId + "; user=" + userId + ";", e);
        }
        return -1;
    }

    public void logResources(int searchId, List<ResourceDecorator> resources, int pageId)
    {
        if(resources.isEmpty())
        {
            log.warn("No resources to log for searchId=" + searchId);
            return;
        }
        if(searchId < 0) // failed to log query, no need to log resources
            return;

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_resource` (" + RESOURCE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"))
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
                        insert.setInt(8, Math.min(thumbnail.getHeight(), 65535));
                        insert.setInt(9, Math.min(thumbnail.getWidth(), 65535));
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

            PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_action` (" + ACTION_COLUMNS + ") VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)");
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
}

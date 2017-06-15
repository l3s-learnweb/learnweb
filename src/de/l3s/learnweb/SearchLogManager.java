package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.SearchFilters.SERVICE;

public class SearchLogManager
{
    private static final Logger log = Logger.getLogger(SearchLogManager.class);
    private static final String QUERY_COLUMNS = "`query`, `mode`, `service`, `language`, `filters`, `user_id`";
    private static final String RESOURCE_COLUMNS = "`search_id`, `rank`, `resource_id`, `url`, `title`, `description`, `thumbnail_url`, `thumbnail_height`, `thumbnail_width`";

    public enum LOG_ACTION
    {
        resource_click,
        resource_dialog_open,
        resource_saved
    };

    private final Learnweb learnweb;

    public SearchLogManager(Learnweb learnweb)
    {
        super();
        this.learnweb = learnweb;
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

            log.debug("log: " + query + " _ " + searchMode + " _ " + searchFilters + " _ " + userId + " _ ");

            PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_query` (" + QUERY_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            insert.setString(1, query);
            insert.setString(2, searchMode.name());
            insert.setString(3, searchFilters);
            insert.setString(4, searchService.name());
            insert.setString(5, language);
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

    protected void logResources(int searchId, List<ResourceDecorator> resources)
    {
        if(resources.size() == 0)
        {
            log.warn("No resources to log for searchId=" + searchId);
            return;
        }
        if(searchId == -1) // failed to log query, no need to log resources
            return;

        try
        {
            PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `learnweb_large`.`sl_resource` (" + RESOURCE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

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
                    insert.setString(5, decoratedResource.getTitle());
                    insert.setString(6, decoratedResource.getDescription());
                    insert.setString(7, decoratedResource.getThumbnail2().getUrl());
                    insert.setInt(8, decoratedResource.getThumbnail2().getHeight());
                    insert.setInt(9, decoratedResource.getThumbnail2().getWidth());
                }
                insert.addBatch();
                insert.executeBatch();
            }

            insert.close();
        }
        catch(Exception e)
        {
            log.error("Could not log resources of searchId=" + searchId, e);
        }
    }

    protected void logAction(int searchId, LOG_ACTION action, int resourceTempId)
    {
        log.debug(searchId + " - " + action + " - " + resourceTempId);

    }
}

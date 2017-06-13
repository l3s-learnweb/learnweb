package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SearchFilters.MODE;

public class SearchLogManager
{
    private static final Logger log = Logger.getLogger(SearchLogManager.class);
    private static final String QUERY_COLUMNS = "`query`, `mode`, `filters`, `user_id`";
    private static final String RESOURCE_COLUMNS = "`search_id`, `rank`, `resource_id`, `url`, `title`, `description`";

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

    protected int logQuery(String query, MODE searchMode, String searchFilters, User user)
    {
        try
        {
            int userId = user == null ? 0 : user.getId();

            if(searchFilters == null)
                searchFilters = "";
            else if(searchFilters.length() > 1000)
                searchFilters = searchFilters.substring(0, 1000);

            log.debug("log: " + query + " _ " + searchMode + " _ " + searchFilters + " _ " + userId + " _ ");

            PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `learnweb_large`.`sl_query` (" + QUERY_COLUMNS + ") VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            replace.setString(1, query);
            replace.setString(2, searchMode.name());
            replace.setString(3, searchFilters);
            replace.setInt(4, userId);
            replace.executeUpdate();

            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            int searchId = rs.getInt(1);

            replace.close();

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

        log.debug(resources.size() + " - " + resources.get(0).getTitle());

        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `learnweb_large`.`sl_resource` (" + RESOURCE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)");

            for(ResourceDecorator decoratedResource : resources)
            {
                ps.setInt(1, searchId);
                ps.setInt(2, decoratedResource.getTempId());
                if(decoratedResource.getResource().getId() > 0) // resource is stored in Learnweb, we do not need to save the title or description
                {
                    ps.setInt(3, decoratedResource.getResource().getId());
                    ps.setNull(4, Types.VARCHAR);
                    ps.setNull(5, Types.VARCHAR);
                    ps.setNull(6, Types.VARCHAR);
                }
                else // no learnweb resource -> store title URL and description
                {
                    ps.setNull(3, Types.INTEGER);
                    ps.setString(4, decoratedResource.getUrl());
                    ps.setString(5, decoratedResource.getTitle());
                    ps.setString(6, decoratedResource.getDescription());
                }
                ps.addBatch();
                ps.executeBatch();
            }

            ps.close();

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

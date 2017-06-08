package de.l3s.learnweb;

import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SearchFilters.MODE;

public class SearchLogManager
{
    public final static Logger log = Logger.getLogger(SearchLogManager.class);

    public enum LOGACTION
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

    public int logQuery(String query, MODE searchMode, String searchFilters, User user)
    {
        /*
        int userId = user == null ? -1 : user.getId();
        log.debug("log: " + query + " _ " + searchMode + " _ " + searchFilters + " _ " + userId + " _ " + new Date());
        
        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_tag` (tag_id, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
        
        if(tag.getId() < 0) // the tag is not yet stored at the database
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, tag.getId());
        replace.setString(2, tag.getName());
        replace.executeUpdate();
        
        if(tag.getId() < 0) // get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            tag.setId(rs.getInt(1));
        }
        
        replace.close();
        
        int searchId = -1;
        
        return searchId;
        */
        return -1;
    }

    private static final String RESOURCE_COLUMNS = "`search_id`, `rank`, `resource_id`, `url`, `title`, `description`";

    void logResources(int searchId, List<ResourceDecorator> resources)
    {
        if(resources.size() == 0)
        {
            log.warn("No resources to log");
            return;
        }
        log.debug(resources.size() + " - " + resources.get(0).getTitle());
        /*
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO `learnweb_large`.`sl_resource` (" + RESOURCE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)");
        
            for(ResourceDecorator decoratedResource : resources)
            {
                ps.setInt(1, searchId);
                ps.setInt(2, decoratedResource.getTempId());
                if(decoratedResource.getResource().getId() > 0) // resource is stored in Learnweb, we do not need to save the title or description
                    ps.setInt(3, decoratedResource.getResource().getId());
                else
                    ps.setNull(3, Types.INTEGER);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
        
        }
        catch(Exception e)
        {
            log.error("Could not log resources of searchId=" + searchId, e);
        }
        */

    }

    void logAction(int searchId, LOGACTION resourceClick, int selectedResourceTempId)
    {
        log.debug(resourceClick + " - " + selectedResourceTempId);

    }
}

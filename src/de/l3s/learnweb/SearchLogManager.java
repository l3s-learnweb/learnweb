package de.l3s.learnweb;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.searchlogclient.Actions.ACTION;

public class SearchLogManager
{
    public final static Logger log = Logger.getLogger(SearchLogManager.class);

    private final Learnweb learnweb;

    public SearchLogManager(Learnweb learnweb)
    {
        super();
        this.learnweb = learnweb;
    }

    public int logQuery(String query, MODE searchMode, String searchFilters, User user)
    {
        int userId = user == null ? -1 : user.getId();
        log.debug("log: " + query + " _ " + searchMode + " _ " + searchFilters + " _ " + userId + " _ " + new Date());

        // TODO put into database and get unique id
        int searchId = -1;

        return searchId;
    }

    void logResources(int searchId, List<ResourceDecorator> resources)
    {
        log.debug(resources.size() + " - " + resources.get(0).getTitle());
        // TODO put into database 
        // in the beginning a myisam table with "insert delete into " might be sufficient. Later we need to do this async
    }

    void logAction(int searchId, ACTION resourceClick, int selectedResourceTempId)
    {
        log.debug(resourceClick + " - " + selectedResourceTempId);

    }
}

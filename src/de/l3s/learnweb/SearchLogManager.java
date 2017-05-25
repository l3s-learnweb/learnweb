package de.l3s.learnweb;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

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

    public SearchLogger createSearchLogger(String query, SearchFilters filters, User user)
    {
        int userId = user == null ? -1 : user.getId();
        log.debug("log: " + query + " _ " + filters.getMode() + " _ " + filters.getFiltersString() + " _ " + userId + " _ " + new Date());

        // TODO put into database and get unique id
        int searchId = -1;

        return new SearchLogger(searchId);
    }

    private void logResources(int searchId, List<ResourceDecorator> resources)
    {
        // TODO put into database 
        // in the beginning a myisam table with "insert delete into " might be sufficient. Later we need to do this async
    }

    public void logAction(int searchId, ACTION resourceClick, int selectedResourceTempId)
    {
        // TODO Auto-generated method stub

    }

    public class SearchLogger implements Serializable
    {
        private static final long serialVersionUID = 5553368645981168998L;
        private int searchId;

        private SearchLogger(int searchId)
        {
            super();
            this.searchId = searchId;
        }

        public int getSearchId()
        {
            return searchId;
        }

        public void logResources(List<ResourceDecorator> resources)
        {
            Learnweb.getInstance().getSearchLogManager().logResources(searchId, resources);
        }

        public void logAction(ACTION action, int selectedResourceTempId)
        {
            Learnweb.getInstance().getSearchLogManager().logAction(searchId, action, selectedResourceTempId);
        }

    }
}

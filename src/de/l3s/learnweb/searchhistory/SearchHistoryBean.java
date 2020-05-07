package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.searchhistory.SearchHistoryManager.Query;
import de.l3s.learnweb.searchhistory.SearchHistoryManager.SearchResult;
import de.l3s.learnweb.searchhistory.SearchHistoryManager.Session;

@Named
@ViewScoped
public class SearchHistoryBean extends ApplicationBean implements Serializable
{
    private static final Logger log = LogManager.getLogger(SearchHistoryBean.class);
    private static final long serialVersionUID = -7682314831788865416L;

    private int selectedUserId;
    private int selectedGroupId;

    private String searchQuery;
    private boolean showGroupHistory;

    private Session selectedSession;
    private Query selectedQuery;

    private List<Session> sessions;
    private List<Query> queries = new ArrayList<>();
    private Map<Integer, List<SearchResult>> snippets = new HashMap<>();

    /**
     * Load the variables that needs values before the view is rendered
     */
    public void onLoad()
    {
        if(isAjaxRequest())
            return;

        if(getUser() == null)
            return;

        if(selectedUserId == 0)
            selectedUserId = getUser().getId();
    }

    public Session getSelectedSession()
    {
        return selectedSession;
    }

    public void setSelectedSession(final Session selectedSession)
    {
        this.selectedSession = selectedSession;

        queries = null;
    }

    public Query getSelectedQuery()
    {
        return selectedQuery;
    }

    public void setSelectedQuery(final Query selectedQuery)
    {
        this.selectedQuery = selectedQuery;
    }

    public List<Session> getSessions() throws SQLException
    {
        if(this.showGroupHistory)
        {
            if(sessions == null && selectedGroupId > 0)
            {
                if(!SessionCache.instance().existsGroupId(selectedGroupId))
                {
                    SessionCache.instance().cacheByGroupId(selectedGroupId, getLearnweb().getSearchHistoryManager().getSessionsForGroupId(selectedGroupId));
                }
                sessions = SessionCache.instance().getByGroupId(selectedGroupId);
            }
        }
        else if(sessions == null)
        {
            sessions = getLearnweb().getSearchHistoryManager().getSessionsForUser(selectedUserId);
        }

        return sessions;
    }

    public List<Query> getQueries()
    {
        if(queries == null && selectedSession != null)
        {
            if(!showGroupHistory)
                queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromCache(selectedSession.getUserId(), selectedSession.getSessionId());
            else
                queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromGroupCache(selectedSession.getUserId(), selectedSession.getSessionId());
        }
        return queries;
    }

    public List<SearchResult> getSearchResults()
    {
        List<SearchResult> searchResults = new ArrayList<>();
        if(selectedQuery != null)
        {
            if(!snippets.containsKey(selectedQuery.getSearchId()))
                snippets.put(selectedQuery.getSearchId(), getLearnweb().getSearchHistoryManager().getSearchResultsForSearchId(selectedQuery.getSearchId(), 100));

            searchResults.addAll(snippets.get(selectedQuery.getSearchId()));
        }
        return searchResults;
    }

    public void onChangeGroup(AjaxBehaviorEvent event)
    {
        //log.info("group id: " + selectedGroupId);
    }

    public void actionSetShowGroupHistory()
    {
        showGroupHistory = true;
        searchQuery = null;
        sessions = null;
    }

    public void actionSetShowUserHistory()
    {
        showGroupHistory = false;
        searchQuery = null;
        sessions = null;
        selectedGroupId = -1;
    }

    public boolean isShowGroupHistory()
    {
        return showGroupHistory;
    }

    public User getCurrentUser()
    {
        User user = null;
        try
        {
            user = getLearnweb().getUserManager().getUser(selectedUserId);
        }
        catch(SQLException e)
        {
            log.error(e);
        }
        return user == null ? getUser() : user;
    }

    public int getSelectedUserId()
    {
        return selectedUserId;
    }

    public void setSelectedUserId(final int selectedUserId)
    {
        this.selectedUserId = selectedUserId;
    }

    public int getSelectedGroupId()
    {
        return selectedGroupId;
    }

    public void setSelectedGroupId(int selectedGroupId)
    {
        if(selectedGroupId != this.selectedGroupId)
        {
            showGroupHistory = true;
            searchQuery = null;
            sessions = null;
        }

        //log.info("selected group id: " + selectedGroupId);
        this.selectedGroupId = selectedGroupId;
    }

    public void search() throws SQLException
    {
        sessions = null;
        filterSessionsByQuery(searchQuery);
    }

    public void reset() throws SQLException
    {
        sessions = null;
        searchQuery = null;
    }

    private void filterSessionsByQuery(String filterQuery) throws SQLException
    {
        if(StringUtils.isEmpty(filterQuery))
        {
            return;
        }

        boolean isSearchUser = false;
        if(filterQuery.startsWith("user:") || filterQuery.startsWith("u:"))
        {
            isSearchUser = true;
            filterQuery = filterQuery.replace("user:", "").replace("u:", "").trim();
        }

        final boolean finalIsSearchUser = isSearchUser;
        final String finalQuery = filterQuery;

        List<Session> allSessions = getSessions();
        if(allSessions == null || allSessions.isEmpty())
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "Sessions list is empty.");
        }
        else
        {
            sessions = allSessions.stream().filter(session -> {
                if(finalIsSearchUser)
                {
                    return StringUtils.containsIgnoreCase(session.getUserName(), finalQuery);
                }

                return session.getQueries().stream().anyMatch(query -> StringUtils.containsIgnoreCase(query.getQuery(), finalQuery));

            }).collect(Collectors.toList());
        }
    }

    public String getSearchQuery()
    {
        return searchQuery;
    }

    public void setSearchQuery(final String searchQuery)
    {
        this.searchQuery = searchQuery;
    }
}

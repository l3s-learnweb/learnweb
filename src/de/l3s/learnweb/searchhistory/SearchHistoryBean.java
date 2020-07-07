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
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.searchhistory.SearchHistoryManager.Query;
import de.l3s.learnweb.searchhistory.SearchHistoryManager.Session;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class SearchHistoryBean extends ApplicationBean implements Serializable {
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
    private final Map<Integer, List<ResourceDecorator>> snippets = new HashMap<>();

    /**
     * Load the variables that needs values before the view is rendered.
     */
    public void onLoad() {
        if (isAjaxRequest()) {
            return;
        }

        if (getUser() == null) {
            return;
        }

        if (selectedUserId == 0) {
            selectedUserId = getUser().getId();
        }
    }

    public Session getSelectedSession() {
        return selectedSession;
    }

    public void setSelectedSession(final Session selectedSession) {
        this.selectedSession = selectedSession;

        queries = null;
    }

    public Query getSelectedQuery() {
        return selectedQuery;
    }

    public void setSelectedQuery(final Query selectedQuery) {
        this.selectedQuery = selectedQuery;
    }

    public List<Session> getSessions() throws SQLException {
        if (sessions == null && showGroupHistory && selectedGroupId > 0) {
            sessions = getLearnweb().getSearchHistoryManager().getSessionsForGroupId(selectedGroupId);
        } else if (sessions == null) {
            sessions = getLearnweb().getSearchHistoryManager().getSessionsForUser(selectedUserId);
        }

        return sessions;
    }

    public List<Query> getQueries() {
        if (queries == null && selectedSession != null) {
            if (!showGroupHistory) {
                queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromCache(selectedSession.getUserId(), selectedSession.getSessionId());
            } else {
                queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromGroupCache(selectedSession.getUserId(), selectedSession.getSessionId());
            }
        }
        return queries;
    }

    public String getSearchResultsView() {
        if ("text".equals(selectedQuery.getMode())) {
            return "list";
        } else if ("image".equals(selectedQuery.getMode())) {
            return "float";
        } else if ("video".equals(selectedQuery.getMode())) {
            return "grid";
        }

        return null;
    }

    public List<ResourceDecorator> getSearchResults() {
        List<ResourceDecorator> searchResults = new ArrayList<>();
        if (selectedQuery != null) {
            if (!snippets.containsKey(selectedQuery.getSearchId())) {
                snippets.put(selectedQuery.getSearchId(), getLearnweb().getSearchHistoryManager().getSearchResultsForSearchId(selectedQuery.getSearchId(), 100));
            }

            searchResults.addAll(snippets.get(selectedQuery.getSearchId()));
        }
        return searchResults;
    }

    public void onChangeGroup(AjaxBehaviorEvent event) throws SQLException {
        reset();
    }

    public void actionSetShowGroupHistory() {
        showGroupHistory = true;
        searchQuery = null;
        sessions = null;
    }

    public void actionSetShowUserHistory() {
        showGroupHistory = false;
        searchQuery = null;
        sessions = null;
        selectedGroupId = -1;
    }

    public boolean isShowGroupHistory() {
        return showGroupHistory;
    }

    public User getCurrentUser() throws SQLException {
        User user = null;

        user = getLearnweb().getUserManager().getUser(selectedUserId);

        return user == null ? getUser() : user;
    }

    public int getSelectedUserId() {
        return selectedUserId;
    }

    public void setSelectedUserId(final int selectedUserId) {
        this.selectedUserId = selectedUserId;
    }

    public int getSelectedGroupId() {
        return selectedGroupId;
    }

    public void setSelectedGroupId(int selectedGroupId) {
        if (selectedGroupId != this.selectedGroupId) {
            showGroupHistory = true;
            searchQuery = null;
            sessions = null;
        }

        //log.info("selected group id: " + selectedGroupId);
        this.selectedGroupId = selectedGroupId;
    }

    public void search() throws SQLException {
        sessions = null;
        filterSessionsByQuery(searchQuery);
    }

    public void reset() throws SQLException {
        sessions = null;
        searchQuery = null;
    }

    private void filterSessionsByQuery(String filterQuery) throws SQLException {
        if (StringUtils.isEmpty(filterQuery)) {
            return;
        }

        boolean isSearchUser = false;
        if (filterQuery.startsWith("user:") || filterQuery.startsWith("u:")) {
            isSearchUser = true;
            filterQuery = filterQuery.replace("user:", "").replace("u:", "").trim();
        }

        final boolean finalIsSearchUser = isSearchUser;
        final String finalQuery = filterQuery;

        List<Session> allSessions = getSessions();
        if (allSessions == null || allSessions.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Sessions list is empty.");
        } else {
            sessions = allSessions.stream().filter(session -> {
                if (finalIsSearchUser) {
                    return StringUtils.containsIgnoreCase(session.getUsername(), finalQuery);
                }

                return session.getQueries().stream().anyMatch(query -> StringUtils.containsIgnoreCase(query.getQuery(), finalQuery));

            }).collect(Collectors.toList());
        }
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(final String searchQuery) {
        this.searchQuery = searchQuery;
    }
}

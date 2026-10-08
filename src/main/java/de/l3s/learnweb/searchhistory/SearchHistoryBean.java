package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

@Named
@ViewScoped
public class SearchHistoryBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -7682314831788865416L;

    private int selectedUserId;
    private int selectedGroupId;

    private String searchQuery;
    private boolean showGroupHistory;
    private SearchHistoryQuery selectedQuery;

    private transient List<SearchSession> sessions;
    private transient Map<Integer, List<ResourceDecorator>> snippets; // by search id

    @Inject
    private UserDao userDao;

    @Inject
    private GroupDao groupDao;

    @Inject
    private SearchHistoryDao searchHistoryDao;

    /**
     * Load the variables that needs values before the view is rendered.
     */
    public void onLoad() {
        getCurrentUser(); // fails early, if the user can't view the history
    }

    public SearchHistoryQuery getSelectedQuery() {
        return selectedQuery;
    }

    public void setSelectedQuery(final SearchHistoryQuery selectedQuery) {
        this.selectedQuery = selectedQuery;
    }

    public List<SearchSession> getSessions() {
        if (sessions == null) {
            // "user:" or "u:" filters by the username, otherwise by the queries
            String filter = StringUtils.trimToNull(searchQuery);
            String username = null;
            if (filter != null && (filter.startsWith("user:") || filter.startsWith("u:"))) {
                username = StringUtils.trimToNull(StringUtils.substringAfter(filter, ":"));
                filter = null;
            }

            if (showGroupHistory && selectedGroupId > 0) { // -1 if no group is selected
                BeanAssert.hasPermission(groupDao.findByIdOrElseThrow(selectedGroupId).canViewSearchHistory(getUser()));
                sessions = searchHistoryDao.findSessionsByGroupId(selectedGroupId, filter, username);
            } else if (!showGroupHistory) {
                sessions = searchHistoryDao.findSessionsByUserId(getCurrentUser().getId(), filter, username);
            }
        }

        return sessions;
    }

    public String getSearchResultsView() {
        if (selectedQuery.mode() == SearchMode.text) {
            return "list";
        } else if (selectedQuery.mode() == SearchMode.image) {
            return "float";
        } else if (selectedQuery.mode() == SearchMode.video) {
            return "grid";
        }

        return null;
    }

    public List<ResourceDecorator> getSearchResults() {
        List<ResourceDecorator> searchResults = new ArrayList<>();
        if (selectedQuery != null) {
            if (snippets == null) { // also after deserialization
                snippets = new HashMap<>();
            }
            searchResults.addAll(snippets.computeIfAbsent(selectedQuery.searchId(), searchId -> searchHistoryDao.findSearchResultsByQuery(selectedQuery, 100)));
        }
        return searchResults;
    }

    public void actionSetShowGroupHistory() {
        showGroupHistory = true;
        reset();
    }

    public void actionSetShowUserHistory() {
        showGroupHistory = false;
        selectedGroupId = -1;
        reset();
    }

    public boolean isShowGroupHistory() {
        return showGroupHistory;
    }

    /**
     * The user whose history is shown, checked on each call, because the view parameters are set again on a postback.
     */
    public User getCurrentUser() {
        BeanAssert.authorized(isLoggedIn());

        if (selectedUserId == 0 || selectedUserId == getUser().getId()) {
            return getUser();
        }

        User user = userDao.findByIdOrElseThrow(selectedUserId);
        BeanAssert.hasPermission(getUser().canModerateUser(user));
        return user;
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
            reset();
        }
        this.selectedGroupId = selectedGroupId;
    }

    public void search() {
        sessions = null; // reloaded with the current filter
    }

    public void reset() {
        sessions = null;
        searchQuery = null;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(final String searchQuery) {
        this.searchQuery = searchQuery;
    }
}

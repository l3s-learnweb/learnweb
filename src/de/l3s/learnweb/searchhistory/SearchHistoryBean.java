package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

@Named
@ViewScoped
public class SearchHistoryBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -7682314831788865416L;
    //private static final Logger log = LogManager.getLogger(SearchHistoryBean.class);

    private int selectedUserId;
    private int selectedGroupId;

    private String searchQuery;
    private boolean showGroupHistory;
    private SearchQuery selectedQuery;

    private transient List<SearchSession> sessions;
    private transient Map<Integer, List<ResourceDecorator>> snippets = new HashMap<>();

    @Inject
    private UserDao userDao;

    @Inject
    private SearchHistoryDao searchHistoryDao;

    /**
     * Load the variables that needs values before the view is rendered.
     */
    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        if (selectedUserId == 0) {
            selectedUserId = getUser().getId();
        }
    }

    public SearchQuery getSelectedQuery() {
        return selectedQuery;
    }

    public void setSelectedQuery(final SearchQuery selectedQuery) {
        this.selectedQuery = selectedQuery;
    }

    public List<SearchSession> getSessions() {
        if (sessions == null && showGroupHistory && selectedGroupId != 0) {
            sessions = searchHistoryDao.findSessionsByGroupId(selectedGroupId);
        } else if (sessions == null && !showGroupHistory) {
            sessions = searchHistoryDao.findSessionsByUserId(selectedUserId);
        }

        return sessions;
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
                snippets.put(selectedQuery.getSearchId(), dao().getSearchHistoryDao().findSearchResultsByQuery(selectedQuery, 100));
            }

            searchResults.addAll(snippets.get(selectedQuery.getSearchId()));
        }
        return searchResults;
    }

    public void onChangeGroup(AjaxBehaviorEvent event) {
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

    public User getCurrentUser() {
        return userDao.findById(selectedUserId).orElse(getUser());
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

    public void search() {
        sessions = null;
        filterSessionsByQuery(searchQuery);
    }

    public void reset() {
        sessions = null;
        searchQuery = null;
    }

    private void filterSessionsByQuery(String filterQuery) {
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

        List<SearchSession> allSessions = getSessions();
        if (allSessions == null || allSessions.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Sessions list is empty.");
        } else {
            sessions = allSessions.stream().filter(session -> {
                if (finalIsSearchUser) {
                    return StringUtils.containsIgnoreCase(session.getUser().getUsername(), finalQuery);
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

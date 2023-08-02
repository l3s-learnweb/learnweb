package de.l3s.learnweb.searchhistory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.searchhistory.Graph.CollabGraph;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

@Named
@ViewScoped
public class SearchHistoryBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -7682314831788865416L;
    //private static final Logger log = LogManager.getLogger(SearchHistoryBean.class);

    private int selectedUserId;
    private int selectedGroupId;

    private String searchQuery;
    private boolean showGroupHistory;
    private SearchQuery selectedQuery;

    private transient List<SearchSession> sessions;
    private final transient Map<Integer, List<ResourceDecorator>> snippets = new HashMap<>();

    @Inject
    private UserDao userDao;
    @Inject
    private GroupDao groupDao;
    @Inject
    private SearchHistoryDao searchHistoryDao;

    private static final String patternDate = "yyyy-MM-dd";
    private static final String patternTime = "HH:mm:ss";
    private static final String patternDateTime = String.format("%s %s", patternDate, patternTime);
    private transient List<JsonSharedObject> sharedObjects = new ArrayList<>();
    private transient Gson gson;

    /**
     * Load the variables that needs values before the view is rendered.
     */
    public void onLoad() throws IOException, InterruptedException {
        BeanAssert.authorized(isLoggedIn());
        if (selectedUserId == 0) {
            selectedUserId = getUser().getId();
        }
        gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe()).create();
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
        if ("text".equals(selectedQuery.mode())) {
            return "list";
        } else if ("image".equals(selectedQuery.mode())) {
            return "float";
        } else if ("video".equals(selectedQuery.mode())) {
            return "grid";
        }
        return null;
    }

    public List<ResourceDecorator> getSearchResults() {
        List<ResourceDecorator> searchResults = new ArrayList<>();
        if (selectedQuery != null) {
            if (!snippets.containsKey(selectedQuery.searchId())) {
                snippets.put(selectedQuery.searchId(), dao().getSearchHistoryDao().findSearchResultsByQuery(selectedQuery, 100));
            }
            searchResults.addAll(snippets.get(selectedQuery.searchId()));
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
        calculateEntities();
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

                return session.getQueries().stream().anyMatch(query -> StringUtils.containsIgnoreCase(query.query(), finalQuery));

            }).collect(Collectors.toList());
        }
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(final String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        DateTimeFormatter format = DateTimeFormatter.ofPattern(patternDateTime);

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value != null) {
                out.value(value.format(format));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            return LocalDateTime.parse(in.nextString(), format);
        }
    }

    /**
    * Calculates and returns a list of top entries for each user belonging to the group.
    * Also exports a rdf turtle file for every user in the group
    * */
    private void calculateEntities() {
        //For testing only
        // userPkg.createSharedObject(getUser(), selectedGroupId, 5, true, "negative5SharedObject");
        // userPkg.createSharedObject(getUser(), selectedGroupId, 10, false, "positive10SharedObject");

        sharedObjects = new ArrayList<>();
        for (User user : userDao.findByGroupId(selectedGroupId)) {
            PKGraph userPkg = PKGraph.createPkg(user);
            JsonSharedObject object = userPkg.createSharedObject(user, selectedGroupId, 3, false, "collabGraph");

            if (object != null) {
                sharedObjects.add(object);
            }
        }
    }

    /**
    * Create the CollabGraph file, export it to visualisation.
    * @return the Json string of the collabGraph
    * */
    public String getQueriesJson() {
        if (sessions == null || sessions.isEmpty() || selectedGroupId <= 0) {
            return null;
        }
        //Get the CollabGraph
        CollabGraph calculatedQuery = new CollabGraph(new ArrayList<>(), new ArrayList<>()).createCollabGraph(sharedObjects);
        //Export file
        return gson.toJson(calculatedQuery);
    }

    public String getSingleQueryJson() {
        PKGraph userPkg = getUserBean().getUserPkg();
        if (getCurrentUser() != getUser()) {
            userPkg = PKGraph.createPkg(getCurrentUser());
        }

        JsonSharedObject obj = userPkg.createSingleGraph();
        if (obj == null) {
            return "";
        }
        CollabGraph calculatedQuery = new CollabGraph(new ArrayList<>(), new ArrayList<>()).createSingleGraph(obj);
        return gson.toJson(calculatedQuery);
    }
}

package de.l3s.learnweb.searchhistory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
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
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.ResourceDecorator;
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

    private static String PATTERN_DATE = "yyyy-MM-dd";
    private static String PATTERN_TIME = "HH:mm:ss";
    private static String PATTERN_DATETIME = String.format("%s %s", PATTERN_DATE, PATTERN_TIME);

    /**
     * Load the variables that needs values before the view is rendered.
     */
    public void onLoad() throws Exception {
        BeanAssert.authorized(isLoggedIn());
        if (selectedUserId == 0) {
            selectedUserId = getUser().getId();
        }
        for (Group group : groupDao.findByUserId(selectedUserId)) {
            JsonQuery.processQuery(searchHistoryDao.findSessionsByGroupId(group.getId()), searchHistoryDao, group.getId(), userDao, groupDao);
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

    public User getCurrentUser() throws Exception {
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

    static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        DateTimeFormatter format = DateTimeFormatter.ofPattern(PATTERN_DATETIME);

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if(value != null)
                out.value(value.format(format));
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            return LocalDateTime.parse(in.nextString(), format);
        }
    }

    public String getQueriesJson() throws Exception {
        if (sessions == null || sessions.isEmpty() || selectedGroupId <= 0) return null;
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
            .create();
        //Create new filePath
        String localPath = System.getProperty("user.dir") + "\\" + groupDao.findById(selectedGroupId).get().getTitle()
            + "_Summary_" + userDao.findById(selectedUserId).get().getUsername() + "_" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE) + ".json";
        String collabPath = System.getProperty("user.dir") + "\\" + groupDao.findById(selectedGroupId).get().getTitle()
            + "_Summary_" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE) + ".json";
        File file = new File(localPath);
        //Calculates and returns a list of top entries for each user belonging to the group
        //Also exports a rdf turtle file for every user in the group
        List<JsonSharedObject> sharedObjects = new JsonQuery(new ArrayList<>(),
            new ArrayList<>()).calculateTopEntries(searchHistoryDao.findAllAnnotationCounts(),
            userDao.findByGroupId(selectedGroupId), groupDao.findById(selectedGroupId).get(), sessions, 3);
        //Results of SharedObjects to DB
        for (JsonSharedObject sharedObject : sharedObjects) {
            if (searchHistoryDao.findObjectByGroupId(selectedGroupId, sharedObject.getUser().getId()).isEmpty()) {
                searchHistoryDao.insertSharedObject(sharedObject.getUser().getId(), selectedGroupId, "sharedObject",
                    gson.toJson(sharedObject));
            }
            else {
                searchHistoryDao.updateSharedObject(gson.toJson(sharedObject), LocalDateTime.now(), sharedObject.getUser().getId(),selectedGroupId);
            }
        }
        //Create new json

        JsonQuery calculatedQuery = new JsonQuery(new ArrayList<>(),
            new ArrayList<>()).createCollabGraph(sharedObjects);
        if (file.createNewFile()) {
                Writer writer = new FileWriter(localPath, StandardCharsets.UTF_8);
                if (sharedObjects.stream().anyMatch(s -> s.getUser().getId() == selectedUserId))
                    gson.toJson(sharedObjects.stream().filter(s -> s.getUser().getId() == selectedUserId).findFirst().get() , writer);
                writer.close();
        }
        file = new File(collabPath);
        if (file.createNewFile()) {
            Writer writer = new FileWriter(collabPath, StandardCharsets.UTF_8);
            gson.toJson(calculatedQuery , writer);
            writer.close();
        }

        return gson.toJson(calculatedQuery);
    }
}

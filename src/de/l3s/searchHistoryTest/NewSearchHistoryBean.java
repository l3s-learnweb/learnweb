package de.l3s.searchHistoryTest;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedProperty;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import de.l3s.searchHistoryTest.SearchHistoryManager.Edge;
import de.l3s.searchHistoryTest.SearchHistoryManager.Query;
import de.l3s.searchHistoryTest.SearchHistoryManager.SearchResult;
import de.l3s.searchHistoryTest.SearchHistoryManager.Session;

@Named
@ViewScoped
public class NewSearchHistoryBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(NewSearchHistoryBean.class);
    private static final long serialVersionUID = -7682314831788865416L;

    private int userId;

    private String searchQuery = "";

    // for graph
    private List<String> queries;
    private Set<String> entities;

    private List<Session> sessions;
    private boolean showGroupHistory = false;

    private int selectedUserId;
    private int selectedGroupId;
    private String selectedSessionId;
    private String selectedEntity;

    private Map<Integer, List<SearchResult>> searchIdSnippets;
    private List<Integer> selectedSearchIds;
    private Map<Integer, String> searchIdQueryMap;

    /**
     * Load the variables that needs values before the view is rendered
     */
    public void preRenderView()
    {
        if(getUser() == null)
            return;

        if(isAjaxRequest())
            return;

        if(userId == 0)
        {
            userId = getUser().getId();
            selectedUserId = userId;
        }
    }

    public NewSearchHistoryBean()
    {
        queries = new ArrayList<>();
        entities = new HashSet<>();
        searchIdSnippets = new HashMap<>();
        selectedSearchIds = new ArrayList<>();
        searchIdQueryMap = new HashMap<>();
    }

    /**
     * Get all queries in a selected session.
     *
     * @return
     */
    public List<String> getQueries()
    {
        return queries;
    }

    public String getQueriesAsJson()
    {
        List<Query> queries = null;
        if(!showGroupHistory)
            queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromCache(selectedUserId, selectedSessionId);
        else
            queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromGroupCache(selectedGroupId, selectedSessionId);

        //log.info(queries.size() + " queries got for user (" + selectedUserId + ") and session id (" + selectedSessionId + ")");
        entities.clear();

        JSONArray queriesArr = new JSONArray();
        for(Query q : queries)
        {
            entities.add(q.getQuery());
            try
            {
                JSONObject queryObj = new JSONObject();
                queryObj.put("search_id", q.getSearchId());
                queryObj.put("query", q.getQuery());
                searchIdQueryMap.put(q.getSearchId(), q.getQuery());

                List<Entity> relatedEntities = getLearnweb().getSearchHistoryManager().getRelatedEntitiesForSearchId(q.getSearchId());
                JSONArray relatedEntitiesArr = new JSONArray();
                for(Entity entity : relatedEntities)
                {
                    entities.add(entity.getEntityName());
                    JSONObject entityObj = new JSONObject();
                    entityObj.put("entity_name", entity.getEntityName());
                    entityObj.put("score", entity.getScore());

                    JSONArray ranks = new JSONArray(entity.getRanks());
                    entityObj.put("ranks", ranks);
                    relatedEntitiesArr.put(entityObj);
                }

                queryObj.put("related_entities", relatedEntitiesArr);
                queriesArr.put(queryObj);
            }
            catch(JSONException e)
            {
                log.error("Error while creating json object for query: " + q.getSearchId(), e);
            }
        }
        return queriesArr.toString();
    }

    public String getEdgesAsJson()
    {
        Set<Edge> edges = getLearnweb().getSearchHistoryManager().getAllEdges(entities);
        JSONArray edgesArr = new JSONArray();
        for(Edge edge : edges)
        {
            try
            {
                JSONObject edgeObj = new JSONObject();
                edgeObj.put("source", edge.getSource());
                edgeObj.put("target", edge.getTarget());
                edgeObj.put("score", edge.getScore());
                edgesArr.put(edgeObj);
            }
            catch(JSONException e)
            {
                log.error("Error while creating json object for edge: " + edge, e);
            }
        }
        return edgesArr.toString();
    }

    /**
     * Get all entities in a selected session.
     *
     * @return
     */
    public Set<String> getEntities()
    {
        return entities;
    }

    public String getSelectedEntity()
    {
        return selectedEntity;
    }

    public String getQuery(int searchId)
    {
        return searchIdQueryMap.getOrDefault(searchId, "");
    }

    public List<Session> getSessions() throws SQLException
    {
        if(this.showGroupHistory)
        {
            if (sessions == null && selectedGroupId > 0) {
                if(!SessionCache.Instance().existsGroupId(selectedGroupId))
                {
                    SessionCache.Instance().cacheByGroupId(selectedGroupId, getLearnweb().getSearchHistoryManager().getSessionsForGroupId(selectedGroupId));
                }
                sessions = SessionCache.Instance().getByGroupId(selectedGroupId);
            }

            return sessions;
        }
        else
        {
            if(sessions == null)
            {
                sessions = getLearnweb().getSearchHistoryManager().getSessionsForUser(userId);
            }

            return sessions;
        }
    }

    public List<Session> getGroupSessions()
    {
        return null;
    }

    public String getSelectedSessionId()
    {
        return selectedSessionId;
    }

    public List<SearchResult> getSearchResults()
    {
        List<SearchResult> searchResults = new ArrayList<>();
        for(int searchId : selectedSearchIds)
        {
            if(!searchIdSnippets.containsKey(searchId))
                searchIdSnippets.put(searchId, getLearnweb().getSearchHistoryManager().getSearchResultsForSearchId(searchId, 10));

            searchResults.addAll(searchIdSnippets.get(searchId));
        }
        return searchResults;
    }

    public void actionUpdateKGData()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String sessionId = params.get("session-id");
        int userId = Integer.parseInt(params.get("user-id"));
        selectedSessionId = sessionId;
        selectedUserId = userId;
        //log.info("session id: " + sessionId + "user id: " + userId);
    }

    public void actionSelectedGroupId()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        int groupId = Integer.parseInt(params.get("group-id"));
        selectedGroupId = groupId;
        log.info("selected group id: " + groupId);
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

    public void actionSelectedSearchId()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        int searchId = Integer.parseInt(params.get("search-id"));
        selectedEntity = params.get("entity-name");
        selectedSearchIds.clear();
        selectedSearchIds.add(searchId);
    }

    public void actionSelectedSearchIds()
    {
        selectedSearchIds.clear();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String searchIdsStr = params.get("search-ids");
        selectedEntity = params.get("entity-name");
        String[] searchIds = searchIdsStr.split(",");
        for(String searchId : searchIds)
            selectedSearchIds.add(Integer.parseInt(searchId));
    }

    public boolean isShowGroupHistory()
    {
        return showGroupHistory;
    }

    public int getUserId()
    {
        return userId;
    }

    public User getCurrentUser()
    {
        User user = null;
        try
        {
            user = getLearnweb().getUserManager().getUser(userId);
        }
        catch(SQLException e)
        {
            log.error(e);
        }
        return user == null ? getUser() : user;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public int getSelectedGroupId()
    {
        return selectedGroupId;
    }

    public void setSelectedGroupId(int selectedGroupId)
    {
        if (selectedGroupId != this.selectedGroupId) {
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

    private void filterSessionsByQuery(String filterQuery) throws SQLException
    {
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
        sessions = getSessions().stream().filter(session -> {
            if (finalIsSearchUser) {
                return StringUtils.containsIgnoreCase(session.getUserName(), finalQuery);
            }

            return session.getQueries().stream().anyMatch(query -> StringUtils.containsIgnoreCase(query.getQuery(), finalQuery));

        }).collect(Collectors.toList());
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

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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.searchHistoryTest.SearchHistoryManager.Edge;
import de.l3s.searchHistoryTest.SearchHistoryManager.Query;
import de.l3s.searchHistoryTest.SearchHistoryManager.SearchResult;
import de.l3s.searchHistoryTest.SearchHistoryManager.Session;

@ManagedBean
@ViewScoped
public class NewSearchHistoryBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(NewSearchHistoryBean.class);
    private static final long serialVersionUID = -7682314831788865416L;

    private List<String> queries;
    private Set<String> entities;

    private List<Session> sessions;
    private String title;
    private int userId;
    private int selectedUserId;
    private int selectedGroupId;
    private boolean groupIdSelected;
    private List<Session> groupSessions;
    private String selectedSessionId;
    private String selectedEntity;
    private DateFormat dateFormatter;
    private DateFormat timeFormatter;
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

        groupIdSelected = false;
    }

    public NewSearchHistoryBean()
    {
        title = "Search History";
        queries = new ArrayList<String>();
        entities = new HashSet<String>();
        searchIdSnippets = new HashMap<Integer, List<SearchResult>>();
        selectedSearchIds = new ArrayList<Integer>();
        searchIdQueryMap = new HashMap<Integer, String>();
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
        if(!groupIdSelected)
            queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromCache(selectedUserId, selectedSessionId);
        else
            queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromGroupCache(selectedGroupId, selectedSessionId);

        System.out.println(queries.size() + " queries got for user (" + selectedUserId + ") and session id (" + selectedSessionId + ")");
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

    public String getTitle()
    {
        return title;
    }

    public String getSelectedEntity()
    {
        return selectedEntity;
    }

    public String getQuery(int searchId)
    {
        return searchIdQueryMap.getOrDefault(searchId, "");
    }

    public List<Session> getSessions()
    {
        if(this.groupIdSelected)
        {
            if(groupSessions != null)
            {
                System.out.println("# group sessions: " + groupSessions.size());
                for(Session session : groupSessions)
                {
                    for(Query query : session.getQueries())
                    {
                        System.out.print(query.getQuery() + " -> ");
                    }
                    System.out.println();
                }
            }
            return groupSessions;
        }
        else
        {
            if(sessions == null)
            {
                try
                {
                    System.out.println("user id: " + userId);
                    sessions = getLearnweb().getSearchHistoryManager().getSessionsForUser(userId);
                }
                catch(SQLException e)
                {
                    log.error("Error while fetching list of sessions for particular user: " + userId, e);
                }
            }

            return sessions;
        }
    }

    public List<Session> getGroupSessions()
    {
        System.out.println("why call me...");
        return null;
    }

    public String getSelectedSessionId()
    {
        return selectedSessionId;
    }

    public List<SearchResult> getSearchResults()
    {
        List<SearchResult> searchResults = new ArrayList<SearchResult>();
        for(int searchId : selectedSearchIds)
        {
            if(!searchIdSnippets.containsKey(searchId))
                searchIdSnippets.put(searchId, getLearnweb().getSearchHistoryManager().getSearchResultsForSearchId(searchId, 10));

            searchResults.addAll(searchIdSnippets.get(searchId));
        }
        return searchResults;
    }

    public void actionUpdateKGData() throws Exception
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String sessionId = params.get("session-id");
        int userId = Integer.parseInt(params.get("user-id"));
        selectedSessionId = sessionId;
        selectedUserId = userId;
        log.info("session id: " + sessionId + "user id: " + userId);
    }

    public void actionSelectedGroupId() throws Exception
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        int groupId = Integer.parseInt(params.get("group-id"));
        selectedGroupId = groupId;
        log.info("group id: " + groupId);

        if(SessionCache.Instance().existsGroupId(selectedGroupId) == false)
        {
            SessionCache.Instance().cacheByGroupId(groupId, getLearnweb().getSearchHistoryManager().getSessionsForGroupId(groupId));
        }
        this.groupSessions = SessionCache.Instance().getByGroupId(groupId);

        groupIdSelected = true;
    }

    public void onChangeGroup(AjaxBehaviorEvent event) throws Exception
    {
        log.info("group id: " + selectedGroupId);

        if(!SessionCache.Instance().existsGroupId(selectedGroupId))
        {
            SessionCache.Instance().cacheByGroupId(selectedGroupId, getLearnweb().getSearchHistoryManager().getSessionsForGroupId(selectedGroupId));
        }
        this.groupSessions = SessionCache.Instance().getByGroupId(selectedGroupId);

        groupIdSelected = true;
    }

    public void actionSetGroupUnselected()
    {
        selectedUserId = getUserId();
        groupIdSelected = false;
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

    public String formatDate(Date date, Locale locale)
    {
        if(dateFormatter == null)
            dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
        return dateFormatter.format(date);
    }

    public String formatTime(Date date, Locale locale)
    {
        if(timeFormatter == null)
            timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
        return timeFormatter.format(date);
    }

    public boolean getGroupIdSelected()
    {
        return groupIdSelected;
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
        System.out.println("selected group id: " + selectedGroupId);
        this.selectedGroupId = selectedGroupId;
    }

}

/*
public void actionUpdateKGData()
{
    Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String sessionId = params.get("session-id");
    selectedSessionId = sessionId;
    System.out.println(sessionId);

    // update queries
    if(this.queries == null)
    {
        this.queries = new ArrayList<String>();
    }
    this.queries.clear();
    SearchHistoryManager manager = this.getLearnweb().getSearchHistoryManager();
    List<Query> queryList = manager.getQueriesForSessionId(this.selectedSessionId);
    for(Query query : queryList)
    {
        this.queries.add(query.getQuery());
    }
    System.out.println("get queries:" + this.queries.size() + ", session id: " + this.selectedSessionId);

    // update entities
    if(this.entities == null)
    {
        this.entities = new ArrayList<String>();
    }
    this.entities.clear();
    Set<String> entitySet = manager.getMergedEntities(queryList);
    for(String entity : entitySet)
    {
        this.entities.add(entity);
    }
    System.out.println("get entities:" + this.entities.size() + ", session id: " + this.selectedSessionId);

    // update related
    if(this.related == null)
    {
        this.related = new ArrayList<>();
    }
    this.related.clear();
    for(Query query : queryList)
    {
        List<String> relatedEntityStrs = new ArrayList<>();
        List<String> relatedEntityList = manager.getRelatedEntitiesForSearchId(query.getSearchId());
        for(String entity : relatedEntityList)
        {
            relatedEntityStrs.add("\"" + entity + "\"");
        }
        this.related.add(relatedEntityStrs);
    }
    System.out.println("get related:" + this.related.size() + ", session id: " + this.selectedSessionId);

    // update edges
    if(this.edges == null)
    {
        this.edges = new ArrayList<>();
    }
    this.edges.clear();

    Set<Edge> edgeSet = manager.getAllEdges(entitySet);
    for(Edge edge : edgeSet)
    {
        List<String> edgeNodes = new ArrayList<>();
        edgeNodes.add("\"" + edge.getSource() + "\"");
        edgeNodes.add("\"" + edge.getTarget() + "\"");
        this.edges.add(edgeNodes);
    }

    System.out.println("get edges:" + this.edges.size() + ", session id: " + this.selectedSessionId);

}
*/

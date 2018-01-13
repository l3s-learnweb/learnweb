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

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    @Deprecated
    private List<Entity> entityList;
    //private List<Integer> ranks;
    // related entities of a query
    private List<List<String>> related;
    private List<List<String>> edges;
    private List<Session> sessions;
    private String title;
    private int userId;
    private String selectedSessionId;
    private DateFormat dateFormatter;
    private DateFormat timeFormatter;
    private Map<Integer, List<SearchResult>> searchIdSnippets;

    private String selectedEntity;
    private List<Integer> selectedSearchIds;

    /**
     * Load the variables that needs values before the view is rendered
     */
    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(getUser() != null)
            userId = getUser().getId();
    }

    public NewSearchHistoryBean()
    {
        log.info("Initializing Search History Bean");
        title = "Search History";
        queries = new ArrayList<String>();
        entities = new HashSet<String>();
        entityList = new ArrayList<Entity>();
        searchIdSnippets = new HashMap<Integer, List<SearchResult>>();
        selectedSearchIds = new ArrayList<Integer>();
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
        List<Query> queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionFromCache(userId, selectedSessionId);
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

    public List<Entity> getEntityList()
    {
        return entityList;
    }

    /**
     * Get snippet ranks for a selected entity
     * 
     * @return
     * @throws Exception
     */
    @Deprecated
    public List<Integer> getRanks() throws Exception
    {
        List<Integer> ranks = new ArrayList<>();
        List<Entity> entityList = this.getEntityList();
        String entityName = this.getSeletedEntity();
        ranks = getLearnweb().getSearchHistoryManager().getRanksForEntity(entityList, entityName);
        //System.out.println("ranks: " + ranks);
        log.info("get ranks:" + ranks.size());
        return ranks;
    }

    /**
     * Get related entities for each query in a selected session.
     * 
     * @return
     */
    @Deprecated
    public List<List<String>> getRelated()
    {
        return this.related;
    }

    /**
     * Get edges in a selected session.
     * 
     * @return
     */
    @Deprecated
    public List<List<String>> getEdges()
    {
        return this.edges;
    }

    public String getTitle()
    {
        return title;
    }

    public List<Session> getSessions()
    {
        if(sessions == null)
        {
            try
            {
                sessions = getLearnweb().getSearchHistoryManager().getSessionsForUser(userId);
            }
            catch(SQLException e)
            {
                log.error("Error while fetching list of sessions for particular user: " + userId, e);
            }
        }

        return sessions;
    }

    public String getSelectedSessionId()
    {
        return selectedSessionId;
    }

    @Deprecated
    public String getSeletedEntity()
    {
        return selectedEntity;
    }

    @Deprecated
    public List<SearchResult> getSearchResults(int searchId)
    {
        if(!searchIdSnippets.containsKey(searchId))
            searchIdSnippets.put(searchId, getLearnweb().getSearchHistoryManager().getSearchResultsForSearchId(searchId, 10));
        return searchIdSnippets.get(searchId);
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
        selectedSessionId = sessionId;
        log.info("session id: " + sessionId);

        /*
        // update queries
        this.queries.clear();
        SearchHistoryManager manager = getLearnweb().getSearchHistoryManager();
        List<Query> queryList = manager.getQueriesForSessionFromCache(userId, selectedSessionId);
        for(Query query : queryList)
        {
            queries.add(query.getQuery());
            if(!searchIdSnippets.containsKey(query.getSearchId()))
                searchIdSnippets.put(query.getSearchId(), manager.getSearchResultsForSearchId(query.getSearchId(), 10));
        }
        log.info("get queries:" + queries.size());
        
        // update entities
        this.entities.clear();
        this.entities.addAll(manager.getMergedEntitiesForSession(queryList));
        log.info("get entities:" + this.entities.size());
        
        //update entityList
        this.entityList.clear();
        this.entityList = manager.getMergedEntitiesInEntitiyForSession(queryList);
        log.info("get entityList: " + this.entityList.size());
        
        // update related
        if(this.related == null)
        {
            this.related = new ArrayList<>();
        }
        this.related.clear();
        
        List<List<String>> relatedEntitiesList = manager.getRelatedEntitiesForQueries(queryList);
        
        for(List<String> relatedEntities : relatedEntitiesList)
        {
            List<String> relatedEntityStrs = new ArrayList<>();
            relatedEntityStrs.addAll(relatedEntities);
            this.related.add(relatedEntityStrs);
        }
        
        log.info("get related:" + this.related.size());
        
        // update edges
        if(this.edges == null)
        {
            this.edges = new ArrayList<>();
        }
        this.edges.clear();
        
        Set<Edge> edgeSet = manager.getAllEdges(entities);
        for(Edge edge : edgeSet)
        {
            List<String> edgeNodes = new ArrayList<>();
            edgeNodes.add(edge.getSource());
            edgeNodes.add(edge.getTarget());
            this.edges.add(edgeNodes);
        }
        
        log.info("get edges:" + this.edges.size());
        */
    }

    public void actionSelectedSearchId()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        int searchId = Integer.parseInt(params.get("search-id"));
        selectedSearchIds.clear();
        selectedSearchIds.add(searchId);
    }

    @Deprecated
    public void actionSelectedEntity()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String entityName = params.get("entity-name");
        selectedEntity = entityName;
    }

    public void actionSelectedSearchIds()
    {
        selectedSearchIds.clear();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String searchIdsStr = params.get("search-ids");
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

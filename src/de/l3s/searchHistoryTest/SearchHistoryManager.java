package de.l3s.searchHistoryTest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

public class SearchHistoryManager
{

    private final static Logger log = Logger.getLogger(SearchHistoryManager.class);
    private final Learnweb learnweb;

    public SearchHistoryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    /**
     * retired function which fetches data from database.
     * 
     * @param sessionId
     * @return
     */
    public List<Query> getQueriesForSessionId(String sessionId)
    {
        List<Query> queries = new ArrayList<>();

        try
        {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                    "SELECT t1.search_id, t1.query FROM learnweb_large.sl_query t1 join learnweb_main.lw_user_log t2 ON (t1.search_id=t2.target_id AND t1.user_id=t2.user_id AND t1.query=t2.params)  WHERE t2.action = 5 AND t1.mode='text' AND t1.language='en' AND t2.session_id=? AND t1.user_id != 0  ORDER BY t1.timestamp ASC");
            pstmt.setString(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                Query query = new Query(rs.getInt("search_id"), rs.getString("query"));
                queries.add(query);
            }
            pstmt.close();
        }
        catch(SQLException e)
        {
            log.error("Error while fetching queries for a specific session: " + sessionId, e);
        }

        return queries;
    }

    /**
     * retired function which fetches data from database.
     * 
     * @param queries
     * @return
     * @throws Exception
     */
    public Set<String> getMergedEntities(List<Query> queries) throws Exception
    {
        Set<String> entities = new HashSet<>();

        for(Query query : queries)
        {
            entities.add(query.getQuery());
            int searchId = query.getSearchId();
            List<Entity> relatedEntities = this.getRelatedEntitiesForSearchId(searchId);
            for(Entity entity : relatedEntities)
            {
                entities.add(entity.getEntityName());
            }
        }

        return entities;
    }
    /*
    @SuppressWarnings("unchecked")
    public List<String> getRelatedEntitiesForSearchId(int searchId)
    {
        List<String> entities = new ArrayList<String>();
        try
        {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement("SELECT related_entities FROM learnweb_large.sl_query_entities WHERE search_id = ?");
            pstmt.setInt(1, searchId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                Object object = Sql.getSerializedObject(rs, "related_entities");
                if(object == null)
                    break;
    
                List<String> related_entities = (List<String>) object;
    
                entities.addAll(related_entities);
            }
            pstmt.close();
        }
        catch(SQLException e)
        {
            log.error("Error while fetching related entities for search id: " + searchId, e);
        }
        return entities;
    }*/
    /*public List<String> getRelatedEntitiesForSearchId(int searchId) throws Exception
    {
        List<String> entities = new ArrayList<String>();
        try
        {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement("SELECT related_entities FROM learnweb_large.sl_query_entities WHERE search_id = ?");
            pstmt.setInt(1, searchId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                Object object = Sql.getSerializedObject(rs, "related_entities");
                if(object == null)
                    break;
                List<String> related_entities = (List<String>) object;
    
                for(String re : related_entities)
                {
                    Entity entity = Entity.fromString(re);
                    entities.add(entity.getEntityName());
                }
            }
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("related entities: " + entities);
        return entities;
    }*/

    public List<SearchResult> getSearchResultsForSearchId(int searchId, int limit)
    {
        List<SearchResult> searchResults = new ArrayList<SearchResult>();
        try
        {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM learnweb_large.sl_resource WHERE search_id = ? ORDER BY rank LIMIT ?");
            pStmt.setInt(1, searchId);
            pStmt.setInt(2, limit);

            ResultSet rs = pStmt.executeQuery();
            while(rs.next())
            {
                SearchResult result = new SearchResult();
                result.setRank(rs.getInt("rank"));
                int resourceId = rs.getInt("resource_id");
                if(resourceId > 0)
                {
                    Resource r = learnweb.getResourceManager().getResource(resourceId);
                    result.setUrl(r.getUrl());
                    result.setTitle(r.getTitle());
                    result.setDescription(r.getDescription());
                }
                else
                {
                    result.setUrl(rs.getString("url"));
                    result.setTitle(rs.getString("title"));
                    result.setDescription(rs.getString("description"));
                }
                searchResults.add(result);
            }
            pStmt.close();
        }
        catch(SQLException e)
        {
            log.error("Error while fetching search results for search id: " + searchId, e);
        }

        return searchResults;
    }

    public List<Entity> getRelatedEntitiesForSearchId(int searchId) throws Exception
    {
        List<Entity> entities = new ArrayList<>();
        try
        {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement("SELECT related_entities FROM learnweb_large.sl_query_entities WHERE search_id = ?");
            pstmt.setInt(1, searchId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                Object object = Sql.getSerializedObject(rs, "related_entities");
                if(object == null)
                    break;
                List<String> related_entities = (List<String>) object;

                for(String re : related_entities)
                {
                    Entity entity = Entity.fromString(re);
                    entities.add(entity);
                }
            }
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //System.out.println("related entities: " + entities);
        return entities;
    }

    public List<Query> getQueriesForSessionFromCache(int userId, String sessionId)
    {
        List<Query> queries = null;

        if(SessionCache.Instance().exists(userId))
        {
            queries = new ArrayList<>();

            List<Session> sessions = SessionCache.Instance().get(userId);

            for(Session session : sessions)
            {
                if(session.getSessionId().equals(sessionId))
                {
                    queries.addAll(session.getQueries());
                }
            }
        }

        return queries;
    }

    public List<List<String>> getRelatedEntitiesForQueries(List<Query> queries) throws Exception
    {
        List<List<String>> entitiesList = new ArrayList<>();

        for(Query query : queries)
        {
            if(query.getRelatedEntities() == null)
            {
                List<Entity> entities = this.getRelatedEntitiesForSearchId(query.searchId);
                List<String> entityStrs = new ArrayList<>();
                for(Entity entity : entities)
                {
                    entityStrs.add(entity.getEntityName());
                }
                query.setRelatedEntities(entityStrs);
            }
            entitiesList.add(query.getRelatedEntities());
        }
        //System.out.println("related entities: " + entitiesList);
        return entitiesList;
    }

    public Set<String> getMergedEntitiesForSession(List<Query> queries) throws Exception
    {
        Set<String> entities = new HashSet<>();

        for(Query query : queries)
        {
            entities.add(query.getQuery());
        }

        List<List<String>> relatedEntitiesList = getRelatedEntitiesForQueries(queries);
        for(List<String> relatedEntities : relatedEntitiesList)
        {
            entities.addAll(relatedEntities);
        }

        return entities;
    }

    public List<Entity> getEntitiesInEntityForQuery(Query query) throws Exception
    {
        List<Entity> entities = new ArrayList<>();
        entities = this.getRelatedEntitiesForSearchId(query.searchId);
        return entities;
    }

    public List<Entity> getMergedEntitiesInEntitiyForSession(List<Query> queries) throws Exception
    {
        List<Entity> entities = new ArrayList<>();
        for(Query query : queries)
        {
            if(query.getRelatedEntities() != null)
            {
                List<Entity> entityList = this.getEntitiesInEntityForQuery(query);
                //List<Entity> entityList = this.getRelatedEntitiesForSearchId(query.searchId);
                entities.addAll(entityList);
            }
        }
        return entities;
    }

    public List<Integer> getRanksForEntity(List<Entity> entities, String entityName)
    {
        List<Integer> ranks = new ArrayList<>();
        for(Entity entity : entities)
        {
            if(entity.getEntityName().equals(entityName))
            {
                ranks = entity.getRanks();
            }
        }
        return ranks;
    }

    public List<Session> getSessionsForUser(int userId) throws SQLException
    {
        List<Session> sessions = new ArrayList<Session>();
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
                "SELECT t2.session_id FROM learnweb_large.`sl_query` t1 join learnweb_main.lw_user_log t2 WHERE t1.search_id=t2.target_id AND t2.action = 5 AND t1.user_id=t2.user_id AND t1.query=t2.params AND t1.mode='text' AND t1.language='en' AND t1.user_id=? GROUP BY t2.session_id ORDER BY t1.timestamp DESC");
        pStmt.setInt(1, userId);
        ResultSet rs = pStmt.executeQuery();

        while(rs.next())
        {
            String sessionId = rs.getString("session_id");
            LinkedList<Query> queries = new LinkedList<Query>();
            PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement(
                    "SELECT t1.search_id, t1.query, t1.timestamp, t1.service FROM learnweb_large.`sl_query` t1 join learnweb_main.lw_user_log t2 WHERE t1.search_id=t2.target_id AND t2.action = 5 AND t1.user_id=t2.user_id AND t1.query=t2.params AND t1.mode='text' AND t1.language='en' AND t2.session_id=? ORDER BY t1.timestamp");
            pStmt2.setString(1, sessionId);
            ResultSet rs2 = pStmt2.executeQuery();
            while(rs2.next())
            {
                Query query = new Query(rs2.getInt("search_id"), rs2.getString("query"), new Date(rs2.getTimestamp("timestamp").getTime()), rs2.getString("service"));
                queries.add(query);
            }
            Session session = new Session(sessionId);
            session.setQueries(queries);
            sessions.add(session);
        }

        SessionCache.Instance().put(userId, sessions);

        return sessions;
    }

    public Set<Edge> getAllEdges(Set<String> entities)
    {
        Set<Edge> edges = new HashSet<>();

        for(String entity : entities)
        {
            Set<Edge> edgesForEachEntity = this.getEdgesForEachEntity(entity);
            for(Edge edge : edgesForEachEntity)
            {
                if(entities.contains(edge.getSource()) && entities.contains(edge.target))
                {
                    edges.add(edge);
                }
            }
        }

        return edges;
    }

    private Set<Edge> getEdgesForEachEntity(String entity)
    {
        Set<Edge> edges = new HashSet<>();

        try
        {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement("SELECT * FROM learnweb_large.sl_entity_co_occur WHERE score > 0.05 AND source = ?");
            pstmt.setString(1, entity);
            ResultSet rs = pstmt.executeQuery();
            //int i = 0;
            while(rs.next())
            {
                //i++;
                try
                {
                    edges.add(new Edge(rs.getString("source"), rs.getString("target"), rs.getDouble("score")));
                }
                catch(InvalidEdgeException e)
                {
                    log.error("Invalid edge fetched for " + entity, e);
                }

            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching edges for entity: " + entity, e);
        }

        return edges;
    }

    /*
    public static void main(String[] args) throws ClassNotFoundException, SQLException
    {
        SearchHistoryManager searchHistoryManager = Learnweb.createInstance("").getSearchHistoryManager();
        long start = System.currentTimeMillis();
        searchHistoryManager.getSessionsForUser(10683);
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    
        System.exit(0);
    }*/
    public static void main(String[] args) throws ClassNotFoundException, SQLException
    {
        SearchHistoryManager manager = Learnweb.createInstance("").getSearchHistoryManager();
        try
        {
            List<Query> queries = manager.getSessionsForUser(10683).get(0).getQueries();
            List<Entity> entities = manager.getMergedEntitiesInEntitiyForSession(queries);

        }
        catch(Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public class Session
    {
        private String sessionId;
        private LinkedList<Query> queries;

        public Session(String sessionId)
        {
            this.sessionId = sessionId;
        }

        public String getSessionId()
        {
            return this.sessionId;
        }

        public void setQueries(LinkedList<Query> queries)
        {
            this.queries = queries;
        }

        public List<Query> getQueries()
        {
            return this.queries;
        }

        public Date getStartTimestamp()
        {
            return queries.getFirst().getTimestamp();
        }

        public Date getEndTimestamp()
        {
            return queries.getLast().getTimestamp();
        }

    }

    public class Query
    {
        private int searchId;
        private String query;
        private Date timestamp;
        private String service;
        private List<String> relatedEntities;
        //private List<Entity> relatedEntitiesInEntity;

        public Query(int searchId, String query)
        {
            this.searchId = searchId;
            this.query = query;
        }

        public Query(int searchId, String query, Date timestamp, String service)
        {
            this.searchId = searchId;
            this.query = query;
            this.timestamp = timestamp;
            this.service = service;
        }

        public int getSearchId()
        {
            return this.searchId;
        }

        public String getQuery()
        {
            return this.query;
        }

        public Date getTimestamp()
        {
            return this.timestamp;
        }

        public String getService()
        {
            return this.service;
        }

        public List<String> getRelatedEntities()
        {
            return this.relatedEntities;
        }

        public void setRelatedEntities(List<String> relatedEntities)
        {
            this.relatedEntities = relatedEntities;
        }

        //public List<Entity> getRelatedEntitiesInEntity()
        //{
        //  return this.relatedEntitiesInEntity;
        //}

        //public void setRelatedEntitiesInEntityForm(List<Entity> relatedEntities)
        //{
        //  this.relatedEntitiesInEntity = relatedEntities;
        //}
    }

    public class SearchResult
    {
        private int rank;
        private String url;
        private String title;
        private String description;

        public int getRank()
        {
            return rank;
        }

        public void setRank(int rank)
        {
            this.rank = rank;
        }

        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            this.url = url;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public class Edge
    {
        private String source;
        private String target;
        private double score;

        public Edge(String source, String target, double score) throws InvalidEdgeException
        {
            if(source == null || target == null)
            {
                throw new InvalidEdgeException("source: " + source + ", target: " + target);
            }
            this.source = source;
            this.target = target;
            this.score = score;
        }

        public Edge(String source, String target) throws InvalidEdgeException
        {
            if(source == null || target == null)
            {
                throw new InvalidEdgeException("source: " + source + ", target: " + target);
            }
            this.source = source;
            this.target = target;
        }

        public String getSource()
        {
            return source;
        }

        public String getTarget()
        {
            return target;
        }

        public double getScore()
        {
            return score;
        }

        @Override
        public int hashCode()
        {
            return this.source.hashCode() ^ this.target.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj instanceof Edge)
            {
                Edge edge = (Edge) obj;

                return (this.source.equals(edge.source) && this.target.equals(edge.target)) || (this.source.equals(edge.target) && this.target.equals(edge.source));
            }
            else
            {
                return false;
            }
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(this.source).append("<-->").append(this.target).append(": (").append(this.score).append(")");
            return builder.toString();
        }

    }

    class InvalidEdgeException extends Exception
    {
        private static final long serialVersionUID = 5313241019420503034L;

        public InvalidEdgeException(String message)
        {
            super(message);
        }
    }
}

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
            // TODO Auto-generated catch block
            log.error("Error while fetching queries for a specific session: " + sessionId, e);
        }

        return queries;
    }

    public List<String> getRelatedEntitiesForSearchId(int searchId)
    {
        List<String> entities = new ArrayList<>();
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
            // TODO Auto-generated catch block
            log.error("Error while fetching related entities for search id: " + searchId, e);
        }
        return entities;
    }

    /**
     * retired function which fetches data from database.
     * 
     * @param queries
     * @return
     */
    public Set<String> getMergedEntities(List<Query> queries)
    {
        Set<String> entities = new HashSet<>();

        for(Query query : queries)
        {
            int searchId = query.getSearchId();
            List<String> relatedEntities = this.getRelatedEntitiesForSearchId(searchId);
            entities.add(query.getQuery());
            entities.addAll(relatedEntities);
        }

        return entities;
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
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement("SELECT * FROM learnweb_large.sl_entity_co_occur WHERE score > 0.02 AND source = ?");
            pstmt.setString(1, entity);
            ResultSet rs = pstmt.executeQuery();
            int i = 0;
            while(rs.next())
            {
                i++;
                edges.add(new Edge(rs.getString("source"), rs.getString("target"), rs.getDouble("score")));
            }
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            log.error("Error while fetching edges for entity: " + entity, e);
        }
        catch(Exception e)
        {
            // TODO Auto-generated catch block
            log.error("Invalid entities for edge fetched: " + entity, e);
        }
        return edges;
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

    public List<List<String>> getRelatedEntitiesForQueries(List<Query> queries)
    {
        List<List<String>> entitiesList = new ArrayList<>();

        for(Query query : queries)
        {
            if(query.getRelatedEntities() == null)
            {
                query.setRelatedEntities(this.getRelatedEntitiesForSearchId(query.searchId));
            }
            entitiesList.add(query.getRelatedEntities());
        }

        return entitiesList;
    }

    public Set<String> getMergedEntitiesForSession(List<Query> queries)
    {
        Set<String> entities = new HashSet<>();

        for(Query query : queries)
        {
            entities.add(query.getQuery());
        }

        List<List<String>> relatedEntitiesList = this.getRelatedEntitiesForQueries(queries);
        for(List<String> relatedEntities : relatedEntitiesList)
        {
            entities.addAll(relatedEntities);
        }

        return entities;
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

    public static void main(String[] args) throws ClassNotFoundException, SQLException
    {
        SearchHistoryManager searchHistoryManager = Learnweb.createInstance("").getSearchHistoryManager();
        long start = System.currentTimeMillis();
        searchHistoryManager.getSessionsForUser(10683);
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        System.exit(0);
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
    }

    public class Edge
    {
        private String source;
        private String target;
        private double score;

        public Edge(String source, String target, double score) throws Exception
        {
            if(source == null || target == null)
            {
                throw new Exception()
                {
                };
            }
            this.source = source;
            this.target = target;
            this.score = score;
        }

        public Edge(String source, String target) throws Exception
        {
            if(source == null || target == null)
            {
                throw new Exception()
                {
                };
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
}

package de.l3s.searchHistoryTest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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

    public List<String> getQueriesForSessionId(String sessionId)
    {
        List<String> queries = new ArrayList<String>();
        return queries;
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

        public String getQueriesAsString()
        {
            String queriesString = "";
            int i = 0;
            for(; i < queries.size() - 1; i++)
                queriesString += queries.get(i).getQuery() + " &#8594; ";

            queriesString += queries.get(i).getQuery();
            return queriesString;
        }
    }

    public class Query
    {
        private int searchId;
        private String query;
        private Date timestamp;
        private String service;
        private List<String> relatedEntities;

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

        public List<String> relatedEntities()
        {
            return this.relatedEntities;
        }

        public void setRelatedEntities(List<String> relatedEntities)
        {
            this.relatedEntities = relatedEntities;
        }
    }
}

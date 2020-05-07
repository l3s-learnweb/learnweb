package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;

public class SearchHistoryManager
{

    private static final Logger log = LogManager.getLogger(SearchHistoryManager.class);
    private final Learnweb learnweb;

    public SearchHistoryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    /**
     * Returns queries for given session id
     *
     * @param sessionId
     * @return
     */
    public LinkedList<Query> getQueriesForSessionId(String sessionId)
    {
        LinkedList<Query> queries = new LinkedList<>();

        try
        {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                    "SELECT t1.search_id, t1.query, t1.timestamp, t1.service FROM learnweb_large.sl_query t1 join learnweb_main.lw_user_log t2 ON (t1.search_id=t2.target_id AND t1.user_id=t2.user_id AND t1.query=t2.params)  WHERE t2.action = 5 AND t1.mode='text' AND t2.session_id=? AND t1.user_id != 0  ORDER BY t1.timestamp ASC");
            pstmt.setString(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                Query query = new Query(rs.getInt("search_id"), rs.getString("query"), new Date(rs.getTimestamp("timestamp").getTime()), rs.getString("service"));
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

    public List<SearchResult> getSearchResultsForSearchId(int searchId, int limit)
    {
        List<SearchResult> searchResults = new ArrayList<>();
        try
        {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM learnweb_large.sl_resource WHERE search_id = ? ORDER BY `rank` LIMIT ?");
            pStmt.setInt(1, searchId);
            pStmt.setInt(2, limit);

            ResultSet rs = pStmt.executeQuery();
            while(rs.next())
            {
                SearchResult result = new SearchResult();
                result.setSearchId(searchId);
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

    public List<Query> getQueriesForSessionFromCache(int userId, String sessionId)
    {
        List<Query> queries = new ArrayList<>();

        if(SessionCache.instance().existsUserId(userId))
        {

            List<Session> sessions = SessionCache.instance().getByUserId(userId);

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

    public List<Query> getQueriesForSessionFromGroupCache(int groupId, String sessionId)
    {
        List<Query> queries = new ArrayList<>();

        if(SessionCache.instance().existsGroupId(groupId))
        {

            List<Session> sessions = SessionCache.instance().getByGroupId(groupId);

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

    public List<Session> getSessionsForUser(int userId) throws SQLException
    {
        List<Session> sessions = new ArrayList<>();
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
                "SELECT t2.session_id " +
                        "FROM learnweb_large.`sl_query` t1 join learnweb_main.lw_user_log t2 " +
                        "WHERE t1.search_id=t2.target_id AND t2.action = 5 AND t1.user_id=t2.user_id AND t1.query=t2.params AND t1.mode='text' AND t1.user_id=? " +
                        "GROUP BY t2.session_id " +
                        "ORDER BY t1.timestamp DESC");

        pStmt.setInt(1, userId);
        ResultSet rs = pStmt.executeQuery();

        while(rs.next())
        {
            String sessionId = rs.getString("session_id");
            Session session = new Session(sessionId, userId, getQueriesForSessionId(sessionId));

            sessions.add(session);
        }

        SessionCache.instance().cacheByUserId(userId, sessions);

        return sessions;
    }

    public List<Session> getSessionsForGroupId(int groupId) throws SQLException
    {
        List<Session> sessions = new ArrayList<>();
        Set<String> sessionIds = new HashSet<>();

        if(SessionCache.instance().existsGroupId(groupId))
        {
            return SessionCache.instance().getByGroupId(groupId);
        }

        PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                "SELECT t1.user_id, t1.session_id, t1.params " +
                        "FROM learnweb_main.lw_user_log t1 " +
                        "JOIN learnweb_main.lw_resource t2 ON (t1.target_id = t2.resource_id) " +
                        "WHERE ((t1.action = 15 AND t2.group_id = ?) OR (t1.action = 24 AND t1.group_id = ?)) AND t2.type NOT IN ('image', 'video') " +
                        "ORDER BY t1.timestamp DESC");

        pstmt.setInt(1, groupId);
        pstmt.setInt(2, groupId);
        ResultSet rs = pstmt.executeQuery();
        while(rs.next())
        {
            String sessionId = rs.getString("session_id");
            int userId = rs.getInt("user_id");
            if(!sessionIds.contains(sessionId))
            {
                String params = rs.getString("params");
                if(params.matches("\\d+ - \\d+"))
                {
                    Session session = new Session(sessionId, userId, getQueriesForSessionId(sessionId));
                    sessions.add(session);
                }
                sessionIds.add(sessionId);
            }
        }

        SessionCache.instance().cacheByGroupId(groupId, sessions);

        return sessions;
    }

    public static class Session implements Serializable
    {
        private static final long serialVersionUID = 6139247221701183553L;

        private final int userId;
        private final String sessionId;
        private final LinkedList<Query> queries;

        public Session(String sessionId, int userId, LinkedList<Query> queries)
        {
            this.sessionId = sessionId;
            this.userId = userId;
            this.queries = queries;
        }

        public String getSessionId()
        {
            return this.sessionId;
        }

        public String getUserName()
        {
            try
            {
                return getUser().getUsername();
            }
            catch(Exception e)
            {
                log.error(e);
                return "unknown";
            }
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

        public int getUserId()
        {
            return userId;
        }

        public User getUser() throws SQLException
        {
            return Learnweb.getInstance().getUserManager().getUser(userId);
        }
    }

    public static class Query implements Serializable
    {
        private static final long serialVersionUID = 4391998336381044255L;

        private int searchId;
        private String query;
        private Date timestamp;
        private String service;

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
    }

    public static class SearchResult implements Serializable
    {
        private static final long serialVersionUID = 2951387044534205707L;

        private int searchId;
        private int rank;
        private String url;
        private String title;
        private String description;

        public int getSearchId()
        {
            return searchId;
        }

        public void setSearchId(int searchId)
        {
            this.searchId = searchId;
        }

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
}

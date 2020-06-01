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
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.learnweb.user.User;

public class SearchHistoryManager {
    private static final Logger log = LogManager.getLogger(SearchHistoryManager.class);
    private final Learnweb learnweb;

    public SearchHistoryManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    /**
     * Returns queries for given session id.
     */
    public LinkedList<Query> getQueriesForSessionId(String sessionId) {
        LinkedList<Query> queries = new LinkedList<>();

        try {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                "SELECT q.search_id, q.query, q.mode, q.timestamp, q.service " +
                    "FROM learnweb_large.sl_query q join learnweb_main.lw_user_log l ON q.search_id = l.target_id AND q.user_id = l.user_id " +
                    "WHERE l.action = 5 AND l.session_id = ? " +
                    "ORDER BY q.timestamp ASC");
            pstmt.setString(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Query query = new Query(
                    rs.getInt("search_id"),
                    rs.getString("query"),
                    rs.getString("mode"),
                    new Date(rs.getTimestamp("timestamp").getTime()),
                    rs.getString("service")
                );
                queries.add(query);
            }
            pstmt.close();
        } catch (SQLException e) {
            log.error("Error while fetching queries for a specific session: " + sessionId, e);
        }

        return queries;
    }

    public List<ResourceDecorator> getSearchResultsForSearchId(int searchId, int limit) {
        List<ResourceDecorator> searchResults = new ArrayList<>();

        try {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
                "SELECT r.resource_id, r.rank, r.url, r.title, r.description, r.thumbnail_url, r.thumbnail_width, r.thumbnail_height, COUNT(a.action = 'resource_clicked') AS clicked, COUNT(a.action = 'resource_saved') AS saved " +
                    "FROM learnweb_large.sl_resource r LEFT JOIN learnweb_large.sl_action a ON r.search_id = a.search_id AND r.rank = a.rank " +
                    "WHERE r.search_id = ? GROUP BY r.resource_id, r.rank, r.url, r.title, r.description ORDER BY r.rank LIMIT ?");
            pStmt.setInt(1, searchId);
            pStmt.setInt(2, limit);

            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                Resource res;
                int resourceId = rs.getInt("resource_id");
                if (resourceId > 0) {
                    res = learnweb.getResourceManager().getResource(resourceId);
                } else {
                    res = new Resource();
                    res.setUrl(rs.getString("url"));
                    res.setTitle(rs.getString("title"));
                    res.setDescription(rs.getString("description"));
                    res.setThumbnail2(new Thumbnail(rs.getString("thumbnail_url"), rs.getInt("thumbnail_width"), rs.getInt("thumbnail_height")));
                }

                ResourceDecorator rd = new ResourceDecorator(res);
                rd.setRank(rs.getInt("rank"));
                rd.setTitle(rs.getString("title"));
                rd.setSnippet(rs.getString("description"));
                rd.setClicked(rs.getInt("clicked") > 0);
                rd.setSaved(rs.getInt("saved") > 0);
                searchResults.add(rd);
            }
            pStmt.close();
        } catch (SQLException e) {
            log.error("Error while fetching search results for search id: " + searchId, e);
        }

        return searchResults;
    }

    public List<Query> getQueriesForSessionFromCache(int userId, String sessionId) {
        List<Query> queries = new ArrayList<>();

        if (SessionCache.instance().existsUserId(userId)) {

            List<Session> sessions = SessionCache.instance().getByUserId(userId);

            for (Session session : sessions) {
                if (session.getSessionId().equals(sessionId)) {
                    queries.addAll(session.getQueries());
                }
            }
        }

        return queries;
    }

    public List<Query> getQueriesForSessionFromGroupCache(int groupId, String sessionId) {
        List<Query> queries = new ArrayList<>();

        if (SessionCache.instance().existsGroupId(groupId)) {
            List<Session> sessions = SessionCache.instance().getByGroupId(groupId);

            for (Session session : sessions) {
                if (session.getSessionId().equals(sessionId)) {
                    queries.addAll(session.getQueries());
                }
            }
        }

        return queries;
    }

    public List<Session> getSessionsForUser(int userId) throws SQLException {
        List<Session> sessions = new ArrayList<>();
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
            "SELECT DISTINCT l.session_id " +
                "FROM learnweb_large.sl_query q JOIN learnweb_main.lw_user_log l ON q.search_id = l.target_id AND q.user_id = l.user_id " +
                "WHERE l.action = 5 AND l.user_id = ? " +
                "ORDER BY q.timestamp DESC LIMIT 30");

        pStmt.setInt(1, userId);
        ResultSet rs = pStmt.executeQuery();

        while (rs.next()) {
            String sessionId = rs.getString("session_id");
            Session session = new Session(sessionId, userId, getQueriesForSessionId(sessionId));

            sessions.add(session);
        }

        SessionCache.instance().cacheByUserId(userId, sessions);

        return sessions;
    }

    public List<Session> getSessionsForGroupId(int groupId) throws SQLException {
        if (SessionCache.instance().existsGroupId(groupId)) {
            return SessionCache.instance().getByGroupId(groupId);
        }

        List<Session> sessions = new ArrayList<>();
        Set<String> sessionIds = new HashSet<>();

        PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
            "SELECT l.user_id, l.session_id " +
                "FROM learnweb_large.`sl_query` q " +
                "JOIN learnweb_main.lw_user_log l ON q.search_id = l.target_id AND q.user_id = l.user_id " +
                "JOIN learnweb_main.lw_group_user ug ON ug.user_id =  l.user_id " +
                "WHERE l.action = 5 AND ug.group_id = ? " +
                "GROUP BY l.user_id, l.session_id " +
                "ORDER BY q.timestamp DESC LIMIT 30");

        pstmt.setInt(1, groupId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            String sessionId = rs.getString("session_id");
            int userId = rs.getInt("user_id");
            if (!sessionIds.contains(sessionId)) {
                Session session = new Session(sessionId, userId, getQueriesForSessionId(sessionId));
                sessions.add(session);
                sessionIds.add(sessionId);
            }
        }

        SessionCache.instance().cacheByGroupId(groupId, sessions);

        return sessions;
    }

    public static class Session implements Serializable {
        private static final long serialVersionUID = 6139247221701183553L;

        private final int userId;
        private final String sessionId;
        private final LinkedList<Query> queries;

        public Session(String sessionId, int userId, LinkedList<Query> queries) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.queries = queries;
        }

        public String getSessionId() {
            return this.sessionId;
        }

        public String getUserName() {
            try {
                return getUser().getUsername();
            } catch (Exception e) {
                log.error(e);
                return "unknown";
            }
        }

        public List<Query> getQueries() {
            return this.queries;
        }

        public Date getStartTimestamp() {
            return queries.getFirst().getTimestamp();
        }

        public Date getEndTimestamp() {
            return queries.getLast().getTimestamp();
        }

        public int getUserId() {
            return userId;
        }

        public User getUser() throws SQLException {
            return Learnweb.getInstance().getUserManager().getUser(userId);
        }
    }

    public static class Query implements Serializable {
        private static final long serialVersionUID = 4391998336381044255L;

        private int searchId;
        private String query;
        private String mode;
        private Date timestamp;
        private String service;

        public Query(int searchId, String query, String mode, Date timestamp, String service) {
            this.searchId = searchId;
            this.query = query;
            this.mode = mode;
            this.timestamp = timestamp;
            this.service = service;
        }

        public int getSearchId() {
            return this.searchId;
        }

        public String getQuery() {
            return this.query;
        }

        public String getMode() {
            return mode;
        }

        public Date getTimestamp() {
            return this.timestamp;
        }

        public String getService() {
            return this.service;
        }
    }

    public static class SearchResult implements Serializable {
        private static final long serialVersionUID = 2951387044534205707L;

        private int searchId;
        private int rank;
        private String url;
        private String title;
        private String description;

        public SearchResult(int searchId) {
            this.searchId = searchId;
        }

        public int getSearchId() {
            return searchId;
        }

        public void setSearchId(int searchId) {
            this.searchId = searchId;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}

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
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.learnweb.user.User;

public class SearchHistoryManager {
    private static final Logger log = LogManager.getLogger(SearchHistoryManager.class);
    private final Learnweb learnweb;

    public SearchHistoryManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    /**
     * Returns queries for given session.
     */
    public LinkedList<Query> getQueries(Session session) {
        LinkedList<Query> queries = new LinkedList<>();

        try {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                "SELECT q.search_id, q.query, q.mode, q.timestamp, q.service " +
                    "FROM learnweb_large.sl_query q join learnweb_main.lw_user_log l ON q.search_id = l.target_id AND q.user_id = l.user_id " +
                    "WHERE l.action = 5 AND l.session_id = ? " +
                    "ORDER BY q.timestamp ASC");
            pstmt.setString(1, session.getSessionId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Query query = new Query(session,
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
            log.error("Error while fetching queries for a specific session: {}", session.getSessionId(), e);
        }

        return queries;
    }

    public List<ResourceDecorator> getSearchResults(Query query, int limit) {
        List<ResourceDecorator> searchResults = new ArrayList<>();

        try {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
                "SELECT r.resource_id, r.rank, r.url, r.title, r.description, r.thumbnail_url, r.thumbnail_width, r.thumbnail_height, COUNT(a.action = 'resource_clicked') AS clicked, COUNT(a.action = 'resource_saved') AS saved " +
                    "FROM learnweb_large.sl_resource r LEFT JOIN learnweb_large.sl_action a ON r.search_id = a.search_id AND r.rank = a.rank " +
                    "WHERE r.search_id = ? GROUP BY r.resource_id, r.rank, r.url, r.title, r.description ORDER BY r.rank ASC LIMIT ?");
            pStmt.setInt(1, query.getSearchId());
            pStmt.setInt(2, limit);

            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                Resource res;
                int resourceId = rs.getInt("resource_id");
                if (resourceId > 0) {
                    res = learnweb.getResourceManager().getResource(resourceId);
                } else {
                    res = new Resource();
                    res.setType(ResourceType.website);
                    res.setSource(ResourceService.learnweb);
                    res.setUserId(query.getSession().getUserId());
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

                // quick fix to not show annotations for each result of same url
                if (rd.getClicked()) {
                    rd.setAnnotations(getAnnotations(res.getUserId(), res.getUrl()));
                }

                searchResults.add(rd);
            }
            pStmt.close();
        } catch (SQLException e) {
            log.error("Error while fetching search results for search id: {}", query.getSearchId(), e);
        }

        return searchResults;
    }

    public List<Session> getSessionsForUser(int userId) throws SQLException {
        // if (SessionCache.instance().existsUserId(userId)) {
        //     return SessionCache.instance().getByUserId(userId);
        // }

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
            Session session = new Session(sessionId, userId);
            session.setQueries(getQueries(session));
            sessions.add(session);
        }

        // SessionCache.instance().cacheByUserId(userId, sessions);
        return sessions;
    }

    public List<Session> getSessionsForGroupId(int groupId) throws SQLException {
        // if (SessionCache.instance().existsGroupId(groupId)) {
        //     return SessionCache.instance().getByGroupId(groupId);
        // }

        List<Session> sessions = new ArrayList<>();
        Set<String> sessionIds = new HashSet<>();

        PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
            "SELECT l.user_id, l.session_id " +
                "FROM learnweb_large.`sl_query` q " +
                "JOIN learnweb_main.lw_user_log l ON q.search_id = l.target_id AND q.user_id = l.user_id " +
                "JOIN learnweb_main.lw_group_user ug ON ug.user_id =  l.user_id " +
                "WHERE l.action = 5 AND ug.group_id = ? " +
                "GROUP BY l.user_id, l.session_id, q.timestamp " +
                "ORDER BY q.timestamp DESC LIMIT 30");

        pstmt.setInt(1, groupId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            String sessionId = rs.getString("session_id");
            int userId = rs.getInt("user_id");
            if (!sessionIds.contains(sessionId)) {
                Session session = new Session(sessionId, userId);
                session.setQueries(getQueries(session));
                sessions.add(session);
                sessionIds.add(sessionId);
            }
        }

        // SessionCache.instance().cacheByGroupId(groupId, sessions);
        return sessions;
    }

    public List<Annotation> getAnnotations(int userId, String url) throws SQLException {
        List<Annotation> annotations = new ArrayList<>();
        try (PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
            "SELECT a.user_id, a.text, a.quote, a.target_uri " +
                "FROM learnweb_annotations.annotation a " +
                "WHERE a.user_id = ? AND a.target_uri_normalized = ? " +
                "ORDER BY a.created")) {

            pStmt.setInt(1, userId);
            pStmt.setString(2, url);
            try (ResultSet rs = pStmt.executeQuery()) {
                while (rs.next()) {
                    Annotation annotation = new Annotation();
                    annotation.setUserId(rs.getInt("user_id"));
                    annotation.setText(rs.getString("text"));
                    annotation.setQuote(rs.getString("quote"));
                    annotation.setTargetUrl(rs.getString("target_uri"));
                    annotations.add(annotation);
                }

                return annotations;
            }
        }
    }

    public static class Session implements Serializable {
        private static final long serialVersionUID = 6139247221701183553L;

        private final int userId;
        private final String sessionId;
        private LinkedList<Query> queries;

        public Session(String sessionId, int userId) {
            this.sessionId = sessionId;
            this.userId = userId;
        }

        public String getSessionId() {
            return this.sessionId;
        }

        public String getUsername() {
            try {
                return getUser().getUsername();
            } catch (Exception e) {
                log.error("Can't get user name of user {}", userId, e);
                return "unknown";
            }
        }

        public void setQueries(final LinkedList<Query> queries) {
            this.queries = queries;
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

        private final Session session;

        private final int searchId;
        private final String query;
        private final String mode;
        private final Date timestamp;
        private final String service;

        public Query(Session session, int searchId, String query, String mode, Date timestamp, String service) {
            this.session = session;
            this.searchId = searchId;
            this.query = query;
            this.mode = mode;
            this.timestamp = timestamp;
            this.service = service;
        }

        public Session getSession() {
            return session;
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

    public static class Annotation implements Serializable {
        private static final long serialVersionUID = 1311485147202161998L;

        private int userId;
        private String text;
        private String quote;
        private String targetUrl;

        public int getUserId() {
            return userId;
        }

        public void setUserId(final int userId) {
            this.userId = userId;
        }

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }

        public String getQuote() {
            return quote;
        }

        public void setQuote(final String quote) {
            this.quote = quote;
        }

        public String getTargetUrl() {
            return targetUrl;
        }

        public void setTargetUrl(final String targetUrl) {
            this.targetUrl = targetUrl;
        }
    }
}

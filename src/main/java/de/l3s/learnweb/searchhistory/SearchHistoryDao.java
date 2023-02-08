package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.resource.web.WebResource;
import de.l3s.learnweb.user.User;
import de.l3s.util.SqlHelper;
import de.l3s.util.StringHelper;

public interface SearchHistoryDao extends SqlObject, Serializable {
    enum SearchAction {
        resource_clicked,
        resource_saved
    }

    @RegisterRowMapper(QueryMapper.class)
    @SqlQuery("SELECT q.search_id, q.query, q.mode, q.timestamp, q.service "
        + "FROM learnweb_large.sl_query q join lw_user_log l ON q.search_id = l.target_id AND q.user_id = l.user_id "
        + "WHERE l.action = 5 AND l.session_id = ? ORDER BY q.timestamp ASC")
    List<SearchQuery> findQueriesBySessionId(String sessionId);

    @RegisterRowMapper(AnnotationMapper.class)
    @SqlQuery("SELECT a.user_id, a.text, a.quote, a.target_uri FROM learnweb_annotations.annotation a "
        + "WHERE a.user_id = ? AND a.target_uri_normalized = ? ORDER BY a.created")
    List<SearchAnnotation> findAnnotationsByUserIdAndUrl(int userId, String url);

    default List<ResourceDecorator> findSearchResultsByQuery(SearchQuery query, int limit) {
        ResourceDao resourceDao = getHandle().attach(ResourceDao.class);

        return getHandle().select("SELECT r.resource_id, r.rank, r.url, r.title, r.description, r.thumbnail_url, r.thumbnail_width, r.thumbnail_height, "
            + "COUNT(a.action = 'resource_clicked') AS clicked, COUNT(a.action = 'resource_saved') AS saved "
            + "FROM learnweb_large.sl_resource r LEFT JOIN learnweb_large.sl_action a ON r.search_id = a.search_id AND r.rank = a.rank "
            + "WHERE r.search_id = ? GROUP BY r.resource_id, r.rank, r.url, r.title, r.description ORDER BY r.rank ASC LIMIT ?", query.searchId(), limit)
            .map((rs, ctx) -> {
                int resourceId = rs.getInt("resource_id");

                Resource res;
                if (resourceId != 0) {
                    res = resourceDao.findByIdOrElseThrow(resourceId);
                } else {
                    res = new WebResource();
                    res.setUrl(rs.getString("url"));
                    res.setTitle(rs.getString("title"));
                    res.setDescription(rs.getString("description"));
                    res.setHeight(rs.getInt("thumbnail_height"));
                    res.setWidth(rs.getInt("thumbnail_width"));
                    res.setThumbnailMedium(rs.getString("thumbnail_url"));
                }

                ResourceDecorator rd = new ResourceDecorator(res);
                rd.setRank(rs.getInt("rank"));
                rd.setTitle(rs.getString("title"));
                rd.setSnippet(rs.getString("description"));
                rd.setClicked(rs.getInt("clicked") > 0);
                rd.setSaved(rs.getInt("saved") > 0);

                // quick fix to not show annotations for each result of same url
                if (rd.getClicked()) {
                    rd.setAnnotations(findAnnotationsByUserIdAndUrl(res.getUserId(), res.getUrl()));
                }
                return rd;
            }).list();
    }

    default List<SearchSession> findSessionsByUserId(int userId) {
        return getHandle().select("SELECT DISTINCT l.session_id FROM learnweb_large.sl_query q JOIN lw_user_log l ON q.search_id = l.target_id "
            + "AND q.user_id = l.user_id WHERE l.action = 5 AND l.user_id = ? ORDER BY q.timestamp DESC LIMIT 30", userId)
            .map((rs, ctx) -> {
                SearchSession session = new SearchSession(rs.getString("session_id"), userId);
                session.setQueries(findQueriesBySessionId(session.getSessionId()));
                return session;
            }).list();
    }

    default List<SearchSession> findSessionsByGroupId(int groupId) {
        return getHandle().select("SELECT DISTINCT l.user_id, l.session_id FROM learnweb_large.sl_query q JOIN lw_user_log l ON q.search_id = l.target_id "
            + "AND q.user_id = l.user_id JOIN lw_group_user ug ON ug.user_id = l.user_id "
            + "WHERE l.action = 5 AND ug.group_id = ? GROUP BY l.user_id, l.session_id, q.timestamp ORDER BY q.timestamp DESC LIMIT 30", groupId)
            .map((rs, ctx) -> {
                SearchSession session = new SearchSession(rs.getString("session_id"), rs.getInt("user_id"));
                session.setQueries(findQueriesBySessionId(session.getSessionId()));
                return session;
            }).list();
    }

    @RegisterRowMapper(AnnotationCountMapper.class)
    @SqlQuery("SELECT * FROM learnweb_annotations.annotation_count WHERE uri = ? AND type LIKE ?")
    Optional<AnnotationCount> findByUriAndType(String uri, String type);

    @RegisterRowMapper(AnnotationCountMapper.class)
    @SqlQuery("SELECT * FROM learnweb_annotations.annotation_count WHERE users REGEXP CONCAT('.*(^|,)', ?, '(,|$).*') ORDER BY created_at")
    List<AnnotationCount> findAnnotationCountByUsername(String username);

    @RegisterRowMapper(JsonSharedObjectMapper.class)
    @SqlQuery("SELECT id, shared_object FROM learnweb_annotations.annotation_objects WHERE group_id = ? AND user_id = ? AND application = ?")
    List<JsonSharedObject> findObjectsByUserId(int groupId, int userId, String application);

    @RegisterRowMapper(JsonSharedObjectMapper.class)
    @SqlQuery("SELECT id, shared_object FROM learnweb_annotations.annotation_objects WHERE group_id = ? AND application = ?")
    List<JsonSharedObject> findObjectsByGroupIdAndType(int groupId, String application);

    @SqlUpdate("INSERT INTO learnweb_annotations.annotation_input_stream (user_id, type, content, date_created) VALUES(?, ?, ?, CURRENT_TIMESTAMP())")
    @GetGeneratedKeys("id")
    int insertInputStream(int userId, String type, String content);

    @RegisterRowMapper(InputStreamRdfMapper.class)
    @SqlQuery("SELECT * FROM learnweb_annotations.annotation_input_stream WHERE id IN (<inputIds>)")
    List<InputStreamRdf> findInputContentById(@Define("inputIds") String inputIds);

    @SqlUpdate("UPDATE learnweb_annotations.annotation_objects SET shared_object = ?, created_at = ? WHERE user_id = ? AND group_id = ? AND application = ?")
    void updateSharedObject(String sharedObject, LocalDateTime createdAt, int userId, int groupId, String application);

    @SqlUpdate("INSERT INTO learnweb_annotations.annotation_objects (user_id, group_id, application, shared_object, created_at) "
        + "VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP())")
    @GetGeneratedKeys("id")
    int insertSharedObject(int userId, int groupId, String application, String sharedObject);

    @SqlUpdate("UPDATE learnweb_annotations.annotation_count SET session_id = ?, users = ?, input_id = ? WHERE uri = ? AND type = ? ")
    void updateQueryAnnotation(String sessionId, String user, String inputId, String uri, String type);

    @SqlUpdate("INSERT INTO learnweb_annotations.annotation_query_count SET search_id = ?, uri_id = ?")
    int insertQueryResult(int searchId, int uriId);

    @SqlQuery("SELECT search_id FROM learnweb_annotations.annotation_query_count WHERE uri_id = ?")
    List<Integer> findSearchIdByResult(int uriId);

    @SqlUpdate("INSERT INTO learnweb_annotations.annotation_count (type, uri, input_id, created_at, surface_form, session_id, users, confidence) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
    @GetGeneratedKeys("uri_id")
    int insertQueryToAnnotation(String type, String uri, String input, LocalDateTime createdAt, String surfaceForm, String sessionId, String users, double confidence);

    @RegisterRowMapper(RdfObjectMapper.class)
    @SqlQuery("SELECT * FROM learnweb_annotations.annotation_rdf WHERE user_id = ?")
    Optional<RdfObject> findRdfById(int userId);

    @SqlUpdate("INSERT INTO learnweb_annotations.annotation_rdf (user_id, group_id, rdf_value) VALUES (?, ?, ?)")
    @GetGeneratedKeys("id")
    int insertRdf(int userId, int groupId, String rdfValue);

    @SqlUpdate("UPDATE learnweb_annotations.annotation_rdf SET rdf_value = ? WHERE user_id = ?")
    void updateRdf(String rdfValue, int userId);

    @SqlUpdate("INSERT INTO learnweb_large.sl_query (query, mode, service, language, filters, user_id, timestamp, learnweb_version) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 3)")
    @GetGeneratedKeys("search_id")
    int insertQuery(String query, SearchMode searchMode, ResourceService searchService, String language, String searchFilters, User user);

    @SqlUpdate("INSERT INTO learnweb_large.sl_query (group_id, query, mode, service, language, filters, user_id, timestamp, learnweb_version) VALUES (?, ?, 'group', 'learnweb', ?, ?, ?, CURRENT_TIMESTAMP, 3)")
    @GetGeneratedKeys("search_id")
    int insertGroupQuery(int groupId, String query, String language, String searchFilters, int userId);

    @SqlUpdate("INSERT INTO learnweb_large.sl_action (search_id, `rank`, user_id, action, timestamp) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)")
    void insertAction(int searchId, int rank, User user, SearchAction action);

    default void insertResources(int searchId, List<ResourceDecorator> resources) {
        if (resources.isEmpty() || searchId == 0) { // failed to log query, no need to log resources
            return;
        }

        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO learnweb_large.sl_resource (search_id, `rank`, resource_id, url, title, description, "
            + "thumbnail_url, thumbnail_height, thumbnail_width) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

        for (ResourceDecorator decoratedResource : resources) {
            batch.bind(0, searchId);
            batch.bind(1, decoratedResource.getRank());

            if (decoratedResource.getResource().getId() != 0) {
                // resource is stored in Learnweb, we do not need to save the title or description
                batch.bind(2, decoratedResource.getResource().getId());
                batch.bindNull(3, Types.VARCHAR);
                batch.bindNull(4, Types.VARCHAR);
                batch.bindNull(5, Types.VARCHAR);
                batch.bindNull(6, Types.VARCHAR);
                batch.bindNull(7, Types.INTEGER);
                batch.bindNull(8, Types.INTEGER);
            } else {
                // no learnweb resource -> store title URL and description
                batch.bindNull(2, Types.INTEGER);
                batch.bind(3, decoratedResource.getUrl());
                batch.bind(4, StringHelper.shortnString(decoratedResource.getTitle(), 250));
                batch.bind(5, StringHelper.shortnString(decoratedResource.getDescription(), 1000));
                batch.bind(6, decoratedResource.getThumbnailMedium());
                batch.bind(7, SqlHelper.toNullable(decoratedResource.getHeight()));
                batch.bind(8, SqlHelper.toNullable(decoratedResource.getWidth()));
            }
            batch.add();
        }

        batch.execute();
    }

    class QueryMapper implements RowMapper<SearchQuery> {
        @Override
        public SearchQuery map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new SearchQuery(
                rs.getInt("search_id"),
                rs.getString("query"),
                rs.getString("mode"),
                SqlHelper.getLocalDateTime(rs.getTimestamp("timestamp")),
                rs.getString("service")
            );
        }
    }

    class AnnotationMapper implements RowMapper<SearchAnnotation> {
        @Override
        public SearchAnnotation map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SearchAnnotation annotation = new SearchAnnotation();
            annotation.setUserId(rs.getInt("user_id"));
            annotation.setText(rs.getString("text"));
            annotation.setQuote(rs.getString("quote"));
            annotation.setTargetUrl(rs.getString("target_uri"));
            return annotation;
        }
    }

    class AnnotationCountMapper implements RowMapper<AnnotationCount> {
        @Override
        public AnnotationCount map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            AnnotationCount annotation = new AnnotationCount();
            annotation.setUriId(rs.getInt("uri_id"));
            annotation.setUri(rs.getString("uri"));
            annotation.setConfidence(rs.getDouble("confidence"));
            annotation.setSurfaceForm(rs.getString("surface_form"));
            annotation.setType(rs.getString("type"));
            annotation.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            annotation.setSessionId(rs.getString("session_id"));
            annotation.setUsers(rs.getString("users"));
            annotation.setInputStreams(rs.getString("input_id"));
            return annotation;
        }
    }

    class JsonSharedObjectMapper implements RowMapper<JsonSharedObject> {
        @Override
        public JsonSharedObject map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            JsonSharedObject sharedObject = new JsonSharedObject(rs.getString("shared_object"), true);
            sharedObject.setId(rs.getInt("id"));
            return sharedObject;
        }
    }

    class RdfObjectMapper implements RowMapper<RdfObject> {
        @Override
        public RdfObject map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            RdfObject obj = new RdfObject(rs.getInt("user_id"), rs.getString("rdf_value"));
            return obj;
        }
    }

    class InputStreamRdfMapper implements RowMapper<InputStreamRdf> {
        @Override
        public InputStreamRdf map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            InputStreamRdf obj = new InputStreamRdf(rs.getInt("id"), rs.getInt("user_id"), rs.getString("content"), rs.getString("type"),
                rs.getDate("date_created"));
            return obj;
        }
    }
}

package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.learnweb.resource.search.SearchMode;
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
            + "WHERE r.search_id = ? GROUP BY r.resource_id, r.rank, r.url, r.title, r.description ORDER BY r.rank ASC LIMIT ?", query.getSearchId(), limit)
            .map((rs, ctx) -> {
                int resourceId = rs.getInt("resource_id");

                Resource res;
                if (resourceId != 0) {
                    res = resourceDao.findById(resourceId);
                } else {
                    res = new Resource();
                    res.setType(ResourceType.website);
                    res.setSource(ResourceService.learnweb);
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
            + "AND q.user_id = l.user_id JOIN lw_group_user ug ON ug.user_id =  l.user_id "
            + "WHERE l.action = 5 AND ug.group_id = ? GROUP BY l.user_id, l.session_id, q.timestamp ORDER BY q.timestamp DESC LIMIT 30", groupId)
            .map((rs, ctx) -> {
                SearchSession session = new SearchSession(rs.getString("session_id"), rs.getInt("user_id"));
                session.setQueries(findQueriesBySessionId(session.getSessionId()));
                return session;
            }).list();
    }

    @SqlUpdate("INSERT INTO learnweb_large.sl_query (query, mode, service, language, filters, user_id, timestamp, learnweb_version) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 3)")
    @GetGeneratedKeys("search_id")
    int insertQuery(String query, SearchMode searchMode, ResourceService searchService, String language, String searchFilters, User user);

    @SqlUpdate("INSERT INTO learnweb_large.sl_query (group_id, query, mode, service, language, filters, user_id, timestamp, learnweb_version) VALUES (?, ?, 'group', 'learnweb', ?, ?, ?, CURRENT_TIMESTAMP, 3)")
    @GetGeneratedKeys("search_id")
    int insertGroupQuery(int groupId, String query, String language, String searchFilters, int userId);

    @SqlUpdate("INSERT INTO learnweb_large.sl_action (search_id, rank, user_id, action, timestamp) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)")
    void insertAction(int searchId, int rank, User user, SearchAction action);

    default void insertResources(int searchId, List<ResourceDecorator> resources) {
        if (resources.isEmpty() || searchId == 0) { // failed to log query, no need to log resources
            return;
        }

        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO learnweb_large.sl_resource (search_id, rank, resource_id, url, title, description, "
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

                Thumbnail thumbnail = decoratedResource.getMediumThumbnail();
                if (thumbnail != null) {
                    batch.bind(6, thumbnail.getUrl());
                    batch.bind(7, Math.min(thumbnail.getHeight(), 65535));
                    batch.bind(8, Math.min(thumbnail.getWidth(), 65535));
                } else {
                    batch.bindNull(6, Types.VARCHAR);
                    batch.bindNull(7, Types.INTEGER);
                    batch.bindNull(8, Types.INTEGER);
                }
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
}

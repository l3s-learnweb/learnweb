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

    @RegisterRowMapper(SearchHistoryQueryMapper.class)
    @SqlQuery("""
        SELECT q.*
        FROM lw_search_history q JOIN lw_user_log l ON q.search_id = l.target_id AND q.user_id = l.user_id
        WHERE l.action = 5 AND l.session_id = ?
        ORDER BY q.created_at ASC
        """)
    List<SearchHistoryQuery> findQueriesBySessionId(String sessionId);

    default List<ResourceDecorator> findSearchResultsByQuery(SearchHistoryQuery query, int limit) {
        ResourceDao resourceDao = getHandle().attach(ResourceDao.class);

        return getHandle().select("""
                SELECT r.*, COUNT(a.action = 'resource_clicked') AS clicked, COUNT(a.action = 'resource_saved') AS saved
                FROM lw_search_history_resource r LEFT JOIN lw_search_history_action a ON r.search_id = a.search_id AND r.rank = a.rank
                WHERE r.search_id = ?
                GROUP BY r.resource_id, r.rank
                ORDER BY r.rank ASC
                LIMIT ?
                """, query.searchId(), limit)
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
                return rd;
            }).list();
    }

    default List<SearchSession> findSessionsByUserId(int userId) {
        return getHandle().select("SELECT DISTINCT l.session_id FROM lw_search_history q JOIN lw_user_log l ON q.search_id = l.target_id "
                + "AND q.user_id = l.user_id WHERE l.action = 5 AND l.user_id = ? ORDER BY q.created_at DESC LIMIT 30", userId)
            .map((rs, ctx) -> {
                SearchSession session = new SearchSession(rs.getString("session_id"), userId);
                session.setQueries(findQueriesBySessionId(session.getSessionId()));
                return session;
            }).list();
    }

    default List<SearchSession> findSessionsByGroupId(int groupId) {
        return getHandle().select("SELECT DISTINCT l.user_id, l.session_id FROM lw_search_history q JOIN lw_user_log l ON q.search_id = l.target_id "
                + "AND q.user_id = l.user_id JOIN lw_group_user ug ON ug.user_id = l.user_id "
                + "WHERE l.action = 5 AND ug.group_id = ? GROUP BY l.user_id, l.session_id, q.created_at ORDER BY q.created_at DESC LIMIT 30", groupId)
            .map((rs, ctx) -> {
                SearchSession session = new SearchSession(rs.getString("session_id"), rs.getInt("user_id"));
                session.setQueries(findQueriesBySessionId(session.getSessionId()));
                return session;
            }).list();
    }

    @SqlUpdate("INSERT INTO lw_search_history (query, mode, service, language, filters, user_id) VALUES (?, ?, ?, ?, ?, ?)")
    @GetGeneratedKeys("search_id")
    int insertQuery(String query, SearchMode searchMode, ResourceService searchService, String language, String searchFilters, User user);

    @SqlUpdate("INSERT INTO lw_search_history (group_id, query, mode, service, language, filters, user_id) VALUES (?, ?, 'group', 'learnweb', ?, ?, ?)")
    @GetGeneratedKeys("search_id")
    int insertGroupQuery(int groupId, String query, String language, String searchFilters, int userId);

    @SqlUpdate("INSERT IGNORE INTO lw_search_history_action (search_id, `rank`, action) VALUES (?, ?, ?)")
    void insertAction(int searchId, int rank, SearchAction action);

    default void insertResources(int searchId, List<ResourceDecorator> resources) {
        if (resources.isEmpty() || searchId == 0) { // failed to log query, no need to log resources
            return;
        }

        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_search_history_resource (search_id, `rank`, resource_id, url, title, description, "
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

    class SearchHistoryQueryMapper implements RowMapper<SearchHistoryQuery> {
        @Override
        public SearchHistoryQuery map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new SearchHistoryQuery(
                rs.getInt("search_id"),
                rs.getString("query"),
                SearchMode.valueOf(rs.getString("mode")),
                ResourceService.valueOf(rs.getString("service")),
                SqlHelper.getLocalDateTime(rs.getTimestamp("created_at"))
            );
        }
    }
}

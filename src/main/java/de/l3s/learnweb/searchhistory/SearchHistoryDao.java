package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * @return a page of the user's queries, including those that don't belong to a session, the latest first
     */
    @RegisterRowMapper(SearchHistoryQueryMapper.class)
    @SqlQuery("SELECT * FROM lw_search_history WHERE user_id = ? ORDER BY created_at DESC, search_id DESC LIMIT ? OFFSET ?")
    List<SearchHistoryQuery> findQueriesByUserId(int userId, int limit, int offset);

    @SqlQuery("SELECT COUNT(*) FROM lw_search_history WHERE user_id = ?")
    int countQueriesByUserId(int userId);

    default List<ResourceDecorator> findSearchResultsByQuery(SearchHistoryQuery query, int limit) {
        record StoredResult(int rank, int resourceId, Resource webResource, String snippet, boolean clicked, boolean saved) {}

        List<StoredResult> results = getHandle().select("""
                SELECT r.*,
                    COUNT(CASE WHEN a.action = 'resource_clicked' THEN 1 END) AS clicked,
                    COUNT(CASE WHEN a.action = 'resource_saved' THEN 1 END) AS saved
                FROM lw_search_history_resource r LEFT JOIN lw_search_history_action a ON r.search_id = a.search_id AND r.rank = a.rank
                WHERE r.search_id = ?
                GROUP BY r.resource_id, r.rank
                ORDER BY r.rank ASC
                LIMIT ?
                """, query.searchId(), limit)
            .map((rs, ctx) -> {
                int resourceId = rs.getInt("resource_id");

                Resource webResource = null;
                if (resourceId == 0) { // not stored in Learnweb, the search history keeps its details
                    webResource = new WebResource();
                    webResource.setUrl(rs.getString("url"));
                    webResource.setTitle(rs.getString("title"));
                    webResource.setDescription(rs.getString("description"));
                    webResource.setHeight(rs.getInt("thumbnail_height"));
                    webResource.setWidth(rs.getInt("thumbnail_width"));
                    webResource.setThumbnailMedium(rs.getString("thumbnail_url"));
                }

                return new StoredResult(rs.getInt("rank"), resourceId, webResource, rs.getString("description"),
                    rs.getInt("clicked") > 0, rs.getInt("saved") > 0);
            }).list();

        Set<Integer> resourceIds = results.stream().map(StoredResult::resourceId).filter(id -> id != 0).collect(Collectors.toSet());
        Map<Integer, Resource> resources = resourceIds.isEmpty() ? Map.of() : getHandle().attach(ResourceDao.class).findByIds(resourceIds).stream()
            .collect(Collectors.toMap(Resource::getId, Function.identity()));

        List<ResourceDecorator> decorators = new ArrayList<>();
        for (StoredResult result : results) {
            Resource resource = result.resourceId() == 0 ? result.webResource() : resources.get(result.resourceId());
            if (resource == null) {
                continue; // deleted from Learnweb
            }

            ResourceDecorator rd = new ResourceDecorator(resource);
            rd.setRank(result.rank());
            rd.setSnippet(result.snippet());
            rd.setClicked(result.clicked());
            rd.setSaved(result.saved());
            decorators.add(rd);
        }
        return decorators;
    }

    /**
     * @param query only the sessions with a query containing it, null for all
     * @param username only the sessions of users whose username contains it, null for all
     */
    default List<SearchSession> findSessionsByUserId(int userId, String query, String username) {
        return findSessions("q.user_id = ?", userId, query, username);
    }

    /**
     * The sessions of all group members, see {@link #findSessionsByUserId(int, String, String)} for the filters.
     */
    default List<SearchSession> findSessionsByGroupId(int groupId, String query, String username) {
        return findSessions("q.user_id IN (SELECT user_id FROM lw_group_user WHERE group_id = ?)", groupId, query, username);
    }

    /**
     * Loads the 30 latest sessions with all their queries at once.
     */
    private List<SearchSession> findSessions(String ownerCondition, int ownerId, String query, String username) {
        List<Object> args = new ArrayList<>(List.of(ownerId));
        StringBuilder sessionsQuery = new StringBuilder("SELECT q.user_id, q.session_id, MAX(q.created_at) AS last_query FROM lw_search_history q WHERE ")
            .append(ownerCondition).append(" AND q.session_id IS NOT NULL");
        if (username != null) {
            sessionsQuery.append(" AND q.user_id IN (SELECT user_id FROM lw_user WHERE LOWER(username) LIKE ? " + SqlHelper.LIKE_ESCAPE + ")");
            args.add(SqlHelper.toContainsPattern(username));
        }
        sessionsQuery.append(" GROUP BY q.user_id, q.session_id");
        if (query != null) { // the whole session is shown, if any of its queries matches
            sessionsQuery.append(" HAVING COUNT(CASE WHEN LOWER(q.query) LIKE ? " + SqlHelper.LIKE_ESCAPE + " THEN 1 END) > 0");
            args.add(SqlHelper.toContainsPattern(query));
        }
        sessionsQuery.append(" ORDER BY last_query DESC LIMIT 30");

        SearchHistoryQueryMapper queryMapper = new SearchHistoryQueryMapper();

        Map<String, SearchSession> sessions = getHandle().select("SELECT q.* FROM lw_search_history q JOIN (" + sessionsQuery + ") s "
                + "ON q.user_id = s.user_id AND q.session_id = s.session_id ORDER BY s.last_query DESC, q.user_id, q.session_id, q.created_at ASC", args.toArray())
            .reduceResultSet(new LinkedHashMap<String, SearchSession>(), (acc, rs, ctx) -> {
                int userId = rs.getInt("user_id");
                String sessionId = rs.getString("session_id");
                acc.computeIfAbsent(userId + ":" + sessionId, key -> new SearchSession(sessionId, userId)).addQuery(queryMapper.map(rs, ctx));
                return acc;
            });

        return new ArrayList<>(sessions.values());
    }

    @SqlUpdate("INSERT INTO lw_search_history (query, mode, service, language, filters, user_id, session_id) VALUES (?, ?, ?, ?, ?, ?, ?)")
    @GetGeneratedKeys("search_id")
    int insertQuery(String query, SearchMode searchMode, ResourceService searchService, String language, String searchFilters, User user, String sessionId);

    @SqlUpdate("INSERT INTO lw_search_history_action (search_id, `rank`, action) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE action = action")
    void insertAction(int searchId, int rank, SearchAction action);

    default void insertResources(int searchId, List<SearchHistoryResult> results) {
        if (results.isEmpty()) {
            return;
        }

        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_search_history_resource (search_id, `rank`, resource_id, url, title, description, "
            + "thumbnail_url, thumbnail_height, thumbnail_width) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

        for (SearchHistoryResult result : results) {
            batch.bind(0, searchId);
            batch.bind(1, result.rank());

            if (result.resourceId() != 0) {
                // resource is stored in Learnweb, we do not need to save the title or description
                batch.bind(2, result.resourceId());
                batch.bindNull(3, Types.VARCHAR);
                batch.bindNull(4, Types.VARCHAR);
                batch.bindNull(5, Types.VARCHAR);
                batch.bindNull(6, Types.VARCHAR);
                batch.bindNull(7, Types.INTEGER);
                batch.bindNull(8, Types.INTEGER);
            } else {
                // no learnweb resource -> store title URL and description
                batch.bindNull(2, Types.INTEGER);
                batch.bind(3, result.url());
                batch.bind(4, StringHelper.shortnString(result.title(), 250));
                batch.bind(5, StringHelper.shortnString(result.description(), 1000));
                batch.bind(6, result.thumbnailUrl());
                batch.bind(7, SqlHelper.toNullable(result.thumbnailHeight()));
                batch.bind(8, SqlHelper.toNullable(result.thumbnailWidth()));
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
                ResourceService.parse(rs.getString("service")),
                SqlHelper.getLocalDateTime(rs.getTimestamp("created_at"))
            );
        }
    }
}

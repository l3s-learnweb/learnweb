package de.l3s.learnweb.logging;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.customizer.DefineList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.user.User;
import de.l3s.util.SqlHelper;
import de.l3s.util.StringHelper;

@RegisterRowMapper(LogDao.LogEntryMapper.class)
public interface LogDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_user_log WHERE group_id = :groupId AND resource_id = :resourceId AND action IN(<actionIds>) ORDER BY created_at DESC")
    List<LogEntry> findByGroupIdAndResourceId(@Bind("groupId") int groupId, @Bind("resourceId") int resourceId, @DefineList("actionIds") List<Integer> actionIds);

    /**
     * Returns all logs of the user, except entries of {@link Action#RETIRED_IDS}.
     */
    default List<LogEntry> findAllByUserId(int userId) {
        return findAllByUserId(userId, Action.RETIRED_IDS);
    }

    @SqlQuery("SELECT * FROM lw_user_log WHERE user_id = :userId AND action NOT IN(<retiredIds>) ORDER BY created_at DESC")
    List<LogEntry> findAllByUserId(@Bind("userId") int userId, @BindList("retiredIds") Collection<Integer> retiredIds);

    /**
     * Get public logs of the user. This includes only logs that occurred in a group context.
     */
    @SqlQuery("SELECT * FROM lw_user_log WHERE user_id = :userId AND action IN(<actionIds>) AND group_id != 0 ORDER BY created_at DESC LIMIT :limit")
    List<LogEntry> findPublicByUserId(@Bind("userId") int userId, @DefineList("actionIds") List<Integer> actionIds, @Bind("limit") int limit);

    /**
     * Get logs of the user.
     */
    @SqlQuery("SELECT * FROM lw_user_log WHERE user_id = :userId AND action IN(<actionIds>) ORDER BY created_at DESC LIMIT :limit")
    List<LogEntry> findByUserId(@Bind("userId") int userId, @DefineList("actionIds") List<Integer> actionIds, @Bind("limit") int limit);

    /**
     * Get logs for the given group. All actions that match the default filter will be returned
     */
    @SqlQuery("SELECT * FROM lw_user_log WHERE group_id = :groupId AND user_id != 0 AND action IN(<actionIds>) ORDER BY created_at DESC")
    List<LogEntry> findByGroupId(@Bind("groupId") int groupId, @DefineList("actionIds") List<Integer> actionIds);

    @SqlQuery("SELECT * FROM lw_user_log WHERE group_id = :groupId AND user_id != 0 AND action IN(<actionIds>) ORDER BY created_at DESC LIMIT :limit")
    List<LogEntry> findByGroupId(@Bind("groupId") int groupId, @DefineList("actionIds") List<Integer> actionIds, @Bind("limit") int limit);

    @SqlQuery("SELECT * FROM lw_user_log WHERE group_id = :groupId AND action IN(<actionIds>) AND user_id != 0 AND created_at between :from AND :to ORDER BY created_at DESC")
    List<LogEntry> findByGroupIdBetweenTime(@Bind("groupId") int groupId, @DefineList("actionIds") List<Integer> actionIds, @Bind("from") LocalDateTime from, @Bind("to") LocalDateTime to);

    /**
     * Returns the newest log entries from the user's groups.
     * This doesn't include the user's own actions.
     */
    @SqlQuery("SELECT * FROM lw_user_log WHERE group_id IN(<groupIds>) AND action IN(<actionIds>) AND user_id != 0 AND user_id != :userId ORDER BY created_at DESC LIMIT :limit")
    List<LogEntry> findByUsersGroupIds(@Bind("userId") int userId, @DefineList("groupIds") List<Integer> groupIds, @DefineList("actionIds") List<Integer> actionIds, @Bind("limit") int limit);

    @SqlQuery("SELECT created_at FROM lw_user_log WHERE user_id = ? AND action = ? ORDER BY created_at DESC LIMIT 1")
    Optional<LocalDateTime> findDateOfLastByUserIdAndAction(int userId, Action action);

    @SqlQuery("SELECT action, COUNT(*) AS count FROM lw_user_log WHERE user_id IN(<userIds>) AND created_at BETWEEN :start AND :end GROUP BY action")
    @KeyColumn("action")
    @ValueColumn("count")
    Map<Integer, Integer> countUsagePerAction(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @SqlQuery("SELECT DATE(created_at) AS `day`, COUNT(*) AS `count` FROM lw_user_log WHERE user_id IN(<userIds>) AND created_at BETWEEN :start AND :end GROUP BY `day`")
    @KeyColumn("day")
    @ValueColumn("count")
    Map<String, Integer> countActionsPerDay(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @SqlQuery("SELECT DATE(created_at) as `day`, COUNT(*) AS `count` FROM lw_user_log WHERE user_id IN(<userIds>) AND created_at BETWEEN :start AND :end AND action in (<actions>) GROUP BY `day`")
    @KeyColumn("day")
    @ValueColumn("count")
    Map<String, Integer> countActionsPerDay(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate, @Define("actions") String actions);

    /**
     * Logs the event with its performer, session and all context ids. Context ids of 0 are stored as NULL.
     */
    default void insert(ActivityEvent event) {
        User user = event.getPerformer();

        if (null == event.getAction()) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        getHandle().createUpdate("""
                INSERT INTO lw_user_log (user_id, session_id, action, group_id, resource_id, folder_id, topic_id, post_id, course_id, target_user_id,
                    search_id, params, created_at)
                VALUES (:userId, :sessionId, :action, NULLIF(:groupId, 0), NULLIF(:resourceId, 0), NULLIF(:folderId, 0), NULLIF(:topicId, 0),
                    NULLIF(:postId, 0), NULLIF(:courseId, 0), NULLIF(:targetUserId, 0), NULLIF(:searchId, 0), :params, CURRENT_TIMESTAMP)
                """)
            .bindBean(event)
            .bind("userId", user == null ? 0 : user.getId())
            .bind("action", event.getAction())
            .bind("params", StringHelper.shortnString(event.getParams(), 250))
            .execute();
    }

    @SqlBatch("INSERT INTO lw_user_log_action (action, name, category) VALUES (:getId, :name, :getCategory)")
    void insertUserLogAction(@BindMethods Action... actions);

    @SuppressWarnings("SqlWithoutWhere")
    @SqlUpdate("DELETE FROM lw_user_log_action")
    void truncateUserLogAction();

    class LogEntryMapper implements RowMapper<LogEntry> {
        @Override
        public LogEntry map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            LogEntry entry = new LogEntry(
                rs.getInt("user_id"),
                Action.findByIdOrElseThrow(rs.getInt("action")),
                SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")),
                rs.getString("params"));
            entry.setGroupId(rs.getInt("group_id"));
            entry.setResourceId(rs.getInt("resource_id"));
            entry.setFolderId(rs.getInt("folder_id"));
            entry.setTopicId(rs.getInt("topic_id"));
            entry.setPostId(rs.getInt("post_id"));
            entry.setCourseId(rs.getInt("course_id"));
            entry.setTargetUserId(rs.getInt("target_user_id"));
            entry.setSearchId(rs.getInt("search_id"));
            return entry;
        }
    }
}

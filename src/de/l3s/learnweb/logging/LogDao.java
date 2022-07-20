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

import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;
import de.l3s.util.SqlHelper;
import de.l3s.util.StringHelper;

@RegisterRowMapper(LogDao.LogEntryMapper.class)
public interface LogDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_user_log WHERE group_id = :groupId AND target_id = :targetId AND action IN(<actionIds>) ORDER BY created_at DESC")
    List<LogEntry> findByGroupIdAndTargetId(@Bind("groupId") int groupId, @Bind("targetId") int targetId, @DefineList("actionIds") List<Integer> actionIds);

    @SqlQuery("SELECT * FROM lw_user_log WHERE user_id = ? ORDER BY created_at DESC")
    List<LogEntry> findAllByUserId(int userId);

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
    Optional<LocalDateTime> findDateOfLastByUserIdAndAction(int userId, int actionOrdinal);

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
     * Logs a user action. The parameters "targetId" and "params" depend on the logged action.
     * Look at the code of {@link de.l3s.learnweb.logging.Action} for explanation.
     *
     * @param groupId the group this action belongs to; null if no group
     * @param targetId optional value; should be 0 if not required
     */
    default void insert(User user, Action action, Integer groupId, Integer targetId, String params, String sessionId) {
        int userId = 0;
        if (user != null) {
            userId = user.getId();

            if (user.getOrganisation().getOption(Organisation.Option.Privacy_Logging_disabled)) {
                return; // we are not allowed to log events for this user
            }
        }

        if (null == action) {
            throw new IllegalArgumentException();
        }

        params = StringHelper.shortnString(params, 250);

        if (groupId != null && groupId == 0) {
            groupId = null;
        }

        getHandle().createUpdate("INSERT INTO lw_user_log (user_id, session_id, action, target_id, params, group_id, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)")
            .bind(0, userId)
            .bind(1, sessionId)
            .bind(2, action)
            .bind(3, targetId)
            .bind(4, params)
            .bind(5, groupId)
            .execute();
    }

    @SqlBatch("INSERT INTO lw_user_log_action (action, name, target, category) VALUES (:ordinal, :name, :getTargetId, :getCategory)")
    void insertUserLogAction(@BindMethods Action... actions);

    @SuppressWarnings("SqlWithoutWhere")
    @SqlUpdate("DELETE FROM lw_user_log_action")
    void truncateUserLogAction();

    class LogEntryMapper implements RowMapper<LogEntry> {
        @Override
        public LogEntry map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            // int logEntryId = rs.getInt("log_entry_id");
            int userId = rs.getInt("user_id");
            // String sessionId = rs.getString("session_id");
            Action action = Action.values()[rs.getInt("action")];
            LocalDateTime dateTime = SqlHelper.getLocalDateTime(rs.getTimestamp("created_at"));
            String params = rs.getString("params");
            int groupId = rs.getInt("group_id");
            int targetId = rs.getInt("target_id");
            return new LogEntry(userId, action, dateTime, params, groupId, targetId);
        }
    }
}

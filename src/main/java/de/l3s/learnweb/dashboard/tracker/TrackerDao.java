package de.l3s.learnweb.dashboard.tracker;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface TrackerDao extends SqlObject, Serializable {

    @SqlQuery("SELECT url_domain, COUNT(*) AS count FROM tracker.track WHERE status = 'PROCESSED' AND client_id = :clientId AND external_user_id IN(<userIds>) "
        + "AND created_at BETWEEN :start AND :end GROUP BY url_domain ORDER BY count DESC")
    @KeyColumn("url_domain")
    @ValueColumn("count")
    Map<String, Integer> countUsagePerDomain(@Bind("clientId") int trackerClientId, @BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @RegisterRowMapper(TrackerStatisticMapper.class)
    @SqlQuery("SELECT external_user_id AS user_id, sum(total_events) AS total_events, sum(time_stay) AS time_stay, sum(time_active) AS time_active, "
        + "sum(clicks) AS clicks, sum(keypress) AS keypresses FROM tracker.track WHERE status = 'PROCESSED' AND client_id = :clientId "
        + "AND external_user_id IN(<userIds>) AND created_at BETWEEN :start AND :end GROUP BY external_user_id")
    List<TrackerUserActivity> countTrackerStatistics(@Bind("clientId") int trackerClientId, @BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    class TrackerStatisticMapper implements RowMapper<TrackerUserActivity> {
        @Override
        public TrackerUserActivity map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            TrackerUserActivity result = new TrackerUserActivity();
            result.setUserId(rs.getInt("user_id"));
            result.setTotalEvents(rs.getInt("total_events"));
            result.setTimeStay(rs.getLong("time_stay"));
            result.setTimeActive(rs.getLong("time_active"));
            result.setClicks(rs.getInt("clicks"));
            result.setKeyPresses(rs.getInt("keypresses"));
            return result;
        }
    }
}

package de.l3s.learnweb.dashboard.tracker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface TrackerDao extends SqlObject {

    @SqlQuery("SELECT url_domain, COUNT(*) AS count FROM tracker.track WHERE status = 'PROCESSED' AND client_id = ? AND external_user_id IN(<userIds>) "
        + "AND created_at BETWEEN ? AND ? group by url_domain order by total_records desc")
    @KeyColumn("url_domain")
    @ValueColumn("count")
    Map<String, Integer> countUsagePerDomain(int trackerClientId, @BindList("userIds") Collection<Integer> userIds, Date startDate, Date endDate);

    @RegisterRowMapper(TrackerStatisticMapper.class)
    @SqlQuery("SELECT external_user_id AS user_id, sum(total_events) AS total_events, sum(time_stay) AS time_stay, sum(time_active) AS time_active, "
        + "sum(clicks) AS clicks, sum(keypress) AS keypresses FROM tracker.track WHERE status = 'PROCESSED' AND client_id = ? "
        + "AND external_user_id IN(<userIds>) AND created_at BETWEEN ? AND ? group by external_user_id")
    List<TrackerUserActivity> countTrackerStatistics(int trackerClientId, @BindList("userIds") Collection<Integer> userIds, Date startDate, Date endDate);

    class TrackerStatisticMapper implements RowMapper<TrackerUserActivity> {
        @Override
        public TrackerUserActivity map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            TrackerUserActivity result = new TrackerUserActivity();
            result.setUserId(rs.getInt("user_id"));
            result.setTotalEvents(rs.getInt("total_events"));
            result.setTimeStay(rs.getInt("time_stay"));
            result.setTimeActive(rs.getInt("time_active"));
            result.setClicks(rs.getInt("clicks"));
            result.setKeyPresses(rs.getInt("keypresses"));
            return result;
        }
    }
}

package de.l3s.learnweb.dashboard.activity;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.jdbi.v3.core.Handle;

import de.l3s.learnweb.Learnweb;

public class ActivityDashboardManager {
    private final Learnweb learnweb;

    public ActivityDashboardManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public Map<String, Integer> getActionsCountPerDay(Collection<Integer> userIds, Date startDate, Date endDate, String actions) throws SQLException {
        Map<String, Integer> actionsPerDay = new TreeMap<>();

        try (Handle handle = learnweb.openHandle()) {
            handle.select("SELECT DATE(timestamp) as day, COUNT(*) AS count FROM lw_user_log "
                + "WHERE user_id IN(<userIds>) AND timestamp BETWEEN :start AND :end AND action in (:actions) GROUP BY day;")
                .bind("start", startDate)
                .bind("end", endDate)
                .bind("actions", actions)
                .bindList("userIds", userIds)
                .map((rs, ctx) -> actionsPerDay.put(rs.getString("day"), rs.getInt("count")));
        }

        return actionsPerDay;
    }
}

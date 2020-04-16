package de.l3s.learnweb.dashboard.tracker;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.StringHelper;

class TrackerDashboardManager
{
    private static final Logger log = LogManager.getLogger(TrackerDashboardManager.class);

    private final Learnweb learnweb;

    TrackerDashboardManager()
    {
        this.learnweb = Learnweb.getInstance();
    }

    Map<String, Integer> getProxySourcesWithCounters(int trackerClientId, Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<String, Integer> countPerSource = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT url_domain, COUNT(*) AS total_records "
                        + "FROM tracker.track "
                        + "WHERE status = 'PROCESSED' AND client_id = ? "
                        + "AND external_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND created_at BETWEEN ? AND ? group by url_domain order by total_records desc"))
        {
            select.setInt(1, trackerClientId);
            select.setTimestamp(2, new Timestamp(startDate.getTime()));
            select.setTimestamp(3, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                String domain = rs.getString("url_domain");
                if(domain == null)
                {
                    log.warn("skip null domain");
                    continue;
                }
                countPerSource.put(domain, rs.getInt("total_records"));
            }
        }

        return countPerSource;
    }

    LinkedList<TrackerUserActivity> getTrackerStatistics(int trackerClientId, Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        LinkedList<TrackerUserActivity> statistic = new LinkedList<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT external_user_id AS user_id, sum(total_events) AS total_events, sum(time_stay) AS time_stay, sum(time_active) AS time_active, sum(clicks) AS clicks, sum(keypress) AS keypresses "
                        + "FROM tracker.track "
                        + "WHERE status = 'PROCESSED' AND client_id = ? "
                        + "AND external_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND created_at BETWEEN ? AND ? group by external_user_id"))
        {
            select.setInt(1, trackerClientId);
            select.setTimestamp(2, new Timestamp(startDate.getTime()));
            select.setTimestamp(3, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                TrackerUserActivity trackerStatistic = new TrackerUserActivity();
                trackerStatistic.setUserId(rs.getInt("user_id"));
                trackerStatistic.setTotalEvents(rs.getInt("total_events"));
                trackerStatistic.setTimeStay(rs.getInt("time_stay"));
                trackerStatistic.setTimeActive(rs.getInt("time_active"));
                trackerStatistic.setClicks(rs.getInt("clicks"));
                trackerStatistic.setKeyPresses(rs.getInt("keypresses"));
                statistic.add(trackerStatistic);
            }
        }

        return statistic;
    }
}

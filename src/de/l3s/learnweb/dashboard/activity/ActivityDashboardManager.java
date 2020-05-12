package de.l3s.learnweb.dashboard.activity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.StringHelper;

public class ActivityDashboardManager
{
    private final Learnweb learnweb;

    public ActivityDashboardManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public Map<String, Integer> getActionsCountPerDay(Collection<Integer> userIds, Date startDate, Date endDate, String actions) throws SQLException
    {
        // action name, count
        Map<String, Integer> actionsPerDay = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT DATE(timestamp) as day, COUNT(*) AS count FROM lw_user_log " +
                        "WHERE user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND timestamp BETWEEN ? AND ? AND action in(" + actions + ") GROUP BY day"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));

            ResultSet rs = select.executeQuery();

            while(rs.next())
                actionsPerDay.put(rs.getString("day"), rs.getInt("count"));
        }

        return actionsPerDay;
    }
}

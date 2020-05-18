package de.l3s.learnweb.dashboard.activity;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.l3s.learnweb.Learnweb;

@Disabled
class ActivityDashboardManagerTest {
    private final ActivityDashboardManager dashboardManager = Learnweb.createInstance().getActivityDashboardManager();

    ActivityDashboardManagerTest() throws SQLException, ClassNotFoundException {}

    @Test
    void getActionsCountPerDay() throws ParseException, SQLException {
        Map<String, Integer> expected = new TreeMap<>();
        expected.put("2019-02-23", 1);
        expected.put("2019-02-25", 1);
        expected.put("2019-05-19", 1);
        expected.put("2019-06-02", 1);
        expected.put("2019-06-05", 1);
        expected.put("2019-06-22", 2);
        expected.put("2019-07-09", 1);
        expected.put("2019-07-22", 1);
        expected.put("2019-07-30", 1);
        expected.put("2019-10-04", 2);
        expected.put("2019-10-10", 12);
        expected.put("2019-10-13", 7);
        expected.put("2019-10-15", 2);
        expected.put("2019-10-16", 4);
        expected.put("2019-10-28", 1);

        ArrayList<Integer> users = new ArrayList<>(Arrays.asList(12502, 12600, 11700));
        Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-20");
        Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-20");
        String actions = "4, 1, 2";
        Map<String, Integer> result = dashboardManager.getActionsCountPerDay(users, startDate, endDate, actions);

        assertEquals(expected, result);
    }
}

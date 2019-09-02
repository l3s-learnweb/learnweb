package de.l3s.learnweb.dashboard.tracker;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.util.MapHelper;
import de.l3s.util.Misc;

@Named
@ViewScoped
public class TrackerDashboardBean extends CommonDashboardUserBean implements Serializable
{
    private static final long serialVersionUID = 3640317272542005280L;
    //private static final Logger log = Logger.getLogger(TrackerDashboardBean.class);

    private static final int TRACKER_CLIENT_ID = 2;

    private TrackerDashboardManager dashboardManager;

    private Map<String, Integer> proxySourcesWithCounters;
    private LinkedList<TrackerUserActivity> trackerStatistics;

    public void onLoad()
    {
        super.onLoad();

        try
        {
            dashboardManager = new TrackerDashboardManager();
            cleanAndUpdateStoredData();
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void cleanAndUpdateStoredData() throws SQLException
    {
        trackerStatistics = null;
        proxySourcesWithCounters = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        if(!Misc.nullOrEmpty(getSelectedUsersIds())) {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            trackerStatistics = dashboardManager.getTrackerStatistics(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
            proxySourcesWithCounters = dashboardManager.getProxySourcesWithCounters(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
        }
    }

    public ArrayList<Map.Entry<String, Integer>> getUsersProxySourcesList()
    {
        if (proxySourcesWithCounters == null) {
            return null;
        }

        return new ArrayList<>(MapHelper.sortByValue(proxySourcesWithCounters).entrySet());
    }

    public LinkedList<TrackerUserActivity> getTrackerStatistics()
    {
        return trackerStatistics;
    }
}

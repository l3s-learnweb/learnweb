package de.l3s.learnweb.dashboard.tracker;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.util.MapHelper;

@Named
@ViewScoped
public class TrackerDashboardBean extends CommonDashboardUserBean implements Serializable
{
    private static final long serialVersionUID = 3640317272542005280L;
    //private static final Logger log = LogManager.getLogger(TrackerDashboardBean.class);

    private static final int TRACKER_CLIENT_ID = 2;

    private Map<String, Integer> proxySourcesWithCounters;
    private LinkedList<TrackerUserActivity> trackerStatistics;

    @Override
    public void onLoad()
    {
        super.onLoad();

        try
        {
            cleanAndUpdateStoredData();
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    @Override
    public void cleanAndUpdateStoredData() throws SQLException
    {
        trackerStatistics = null;
        proxySourcesWithCounters = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        if(!CollectionUtils.isEmpty(getSelectedUsersIds()))
        {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            trackerStatistics = getLearnweb().getTrackerDashboardManager().getTrackerStatistics(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
            proxySourcesWithCounters = getLearnweb().getTrackerDashboardManager().getProxySourcesWithCounters(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
        }
    }

    public ArrayList<Map.Entry<String, Integer>> getUsersProxySourcesList() throws SQLException
    {
        return new ArrayList<>(MapHelper.sortByValue(proxySourcesWithCounters).entrySet());
    }

    public LinkedList<TrackerUserActivity> getTrackerStatistics() throws SQLException
    {
        return trackerStatistics;
    }
}

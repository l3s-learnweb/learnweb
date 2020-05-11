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

    private transient TrackerDashboardManager dashboardManager;
    private transient Map<String, Integer> proxySourcesWithCounters;
    private transient LinkedList<TrackerUserActivity> trackerStatistics;

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
    
    private TrackerDashboardManager getDashboardManager()
    {
        if(dashboardManager == null)
            dashboardManager = new TrackerDashboardManager();
        
        return dashboardManager;
    }

    private void fetchDataFromManager() throws SQLException
    {
        if(!CollectionUtils.isEmpty(getSelectedUsersIds()))
        {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            trackerStatistics = getDashboardManager().getTrackerStatistics(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
            proxySourcesWithCounters = getDashboardManager().getProxySourcesWithCounters(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
        }
    }

    public ArrayList<Map.Entry<String, Integer>> getUsersProxySourcesList() throws SQLException
    {
        if(proxySourcesWithCounters == null)
            fetchDataFromManager();

        if(proxySourcesWithCounters.isEmpty())
            return null;

        return new ArrayList<>(MapHelper.sortByValue(proxySourcesWithCounters).entrySet());
    }

    public LinkedList<TrackerUserActivity> getTrackerStatistics() throws SQLException
    {
        if (trackerStatistics == null)
            fetchDataFromManager();

        if(trackerStatistics.isEmpty())
            return null;

        return trackerStatistics;
    }
}

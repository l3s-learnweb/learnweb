package de.l3s.learnweb.dashboard.tracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.collections4.CollectionUtils;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.util.MapHelper;

@Named
@ViewScoped
public class TrackerDashboardBean extends CommonDashboardUserBean implements Serializable {
    private static final long serialVersionUID = 3640317272542005280L;
    //private static final Logger log = LogManager.getLogger(TrackerDashboardBean.class);

    private static final int TRACKER_CLIENT_ID = 2;

    private TrackerDao trackerDao;
    private Map<String, Integer> proxySourcesWithCounters;
    private List<TrackerUserActivity> trackerStatistics;

    @Override
    public void onLoad() {
        super.onLoad();

        trackerDao = dao().getJdbi().onDemand(TrackerDao.class);
        cleanAndUpdateStoredData();
    }

    @Override
    public void cleanAndUpdateStoredData() {
        trackerStatistics = null;
        proxySourcesWithCounters = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() {
        if (!CollectionUtils.isEmpty(getSelectedUsersIds())) {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            trackerStatistics = trackerDao.countTrackerStatistics(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
            proxySourcesWithCounters = trackerDao.countUsagePerDomain(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
        }
    }

    public List<Map.Entry<String, Integer>> getUsersProxySourcesList() {
        return new ArrayList<>(MapHelper.sortByValue(proxySourcesWithCounters).entrySet());
    }

    public List<TrackerUserActivity> getTrackerStatistics() {
        return trackerStatistics;
    }
}

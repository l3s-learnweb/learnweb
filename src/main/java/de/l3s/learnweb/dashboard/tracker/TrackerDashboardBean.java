package de.l3s.learnweb.dashboard.tracker;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.collections4.CollectionUtils;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;

@Named
@ViewScoped
public class TrackerDashboardBean extends CommonDashboardUserBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 3640317272542005280L;
    //private static final Logger log = LogManager.getLogger(TrackerDashboardBean.class);

    private static final int TRACKER_CLIENT_ID = 2;

    private TrackerDao trackerDao;
    private Map<String, Integer> proxySources;
    private List<TrackerUserActivity> statistics;

    @Override
    public void onLoad() {
        super.onLoad();

        trackerDao = dao().getJdbi().onDemand(TrackerDao.class);
        cleanAndUpdateStoredData();
    }

    @Override
    public void cleanAndUpdateStoredData() {
        statistics = null;
        proxySources = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() {
        if (!CollectionUtils.isEmpty(getSelectedUsersIds())) {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            statistics = trackerDao.countTrackerStatistics(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
            proxySources = trackerDao.countUsagePerDomain(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
        }
    }

    public List<TrackerUserActivity> getStatistics() {
        return statistics;
    }

    public Map<String, Integer> getProxySources() {
        return proxySources;
    }
}

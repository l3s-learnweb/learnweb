package de.l3s.learnweb.dashboard;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryFieldSummery;
import de.l3s.util.MapHelper;
import org.apache.log4j.Logger;
import org.primefaces.model.chart.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;

@ManagedBean
@ViewScoped
public class DashboardUserBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073418345L;
    private static final Logger log = Logger.getLogger(DashboardUserBean.class);

    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";
    private static final String TRACKER_CLIENT_ID = "1";

    private Integer paramUserId = null;

    private Date startDate = null;
    private Date endDate = null;
    private List<Integer> selectedUsersIds = null;
    private DashboardManager dashboardManager = null;

    private Integer totalConcepts = null;
    private Integer totalTerms = null;
    private Integer totalSources = null;
    private ArrayList<GlossaryFieldSummery> glossaryFieldsSummeryPerUser;
    private Map<String, Integer> glossarySourcesWithCounters;
    private Map<Integer, Integer> actionsWithCounters;
    private Map<String, Integer> actionsCountPerDay;
    private ArrayList<DashboardManager.DescFieldData> descFieldsStatistic;
    private ArrayList<String> glossaryDescriptions;
    private Map<String, Integer> proxySourcesWithCounters;
    private LinkedList<DashboardManager.TrackerStatistic> trackerStatistics;

    private LineChartModel interactionsChart;
    private BarChartModel usersActivityTypesChart;
    private PieChartModel usersSourcesChart;
    private BarChartModel userFieldsChart;
    private BarChartModel proxySourcesChart;

    public DashboardUserBean()
    {
    }

    public void onLoad()
    {
        log.debug("onLoad");
        User user = getUser(); // the current user
        if(user == null || (!user.isModerator() && paramUserId != null && paramUserId != user.getId())) // not logged in or no privileges
            return;

        try
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);

            String savedStartDate = getPreference(PREFERENCE_STARTDATE, Long.toString(cal.getTimeInMillis())); // month ago
            String savedEndDate = getPreference(PREFERENCE_ENDDATE, Long.toString(new Date().getTime()));
            startDate = new Date(Long.parseLong(savedStartDate));
            endDate = new Date(Long.parseLong(savedEndDate));

            selectedUsersIds = Collections.singletonList(paramUserId == null ? user.getId() : paramUserId);
            dashboardManager = getLearnweb().getDashboardManager();

            fetchDataFromManager();
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void cleanAndUpdateStoredData() throws SQLException
    {
        interactionsChart = null;
        usersActivityTypesChart = null;
        usersSourcesChart = null;
        userFieldsChart = null;
        proxySourcesChart = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        totalConcepts = dashboardManager.getTotalConcepts(selectedUsersIds, startDate, endDate);
        totalTerms = dashboardManager.getTotalTerms(selectedUsersIds, startDate, endDate);
        totalSources = dashboardManager.getTotalSources(selectedUsersIds, startDate, endDate);

        glossaryFieldsSummeryPerUser = dashboardManager.getGlossaryFieldSummeryPerUser(selectedUsersIds, startDate, endDate);
        glossarySourcesWithCounters = dashboardManager.getGlossarySourcesWithCounters(selectedUsersIds, startDate, endDate);
        actionsWithCounters = dashboardManager.getActionsWithCounters(selectedUsersIds, startDate, endDate);
        actionsCountPerDay = dashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate);
        glossaryDescriptions = dashboardManager.getGlossaryDescriptions(selectedUsersIds, startDate, endDate);
        trackerStatistics = dashboardManager.getTrackerStatistics(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
        descFieldsStatistic = dashboardManager.getLangDescStatistic(selectedUsersIds, startDate, endDate);
        proxySourcesWithCounters = dashboardManager.getProxySourcesWithCounters(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;

        setPreference(PREFERENCE_STARTDATE, Long.toString(startDate.getTime()));
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;

        setPreference(PREFERENCE_ENDDATE, Long.toString(endDate.getTime()));
    }

    public LineChartModel getInteractionsChart()
    {
        if(interactionsChart == null)
            interactionsChart = DashboardChartsFactory.createInteractionsChart(actionsCountPerDay, startDate, endDate);
        return interactionsChart;
    }

    public BarChartModel getUserFieldsChart()
    {
        if(userFieldsChart == null)
            userFieldsChart = DashboardChartsFactory.createUserFieldsChart(glossaryFieldsSummeryPerUser);
        return userFieldsChart;
    }

    public BarChartModel getUsersActivityTypesChart()
    {
        if(usersActivityTypesChart == null)
            usersActivityTypesChart = DashboardChartsFactory.createActivityTypesChart(actionsWithCounters);
        return usersActivityTypesChart;
    }

    public BarChartModel getProxySourcesChart()
    {
        if(proxySourcesChart == null)
            proxySourcesChart = DashboardChartsFactory.createProxySourcesChart(proxySourcesWithCounters);
        return proxySourcesChart;
    }

    public PieChartModel getUsersSourcesChart()
    {
        if(usersSourcesChart == null)
            usersSourcesChart = DashboardChartsFactory.createUsersSourcesChart(glossarySourcesWithCounters);
        return usersSourcesChart;
    }

    public Integer getTotalConcepts()
    {
        return totalConcepts;
    }

    public Integer getTotalTerms()
    {
        return totalTerms;
    }

    public Integer getTotalSources()
    {
        return totalSources;
    }

    public ArrayList<Map.Entry<String, Integer>> getUsersProxySourcesList()
    {
        return new ArrayList<>(MapHelper.sortByValue(proxySourcesWithCounters).entrySet());
    }

    public List<String> getUsersGlossaryDescriptions()
    {
        return glossaryDescriptions;
    }

    public DashboardManager.TrackerStatistic getTrackerStatistic()
    {
        if (trackerStatistics.isEmpty()) {
            return new DashboardManager.TrackerStatistic();
        }

        return trackerStatistics.get(0);
    }

    public Integer getParamUserId()
    {
        return paramUserId;
    }

    public void setParamUserId(final Integer paramUserId)
    {
        this.paramUserId = paramUserId;
    }

    public String getRatioTermConcept()
    {
        float res = (float) totalTerms / totalConcepts;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return twoDForm.format(res);
    }
}

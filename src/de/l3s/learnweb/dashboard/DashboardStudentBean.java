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
public class DashboardStudentBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073418345L;
    private static final Logger log = Logger.getLogger(DashboardStudentBean.class);

    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";
    private static final String TRACKER_CLIENT_ID = "1";

    private Integer paramStudentId = null;

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
    private BarChartModel studentsActivityTypesChart;
    private PieChartModel studentsSourcesChart;
    private BarChartModel studentFieldsChart;
    private BarChartModel proxySourcesChart;

    public DashboardStudentBean()
    {
    }

    public void onLoad()
    {
        log.debug("onLoad");
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        try
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);

            String savedStartDate = getPreference(PREFERENCE_STARTDATE, Long.toString(cal.getTimeInMillis())); // month ago
            String savedEndDate = getPreference(PREFERENCE_ENDDATE, Long.toString(new Date().getTime()));
            startDate = new Date(Long.parseLong(savedStartDate));
            endDate = new Date(Long.parseLong(savedEndDate));

            selectedUsersIds = Collections.singletonList(paramStudentId);
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
        studentsActivityTypesChart = null;
        studentsSourcesChart = null;
        studentFieldsChart = null;
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
        // TODO: display them somewhere on the user stat page
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

    public BarChartModel getStudentFieldsChart()
    {
        if(studentFieldsChart == null)
            studentFieldsChart = DashboardChartsFactory.createStudentFieldsChart(glossaryFieldsSummeryPerUser);
        return studentFieldsChart;
    }

    public BarChartModel getStudentsActivityTypesChart()
    {
        if(studentsActivityTypesChart == null)
            studentsActivityTypesChart = DashboardChartsFactory.createActivityTypesChart(actionsWithCounters);
        return studentsActivityTypesChart;
    }

    public BarChartModel getProxySourcesChart()
    {
        if(proxySourcesChart == null)
            proxySourcesChart = DashboardChartsFactory.createProxySourcesChart(proxySourcesWithCounters);
        return proxySourcesChart;
    }

    public PieChartModel getStudentsSourcesChart()
    {
        if(studentsSourcesChart == null)
            studentsSourcesChart = DashboardChartsFactory.createStudentsSourcesChart(glossarySourcesWithCounters);
        return studentsSourcesChart;
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

    public ArrayList<Map.Entry<String, Integer>> getStudentsProxySourcesList()
    {
        return new ArrayList<>(MapHelper.sortByValue(proxySourcesWithCounters).entrySet());
    }

    public List<String> getStudentsGlossaryDescriptions()
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

    public Integer getParamStudentId()
    {
        return paramStudentId;
    }

    public void setParamStudentId(final Integer paramStudentId)
    {
        this.paramStudentId = paramStudentId;
    }

    public String getRatioTermConcept()
    {
        float res = (float) totalTerms / totalConcepts;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return twoDForm.format(res);
    }
}

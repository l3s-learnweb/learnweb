package de.l3s.learnweb.dashboard;

import de.l3s.learnweb.Organisation;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryFieldSummery;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryStatistic;
import de.l3s.learnweb.dashboard.DashboardManager.TrackerStatistic;
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

    private Date startDate = null;
    private Date endDate = null;
    private Integer selectedUserId = null;

    private String trackerClientId = "1";

    private List<Integer> orgUserIds = null;
    private DashboardManager dashboardManager = null;

    private Integer totalConcepts = null;
    private Integer totalTerms = null;
    private Integer totalSources = null;
    private List<GlossaryFieldSummery> glossaryFieldSummeryPerUser;
    private List<TrackerStatistic> trackerStatistics;
    private List<String> glossaryDescriptions;
    private Map<String, Integer> glossaryConceptsCountPerUser;
    private Map<String, Integer> glossarySourcesWithCounters;
    private Map<String, Integer> glossaryTermsCountPerUser;
    private Map<Integer, Integer> actionsWithCounters;
    private Map<String, Integer> actionsCountPerDay;
    private Map<String, Integer> proxySourcesWithCounters;
    private Map<Integer, GlossaryStatistic> glossaryStatisticPerUser;

    private LineChartModel interactionsChart;
    private BarChartModel studentsActivityTypesChart;
    private BarChartModel studentsGlossary;
    private BarChartModel proxySources;
    private BarChartModel studentFields;
    private PieChartModel studentsSources;
    private ArrayList<GlossaryStatistic> glossaryStat;

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

            orgUserIds = Collections.singletonList(selectedUserId);
            dashboardManager = getLearnweb().getDashboardManager();

            updateStatsData();
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    private void updateStatsData() throws SQLException
    {
        totalConcepts = dashboardManager.getTotalConcepts(orgUserIds, startDate, endDate);
        totalTerms = dashboardManager.getTotalTerms(orgUserIds, startDate, endDate);
        totalSources = dashboardManager.getTotalSources(orgUserIds, startDate, endDate);
        glossaryFieldSummeryPerUser = dashboardManager.getGlossaryFieldSummeryPerUser(orgUserIds, startDate, endDate);
        trackerStatistics = dashboardManager.getTrackerStatistics(trackerClientId, orgUserIds, startDate, endDate);
        glossaryDescriptions = dashboardManager.getGlossaryDescriptions(orgUserIds, startDate, endDate);
        glossaryConceptsCountPerUser = dashboardManager.getGlossaryConceptsCountPerUser(orgUserIds, startDate, endDate);
        glossarySourcesWithCounters = dashboardManager.getGlossarySourcesWithCounters(orgUserIds, startDate, endDate);
        glossaryTermsCountPerUser = dashboardManager.getGlossaryTermsCountPerUser(orgUserIds, startDate, endDate);
        actionsWithCounters = dashboardManager.getActionsWithCounters(orgUserIds, startDate, endDate);
        actionsCountPerDay = dashboardManager.getActionsCountPerDay(orgUserIds, startDate, endDate);
        proxySourcesWithCounters = dashboardManager.getProxySourcesWithCounters(trackerClientId, orgUserIds, startDate, endDate);
        glossaryStatisticPerUser = dashboardManager.getGlossaryStatisticPerUser(orgUserIds, startDate, endDate);
    }

    public void onDateChanged() throws SQLException
    {
        updateStatsData();

        interactionsChart = null;
        studentFields = null;
        studentsActivityTypesChart = null;
        studentsGlossary = null;
        studentsSources = null;
        glossaryStat = null;
        proxySources = null;
    }

    public LineChartModel getInteractionsChart()
    {
        if(interactionsChart == null)
            interactionsChart = DashboardChartsFactory.createInteractionsChart(actionsCountPerDay, this.startDate, this.endDate);
        return interactionsChart;
    }

    public BarChartModel getStudentFields()
    {
        if(studentFields == null)
            studentFields = DashboardChartsFactory.createStudentFieldsChart(glossaryFieldSummeryPerUser);
        return studentFields;
    }

    public BarChartModel getStudentsActivityTypesChart()
    {
        if(studentsActivityTypesChart == null)
            studentsActivityTypesChart = DashboardChartsFactory.createActivityTypesChart(actionsWithCounters);
        return studentsActivityTypesChart;
    }

    public BarChartModel getStudentsGlossary()
    {
        if(studentsGlossary == null)
            studentsGlossary = DashboardChartsFactory.createStudentsGlossaryChart(glossaryConceptsCountPerUser, glossaryTermsCountPerUser);
        return studentsGlossary;
    }

    public BarChartModel getProxySources()
    {
        if(proxySources == null)
            proxySources = DashboardChartsFactory.createProxySourcesChart(proxySourcesWithCounters);
        return proxySources;
    }

    public PieChartModel getStudentsSources()
    {
        if(studentsSources == null)
            studentsSources = DashboardChartsFactory.createStudentsSourcesChart();
        return studentsSources;
    }

    public ArrayList<GlossaryStatistic> getSummary()
    {
        if(glossaryStat == null)
        {
            glossaryStat = new ArrayList<>();
            for(Integer uid : orgUserIds)
            {
                GlossaryStatistic glossaryStatistic =
                        glossaryStatisticPerUser.getOrDefault(uid, new GlossaryStatistic());
                glossaryStatistic.setUserId(uid);
                glossaryStat.add(glossaryStatistic);
            }
        }
        return glossaryStat;
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

    public Map<String, Integer> getProxyLogList()
    {
        return MapHelper.sortByValue(proxySourcesWithCounters);
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

    public void setTotalSources(Integer totalSources)
    {
        this.totalSources = totalSources;
    }

    public Map<String, Integer> getGlossaryConceptsCountPerUser()
    {
        return glossaryConceptsCountPerUser;
    }

    public Map<String, Integer> getGlossarySourcesWithCounters()
    {
        return glossarySourcesWithCounters;
    }

    public Map<String, Integer> getGlossaryTermsCountPerUser()
    {
        return glossaryTermsCountPerUser;
    }

    public Map<Integer, Integer> getActionsWithCounters()
    {
        return actionsWithCounters;
    }

    public Map<String, Integer> getActionsCountPerDay()
    {
        return actionsCountPerDay;
    }

    public List<String> getGlossaryDescriptions()
    {
        return glossaryDescriptions;
    }

    public Map<Integer, GlossaryStatistic> getGlossaryStatisticPerUser()
    {
        return glossaryStatisticPerUser;
    }

    public Map<String, Integer> getProxySourcesWithCounters()
    {
        return proxySourcesWithCounters;
    }

    public List<TrackerStatistic> getTrackerStatistics()
    {
        return trackerStatistics;
    }

    public Organisation getOrganization()
    {
        return getUser().getOrganisation();
    }

    public Integer getSelectedUserId()
    {
        return selectedUserId;
    }

    public void setSelectedUserId(Integer selectedUserId)
    {
        log.debug("getSelectedUserId: " + selectedUserId);
        this.selectedUserId = selectedUserId;
    }

    public String getRatioTermConcept()
    {
        float res = (float) totalTerms / totalConcepts;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return twoDForm.format(res);
    }
}

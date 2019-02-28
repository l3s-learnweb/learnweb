package de.l3s.learnweb.dashboard.glossary;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.dashboard.glossary.GlossaryDashboardChartsFactory.*;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.MapHelper;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.PieChartModel;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Named
@SessionScoped
public class GlossaryDashboardUsersBean extends CommonDashboardUserBean
{
    private static final long serialVersionUID = -8766501339911729826L;
    //private static final Logger log = Logger.getLogger(DashboardUserBean.class);

    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";
    private static final int TRACKER_CLIENT_ID = 2;

    private Integer paramUserId;

    private Date startDate;
    private Date endDate;
    private User selectedUser;
    private List<User> selectedUsers;
    private List<User> defaultUsersList;
    private List<Integer> selectedUsersIds;
    private GlossaryDashboardManager dashboardManager;

    private Integer totalConcepts;
    private Integer totalTerms;
    private Integer totalSources;
    private ArrayList<GlossaryUserTermsSummary> glossaryFieldsSummeryPerUser;
    private Map<String, Integer> glossarySourcesWithCounters;
    private Map<Integer, Integer> actionsWithCounters;
    private Map<String, Integer> actionsCountPerDay;
    private ArrayList<GlossaryEntryDescLang> descFieldsStatistic;
    private ArrayList<String> glossaryDescriptions;
    private Map<String, Integer> proxySourcesWithCounters;
    private LinkedList<TrackerUserActivity> trackerStatistics;

    private LineChartModel interactionsChart;
    private BarChartModel usersActivityTypesChart;
    private PieChartModel usersSourcesChart;
    private BarChartModel userFieldsChart;
    private BarChartModel proxySourcesChart;
    private List<Resource> glossaryResources;

    public GlossaryDashboardUsersBean()
    {
    }

    public void onLoad() throws SQLException
    {
        User user = getUser();
        if(user == null)
            return;

        if(!user.isModerator() && paramUserId != null && paramUserId != user.getId())
        {
            addAccessDeniedMessage();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);

        String savedStartDate = getPreference(PREFERENCE_STARTDATE, Long.toString(cal.getTimeInMillis())); // month ago
        String savedEndDate = getPreference(PREFERENCE_ENDDATE, Long.toString(new Date().getTime()));
        startDate = new Date(Long.parseLong(savedStartDate));
        endDate = new Date(Long.parseLong(savedEndDate));

        selectedUser = paramUserId == null ? user : getLearnweb().getUserManager().getUser(paramUserId);
        selectedUsersIds = Collections.singletonList(selectedUser.getId());
        selectedUsers=getUsersList();
        dashboardManager = new GlossaryDashboardManager();

        fetchDataFromManager();
    }



    public List<User> getDefaultUsersList(){
        return defaultUsersList;
    }
    public void setDefaultUsersList(List<User> DefaultUsersList){
        this.defaultUsersList=DefaultUsersList;
    }

    public List<Group> getGroups() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getGroupsByCourseId(485);
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
        trackerStatistics = dashboardManager.getTrackerStatistics(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);
        proxySourcesWithCounters = dashboardManager.getProxySourcesWithCounters(TRACKER_CLIENT_ID, selectedUsersIds, startDate, endDate);

        //TODO Tetiana: redo query when description field will be restored
        glossaryDescriptions = dashboardManager.getGlossaryDescriptions(selectedUsersIds, startDate, endDate);
        descFieldsStatistic = dashboardManager.getLangDescStatistic(selectedUsersIds, startDate, endDate);

        glossaryResources = getLearnweb().getResourceManager().getGlossaryResourcesByUserId(selectedUser.getId());
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

    public User getSelectedUser()
    {
        return selectedUser;
    }

    public LineChartModel getInteractionsChart()
    {
        if(interactionsChart == null)
            interactionsChart = GlossaryDashboardChartsFactory.createInteractionsChart(actionsCountPerDay, startDate, endDate);
        return interactionsChart;
    }

    public BarChartModel getUserFieldsChart()
    {
        if(userFieldsChart == null)
            userFieldsChart = GlossaryDashboardChartsFactory.createUserFieldsChart(glossaryFieldsSummeryPerUser);
        return userFieldsChart;
    }

    public BarChartModel getUsersActivityTypesChart()
    {
        if(usersActivityTypesChart == null)
            usersActivityTypesChart = GlossaryDashboardChartsFactory.createActivityTypesChart(actionsWithCounters);
        return usersActivityTypesChart;
    }

    public BarChartModel getProxySourcesChart()
    {
        if(proxySourcesChart == null)
            proxySourcesChart = GlossaryDashboardChartsFactory.createProxySourcesChart(proxySourcesWithCounters);
        return proxySourcesChart;
    }

    public PieChartModel getUsersSourcesChart()
    {
        if(usersSourcesChart == null)
            usersSourcesChart = GlossaryDashboardChartsFactory.createUsersSourcesChart(glossarySourcesWithCounters);
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

    public LinkedList<TrackerUserActivity> getTrackerStatistics()
    {
        return trackerStatistics;
    }

    public TrackerUserActivity getSingleTrackerStatistics()
    {
        if(trackerStatistics.isEmpty())
        {
            return new TrackerUserActivity();
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
        float res = 0;
        if(totalConcepts == 0)
            res = 0;
        else
            res = (float) totalTerms / totalConcepts;
        return String.format("%.2f", res);
    }

    public List<Resource> getGlossaryResources()
    {
        return glossaryResources;
    }
}

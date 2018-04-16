package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import de.l3s.learnweb.UserManager;
import org.apache.log4j.Logger;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.dashboard.DashboardManager.DescFieldData;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryStatistic;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryFieldSummery;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.PieChartModel;

@ManagedBean
@ViewScoped
public class DashboardBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073418345L;
    private static final Logger log = Logger.getLogger(DashboardBean.class);

    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";

    private Date startDate = null;
    private Date endDate = null;
    private List<Integer> selectedUsersIds = null;
    private DashboardManager dashboardManager = null;

    private Integer totalConcepts = null;
    private Integer totalTerms = null;
    private ArrayList<GlossaryFieldSummery> glossaryFieldsSummeryPerUser;
    private Map<Integer, GlossaryStatistic> glossaryStatisticPerUser;
    private Map<String, Integer> glossaryConceptsCountPerUser;
    private Map<String, Integer> glossarySourcesWithCounters;
    private Map<String, Integer> glossaryTermsCountPerUser;
    private Map<Integer, Integer> actionsWithCounters;
    private Map<String, Integer> actionsCountPerDay;
    private ArrayList<DescFieldData> descFieldsStatistic;

    private LineChartModel interactionsChart;
    private BarChartModel studentsActivityTypesChart;
    private BarChartModel studentsGlossaryChart;
    private PieChartModel studentsSourcesChart;

    public DashboardBean()
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

            selectedUsersIds = getUser().getOrganisation().getUserIds();
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
        studentsGlossaryChart = null;
        studentsSourcesChart = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        totalConcepts = dashboardManager.getTotalConcepts(selectedUsersIds, startDate, endDate);
        totalTerms = dashboardManager.getTotalTerms(selectedUsersIds, startDate, endDate);

        descFieldsStatistic = dashboardManager.getLangDescStatistic(selectedUsersIds, startDate, endDate);
        glossaryFieldsSummeryPerUser = dashboardManager.getGlossaryFieldSummeryPerUser(selectedUsersIds, startDate, endDate);
        glossaryConceptsCountPerUser = dashboardManager.getGlossaryConceptsCountPerUser(selectedUsersIds, startDate, endDate);
        glossarySourcesWithCounters = dashboardManager.getGlossarySourcesWithCounters(selectedUsersIds, startDate, endDate);
        glossaryTermsCountPerUser = dashboardManager.getGlossaryTermsCountPerUser(selectedUsersIds, startDate, endDate);
        actionsWithCounters = dashboardManager.getActionsWithCounters(selectedUsersIds, startDate, endDate);
        actionsCountPerDay = dashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate);
        glossaryStatisticPerUser = dashboardManager.getGlossaryStatisticPerUser(selectedUsersIds, startDate, endDate);
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

    public BarChartModel getStudentsActivityTypesChart()
    {
        if(studentsActivityTypesChart == null)
            studentsActivityTypesChart = DashboardChartsFactory.createActivityTypesChart(actionsWithCounters);
        return studentsActivityTypesChart;
    }

    public BarChartModel getStudentsGlossaryChart()
    {
        if(studentsGlossaryChart == null)
            studentsGlossaryChart = DashboardChartsFactory.createStudentsGlossaryChart(glossaryConceptsCountPerUser, glossaryTermsCountPerUser);
        return studentsGlossaryChart;
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

    public ArrayList<GlossaryFieldSummery> getGlossaryFieldsSummeryPerUser()
    {
        return glossaryFieldsSummeryPerUser;
    }

    public ArrayList<GlossaryStatistic> getGlossaryStatisticPerUser()
    {
        return new ArrayList<>(glossaryStatisticPerUser.values());
    }

    public ArrayList<DescFieldData> getDescFieldsStatistic()
    {
        return descFieldsStatistic;
    }

    public ArrayList<User> getStudentsList() throws SQLException
    {
        ArrayList<User> students = new ArrayList<>();
        UserManager userManager = getLearnweb().getUserManager();
        for (int studentId : selectedUsersIds) {
            students.add(userManager.getUser(studentId));
        }
        return students;
    }

    public void onSubmitSelectedUsers()
    {
        try
        {
            this.selectedUsersIds = getSelectedUsers();
            cleanAndUpdateStoredData();
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    private ArrayList<Integer> getSelectedUsers()
    {
        HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String[] tempSelectedUsers = request.getParameterValues("selected_users");

        if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
        {
            addMessage(FacesMessage.SEVERITY_WARN, "select_user");
            return null;
        }

        ArrayList<Integer> selectedUsersSet = new ArrayList<>();
        for(String userId : tempSelectedUsers)
            selectedUsersSet.add(Integer.parseInt(userId));

        return selectedUsersSet;
    }
}

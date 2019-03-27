package de.l3s.learnweb.dashboard.glossary;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.PieChartModel;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.learnweb.user.User;

@Named
@SessionScoped
public class GlossaryDashboardBean extends CommonDashboardUserBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073418345L;
    private static final Logger log = Logger.getLogger(GlossaryDashboardBean.class);

    private GlossaryDashboardManager dashboardManager = null;

    @Deprecated
    @Inject
    private GlossaryDashboardUsersBean glossaryDashboardUsersBean;

    private transient Integer totalConcepts = null;
    private transient Integer totalTerms = null;
    private transient ArrayList<GlossaryUserTermsSummary> glossaryFieldsSummeryPerUser;
    private transient Map<Integer, GlossaryUserActivity> glossaryStatisticPerUser;
    private transient Map<String, Integer> glossaryConceptsCountPerUser;
    private transient Map<String, Integer> glossarySourcesWithCounters;
    private transient Map<String, Integer> glossaryTermsCountPerUser;
    private transient Map<Integer, Integer> actionsWithCounters;
    private transient Map<String, Integer> actionsCountPerDay;
    private transient ArrayList<GlossaryEntryDescLang> descFieldsStatistic;

    private transient LineChartModel interactionsChart;
    private transient BarChartModel usersActivityTypesChart;
    private transient BarChartModel usersGlossaryChart;
    private transient PieChartModel usersSourcesChart;

    public GlossaryDashboardBean()
    {
    }

    public void onLoad()
    {
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        try
        {

            dashboardManager = new GlossaryDashboardManager();

            getSelectedUsersIds(); // TODO
            glossaryDashboardUsersBean.setDefaultUsersList();
            if(glossaryDashboardUsersBean.getSelectedUsersIds() != null && glossaryDashboardUsersBean.getSelectedUsersIds().size() != 0)
            {
                cleanAndUpdateStoredData();
            }
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void cleanAndUpdateStoredData() throws SQLException
    {
        log.debug("cleanAndUpdateStoredData " + glossaryDashboardUsersBean.getSelectedUsersIds());

        interactionsChart = null;
        usersActivityTypesChart = null;
        usersGlossaryChart = null;
        usersSourcesChart = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        List<Integer> selectedUsersIds = glossaryDashboardUsersBean.getSelectedUsersIds();
        totalConcepts = dashboardManager.getTotalConcepts(selectedUsersIds, startDate, endDate);
        totalTerms = dashboardManager.getTotalTerms(selectedUsersIds, startDate, endDate);

        //TODO Tetiana: redo descFieldsStatistic when description field will be restored
        descFieldsStatistic = dashboardManager.getLangDescStatistic(selectedUsersIds, startDate, endDate);
        glossaryFieldsSummeryPerUser = dashboardManager.getGlossaryFieldSummeryPerUser(selectedUsersIds, startDate, endDate);
        glossaryConceptsCountPerUser = dashboardManager.getGlossaryConceptsCountPerUser(selectedUsersIds, startDate, endDate);
        glossarySourcesWithCounters = dashboardManager.getGlossarySourcesWithCounters(selectedUsersIds, startDate, endDate);
        glossaryTermsCountPerUser = dashboardManager.getGlossaryTermsCountPerUser(selectedUsersIds, startDate, endDate);
        actionsWithCounters = dashboardManager.getActionsWithCounters(selectedUsersIds, startDate, endDate);
        if(actionsWithCounters == null)
            throw new NullPointerException("should not happen");

        actionsCountPerDay = dashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate);
        glossaryStatisticPerUser = dashboardManager.getGlossaryStatisticPerUser(selectedUsersIds, startDate, endDate);
    }

    public LineChartModel getInteractionsChart()
    {
        if(interactionsChart == null)
            interactionsChart = GlossaryDashboardChartsFactory.createInteractionsChart(actionsCountPerDay, startDate, endDate);
        return interactionsChart;
    }

    public BarChartModel getUsersActivityTypesChart()
    {
        if(usersActivityTypesChart == null)
            usersActivityTypesChart = GlossaryDashboardChartsFactory.createActivityTypesChart(actionsWithCounters);
        return usersActivityTypesChart;
    }

    public BarChartModel getUsersGlossaryChart()
    {
        if(usersGlossaryChart == null)
            usersGlossaryChart = GlossaryDashboardChartsFactory.createUsersGlossaryChart(glossaryConceptsCountPerUser, glossaryTermsCountPerUser);
        return usersGlossaryChart;
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

    public ArrayList<GlossaryUserTermsSummary> getGlossaryFieldsSummeryPerUser()
    {
        return glossaryFieldsSummeryPerUser;
    }

    public ArrayList<GlossaryUserActivity> getGlossaryStatisticPerUser()
    {
        return new ArrayList<>(glossaryStatisticPerUser.values());
    }

    public ArrayList<GlossaryEntryDescLang> getDescFieldsStatistic()
    {
        return descFieldsStatistic;
    }

    @Override
    public void onSubmitSelectedUsers() throws SQLException
    {
        glossaryDashboardUsersBean.onSubmitSelectedUsers();
        if(glossaryDashboardUsersBean.getSelectedUsersIds() != null)
        {
            cleanAndUpdateStoredData();
        }
    }

}

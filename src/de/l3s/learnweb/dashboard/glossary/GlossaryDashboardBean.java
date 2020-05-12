package de.l3s.learnweb.dashboard.glossary;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.pie.PieChartModel;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.learnweb.resource.Resource;

@Named
@ViewScoped
public class GlossaryDashboardBean extends CommonDashboardUserBean implements Serializable
{
    private static final long serialVersionUID = 1708095580068235081L;
    //private static final Logger log = LogManager.getLogger(GlossaryDashboardBean.class);

    private Integer totalConcepts;
    private Integer totalTerms;
    private Integer totalSources;
    private List<GlossaryUserTermsSummary> glossaryFieldsSummeryPerUser;
    private Map<Integer, GlossaryUserActivity> glossaryStatisticPerUser;
    private Map<String, Integer> glossaryConceptsCountPerUser;
    private Map<String, Integer> glossarySourcesWithCounters;
    private Map<String, Integer> glossaryTermsCountPerUser;
    private Map<Integer, Integer> actionsWithCounters;
    private Map<String, Integer> actionsCountPerDay;
    private List<GlossaryEntryDescLang> descFieldsStatistic;

    private transient LineChartModel interactionsChart;
    private transient BarChartModel usersActivityTypesChart;
    private transient BarChartModel usersGlossaryChart;
    private transient PieChartModel usersSourcesChart;

    private List<Resource> glossaryResources;

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
        interactionsChart = null;
        usersActivityTypesChart = null;
        usersGlossaryChart = null;
        usersSourcesChart = null;

        totalConcepts = null;
        totalTerms = null;
        totalSources = null;
        glossaryFieldsSummeryPerUser = null;
        glossaryStatisticPerUser = null;
        glossaryConceptsCountPerUser = null;
        glossarySourcesWithCounters = null;
        glossaryTermsCountPerUser = null;
        actionsWithCounters = null;
        actionsCountPerDay = null;
        descFieldsStatistic = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        if(!CollectionUtils.isEmpty(getSelectedUsersIds()))
        {
            GlossaryDashboardManager dashboardManager = getLearnweb().getGlossaryDashboardManager();
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            totalConcepts = dashboardManager.getTotalConcepts(selectedUsersIds, startDate, endDate);
            totalTerms = dashboardManager.getTotalTerms(selectedUsersIds, startDate, endDate);
            totalSources = dashboardManager.getTotalSources(selectedUsersIds, startDate, endDate);

            descFieldsStatistic = dashboardManager.getLangDescStatistic(selectedUsersIds, startDate, endDate);
            glossaryFieldsSummeryPerUser = dashboardManager.getGlossaryFieldSummeryPerUser(selectedUsersIds, startDate, endDate);
            glossaryConceptsCountPerUser = dashboardManager.getGlossaryConceptsCountPerUser(selectedUsersIds, startDate, endDate);
            glossarySourcesWithCounters = dashboardManager.getGlossarySourcesWithCounters(selectedUsersIds, startDate, endDate);
            glossaryTermsCountPerUser = dashboardManager.getGlossaryTermsCountPerUser(selectedUsersIds, startDate, endDate);
            actionsWithCounters = dashboardManager.getActionsWithCounters(selectedUsersIds, startDate, endDate);
            actionsCountPerDay = dashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate);
            glossaryStatisticPerUser = dashboardManager.getGlossaryStatisticPerUser(selectedUsersIds, startDate, endDate);

            glossaryResources = getLearnweb().getResourceManager().getGlossaryResourcesByUserId(selectedUsersIds.get(0));
        }
    }

    public List<Resource> getGlossaryResources()
    {
        return glossaryResources;
    }

    public LineChartModel getInteractionsChart() throws SQLException
    {
        if(interactionsChart == null)
        {
            if(actionsCountPerDay == null)
            {
                actionsCountPerDay = getLearnweb().getGlossaryDashboardManager().getActionsCountPerDay(getSelectedUsersIds(), startDate, endDate);
            }
            interactionsChart = GlossaryDashboardChartsFactory.createInteractionsChart(actionsCountPerDay, startDate, endDate);
        }
        return interactionsChart;
    }

    public BarChartModel getUsersActivityTypesChart() throws SQLException
    {
        if(usersActivityTypesChart == null)
        {
            if(actionsWithCounters == null)
            {
                actionsWithCounters = getLearnweb().getGlossaryDashboardManager().getActionsWithCounters(getSelectedUsersIds(), startDate, endDate);
            }
            usersActivityTypesChart = GlossaryDashboardChartsFactory.createActivityTypesChart(actionsWithCounters, getUserBean().getLocale());
        }
        return usersActivityTypesChart;
    }

    public BarChartModel getUsersGlossaryChart() throws SQLException
    {
        if(usersGlossaryChart == null)
        {
            if(glossaryConceptsCountPerUser == null)
            {
                glossaryConceptsCountPerUser = getLearnweb().getGlossaryDashboardManager().getGlossaryConceptsCountPerUser(getSelectedUsersIds(), startDate, endDate);
                glossaryTermsCountPerUser = getLearnweb().getGlossaryDashboardManager().getGlossaryTermsCountPerUser(getSelectedUsersIds(), startDate, endDate);
            }
            usersGlossaryChart = GlossaryDashboardChartsFactory.createUsersGlossaryChart(glossaryConceptsCountPerUser, glossaryTermsCountPerUser);
        }
        return usersGlossaryChart;
    }

    public PieChartModel getUsersSourcesChart() throws SQLException
    {
        if(usersSourcesChart == null)
        {
            if(glossarySourcesWithCounters == null)
            {
                glossarySourcesWithCounters = getLearnweb().getGlossaryDashboardManager().getGlossarySourcesWithCounters(getSelectedUsersIds(), startDate, endDate);
            }
            usersSourcesChart = GlossaryDashboardChartsFactory.createUsersSourcesChart(glossarySourcesWithCounters);
        }
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

    public float getTermsToConcepts()
    {
        return ((float) totalConcepts / totalTerms);
    }

    public Integer getTotalSources()
    {
        return totalSources;
    }

    public List<GlossaryUserTermsSummary> getGlossaryFieldsSummeryPerUser()
    {
        return glossaryFieldsSummeryPerUser;
    }

    public List<GlossaryUserActivity> getGlossaryStatisticPerUser()
    {
        return new ArrayList<>(glossaryStatisticPerUser.values());
    }

    public List<GlossaryEntryDescLang> getDescFieldsStatistic()
    {
        return descFieldsStatistic;
    }
}

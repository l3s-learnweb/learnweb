package de.l3s.learnweb.dashboard.glossary;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.collections4.CollectionUtils;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.resource.glossary.GlossaryEntryDao;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.glossary.GlossaryTermDao;

@Named
@ViewScoped
public class GlossaryDashboardBean extends CommonDashboardUserBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1708095580068235081L;

    private transient List<GlossaryResource> glossaryResources;
    private transient Integer totalConcepts;
    private transient Integer totalTerms;
    private transient Integer totalSources;
    private transient List<GlossaryUserTermsSummary> glossaryFieldsSummeryPerUser;
    private transient List<GlossaryUserActivity> glossaryStatisticPerUser;
    private transient Map<String, Integer> glossaryConceptsCountPerUser;
    private transient Map<String, Integer> glossarySourcesWithCounters;
    private transient Map<String, Integer> glossaryTermsCountPerUser;
    private transient Map<Integer, Integer> actionsWithCounters;
    private transient Map<String, Integer> actionsCountPerDay;
    private transient List<GlossaryDescriptionSummary> descFieldsStatistic;
    private transient String interactionsChart;
    private transient String usersActivityTypesChart;
    private transient String usersGlossaryChart;
    private transient String usersSourcesChart;

    @Inject
    private LogDao logDao;

    @Inject
    private GlossaryEntryDao glossaryEntryDao;

    @Inject
    private GlossaryTermDao glossaryTermDao;

    @Override
    public void onLoad() {
        super.onLoad();

        cleanAndUpdateStoredData();
    }

    @Override
    public void cleanAndUpdateStoredData() {
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

    private void fetchDataFromManager() {
        if (!CollectionUtils.isEmpty(getSelectedUsersIds())) {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            totalConcepts = glossaryEntryDao.countTotalEntries(selectedUsersIds, startDate, endDate);
            totalTerms = glossaryTermDao.countTotalTerms(selectedUsersIds, startDate, endDate);
            totalSources = glossaryTermDao.countTotalSources(selectedUsersIds, startDate, endDate);

            descFieldsStatistic = glossaryEntryDao.countGlossaryDescriptionSummary(selectedUsersIds, startDate, endDate);
            glossaryFieldsSummeryPerUser = glossaryTermDao.countGlossaryUserTermsSummary(selectedUsersIds, startDate, endDate);
            glossaryConceptsCountPerUser = glossaryEntryDao.countEntriesPerUser(selectedUsersIds, startDate, endDate);
            glossarySourcesWithCounters = glossaryTermDao.countUsagePerSource(selectedUsersIds, startDate, endDate);
            glossaryTermsCountPerUser = glossaryTermDao.countTermsPerUser(selectedUsersIds, startDate, endDate);
            actionsWithCounters = logDao.countUsagePerAction(selectedUsersIds, startDate, endDate);
            actionsCountPerDay = logDao.countActionsPerDay(selectedUsersIds, startDate, endDate);
            glossaryStatisticPerUser = glossaryTermDao.countGlossaryUserActivity(selectedUsersIds, startDate, endDate);

            glossaryResources = dao().getGlossaryDao().findByOwnerIds(selectedUsersIds);
        }
    }

    public List<GlossaryResource> getGlossaryResources() {
        return glossaryResources;
    }

    public String getInteractionsChart() {
        if (interactionsChart == null) {
            if (actionsCountPerDay == null) {
                actionsCountPerDay = logDao.countActionsPerDay(getSelectedUsersIds(), startDate, endDate);
            }
            interactionsChart = GlossaryDashboardChartsFactory.createInteractionsChart(actionsCountPerDay, startDate, endDate);
        }
        return interactionsChart;
    }

    public String getUsersActivityTypesChart() {
        if (usersActivityTypesChart == null) {
            if (actionsWithCounters == null) {
                actionsWithCounters = logDao.countUsagePerAction(getSelectedUsersIds(), startDate, endDate);
            }
            usersActivityTypesChart = GlossaryDashboardChartsFactory.createActivityTypesChart(actionsWithCounters);
        }
        return usersActivityTypesChart;
    }

    public String getUsersGlossaryChart() {
        if (usersGlossaryChart == null) {
            if (glossaryConceptsCountPerUser == null) {
                glossaryConceptsCountPerUser = glossaryEntryDao.countEntriesPerUser(getSelectedUsersIds(), startDate, endDate);
                glossaryTermsCountPerUser = glossaryTermDao.countTermsPerUser(getSelectedUsersIds(), startDate, endDate);
            }
            usersGlossaryChart = GlossaryDashboardChartsFactory.createUsersGlossaryChart(glossaryConceptsCountPerUser, glossaryTermsCountPerUser);
        }
        return usersGlossaryChart;
    }

    public String getUsersSourcesChart() {
        if (usersSourcesChart == null) {
            if (glossarySourcesWithCounters == null) {
                glossarySourcesWithCounters = glossaryTermDao.countUsagePerSource(getSelectedUsersIds(), startDate, endDate);
            }
            usersSourcesChart = GlossaryDashboardChartsFactory.createUsersSourcesChart(glossarySourcesWithCounters);
        }
        return usersSourcesChart;
    }

    public Integer getTotalConcepts() {
        return totalConcepts;
    }

    public Integer getTotalTerms() {
        return totalTerms;
    }

    public float getTermsToConcepts() {
        return ((float) totalConcepts / totalTerms);
    }

    public Integer getTotalSources() {
        return totalSources;
    }

    public List<GlossaryUserTermsSummary> getGlossaryFieldsSummeryPerUser() {
        return glossaryFieldsSummeryPerUser;
    }

    public List<GlossaryUserActivity> getGlossaryStatisticPerUser() {
        return glossaryStatisticPerUser;
    }

    public List<GlossaryDescriptionSummary> getDescFieldsStatistic() {
        return descFieldsStatistic;
    }
}

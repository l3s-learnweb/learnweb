package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Organisation;
import org.apache.log4j.Logger;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.dashboard.DashboardManager.TrackerStatistic;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryStatistic;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryFieldSummery;
import org.primefaces.model.chart.*;

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
    private Integer selectedUserId = null; // optionally select a single user to display
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

    private LineChartModel interactionsChart;
    private BarChartModel studentsActivityTypesChart;
    private BarChartModel studentsGlossary;
    private PieChartModel studentsSources;
    private ArrayList<GlossaryStatistic> glossaryStat;
    private ArrayList<DescFieldData> langSescDataList;

    private Map<Integer, GlossaryStatistic> glossaryStatisticPerUser;

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

            orgUserIds = getUser().getOrganisation().getUserIds();
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
        glossaryConceptsCountPerUser = dashboardManager.getGlossaryConceptsCountPerUser(orgUserIds, startDate, endDate);
        glossarySourcesWithCounters = dashboardManager.getGlossarySourcesWithCounters(orgUserIds, startDate, endDate);
        glossaryTermsCountPerUser = dashboardManager.getGlossaryTermsCountPerUser(orgUserIds, startDate, endDate);
        actionsWithCounters = dashboardManager.getActionsWithCounters(orgUserIds, startDate, endDate);
        actionsCountPerDay = dashboardManager.getActionsCountPerDay(orgUserIds, startDate, endDate);
        glossaryDescriptions = dashboardManager.getGlossaryDescriptions(orgUserIds, startDate, endDate);
        glossaryStatisticPerUser = dashboardManager.getGlossaryStatisticPerUser(orgUserIds, startDate, endDate);
        proxySourcesWithCounters = dashboardManager.getProxySourcesWithCounters(trackerClientId, orgUserIds, startDate, endDate);
        trackerStatistics = dashboardManager.getTrackerStatistics(trackerClientId, orgUserIds, startDate, endDate);
    }

    public void onDateChanged() throws SQLException
    {
        updateStatsData();

        interactionsChart = null;
        studentsActivityTypesChart = null;
        studentsGlossary = null;
        studentsSources = null;
        glossaryStat = null;
        langSescDataList = null;
    }

    public LineChartModel getInteractionsChart()
    {
        if(interactionsChart == null)
            interactionsChart = DashboardChartsFactory.createInteractionsChart(actionsCountPerDay, this.startDate, this.endDate);
        return interactionsChart;
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

    public PieChartModel getStudentsSources()
    {
        if(studentsSources == null)
            studentsSources = DashboardChartsFactory.createStudentsSourcesChart(glossarySourcesWithCounters);
        return studentsSources;
    }

    public ArrayList<GlossaryStatistic> getSummary()
    {
        if(glossaryStat == null)
        {
            glossaryStat = new ArrayList<>();
            for(Integer uid : orgUserIds)
                glossaryStat.add(glossaryStatisticPerUser.getOrDefault(uid, new GlossaryStatistic(uid)));
        }
        return glossaryStat;
    }

    public ArrayList<DescFieldData> getLangDesclist()
    {
        if(langSescDataList == null)
        {
            langSescDataList = new ArrayList<>();
            DescFieldData d = new DescFieldData();
            d.setDescription("Vitamin");
            d.setEnrtyid(1322);
            d.setLang("en");
            d.setLenght(1);
            d.setUserid(10111);
            langSescDataList.add(d);

            d = new DescFieldData();
            d.setDescription("Il diabete ");
            d.setEnrtyid(1428);
            d.setLang("it");
            d.setLenght(2);
            d.setUserid(10111);
            langSescDataList.add(d);

            d = new DescFieldData();
            d.setDescription("Epidemiology");
            d.setEnrtyid(1113);
            d.setLang("en");
            d.setLenght(1);
            d.setUserid(10150);
            langSescDataList.add(d);

            Integer userid = 10109;
            Integer lenght = 1;
            String lang = "unk";
            d = new DescFieldData();
            d.setDescription("g");
            d.setEnrtyid(1320);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            langSescDataList.add(d);

            d = new DescFieldData();
            d.setDescription("v");
            d.setEnrtyid(1323);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            langSescDataList.add(d);

            d = new DescFieldData();
            d.setDescription("c");
            d.setEnrtyid(1324);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            langSescDataList.add(d);

            d = new DescFieldData();
            d.setDescription("g");
            d.setEnrtyid(1327);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            langSescDataList.add(d);

            d = new DescFieldData();
            d.setDescription("b");
            d.setEnrtyid(1328);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            langSescDataList.add(d);

            d = new DescFieldData();
            d.setDescription("h");
            d.setEnrtyid(1350);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            langSescDataList.add(d);
        }
        return langSescDataList;
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

    public List<GlossaryFieldSummery> getGlossaryFieldSummeryPerUser()
    {
        return glossaryFieldSummeryPerUser;
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

    public List<GlossaryFieldSummery> getFields()
    {
        return glossaryFieldSummeryPerUser;
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

    public class DescFieldData
    {
        Integer userid;
        String description;
        String lang;
        Integer lenght;
        Integer enrtyid;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public String getLang()
        {
            return lang;
        }

        public void setLang(String lang)
        {
            this.lang = lang;
        }

        public Integer getLenght()
        {
            return lenght;
        }

        public void setLenght(Integer lenght)
        {
            this.lenght = lenght;
        }

        public Integer getUserid()
        {
            return userid;
        }

        public void setUserid(Integer userid)
        {
            this.userid = userid;
        }

        public Integer getEnrtyid()
        {
            return enrtyid;
        }

        public void setEnrtyid(Integer enrtyid)
        {
            this.enrtyid = enrtyid;
        }

    }

}

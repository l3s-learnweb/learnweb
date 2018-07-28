package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.LineChartModel;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

@Named
@SessionScoped
public class ActivityDashboardBean extends ApplicationBean implements Serializable
{
    @Inject
    private ActivityDashboardUsersBean activityDashboardUsersBean;
    private static final long serialVersionUID = 3326736281893564706L;

    private static final String PREFERENCE_STARTDATE = "activity_dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "activity_dashboard_enddate";

    private Date startDate = null;
    private Date endDate = null;
    private transient LineChartModel interactionsChart = null;

    private ActivityDashboardManager activityDashboardManager;

    private Map<String, String> actions;

    private String[] selectedActionItems;

    private List<SelectItem> groupedActions;

    private String[] selectedGroupedActions;

    @PostConstruct
    public void init()
    {
        actions = new TreeMap<String, String>();
        actions.put("Resource actions", getStringOfActions(Action.RESOURCE_RELATED_ACTIONS));
        actions.put("Folder actions", getStringOfActions(Action.FOLDER_ACTIONS));
        actions.put("Glossary actions", getStringOfActions(Action.GLOSSARY_RELATED_ACTIONS));
        actions.put("Login/Logout actions", getStringOfActions(Action.LOG_ACTIONS));
        actions.put("Search actions", getStringOfActions(Action.SEARCH_RELATED_ACTIONS));
        actions.put("Other actions", getStringOfActions(Action.OTHER_ACTIONS));
        actions.put("Group actions", getStringOfActions(Action.GROUP_ACTIONS));
        groupedActions = new ArrayList<SelectItem>();

        groupedActions.add(createGroupCheckboxes("Resource actions", Action.RESOURCE_RELATED_ACTIONS));
        groupedActions.add(createGroupCheckboxes("Folder actions", Action.FOLDER_ACTIONS));
        groupedActions.add(createGroupCheckboxes("Glossary actions", Action.GLOSSARY_RELATED_ACTIONS));
        groupedActions.add(createGroupCheckboxes("Login/Logout actions", Action.LOG_ACTIONS));
        groupedActions.add(createGroupCheckboxes("Search actions", Action.SEARCH_RELATED_ACTIONS));
        groupedActions.add(createGroupCheckboxes("Group actions", Action.GROUP_ACTIONS));
        groupedActions.add(createGroupCheckboxes("Other actions", Action.OTHER_ACTIONS));
    }

    public void onFullActivitiesClick()
    {
        selectedActionItems = null;
    }

    public void onGroupedActivitiesClick()
    {
        selectedGroupedActions = null;
    }

    private SelectItemGroup createGroupCheckboxes(String name, Set<Action> actions)
    {
        SelectItemGroup itemGroup = new SelectItemGroup(name);
        List<SelectItem> itemList = new ArrayList<>();
        for(Action action : actions)
        {
            itemList.add(new SelectItem(action.ordinal(), action.name()));
        }
        SelectItem[] itemArr = new SelectItem[itemList.size()];
        itemGroup.setSelectItems(itemList.toArray(itemArr));
        return itemGroup;
    }

    private String getStringOfActions(Set<Action> actions)
    {
        return actions.stream()
                .map(a -> String.valueOf(a.ordinal()))
                .collect(Collectors.joining(","));
    }

    public void onLoad()
    {
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

            activityDashboardManager = getLearnweb().getActivityDashboardManager();
            if(activityDashboardUsersBean.getSelectedUsersIds() == null)
            {
                activityDashboardUsersBean.setSelectedUsersIds(getUser().getOrganisation().getUserIds());
            }
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void cleanAndUpdateStoredData() throws SQLException
    {
        interactionsChart = null;
        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        List<Integer> selectedUsersIds = activityDashboardUsersBean.getSelectedUsersIds();

        if(selectedActionItems != null && selectedActionItems.length != 0)
        {
            List<ActivityGraphData> data = new ArrayList<>();
            for(String activityGroupName : selectedActionItems)
            {
                ActivityGraphData activityData = new ActivityGraphData();
                activityData.setName(activityGroupName);
                activityData.setActionsPerDay(activityDashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate, actions.get(activityGroupName)));
                data.add(activityData);
            }
            interactionsChart = DashboardChartsFactory.createActivitiesChart(data, startDate, endDate);

        }
        else if(selectedGroupedActions != null && selectedGroupedActions.length != 0)
        {
            List<ActivityGraphData> data = new ArrayList<>();
            for(String activityGroupName : selectedGroupedActions)
            {
                ActivityGraphData activityData = new ActivityGraphData();
                activityData.setName(Action.values()[Integer.valueOf(activityGroupName)].name());
                activityData.setActionsPerDay(activityDashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate, activityGroupName));
                data.add(activityData);
            }
            interactionsChart = DashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
        }
    }

    public void onSubmitSelectedUsers() throws SQLException
    {
        activityDashboardUsersBean.onSubmitSelectedUsers();
        if(activityDashboardUsersBean.getSelectedUsersIds() != null)
        {
            cleanAndUpdateStoredData();
        }
    }

    public LineChartModel getInteractionsChart()
    {

        return interactionsChart;
    }

    public ActivityDashboardUsersBean getActivityDashboardUsersBean()
    {
        return activityDashboardUsersBean;
    }

    public void setActivityDashboardUsersBean(ActivityDashboardUsersBean activityDashboardUsersBean)
    {
        this.activityDashboardUsersBean = activityDashboardUsersBean;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public Map<String, String> getActions()
    {
        return actions;
    }

    public String[] getSelectedActionItems()
    {
        return selectedActionItems;
    }

    public void setSelectedActionItems(String[] selectedActionItems)
    {
        this.selectedActionItems = selectedActionItems;
    }

    public List<SelectItem> getGroupedActions()
    {
        return groupedActions;
    }

    public void setGroupedActions(List<SelectItem> groupedActions)
    {
        this.groupedActions = groupedActions;
    }

    public String[] getSelectedGroupedActions()
    {
        return selectedGroupedActions;
    }

    public void setSelectedGroupedActions(String[] selectedGroupedActions)
    {
        this.selectedGroupedActions = selectedGroupedActions;
    }

}

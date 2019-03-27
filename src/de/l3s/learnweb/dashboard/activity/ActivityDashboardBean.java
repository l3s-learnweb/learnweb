package de.l3s.learnweb.dashboard.activity;

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

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import org.primefaces.model.chart.LineChartModel;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.dashboard.activity.ActivityDashboardChartsFactory.ActivityGraphData;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.learnweb.user.User;

@Named
@SessionScoped
public class ActivityDashboardBean extends CommonDashboardUserBean implements Serializable
{
    @Deprecated
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
        actions.put("Resource actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.RESOURCE)));
        actions.put("Folder actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.FOLDER)));
        actions.put("Glossary actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.GLOSSARY)));
        actions.put("Login/Logout actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.USER)));
        actions.put("Search actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.SEARCH)));
        actions.put("Other actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.OTHER)));
        actions.put("Group actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.GROUP)));

        groupedActions = new ArrayList<SelectItem>();
        groupedActions.add(createGroupCheckboxes("Resource actions", Action.getActionsByCategory(ActionCategory.RESOURCE)));
        groupedActions.add(createGroupCheckboxes("Folder actions", Action.getActionsByCategory(ActionCategory.FOLDER)));
        groupedActions.add(createGroupCheckboxes("Glossary actions", Action.getActionsByCategory(ActionCategory.GLOSSARY)));
        groupedActions.add(createGroupCheckboxes("Login/Logout actions", Action.getActionsByCategory(ActionCategory.USER)));
        groupedActions.add(createGroupCheckboxes("Search actions", Action.getActionsByCategory(ActionCategory.SEARCH)));
        groupedActions.add(createGroupCheckboxes("Group actions", Action.getActionsByCategory(ActionCategory.GROUP)));
        groupedActions.add(createGroupCheckboxes("Other actions", Action.getActionsByCategory(ActionCategory.OTHER)));
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
            cal.add(Calendar.MONTH, -3); // load data from last 3 month until now

            startDate = new Date(cal.getTimeInMillis());
            endDate = new Date(new Date().getTime());
            activityDashboardManager = new ActivityDashboardManager();
            if(getSelectedUsersIds() != null && getSelectedUsersIds().size() != 0)
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
        interactionsChart = null;
        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException
    {
        List<Integer> selectedUsersIds = activityDashboardUsersBean.getSelectedUsersIds();
        if(selectedActionItems != null)
        {
            List<ActivityGraphData> data = new ArrayList<>();
            for(String activityGroupName : selectedActionItems)
            {
                ActivityGraphData activityData = new ActivityGraphData();
                activityData.setName(activityGroupName);
                activityData.setActionsPerDay(activityDashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate, actions.get(activityGroupName)));
                data.add(activityData);
            }
            interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
            System.out.println(1);
            System.out.println(getInteractionsChart());

        }
        else if(selectedGroupedActions != null)
        {
            List<ActivityGraphData> data = new ArrayList<>();
            for(String activityGroupName : selectedGroupedActions)
            {
                ActivityGraphData activityData = new ActivityGraphData();
                activityData.setName(Action.values()[Integer.valueOf(activityGroupName)].name());
                activityData.setActionsPerDay(activityDashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate, activityGroupName));
                data.add(activityData);
            }
            interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
            System.out.println(2);
            System.out.println(getInteractionsChart());
        }
    }

    public void onSubmitSelectedUsers() throws SQLException
    {
        if(getSelectedUsersIds() != null)
        {
            cleanAndUpdateStoredData();
        }
    }

    public LineChartModel getInteractionsChart()
    {

        return interactionsChart;
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
        System.out.println(this.selectedActionItems);
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

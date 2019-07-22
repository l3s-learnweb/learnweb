package de.l3s.learnweb.dashboard.activity;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Named;

import org.primefaces.model.chart.LineChartModel;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.learnweb.dashboard.activity.ActivityDashboardChartsFactory.ActivityGraphData;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.learnweb.user.User;

@Named
@SessionScoped
public class ActivityDashboardBean extends CommonDashboardUserBean implements Serializable
{
    private static final long serialVersionUID = 3326736281893564706L;

    private ActivityDashboardManager activityDashboardManager;

    private Map<String, String> actions;
    private List<String> selectedActionItems;
    private List<SelectItemGroup> groupedActions;
    private List<Integer> selectedGroupedActions;

    private transient LineChartModel interactionsChart = null;
    private transient List<Map<String, Object>> interactionsTable = null;

    public ActivityDashboardBean()
    {
    }

    @PostConstruct
    public void init()
    {
        actions = new TreeMap<>();
        actions.put("Resource actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.RESOURCE)));
        actions.put("Folder actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.FOLDER)));
        actions.put("Glossary actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.GLOSSARY)));
        actions.put("Login/Logout actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.USER)));
        actions.put("Search actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.SEARCH)));
        actions.put("Group actions", getStringOfActions(Action.getActionsByCategory(ActionCategory.GROUP)));
        groupedActions = new ArrayList<>();
        groupedActions.add(createGroupCheckboxes("Resource actions", Action.getActionsByCategory(ActionCategory.RESOURCE)));
        groupedActions.add(createGroupCheckboxes("Folder actions", Action.getActionsByCategory(ActionCategory.FOLDER)));
        groupedActions.add(createGroupCheckboxes("Glossary actions", Action.getActionsByCategory(ActionCategory.GLOSSARY)));
        groupedActions.add(createGroupCheckboxes("Login/Logout actions", Action.getActionsByCategory(ActionCategory.USER)));
        groupedActions.add(createGroupCheckboxes("Search actions", Action.getActionsByCategory(ActionCategory.SEARCH)));
        groupedActions.add(createGroupCheckboxes("Group actions", Action.getActionsByCategory(ActionCategory.GROUP)));
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
        super.onLoad();

        try
        {
            activityDashboardManager = new ActivityDashboardManager();
            cleanAndUpdateStoredData();
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void cleanAndUpdateStoredData() throws SQLException
    {
        interactionsChart = null;
        interactionsTable = null;

        if(getSelectedUsersIds() != null && getSelectedUsersIds().size() != 0)
        {
            fetchDataFromManager();
        }
    }

    private void fetchDataFromManager() throws SQLException
    {
        List<Integer> selectedUsersIds = getSelectedUsersIds();
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
            interactionsTable = ActivityDashboardChartsFactory.createActivitiesTable(data, startDate, endDate);
        }
        else if(selectedGroupedActions != null)
        {
            List<ActivityGraphData> data = new ArrayList<>();
            for(Integer activityGroupName : selectedGroupedActions)
            {
                ActivityGraphData activityData = new ActivityGraphData();
                activityData.setName(Action.values()[activityGroupName].name());
                activityData.setActionsPerDay(activityDashboardManager.getActionsCountPerDay(selectedUsersIds, startDate, endDate, activityGroupName.toString()));
                data.add(activityData);
            }
            interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
            interactionsTable = ActivityDashboardChartsFactory.createActivitiesTable(data, startDate, endDate);
        }
    }

    @Override
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

    public List<Map<String, Object>> getInteractionsTable()
    {
        return interactionsTable;
    }

    public Set<String> getInteractionsTableColumnNames()
    {
        if(interactionsTable == null)
            return null;
        return interactionsTable.size() > 0 ? interactionsTable.get(0).keySet() : new HashSet<>();
    }

    public Map<String, String> getActions()
    {
        return actions;
    }

    public List<String> getSelectedActionItems()
    {
        return selectedActionItems;
    }

    public void setSelectedActionItems(List<String> selectedActionItems)
    {
        this.selectedActionItems = selectedActionItems;
    }

    public List<SelectItemGroup> getGroupedActions()
    {
        return groupedActions;
    }

    public void setGroupedActions(List<SelectItemGroup> groupedActions)
    {
        this.groupedActions = groupedActions;
    }

    public List<Integer> getSelectedGroupedActions()
    {
        return selectedGroupedActions;
    }

    public void setSelectedGroupedActions(List<Integer> selectedGroupedActions)
    {
        this.selectedGroupedActions = selectedGroupedActions;
    }

}

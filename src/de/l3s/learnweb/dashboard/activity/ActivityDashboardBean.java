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
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.primefaces.model.charts.line.LineChartModel;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.learnweb.dashboard.activity.ActivityDashboardChartsFactory.ActivityGraphData;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;

@Named
@ViewScoped
public class ActivityDashboardBean extends CommonDashboardUserBean implements Serializable {
    private static final long serialVersionUID = 3326736281893564706L;

    private Map<String, String> actions;
    private List<SelectItemGroup> groupedActions;
    private List<String> selectedActionItems;
    private List<Integer> selectedGroupedActions;

    private transient LineChartModel interactionsChart;
    private transient List<Map<String, Object>> interactionsTable;

    @PostConstruct
    public void init() {
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

    private SelectItemGroup createGroupCheckboxes(String name, Set<Action> actions) {
        SelectItemGroup itemGroup = new SelectItemGroup(name);
        List<SelectItem> itemList = new ArrayList<>();
        for (Action action : actions) {
            itemList.add(new SelectItem(action.ordinal(), action.name()));
        }
        SelectItem[] itemArr = new SelectItem[itemList.size()];
        itemGroup.setSelectItems(itemList.toArray(itemArr));
        return itemGroup;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        try {
            selectedActionItems = new ArrayList<>(actions.keySet());

            cleanAndUpdateStoredData();
        } catch (SQLException e) {
            addErrorMessage(e);
        }
    }

    @Override
    public void cleanAndUpdateStoredData() throws SQLException {
        interactionsChart = null;
        interactionsTable = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() throws SQLException {
        if (!CollectionUtils.isEmpty(getSelectedUsersIds())) {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            if (selectedActionItems != null) {
                List<ActivityGraphData> data = new ArrayList<>();
                for (String activityGroupName : selectedActionItems) {
                    ActivityGraphData activityData = new ActivityGraphData();
                    activityData.setName(activityGroupName);
                    activityData.setActionsPerDay(getLearnweb().getActivityDashboardManager().getActionsCountPerDay(selectedUsersIds, startDate, endDate, actions.get(activityGroupName)));
                    data.add(activityData);
                }
                interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
                interactionsTable = ActivityDashboardChartsFactory.createActivitiesTable(data, startDate, endDate);
            } else if (selectedGroupedActions != null) {
                List<ActivityGraphData> data = new ArrayList<>();
                for (Integer activityGroupName : selectedGroupedActions) {
                    ActivityGraphData activityData = new ActivityGraphData();
                    activityData.setName(Action.values()[activityGroupName].name());
                    activityData.setActionsPerDay(getLearnweb().getActivityDashboardManager().getActionsCountPerDay(selectedUsersIds, startDate, endDate, activityGroupName.toString()));
                    data.add(activityData);
                }
                interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
                interactionsTable = ActivityDashboardChartsFactory.createActivitiesTable(data, startDate, endDate);
            }
        }
    }

    public LineChartModel getInteractionsChart() throws SQLException {
        if (null == interactionsChart) {
            fetchDataFromManager();
        }

        return interactionsChart;
    }

    public List<Map<String, Object>> getInteractionsTable() throws SQLException {
        if (null == interactionsTable) {
            fetchDataFromManager();
        }

        return interactionsTable;
    }

    public Set<String> getInteractionsTableColumnNames() throws SQLException {
        if (getInteractionsTable() == null) {
            return null;
        }

        return getInteractionsTable().isEmpty() ? new HashSet<>() : interactionsTable.get(0).keySet();
    }

    public Map<String, String> getActions() {
        return actions;
    }

    public List<String> getSelectedActionItems() {
        return selectedActionItems;
    }

    public void setSelectedActionItems(List<String> selectedActionItems) {
        this.selectedGroupedActions = null;
        this.selectedActionItems = selectedActionItems;
    }

    public List<SelectItemGroup> getGroupedActions() {
        return groupedActions;
    }

    public void setGroupedActions(List<SelectItemGroup> groupedActions) {
        this.groupedActions = groupedActions;
    }

    public List<Integer> getSelectedGroupedActions() {
        return selectedGroupedActions;
    }

    public void setSelectedGroupedActions(List<Integer> selectedGroupedActions) {
        this.selectedActionItems = null;
        this.selectedGroupedActions = selectedGroupedActions;
    }

    private static String getStringOfActions(Set<Action> actions) {
        return actions.stream()
            .map(a -> String.valueOf(a.ordinal()))
            .collect(Collectors.joining(","));
    }

}

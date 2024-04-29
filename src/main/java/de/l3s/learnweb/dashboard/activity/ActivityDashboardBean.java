package de.l3s.learnweb.dashboard.activity;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;
import de.l3s.learnweb.dashboard.activity.ActivityDashboardChartsFactory.ActivityGraphData;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.learnweb.logging.LogDao;

@Named
@ViewScoped
public class ActivityDashboardBean extends CommonDashboardUserBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 3326736281893564706L;

    private TreeMap<String, String> actions;
    private ArrayList<SelectItemGroup> groupedActions;
    private ArrayList<String> selectedActionItems;
    private ArrayList<Integer> selectedGroupedActions;

    private transient String interactionsChart;
    private transient List<Map<String, Object>> interactionsTable;

    @Inject
    private LogDao logDao;

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

        selectedActionItems = new ArrayList<>(actions.keySet());

        cleanAndUpdateStoredData();
    }

    @Override
    public void cleanAndUpdateStoredData() {
        interactionsChart = null;
        interactionsTable = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() {
        if (getSelectedUsersIds() != null && !getSelectedUsersIds().isEmpty()) {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            if (selectedActionItems != null) {
                List<ActivityGraphData> data = new ArrayList<>();
                for (String activityGroupName : selectedActionItems) {
                    ActivityGraphData activityData = new ActivityGraphData();
                    activityData.setName(activityGroupName);
                    activityData.setActionsPerDay(logDao.countActionsPerDay(selectedUsersIds, startDate, endDate, actions.get(activityGroupName)));
                    data.add(activityData);
                }
                interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
                interactionsTable = ActivityDashboardChartsFactory.createActivitiesTable(data, startDate, endDate);
            } else if (selectedGroupedActions != null) {
                List<ActivityGraphData> data = new ArrayList<>();
                for (Integer activityGroupName : selectedGroupedActions) {
                    ActivityGraphData activityData = new ActivityGraphData();
                    activityData.setName(Action.values()[activityGroupName].name());
                    activityData.setActionsPerDay(logDao.countActionsPerDay(selectedUsersIds, startDate, endDate, activityGroupName.toString()));
                    data.add(activityData);
                }
                interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
                interactionsTable = ActivityDashboardChartsFactory.createActivitiesTable(data, startDate, endDate);
            }
        }
    }

    public String getInteractionsChart() {
        if (null == interactionsChart) {
            fetchDataFromManager();
        }

        return interactionsChart;
    }

    public List<Map<String, Object>> getInteractionsTable() {
        if (null == interactionsTable) {
            fetchDataFromManager();
        }

        return interactionsTable;
    }

    public Set<String> getInteractionsTableColumnNames() {
        if (getInteractionsTable() == null) {
            return null;
        }

        return getInteractionsTable().isEmpty() ? new HashSet<>() : interactionsTable.getFirst().keySet();
    }

    public Map<String, String> getActions() {
        return actions;
    }

    public ArrayList<String> getSelectedActionItems() {
        return selectedActionItems;
    }

    public void setSelectedActionItems(ArrayList<String> selectedActionItems) {
        this.selectedGroupedActions = null;
        this.selectedActionItems = selectedActionItems;
    }

    public ArrayList<SelectItemGroup> getGroupedActions() {
        return groupedActions;
    }

    public void setGroupedActions(ArrayList<SelectItemGroup> groupedActions) {
        this.groupedActions = groupedActions;
    }

    public ArrayList<Integer> getSelectedGroupedActions() {
        return selectedGroupedActions;
    }

    public void setSelectedGroupedActions(ArrayList<Integer> selectedGroupedActions) {
        this.selectedActionItems = null;
        this.selectedGroupedActions = selectedGroupedActions;
    }

    private static String getStringOfActions(Set<Action> actions) {
        return actions.stream()
            .map(a -> String.valueOf(a.ordinal()))
            .collect(Collectors.joining(","));
    }

}

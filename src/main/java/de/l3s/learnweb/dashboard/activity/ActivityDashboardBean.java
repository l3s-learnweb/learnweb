package de.l3s.learnweb.dashboard.activity;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private LinkedHashMap<String, String> actions;
    private ArrayList<SelectItemGroup> groupedActions;
    private ArrayList<String> selectedActionItems;
    private ArrayList<Integer> selectedGroupedActions;

    private transient String interactionsChart;
    private transient List<Map<String, Object>> interactionsTable;
    private transient Map<String, String> interactionsTableHeaders; // column key -> header, the same names as in the chart legend

    @Inject
    private LogDao logDao;

    @PostConstruct
    public void init() {
        actions = new LinkedHashMap<>();
        groupedActions = new ArrayList<>();
        addActionGroup("activity.actions_resource", ActionCategory.RESOURCE);
        addActionGroup("activity.actions_folder", ActionCategory.FOLDER);
        addActionGroup("activity.actions_glossary", ActionCategory.GLOSSARY);
        addActionGroup("activity.actions_user", ActionCategory.USER);
        addActionGroup("activity.actions_search", ActionCategory.SEARCH);
        addActionGroup("activity.actions_group", ActionCategory.GROUP);
    }

    /**
     * @param msgKey the i18n key of the group name, it is also used as the value of the group checkbox
     */
    private void addActionGroup(String msgKey, ActionCategory category) {
        Set<Action> categoryActions = Action.getActionsByCategory(category);
        actions.put(msgKey, getStringOfActions(categoryActions));
        groupedActions.add(createGroupCheckboxes(getLocaleMessage(msgKey), categoryActions));
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
        interactionsTableHeaders = null;

        fetchDataFromManager();
    }

    private void fetchDataFromManager() {
        if (getSelectedUsersIds() != null && !getSelectedUsersIds().isEmpty()) {
            List<Integer> selectedUsersIds = getSelectedUsersIds();
            if (selectedActionItems != null) {
                List<ActivityGraphData> data = new ArrayList<>();
                for (String activityGroupName : selectedActionItems) {
                    ActivityGraphData activityData = new ActivityGraphData();
                    activityData.setKey(activityGroupName);
                    activityData.setName(getLocaleMessage(activityGroupName));
                    activityData.setActionsPerDay(logDao.countActionsPerDay(selectedUsersIds, startDate, endDate, actions.get(activityGroupName)));
                    data.add(activityData);
                }
                setInteractionsData(data);
            } else if (selectedGroupedActions != null) {
                List<ActivityGraphData> data = new ArrayList<>();
                for (Integer activityGroupName : selectedGroupedActions) {
                    ActivityGraphData activityData = new ActivityGraphData();
                    String actionName = Action.values()[activityGroupName].name();
                    activityData.setKey(actionName);
                    activityData.setName(actionName);
                    activityData.setActionsPerDay(logDao.countActionsPerDay(selectedUsersIds, startDate, endDate, activityGroupName.toString()));
                    data.add(activityData);
                }
                setInteractionsData(data);
            }
        }
    }

    private void setInteractionsData(List<ActivityGraphData> data) {
        interactionsChart = ActivityDashboardChartsFactory.createActivitiesChart(data, startDate, endDate);
        interactionsTable = ActivityDashboardChartsFactory.createActivitiesTable(data, startDate, endDate);

        interactionsTableHeaders = new HashMap<>();
        interactionsTableHeaders.put("date", getLocaleMessage("date"));
        data.forEach(activityData -> interactionsTableHeaders.put(activityData.getKey(), activityData.getName()));
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

    public Map<String, String> getInteractionsTableHeaders() {
        if (null == interactionsTableHeaders) {
            fetchDataFromManager();
        }

        return interactionsTableHeaders;
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

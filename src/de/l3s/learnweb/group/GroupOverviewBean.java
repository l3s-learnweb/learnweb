package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupOverviewBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -6297485484480890425L;
    private static final Logger log = LogManager.getLogger(GroupOverviewBean.class);

    private static final int MEMBERS_LIST_LIMIT = 12;
    private static final int ACTIVITY_LIST_LIMIT = 10;
    private static final Action[] OVERVIEW_ACTIONS = {
        Action.forum_topic_added, Action.deleting_resource, Action.adding_resource, Action.group_joining,
        Action.group_leaving, Action.forum_post_added, Action.changing_office_resource
    };

    private int groupId;
    private Group group;

    private boolean showAllMembers = false;
    private List<User> members;

    private String summaryTitle;
    private SummaryOverview groupSummary;

    private boolean showAllLogs = false;
    private List<LogEntry> logMessages;

    public void onLoad() throws SQLException {
        User user = getUser();
        if (null == user) { // not logged in
            return;
        }

        group = getLearnweb().getGroupManager().getGroupById(groupId);

        if (null == group) {
            addInvalidParameterMessage("group_id");
        }

        if (null != group) {
            group.setLastVisit(user);
        }
    }

    public void fetchAllMembers() throws SQLException {
        showAllMembers = true;
        members = getLearnweb().getUserManager().getUsersByGroupId(group.getId());
    }

    public boolean isShowAllMembers() throws SQLException {
        return showAllMembers || MEMBERS_LIST_LIMIT >= group.getMemberCount();
    }

    public List<User> getMembers() throws SQLException {
        if (null == members && group != null) {
            members = getLearnweb().getUserManager().getUsersByGroupId(group.getId(), MEMBERS_LIST_LIMIT);
        }
        return members;
    }

    public void fetchAllLogs() throws SQLException {
        showAllLogs = true;
        logMessages = getLearnweb().getLogManager().getLogsByGroup(groupId, -1);
    }

    public boolean isShowAllLogs() {
        return showAllLogs;
    }

    public List<LogEntry> getLogMessages() throws SQLException {
        if (null == logMessages) {
            logMessages = getLearnweb().getLogManager().getLogsByGroup(groupId, ACTIVITY_LIST_LIMIT);
        }
        return logMessages;
    }

    public String getSummaryTitle() {
        return summaryTitle;
    }

    public SummaryOverview getSummaryOverview() {
        try {
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, OVERVIEW_ACTIONS, LocalDateTime.now().minusWeeks(1), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_week_changes");
            }
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, OVERVIEW_ACTIONS, LocalDateTime.now().minusMonths(1), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_month_overview_changes");
            }
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, OVERVIEW_ACTIONS, LocalDateTime.now().minusMonths(6), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_six_month_changes");
            }
            return groupSummary;
        } catch (Exception e) {
            log.error("Can't create group summery", e);
            return null;
        }
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() {
        return group;
    }

    public boolean isMember() throws SQLException {
        User user = getUser();

        if (null == user) {
            return false;
        }

        if (null == group) {
            return false;
        }

        return group.isMember(user);
    }

    public boolean isUserDetailsHidden() {
        User user = getUser();
        if (user == null) {
            return false;
        }
        if (user.getOrganisation().getOption(Organisation.Option.Privacy_Anonymize_usernames)) {
            return true;
        }
        return false;
    }
}

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
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
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
        BeanAsserts.authorized(user);

        group = getLearnweb().getGroupManager().getGroupById(groupId);
        BeanAsserts.groupNotNull(group);

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
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, LocalDateTime.now().minusWeeks(1), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_week_changes");
            }
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, LocalDateTime.now().minusMonths(1), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_month_overview_changes");
            }
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, LocalDateTime.now().minusMonths(6), LocalDateTime.now());
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

package de.l3s.learnweb.group;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

@Named
@ViewScoped
public class GroupOverviewBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -6297485484480890425L;
    private static final Logger log = LogManager.getLogger(GroupOverviewBean.class);

    private static final int MEMBERS_LIST_LIMIT = 12;
    private static final int ACTIVITY_LIST_LIMIT = 10;

    private static final EnumSet<Action> OVERVIEW_ACTIONS = EnumSet.of(Action.forum_topic_added, Action.deleting_resource, Action.adding_resource,
        Action.group_joining, Action.group_leaving, Action.forum_post_added, Action.changing_office_resource);

    private int groupId;
    private Group group;

    private boolean showAllMembers = false;
    private List<User> members;

    private String summaryTitle;
    private SummaryOverview groupSummary;

    private boolean showAllLogs = false;
    private List<LogEntry> logMessages;

    @Inject
    private GroupDao groupDao;

    @Inject
    private UserDao userDao;

    @Inject
    private LogDao logDao;

    public void onLoad() {
        User user = getUser();
        BeanAssert.authorized(user);

        group = groupDao.findByIdOrElseThrow(groupId);

        if (null != group) {
            group.setLastVisit(user);
        }
    }

    public void fetchAllMembers() {
        showAllMembers = true;
        members = userDao.findByGroupId(group.getId());
    }

    public boolean isShowAllMembers() {
        return showAllMembers || MEMBERS_LIST_LIMIT >= group.getMemberCount();
    }

    public List<User> getMembers() {
        if (null == members && group != null) {
            members = userDao.findByGroupIdLastJoined(group.getId(), MEMBERS_LIST_LIMIT);
        }
        return members;
    }

    public void fetchAllLogs() {
        showAllLogs = true;
        logMessages = logDao.findByGroupId(groupId, Action.collectOrdinals(Action.LOGS_DEFAULT_FILTER));
        removeForeignResources(logMessages);
    }

    public boolean isShowAllLogs() {
        return showAllLogs;
    }

    public List<LogEntry> getLogMessages() {
        if (null == logMessages) {
            logMessages = logDao.findByGroupId(groupId, Action.collectOrdinals(Action.LOGS_DEFAULT_FILTER), ACTIVITY_LIST_LIMIT);
            removeForeignResources(logMessages);
        }
        return logMessages;
    }

    public String getSummaryTitle() {
        return summaryTitle;
    }

    public SummaryOverview getSummaryOverview() {
        try {
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = createSummaryOverview(LocalDateTime.now().minusWeeks(1), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_week_changes");
            }
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = createSummaryOverview(LocalDateTime.now().minusMonths(1), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_month_overview_changes");
            }
            if (groupSummary == null || groupSummary.isEmpty()) {
                groupSummary = createSummaryOverview(LocalDateTime.now().minusMonths(6), LocalDateTime.now());
                summaryTitle = getLocaleMessage("last_six_month_changes");
            }
            return groupSummary;
        } catch (Exception e) {
            log.error("Can't create group summery", e);
            return null;
        }
    }

    private void removeForeignResources(List<LogEntry> logs) {
        logs.removeIf(logEntry -> logEntry.getResource() != null && logEntry.getResource().getGroupId() != groupId);
    }

    private SummaryOverview createSummaryOverview(LocalDateTime from, LocalDateTime to) {
        List<LogEntry> logs = logDao.findByGroupIdBetweenTime(groupId, Action.collectOrdinals(OVERVIEW_ACTIONS), from, to);
        removeForeignResources(logs);

        if (logs.isEmpty()) {
            return null;
        }

        SummaryOverview summary = new SummaryOverview();

        for (LogEntry logEntry : logs) {
            switch (logEntry.getAction()) {
                case deleting_resource:
                    summary.getDeletedResources().add(logEntry);
                    break;
                case adding_resource:
                    if (logEntry.getResource() != null) {
                        summary.getAddedResources().add(logEntry);
                    }
                    break;
                case forum_topic_added:
                case forum_post_added:
                    summary.getForumsInfo().add(logEntry);
                    break;
                case group_joining:
                case group_leaving:
                    summary.getMembersInfo().add(logEntry);
                    break;
                case changing_office_resource:
                    if (logEntry.getResource() != null) {
                        Resource logEntryResource = logEntry.getResource();

                        if (summary.getUpdatedResources().containsKey(logEntryResource)) {
                            summary.getUpdatedResources().get(logEntryResource).add(logEntry);
                        } else {
                            summary.getUpdatedResources().put(logEntryResource, new ArrayList<>(Collections.singletonList(logEntry)));
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return summary;
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

    public boolean isMember() {
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

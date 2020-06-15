package de.l3s.learnweb.logging;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.SummaryOverview;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public final class LogManager {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LogManager.class);

    // TODO Philipp: WIP refactoring
    private static final String LOG_SELECT = "SELECT user_id, '',action, target_id, params, timestamp, ul.group_id, r.title AS resource_title, g.title AS group_title FROM lw_user_log ul LEFT JOIN lw_resource r ON action IN(0,1,2,3,15,14,19,21,32,11,54,55,6,8) AND target_id = r.resource_id LEFT JOIN lw_group g ON ul.group_id = g.group_id";
    private static final Action[] LOG_DEFAULT_FILTER = {Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource,
        Action.group_changing_description, Action.group_changing_leader, Action.group_changing_title, Action.group_creating, Action.group_deleting,
        Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource,
        Action.changing_office_resource, Action.forum_topic_added, Action.forum_post_added, Action.deleting_folder, Action.add_folder};

    private static final String resourceActionIds; // only log entries representing theses actions will be retrieved for a given resource
    private static LogManager instance;

    static {
        Set<Action> resourceActions = Arrays.stream(Action.values()).filter(a -> a.getTargetId() == ActionTargetId.RESOURCE_ID).collect(Collectors.toSet());
        // remove actions we don't want to show
        resourceActions.remove(Action.opening_resource);
        resourceActions.remove(Action.glossary_open);
        resourceActions.remove(Action.lock_interrupted_returned_resource);
        resourceActions.remove(Action.lock_rejected_edit_resource);
        resourceActions.remove(Action.downloading);

        resourceActionIds = resourceActions.stream().map(a -> Integer.toString(a.ordinal())).collect(Collectors.joining(","));
    }

    private final Learnweb learnweb;

    private LogManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    /**
     * @param groupId the group this action belongs to; null if no group
     * @param targetId optional value; should be 0 if not required
     */
    public void log(User user, Action action, int groupId, int targetId, String params, String sessionId) {
        int userId = 0;
        if (user != null) {
            userId = user.getId();

            if (user.getOrganisation().getOption(Organisation.Option.Privacy_Logging_disabled)) {
                return; // we are not allowed to log events for this user
            }
        }

        if (groupId == -1) {
            groupId = 0;
        }

        log(userId, action, groupId, targetId, params, sessionId);
    }

    /**
     * Logs a user action. The parameters "targetId" and "params" depend on the
     * logged action. Look at the code of @see de.l3s.learnweb.logging.Action for explanation.
     *
     * @param groupId the group this action belongs to; null if no group
     * @param targetId optional value; should be 0 if not required
     */
    private void log(int userId, Action action, int groupId, int targetId, String params, String sessionId) {
        if (null == action) {
            throw new IllegalArgumentException();
        }

        params = StringHelper.shortnString(params, 250);

        try (PreparedStatement pstmtLog = learnweb.getConnection().prepareStatement("INSERT INTO `lw_user_log` (`user_id`, `session_id`, `action`, `target_id`, `params`, `group_id`, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            pstmtLog.setInt(1, userId);
            pstmtLog.setString(2, sessionId);
            pstmtLog.setInt(3, action.ordinal());
            pstmtLog.setInt(4, targetId);
            pstmtLog.setString(5, params);
            pstmtLog.setInt(6, groupId);
            pstmtLog.setTimestamp(7, new Timestamp(new Date().getTime()));
            pstmtLog.executeUpdate();
        } catch (SQLException e) {
            log.error("Can't store log entry: " + action + "; Target: " + targetId + "; User: " + userId, e);
        }
    }

    /**
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entries are returned
     */
    public List<LogEntry> getLogsByUser(int userId, Action[] actions, int limit) throws SQLException {

        if (null == actions) {
            actions = LOG_DEFAULT_FILTER;
        }

        return getLogs(LOG_SELECT + " WHERE user_id = ? AND action IN(" + idsFromActions(actions) + ") ORDER BY timestamp DESC LIMIT " + limit, userId);
    }

    public List<LogEntry> getLogs(String query, Object... parameter) {
        LinkedList<LogEntry> logEntries = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement(query)) {
            int i = 1;
            for (Object param : parameter) {
                select.setObject(i++, param);
            }

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                logEntries.add(new LogEntry(rs));
            }
        } catch (SQLException e) {
            log.error("Can't get logs for query: " + query + "; parameter: " + Arrays.asList(parameter), e);
        }
        return logEntries;
    }

    /**
     * @param limit if limit is -1 all log entries are returned
     */
    public List<LogEntry> getLogsByResource(Resource resource, int limit) throws SQLException {
        // TODO philipp change query and add index
        String limitStr = limit > 0 ? "LIMIT " + limit : "";

        Instant start = Instant.now();
        List<LogEntry> logs = getLogs(LOG_SELECT + " WHERE ul.group_id = ? AND action IN(" + resourceActionIds + ") AND target_id = ? ORDER BY timestamp DESC " + limitStr, resource.getGroupId(), resource.getId());

        int duration = Duration.between(start, Instant.now()).getNano();
        if (duration > 500000) {
            log.warn("getLogs took {}ns; resourceId: {}; limit: {};", duration, resource.getId(), limit);
        }

        return logs;
        /*
         * LIMIT ? OFFSET ?
         *  int page, int pageSize
         *          select.setInt(2, pageSize);
        select.setInt(3, page * pageSize);
         */
    }

    /**
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entries are returned
     */
    public List<LogEntry> getLogsByGroup(int groupId, Action[] actions, int limit) throws SQLException {
        if (null == actions) {
            actions = LOG_DEFAULT_FILTER;
        }

        String limitStr = limit > 0 ? "LIMIT " + limit : "";

        return getLogs(LOG_SELECT + " WHERE ul.group_id = ? AND user_id != 0 AND action IN(" + idsFromActions(actions) + ") ORDER BY timestamp DESC " + limitStr, groupId);
    }

    public SummaryOverview getLogsByGroup(int groupId, Action[] actions, LocalDateTime from, LocalDateTime to) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement(LOG_SELECT + " WHERE ul.group_id = ? AND user_id != 0 "
            + "AND action IN(" + idsFromActions(actions) + ") AND timestamp between ? AND ? ORDER BY timestamp DESC ")) {
            select.setInt(1, groupId);
            select.setTimestamp(2, Timestamp.valueOf(from));
            select.setTimestamp(3, Timestamp.valueOf(to));
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                return null;
            }

            SummaryOverview summary = new SummaryOverview();

            do {
                LogEntry logEntry = new LogEntry(rs);
                switch (logEntry.getAction()) {
                    case deleting_resource:
                        summary.getDeletedResources().add(logEntry);
                        break;
                    case adding_resource:
                        Resource resource = logEntry.getResource();
                        if (resource != null) {
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
                        Resource logEntryResource = logEntry.getResource();
                        if (logEntryResource != null) {
                            if (summary.getUpdatedResources().containsKey(logEntryResource)) {
                                summary.getUpdatedResources().get(logEntryResource).add(logEntry);
                            } else {
                                summary.getUpdatedResources().put(logEntryResource, new LinkedList<>(Collections.singletonList(logEntry)));
                            }
                        }
                        break;
                    default:
                        break;
                }
            } while (rs.next());

            return summary;
        }

    }

    public List<LogEntry> getActivityLogOfUserGroups(int userId, Action[] actions, int limit) throws SQLException {
        LinkedList<LogEntry> log = new LinkedList<>();

        if (null == actions) {
            actions = LOG_DEFAULT_FILTER;
        }

        String limitStr = "";
        if (limit > 0) {
            limitStr = "LIMIT " + limit;
        }

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            LOG_SELECT + " WHERE ul.group_id IN(SELECT group_id FROM lw_group_user WHERE user_id=?) "
                + "AND action IN(" + idsFromActions(actions) + ") AND user_id != 0 AND user_id!=? ORDER BY timestamp DESC " + limitStr)) {
            select.setInt(1, userId);
            select.setInt(2, userId);

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                log.add(new LogEntry(rs));
            }
        }

        return log;
    }

    public static LogManager getInstance(Learnweb learnweb) {
        if (instance == null) {
            instance = new LogManager(learnweb);
        }
        return instance;
    }

    private static String idsFromActions(Action[] actions) {
        StringBuilder sb = new StringBuilder();
        for (Action action : actions) {
            sb.append(",");
            sb.append(action.ordinal());
        }
        return sb.substring(1);
    }

}

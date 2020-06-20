package de.l3s.learnweb.logging;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.SummaryOverview;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public final class LogManager {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LogManager.class);

    private static final String LOG_SELECT = "SELECT `log_entry_id`,`user_id`,`session_id`,`action`,`target_id`,`params`,`timestamp`,`group_id` FROM `lw_user_log`";

    private static final Action[] LOG_DEFAULT_FILTER = {Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource,
        Action.group_changing_description, Action.group_changing_leader, Action.group_changing_title, Action.group_creating, Action.group_deleting,
        Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource,
        Action.changing_office_resource, Action.forum_topic_added, Action.forum_post_added, Action.deleting_folder, Action.add_folder};

    private static final String resourceActionIds; // only logEntries representing these actions will be retrieved for a given resource
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
    private LoadingCache<Resource, List<LogEntry>> logsByResourceCache;

    public static LogManager getInstance(Learnweb learnweb) {
        if (instance == null) {
            instance = new LogManager(learnweb);
        }
        return instance;
    }

    private LogManager(Learnweb learnweb) {
        this.learnweb = learnweb;

        logsByResourceCache = CacheBuilder.newBuilder()
            .expireAfterAccess(3000, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<Resource, List<LogEntry>>() {
                @Override
                public List<LogEntry> load(Resource resource) throws Exception {
                    int limit = 50;

                    Instant start = Instant.now();

                    List<LogEntry> logs = getLogs(false, LOG_SELECT + " WHERE group_id = ? AND action IN(" + resourceActionIds + ") AND target_id = ? ORDER BY timestamp DESC ", resource.getGroupId(), resource.getId());

                    long duration = Duration.between(start, Instant.now()).toMillis();
                    if (duration > 100) {
                        log.warn("getLogs took {}ms; resourceId: {}; limit: {};", duration, resource.getId(), limit);
                    }

                    return logs;
                }
            });
    }

    /**
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entries are returned
     */
    public List<LogEntry> getLogsByUser(int userId, Action[] actions, int limit, boolean includePrivateEntries) throws SQLException {

        if (null == actions) {
            actions = LOG_DEFAULT_FILTER;
        }

        return getLogs(includePrivateEntries, LOG_SELECT + " WHERE user_id = ? AND action IN(" + idsFromActions(actions) + ") ORDER BY timestamp DESC LIMIT " + limit, userId);
    }

    /**
     *
     * @param includePrivateEntries set to true if user accesses his own logs. Or for moderators.
     * @param query
     * @param parameter
     * @return
     */
    public List<LogEntry> getLogs(boolean includePrivateEntries, String query, Object... parameter) {
        LinkedList<LogEntry> logEntries = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement(query)) {
            int i = 1;
            for (Object param : parameter) {
                select.setObject(i++, param);
            }

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                LogEntry logEntry = createLogEntry(rs);

                if (includePrivateEntries || !logEntry.isPrivate()) {
                    logEntries.add(logEntry);
                }
            }
        } catch (SQLException e) {
            log.error("Can't get logs for query: " + query + "; parameter: " + Arrays.asList(parameter), e);
        }
        return logEntries;
    }

    public List<LogEntry> getLogsByResource(Resource resource) throws SQLException {

        log.debug("get logs for resource: {}", resource);
        try {
            return logsByResourceCache.get(resource);
        } catch (ExecutionException e) {
            log.error("Can't get logs for resource: {}", resource, e);
            return null;
        }
    }

    /**
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entries are returned
     */
    public List<LogEntry> getLogsByGroup(int groupId, int limit) throws SQLException {

        Action[] actions = LOG_DEFAULT_FILTER;

        String limitStr = limit > 0 ? "LIMIT " + limit : "";

        return getLogs(false, LOG_SELECT + " WHERE group_id = ? AND user_id != 0 AND action IN(" + idsFromActions(actions) + ") ORDER BY timestamp DESC " + limitStr, groupId);
    }

    private static final Action[] OVERVIEW_ACTIONS = {
        Action.forum_topic_added, Action.deleting_resource, Action.adding_resource, Action.group_joining,
        Action.group_leaving, Action.forum_post_added, Action.changing_office_resource
    };

    public SummaryOverview getLogsByGroup(int groupId, LocalDateTime from, LocalDateTime to) throws SQLException {

        List<LogEntry> logs = getLogs(false, LOG_SELECT + " WHERE group_id = ? "
            + "AND action IN(" + idsFromActions(OVERVIEW_ACTIONS) + ") AND user_id != 0 AND timestamp between ? AND ? ORDER BY timestamp DESC ", groupId, Timestamp.valueOf(from), Timestamp.valueOf(to));

        if (logs.size() == 0) {
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
                            summary.getUpdatedResources().put(logEntryResource, new ArrayList<>(Arrays.asList(logEntry)));
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return summary;
    }

    /**
     * Returns the newest log entries from the given users groups.
     * This doesn't include the user's own actions.
     *
     * @param user
     * @param actions
     * @param limit
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getActivityLogOfUserGroups(User user, Action[] actions, int limit) throws SQLException {
        LinkedList<LogEntry> logs = new LinkedList<>();

        if (user.getGroupCount() == 0) { // no groups, no logs to return
            return logs;
        }

        if (null == actions) {
            actions = LOG_DEFAULT_FILTER;
        }

        String limitStr = "";

        if (limit > 0) {
            limitStr = "LIMIT " + limit;
        }

        // ids of all groups the user is member of
        String groupIds = user.getGroups().stream().map(g -> Integer.toString(g.getId())).collect(Collectors.joining(","));

        return getLogs(false, LOG_SELECT + " WHERE group_id IN(" + groupIds + ") AND action IN(" + idsFromActions(actions) + ") AND user_id != 0 AND user_id!=? ORDER BY timestamp DESC " + limitStr, user.getId());
    }

    private static String idsFromActions(Action[] actions) {
        StringBuilder sb = new StringBuilder();
        for (Action action : actions) {
            sb.append(",");
            sb.append(action.ordinal());
        }
        return sb.substring(1);
    }

    private LogEntry createLogEntry(ResultSet rs) throws SQLException {

        int userId = rs.getInt("user_id");

        Action action = Action.values()[rs.getInt("action")];
        String params = rs.getString("params");
        Date date = new Date(rs.getTimestamp("timestamp").getTime());
        int groupId = rs.getInt("group_id");

        int targetId = rs.getInt("target_id");

        return new LogEntry(userId, action, date, params, groupId, targetId);
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
}

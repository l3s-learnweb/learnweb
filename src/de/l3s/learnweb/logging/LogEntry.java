package de.l3s.learnweb.logging;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = -4239479043091966928L;

    private final int userId;
    private final Action action;
    private final Date date;
    private final String params;
    private final int groupId;
    private String username;
    private String description;
    private int resourceId;
    private String userImage = "";

    // cache
    private transient Resource resource;

    public LogEntry(ResultSet rs) throws SQLException {
        userId = rs.getInt("user_id");
        User user = Learnweb.getInstance().getUserManager().getUser(userId);
        if (user != null) {
            username = user.getUsername();
            userImage = user.getImage();
        }

        action = Action.values()[rs.getInt("action")];
        params = rs.getString("params");
        date = new Date(rs.getTimestamp("timestamp").getTime());
        groupId = rs.getInt("group_id");

        String groupTitle = rs.getString("group_title");
        String resourceTitle = rs.getString("resource_title");

        int targetId = rs.getInt("target_id");

        switch (action.getTargetId()) {
            case RESOURCE_ID:
                resourceId = targetId;
                break;
            default:
                break; // right now other id types are not handled
        }

        description = createDescription(this, targetId, groupTitle, resourceTitle);
    }

    public int getUserId() {
        return userId;
    }

    public Action getAction() {
        return action;
    }

    public int getGroupId() {
        return groupId;
    }

    public Date getDate() {
        return date;
    }

    public String getParams() {
        return params;
    }

    public String getUsername() {
        return username;
    }

    public String getDescription() {
        return description;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getUserImage() {
        return userImage;
    }

    public Resource getResource() throws SQLException {
        if (resource == null && resourceId > 0) {
            resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);

            if (resource == null || resource.isDeleted()) {
                resourceId = 0;
                resource = null;
            }
        }
        return resource;
    }

    public boolean isQueryNeeded() {
        return action == Action.adding_resource && resource != null && !resource.getQuery().equalsIgnoreCase("none");

    }

    private static String createDescription(final LogEntry logEntry, final int targetId, String groupTitle, String resourceTitle) throws SQLException {
        String url = ""; // Learnweb.getInstance().getServerUrl() + "/lw/";

        String usernameLink = "<a href=\"" + url + "user/detail.jsf?user_id=" + logEntry.getUserId() + "\">" + logEntry.getUsername() + "</a> ";

        resourceTitle = (null == resourceTitle) ? "a resource" : "<b>" + StringHelper.shortnString(resourceTitle, 80) + "</b>";

        String groupLink;

        if (null == groupTitle) {
            groupTitle = "private group";
            groupLink = "private group";
        } else {
            groupTitle = StringHelper.shortnString(groupTitle, 80);
            groupLink = "<a href=\"" + url + "group/overview.jsf?group_id=" + logEntry.getGroupId() + "\" >" + groupTitle + "</a>";
        }

        switch (logEntry.getAction()) {
            // Resource action
            case adding_resource:
                return usernameLink + UtilBean.getLocaleMessage("log_adding_resource", resourceTitle, groupLink);
            case deleting_resource:
                if (!StringUtils.isEmpty(logEntry.getParams())) {
                    resourceTitle = "<b>" + logEntry.getParams() + "</b>";
                }
                return usernameLink + UtilBean.getLocaleMessage("log_deleting_resource", resourceTitle); // resourceTitle
            case edit_resource:
                return usernameLink + UtilBean.getLocaleMessage("log_edit_resource", resourceTitle);
            case move_resource:
                return usernameLink + UtilBean.getLocaleMessage("log_move_resource", resourceTitle);
            case opening_resource:
                return usernameLink + UtilBean.getLocaleMessage("log_opening_resource", resourceTitle);
            case tagging_resource:
                return usernameLink + UtilBean.getLocaleMessage("log_tagging_resource", resourceTitle, logEntry.getParams());
            case commenting_resource:
                String description = usernameLink + UtilBean.getLocaleMessage("log_commenting_resource", resourceTitle);
                Comment comment = Learnweb.getInstance().getResourceManager().getComment(StringHelper.parseInt(logEntry.getParams()));
                if (comment != null) {
                    description += " " + UtilBean.getLocaleMessage("with") + " " + "<b>" + comment.getText() + "</b>";
                }
                return description;
            case rating_resource:
            case thumb_rating_resource:
                return usernameLink + UtilBean.getLocaleMessage("log_thumb_rating_resource", resourceTitle);
            case searching:
                return usernameLink + UtilBean.getLocaleMessage("log_searching_resource", logEntry.getParams());
            case downloading:
                return usernameLink + UtilBean.getLocaleMessage("log_downloading", resourceTitle);
            case changing_office_resource:
                return usernameLink + UtilBean.getLocaleMessage("log_document_changing", resourceTitle);
            case adding_resource_metadata:
                return usernameLink + UtilBean.getLocaleMessage("log_add_resource_metadata", logEntry.getParams()) + resourceTitle;

            // Folder actions
            case add_folder:
                return usernameLink + UtilBean.getLocaleMessage("log_add_folder", logEntry.getParams());
            case deleting_folder:
                return usernameLink + UtilBean.getLocaleMessage("log_deleting_folder", logEntry.getParams());
            case move_folder:
                return usernameLink + UtilBean.getLocaleMessage("log_move_folder", logEntry.getParams());
            case opening_folder:
                return usernameLink + UtilBean.getLocaleMessage("log_open_folder", logEntry.getParams());

            // Group actions
            case group_joining:
                return usernameLink + UtilBean.getLocaleMessage("log_group_joining", groupLink);
            case group_leaving:
                return usernameLink + UtilBean.getLocaleMessage("log_group_leaving", groupLink);
            case group_creating:
                return usernameLink + UtilBean.getLocaleMessage("log_group_creating", groupLink);
            case group_deleting:
                return usernameLink + UtilBean.getLocaleMessage("log_group_deleting", groupTitle);
            case group_changing_title:
                return usernameLink + UtilBean.getLocaleMessage("log_group_changing_title", groupLink);
            case group_changing_description:
                return usernameLink + UtilBean.getLocaleMessage("log_group_changing_description", groupLink);
            case group_changing_leader:
                return usernameLink + UtilBean.getLocaleMessage("log_group_changing_leader", groupLink);
            case group_deleting_link:
                return usernameLink + UtilBean.getLocaleMessage("log_group_deleting_link", groupLink);
            case forum_topic_added:
                String topicLink = "<a href=\"" + url + "group/forum_post.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + logEntry.getParams() + "</a>";
                return usernameLink + "has added " + "<b>" + topicLink + "</b>" + " post";
            case forum_post_added:
                String topic = "<a href=\"" + url + "group/forum_post.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + logEntry.getParams() + "</a>";
                return usernameLink + "has replied to " + "<b>" + topic + "</b>" + " topic";

            // General actions
            case login:
                return usernameLink + UtilBean.getLocaleMessage("log_login");
            case logout:
                return usernameLink + UtilBean.getLocaleMessage("log_logout");
            case register:
                return usernameLink + UtilBean.getLocaleMessage("log_register");
            case changing_profile:
                return usernameLink + UtilBean.getLocaleMessage("log_change_profile");
            case submission_view_resources:
                return usernameLink + UtilBean.getLocaleMessage("log_submission_view_resources");
            case submission_submitted:
                return usernameLink + UtilBean.getLocaleMessage("log_submission_submit");
            default:
                return "no message for action " + logEntry.getAction().name(); // should never happen;

            // unused translations that might become useful again: log_opening_url_resource, log_group_removing_resource
        }
    }
}

package de.l3s.learnweb.logging;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.ProfileBean;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = -4239479233091966928L;
    private static final Logger log = LogManager.getLogger(ProfileBean.class);

    private final int userId;
    private final Action action;
    private final Date date;
    private final String params;
    private final int groupId;
    private final int targetId;

    // cache
    private transient Optional<Resource> resource;
    private transient User user;
    private transient Group group;
    private Map<Locale, String> descriptions; // stores a description of this entry for different locales

    LogEntry(int userId, Action action, Date date, String params, int groupId, int targetId) {
        super();
        this.userId = userId;
        this.action = action;
        this.date = date;
        this.params = params;
        this.groupId = groupId;
        this.targetId = targetId;
    }

    public User getUser() throws SQLException {
        if (null == user) {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public Group getGroup() throws SQLException {
        if (null == group) {
            group = Learnweb.getInstance().getGroupManager().getGroupById(groupId);
        }
        return group;
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

    public Resource getResource() throws SQLException {
        if (resource == null) {
            if (action.getTargetId().equals(ActionTargetId.RESOURCE_ID) && targetId > 0) {
                Resource r = Learnweb.getInstance().getResourceManager().getResource(targetId);

                resource = (r == null || r.isDeleted()) ? Optional.empty() : Optional.of(r);
            } else {
                resource = Optional.empty();
            }
        }
        return resource.orElse(null);
    }

    public boolean isQueryNeeded() throws SQLException {
        return action == Action.adding_resource && getResource() != null && !getResource().getQuery().equalsIgnoreCase("none");
    }

    private String getGroupLink(Locale locale) {
        if (getGroupId() == 0) {
            return "<a href=\"" + Learnweb.getInstance().getServerUrl() + "/lw/myhome/resources.jsf\" >" + LanguageBundle.getLocaleMessage(locale, "myPrivateResources") + "</a> ";
        }

        try {
            Group group = getGroup();

            if (null == group) {
                return "Deleted group";
            } else {
                return "<a href=\"" + Learnweb.getInstance().getServerUrl() + "/lw/group/overview.jsf?group_id=" + getGroupId() + "\" target=\"_top\">" + group.getTitle() + "</a> ";
            }
        } catch (SQLException e) {
            log.error("Can't create the group link; groupId: {}", groupId, e);
            return "a group ";
        }
    }

    private String getUsernameLink(Locale locale) {
        try {
            if (getUser() == null || getUser().isDeleted()) {
                return "Deleted user";
            }
            return "<a href=\"" + Learnweb.getInstance().getServerUrl() + "/lw/user/detail.jsf?user_id=" + getUserId() + "\" target=\"_top\">" + getUser().getUsername() + "</a>";
        } catch (SQLException e) {
            log.error("Can't create the user link; userId: {}", userId, e);
            return "a user";
        }
    }

    private String getResourceLink(Locale locale) {
        try {
            if (getResource() != null) {
                return "<a href=\"" + Learnweb.getInstance().getServerUrl() + "/lw/resource.jsf?resource_id=" + getResource().getId() + "\" target=\"_top\"><b>" + StringHelper.shortnString(getResource().getTitle(), 40) + "</b></a> ";
            }
        } catch (SQLException e) {
            log.error("Can't create the resource link; resourceId: {}", targetId, e);
        }
        return LanguageBundle.getLocaleMessage(locale, "log_a_resource");
    }

    public boolean isPrivate() {
        switch (getAction()) {
            case adding_resource:
                return getGroupId() == 0; // added to "my private resources"
            default:
                return false;
        }
    }

    public String getDescription(Locale locale) throws SQLException {
        // create description cache if it doesn't exist yet
        if (null == descriptions) {
            descriptions = new HashMap<Locale, String>();
        }

        // try to get description from cache
        String description = descriptions.get(locale);
        if (description != null) {
            return description;
        }

        String usernameLink = getUsernameLink(locale) + " ";

        switch (getAction()) {
            case adding_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_adding_resource", getResourceLink(locale), getGroupLink(locale));
                break;
            case deleting_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_deleting_resource", "<b>" + getParams() + "</b>");
                break;
            case edit_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_edit_resource", getResourceLink(locale));
                break;
            case move_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_move_resource", getResourceLink(locale));
                break;
            case opening_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_opening_resource", getResourceLink(locale));
                break;
            case tagging_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_tagging_resource", getResourceLink(locale), getParams());
                break;
            case commenting_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_commenting_resource", getResourceLink(locale));
                Comment comment = Learnweb.getInstance().getResourceManager().getComment(NumberUtils.toInt(getParams()));
                if (comment != null) {
                    description += " " + LanguageBundle.getLocaleMessage(locale, "with") + " " + "<b>" + StringHelper.shortnString(comment.getText(), 100) + "</b>";
                }
                break;
            case deleting_comment:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_deleting_comment", getResourceLink(locale));
                break;
            case rating_resource:
            case thumb_rating_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_thumb_rating_resource", getResourceLink(locale));
                break;
            case searching:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_searching_resource", getParams());
                break;
            case downloading:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_downloading", getResourceLink(locale));
                break;
            case changing_office_resource:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_document_changing", getResourceLink(locale));
                break;
            case adding_resource_metadata:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_add_resource_metadata", getParams()) + getResourceLink(locale);

                // Folder actions
                break;
            case add_folder:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_add_folder", getParams());
                break;
            case deleting_folder:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_deleting_folder", getParams());
                break;
            case move_folder:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_move_folder", getParams());
                break;
            case opening_folder:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_open_folder", getParams());

                // Group actions
                break;
            case group_joining:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_joining", getGroupLink(locale));
                break;
            case group_leaving:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_leaving", getGroupLink(locale));
                break;
            case group_creating:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_creating", getGroupLink(locale));
                break;
            case group_deleting:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_deleting", getParams());
                break;
            case group_changing_title:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_changing_title", getGroupLink(locale));
                break;
            case group_changing_description:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_changing_description", getGroupLink(locale));
                break;
            case group_changing_leader:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_changing_leader", getGroupLink(locale));
                break;
            case group_deleting_link:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_deleting_link", getGroupLink(locale));
                break;
            case forum_topic_added:
                String topicLink = "<a href=\"" + Learnweb.getInstance().getServerUrl() + "/lw/group/forum_topic.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + getParams() + "</a>";
                description = usernameLink + "has added " + "<b>" + topicLink + "</b>" + " post";
                break;
            case forum_post_added:
                String topic = "<a href=\"" + Learnweb.getInstance().getServerUrl() + "/lw/group/forum_topic.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + getParams() + "</a>";
                description = usernameLink + "has replied to " + "<b>" + topic + "</b>" + " topic";

                // General actions
                break;
            case login:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_login");
                break;
            case logout:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_logout");
                break;
            case register:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_register");
                break;
            case changing_profile:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_change_profile");
                break;
            case submission_view_resources:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_submission_view_resources");
                break;
            case submission_submitted:
                description = usernameLink + LanguageBundle.getLocaleMessage(locale, "log_submission_submit");
                break;
            case glossary_entry_edit:
                description = usernameLink + " has edited an entry of " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
                //LanguageBundle.getLocaleMessage(locale, "log_glossary_entry_edit", getResourceLink(locale));
                break;
            case glossary_entry_delete:
                description = usernameLink + "has deleted an entry from " + getResourceLink(locale); // TODO @kemkes: incorporate details of entry, translate
                break;
            case glossary_entry_add:
                description = usernameLink + "has added an entry to " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
                break;
            case glossary_term_edit:
                description = usernameLink + "has edited a term in " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
                break;
            case glossary_term_add:
                description = usernameLink + "has added a term to " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
                break;
            case glossary_term_delete:
                description = usernameLink + "has removed a term from " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
                break;

            default:
                if (getAction().getTargetId().equals(ActionTargetId.RESOURCE_ID)) {
                    description = usernameLink + " has executed action <i>" + getAction().name() + "</i> on " + getResourceLink(locale);
                } else {
                    description = "Performed action <i>" + getAction().name() + "</i>"; // should never happen;
                }

        }
        // unused translations that might become useful again: log_opening_url_resource, log_group_removing_resource

        this.descriptions.put(locale, description);

        return description;
    }
}

package de.l3s.learnweb.logging;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.commons.lang3.math.NumberUtils;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = -4239479233091966928L;

    private final int userId;
    private final Action action;
    private final LocalDateTime date;
    private final String params;
    private final int groupId;
    private final int targetId;

    // cache
    private transient Optional<Resource> resource;
    private transient User user;
    private transient Group group;
    private HashMap<Locale, String> descriptions; // stores a description of this entry for different locales

    public LogEntry(int userId, Action action, LocalDateTime date, String params, int groupId, int targetId) {
        this.userId = userId;
        this.action = action;
        this.date = date;
        this.params = params;
        this.groupId = groupId;
        this.targetId = targetId;
    }

    /**
     * @return the user who performed the action, null if the user wasn't logged in
     */
    public User getUser() {
        if (null == user && userId != 0) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public Group getGroup() {
        if (null == group) {
            group = Learnweb.dao().getGroupDao().findByIdOrElseThrow(groupId);
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

    public LocalDateTime getDate() {
        return date;
    }

    public String getParams() {
        return params;
    }

    /**
     * Params usually contain user input (titles, tags, queries), they must be escaped before being embedded into the HTML description.
     */
    private String getParamsHtml() {
        return StringHelper.escapeHtml(params);
    }

    public Resource getResource() {
        if (resource == null) {
            if (action.getTargetId() == ActionTargetId.RESOURCE_ID && targetId != 0) {
                resource = Learnweb.dao().getResourceDao().findById(targetId).filter(res -> !res.isDeleted());
            } else {
                resource = Optional.empty();
            }
        }
        return resource.orElse(null);
    }

    public boolean isQueryNeeded() {
        return action == Action.adding_resource && getResource() != null && getResource().getQuery() != null;
    }

    private String getGroupLink(ResourceBundle bundle) {
        if (getGroupId() == 0) {
            return "<a href=\"myhome/resources.jsf\" >" + bundle.getString("myPrivateResources") + "</a> ";
        }

        Group group = getGroup();

        if (null == group) {
            return "<b>" + bundle.getString("deleted_group") + "</b>";
        } else {
            return "<a href=\"group/overview.jsf?group_id=" + getGroupId() + "\" target=\"_top\">" + StringHelper.escapeHtml(group.getTitle()) + "</a> ";
        }
    }

    private String getUsernameLink(ResourceBundle bundle) {
        if (getUser() == null || getUser().isDeleted()) {
            return "<b>" + bundle.getString("deleted_user") + "</b>";
        }
        return "<a href=\"user/detail.jsf?user_id=" + getUserId() + "\" target=\"_top\">" + StringHelper.escapeHtml(getUser().getDisplayName()) + "</a>";
    }

    private String getCommentText(int commentId, ResourceBundle bundle) {
        Optional<Comment> comment = Learnweb.dao().getCommentDao().findById(commentId);
        return comment.map(value -> " " + bundle.getString("with") + " <b>"
            + StringHelper.escapeHtml(StringHelper.shortnString(value.getText(), 100)) + "</b>").orElse("");
    }

    private String getResourceLink(ResourceBundle bundle) {
        if (getResource() != null) {
            return "<a href=\"resource.jsf?resource_id=" + getResource().getId() + "\" target=\"_top\"><b>" + StringHelper.escapeHtml(StringHelper.shortnString(getResource().getTitle(), 40)) + "</b></a> ";
        }
        return bundle.getString("log_a_resource");
    }

    private String getForumLink(ResourceBundle bundle) {
        return "<a href=\"group/forum_topic.jsf?topic_id=" + targetId + "\" target=\"_top\"><b>" + getParamsHtml() + "</b></a> ";
    }

    public boolean isPrivate() {
        return switch (getAction()) {
            case adding_resource -> getGroupId() == 0; // added to "my private resources"
            default -> false;
        };
    }

    public String getDescription(Locale locale) {
        // create description cache if it doesn't exist yet
        if (null == descriptions) {
            descriptions = new HashMap<>();
        }

        // try to get description from cache
        String description = descriptions.get(locale);
        if (description != null) {
            return description;
        }

        MessagesBundle bundle = new MessagesBundle(locale);
        String usernameLink = getUsernameLink(bundle) + " ";

        description = switch (getAction()) {
            case adding_resource:
                yield usernameLink + bundle.format("log_adding_resource", getResourceLink(bundle), getGroupLink(bundle));
            case deleting_resource:
                yield usernameLink + bundle.format("log_deleting_resource", "<b>" + getParamsHtml() + "</b>");
            case edit_resource:
                yield usernameLink + bundle.format("log_edit_resource", getResourceLink(bundle));
            case move_resource:
                yield usernameLink + bundle.format("log_move_resource", getResourceLink(bundle));
            case opening_resource:
                yield usernameLink + bundle.format("log_opening_resource", getResourceLink(bundle));
            case tagging_resource:
                yield usernameLink + bundle.format("log_tagging_resource", getResourceLink(bundle), getParamsHtml());
            case commenting_resource:
                yield usernameLink + bundle.format("log_commenting_resource", getResourceLink(bundle))
                    + getCommentText(NumberUtils.toInt(getParams()), bundle);
            case deleting_comment:
                yield usernameLink + bundle.format("log_deleting_comment", getResourceLink(bundle));
            case rating_resource, thumb_rating_resource:
                yield usernameLink + bundle.format("log_thumb_rating_resource", getResourceLink(bundle));
            case searching:
                yield usernameLink + bundle.format("log_searching_resource", getParamsHtml());
            case downloading:
                yield usernameLink + bundle.format("log_downloading", getResourceLink(bundle));
            case changing_office_resource:
                yield usernameLink + bundle.format("log_document_changing", getResourceLink(bundle));
            case adding_resource_metadata:
                yield usernameLink + bundle.format("log_add_resource_metadata", getParamsHtml()) + getResourceLink(bundle);

            // Folder actions
            case add_folder:
                yield usernameLink + bundle.format("log_add_folder", getParamsHtml());
            case deleting_folder:
                yield usernameLink + bundle.format("log_deleting_folder", getParamsHtml());
            case move_folder:
                yield usernameLink + bundle.format("log_move_folder", getParamsHtml());
            case opening_folder:
                yield usernameLink + bundle.format("log_open_folder", getParamsHtml());

            // Group actions
            case group_joining:
                yield usernameLink + bundle.format("log_group_joining", getGroupLink(bundle));
            case group_leaving:
                yield usernameLink + bundle.format("log_group_leaving", getGroupLink(bundle));
            case group_creating:
                yield usernameLink + bundle.format("log_group_creating", getGroupLink(bundle));
            case group_deleting:
                yield usernameLink + bundle.format("log_group_deleting", getParamsHtml());
            case group_changing_title:
                yield usernameLink + bundle.format("log_group_changing_title", getGroupLink(bundle));
            case group_changing_description:
                yield usernameLink + bundle.format("log_group_changing_description", getGroupLink(bundle));
            case group_changing_leader:
                yield usernameLink + bundle.format("log_group_changing_leader", getGroupLink(bundle));
            case group_deleting_link:
                yield usernameLink + bundle.format("log_group_deleting_link", getGroupLink(bundle));
            case forum_topic_added:
                yield usernameLink + bundle.format("log_forum_topic_added", getForumLink(bundle));
            case forum_post_added:
                yield usernameLink + bundle.format("log_forum_post_added", getForumLink(bundle));

            // General actions
            case login:
                yield usernameLink + bundle.getString("log_login");
            case logout:
                yield usernameLink + bundle.getString("log_logout");
            case register:
                yield usernameLink + bundle.getString("log_register");
            case changing_profile:
                yield usernameLink + bundle.getString("log_change_profile");
            case glossary_entry_edit:
                yield usernameLink + bundle.format("log_glossary_entry_edit", getResourceLink(bundle)); // TODO @kemkes: incorporate link to entry
            case glossary_entry_delete:
                yield usernameLink + bundle.format("log_glossary_entry_delete", getResourceLink(bundle)); // TODO @kemkes: incorporate details of entry
            case glossary_entry_add:
                yield usernameLink + bundle.format("log_glossary_entry_add", getResourceLink(bundle)); // TODO @kemkes: incorporate link to entry
            case glossary_term_edit:
                yield usernameLink + bundle.format("log_glossary_term_edit", getResourceLink(bundle)); // TODO @kemkes: incorporate link to entry
            case glossary_term_add:
                yield usernameLink + bundle.format("log_glossary_term_add", getResourceLink(bundle)); // TODO @kemkes: incorporate link to entry
            case glossary_term_delete:
                yield usernameLink + bundle.format("log_glossary_term_delete", getResourceLink(bundle)); // TODO @kemkes: incorporate link to entry

            default:
                if (getAction().getTargetId() == ActionTargetId.RESOURCE_ID) {
                    yield usernameLink + bundle.format("log_performed_action_on", getAction().name(), getResourceLink(bundle));
                } else {
                    yield usernameLink + bundle.format("log_performed_action", getAction().name()); // should never happen
                }
        };

        this.descriptions.put(locale, description);

        return description;
    }
}

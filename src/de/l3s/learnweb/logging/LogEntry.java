package de.l3s.learnweb.logging;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = -4239479233091966928L;
    //private static final Logger log = LogManager.getLogger(LogEntry.class);

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
    private Map<Locale, String> descriptions; // stores a description of this entry for different locales

    public LogEntry(int userId, Action action, LocalDateTime date, String params, int groupId, int targetId) {
        this.userId = userId;
        this.action = action;
        this.date = date;
        this.params = params;
        this.groupId = groupId;
        this.targetId = targetId;
    }

    public User getUser() {
        if (null == user) {
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

    private String getGroupLink(Locale locale) {
        if (getGroupId() == 0) {
            return "<a href=\"myhome/resources.jsf\" >" + LanguageBundle.getLocaleMessage(locale, "myPrivateResources") + "</a> ";
        }

        Group group = getGroup();

        if (null == group) {
            return "<b>Deleted group</b>";
        } else {
            return "<a href=\"group/overview.jsf?group_id=" + getGroupId() + "\" target=\"_top\">" + group.getTitle() + "</a> ";
        }
    }

    private String getUsernameLink(Locale locale) {
        if (getUser() == null || getUser().isDeleted()) {
            return "<b>Deleted user</b>";
        }
        return "<a href=\"user/detail.jsf?user_id=" + getUserId() + "\" target=\"_top\">" + getUser().getUsername() + "</a>";
    }

    private String getCommentText(int commentId, Locale locale) {
        Optional<Comment> comment = Learnweb.dao().getCommentDao().findById(commentId);
        return comment.map(value -> " " + LanguageBundle.getLocaleMessage(locale, "with") + " <b>"
            + StringHelper.shortnString(value.getText(), 100) + "</b>").orElse("");
    }

    private String getResourceLink(Locale locale) {
        if (getResource() != null) {
            return "<a href=\"resource.jsf?resource_id=" + getResource().getId() + "\" target=\"_top\"><b>" + StringHelper.shortnString(getResource().getTitle(), 40) + "</b></a> ";
        }
        return LanguageBundle.getLocaleMessage(locale, "log_a_resource");
    }

    private String getForumLink(Locale locale) {
        return "<a href=\"group/forum_topic.jsf?topic_id=" + targetId + "\" target=\"_top\"><b>" + getParams() + "</b></a> ";
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

        String usernameLink = getUsernameLink(locale) + " ";

        description = switch (getAction()) {
            case adding_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_adding_resource", getResourceLink(locale), getGroupLink(locale));
            case deleting_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_deleting_resource", "<b>" + getParams() + "</b>");
            case edit_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_edit_resource", getResourceLink(locale));
            case move_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_move_resource", getResourceLink(locale));
            case opening_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_opening_resource", getResourceLink(locale));
            case tagging_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_tagging_resource", getResourceLink(locale), getParams());
            case commenting_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_commenting_resource", getResourceLink(locale))
                    + getCommentText(NumberUtils.toInt(getParams()), locale);
            case deleting_comment:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_deleting_comment", getResourceLink(locale));
            case rating_resource, thumb_rating_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_thumb_rating_resource", getResourceLink(locale));
            case searching:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_searching_resource", getParams());
            case downloading:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_downloading", getResourceLink(locale));
            case changing_office_resource:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_document_changing", getResourceLink(locale));
            case adding_resource_metadata:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_add_resource_metadata", getParams()) + getResourceLink(locale);

                // Folder actions
            case add_folder:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_add_folder", getParams());
            case deleting_folder:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_deleting_folder", getParams());
            case move_folder:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_move_folder", getParams());
            case opening_folder:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_open_folder", getParams());

                // Group actions
            case group_joining:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_joining", getGroupLink(locale));
            case group_leaving:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_leaving", getGroupLink(locale));
            case group_creating:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_creating", getGroupLink(locale));
            case group_deleting:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_deleting", getParams());
            case group_changing_title:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_changing_title", getGroupLink(locale));
            case group_changing_description:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_changing_description", getGroupLink(locale));
            case group_changing_leader:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_changing_leader", getGroupLink(locale));
            case group_deleting_link:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_group_deleting_link", getGroupLink(locale));
            case forum_topic_added:
                yield usernameLink + "has added " + "<b>" + getForumLink(locale) + "</b>" + " post";
            case forum_post_added:
                yield usernameLink + "has replied to " + "<b>" + getForumLink(locale) + "</b>" + " topic";

                // General actions
            case login:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_login");
            case logout:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_logout");
            case register:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_register");
            case changing_profile:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_change_profile");
            case submission_view_resources:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_submission_view_resources");
            case submission_submitted:
                yield usernameLink + LanguageBundle.getLocaleMessage(locale, "log_submission_submit");
            case glossary_entry_edit:
                yield usernameLink + " has edited an entry of " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
            case glossary_entry_delete:
                yield usernameLink + "has deleted an entry from " + getResourceLink(locale); // TODO @kemkes: incorporate details of entry, translate
            case glossary_entry_add:
                yield usernameLink + "has added an entry to " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
            case glossary_term_edit:
                yield usernameLink + "has edited a term in " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
            case glossary_term_add:
                yield usernameLink + "has added a term to " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate
            case glossary_term_delete:
                yield usernameLink + "has removed a term from " + getResourceLink(locale); // TODO @kemkes: incorporate link to entry, translate

            default:
                if (getAction().getTargetId() == ActionTargetId.RESOURCE_ID) {
                    yield usernameLink + " has executed action <i>" + getAction().name() + "</i> on " + getResourceLink(locale);
                } else {
                    yield "Performed action <i>" + getAction().name() + "</i>"; // should never happen;
                }
        };
        // unused translations that might become useful again: log_opening_url_resource, log_group_removing_resource

        this.descriptions.put(locale, description);

        return description;
    }
}

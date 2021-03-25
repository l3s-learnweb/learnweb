package de.l3s.learnweb.logging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = -4239479233091966928L;
    private static final Logger log = LogManager.getLogger(LogEntry.class);

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
        switch (getAction()) {
            case adding_resource:
                return getGroupId() == 0; // added to "my private resources"
            default:
                return false;
        }
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
                Optional<Comment> comment = Learnweb.dao().getCommentDao().findById(NumberUtils.toInt(getParams()));
                if (comment.isPresent()) {
                    description += " " + LanguageBundle.getLocaleMessage(locale, "with") +
                        " <b>" + StringHelper.shortnString(comment.get().getText(), 100) + "</b>";
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
                description = usernameLink + "has added " + "<b>" + getForumLink(locale) + "</b>" + " post";
                break;
            case forum_post_added:
                description = usernameLink + "has replied to " + "<b>" + getForumLink(locale) + "</b>" + " topic";

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
                if (getAction().getTargetId() == ActionTargetId.RESOURCE_ID) {
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

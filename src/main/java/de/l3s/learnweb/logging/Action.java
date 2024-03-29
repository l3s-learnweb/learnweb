package de.l3s.learnweb.logging;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;

public enum Action implements Argument {
    // add new values add the BOTTOM !!!
    tagging_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = the tag
    rating_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = rate
    commenting_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = comment id
    opening_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    unused0(ActionTargetId.NONE, ActionCategory.OTHER),
    searching(ActionTargetId.NONE, ActionCategory.SEARCH), // param = search query
    group_joining(ActionTargetId.GROUP_ID, ActionCategory.GROUP),
    group_creating(ActionTargetId.GROUP_ID, ActionCategory.GROUP),
    group_leaving(ActionTargetId.GROUP_ID, ActionCategory.GROUP),
    login(ActionTargetId.NONE, ActionCategory.USER), // param = page the user logged in on
    logout(ActionTargetId.NONE, ActionCategory.USER),
    forum_topic_added(ActionTargetId.FORUM_TOPIC_ID, ActionCategory.FORUM), // param = topic title
    register(ActionTargetId.NONE, ActionCategory.USER),
    changing_profile(ActionTargetId.USER_ID, ActionCategory.USER), // target_id = user_id of the user whose profile was changed
    deleting_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = resource title;
    adding_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    forum_post_deleted(ActionTargetId.FORUM_POST_ID, ActionCategory.FORUM), // param = topic title;
    deleting_comment(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = comment_id
    survey_save(ActionTargetId.RESOURCE_ID, ActionCategory.SURVEY), // target_id = survey resource id
    edit_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    survey_submit(ActionTargetId.RESOURCE_ID, ActionCategory.SURVEY), // target_id = survey resource id
    thumb_rating_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    group_deleting(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = group name
    group_changing_description(ActionTargetId.GROUP_ID, ActionCategory.GROUP),
    group_changing_title(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = old title
    group_changing_leader(ActionTargetId.GROUP_ID, ActionCategory.GROUP), //
    group_changing_restriction(ActionTargetId.GROUP_ID, ActionCategory.GROUP), //
    unused9(ActionTargetId.NONE, ActionCategory.OTHER),
    unused10(ActionTargetId.NONE, ActionCategory.OTHER),
    opening_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER),
    unused7(ActionTargetId.NONE, ActionCategory.OTHER),
    deleting_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // param = folder name;
    downloading(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = file_id
    group_deleting_link(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = title of deleted link;
    group_resource_search(ActionTargetId.NONE, ActionCategory.SEARCH), // param = query
    add_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // param = folder name
    edit_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // param = folder name
    glossary_open(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = glossary resource id
    unused(ActionTargetId.NONE, ActionCategory.OTHER),
    glossary_entry_edit(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // param = glossary entry id
    glossary_entry_add(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // param = glossary entry id
    glossary_entry_delete(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // param = glossary entry id
    glossary_term_edit(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // param = glossary id
    glossary_term_add(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // param = glossary id
    glossary_term_delete(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // param = glossary_term_id
    resource_thumbnail_update(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    unused1(ActionTargetId.NONE, ActionCategory.OTHER), //target_id = resource_id
    unused8(ActionTargetId.NONE, ActionCategory.OTHER),
    unused2(ActionTargetId.NONE, ActionCategory.OTHER),
    adding_resource_metadata(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // was added by chloe . can be reused
    edit_resource_metadata(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // was added by chloe . can be reused
    unused3(ActionTargetId.NONE, ActionCategory.OTHER),
    unused4(ActionTargetId.NONE, ActionCategory.OTHER),
    unused11(ActionTargetId.NONE, ActionCategory.OTHER),
    changing_office_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    forum_post_added(ActionTargetId.FORUM_TOPIC_ID, ActionCategory.FORUM), // param = topic title
    moderator_login(ActionTargetId.USER_ID, ActionCategory.MODERATOR), // target_id = user_id of the moderator logs into a user account
    course_delete(ActionTargetId.COURSE_ID, ActionCategory.MODERATOR), // target_id = course_id
    course_anonymize(ActionTargetId.COURSE_ID, ActionCategory.MODERATOR), // target_id = course_id
    // when one user editing resource and another one want to edit the same resource, but locker is not allowing it
    lock_rejected_edit_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    // when first user who edits resource after inactive time returns to editing, but locker is now longer belongs to it
    lock_interrupted_returned_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    unused5(ActionTargetId.NONE, ActionCategory.OTHER),
    unused6(ActionTargetId.NONE, ActionCategory.OTHER),
    deleted_user_soft(ActionTargetId.USER_ID, ActionCategory.USER),
    deleted_user_hard(ActionTargetId.USER_ID, ActionCategory.USER),
    move_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // param = folder name;
    move_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE); // param = resource name;

    private static final ArrayList<Set<Action>> ACTIONS_BY_CATEGORY = new ArrayList<>(ActionCategory.values().length);

    public static final EnumSet<Action> LOGS_DEFAULT_FILTER = EnumSet.of(adding_resource, commenting_resource, edit_resource, deleting_resource,
        group_changing_description, group_changing_leader, group_changing_title, group_creating, group_deleting, group_joining, group_leaving,
        rating_resource, tagging_resource, thumb_rating_resource, changing_office_resource, forum_topic_added, forum_post_added, deleting_folder, add_folder);

    public static final EnumSet<Action> LOGS_RESOURCE_FILTER;

    static {
        // init one EnumSet per category
        for (int i = 0, len = ActionCategory.values().length; i < len; i++) {
            ACTIONS_BY_CATEGORY.add(EnumSet.noneOf(Action.class));
        }

        // add actions to category hashsets
        for (Action action : values()) {
            getActionsByCategory(action.getCategory()).add(action);
        }

        // make hashsets immutable
        for (int i = 0, len = ActionCategory.values().length; i < len; i++) {
            ACTIONS_BY_CATEGORY.set(i, Collections.unmodifiableSet(ACTIONS_BY_CATEGORY.get(i)));
        }

        EnumSet<Action> resourceActions = EnumSet.copyOf(getActionsByCategory(ActionCategory.RESOURCE));
        // remove actions we don't want to show
        resourceActions.remove(opening_resource);
        resourceActions.remove(glossary_open);
        resourceActions.remove(lock_interrupted_returned_resource);
        resourceActions.remove(lock_rejected_edit_resource);
        resourceActions.remove(downloading);
        LOGS_RESOURCE_FILTER = resourceActions;
    }

    private final ActionTargetId targetId;
    private final ActionCategory category;

    /**
     * @param targetId the meaning of the target_id column of log entries of this type
     * @param category only relevant for the grouping of log entries in the admin dashboard
     */
    Action(ActionTargetId targetId, ActionCategory category) {
        this.targetId = targetId;
        this.category = category;
    }

    public ActionTargetId getTargetId() {
        return targetId;
    }

    public ActionCategory getCategory() {
        return category;
    }

    @Override
    public void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException {
        statement.setInt(position, ordinal());
    }

    public static List<Integer> collectOrdinals(EnumSet<Action> actions) {
        return actions.stream().map(Enum::ordinal).toList();
    }

    public static Set<Action> getActionsByCategory(ActionCategory category) {
        return ACTIONS_BY_CATEGORY.get(category.ordinal());
    }
}

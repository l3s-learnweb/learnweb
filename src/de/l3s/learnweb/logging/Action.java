package de.l3s.learnweb.logging;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.l3s.learnweb.Learnweb;

public enum Action
{
    // add new values add the BOTTOM !!!
    tagging_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = the tag
    rating_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = rate
    commenting_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // param = comment id
    opening_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    submission_view_resources(ActionTargetId.OTHER, ActionCategory.OTHER), // target_id = submission_id; param = user_id of the submission
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
    group_deleting(ActionTargetId.GROUP_ID, ActionCategory.GROUP),
    group_changing_description(ActionTargetId.GROUP_ID, ActionCategory.GROUP),
    group_changing_title(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = old title
    group_changing_leader(ActionTargetId.GROUP_ID, ActionCategory.GROUP), //
    group_changing_restriction(ActionTargetId.GROUP_ID, ActionCategory.GROUP), //
    group_adding_link(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = title
    group_adding_document(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = title
    opening_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER),
    unused7(ActionTargetId.NONE, ActionCategory.OTHER), // TODO: replace by deleting_resource
    deleting_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // target_id = folder_id, param = folder name;
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
    resource_thumbnail_update(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource_id
    submission_submitted(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), //target_id = resource_id
    unused8(ActionTargetId.NONE, ActionCategory.OTHER),
    unused2(ActionTargetId.NONE, ActionCategory.OTHER),
    adding_resource_metadata(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // was added by chloe . can be reused
    edit_resource_metadata(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // was added by chloe . can be reused
    unused3(ActionTargetId.NONE, ActionCategory.OTHER),
    unused4(ActionTargetId.NONE, ActionCategory.OTHER),
    group_changing_hypothesis_link(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = old value
    changing_office_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    forum_post_added(ActionTargetId.FORUM_TOPIC_ID, ActionCategory.FORUM), // param = topic title
    moderator_login(ActionTargetId.USER_ID, ActionCategory.MODERATOR), // target_id = user_id of the moderator logs into a user account
    course_delete(ActionTargetId.COURSE_ID, ActionCategory.MODERATOR), // target_id = course_id
    course_anonymize(ActionTargetId.COURSE_ID, ActionCategory.MODERATOR), // target_id = course_id
    lock_rejected_edit_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // when one user editing resource and another one want to edit the same resource, but locker is not allowing it
    lock_interrupted_returned_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // when first user who edits resource after inactive time returns to editing, but locker is now longer belongs to it
    unused5(ActionTargetId.NONE, ActionCategory.OTHER),
    unused6(ActionTargetId.NONE, ActionCategory.OTHER),
    deleted_user_soft(ActionTargetId.USER_ID, ActionCategory.USER),
    deleted_user_hard(ActionTargetId.USER_ID, ActionCategory.USER),
    move_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // target_id = folder_id, param = folder name;
    move_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource_id, param = resource name;
    ;

    private final ActionTargetId targetId;
    private final ActionCategory category;

    /**
     * @param targetId the meaning of the target_id column of log entries of this type
     * @param category only relevant for the grouping of log entries in the admin dashboard
     */
    Action(ActionTargetId targetId, ActionCategory category)
    {
        this.targetId = targetId;
        this.category = category;
    }

    public ActionTargetId getTargetId()
    {
        return targetId;
    }

    public ActionCategory getCategory()
    {
        return category;
    }

    private static final ArrayList<Set<Action>> ACTIONS_BY_CATEGORY = new ArrayList<>(ActionCategory.values().length);

    static
    {
        // init one hashset per category
        for(int i = 0; i < ActionCategory.values().length; i++)
            ACTIONS_BY_CATEGORY.add(new HashSet<>());

        // add actions to category hashsets
        for(Action action : Action.values())
            getActionsByCategory(action.getCategory()).add(action);

        // make hashsets immutable
        for(int i = 0; i < ActionCategory.values().length; i++)
            ACTIONS_BY_CATEGORY.set(i, Collections.unmodifiableSet(ACTIONS_BY_CATEGORY.get(i)));
    }

    public static Set<Action> getActionsByCategory(ActionCategory category)
    {
        return ACTIONS_BY_CATEGORY.get(category.ordinal());
    }

    /**
     * Updates the lw_user_log_action table
     *
     * @param args
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException
    {
        Learnweb learnweb = Learnweb.createInstance();
        learnweb.getConnection().createStatement().execute("TRUNCATE TABLE `lw_user_log_action`");

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_user_log_action` (`action`, `name`, `target`, `category`) VALUES (?,?,?,?)"))
        {
            for(Action action : Action.values())
            {
                insert.setInt(1, action.ordinal());
                insert.setString(2, action.name());
                insert.setString(3, action.getTargetId().name());
                insert.setString(4, action.getCategory().name());
                insert.executeUpdate();
            }
        }

        learnweb.onDestroy();
    }
}

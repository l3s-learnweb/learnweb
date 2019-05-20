package de.l3s.learnweb.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// import de.l3s.learnweb.logging.ActionTargetId.*;

public enum Action
{
    // add new values add the BOTTOM !!!
    tagging_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id, param = the tag
    rating_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id, param = rate
    commenting_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id, param = comment id
    opening_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id
    submission_view_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = submission_id; param = user_id of the submission
    searching(ActionTargetId.NONE, ActionCategory.SEARCH), // param = search query
    group_joining(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id
    group_creating(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id
    group_leaving(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id
    login(ActionTargetId.NONE, ActionCategory.USER), // param = page the user logged in on
    logout(ActionTargetId.NONE, ActionCategory.USER),
    forum_topic_added(ActionTargetId.FORUM_TOPIC_ID, ActionCategory.FORUM), // target_id = topic_id, param = topic title
    register(ActionTargetId.NONE, ActionCategory.USER),
    changing_profile(ActionTargetId.USER_ID, ActionCategory.USER), // target_id = user_id of the user whose profile was changed
    deleting_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id, param = resource title;
    adding_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id
    forum_post_deleted(ActionTargetId.FORUM_POST_ID, ActionCategory.FORUM), //target_id = forum_post_id, param = topic title;
    deleting_comment(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id; param = comment_id
    survey_save(ActionTargetId.RESOURCE_ID, ActionCategory.SURVEY), // target_id = survey resource id
    edit_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id
    survey_submit(ActionTargetId.RESOURCE_ID, ActionCategory.SURVEY), // target_id = survey resource id
    thumb_rating_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource id
    group_deleting(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id
    group_changing_description(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id
    group_changing_title(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id; param = old title
    group_changing_leader(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id
    group_changing_restriction(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id
    group_adding_link(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id; param = title
    group_adding_document(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // target_id = group_id; param = title
    opening_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER),
    group_removing_resource(ActionTargetId.RESOURCE_ID, ActionCategory.GROUP), // target_id = resource id
    deleting_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // target_id = folder_id, param = folder name;
    downloading(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource_id, param = file_id
    group_deleting_link(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = title of deleted link;
    group_resource_search(ActionTargetId.NONE, ActionCategory.SEARCH), // param = query
    add_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // param = folder name; target_id = folder_id
    edit_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // param = folder name; target_id = folder_id
    glossary_open(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary id
    glossary_create(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary id
    glossary_entry_edit(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary id
    glossary_entry_add(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary id
    glossary_entry_delete(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary id
    glossary_term_edit(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary id
    glossary_term_add(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary id
    glossary_term_delete(ActionTargetId.RESOURCE_ID, ActionCategory.GLOSSARY), // target_id = resource id, param = glossary_term_id
    resource_thumbnail_update(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource_id
    submission_submitted(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), //target_id = resource_id
    extended_metadata_open_dialog(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), //target_id = resource_id
    //log entries for extended metadata (yell group only)
    adding_yourown_metadata(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), //target_id = resource id, param = type of metadata added
    adding_resource_metadata(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), //target_id = resource id, param = type of metadata added (options: author, language, media source, media type)
    edit_resource_metadata(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), //target_id = resource id, param = type of metadata edited (options: author, language, media source, media type)
    group_metadata_search(ActionTargetId.NONE, ActionCategory.SEARCH), // param = filter:value only if it is not null
    group_category_search(ActionTargetId.NONE, ActionCategory.SEARCH), // param = clicked category
    group_changing_hypothesis_link(ActionTargetId.GROUP_ID, ActionCategory.GROUP), // param = old value
    changing_office_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE),
    forum_post_added(ActionTargetId.FORUM_TOPIC_ID, ActionCategory.FORUM), //target_id = topic id, param = topic title
    moderator_login(ActionTargetId.USER_ID, ActionCategory.MODERATOR), // target_id = user_id of the moderator logs into a user account
    course_delete(ActionTargetId.COURSE_ID, ActionCategory.MODERATOR), // target_id = course_id
    course_anonymize(ActionTargetId.COURSE_ID, ActionCategory.MODERATOR), // target_id = course_id

    lock_rejected_edit_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // when one user editing resource and another one want to edit the same resource, but locker is not allowed it
    lock_interrupted_returned_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // when first user which edit resource after inactive time returns to editing, but locker is no longer belongs to it
    lock_rejected_edit_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER),
    lock_interrupted_returned_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER),
    deleted_user_soft(ActionTargetId.USER_ID, ActionCategory.USER),
    deleted_user_hard(ActionTargetId.USER_ID, ActionCategory.USER),
    move_folder(ActionTargetId.FOLDER_ID, ActionCategory.FOLDER), // target_id = folder_id, param = folder name;
    move_resource(ActionTargetId.RESOURCE_ID, ActionCategory.RESOURCE), // target_id = resource_id, param = resource name;
    ;
    private final ActionTargetId targetId;
    private final ActionCategory category;

    /**
     *
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

    public static void main(String[] arg)
    {
       
        for(ActionCategory category : ActionCategory.values())
        {
            System.out.print("\n" + category + ": ");

            List<Action> categoryActions = new ArrayList<>(Action.getActionsByCategory(category));
            categoryActions.sort(Comparator.comparing(Enum::name));
            for(Action action : categoryActions)
            {
                System.out.print("" + action.name() + ", ");
            }
        }

        /*
      Action[] actions = { Action.unused };

        System.out.print("\nCASE action");

        for(Action action : actions)
        {
            System.out.print(" WHEN " + action.ordinal() + " THEN '" + action.name() + "'");
        }
        System.out.println(" END CASE");
        */
    }

}

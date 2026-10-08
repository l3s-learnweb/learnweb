package de.l3s.learnweb.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.l3s.util.HasId;

/**
 * The context of a logged action (resource, folder, group, ...) is stored in the typed columns of lw_user_log, see {@link ActivityEvent}.
 */
public enum Action implements HasId {
    // The id is stored in lw_user_log.action and must never change; the enum order is free. Never reuse one of RETIRED_IDS.
    login(9, ActionCategory.USER), // param = page the user logged in on
    logout(10, ActionCategory.USER),
    register(12, ActionCategory.USER),
    changing_profile(13, ActionCategory.USER), // target user = the user whose profile was changed
    deleted_user_soft(63, ActionCategory.USER),
    deleted_user_hard(64, ActionCategory.USER),
    moderator_login(56, ActionCategory.MODERATOR), // target user = the moderator who logs into a user account
    course_delete(57, ActionCategory.MODERATOR),
    course_anonymize(58, ActionCategory.MODERATOR),

    // Search actions
    searching(5, ActionCategory.SEARCH), // param = search query

    // Group actions
    group_joining(6, ActionCategory.GROUP),
    group_creating(7, ActionCategory.GROUP),
    group_leaving(8, ActionCategory.GROUP),
    group_deleting(22, ActionCategory.GROUP), // param = group name
    group_changing_description(23, ActionCategory.GROUP),
    group_changing_title(24, ActionCategory.GROUP), // param = old title
    group_changing_leader(25, ActionCategory.GROUP),
    group_changing_restriction(26, ActionCategory.GROUP),
    group_deleting_link(33, ActionCategory.GROUP), // param = title of deleted link;
    group_resource_search(34, ActionCategory.SEARCH), // param = query

    // Folder actions
    opening_folder(29, ActionCategory.FOLDER), // param = folder name
    deleting_folder(31, ActionCategory.FOLDER), // param = folder name;
    add_folder(35, ActionCategory.FOLDER), // param = folder name
    edit_folder(36, ActionCategory.FOLDER), // param = folder name
    move_folder(65, ActionCategory.FOLDER), // param = folder name;

    // Resource actions
    tagging_resource(0, ActionCategory.RESOURCE), // param = the tag
    rating_resource(1, ActionCategory.RESOURCE), // param = rate
    commenting_resource(2, ActionCategory.RESOURCE), // param = comment id
    opening_resource(3, ActionCategory.RESOURCE),
    deleting_resource(14, ActionCategory.RESOURCE), // param = resource title;
    adding_resource(15, ActionCategory.RESOURCE),
    deleting_comment(17, ActionCategory.RESOURCE), // param = comment_id
    edit_resource(19, ActionCategory.RESOURCE),
    thumb_rating_resource(21, ActionCategory.RESOURCE),
    downloading(32, ActionCategory.RESOURCE), // param = file_id
    resource_thumbnail_update(45, ActionCategory.RESOURCE),
    adding_resource_metadata(49, ActionCategory.RESOURCE), // was added by chloe . can be reused
    edit_resource_metadata(50, ActionCategory.RESOURCE), // was added by chloe . can be reused
    changing_office_resource(54, ActionCategory.RESOURCE),
    // when one user editing resource and another one want to edit the same resource, but locker is not allowing it
    lock_rejected_edit_resource(59, ActionCategory.RESOURCE),
    // when first user who edits resource after inactive time returns to editing, but locker is now longer belongs to it
    lock_interrupted_returned_resource(60, ActionCategory.RESOURCE),
    move_resource(66, ActionCategory.RESOURCE), // param = resource name;

    // Forum actions
    forum_topic_added(11, ActionCategory.FORUM), // param = topic title
    forum_post_deleted(16, ActionCategory.FORUM), // param = topic title;
    forum_post_added(55, ActionCategory.FORUM), // param = topic title

    // Glossary actions
    glossary_open(37, ActionCategory.GLOSSARY),
    glossary_entry_edit(39, ActionCategory.GLOSSARY), // param = glossary entry id
    glossary_entry_add(40, ActionCategory.GLOSSARY), // param = glossary entry id
    glossary_entry_delete(41, ActionCategory.GLOSSARY), // param = glossary entry id
    glossary_term_edit(42, ActionCategory.GLOSSARY), // param = glossary id
    glossary_term_add(43, ActionCategory.GLOSSARY), // param = glossary id
    glossary_term_delete(44, ActionCategory.GLOSSARY), // param = glossary_term_id

    // Survey actions
    survey_save(18, ActionCategory.SURVEY),
    survey_submit(20, ActionCategory.SURVEY);

    /**
     * Ids of removed actions. Old log entries may still contain them, they must not be assigned to new actions.
     */
    public static final Set<Integer> RETIRED_IDS = Set.of(4, 27, 28, 30, 38, 46, 47, 48, 51, 52, 53, 61, 62);

    private static final Map<Integer, Action> BY_ID = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(Action::getId, Function.identity()));

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

    private final int id;
    private final ActionCategory category;

    /**
     * @param id the value stored in lw_user_log.action, must never change
     * @param category only relevant for the grouping of log entries in the admin dashboard
     */
    Action(int id, ActionCategory category) {
        this.id = id;
        this.category = category;
    }

    @Override
    public int getId() {
        return id;
    }

    public ActionCategory getCategory() {
        return category;
    }

    /**
     * @return empty for {@link #RETIRED_IDS}, which may still exist in old log entries
     */
    public static Optional<Action> findById(int id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Action findByIdOrElseThrow(int id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown action id: " + id));
    }

    public static Set<Action> getActionsByCategory(ActionCategory category) {
        return ACTIONS_BY_CATEGORY.get(category.ordinal());
    }
}

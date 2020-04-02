package de.l3s.learnweb.logging;

/**
 * Every LogEntry has a target_id. Its meaning depends on the LogEntry.Action attribute. It can represent one of the listed ids
 *
 * @author Philipp
 *
 */
public enum ActionTargetId
{
    NONE,
    RESOURCE_ID,
    GROUP_ID,
    USER_ID,
    FORUM_TOPIC_ID,
    FORUM_POST_ID,
    COURSE_ID,
    FOLDER_ID,
    OTHER
}

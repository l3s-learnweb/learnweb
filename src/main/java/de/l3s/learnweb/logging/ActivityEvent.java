package de.l3s.learnweb.logging;

import de.l3s.learnweb.user.User;

/**
 * A user activity, fired by {@link EventDispatcher} and stored in the activity log by {@link LoggingEventListener}.
 * Subclasses fill in every id of the context the activity happened in (e.g. a resource, its folder and its group),
 * each id is stored in its own column of lw_user_log. An id of 0 means "not applicable".
 */
public class ActivityEvent {
    private final Action action;
    private String params;

    // context ids
    private int groupId;
    private int resourceId;
    private int folderId;
    private int topicId;
    private int postId;
    private int courseId;
    private int targetUserId;
    private int searchId;

    // context, set by EventDispatcher
    private User performer;
    private String sessionId;

    public ActivityEvent(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public String getParams() {
        return params;
    }

    public ActivityEvent setParams(String params) {
        this.params = params;
        return this;
    }

    public ActivityEvent setParams(int params) {
        this.params = String.valueOf(params);
        return this;
    }

    public int getGroupId() {
        return groupId;
    }

    public ActivityEvent setGroupId(int groupId) {
        this.groupId = groupId;
        return this;
    }

    public int getResourceId() {
        return resourceId;
    }

    public ActivityEvent setResourceId(int resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public int getFolderId() {
        return folderId;
    }

    public ActivityEvent setFolderId(int folderId) {
        this.folderId = folderId;
        return this;
    }

    public int getTopicId() {
        return topicId;
    }

    public ActivityEvent setTopicId(int topicId) {
        this.topicId = topicId;
        return this;
    }

    public int getPostId() {
        return postId;
    }

    public ActivityEvent setPostId(int postId) {
        this.postId = postId;
        return this;
    }

    public int getCourseId() {
        return courseId;
    }

    public ActivityEvent setCourseId(int courseId) {
        this.courseId = courseId;
        return this;
    }

    public int getTargetUserId() {
        return targetUserId;
    }

    public ActivityEvent setTargetUserId(int targetUserId) {
        this.targetUserId = targetUserId;
        return this;
    }

    public int getSearchId() {
        return searchId;
    }

    public ActivityEvent setSearchId(int searchId) {
        this.searchId = searchId;
        return this;
    }

    /**
     * @return the user who performed the event, null if the user isn't logged in
     */
    public User getPerformer() {
        return performer;
    }

    public String getSessionId() {
        return sessionId;
    }

    void setContext(User performer, String sessionId) {
        this.performer = performer;
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "[event=" + action + ", groupId=" + groupId + ", resourceId=" + resourceId + ", folderId=" + folderId
            + ", topicId=" + topicId + ", postId=" + postId + ", courseId=" + courseId + ", targetUserId=" + targetUserId
            + ", searchId=" + searchId + ", params=" + params + ", userId=" + (performer != null ? performer.getId() : null) + "]";
    }
}

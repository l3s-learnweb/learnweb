package de.l3s.learnweb.logging;

import de.l3s.learnweb.user.User;

/**
 * A user activity, fired by {@link EventDispatcher} and observed asynchronously, e.g. by {@link LoggingEventListener}.
 * Subclasses take the subject of the activity and use its id as the target id, see {@link Action#getTargetId()}.
 */
public class ActivityEvent {
    private final Action action;
    private final int groupId;
    private int targetId;
    private String params;

    // context, set by EventDispatcher
    private User performer;
    private String sessionId;

    public ActivityEvent(Action action) {
        this(action, 0, 0);
    }

    protected ActivityEvent(Action action, int groupId, int targetId) {
        this(action, groupId, targetId, null);
    }

    protected ActivityEvent(Action action, int groupId, int targetId, String params) {
        this.action = action;
        this.groupId = groupId;
        this.targetId = targetId;
        this.params = params;
    }

    public Action getAction() {
        return action;
    }

    public int getTargetId() {
        return targetId;
    }

    public ActivityEvent setTargetId(int targetId) {
        this.targetId = targetId;
        return this;
    }

    public int getGroupId() {
        return groupId;
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
        return "[event=" + action + ", groupId=" + groupId + ", targetId=" + targetId + ", params=" + params
            + ", userId=" + (performer != null ? performer.getId() : null) + "]";
    }
}

package de.l3s.learnweb.logging;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import de.l3s.learnweb.user.User;

public class LearnwebEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 8491339662817986506L;

    private final Action action;
    private User targetUser;
    private Integer targetId;
    private String params;
    private final Instant created;

    public LearnwebEvent(Action action) {
        this(action, null);
    }

    public LearnwebEvent(Action action, String params) {
        this.action = action;
        this.params = params;
        this.created = Instant.now();
    }

    public Action getAction() {
        return action;
    }

    public User getTargetUser() {
        return targetUser;
    }

    public LearnwebEvent setTargetUser(User targetUser) {
        this.targetUser = targetUser;
        return this;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public LearnwebEvent setTargetId(Integer targetId) {
        this.targetId = targetId;
        return this;
    }

    public String getParams() {
        return params;
    }

    public LearnwebEvent setParams(String params) {
        this.params = params;
        return this;
    }

    public LearnwebEvent setParams(int params) {
        this.params = String.valueOf(params);
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return "[event=" + action + ", targetUser=" + targetUser + ", targetId=" + targetId + ", params=" + params + "]";
    }
}

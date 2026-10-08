package de.l3s.learnweb.logging;

import de.l3s.learnweb.user.User;

public class UserEvent extends ActivityEvent {
    public UserEvent(Action action, User user) {
        super(action);
        setTargetUserId(user.getId());
    }
}

package de.l3s.learnweb.logging;

import de.l3s.learnweb.user.User;

@FunctionalInterface
public interface LearnwebEventListener {
    void onEvent(LearnwebEvent event, User user, String sessionId);

    default boolean isInterestedIn(LearnwebEvent event) {
        return true;
    }
}

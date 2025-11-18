package de.l3s.learnweb.logging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;

/**
 * Dispatches events asynchronously, enriched with the current user and session context.
 * Can be injected into session and view scoped beans (CDI serializes only a reference to the client proxy).
 */
@ApplicationScoped
public class EventDispatcher {
    private static final Logger log = LogManager.getLogger(EventDispatcher.class);

    @Inject
    private UserBean userBean;

    @Inject
    private Event<ActivityEvent> events;

    /**
     * Fires the event on behalf of the currently logged-in user.
     */
    public void fire(ActivityEvent event) {
        fire(event, userBean.getUser());
    }

    /**
     * Fires the event on behalf of the given user, e.g. when the user is not logged in yet.
     */
    public void fire(ActivityEvent event, User performer) {
        event.setContext(performer, userBean.getSessionId());
        log.debug("Event fired: {}", event);
        events.fireAsync(event);
    }
}

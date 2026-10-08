package de.l3s.learnweb.logging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.user.User;

/**
 * Stores the activity events in the activity log (lw_user_log).
 */
@ApplicationScoped
public class LoggingEventListener {
    private static final Logger log = LogManager.getLogger(LoggingEventListener.class);

    @Inject
    private LogDao logDao;

    /**
     * Logs only the events of logged-in users, whose organisation allows the activity log.
     *
     * @param event the event to be logged, including the user and session context
     */
    public void onEvent(@ObservesAsync ActivityEvent event) {
        User performer = event.getPerformer();
        if (performer == null || !performer.getOrganisation().isLoggingEnabled()) {
            return;
        }

        try {
            logDao.insert(event.getPerformer(), event.getAction(), event.getGroupId(), event.getTargetId(), event.getParams(), event.getSessionId());
        } catch (Exception e) {
            log.error("Error logging event to database: {}", event, e);
        }
    }
}

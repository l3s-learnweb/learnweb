package de.l3s.learnweb.logging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.searchhistory.SearchEvent;

/**
 * Listener for logging events to the database.
 * Observes events asynchronously via CDI events.
 */
@ApplicationScoped
public class LoggingEventListener {
    private static final Logger log = LogManager.getLogger(LoggingEventListener.class);

    @Inject
    private LogDao logDao;

    /**
     * Observes events asynchronously and logs them to the database.
     * Search events other than the query itself are tracked by SearchHistoryListener.
     *
     * @param event the event to be logged
     */
    public void onEvent(@ObservesAsync ActivityEvent event) {
        if (event.getPerformer() == null) {
            return; // TODO: anonymous logging
        }
        if (event instanceof SearchEvent && event.getAction() != Action.searching) {
            return;
        }

        try {
            logDao.insert(event.getPerformer(), event.getAction(), event.getGroupId(), event.getTargetId(), event.getParams(), event.getSessionId());
        } catch (Exception e) {
            log.error("Error logging event to database: {}", event, e);
        }
    }
}

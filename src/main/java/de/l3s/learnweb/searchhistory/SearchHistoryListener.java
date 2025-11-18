package de.l3s.learnweb.searchhistory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Listener for search events.
 * Observes search events asynchronously via CDI events.
 */
@ApplicationScoped
public class SearchHistoryListener {
    private static final Logger log = LogManager.getLogger(SearchHistoryListener.class);

    /**
     * Observes search events asynchronously and processes them.
     *
     * @param event the search event, including the user and session context
     */
    public void onEvent(@ObservesAsync SearchEvent event) {
        // TODO: Implement search history tracking
        // replace search.logQuery, search.logResourceClicked, search.logResourceSaved
        log.debug("Search event received: {}", event);
    }
}

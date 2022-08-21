package de.l3s.learnweb.web;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cleans up requests from RequestManager that are older than specified number of days.
 *
 * @author Kate
 */
public class RequestsTaskHandler implements Runnable {
    private static final Logger log = LogManager.getLogger(RequestsTaskHandler.class);

    @Inject
    private RequestManager requestManager;

    @Override
    public void run() {
        try {
            requestManager.cleanOldRequests();
            requestManager.flushRequests();
        } catch (Throwable e) {
            log.error("error", e);
        }
    }

}

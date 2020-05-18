package de.l3s.learnweb.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Cleans up requests from RequestManager that are older than specified number of days.
 *
 * @author Kate
 */
public class RequestsTaskHandler implements Runnable {
    private static final Logger log = LogManager.getLogger(RequestsTaskHandler.class);

    @Override
    public void run() {
        try {
            RequestManager requestManager = Learnweb.getInstance().getRequestManager();
            requestManager.cleanOldRequests();
            requestManager.recordRequestsToDB();
        } catch (Throwable e) {
            log.error("error", e);
        }
    }

}

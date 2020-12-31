package de.l3s.learnweb.web;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cleans up expired bans (>3 days old) from the bans database and forces ProtectionManager and RequestManager to reload their ban lists.
 *
 * @author Kate
 */
public class ExpiredBansCleaner implements Runnable {
    private static final Logger log = LogManager.getLogger(ExpiredBansCleaner.class);

    @Inject
    private RequestManager requestManager;

    @Override
    public void run() {
        try {
            requestManager.clearOutdatedBans();
        } catch (Throwable e) {
            log.error("Unable to clear bans", e);
        }
    }
}

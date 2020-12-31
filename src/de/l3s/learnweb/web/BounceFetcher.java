package de.l3s.learnweb.web;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Every scheduled amount of time gets all emails from the inbox that were sent after last scan.
 *
 * @author Kate
 */
public class BounceFetcher implements Runnable {
    private static final Logger log = LogManager.getLogger(BounceFetcher.class);

    @Inject
    private BounceManager bounceManager;

    @Override
    public void run() {
        try {
            bounceManager.parseInbox();
        } catch (Throwable e) {
            log.error("err", e);
        }
    }

}

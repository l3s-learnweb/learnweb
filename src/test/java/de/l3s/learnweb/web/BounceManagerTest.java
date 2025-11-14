package de.l3s.learnweb.web;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Inject;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.l3s.learnweb.app.DaoProvider;

@EnableAutoWeld
@AddPackages(DaoProvider.class)
class BounceManagerTest {
    private static final Logger log = LogManager.getLogger(BounceManagerTest.class);

    @Inject
    BounceManager bounceManager;

    /**
     * Debug/analysis function that checks the contents of bounce folder.
     */
    @Test
    @Disabled("Call to real mail server")
    void testConnection() {
        assertTrue(bounceManager.checkConnection());
    }

    @Test
    @Disabled("Debug/analysis function that checks contents of bounce folder")
    void checkBounceFolder() throws MessagingException {
        Store store = bounceManager.getStore();
        store.connect();

        Folder bounceFolder = store.getFolder("INBOX").getFolder("BOUNCES");
        if (bounceFolder.exists()) {
            bounceFolder.open(Folder.READ_ONLY);
            Message[] bounces = bounceFolder.getMessages();

            if (bounces.length != 0) {
                log.debug("Bounced emails folder contains {} messages. Oldest and newest messages printed below:", bounceFolder.getMessageCount());
                log.debug("{} {}", bounces[0].getSubject(), bounces[0].getReceivedDate());
                log.debug("{} {}", bounces[bounces.length - 1].getSubject(), bounces[bounces.length - 1].getReceivedDate());
            } else {
                log.debug("Bounced emails folder is empty.");
            }

            bounceFolder.close(false);
        } else {
            log.debug("Folder doesn't exist.");
        }

        store.close();
    }
}

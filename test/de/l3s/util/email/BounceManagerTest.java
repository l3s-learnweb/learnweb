package de.l3s.util.email;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.l3s.learnweb.Learnweb;

@Disabled
class BounceManagerTest {
    private static final Logger log = LogManager.getLogger(BounceManagerTest.class);
    private final Learnweb learnweb = Learnweb.createInstance();

    BounceManagerTest() throws SQLException, ClassNotFoundException {}

    /**
     * Debug/analysis function that checks contents of bounce folder.
     */
    @Test
    void testConnection() throws MessagingException {
        BounceManager bounceManager = new BounceManager(learnweb);
        Store store = bounceManager.getStore();
        store.connect();

        Folder inboxFolder = store.getFolder("INBOX");
        assertTrue(inboxFolder.exists());
        store.close();
    }

    /**
     * Debug/analysis function that checks contents of bounce folder.
     */
    @Test
    void checkBounceFolder() throws MessagingException {
        BounceManager bounceManager = new BounceManager(learnweb);
        Store store = bounceManager.getStore();
        store.connect();

        Folder bounceFolder = store.getFolder("INBOX").getFolder("BOUNCES");
        if (!bounceFolder.exists()) {
            log.debug("Folder doesn't exist.");
        } else {
            bounceFolder.open(Folder.READ_ONLY);
            Message[] bounces = bounceFolder.getMessages();

            if (bounces.length > 0) {
                log.debug("Bounced emails folder contains {} messages. Oldest and newest messages printed below:", bounceFolder.getMessageCount());
                log.debug("{} {}", bounces[0].getSubject(), bounces[0].getReceivedDate());
                log.debug("{} {}", bounces[bounces.length - 1].getSubject(), bounces[bounces.length - 1].getReceivedDate());
            } else {
                log.debug("Bounced emails folder is empty.");
            }

            bounceFolder.close(false);
        }

        store.close();
    }
}

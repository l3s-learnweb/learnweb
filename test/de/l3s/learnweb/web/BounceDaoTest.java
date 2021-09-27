package de.l3s.learnweb.web;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class BounceDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final BounceDao bounceDao = learnwebExt.attach(BounceDao.class);

    @Test
    void findLastBounceDate() {
        Optional<Instant> local = bounceDao.findLastBounceDate();
        assertTrue(local.isPresent());
        assertEquals(Instant.ofEpochSecond(1607450879), local.get());
    }

    @Test
    void findByEmail() {
        Optional<BounceDao.Bounce> bounce = bounceDao.findByEmail("hans15@example.net");
        assertTrue(bounce.isPresent());
        assertEquals("5.1.10", bounce.get().errorCode());
    }

    @Test
    void save() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        bounceDao.save("test12@gmail.com", now, "msg", "no description");
        Optional<Instant> local = bounceDao.findLastBounceDate();
        assertTrue(local.isPresent());
        assertEquals(now, local.get());
        Optional<BounceDao.Bounce> retrieved = bounceDao.findByEmail("test12@gmail.com");
        assertTrue(retrieved.isPresent());
        assertEquals("no description", retrieved.get().description());
    }

    @Test
    void findAll() {
        List<BounceDao.Bounce> bounceEmails = bounceDao.findAll();
        assertFalse(bounceEmails.isEmpty());
    }

    @Disabled
    @Test
    void validateTest() {
        String email = "hans15@example.net";
        bounceDao.findByEmail(email).ifPresent(bounce -> {
            String message = "In the past emails to " + email + " could not be delivered. On " + bounce.received()
                + " we received the following error: " + bounce.description();
            assertEquals("In the past emails to hans15@example.net could not be delivered."
                + " On 2020-06-15T18:25:12Z we received the following error: Permanent Failure: Unspecified mailing error", message);
        });
    }
}

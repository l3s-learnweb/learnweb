package de.l3s.learnweb.web;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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
        assertEquals(Instant.ofEpochSecond(1607447279), local.get());
    }

    @Test
    void save() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        bounceDao.save("", now, "msg", "no description");

        Optional<Instant> local = bounceDao.findLastBounceDate();
        assertTrue(local.isPresent());
        assertEquals(now, local.get());
    }
}

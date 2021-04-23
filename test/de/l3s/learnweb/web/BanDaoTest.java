package de.l3s.learnweb.web;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;
import de.l3s.util.SqlHelper;

class BanDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final BanDao banDao = learnwebExt.attach(BanDao.class);

    @Test
    void findAll() {
        List<Ban> retrieved = banDao.findAll();
        assertFalse(retrieved.isEmpty());
        assertTrue(retrieved.size() >= 10);
    }

    @Test
    void delete() {
        List<Ban> oldList = banDao.findAll();
        banDao.delete("222.184.105.9");
        List<Ban> retrieved = banDao.findAll();
        assertTrue(retrieved.size() < 10);
        assertFalse(retrieved.contains(oldList.get(2)));
    }

    @Test
    void findByAddr() {
        Optional<Ban> retrieved = banDao.findByAddr("111.114.109.179");
        assertFalse(retrieved.isEmpty());
        assertEquals("111.114.109.179", retrieved.get().getAddr());
    }

    @Test
    void deleteOutdated() {
        List<Ban> oldList = banDao.findAll();
        banDao.deleteOutdated();
        List<Ban> retrieved = banDao.findAll();
        assertNotEquals(oldList, retrieved);
    }

    @Test
    void save() {
        Ban ban = new Ban("127.0.0.1");
        ban.setAttempts(3);
        ban.setReason("SQL Injection");
        ban.setCreatedAt(LocalDateTime.of(2021, Month.FEBRUARY, 28, 18, 0, 0));
        ban.setExpires(ban.getCreatedAt().plus(1, ChronoUnit.WEEKS));
        banDao.save(ban);

        Optional<Ban> retrieved = banDao.findByAddr("127.0.0.1");
        assertTrue(retrieved.isPresent());
        assertEquals(ban.getAddr(), retrieved.get().getAddr());
        assertEquals(ban.getAllowedAttempts(), retrieved.get().getAllowedAttempts());
        assertEquals(ban.getAttempts(), retrieved.get().getAttempts());
        assertEquals(ban.getCreatedAt(), retrieved.get().getCreatedAt());
        assertEquals(ban.getExpires(), retrieved.get().getExpires());
        assertEquals(ban.getReason(), retrieved.get().getReason());

        LocalDateTime newExpires = SqlHelper.now().plusDays(10);
        ban.setExpires(newExpires);
        banDao.save(ban);

        Optional<Ban> updated = banDao.findByAddr("127.0.0.1");
        assertTrue(updated.isPresent());
        assertEquals(newExpires, updated.get().getExpires());
    }
}

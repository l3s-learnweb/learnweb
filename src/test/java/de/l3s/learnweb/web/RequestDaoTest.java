package de.l3s.learnweb.web;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class RequestDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final RequestDao requestDao = learnwebExt.attach(RequestDao.class);

    @Test
    void findByIp() {
        List<Request> retrieved = requestDao.findByIp("33.143.226.138");
        assertFalse(retrieved.isEmpty());
        assertEquals("33.143.226.138", retrieved.getFirst().getAddr());
    }

    @Test
    void findAfterDate() {
        List<Request> retrieved = requestDao.findAfterDate(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0, 0));
        assertFalse(retrieved.isEmpty());
        assertArrayEquals(new String[] {"207.84.18.203", "51.75.160.7"}, retrieved.stream().map(Request::getAddr).sorted().toArray(String[]::new));
    }

    @Test
    void deleteAll() {
        requestDao.deleteAll();
        assertTrue(requestDao.findByIp("33.143.226.138").isEmpty());
    }

    @Test
    void save() {
        Request req = new Request("123.0.0.1", null);
        req.setRequests(2);
        req.setLoginCount(1);
        req.setUsernames("[admin4]");
        req.setCreatedAt(LocalDateTime.of(2021, Month.FEBRUARY, 28, 18, 0, 0));
        requestDao.save(req);

        List<Request> retrieved = requestDao.findByIp("123.0.0.1");
        assertFalse(retrieved.isEmpty());
        assertEquals(req.getUsernames(), retrieved.getFirst().getUsernames());
        assertEquals(req.getCreatedAt(), retrieved.getFirst().getCreatedAt());
    }
}

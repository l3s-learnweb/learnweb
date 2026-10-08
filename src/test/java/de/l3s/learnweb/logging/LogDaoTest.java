package de.l3s.learnweb.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.test.LearnwebExtension;
import de.l3s.util.HasId;

class LogDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final LogDao logDao = learnwebExt.attach(LogDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);

    @Test
    void findByGroupIdAndResourceId() {
        // add_folder (35) logged folder 9, it must not be returned for resource 9
        List<Integer> actions = HasId.collectIds(EnumSet.of(Action.opening_resource, Action.adding_resource, Action.add_folder));
        List<LogEntry> retrieved = logDao.findByGroupIdAndResourceId(1, 9, actions);
        assertEquals(4, retrieved.size());
        retrieved.forEach(logEntry -> {
            assertEquals(9, logEntry.getResourceId());
            assertEquals(0, logEntry.getFolderId());
        });
    }

    @Test
    void findAllByUserId() {
        List<LogEntry> retrieved = logDao.findAllByUserId(1);
        assertEquals(61, retrieved.size());
        assertTrue(retrieved.getFirst().getCreated().isAfter(retrieved.get(60).getCreated()));
    }

    @Test
    void findByUserId() {
        List<Integer> actions = Arrays.asList(7, 6, 29, 35);
        List<LogEntry> retrieved = logDao.findPublicByUserId(1, actions, 10);
        assertEquals(10, retrieved.size());
        assertEquals("", retrieved.getFirst().getParams());
    }

    @Test
    void findByGroupId() {
        List<Integer> actions = Arrays.asList(7, 6, 29, 35);
        List<LogEntry> retrieved = logDao.findByGroupId(1, actions);
        assertEquals(19, retrieved.size());
        assertTrue(retrieved.getFirst().getCreated().isAfter(retrieved.get(18).getCreated()));
        retrieved.forEach(logEntry -> assertEquals(1, logEntry.getGroupId()));
    }

    @Test
    void testFindByGroupId() {
        List<Integer> actions = Arrays.asList(7, 6, 29, 35);
        List<LogEntry> retrieved = logDao.findByGroupId(1, actions, 10);
        assertEquals(10, retrieved.size());
        assertTrue(retrieved.get(0).getCreated().isAfter(retrieved.get(9).getCreated()));
        retrieved.forEach(logEntry -> assertEquals(1, logEntry.getGroupId()));
    }

    @Test
    void findByGroupIdBetweenTime() {
        List<Integer> actions = Arrays.asList(7, 6, 29, 35);
        LocalDateTime start = LocalDateTime.of(2020, 4, 17, 18, 30, 0);
        LocalDateTime end = LocalDateTime.of(2021, 4, 17, 18, 30, 0);
        List<LogEntry> retrieved = logDao.findByGroupIdBetweenTime(1, actions, start, end);
        assertEquals(19, retrieved.size());
        retrieved.forEach(logEntry -> assertTrue(logEntry.getCreated().isAfter(start)));
        retrieved.forEach(logEntry -> assertTrue(logEntry.getCreated().isBefore(end)));
        retrieved.forEach(logEntry -> assertEquals(1, logEntry.getGroupId()));
    }

    @Test
    void findByUsersGroupIds() {
        List<Integer> actions = Arrays.asList(7, 6);
        List<Integer> groups = Arrays.asList(1, 2, 3, 4, 5);
        List<LogEntry> retrieved = logDao.findByUsersGroupIds(1, groups, actions, 10);
        assertEquals(4, retrieved.size());
        retrieved.forEach(logEntry -> assertTrue(groups.contains(logEntry.getGroupId())));
        retrieved.forEach(logEntry -> assertTrue(logEntry.getUserId() != 0 && logEntry.getUserId() != 1));
    }

    @Test
    void findDateOfLastByUserIdAndAction() {
        Optional<LocalDateTime> retrieved = logDao.findDateOfLastByUserIdAndAction(1, Action.register);
        assertTrue(retrieved.isPresent());
        assertEquals(LocalDateTime.of(2021, 2, 18, 11, 27, 21), retrieved.get());
    }

    @Test
    void countUsagePerAction() {
        List<Integer> users = Collections.singletonList(1);
        LocalDate start = LocalDate.of(2021, 1, 17);
        LocalDate end = LocalDate.of(2021, 4, 17);
        Map<Integer, Integer> expected = Map.ofEntries(
            Map.entry(0, 5),
            Map.entry(2, 3),
            Map.entry(3, 4),
            Map.entry(7, 4),
            Map.entry(9, 8),
            Map.entry(10, 4),
            Map.entry(11, 2),
            Map.entry(12, 1),
            Map.entry(15, 3),
            Map.entry(22, 1),
            Map.entry(29, 10),
            Map.entry(35, 8),
            Map.entry(55, 8)
        );
        Map<Integer, Integer> retrieved = logDao.countUsagePerAction(users, start, end);
        assertEquals(expected, retrieved);
    }

    @Test
    void countActionsPerDay() {
        List<Integer> users = Collections.singletonList(1);
        LocalDate start = LocalDate.of(2021, 1, 17);
        LocalDate end = LocalDate.of(2021, 4, 17);
        Map<String, Integer> expected = Map.ofEntries(
            Map.entry("2021-02-18", 3),
            Map.entry("2021-02-19", 58)
        );
        Map<String, Integer> retrieved = logDao.countActionsPerDay(users, start, end);
        assertEquals(expected, retrieved);
    }

    @Test
    void testCountActionsPerDay() {
        List<Integer> users = Collections.singletonList(1);
        LocalDate start = LocalDate.of(2021, 1, 17);
        LocalDate end = LocalDate.of(2021, 4, 17);
        Map<String, Integer> expected = Map.ofEntries(
            Map.entry("2021-02-19", 3)
        );
        Map<String, Integer> retrieved = logDao.countActionsPerDay(users, start, end, "2");
        assertEquals(expected, retrieved);
    }

    @Test
    void insert() {
        Optional<User> user = userDao.findById(1);
        assertTrue(user.isPresent());
        List<LogEntry> old = logDao.findAllByUserId(1);
        assertEquals(61, old.size());

        ActivityEvent event = new ActivityEvent(Action.course_delete).setCourseId(5).setParams("test");
        event.setContext(user.get(), "7E258C2E491CE84BF26128CADF47A0C2");
        logDao.insert(event);

        List<LogEntry> retrieved = logDao.findAllByUserId(1);
        assertEquals(62, retrieved.size());

        LogEntry inserted = retrieved.getFirst();
        assertEquals(Action.course_delete, inserted.getAction());
        assertEquals(5, inserted.getCourseId());
        assertEquals(0, inserted.getResourceId());
        assertEquals(0, inserted.getGroupId());
        assertEquals("test", inserted.getParams());
    }

    @Test
    void insertUserLogAction() {
        logDao.truncateUserLogAction();
        int count = logDao.withHandle(handle -> handle.select("SELECT count(*) FROM lw_user_log_action").mapTo(Integer.class).one());
        assertEquals(0, count);
        logDao.insertUserLogAction(Action.values());
        count = logDao.withHandle(handle -> handle.select("SELECT count(*) FROM lw_user_log_action").mapTo(Integer.class).one());
        assertEquals(Action.values().length, count);
    }
}

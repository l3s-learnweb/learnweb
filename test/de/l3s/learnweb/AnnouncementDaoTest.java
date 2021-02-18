package de.l3s.learnweb;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class AnnouncementDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final AnnouncementDao announcementDao = learnwebExt.attach(AnnouncementDao.class);

    @Test
    void findById() {
        Optional<Announcement> retrieved = announcementDao.findById(1);
        assertTrue(retrieved.isPresent());
        assertEquals(1, retrieved.get().getId());
        assertEquals("Dolores eum illum neque.", retrieved.get().getTitle());
        assertEquals("Omnis tempore et deserunt. Quia qui rerum qui eum commodi sint non. Porro in enim nam quia quo dolores nulla.", retrieved.get().getText());
        assertEquals(2, retrieved.get().getUserId());
        assertEquals(LocalDateTime.of(2017, 3, 3, 0, 1, 2), retrieved.get().getDate());
    }

    @Test
    void findAll() {
        List<Announcement> announcements = announcementDao.findAll();
        assertEquals(10, announcements.size());
    }

    @Test
    void findLastCreated() {
        List<Announcement> announcements = announcementDao.findLastCreated(3);
        assertEquals(3, announcements.size());
        assertEquals("Quo qui eos aliquid iure.", announcements.get(0).getTitle());
        assertEquals(LocalDateTime.of(2015, 9, 9, 12, 7, 19), announcements.get(0).getDate());
        assertEquals("Facilis quisquam praesentium cum consequatur.", announcements.get(1).getTitle());
        assertEquals(LocalDateTime.of(2014, 4, 7, 2, 50, 31), announcements.get(1).getDate());
        assertEquals("Omnis dolorem sit est.", announcements.get(2).getTitle());
        assertEquals(LocalDateTime.of(2013, 5, 16, 3, 13, 4), announcements.get(2).getDate());
    }

    @Test
    void delete() {
        assertTrue(announcementDao.findById(1).isPresent());
        announcementDao.delete(1);
        assertFalse(announcementDao.findById(1).isPresent());
    }

    @Test
    void save() {
        Announcement announcement = new Announcement();
        announcement.setUserId(1);
        announcement.setTitle("ICWL conference, Magdeburg, Germany");
        announcement.setText("MSc. Tetiana Tolmachova presented the full paper...");
        announcementDao.save(announcement);
        assertTrue(announcement.getId() > 0);
        assertNotNull(announcement.getDate());

        Optional<Announcement> retrieved = announcementDao.findById(announcement.getId());
        assertTrue(retrieved.isPresent());
        assertNotSame(announcement, retrieved.get());
        assertEquals(announcement.getId(), retrieved.get().getId());
        assertEquals(announcement.getTitle(), retrieved.get().getTitle());
        assertEquals(announcement.getText(), retrieved.get().getText());
        assertEquals(announcement.getUserId(), retrieved.get().getUserId());
        assertEquals(announcement.getDate(), retrieved.get().getDate());
    }
}

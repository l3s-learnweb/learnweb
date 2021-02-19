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
        Optional<Announcement> announcement = announcementDao.findById(1);
        assertTrue(announcement.isPresent());
        assertEquals(1, announcement.get().getId());
        assertEquals("Dolores eum illum neque.", announcement.get().getTitle());
        assertEquals("Omnis tempore et deserunt. Quia qui rerum qui eum commodi sint non.", announcement.get().getText());
        assertEquals(2, announcement.get().getUserId());
        assertEquals(LocalDateTime.of(2017, 3, 3, 0, 1, 2), announcement.get().getDate());
    }

    @Test
    void findAll() {
        List<Announcement> announcements = announcementDao.findAll();
        assertEquals(10, announcements.size());
    }

    @Test
    void findLastCreated() {
        List<Announcement> announcements = announcementDao.findLastCreated(3);
        assertFalse(announcements.isEmpty());
        assertArrayEquals(new Integer[] {7, 4, 9}, announcements.stream().map(Announcement::getId).sorted().toArray(Integer[]::new));
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
        announcement.setTitle("Quisque porta volutpat erat");
        announcement.setText("Porro in enim nam quia quo dolores nulla.");
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

        announcement.setText("updated text");
        announcementDao.save(announcement);
        assertNotEquals(retrieved.get().getText(), announcement.getText());

        Optional<Announcement> updated = announcementDao.findById(announcement.getId());
        assertTrue(updated.isPresent());
        assertEquals(announcement.getText(), updated.get().getText());
    }
}

package de.l3s.learnweb;

import static org.junit.jupiter.api.Assertions.*;

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
    void getters() {
        Announcement announcement = new Announcement();
        announcement.setTitle("ICWL conference, Magdeburg, Germany");
        announcement.setText("MSc. Tetiana Tolmachova presented the full paper...");
        announcementDao.save(announcement);
        assertTrue(announcement.getId() > 0);
        assertFalse(announcement.isHidden());
        assertNotNull(announcement.getDate());

        Announcement announcement2 = new Announcement();
        announcement2.setTitle("Working Towards the Ideal Search History Interface");
        announcement2.setText("Tetiana Tolmachova, Eleni Ilkou and Luyan Xu presented their work...");
        announcementDao.save(announcement2);
        assertTrue(announcement2.getId() > 0);

        Optional<Announcement> retrieved = announcementDao.findById(announcement.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(announcement.getId(), retrieved.get().getId());
        assertEquals(announcement.getTitle(), retrieved.get().getTitle());
        assertEquals(announcement.getText(), retrieved.get().getText());
        assertNotNull(retrieved.get().getDate());

        List<Announcement> announcements = announcementDao.findAll();
        assertEquals(2, announcements.size());
    }

    @Test
    void save() {
        Announcement announcement = new Announcement();
        announcement.setTitle("ICWL conference, Magdeburg, Germany");
        announcement.setText("MSc. Tetiana Tolmachova presented the full paper...");
        announcementDao.save(announcement);
        assertTrue(announcement.getId() > 0);

        Optional<Announcement> retrieved = announcementDao.findById(announcement.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(announcement.getId(), retrieved.get().getId());
        assertEquals(announcement.getTitle(), retrieved.get().getTitle());
        assertEquals(announcement.getText(), retrieved.get().getText());

        announcement.setText("updated text");
        announcementDao.save(announcement);
        assertNotEquals(retrieved.get().getText(), announcement.getText());

        Optional<Announcement> updated = announcementDao.findById(announcement.getId());
        assertTrue(updated.isPresent());
        assertEquals(announcement.getText(), updated.get().getText());

        announcementDao.delete(announcement.getId());
        Optional<Announcement> deleted = announcementDao.findById(announcement.getId());
        assertFalse(deleted.isPresent());
    }
}

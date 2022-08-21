package de.l3s.learnweb.beans.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.AnnouncementDao;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class AdminAnnouncementsBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -5638619427036990427L;
    // private static final Logger log = LogManager.getLogger(AdminAnnouncementsBean.class);

    private List<Announcement> announcements;

    @Inject
    private AnnouncementDao announcementDao;

    @PostConstruct
    public void init() {
        announcements = announcementDao.findAll();
    }

    public void onDelete(Announcement announcement) {
        announcementDao.delete(announcement.getId());
        announcements.remove(announcement);
        addMessage(FacesMessage.SEVERITY_INFO, "entry_deleted");
    }

    public void onSave(Announcement announcement) {
        announcementDao.save(announcement);
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public List<Announcement> getAnnouncements() {
        return announcements;
    }
}

package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.AnnouncementDao;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class AdminAnnouncementsBean extends ApplicationBean implements Serializable {
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

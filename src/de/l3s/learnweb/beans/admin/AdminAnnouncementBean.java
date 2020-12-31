package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.AnnouncementDao;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

@Named
@ViewScoped
public class AdminAnnouncementBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -5638619327036890427L;
    //    private static final Logger log = LogManager.getLogger(AdminAnnouncementsBean.class);

    private int announcementId;
    private Announcement announcement;
    private String pageTitle;

    @Inject
    private AnnouncementDao announcementDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isAdmin());

        if (announcementId != 0) {
            announcement = announcementDao.findById(announcementId).orElseThrow(BeanAssert.NOT_FOUND);
            pageTitle = announcement.getTitle();
        } else { // create new announcement
            pageTitle = StringUtils.capitalize(getLocaleMessage("new_word"));

            announcement = new Announcement();
            announcement.setUserId(getUser().getId());
            announcement.setCreatedAt(LocalDateTime.now());
        }
    }

    public String onSave() {
        announcementDao.save(announcement);
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        setKeepMessages();

        return "/lw/admin/announcements.xhtml?faces-redirect=true";
    }

    public Announcement getAnnouncement() {
        return announcement;
    }

    public int getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(final int announcementId) {
        this.announcementId = announcementId;
    }

    public String getPageTitle() {
        return pageTitle;
    }

}

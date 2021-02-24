package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

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

        if (announcementId == 0) { // create new announcement
            pageTitle = StringUtils.capitalize(getLocaleMessage("new_word"));

            announcement = new Announcement();
            announcement.setUserId(getUser().getId());
            announcement.setDate(LocalDateTime.now());
        } else {
            announcement = announcementDao.findById(announcementId).orElse(null);
            BeanAssert.isFound(announcement);
            pageTitle = announcement.getTitle();
        }
    }

    public String onSave() {
        announcementDao.save(announcement);
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        setKeepMessages();

        return "admin/announcements.xhtml?faces-redirect=true";
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

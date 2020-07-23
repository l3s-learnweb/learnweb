package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Announcement;
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

    public void onLoad() throws SQLException {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isAdmin());

        if (announcementId == 0) { // create new announcement
            pageTitle = StringUtils.capitalize(getLocaleMessage("new_word"));

            announcement = new Announcement();
            announcement.setUserId(getUser().getId());
            announcement.setDate(new Date());
        } else {
            announcement = getLearnweb().getAnnouncementsManager().getAnnouncementById(announcementId);
            BeanAssert.isFound(announcement);
            pageTitle = announcement.getTitle();
        }
    }

    public String onSave() {
        try {
            announcement.save();

            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");

            setKeepMessages();

            return "/lw/admin/announcements.xhtml?faces-redirect=true";
        } catch (Exception e) {
            addErrorMessage(e);

            return null;
        }
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

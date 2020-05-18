package de.l3s.learnweb.beans.publicPages;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class FrontpageBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 6447676611928379575L;
    private List<Announcement> announcements;

    public FrontpageBean() {
        try {
            announcements = getLearnweb().getAnnouncementsManager().getTopAnnouncements();
        } catch (SQLException e) {
            addErrorMessage("Can't load announcements", e);
        }
    }

    public List<Announcement> getAnnouncements() {
        return announcements;
    }
}

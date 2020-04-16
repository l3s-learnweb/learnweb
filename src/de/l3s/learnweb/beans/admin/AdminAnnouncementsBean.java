package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class AdminAnnouncementsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5638619427036990427L;
    //    private static final Logger log = LogManager.getLogger(AdminAnnouncementsBean.class);

    private List<Announcement> announcements;

    public AdminAnnouncementsBean()
    {
        onLoad();
    }

    public void onLoad()
    {
        try
        {
            announcements = new ArrayList<>(getLearnweb().getAnnouncementsManager().getAnnouncementsAll());
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void onDelete(Announcement announcement)
    {
        try
        {
            getLearnweb().getAnnouncementsManager().delete(announcement);
            addMessage(FacesMessage.SEVERITY_INFO, "entry_deleted");
            onLoad();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void onSave(Announcement announcement)
    {
        try
        {
            announcement.save();

            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public List<Announcement> getAnnouncements()
    {
        return announcements;
    }
}

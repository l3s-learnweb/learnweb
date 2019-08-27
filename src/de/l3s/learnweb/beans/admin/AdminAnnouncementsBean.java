package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class AdminAnnouncementsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5638619427036990427L;
    private static final Logger log = Logger.getLogger(AdminAnnouncementsBean.class);

    private List<Announcement> announcements;
    @NotEmpty
    private String text;
    @NotEmpty
    private String title;

    private Date date;

    private boolean hidden;

    public AdminAnnouncementsBean() throws SQLException
    {
        onLoad();
    }

    public void onLoad() throws SQLException
    {
        try
        {
            announcements = new ArrayList<>(getLearnweb().getAnnouncementsManager().getAnnouncementsAll());
        }
        catch(Exception e)
        {
            log.error(e);
        }
    }

    public void onCreateNews()
    {
        try
        {
            Announcement announcement = new Announcement();
            announcement.setTitle(title);
            announcement.setText(text);
            if(date != null)
            {
                announcement.setDate(date);
            }
            announcement.setHidden(hidden);
            announcement.setUserId(getUser().getId());
            log.debug(announcement.toString());
            getLearnweb().getAnnouncementsManager().save(announcement);
            addGrowl(FacesMessage.SEVERITY_INFO, "Announcement was added !");
            onLoad();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void onDeleteNews(Announcement announcement)
    {
        try
        {
            getLearnweb().getAnnouncementsManager().delete(announcement);
            addGrowl(FacesMessage.SEVERITY_INFO, "Announcement was deleted !");
            onLoad();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void onHidden(int announcementId) // TODO rename to onToggleVisibility
    {
        try
        {
            // TODO this looks to complex. Why to you have to create a new Announcement instance. This will cause problems to the cache

            Announcement announcement = new Announcement();
            boolean hidden = getLearnweb().getAnnouncementsManager().getAnnouncementById(announcementId).isHidden();
            announcement.setHidden(hidden);
            announcement.setId(announcementId);
            log.debug(announcement.toString());
            getLearnweb().getAnnouncementsManager().hide(announcement);
            if(hidden)
            {
                addGrowl(FacesMessage.SEVERITY_INFO, "Announcement is hidden.");
            }
            else
            {
                addGrowl(FacesMessage.SEVERITY_INFO, "Announcement is shown.");
            }
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(final Date date)
    {
        this.date = date;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(final boolean hidden)
    {
        this.hidden = hidden;
    }

    public List<Announcement> getAnnouncements()
    {
        return announcements;
    }

    // TODO this method is used by the frontpage. This should be moved to a separate FrontPageBean
    /*
     * I've already created the bean in de.l3s.learnweb.beans;
     *
     * The used loop isn't very nice. Use a sql query to get the desired entries.
     */
    public List<Announcement> getAvailableAnnouncements()
    {

        for(int i = 0; i < announcements.size();)
        {
            if(announcements.get(i).isHidden())
            {
                announcements.remove(i);
            }
            else
            {
                i++;
            }
        }
        return announcements;
    }

}

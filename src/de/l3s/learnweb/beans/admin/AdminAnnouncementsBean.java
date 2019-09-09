package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class AdminAnnouncementsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5638619427036990427L;
    private static final Logger log = Logger.getLogger(AdminAnnouncementsBean.class);

    private List<Announcement> announcements;
    @NotBlank
    private String text;
    @NotBlank
    private String title;

    private Date date;

    private boolean hidden;

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

    public void onToggleVisibility(int announcementId)
    {
        try
        {
            getLearnweb().getAnnouncementsManager().hide(getLearnweb().getAnnouncementsManager().getAnnouncementById(announcementId));
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
}

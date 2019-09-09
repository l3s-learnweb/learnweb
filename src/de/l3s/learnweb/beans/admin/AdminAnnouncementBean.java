package de.l3s.learnweb.beans.admin;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.beans.ApplicationBean;
import org.apache.log4j.Logger;
import javax.validation.constraints.NotBlank;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;


@Named
@ViewScoped
public class AdminAnnouncementBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5638619327036890427L;
    private static final Logger log = Logger.getLogger(AdminAnnouncementsBean.class);

    @NotBlank
    private String text;
    @NotBlank
    private String title;
    @NotBlank
    private Date date;
    private boolean hidden;

    private int announcementId;
    private Announcement announcement;

    public void onLoad() throws SQLException
    {
        if(getUser() == null)
            return;

        if(!getUser().isAdmin())
        {
            addAccessDeniedMessage();
            return;
        }

        announcement = getLearnweb().getAnnouncementsManager().getAnnouncementById(announcementId);
        if(announcement == null)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "invalid announcement_id parameter");
        }else{
            setDate(announcement.getDate());
            setText(announcement.getText());
            setTitle(announcement.getTitle());
            setHidden(announcement.isHidden());
        }
    }

    public void onUpdateNews(int announcementId)
    {
        try
        {
            Announcement announcement = new Announcement();
            announcement.setTitle(this.getTitle());
            announcement.setText(this.getText());
            announcement.setDate(this.getDate());
            announcement.setId(this.getAnnouncementId());
            announcement.setHidden(this.isHidden());
            getLearnweb().getAnnouncementsManager().update(announcement);
            addGrowl(FacesMessage.SEVERITY_INFO, "Announcement was updated !");
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

    public Announcement getAnnouncement()
    {
        return announcement;
    }

    public int getAnnouncementId()
    {
        return announcementId;
    }

    public void setAnnouncementId(final int announcementId)
    {
        this.announcementId = announcementId;
    }

    public Date getDate() { return date; }

    public void setDate(final Date date) { this.date = date; }

    public boolean isHidden() { return hidden; }

    public void setHidden(final boolean hidden) { this.hidden = hidden; }
}

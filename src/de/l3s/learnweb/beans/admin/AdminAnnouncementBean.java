package de.l3s.learnweb.beans.admin;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.beans.ApplicationBean;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;

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

    @NotEmpty
    private String text;
    @NotEmpty
    private String title;
    private int announcementId;
    private Announcement announcement;
    @NotEmpty
    private Date date;
    private boolean hidden;

    public void onLoad() throws SQLException
    {
        // announcement = getLearnweb().getAnnouncementsManager().getNewsById(getParameterInt("announcement_id"));
        log.debug("init AdminOrganisationBean");

        if(getUser() == null)
            return;

        if(!getUser().isAdmin())
        {
            addAccessDeniedMessage();
            return;
        }
        if(announcementId > 0){
            announcement = getLearnweb().getAnnouncementsManager().getAnnouncementById(announcementId);
            if(announcement == null)
            {
                addGrowl(FacesMessage.SEVERITY_FATAL, "invalid announcement_id parameter");
            }else{
                log.debug(announcement.toString());
            }
        }else{
            log.debug("announcementId is " + announcementId);
        }

    }

    public void onUpdateNews(int announcementId)
    {
        try
        {
            Announcement announcement = new Announcement();
            announcement.setTitle(this.announcement.getTitle());
            announcement.setText(this.announcement.getText());
            announcement.setDate(this.announcement.getDate());
            announcement.setId(announcementId);
            announcement.setHidden(this.announcement.isHidden());
            log.debug(announcement.toString());
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
}

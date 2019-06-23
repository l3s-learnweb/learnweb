package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.Announcement;


@Named
@RequestScoped
public class AdminAnnouncementsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5638619427036990427L;
    private static final Logger log = Logger.getLogger(AdminAnnouncementsBean.class);

    private List<Announcement> announcementList;
    @NotEmpty
    private String text;
    @NotEmpty
    private String title;

    public AdminAnnouncementsBean() throws SQLException
    {
        onLoad();
    }

    public void onLoad() throws SQLException
    {

        try
        {
            announcementList = new ArrayList<>(getLearnweb().getAnnouncementsManager().getAnnouncementsAll());
        }
        catch(Exception e)
        {
            log.error(e);
        }

    }

    public void onCreateNews() throws SQLException
    {
        try
        {
            Announcement announcement = new Announcement();
            announcement.setTitle(title);
            announcement.setText(text);
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

    public void onDeleteNews(Announcement announcement) throws SQLException
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

    public List<Announcement> getAnnouncementList()
    {
        return announcementList;
    }

}

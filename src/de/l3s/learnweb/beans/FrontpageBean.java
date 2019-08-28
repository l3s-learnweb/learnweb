package de.l3s.learnweb.beans;

import de.l3s.learnweb.Announcement;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

@Named
@RequestScoped
public class FrontpageBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6447676610928379575L;
    private static final int MAX_ANNOUNCEMENTS = 5; // maximal number of announcements that will be shown
    private List<Announcement> announcements;

    public FrontpageBean()
    {
        try
        {
            announcements =  getLearnweb().getAnnouncementsManager().getTopAnnouncements(MAX_ANNOUNCEMENTS);
        }
        catch(SQLException e)
        {
            addErrorMessage("Can't load announcements", e);
        }
    }

    public List<Announcement> getAnnouncements()
    {
        return announcements;
    }
}

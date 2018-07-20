package de.l3s.learnweb.user;

import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Organisation.Option;

@Named
@RequestScoped
public class UserDetailBean extends ApplicationBean
{
    private static final Logger log = Logger.getLogger(UserDetailBean.class);

    private int userId;
    private User user;
    private boolean pageHidden = false; // true when the course uses username anonymization

    public UserDetailBean()
    {
        // to nothing constructor
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    @Override
    public User getUser()
    {
        return user;
    }

    public void loadUser()
    {

        if(0 == userId)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid user id");
            return;
        }

        try
        {
            user = getLearnweb().getUserManager().getUser(userId);
        }
        catch(SQLException e)
        {
            log.error("Can't load user", e);
        }

        if(null == user)
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid user id");

        if(user.getOrganisation().getId() == 1249 && user.getOrganisation().getOption(Option.Privacy_Anonymize_usernames))
            pageHidden = true;
    }

    /**
     * true when the course uses username anonymization
     * 
     * @return
     */
    public boolean isPageHidden()
    {
        return pageHidden;
    }

}

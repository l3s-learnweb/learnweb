package de.l3s.learnweb.beans;

import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Organisation.Option;
import de.l3s.learnweb.User;

@ManagedBean
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

        if(user.getOrganisation().getId() == 1249 && user.getOrganisation().getOption(Option.Misc_Anonymize_usernames))
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

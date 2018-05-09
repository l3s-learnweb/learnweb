package de.l3s.learnweb.beans;

import de.l3s.learnweb.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.io.Serializable;
import java.sql.SQLException;

@ManagedBean
@SessionScoped
public class ConfirmRequiredBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(ConfirmRequiredBean.class);
    private static final long serialVersionUID = 934105342636869805L;

    private User loggedInUser;

    private String newEmail;

    public User getUser()
    {
        if (super.getUser() != null) {
            return super.getUser();
        }

        return loggedInUser;
    }

    public boolean isConfirmed()
    {
        return getUser() != null && getUser().getIsEmailConfirmed();
    }

    public String getNewEmail()
    {
        return newEmail;
    }

    public void setNewEmail(String newEmail)
    {
        this.newEmail = newEmail;
    }

    public void onSubmitNewEmail()
    {
        log.debug("onSubmitNewEmail");
        User user = getUser();
        try
        {
            if (!StringUtils.equals(user.getEmail(), newEmail)) {
                user.setEmail(newEmail);
                user.save();
                addMessage(FacesMessage.SEVERITY_INFO, "email_changed");
            } else {
                addMessage(FacesMessage.SEVERITY_INFO, "nothing_to_change");
            }

            newEmail = null;
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public User getLoggedInUser()
    {
        return loggedInUser;
    }

    public void setLoggedInUser(User loggedInUser)
    {
        this.loggedInUser = loggedInUser;
    }
}

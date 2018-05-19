package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Email;

import de.l3s.learnweb.User;

@ManagedBean
@SessionScoped
public class ConfirmRequiredBean extends ApplicationBean implements Serializable
{
    //private static final Logger log = Logger.getLogger(ConfirmRequiredBean.class);
    private static final long serialVersionUID = 934105342636869805L;

    private User loggedInUser;

    @Email
    private String email;

    @Override
    public User getUser()
    {
        if(super.getUser() != null)
        {
            return super.getUser();
        }

        return loggedInUser;
    }

    public boolean isConfirmed()
    {
        return getUser() != null && getUser().isEmailConfirmed();
    }

    public void onSubmitNewEmail()
    {
        if(email.endsWith("uni.au.dk"))
        {
            addMessage(FacesMessage.SEVERITY_FATAL, "This is not a valid mail address. Use for example stundetid@post.au.dk");
            return;
        }
        User user = getUser();
        try
        {
            if(!StringUtils.equals(user.getEmail(), email))
            {
                user.setEmail(email);
                user.save();
            }

            if(user.sendEmailConfirmation())
                addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_send");
            else
                addMessage(FacesMessage.SEVERITY_FATAL, "We were not able to send a confirmation mail");
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
        this.email = loggedInUser.getEmail();
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}

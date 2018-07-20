package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.inject.Inject;
import javax.enterprise.context.RequestScoped;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class ConfirmEmailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6040579499279231182L;

    private String email;
    private String token;

    private User user = null;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public String onLoad() throws SQLException
    {
        if(StringUtils.isAnyEmpty(email, token))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
            return null;
        }

        if(token.length() < 32)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "confirm_token_to_short");
            return null;
        }

        user = getLearnweb().getUserManager().getUserByEmailAndConfirmationToken(email, token);

        if(user == null)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "confirm_token_invalid");
            return null;
        }

        user.setEmailConfirmed(true);
        user.setEmailConfirmationToken(null);
        user.save();

        addMessage(FacesMessage.SEVERITY_INFO, "email_confirmed");

        if(user.equals(getConfirmRequiredBean().getLoggedInUser()))
        {
            LoginBean.loginUser(this, user);
            return "/lw/" + user.getOrganisation().getWelcomePage() + "?faces-redirect=true";
        }
        return null;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public boolean isConfirmed()
    {
        return user != null && user.isEmailConfirmed();
    }

    @Override
    public User getUser()
    {
        return user;
    }

    public ConfirmRequiredBean getConfirmRequiredBean()
    {
        return confirmRequiredBean;
    }

    public void setConfirmRequiredBean(ConfirmRequiredBean confirmRequiredBean)
    {
        this.confirmRequiredBean = confirmRequiredBean;
    }
}

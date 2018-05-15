package de.l3s.learnweb.beans;

import de.l3s.learnweb.User;
import org.apache.commons.lang3.StringUtils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import java.io.Serializable;
import java.sql.SQLException;

@ManagedBean
@RequestScoped
public class ConfirmEmailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6040579499279231182L;

    private String email;
    private String token;

    private User user = null;

    @ManagedProperty(value = "#{confirmRequiredBean}")
    private ConfirmRequiredBean confirmRequiredBean;

    public void onLoad() throws SQLException
    {
        if(StringUtils.isAnyEmpty(email, token))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
            return;
        }

        user = getLearnweb().getUserManager().getUserByEmailAndConfirmationToken(email, token);

        if(user == null)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "confirm_token_invalid");
            return;
        }

        user.setIsEmailConfirmed(true);
        user.setEmailConfirmationToken(null);
        user.save();

        if (user.equals(confirmRequiredBean.getLoggedInUser())) {
            LoginBean.loginUser(this, user);
            UtilBean.redirect("/lw/" + user.getOrganisation().getWelcomePage() + "?faces-redirect=true");
        }
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
        return user != null && user.getIsEmailConfirmed();
    }

    @Override
    public User getUser()
    {
        return user;
    }

    @SuppressWarnings("unused")
    public ConfirmRequiredBean getConfirmRequiredBean()
    {
        return confirmRequiredBean;
    }

    @SuppressWarnings("unused")
    public void setConfirmRequiredBean(ConfirmRequiredBean confirmRequiredBean)
    {
        this.confirmRequiredBean = confirmRequiredBean;
    }
}

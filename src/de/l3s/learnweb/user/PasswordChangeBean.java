package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class PasswordChangeBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(PasswordChangeBean.class);
    private static final long serialVersionUID = 2237249691332567548L;

    private String parameter;

    private String password;
    private String confirmPassword;

    private User user = null;

    public void onLoad()
    {
        try {
            String[] splits = parameter.split("_");
            int userId = Integer.parseInt(splits[0]);
            String hash = splits[1];

            user = getLearnweb().getUserManager().getUser(userId);
            if(!hash.equals(PasswordBean.createPasswordChangeHash(user)))
                throw new IllegalArgumentException();
        }
        catch(Exception e)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
            user = null;
        }
    }

    public String changePassword()
    {
        log.debug("onChangePassword");
        UserManager um = getLearnweb().getUserManager();
        try
        {
            user.setPassword(password);
            um.save(user);

            getFacesContext().getExternalContext().getFlash().setKeepMessages(true); // keep message after redirect
            addMessage(FacesMessage.SEVERITY_INFO, "password_changed");
            return "/lw/user/login.xhtml?faces-redirect=true";
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
            return null;
        }
    }

    public String getParameter()
    {
        return parameter;
    }

    public void setParameter(String parameter)
    {
        this.parameter = parameter;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public User getUser()
    {
        return user;
    }
}

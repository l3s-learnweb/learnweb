package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import org.hibernate.validator.constraints.NotBlank;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
@RequestScoped
public class LoginBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = 7980062591522267111L;
    @NotBlank
    private String username;
    @NotBlank
    private String password;

    public String getUsername()
    {
	return username;
    }

    public void setUsername(String name)
    {
	this.username = name;
    }

    public String getPassword()
    {
	return password;
    }

    public void setPassword(String password)
    {
	this.password = password;
    }

    public String login() throws SQLException
    {
	final User user = getLearnweb().getUserManager().getUser(username, password);

	if(null == user)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "wrong_username_or_password");
	    return null;
	}

	UtilBean.getUserBean().setUser(user); // logs the user in
	//addMessage(FacesMessage.SEVERITY_INFO, "welcome_username", user.getUsername());

	log(Action.login, 0);

	// uncommented until interwebJ works correct
	Runnable preFetch = new Runnable()
	{
	    @Override
	    public void run()
	    {
		InterWeb interweb = user.getInterweb();
		try
		{
		    interweb.getAuthorizationInformation(false);
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		}
	    }
	};
	new Thread(preFetch).start();

	// if the user logs in from the start or the login page, redirect him to the welcome page
	String viewId = getFacesContext().getViewRoot().getViewId();
	if(viewId.endsWith("/user/login.xhtml") || viewId.endsWith("index.xhtml") || viewId.endsWith("error.xhtml") || viewId.endsWith("expired.xhtml"))
	{
	    return "/lw/" + user.getOrganisation().getWelcomePage() + "?faces-redirect=true";
	}

	// otherwise reload his last page
	return viewId + "?faces-redirect=true&includeViewParams=true";

    }

    public String logout()
    {
	setKeepMessages();

	log(Action.logout, 0, null);
	UtilBean.getUserBean().setUser(null);

	//addMessage(FacesMessage.SEVERITY_INFO, "logout_success");
	FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
	return "/lw/index.xhtml?faces-redirect=true";
    }
}

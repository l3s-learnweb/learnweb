package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Organisation;
import de.l3s.learnweb.User;
import de.l3s.learnweb.loginprotection.ProtectionManager;
import de.l3s.util.BeanHelper;

@ManagedBean
@RequestScoped
public class LoginBean extends ApplicationBean implements Serializable
{
    // private static final Logger log = Logger.getLogger(LoginBean.class);

    private static final long serialVersionUID = 7980062591522267111L;
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;

    public LoginBean()
    {
        super();
    }

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

    public boolean captchaRequired()
    {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String ip = BeanHelper.getIp(request);

        return getLearnweb().getProtectionManager().needsCaptcha(ip);
    }

    public String login() throws SQLException
    {
        //Getting IP
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String ip = BeanHelper.getIp(request);

        //Gets the ip and username info from protection manager
        ProtectionManager PM = getLearnweb().getProtectionManager();
        Date now = new Date();

        Date ipban = PM.getBannedUntil(ip);
        Date userban = PM.getBannedUntil(username);

        if(ipban != null && ipban.after(now))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "ip_banned" + ipban.toString());
            return null;
        }

        if(userban != null && userban.after(now))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "username_banned" + userban.toString());
            return null;
        }

        //USER AUTHORIZATION HAPPENS HERE
        final User user = getLearnweb().getUserManager().getUser(username, password);

        if(null == user)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "wrong_username_or_password");
            PM.updateFailedAttempts(ip, username);
            return null;
        }
        else
        {
            PM.updateSuccessfuldAttempts(ip, username);

            getLearnweb().getRequestManager().recordLogin(ip, username);
        }

        return loginUser(this, user);
    }

    public static String loginUser(ApplicationBean bean, User user) throws SQLException
    {
        return loginUser(bean, user, false);
    }

    /**
     *
     * @param bean
     * @param user
     * @param disableLog Only useful when a moderator logs into a user account
     * @return
     * @throws SQLException
     */
    public static String loginUser(ApplicationBean bean, User user, boolean disableLog) throws SQLException
    {
        UtilBean.getUserBean().setUser(user); // logs the user in
        //addMessage(FacesMessage.SEVERITY_INFO, "welcome_username", user.getUsername());
        user.setCurrentLoginDate(new Date());

        if(!disableLog)
            bean.log(Action.login, 0, 0);

        // uncommented until interwebJ works correct
        /*
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
        	    log.error("Interweb error", e);
        	}
            }
        };
        new Thread(preFetch).start();
        */

        Organisation userOrganisation = user.getOrganisation();

        if(userOrganisation.getDefaultLanguage() != null)
        {
            UtilBean.getUserBean().setLocaleCode(userOrganisation.getDefaultLanguage());
        }

        // set default search service if not already selected
        if(bean.getPreference("SEARCH_SERVICE_TEXT") == null || bean.getPreference("SEARCH_SERVICE_IMAGE") == null || bean.getPreference("SEARCH_SERVICE_VIDEO") == null)
        {
            bean.setPreference("SEARCH_SERVICE_TEXT", userOrganisation.getDefaultSearchServiceText().name());
            bean.setPreference("SEARCH_SERVICE_IMAGE", userOrganisation.getDefaultSearchServiceImage().name());
            bean.setPreference("SEARCH_SERVICE_VIDEO", userOrganisation.getDefaultSearchServiceVideo().name());
        }

        // if the user logs in from the start or the login page, redirect him to the welcome page
        String viewId = getFacesContext().getViewRoot().getViewId();
        if(viewId.endsWith("/user/login.xhtml") || viewId.endsWith("index.xhtml") || viewId.endsWith("error.xhtml") || viewId.endsWith("expired.xhtml") || viewId.endsWith("register.xhtml") || viewId.endsWith("admin/users.xhtml") && disableLog)
        {
            return "/lw/" + userOrganisation.getWelcomePage() + "?faces-redirect=true";
        }

        // otherwise reload his last page
        return viewId + "?faces-redirect=true&includeViewParams=true";
    }

    public String logout()
    {
        UserBean userBean = UtilBean.getUserBean();
        int organisationId = userBean.getUser().getOrganisationId();

        if(userBean.getModeratorUser() != null && !userBean.getModeratorUser().equals(userBean.getUser())) // a moderator logs out from a user account
        {
            userBean.setUser(userBean.getModeratorUser()); // logout user and login moderator
            return "/lw/admin/users.xhtml?faces-redirect=true";
        }
        else
            getFacesContext().getExternalContext().invalidateSession(); // end session

        log(Action.logout, 0, 0);

        //userBean.setUser(null);

        if(organisationId == 848) // is archive web course
        {
            //userBean.setActiveCourseId(891);

            return "/aw/index.xhtml?faces-redirect=true";
        }
        else
            return "/lw/index.xhtml?faces-redirect=true";
    }

}

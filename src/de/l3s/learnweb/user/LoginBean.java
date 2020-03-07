package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.InetAddresses;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;
import de.l3s.util.bean.BeanHelper;

@Named
@RequestScoped
public class LoginBean extends ApplicationBean implements Serializable
{
    // private static final Logger log = Logger.getLogger(LoginBean.class);
    private static final long serialVersionUID = 7980062591522267111L;
    private static final String LOGIN_PAGE = "/lw/user/login.xhtml";

    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private boolean captchaRequired;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public LoginBean()
    {
        String ip = BeanHelper.getIp();

        if(!InetAddresses.isInetAddress(ip))
            captchaRequired = true;
        else
            captchaRequired = getLearnweb().getProtectionManager().needsCaptcha(ip);
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = StringUtils.trim(username);
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public boolean isCaptchaRequired()
    {
        return captchaRequired;
    }

    public String login() throws SQLException
    {
        //Getting IP
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String ip = BeanHelper.getIp(request);

        if(!InetAddresses.isInetAddress(ip))
        {
            return LOGIN_PAGE;
        }

        //Gets the ip and username info from protection manager
        ProtectionManager pm = getLearnweb().getProtectionManager();
        Date now = new Date();

        Date ipBan = pm.getBannedUntil(ip);
        Date userBan = pm.getBannedUntil(username);

        if(ipBan != null && ipBan.after(now))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "ip_banned" + ipBan.toString());
            return LOGIN_PAGE;
        }

        if(userBan != null && userBan.after(now))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "username_banned" + userBan.toString());
            return LOGIN_PAGE;
        }

        //USER AUTHORIZATION HAPPENS HERE
        final User user = getLearnweb().getUserManager().getUser(username, password);

        if(null == user)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "wrong_username_or_password");
            pm.updateFailedAttempts(ip, username);
            return LOGIN_PAGE;
        }

        pm.updateSuccessfulAttempts(ip, username);
        getLearnweb().getRequestManager().recordLogin(ip, username);

        if(!user.isEmailConfirmed())
        {
            confirmRequiredBean.setLoggedInUser(user);
            return "/lw/user/confirm_required.xhtml?faces-redirect=true";
        }

        return loginUser(this, user);
    }

    public static String loginUser(ApplicationBean bean, User user) throws SQLException
    {
        return loginUser(bean, user, -1);
    }

    /**
     * @param moderatorUserId larger zero if a moderator logs into a user account through the admin interface
     */
    public static String loginUser(ApplicationBean bean, User user, int moderatorUserId) throws SQLException
    {
        UtilBean.getUserBean().setUser(user); // logs the user in
        //addMessage(FacesMessage.SEVERITY_INFO, "welcome_username", user.getUsername());

        user.updateLoginDate(); // the last login date has to be updated before we log a new login event

        if(moderatorUserId > 0)
            bean.log(Action.moderator_login, 0, 0);
        else
            bean.log(Action.login, 0, 0, BeanHelper.getRequestURI());

        Organisation userOrganisation = user.getOrganisation();

        // set default search service if not already selected
        if(bean.getPreference("SEARCH_SERVICE_TEXT") == null || bean.getPreference("SEARCH_SERVICE_IMAGE") == null || bean.getPreference("SEARCH_SERVICE_VIDEO") == null)
        {
            bean.setPreference("SEARCH_SERVICE_TEXT", userOrganisation.getDefaultSearchServiceText().name());
            bean.setPreference("SEARCH_SERVICE_IMAGE", userOrganisation.getDefaultSearchServiceImage().name());
            bean.setPreference("SEARCH_SERVICE_VIDEO", userOrganisation.getDefaultSearchServiceVideo().name());
        }

        // if the user logs in from the start or the login page, redirect him to the welcome page
        String viewId = getFacesContext().getViewRoot().getViewId();
        if(viewId.endsWith("/user/login.xhtml") || viewId.endsWith("index.xhtml") || viewId.endsWith("error.xhtml") || viewId.endsWith("expired.xhtml") || viewId.endsWith("register.xhtml") || viewId.endsWith("admin/users.xhtml") && moderatorUserId > 0)
        {
            return userOrganisation.getWelcomePage() + "?faces-redirect=true";
        }

        // otherwise reload his last page
        return viewId + "?faces-redirect=true&includeViewParams=true";
    }

    public String logout()
    {
        UserBean userBean = UtilBean.getUserBean();
        User user = userBean.getUser();
        String logoutPage = user.getOrganisation().getLogoutPage();

        if(userBean.getModeratorUser() != null && !userBean.getModeratorUser().equals(user)) // a moderator logs out from a user account
        {
            userBean.setUser(userBean.getModeratorUser()); // logout user and login moderator
            userBean.setModeratorUser(null);
            return "/lw/admin/users.xhtml?faces-redirect=true";
        }
        else
        {
            log(Action.logout, 0, 0);
            user.onDestroy();
            getFacesContext().getExternalContext().invalidateSession(); // end session
        }

        return logoutPage + "?faces-redirect=true";
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

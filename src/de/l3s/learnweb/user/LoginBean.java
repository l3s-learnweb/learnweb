package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import com.google.common.net.InetAddresses;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;

@Named
@RequestScoped
public class LoginBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 7980062591522267111L;

    private static final Logger log = LogManager.getLogger(LoginBean.class);
    private static final String LOGIN_PAGE = "/lw/user/login.xhtml";
    public static final String AUTH_COOKIE_NAME = "auth_uuid";
    private static final int AUTH_COOKIE_AGE = 30 * 24 * 60 * 60;

    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private boolean remember;
    private final boolean captchaRequired;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public LoginBean() {
        String ip = Faces.getRemoteAddr();

        if (!InetAddresses.isInetAddress(ip)) {
            captchaRequired = true;
        } else {
            captchaRequired = getLearnweb().getProtectionManager().needsCaptcha(ip);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = StringUtils.trim(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRemember() {
        return remember;
    }

    public void setRemember(final boolean remember) {
        this.remember = remember;
    }

    public boolean isCaptchaRequired() {
        return captchaRequired;
    }

    public String login() throws SQLException {
        String ip = Faces.getRemoteAddr();

        if (!InetAddresses.isInetAddress(ip)) {
            return LOGIN_PAGE;
        }

        // Gets the ip and username info from protection manager
        ProtectionManager pm = getLearnweb().getProtectionManager();
        BeanAssert.hasPermission(!pm.isBanned(ip), "ip_banned");
        BeanAssert.hasPermission(!pm.isBanned(username), "username_banned");

        // USER AUTHORIZATION HAPPENS HERE
        final User user = getLearnweb().getUserManager().getUser(username, password);

        if (null == user) {
            addMessage(FacesMessage.SEVERITY_ERROR, "wrong_username_or_password");
            pm.updateFailedAttempts(ip, username);
            return LOGIN_PAGE;
        }

        pm.updateSuccessfulAttempts(ip, username);
        getLearnweb().getRequestManager().recordLogin(ip, username);

        if (!user.isEmailConfirmed() && user.isEmailRequired()) {
            confirmRequiredBean.setLoggedInUser(user);
            return "/lw/user/confirm_required.xhtml?faces-redirect=true";
        }

        if (remember) {
            long authId = RandomUtils.nextLong();
            String token = RandomStringUtils.randomAlphanumeric(128);

            Faces.addResponseCookie(AUTH_COOKIE_NAME, authId + ":" + token, "/", AUTH_COOKIE_AGE);
            getLearnweb().getUserManager().saveAuth(user, authId, token, AUTH_COOKIE_AGE);
        } else {
            Faces.removeResponseCookie(AUTH_COOKIE_NAME, "/");
        }

        return loginUser(this, user);
    }

    /**
     * Performs different logout actions depending on whether a simple user is currently logged in
     * or if a moderator had logged into another user's account.
     */
    public String logout() {
        UserBean userBean = getUserBean();
        User user = userBean.getUser();
        String logoutPage = user.getOrganisation().getLogoutPage();

        if (userBean.getModeratorUser() != null && !userBean.getModeratorUser().equals(user)) { // a moderator logs out from a user account
            userBean.setUser(userBean.getModeratorUser()); // logout user and login moderator
            userBean.setModeratorUser(null);
            return "/lw/admin/users.xhtml?faces-redirect=true";
        } else {
            log(Action.logout, 0, 0);
            user.onDestroy();
            Faces.invalidateSession();
            Faces.removeResponseCookie(AUTH_COOKIE_NAME, "/");
            return logoutPage + "?faces-redirect=true";
        }
    }

    public ConfirmRequiredBean getConfirmRequiredBean() {
        return confirmRequiredBean;
    }

    public void setConfirmRequiredBean(ConfirmRequiredBean confirmRequiredBean) {
        this.confirmRequiredBean = confirmRequiredBean;
    }

    public static String rootLogin(ApplicationBean bean, User targetUser) throws SQLException {
        UserBean userBean = bean.getUserBean();
        // validate permission
        BeanAssert.hasPermission(userBean.canLoginToAccount(targetUser), userBean.getUser() + " tried to hijack account");
        // store moderator account while logged in as user
        userBean.setModeratorUser(userBean.getUser());
        // login
        return loginUser(bean, targetUser);
    }

    public static String loginUser(ApplicationBean bean, User user) throws SQLException {
        UserBean userBean = bean.getUserBean();
        userBean.setUser(user); // logs the user in
        // addMessage(FacesMessage.SEVERITY_INFO, "welcome_username", user.getUsername());

        user.updateLoginDate(); // the last login date has to be updated before we log a new login event

        if (userBean.getModeratorUser() != null) {
            bean.log(Action.moderator_login, 0, userBean.getModeratorUser().getId());
        } else {
            bean.log(Action.login, 0, 0, Faces.getRequestURI());
        }

        Organisation userOrganisation = user.getOrganisation();

        // set default search service if not already selected
        if (userBean.getPreference("SEARCH_SERVICE_TEXT") == null
            || userBean.getPreference("SEARCH_SERVICE_IMAGE") == null
            || userBean.getPreference("SEARCH_SERVICE_VIDEO") == null) {

            userBean.setPreference("SEARCH_SERVICE_TEXT", userOrganisation.getDefaultSearchServiceText().name());
            userBean.setPreference("SEARCH_SERVICE_IMAGE", userOrganisation.getDefaultSearchServiceImage().name());
            userBean.setPreference("SEARCH_SERVICE_VIDEO", userOrganisation.getDefaultSearchServiceVideo().name());
        }

        String redirect = Faces.getRequestParameter("redirect");
        if (StringUtils.isNotEmpty(redirect)) {
            return redirect(user, redirect);
        }

        if (userOrganisation.getId() == 1249) { // TODO @astappiev: EU-MADE4LL user have to be redirect to the backup of Learnweb V2
            return "/lw/eumade4all/statistics.xhtml?faces-redirect=true";
        }

        // if the user logs in from the start or the login page, redirect him to the welcome page
        String viewId = Faces.getViewId();
        if (StringUtils.endsWithAny(viewId, "/index.xhtml", "/user/login.xhtml", "/user/register.xhtml", "/admin/users.xhtml")) {
            return userOrganisation.getWelcomePage() + "?faces-redirect=true";
        }

        // otherwise reload his last page
        return viewId + "?faces-redirect=true&includeViewParams=true";
    }

    public static String redirect(User user, String redirectUrl) throws SQLException {
        if (StringUtils.isEmpty(redirectUrl)) {
            redirectUrl = Faces.getRequestParameter("redirect");
        }

        if (user != null && StringUtils.isNotEmpty(redirectUrl)) {
            // this `grant` parameter is used by annotation client/waps proxy to receive grant token for user auth
            String grant = Faces.getRequestParameter("grant");
            if (StringUtils.isNotEmpty(grant)) {
                String token = Learnweb.getInstance().getUserManager().getGrantToken(user.getId());
                log.debug("Grant token [{}] requested for user [{}], redirect to {}", token, user.getId(), redirectUrl);
                Faces.redirect(redirectUrl + "?token=" + token);
            }

            return redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "faces-redirect=true";
        }
        return null;
    }
}

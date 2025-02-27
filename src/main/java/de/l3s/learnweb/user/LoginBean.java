package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;
import org.omnifaces.util.Utils;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.EventBus;
import de.l3s.learnweb.logging.LearnwebEvent;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.util.HashHelper;

@Named
@RequestScoped
public class LoginBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 7980062591522267111L;
    private static final Logger log = LogManager.getLogger(LoginBean.class);
    public static final String AUTH_COOKIE_NAME = "auth_uuid";
    private static final int AUTH_COOKIE_AGE_DAYS = 30;

    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private boolean remember;

    @Inject
    private UserDao userDao;

    @Inject
    private TokenDao tokenDao;

    @Inject
    private EventBus eventBus;

    @Inject
    private RequestManager requestManager;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    private transient String remoteAddr;

    public String onLoad() {
        String redirectUrl = Faces.getRequestParameter("redirect");
        if (isLoggedIn() && StringUtils.isNotEmpty(redirectUrl)) {
            return redirect(getUser().getId(), redirectUrl);
        }

        return null;
    }

    private String getRemoteAddr() {
        if (remoteAddr == null) {
            remoteAddr = Faces.getRemoteAddr();
        }
        return remoteAddr;
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
        return requestManager.isCaptchaRequired(getRemoteAddr());
    }

    public String login() {
        // Gets the ip and username info from protection manager
        BeanAssert.hasPermission(!requestManager.isBanned(getRemoteAddr()), "ip_banned");
        BeanAssert.hasPermission(!requestManager.isBanned(username), "username_banned");

        // USER AUTHORIZATION HAPPENS HERE
        Optional<User> userOptional;
        try {
            userOptional = userDao.findByUsernameAndPassword(username, password);
        } catch (IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Your password used to be hashed with an old algorithm. Please reset your password.");
            return "/lw/user/password.xhtml?faces-redirect=true";
        }

        if (userOptional.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "wrong_username_or_password");
            requestManager.recordFailedAttempt(getRemoteAddr(), username);
            return "/lw/user/login.xhtml";
        }

        final User user = userOptional.get();
        requestManager.recordSuccessfulAttempt(getRemoteAddr(), username);

        if (!user.isEmailConfirmed() && user.isEmailRequired()) {
            confirmRequiredBean.setLoggedInUser(user);
            return "/lw/user/confirm_required.xhtml?faces-redirect=true";
        }

        if (remember) {
            String authToken = RandomStringUtils.secure().nextAlphanumeric(128);
            int tokenId = tokenDao.insert(user.getId(), Token.TokenType.AUTH, HashHelper.sha512(authToken), LocalDateTime.now().plusDays(AUTH_COOKIE_AGE_DAYS));
            Faces.addResponseCookie(AUTH_COOKIE_NAME, tokenId + ":" + authToken, config().getContextPath(), Math.toIntExact(Duration.ofDays(AUTH_COOKIE_AGE_DAYS).toSeconds()));
        } else {
            Faces.removeResponseCookie(AUTH_COOKIE_NAME, config().getContextPath());
        }

        return loginUser(this, eventBus, user);
    }

    /**
     * Performs different logout actions depending on whether a simple user is currently logged in
     * or if a moderator had logged into another user's account.
     */
    public String logout() {
        UserBean userBean = getUserBean();
        User user = userBean.getUser();

        if (userBean.getModeratorUser() != null && !userBean.getModeratorUser().equals(user)) { // a moderator logs out from a user account
            userBean.setUser(userBean.getModeratorUser()); // logout user and login moderator
            userBean.setModeratorUser(null);
            return "/lw/admin/users.xhtml?faces-redirect=true";
        } else {
            eventBus.dispatch(new LearnwebEvent(Action.logout));
            Faces.invalidateSession();
            Faces.removeResponseCookie(AUTH_COOKIE_NAME, config().getContextPath());
            return "/lw/index.jsf?faces-redirect=true";
        }
    }

    public static String rootLogin(ApplicationBean bean, EventBus eventBus, User targetUser) {
        UserBean userBean = bean.getUserBean();
        // validate permission
        BeanAssert.hasPermission(userBean.canLoginToAccount(targetUser), userBean.getUser() + " tried to hijack account");
        // store moderator account while logged in as user
        userBean.setModeratorUser(userBean.getUser());
        // login
        return loginUser(bean, eventBus, targetUser);
    }

    public static String loginUser(ApplicationBean bean, EventBus eventBus, User user) {
        UserBean userBean = bean.getUserBean();
        if (userBean.getModeratorUser() != null) {
            eventBus.dispatch(new LearnwebEvent(Action.moderator_login).setTargetUser(user));
        } else {
            user.updateLoginDate(); // the last login date has to be updated before we log a new login event
            eventBus.dispatch(new LearnwebEvent(Action.login, Faces.getRequestURI()).setTargetUser(user));
        }

        userBean.setUser(user); // logs the user in

        Organisation userOrganisation = user.getOrganisation();
        String redirect = Faces.getRequestParameter("redirect");
        if (StringUtils.isNotEmpty(redirect)) {
            return redirect(user.getId(), redirect);
        }

        if (userOrganisation.getId() == 1249) {
            return "https://learnweb.l3s.uni-hannover.de/v2/lw/eumade4all/statistics.jsf?faces-redirect=true";
        }

        // if the user logs in from the start or the login page, redirect him to the welcome page
        String viewId = Faces.getViewId();
        if (Strings.CS.endsWithAny(viewId, "/index.xhtml", "/user/login.xhtml", "/user/register.xhtml", "/admin/users.xhtml")) {
            return "/lw/" + userOrganisation.getWelcomePage() + "?faces-redirect=true";
        }

        // otherwise reload his last page
        return viewId + "?faces-redirect=true&includeViewParams=true";
    }

    private static String redirect(int userId, String redirectUrl) {
        // this `grant` parameter is used by annotation client/waps proxy to receive grant token for user auth
        String grant = Faces.getRequestParameter("grant");
        if (StringUtils.isNotEmpty(grant)) {
            String token = Learnweb.dao().getTokenDao().findOrCreate(Token.TokenType.GRANT, userId);
            log.debug("Grant token [{}] requested for user [{}], redirect to {}", token, userId, redirectUrl);
            Faces.redirect(redirectUrl + "?token=" + token);
        }

        if ("/lw/".equals(redirectUrl)) {
            redirectUrl = "/lw/index.jsf"; // redirect to view id
        }

        return redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "faces-redirect=true";
    }

    public static String prepareLoginURL(HttpServletRequest request) {
        String requestURI = Servlets.getRequestURI(request).substring(request.getContextPath().length());
        String queryString = Servlets.getRequestQueryString(request);
        String redirectToUrl = (queryString == null) ? requestURI : (requestURI + "?" + queryString);
        return request.getContextPath() + "/lw/user/login.jsf?redirect=" + Utils.encodeURL(redirectToUrl);
    }
}

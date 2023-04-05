package de.l3s.learnweb.beans;

import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.app.DaoProvider;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class ApplicationBean {
    private static final Logger log = LogManager.getLogger(ApplicationBean.class);

    private transient Learnweb learnweb;
    private transient String sessionId;

    @Inject
    private UserBean userBean;

    // User ------------------------------------------------------------------------------------------------------------

    /**
     * Returns the currently logged-in user.
     *
     * @return null if not logged in
     */
    protected User getUser() {
        // This value shall not be cached. The value would not be updated if the user logs out.
        return userBean.getUser();
    }

    /**
     * Returns the current locale.
     */
    protected Locale getLocale() {
        return userBean.getLocale();
    }

    /**
     * @return true if the user is logged in
     */
    protected boolean isLoggedIn() {
        return userBean.isLoggedIn();
    }

    public UserBean getUserBean() {
        return userBean;
    }

    // i18n ------------------------------------------------------------------------------------------------------------

    public ResourceBundle getBundle() {
        return MessagesBundle.of(getLocale());
    }

    /**
     * Get a message from the messages bundle depending on the currently used local.
     * If the msgKey doesn't exist the msgKey itself will be returned.
     */
    public String getLocaleMessage(String msgKey, Object... args) {
        return MessagesBundle.format(getBundle(), msgKey, args);
    }

    // Preferences -----------------------------------------------------------------------------------------------------

    /**
     * Retrieves an object that was previously set by setPreference().
     * @return defaultValue if no corresponding value is found for the key.
     */
    public String getPreference(String key, String defaultValue) {
        String obj = userBean.getPreference(key);
        return obj == null ? defaultValue : obj;
    }

    /**
     * Stores an object in the session.
     */
    public void setPreference(String key, String value) {
        userBean.setPreference(key, value);
    }

    // Logging ---------------------------------------------------------------------------------------------------------

    /**
     * Logs a user action for the currently active user.
     *
     * @param targetId depend on the logged action, look at the code of LogEntry.Action for explanation.
     */
    public void log(Action action, int groupId, int targetId, int params) {
        log(action, groupId, targetId, Integer.toString(params), getUser());
    }

    /**
     * Logs a user action for the currently active user.
     *
     * @param targetId depend on the logged action, look at the code of LogEntry.Action for explanation.
     */
    public void log(Action action, int groupId, int targetId, String params) {
        log(action, groupId, targetId, params, getUser());
    }

    /**
     * Logs a user action for the currently active user.
     *
     * @param targetId depend on the logged action, look at the code of LogEntry.Action for explanation.
     */
    public void log(Action action, int groupId, int targetId) {
        log(action, groupId, targetId, null, getUser());
    }

    public void log(Action action, Resource resource, int params) {
        log(action, resource.getGroupId(), resource.getId(), Integer.toString(params), getUser());
    }

    public void log(Action action, Resource resource, String params) {
        log(action, resource.getGroupId(), resource.getId(), params, getUser());
    }

    public void log(Action action, Resource resource) {
        log(action, resource.getGroupId(), resource.getId(), null, getUser());
    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" and "params" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     */
    protected void log(Action action, Integer groupId, Integer targetId, String params, User user) {
        if (null != user) {
            // TODO: anonymous logging
            dao().getLogDao().insert(user, action, groupId, targetId, params, getSessionId());
        }
    }

    // Messaging -------------------------------------------------------------------------------------------------------

    protected FacesMessage getFacesMessage(FacesMessage.Severity severity, String msgKey, Object... args) {
        return new FacesMessage(severity, getLocaleMessage(msgKey, args), null);
    }

    /**
     * Call this method if you want to keep messages during a post-redirect-get.
     * This value determines whether any FacesMessage instances queued in the current FacesContext must be preserved, so they are accessible on
     * the next traversal of the lifecycle on this session, regardless of the request being a redirect after post, or a normal postback.
     */
    public void setKeepMessages() {
        Faces.getFlash().setKeepMessages(true);
    }

    /**
     * Adds a global message to the Faces context. Which will be displayed by the p:messages component.
     * Use if for errors and persistent messages, like expires resources, mistakes, etc.
     */
    protected void addMessage(FacesMessage.Severity severity, String msgKey, Object... args) {
        Messages.add(null, getFacesMessage(severity, msgKey, args));

        if (FacesMessage.SEVERITY_FATAL == severity) {
            throw new BadRequestHttpException(getLocaleMessage(msgKey, args));
        }
    }

    /**
     * Adds a global message to the Faces context. Which will be displayed aside for 5 seconds by the p:growl component.
     * Use it to notify users about saved data, loaded results, etc. Use it for things, that isn't necessary to read.
     */
    protected void addGrowl(FacesMessage.Severity severity, String msgKey, Object... args) {
        Messages.add("growl", getFacesMessage(severity, msgKey, args));
    }

    // Helper ----------------------------------------------------------------------------------------------------------

    public String getSessionId() {
        if (null == sessionId) {
            sessionId = Faces.getSessionId();
            if (sessionId == null) {
                log.warn("Couldn't create session");
            }
        }
        return sessionId;
    }

    protected Learnweb getLearnweb() {
        if (null == learnweb) {
            learnweb = Learnweb.getInstance();
        }
        return learnweb;
    }

    protected DaoProvider dao() {
        return getLearnweb().getDaoProvider();
    }

    protected ConfigProvider config() {
        return getLearnweb().getConfigProvider();
    }
}

package de.l3s.learnweb.beans;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;

import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.app.DaoProvider;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.logging.ActivityEvent;
import de.l3s.learnweb.logging.EventDispatcher;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;
import de.l3s.util.StringHelper;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class ApplicationBean {

    @Inject
    private UserBean userBean;

    @Inject
    private EventDispatcher eventDispatcher;

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

    // Activity --------------------------------------------------------------------------------------------------------

    /**
     * Fires the activity event on behalf of the currently logged-in user.
     */
    protected void fireEvent(ActivityEvent event) {
        eventDispatcher.fire(event);
    }

    /**
     * Fires the activity event on behalf of the given user, e.g. when the user is not logged in yet.
     */
    protected void fireEvent(ActivityEvent event, User performer) {
        eventDispatcher.fire(event, performer);
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
        return MessagesBundle.format(getLocale(), msgKey, args);
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

    // Messaging -------------------------------------------------------------------------------------------------------

    /**
     * Messages are rendered as HTML (messages may contain markup), so string arguments, usually user input, are escaped here.
     */
    protected FacesMessage getFacesMessage(FacesMessage.Severity severity, String msgKey, Object... args) {
        Object[] escapedArgs = args == null ? null : Arrays.stream(args).map(arg -> arg instanceof CharSequence str ? StringHelper.escapeHtml(str.toString()) : arg).toArray();
        return new FacesMessage(severity, getLocaleMessage(msgKey, escapedArgs), null);
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
     *
     * @param args string arguments are HTML-escaped (see {@link #getFacesMessage}), don't pass markup or already escaped text
     */
    protected void addMessage(FacesMessage.Severity severity, String msgKey, Object... args) {
        Messages.add(null, getFacesMessage(severity, msgKey, args));
    }

    /**
     * Adds a global message to the Faces context. Which will be displayed aside for 5 seconds by the p:growl component.
     * Use it to notify users about saved data, loaded results, etc. Use it for things, that isn't necessary to read.
     *
     * @param args string arguments are HTML-escaped (see {@link #getFacesMessage}), don't pass markup or already escaped text
     */
    protected void addGrowl(FacesMessage.Severity severity, String msgKey, Object... args) {
        Messages.add("growl", getFacesMessage(severity, msgKey, args));
    }

    // Helper ----------------------------------------------------------------------------------------------------------

    protected Learnweb getLearnweb() {
        return Learnweb.getInstance();
    }

    protected DaoProvider dao() {
        return Learnweb.dao();
    }

    protected ConfigProvider config() {
        return Learnweb.config();
    }
}

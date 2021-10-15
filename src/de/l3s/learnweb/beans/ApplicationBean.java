package de.l3s.learnweb.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.model.SelectItem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.app.DaoProvider;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;
import de.l3s.util.Misc;
import de.l3s.util.bean.BeanHelper;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class ApplicationBean {
    private static final Logger log = LogManager.getLogger(ApplicationBean.class);

    private transient Learnweb learnweb;
    private transient String sessionId;
    private transient UserBean userBean;

    // User ------------------------------------------------------------------------------------------------------------

    /**
     * Returns the currently logged-in user.
     *
     * @return null if not logged in
     */
    protected User getUser() {
        // This value shall not be cached. The value would not be updated if the user logs out.
        return getUserBean().getUser();
    }

    /**
     * Returns the current locale.
     */
    protected Locale getLocale() {
        return getUserBean().getLocale();
    }

    /**
     * @return true if the user is logged in
     */
    protected boolean isLoggedIn() {
        return getUserBean().isLoggedIn();
    }

    public UserBean getUserBean() {
        if (null == userBean) {
            userBean = Beans.getInstance(UserBean.class);
        }
        return userBean;
    }

    // i18n ------------------------------------------------------------------------------------------------------------

    /**
     * Get a message from the messages bundle depending on the currently used local.
     * If the msgKey doesn't exist the msgKey itself will be returned.
     */
    public String getLocaleMessage(String msgKey, Object... args) {
        // get locale or load default
        Locale locale;
        try {
            locale = getLocale();
        } catch (Exception e) {
            locale = Locale.ENGLISH;
        }

        return LanguageBundle.getLocaleMessage(locale, msgKey, args);
    }

    /**
     * Converts a list of Locales to a list of SelectItems. The Locales are translated to the current frontend language
     */
    protected List<SelectItem> localesToSelectItems(List<Locale> locales) {
        ArrayList<SelectItem> selectItems = new ArrayList<>(locales.size());

        for (Locale locale : locales) {
            selectItems.add(new SelectItem(locale, getLocaleMessage("language_" + locale.getLanguage())));
        }
        selectItems.sort(Misc.SELECT_ITEM_LABEL_COMPARATOR);

        return selectItems;
    }

    // Preferences -----------------------------------------------------------------------------------------------------

    /**
     * Retrieves an object that was previously set by setPreference().
     * @return defaultValue if no corresponding value is found for the key.
     */
    public String getPreference(String key, String defaultValue) {
        String obj = getUserBean().getPreference(key);
        return obj == null ? defaultValue : obj;
    }

    /**
     * Stores an object in the session.
     */
    public void setPreference(String key, String value) {
        getUserBean().setPreference(key, value);
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
        dao().getLogDao().insert(user, action, groupId, targetId, params, getSessionId());
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
     * adds a global message to the jsf context. this will be displayed by the p:messages component
     */
    protected void addMessage(FacesMessage.Severity severity, String msgKey, Object... args) {
        Messages.add(null, getFacesMessage(severity, msgKey, args));

        if (FacesMessage.SEVERITY_FATAL == severity) {
            throw new BadRequestHttpException(getLocaleMessage(msgKey, args));
        }
    }

    /**
     * adds a global message to the jsf context. this will be displayed for a minute by the p:growl component.
     */
    protected void addGrowl(FacesMessage.Severity severity, String msgKey, Object... args) {
        Messages.add("growl", getFacesMessage(severity, msgKey, args));
    }

    /**
     * Shows a default fatal_error message.
     */
    protected void addErrorMessage(Throwable exception) {
        addErrorMessage(null, exception);
    }

    /**
     * @param desc A descriptive message that is shown to the user. If null a default fatal_error message will be shown
     */
    protected void addErrorMessage(String desc, Throwable exception) {
        log.error("{}; {}", desc != null ? desc : "Fatal lazy error", BeanHelper.getRequestSummary(), exception);

        throw new HttpException(desc, exception);
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

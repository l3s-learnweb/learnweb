package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.bean.BeanHelper;

/**
 * This class is only for developing purpose.
 * Replace ApplicationBean with this class if you want to run a bean outside FacesContext (in console for example)
 *
 * @author Philipp
 */
public class ApplicationDebuggingBean implements Serializable {
    private static final long serialVersionUID = 6714523666863887982L;
    private static final Logger log = LogManager.getLogger(ApplicationDebuggingBean.class);

    protected User user;

    public ApplicationDebuggingBean() {

    }

    public String getSessionId() {
        return "---";
    }

    protected FacesContext getFacesContext() {
        return null;
    }

    /**
     * returns the currently logged in user.
     *
     * @return null if not logged in
     */
    protected User getUser() {
        if (null == user) {
            log.warn("getUser() returns null of you do not manually set a user");
        }
        return user;
    }

    protected Learnweb getLearnweb() {
        return Learnweb.getInstance();
    }

    /**
     * get a message from the message property files depending on the currently used local.
     */
    protected String getLocaleMessage(String msgKey, Object... args) {
        ResourceBundle bundle = LanguageBundle.getLanguageBundle(Locale.US);

        String msg;
        try {
            msg = bundle.getString(msgKey);
            if (args != null) {
                MessageFormat format = new MessageFormat(msg);
                msg = format.format(args);
            }
        } catch (MissingResourceException e) {
            msg = msgKey;
        }
        return msg;
    }

    protected FacesMessage getFacesMessage(FacesMessage.Severity severity, String msgKey, Object... args) {
        return new FacesMessage(severity, getLocaleMessage(msgKey, args), null);
    }

    /**
     * adds a global message to the jsf context. this will be display by the p:messages tag
     */
    protected void addMessage(FacesMessage.Severity severity, String msgKey, Object... args) {
    }

    /**
     * adds a global message to the jsf context. this will be display for a minute by the p:growl tag
     */
    protected void addGrowl(FacesMessage.Severity severity, String msgKey, Object... args) {
    }

    /**
     * returns the currently used template directory. By default this is "lw/"
     */
    protected String getTemplateDir() {

        return "/lw";
    }

    /**
     * retrieves an object that was previously set by setPreference().
     */
    public String getPreference(String key) {
        return null;
    }

    /**
     * returns defaultValue if no corresponding value is found for the key.
     */
    public String getPreference(String key, String defaultValue) {
        return defaultValue;
    }

    /**
     * Stores an object in the session.
     */
    public void setPreference(String key, Object value) {

    }

    protected void addFatalMessage(Throwable exception) {
        addFatalMessage(null, exception);
    }

    protected void addFatalMessage(String desc, Throwable exception) {
        log.fatal((desc != null ? desc : "Fatal unhandled error") + "; " + BeanHelper.getRequestSummary(), exception);
    }

    public void addAccessDeniedMessage() {

    }
}

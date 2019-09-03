package de.l3s.learnweb.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;
import de.l3s.util.Misc;
import de.l3s.util.bean.BeanHelper;

public class ApplicationBean
{
    private transient Learnweb learnweb;
    private transient String sessionId;
    private transient UserBean userBean;

    public ApplicationBean()
    {
    }

    public String getSessionId()
    {
        if(null == sessionId)
        {
            HttpSession session;
            FacesContext facesContext = getFacesContext();
            if(facesContext == null)
                sessionId = null;
            else if((session = (HttpSession) facesContext.getExternalContext().getSession(true)) != null)
                sessionId = session.getId();

            if(sessionId == null)
                Logger.getLogger(ApplicationBean.class).warn("Couldn't create session");
        }
        return sessionId;
    }

    protected boolean isAjaxRequest()
    {
        if(null == FacesContext.getCurrentInstance())
            return false;

        return FacesContext.getCurrentInstance().isPostback();
    }

    protected static FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }

    /**
     * Returns the http get parameter or null if not found
     *
     * Should not be used in most cases. Use template f:viewParam and f:viewAction
     *
     * @param param
     * @return
     */
    public static String getParameter(String param)
    {
        String value = getFacesContext().getExternalContext().getRequestParameterMap().get(param);

        return value;
    }

    /**
     * Returns the http get parameter as int.
     * Is null if not found or couldn't be parsed
     *
     * Should not be used in most cases. Use template f:viewParam and f:viewAction
     *
     * @param param
     * @return
     */
    public static Integer getParameterInt(String param)
    {
        String value = getFacesContext().getExternalContext().getRequestParameterMap().get(param);

        if(null == value)
            return null;

        try
        {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * Returns the currently logged in user.
     *
     * @return null if not logged in
     */
    protected User getUser()
    {
        if(null == userBean)
            userBean = UtilBean.getUserBean();

        // This value should not be cached. The value would not be updated if the user logs out.
        return userBean.getUser();
    }

    protected Learnweb getLearnweb()
    {
        if(null == learnweb)
            learnweb = Learnweb.getInstance();
        return learnweb;
    }

    /**
     * get a message from the message property files depending on the currently used local
     *
     * @param msgKey
     * @param args
     * @return
     */
    public String getLocaleMessage(String msgKey, Object... args)
    {
        return UtilBean.getLocaleMessage(msgKey, args);
    }

    protected FacesMessage getFacesMessage(FacesMessage.Severity severity, String msgKey, Object... args)
    {
        return new FacesMessage(severity, getLocaleMessage(msgKey, args), null);
    }

    /**
     * Call this method if you want to keep messages during a post-redirect-get. <br/>
     * This value determines whether or not any FacesMessage instances queued in the current FacesContext must be preserved so they are accessible on
     * the next traversal of the lifecycle on this session, regardless of the request being a redirect after post, or a normal postback.
     */
    public void setKeepMessages()
    {
        getFacesContext().getExternalContext().getFlash().setKeepMessages(true);
    }

    /**
     * adds a global message to the jsf context. this will be displayed by the p:messages component
     *
     * @param severity
     * @param msgKey
     * @param args
     */
    protected void addMessage(FacesMessage.Severity severity, String msgKey, Object... args)
    {
        //Logger.getLogger(ApplicationBean.class).debug("JSF message: " + getLocaleMessage(msgKey, args));
        getFacesContext().addMessage("message", new FacesMessage(severity, getLocaleMessage(msgKey, args), null));
    }

    /**
     * adds a global message to the jsf context. this will be displayed for a minute by the p:growl component
     *
     * @param severity
     * @param msgKey
     * @param args
     */
    protected void addGrowl(FacesMessage.Severity severity, String msgKey, Object... args)
    {
        getFacesContext().addMessage(null, new FacesMessage(severity, getLocaleMessage(msgKey, args), null));
    }

    /**
     * returns the currently used template directory. By default this is "lw/"
     *
     * @return
     */
    protected static String getTemplateDir()
    {
        String path = getFacesContext().getExternalContext().getRequestServletPath();
        int index = path.indexOf("/", 1);

        if(index == -1)
            return "";
        return path.substring(0, index);
    }

    /**
     * retrieves an object that was previously set by setPreference()
     *
     * @param key
     * @return
     */
    public String getPreference(String key)
    {
        return UtilBean.getUserBean().getPreference(key);
    }

    /**
     * returns defaultValue if no corresponding value is found for the key
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public String getPreference(String key, String defaultValue)
    {
        String obj = getPreference(key);
        return obj == null ? defaultValue : obj;
    }

    /**
     * Stores an object in the session
     *
     * @param key
     * @param value
     */
    public void setPreference(String key, String value)
    {
        UtilBean.getUserBean().setPreference(key, value);
    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" and "params" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     */
    protected void log(Action action, int groupId, int targetId, int params)
    {
        log(action, groupId, targetId, Integer.toString(params), getUser());
    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" and "params" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     */
    public void log(Action action, int groupId, int targetId, String params)
    {
        log(action, groupId, targetId, params, getUser());
    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     */
    public void log(Action action, int groupId, int targetId)
    {
        log(action, groupId, targetId, null, getUser());
    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" and "params" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     */
    protected void log(Action action, int groupId, int targetId, String params, User user)
    {
        getLearnweb().getLogManager().log(user, action, groupId, targetId, params, getSessionId());
    }

    protected void log(Action action, Resource resource, int params)
    {
        log(action, resource.getGroupId(), resource.getId(), Integer.toString(params), getUser());
    }

    protected void log(Action action, Resource resource, String params)
    {
        log(action, resource.getGroupId(), resource.getId(), params, getUser());
    }

    protected void log(Action action, Resource resource)
    {
        log(action, resource.getGroupId(), resource.getId(), null, getUser());
    }

    protected void addErrorMessage(Throwable exception)
    {
        addErrorMessage(null, exception);
    }

    protected void addErrorMessage(String desc, Throwable exception)
    {
        addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");

        Logger.getLogger(ApplicationBean.class).error((desc != null ? desc : "Fatal unhandled error") + "; " + BeanHelper.getRequestSummary(), exception);
    }

    /**
     *
     * @param parameter invalid parameters
     */
    protected void addInvalidParameterMessage(String parameter)
    {
        addMessage(FacesMessage.SEVERITY_FATAL, "Invalid Parameter given. Check the URL you used.");

        Logger.getLogger(ApplicationBean.class).warn("Invalid parameter for " + parameter + "; " + BeanHelper.getRequestSummary(), new IllegalArgumentException());
    }

    protected void addAccessDeniedMessage()
    {
        addMessage(FacesMessage.SEVERITY_FATAL, "access_denied");

        Logger.getLogger(ApplicationBean.class).warn("access denied " + BeanHelper.getRequestSummary(), new IllegalArgumentException());
    }

    /**
     * Converts a list of Locales to a list of SelectItems. The Locales are translated to the current frontend language
     *
     * @param locales
     * @return
     */
    protected List<SelectItem> localesToSelectItems(List<Locale> locales)
    {
        ArrayList<SelectItem> selectItems = new ArrayList<>(locales.size());

        for(Locale locale : locales)
        {
            selectItems.add(new SelectItem(locale, getLocaleMessage("language_" + locale.getLanguage())));
        }
        selectItems.sort(Misc.selectItemLabelComparator);

        return selectItems;
    }

    /**
     * To overcome browser caching problems force revalidation
     */
    protected void forceRevalidation()
    {
        HttpServletResponse response = (HttpServletResponse) getFacesContext().getExternalContext().getResponse();

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
    }
}

package de.l3s.learnweb.beans;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.User;
import de.l3s.util.BeanHelper;

public class ApplicationBean
{
    private transient Learnweb learnweb;
    private transient String sessionId;
    private long startTime;

    public ApplicationBean()
    {
        startTime = System.currentTimeMillis();
    }

    public String getSessionId()
    {
        if(null == sessionId)
        {
            HttpSession session;
            FacesContext facesContect = getFacesContext();
            if(facesContect == null)
                sessionId = null;
            else if((session = (HttpSession) facesContect.getExternalContext().getSession(true)) != null)
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
     * Returns the http get paramater or null if not found
     *
     * @param param
     * @return
     */
    protected static String getParameter(String param)
    {
        String value = getFacesContext().getExternalContext().getRequestParameterMap().get(param);

        return value;
    }

    /**
     * Returns the http get parameter as int.
     * Is null if not found or couldn't be parsed
     *
     * @param param
     * @return
     */
    protected static Integer getParameterInt(String param)
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
        /*
         * This value should not be cached. The value would not be updated if the user logs out.
         */
        return UtilBean.getUserBean().getUser();
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
     *
     * @param action
     * @param targetId
     * @param params
     *
     *            protected void log(LogEntry.Action action, int targetId, String params)
     *            {
     *            int executionTime = (int) (System.currentTimeMillis() - startTime);
     *            getLearnweb().log(getUser(), action, targetId, params, getSessionId(), executionTime);
     *            }
     */
    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" and "params" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     *
     * @param action
     * @param groupId
     * @param targetId
     * @param params
     */
    protected void log(LogEntry.Action action, int groupId, int targetId, String params)
    {
        int executionTime = (int) (System.currentTimeMillis() - startTime);
        getLearnweb().log(getUser(), action, groupId, targetId, params, getSessionId(), executionTime);
    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     *
     * @param action
     * @param groupId
     * @param targetId
     */
    protected void log(LogEntry.Action action, int groupId, int targetId)
    {
        int executionTime = (int) (System.currentTimeMillis() - startTime);
        getLearnweb().log(getUser(), action, groupId, targetId, null, getSessionId(), executionTime);
    }

    protected void addFatalMessage(Throwable exception)
    {
        addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
        //addGrowl(FacesMessage.SEVERITY_FATAL, "fatal_error");

        Logger.getLogger(ApplicationBean.class).fatal("Fatal unhandled error on " + BeanHelper.getRequestSummary(), exception);
    }

    protected void addInvalidParameterMessage(String... parameter)
    {
        addMessage(FacesMessage.SEVERITY_FATAL, "Invalid Parameter given. Check the URL you used.");
        //addGrowl(FacesMessage.SEVERITY_FATAL, "fatal_error");

        Logger.getLogger(ApplicationBean.class).error("Invalid parameter for " + parameter + "; " + BeanHelper.getRequestSummary(), new IllegalArgumentException());
    }
}

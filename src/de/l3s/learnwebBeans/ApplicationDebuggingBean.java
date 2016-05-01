package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.User;

/**
 * 
 * This class is only for developing purpose.
 * Replace ApplicationBean with this class if you want to run a bean outside FacesContext (in console for example)
 * 
 * @author Philipp
 *
 */
public class ApplicationDebuggingBean implements Serializable
{

    private static final long serialVersionUID = 6714523666863887982L;

    private long startTime;

    public ApplicationDebuggingBean()
    {

	startTime = System.currentTimeMillis();
    }

    public String getSessionId()
    {

	return "---";
    }

    protected FacesContext getFacesContext()
    {
	return null;
    }

    /**
     * returns the currently logged in user.
     * 
     * @return null if not logged in
     */
    protected User getUser()
    {
	return null;
    }

    protected Learnweb getLearnweb()
    {

	return Learnweb.getInstance();

    }

    /**
     * get a message from the message property files depending on the currently used local
     * 
     * @param msgKey
     * @param args
     * @return
     */
    protected String getLocaleMessage(String msgKey, Object... args)
    {

	ResourceBundle bundle = ResourceBundle.getBundle("de.l3s.learnweb.lang.messages", Locale.US);

	String msg;
	try
	{
	    msg = bundle.getString(msgKey);
	    if(args != null)
	    {
		MessageFormat format = new MessageFormat(msg);
		msg = format.format(args);
	    }
	}
	catch(MissingResourceException e)
	{
	    msg = msgKey;
	}
	return msg;
    }

    protected FacesMessage getFacesMessage(FacesMessage.Severity severity, String msgKey, Object... args)
    {
	return new FacesMessage(severity, getLocaleMessage(msgKey, args), null);
    }

    /**
     * adds a global message to the jsf context. this will be display by the p:messages tag
     * 
     * @param severity
     * @param msgKey
     * @param args
     */
    protected void addMessage(FacesMessage.Severity severity, String msgKey, Object... args)
    {
    }

    /**
     * adds a global message to the jsf context. this will be display for a minute by the p:growl tag
     * 
     * @param severity
     * @param msgKey
     * @param args
     */
    protected void addGrowl(FacesMessage.Severity severity, String msgKey, Object... args)
    {
    }

    /**
     * returns the currently used template directory. By default this is "lw/"
     * 
     * @return
     */
    protected String getTemplateDir()
    {

	return "/lw";
    }

    /**
     * retrieves an object that was previously set by setPreference()
     * 
     * @param key
     * @return
     */
    public Object getPreference(String key)
    {
	return null;
    }

    /**
     * returns defaultValue if no correspondig value is found for the key
     * 
     * @param key
     * @param defaultValue
     * @return
     */
    public Object getPreference(String key, Object defaultValue)
    {
	Object obj = getPreference(key);
	return obj == null ? defaultValue : obj;
    }

    /**
     * Stores an object in the session
     * 
     * @param key
     * @param value
     */
    public void setPreference(String key, Object value)
    {

    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" and "params" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     * 
     * @param action one value of LogEntry.Action
     * @param targetId optional value; should be 0 if not required
     * @param params
     * @throws SQLException
     */
    protected void log(LogEntry.Action action, int targetId, String params)
    {
	int executionTime = (int) (System.currentTimeMillis() - startTime);
	getLearnweb().log(getUser(), action, targetId, params, getSessionId(), executionTime);
    }

    /**
     * Logs a user action for the currently active user.
     * The parameters "targetId" depend on the logged action.
     * Look at the code of LogEntry.Action for explanation.
     * 
     * @param action one value of LogEntry.Action
     * @param targetId optional value; should be 0 if not required
     * @throws SQLException
     */
    protected void log(LogEntry.Action action, int targetId)
    {
	int executionTime = (int) (System.currentTimeMillis() - startTime);
	getLearnweb().log(getUser(), action, targetId, null, getSessionId(), executionTime);
    }

}

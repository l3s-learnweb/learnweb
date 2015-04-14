package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

@ManagedBean
@SessionScoped
public class UserBean implements Serializable
{
    private final static long serialVersionUID = -8577036953815676943L;
    private final static Logger log = Logger.getLogger(UserBean.class);

    private User user = null;
    private Locale locale;
    private HashMap<String, String> preferences; // user preferences like search mode

    private Course activeCourse;
    private List<Group> newGroups = null;

    public UserBean()
    {
	locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();

	preferences = new HashMap<String, String>();
    }

    public boolean isLoggedIn()
    {
	return user != null;
    }

    public boolean isLoggedInInterweb()
    {
	if(!isLoggedIn())
	    return false;

	return user.isLoggedInInterweb();
    }

    /**
     * The currently logged in user
     * 
     * @return
     */
    public User getUser()
    {
	return user;
    }

    /**
     * Use this function to log in a user.
     * 
     * @param user
     */
    public void setUser(User user)
    {

	// store the user also in the session so that it is accessible in the download servlet
	HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
	session.setAttribute("learnweb_user", user);

	if(user != null)
	{
	    try
	    {
		activeCourse = user.getCourses().get(0);
	    }
	    catch(SQLException e)
	    {
		e.printStackTrace();
	    }

	    preferences = user.getPreferences();
	}
	else
	// user logged out -> clear caches
	{
	    newGroups = null;
	    onDestroy();
	}

	this.user = user;
    }

    @PreDestroy
    public void onDestroy()
    {
	if(null != user)
	{
	    user.setPreferences(preferences);
	    user.onDestroy();
	}
    }

    public String getPreference(String key)
    {
	//System.out.println("get " + key + " a: " + preferences.get(key));
	return preferences.get(key);
    }

    public void setPreference(String key, String value)
    {
	//System.out.println("set " + key + " a: " + value);
	preferences.put(key, value);
    }

    public void setPreferenceRemote()
    {
	String key = getParameter("key");
	String value = getParameter("value");

	setPreference(key, value);
    }

    public Locale getLocale()
    {
	return locale;
    }

    /**
     * 
     * @return example "de_DE"
     */
    public String getLocaleAsString()
    {
	return locale.toString();
    }

    /**
     * example "de"
     * 
     * @return
     */
    public String getLocaleCode()
    {
	return locale.getLanguage();
    }

    public String setLocaleCode(String localeCode)
    {
	switch(localeCode)
	{
	case "de":
	    locale = Locale.GERMANY;
	    break;
	case "en":
	    locale = new Locale("en", "gb");
	    break;
	case "it":
	    locale = Locale.ITALY;
	    break;
	case "pt":
	    locale = new Locale("pt", "br");
	    break;
	default:
	    locale = Locale.ENGLISH;
	    log.error("Unsupported language: " + localeCode);
	}

	FacesContext facesContext = FacesContext.getCurrentInstance();
	facesContext.getViewRoot().setLocale(locale);

	String viewId = facesContext.getViewRoot().getViewId();
	return viewId + "?faces-redirect=true&includeViewParams=true";
    }

    public boolean isAdmin()
    {
	if(null == user)
	    return false;

	return user.isAdmin();
    }

    public boolean isModerator()
    {
	if(null == user)
	    return false;

	return user.isModerator();
    }

    public TimeZone getTimeZone()
    {
	if(user == null)
	    return TimeZone.getTimeZone("Europe/Berlin");

	return user.getTimeZone();
    }

    public Course getActiveCourse()
    {
	return activeCourse;
    }

    public void setActiveCourse(Course activeCourse)
    {
	this.activeCourse = activeCourse;
    }

    /**
     * Returns a list of groups that have been created since the last login of this user
     * 
     * @return
     * @throws SQLException
     */
    public List<Group> getNewGroups() throws SQLException
    {
	if(null == newGroups)
	{
	    newGroups = Learnweb.getInstance().getGroupManager().getGroupsByCourseId(activeCourse.getId(), user.getLastLoginDate());
	}

	return newGroups;
    }

    public int getNewGroupsCount()
    {
	try
	{
	    return getNewGroups().size();
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}

	return 0;
    }

    public boolean isSearchHistoryEnabled()
    {
	if(activeCourse == null)
	    return false;

	return activeCourse.getOption(Course.Option.Search_History_log_enabled);
    }

    @Override
    public String toString()
    {
	if(user == null)
	    return "not logged in";

	return "userId: " + user.getId() + " name: " + user.getUsername();
    }

    private String getParameter(String param)
    {
	return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(param);
    }

    //Function to format Date variables in the UI
    public String formatDate(String pattern, Date date)
    {
	return new SimpleDateFormat(pattern, locale).format(date);
    }
}

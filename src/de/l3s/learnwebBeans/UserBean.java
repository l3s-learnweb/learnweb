package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
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

    private int userId = 0;
    private transient User userCache = null; // to avoid inconsistencies with the user cache the UserBean does not store the user itself
    private long userCacheTime = 0L; // stores when the userCache was refreshed the last time

    private Locale locale;
    private HashMap<String, String> preferences; // user preferences like search mode

    private int activeCourseId = 0;
    private transient Course activeCourseCache = null;
    private long activeCourseCacheTime = 0L;

    private List<Group> newGroups = null;

    public UserBean()
    {
	locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();

	preferences = new HashMap<String, String>();
    }

    public boolean isLoggedIn()
    {
	//return user != null;
	return userId != 0;
    }

    public boolean isLoggedInInterweb()
    {
	if(!isLoggedIn())
	    return false;

	return getUser().isLoggedInInterweb();
    }

    /**
     * The currently logged in user
     * 
     * @return
     */
    public User getUser()
    {
	if(userId == 0)
	    return null;

	if(userCache == null || userCacheTime + 60000L < System.currentTimeMillis())
	{
	    log.debug("Load user: " + userId);
	    try
	    {
		userCache = Learnweb.getInstance().getUserManager().getUser(userId);
		userCacheTime = System.currentTimeMillis();
	    }
	    catch(SQLException e)
	    {
		log.fatal("Can't retrieve user", e);
	    }
	}
	return userCache;
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
		if(user.getId() == 2969) // paviamod set to dentists2015
		    activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(884);
		else
		{
		    getPreference("active_course");
		    activeCourseCache = user.getCourses().get(0);

		}

		activeCourseId = activeCourseCache.getId();
	    }
	    catch(SQLException e)
	    {
		e.printStackTrace();
	    }

	    preferences = user.getPreferences();
	    userId = user.getId();
	}
	else
	// user logged out -> clear caches
	{
	    newGroups = null;
	    userId = 0;
	    userCache = null;
	    activeCourseId = 0;
	    activeCourseCache = null;
	    onDestroy();
	}

	//this.user = user;

    }

    @PreDestroy
    public void onDestroy()
    {
	User user = getUser();
	if(null != user)
	{
	    user.setPreferences(preferences);
	    user.onDestroy();
	}
    }

    public String getPreference(String key)
    {
	return preferences.get(key);
    }

    public void setPreference(String key, String value)
    {
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
	if(localeCode.equals("de"))
	    locale = Locale.GERMANY;
	else if(localeCode.equals("en"))
	    locale = new Locale("en", "gb");
	else if(localeCode.equals("it"))
	    locale = Locale.ITALY;
	else if(localeCode.equals("pt"))
	    locale = new Locale("pt", "br");
	else
	{
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
	User user = getUser();
	if(null == user)
	    return false;

	return user.isAdmin();
    }

    public boolean isModerator()
    {
	User user = getUser();
	if(null == user)
	    return false;

	return user.isModerator();
    }

    public TimeZone getTimeZone()
    {
	User user = getUser();
	if(user == null)
	    return TimeZone.getTimeZone("Europe/Berlin");

	return user.getTimeZone();
    }

    public Course getActiveCourse()
    {
	if(activeCourseCacheTime + 60000 < System.currentTimeMillis() || activeCourseCache == null)
	{
	    this.activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(activeCourseId);
	    this.activeCourseCacheTime = System.currentTimeMillis();

	    log.debug("Load course for user: " + userCache.getUsername() + " - " + activeCourseId + " - " + activeCourseCache);
	}
	return activeCourseCache;
    }

    public void setActiveCourse(Course activeCourse)
    {
	this.activeCourseCache = activeCourse;
	this.activeCourseId = activeCourse.getId();
	this.activeCourseCacheTime = System.currentTimeMillis();

	setPreference("active_course", Integer.toString(activeCourseId));
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
	    newGroups = Learnweb.getInstance().getGroupManager().getGroupsByCourseId(activeCourseId, getUser().getLastLoginDate());
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
	if(!isLoggedIn())
	    return false;

	return getActiveCourse().getOption(Course.Option.Search_History_log_enabled);
    }

    @Override
    public String toString()
    {
	User user = getUser();
	if(user == null)
	    return "not logged in";

	return "userId: " + user.getId() + " name: " + user.getUsername();
    }

    private String getParameter(String param)
    {
	return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(param);
    }

    //Function to format Date variables in the UI
    public String formatDate(Date date)
    {
	return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, getLocale()).format(date);
    }
}

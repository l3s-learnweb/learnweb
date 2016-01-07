package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.ocpsoft.prettytime.PrettyTime;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSubMenu;

import com.sun.jersey.api.client.ClientHandlerException;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.searchlogclient.SearchLogClient;

@ManagedBean
@SessionScoped
public class UserBean implements Serializable
{
    private final static long serialVersionUID = -8577036953815676943L;
    private final static Logger log = Logger.getLogger(UserBean.class);

    private int userId = 0;
    private transient User userCache = null; // to avoid inconsistencies with the user cache the UserBean does not store the user itself
    private transient long userCacheTime = 0L; // stores when the userCache was refreshed the last time

    private Locale locale;
    private transient PrettyTime localePrettyTime;
    private HashMap<String, String> preferences; // user preferences like search mode

    private int activeCourseId = 0;
    private transient Course activeCourseCache = null;
    private transient long activeCourseCacheTime = 0L;

    private transient List<Group> newGroups = null;

    private boolean cacheShowMessageJoinGroup = true;
    private boolean cacheShowMessageAddResource = true;

    public UserBean()
    {
	locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();

	preferences = new HashMap<String, String>();

	storeMetadataInSession(null);
    }

    /**
     * This methods sets values which are required by the Download Servlet
     * ands provides data which is shown on the Tomcat mananger session page
     * and
     */
    private void storeMetadataInSession(User user)
    {
	ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
	HttpServletRequest request = (HttpServletRequest) context.getRequest();

	// get ip
	String ipAddress = request.getHeader("X-FORWARDED-FOR");
	if(ipAddress == null)
	{
	    ipAddress = request.getRemoteAddr();
	}

	String userAgent = request.getHeader("User-Agent");
	String userName = user == null ? "logged_out" : user.getUsername();

	String info = userName + " | " + ipAddress + " | " + userAgent;

	// store the user also in the session so that it is accessible by DownloadServlet and TomcatManager
	HttpSession session = (HttpSession) context.getSession(true);
	session.setAttribute("userName", info); // set only to display it in Tomcat manager app
	session.setAttribute("Locale", locale); // set only to display it in Tomcat manager app	
	session.setAttribute("learnweb_user_id", new Integer(user == null ? 0 : user.getId())); // required by DownloadServlet
    }

    public boolean isLoggedIn()
    {
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
		log.fatal("Can't retrieve user " + userId, e);
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
	//clear caches
	newGroups = null;
	userId = 0;
	userCache = null;
	activeCourseId = 0;
	activeCourseCache = null;
	cacheShowMessageJoinGroup = true;
	cacheShowMessageAddResource = true;

	if(user != null)
	{
	    preferences = user.getPreferences();
	    userId = user.getId();

	    storeMetadataInSession(user);

	    try
	    {
		if(user.getId() == 2969) // paviamod set to dentists2015 // TODO this is only a quick fix
		    activeCourseId = 884; // activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(884);
		else if(user.getId() == 5143) // yell set to yell // TODO this is only a quick fix
		    activeCourseId = 505; //activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(505);
		else
		{
		    String lastActiveCourse = getPreference("active_course");
		    if(lastActiveCourse != null)
		    {
			activeCourseId = Integer.parseInt(lastActiveCourse);
			log.debug("load course from preferences");
		    }
		    else
		    {
			activeCourseId = user.getCourses().get(0).getId();
			log.debug("use public course");
		    }
		}
	    }
	    catch(SQLException e)
	    {
		log.error("Couldn't login user " + user.getId(), e);
	    }

	}
	else
	{
	    // user logged out
	    onDestroy();
	}
    }

    @PreDestroy
    public void onDestroy()
    {
	// persist user preferences in database
	User user = getUser();
	if(null != user)
	{
	    user.setPreferences(preferences);
	    user.onDestroy();
	}

	SearchLogClient searchLogClient = Learnweb.getInstance().getSearchlogClient();
	try
	{
	    searchLogClient.pushBatchResultsetList();
	    searchLogClient.postResourceLog();
	    searchLogClient.passUpdateResultset();
	    searchLogClient.pushTagList();
	}
	catch(ClientHandlerException e)
	{
	    log.warn("Search Tracker service is down");
	}
	catch(RuntimeException e)
	{
	    log.error(e.getMessage());
	}

	log.debug("Session Destroyed;");
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
	String key = ApplicationBean.getParameter("key");
	String value = ApplicationBean.getParameter("value");

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
	    locale = new Locale("en", "GB");
	else if(localeCode.equals("it"))
	    locale = Locale.ITALY;
	else if(localeCode.equals("pt"))
	    locale = new Locale("pt", "BR");
	else if(localeCode.equals("xx")) // only for translation editors
	    locale = new Locale("xx", "xx");
	else
	{
	    locale = Locale.ENGLISH;
	    log.error("Unsupported language: " + localeCode);
	}

	localePrettyTime = null; // reset date formatter 

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
	if(activeCourseCacheTime + 6000000 < System.currentTimeMillis() || activeCourseCache == null)
	{
	    if(activeCourseId == 0)
	    {
		activeCourseId = 485; // set to public course
	    }
	    this.activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(activeCourseId);
	    this.activeCourseCacheTime = System.currentTimeMillis();
	}
	return activeCourseCache;
    }

    public void setActiveCourseId(int activeCourseId)
    {
	this.activeCourseCache = null;
	this.activeCourseId = activeCourseId;

	setPreference("active_course", Integer.toString(activeCourseId));
    }

    public int getActiveCourseId()
    {
	return activeCourseId;
    }

    @Override
    public String toString()
    {
	User user = getUser();
	if(user == null)
	    return "not logged in";

	return "userId: " + user.getId() + " name: " + user.getUsername();
    }

    // -------------------- Frontend ---------------------------

    /**
     * Function to format Date variables in the UI depending on the users locale
     * 
     * @param date
     * @return
     */
    public String formatDate(Date date)
    {
	if(date != null)
	{
	    long timeDifference = (new Date().getTime() - date.getTime());
	    if(timeDifference > 300000)
		return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, getLocale()).format(date);
	    else
		return UtilBean.getLocaleMessage("a_few_minutes_ago");
	}
	else
	    return "";
    }

    /**
     * Returns the css code for the banner image of the active course or an empty string if no image is defined
     * 
     * @return
     * @throws SQLException
     */
    public String getBannerImage() throws SQLException
    {
	Course selectCourse = getActiveCourse();
	if(selectCourse != null && selectCourse.getBannerImage() != null)
	    return "background-image: url(" + selectCourse.getBannerImage() + ");";

	return "";
    }

    /**
     * Returns the css banner color of the active course or an empty string if no color is defined
     * 
     * @return
     */
    public String getBannerColor()
    {
	String bannerColor = "#489a83";
	Course selectCourse = getActiveCourse();

	if(selectCourse != null && selectCourse.getBannerColor() != null && selectCourse.getBannerColor().length() > 3)
	    bannerColor = "#" + selectCourse.getBannerColor();

	return bannerColor;
    }

    public String getBannerLink() throws SQLException
    {
	if(getUser() == null)
	    return "";

	return getUser().getOrganisation().getWelcomePage();
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
	    log.error(e);
	}

	return 0;
    }

    public boolean isSearchHistoryEnabled()
    {
	if(!isLoggedIn())
	    return false;

	return getActiveCourse().getOption(Course.Option.Search_History_log_enabled);
    }

    public boolean isGoogleDocsSignInEnabled()
    {
	if(!isLoggedIn())
	    return false;

	return getActiveCourse().getOption(Course.Option.Course_Google_Docs_Sign_In_enabled);
    }

    /**
     * Model for the group menu
     * 
     * @return
     */
    public LinkedList<DefaultSubMenu> getGroupMenu()
    {
	String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();

	Integer groupId = ApplicationBean.getParameterInt("group_id");

	LinkedList<DefaultSubMenu> menu = new LinkedList<DefaultSubMenu>();
	try
	{
	    for(Group group : getUser().getGroups())// getActiveCourse().getGroupsFilteredByUser(getUser()))
	    {
		boolean isActiveGroup = false;

		DefaultSubMenu submenu = new DefaultSubMenu();
		submenu.setLabel(group.getLongTitle());
		submenu.setId(Integer.toString(group.getId()));

		if(groupId != null && groupId.equals(group.getId()))
		{
		    submenu.setStyleClass("active");
		    isActiveGroup = true;
		}

		DefaultMenuItem item = new DefaultMenuItem();
		item.setValue(UtilBean.getLocaleMessage("overview"));
		item.setUrl("./group/overview.jsf?group_id=" + group.getId());
		if(isActiveGroup && viewId.endsWith("overview.xhtml"))
		    item.setStyleClass("active");
		submenu.addElement(item);

		item = new DefaultMenuItem();
		item.setValue(UtilBean.getLocaleMessage("resources"));
		item.setUrl("./group/resources.jsf?group_id=" + group.getId());
		if(isActiveGroup && viewId.endsWith("resources.xhtml"))
		    item.setStyleClass("active");
		submenu.addElement(item);

		item = new DefaultMenuItem();
		item.setValue(UtilBean.getLocaleMessage("members"));
		item.setUrl("./group/members.jsf?group_id=" + group.getId());
		if(isActiveGroup && viewId.endsWith("members.xhtml"))
		    item.setStyleClass("active");
		submenu.addElement(item);

		item = new DefaultMenuItem();
		item.setValue(UtilBean.getLocaleMessage("presentations"));
		item.setUrl("./group/presentations.jsf?group_id=" + group.getId());
		if(isActiveGroup && viewId.endsWith("presentations.xhtml"))
		    item.setStyleClass("active");
		submenu.addElement(item);

		item = new DefaultMenuItem();
		item.setValue(UtilBean.getLocaleMessage("links"));
		item.setUrl("./group/links.jsf?group_id=" + group.getId());
		if(isActiveGroup && viewId.endsWith("links.xhtml"))
		    item.setStyleClass("active");
		submenu.addElement(item);

		item = new DefaultMenuItem();
		item.setValue(UtilBean.getLocaleMessage("forum"));
		item.setUrl("./group/forum.jsf?group_id=" + group.getId());
		if(isActiveGroup && (viewId.endsWith("forum.xhtml") || viewId.endsWith("forum_post.xhtml")))
		    item.setStyleClass("active");
		submenu.addElement(item);

		if(isModerator() || isAdmin() || group.isLeader(getUser()))
		{
		    item = new DefaultMenuItem();
		    item.setValue(UtilBean.getLocaleMessage("options"));
		    item.setUrl("./group/options.jsf?group_id=" + group.getId());
		    if(isActiveGroup && viewId.endsWith("options.xhtml"))
			item.setStyleClass("active");
		    submenu.addElement(item);
		}
		menu.add(submenu);
	    }

	}
	catch(SQLException e)
	{
	    log.error("Can't create menu model", e);
	}

	return menu;
    }

    public LinkedList<DefaultSubMenu> getGroupFoldersAsMenu()
    {
	Integer groupId = ApplicationBean.getParameterInt("group_id");
	Integer folderId = ApplicationBean.getParameterInt("folder_id");

	LinkedList<DefaultSubMenu> menu = new LinkedList<DefaultSubMenu>();
	try
	{
	    for(Group group : getUser().getGroups())// getActiveCourse().getGroupsFilteredByUser(getUser()))
	    {
		boolean isActiveGroup = false;

		DefaultSubMenu submenu = new DefaultSubMenu();
		submenu.setLabel(group.getLongTitle());
		submenu.setId(Integer.toString(group.getId()));

		if(groupId != null && groupId.equals(group.getId()))
		{
		    submenu.setStyleClass("active");
		    isActiveGroup = true;
		}

		for(Folder folder : group.getFolders())
		{
		    DefaultMenuItem item = new DefaultMenuItem();
		    item.setValue(folder.getPrettyPath());
		    item.setUrl("./group/resources.jsf?group_id=" + group.getId() + "&folder_id=" + folder.getFolderId());
		    if(isActiveGroup && folderId != null && folderId.equals(folder.getFolderId()))
			item.setStyleClass("active");
		    submenu.addElement(item);
		}

		menu.add(submenu);
	    }

	}
	catch(SQLException e)
	{
	    log.error("Can't create menu model", e);
	}

	return menu;
    }

    public String onCourseChange(int courseId) throws SQLException
    {
	setActiveCourseId(courseId);

	return ApplicationBean.getTemplateDir() + "/" + getUser().getOrganisation().getWelcomePage() + "?faces-redirect=true";
    }

    public DefaultMenuModel getCourseMenuModel() throws SQLException
    {
	DefaultMenuModel model = new DefaultMenuModel();

	//First submenu
	DefaultSubMenu firstSubmenu = new DefaultSubMenu("Active course: " + getActiveCourse().getTitle());

	for(Course course : getUser().getCourses())
	{
	    DefaultMenuItem item = new DefaultMenuItem(course.getTitle());
	    item.setCommand("#{userBean.onCourseChange(" + course.getId() + ")}");
	    item.setAjax(false);

	    firstSubmenu.addElement(item);
	}

	model.addElement(firstSubmenu);

	return model;
    }

    /**
     * Returns true when there is any tooltip message to show
     * 
     * @return
     * @throws SQLException
     */
    public boolean isShowMessageAny() throws SQLException
    {
	if(!isLoggedIn())
	    return false;

	return isShowMessageJoinGroup() || isShowMessageAddResource();
    }

    public boolean isShowMessageJoinGroup() throws SQLException
    {
	if(cacheShowMessageJoinGroup) // check until the user has joined a group 
	{
	    User user = getUser();
	    if(null == user)
		return false;
	    cacheShowMessageJoinGroup = getUser().getGroupCount() == 0;
	}
	return cacheShowMessageJoinGroup;
    }

    public boolean isShowMessageJoinGroupInHeader() throws SQLException
    {
	String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
	if(viewId.contains("groups.xhtml"))
	    return false;

	return isShowMessageJoinGroup();
    }

    public boolean isShowMessageAddResourceInHeader() throws SQLException
    {
	String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
	if(viewId.contains("overview.xhtml") || viewId.contains("resources.xhtml") || viewId.contains("activity.xhtml"))
	    return false;

	return isShowMessageAddResource();
    }

    public boolean isShowMessageAddResource() throws SQLException
    {
	if(cacheShowMessageAddResource) // check until the user has added a resource
	{
	    if(isShowMessageJoinGroup())
		return false;

	    User user = getUser();
	    if(null == user)
		return false;

	    cacheShowMessageAddResource = getUser().getResourceCount() == 0;
	}
	return cacheShowMessageAddResource;
    }

    public String getPrettyDate(Date date)
    {
	//return StringHelper.getPrettyDate(date, locale);
	if(localePrettyTime == null)
	    localePrettyTime = new PrettyTime(locale);

	return localePrettyTime.format(date);
    }
}

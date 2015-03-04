package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.PreDestroy;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpSession;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

@ManagedBean
@SessionScoped
public class UserBean implements Serializable
{

    private static final long serialVersionUID = -8577036953815676943L;
    private User user = null;
    private Locale locale;
    private HashMap<String, Object> preferences; // user preferences like search mode
    private int screenWidth;
    private int screenHeight;
    private Course activeCourse;
    private List<Group> newGroups = null;

    public UserBean()
    {
	locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
	preferences = new HashMap<String, Object>();
    }

    public void preRenderView(ComponentSystemEvent event)
    {
	FacesContext fc = FacesContext.getCurrentInstance();
	if(isLoggedIn())
	{
	    ConfigurableNavigationHandler nav = (ConfigurableNavigationHandler) fc.getApplication().getNavigationHandler();
	    nav.performNavigation("/lw/myhome/activity");
	}
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
	this.user = user;

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
	}
	else
	// user logged out -> clear caches
	{
	    newGroups = null;
	}
    }

    @PreDestroy
    public void onDestroy()
    {
	if(null != user)
	    user.onDestroy();
    }

    public Object getPreference(String key)
    {
	return preferences.get(key);
    }

    public void setPreference(String key, Object value)
    {
	preferences.put(key, value);
    }

    public int getScreenWidth()
    {
	return screenWidth;
    }

    public void setScreenWidth(int screenWidth)
    {
	this.screenWidth = screenWidth;
    }

    public int getScreenHeight()
    {
	return screenHeight;
    }

    public void setScreenHeight(int screenHeight)
    {
	this.screenHeight = screenHeight;
    }

    private static Map<String, Object> countries;
    static
    {
	countries = new LinkedHashMap<String, Object>();
	countries.put("Deutsch", Locale.GERMANY);
	countries.put("English", Locale.US); //label, value

    }

    public Map<String, Object> getCountriesInMap()
    {
	return countries;
    }

    // value change event listener
    public void countryLocaleCodeChanged(ValueChangeEvent e)
    {

	String newLocaleValue = e.getNewValue().toString();
	//System.out.println("newLocaleValue "+newLocaleValue);
	// loop country map to compare the locale code
	for(Map.Entry<String, Object> entry : countries.entrySet())
	{
	    //System.out.println("value "+entry.getValue().toString());
	    if(entry.getValue().toString().equals(newLocaleValue))
	    {

		FacesContext.getCurrentInstance().getViewRoot().setLocale((Locale) entry.getValue());
		setLocale((Locale) entry.getValue());
	    }
	}

	/*
	String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();		
	return viewId +"?faces-redirect=true&includeViewParams=true";*/
    }

    public void setLocale(Locale locale)
    {
	this.locale = locale;
	if(isLoggedIn())
	    user.setLocale(locale);
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

    public void setLocaleAsString(String foo)
    {
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

    public void setLocaleCode(String lo)
    {
	System.out.println("set locale" + lo);

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

    /*
    private static MenuItem localItem(String title, String url)
    {
    	return item(UtilBean.getLocaleMessage(title), url);
    }
    
    private static MenuItem item(String title, String url)
    {
    	MenuItem item = new MenuItem();
    	item.setValue(title);
    	item.setUrl(url);
    	return item;
    }
    
    private static Submenu localSubMenu(String title, String url)	
    {
    	return subMenu(UtilBean.getLocaleMessage(title), url);
    }
    
    
    private static Submenu subMenu(String title, String url)
    {
    	Submenu submenu = new Submenu();
    	submenu.setLabel(title);
    	// TODO set url
    	return submenu;
    }
    
    
    public MenuModel getMenu() throws SQLException 
    {
    	long millios = System.currentTimeMillis();
    	System.out.println("getMenu()");
    	
    	DefaultMenuModel model = new DefaultMenuModel();
    	Submenu groups = localSubMenu("myGroups", "myhome/groups");
    	
    	String url = UtilBean.getLearnwebBean().getBaseUrl() +"group/overview.jsf?group_id=";
    	if(isLoggedIn())
    	{
    		boolean first=true;
    		for(Course course : user.getCourses())
    		{
    			//Submenu submenu = new Submenu();
    			/*
    			submenu.setLabel(course.getTitle());
    			
    			if(course.getOption(Option.Course_Forum_enabled))
    			{
    				MenuItem item = new MenuItem();
    				item.setValue(Util.getLocaleMessage("forum"));
    				item.setUrl(course.getForumUrl(user));
    				item.setTarget("_blank");
    				submenu.getChildren().add(item);
    			}* /
    			
    			for(Group group : course.getGroupsFilteredByUser(user))
    			{
    				MenuItem item = new MenuItem();
    				item.setValue(group.getTitle());
    				item.setUrl(url + group.getId());
    				groups.getChildren().add(item);
    			}
    			
    			//groups.addSubmenu(submenu);
    		}
    		model.addSubmenu(groups);
    	}
    	else
    		model.addMenuItem(localItem("myGroups", "myhome/groups.jsf"));
    		
    	Submenu resources = localSubMenu("myResourcesTitle", "myhome/resources_grid.jsf");
    	resources.getChildren().add(localItem("addResource", "myhome/add_resource.jsf"));
    	resources.getChildren().add(localItem("myCommentsTitle", "myhome/comments.jsf"));
    	resources.getChildren().add(localItem("myRatedResourcesTitle", "myhome/rated_resources.jsf"));
    	resources.getChildren().add(localItem("myTags", "myhome/tags.jsf"));
    	resources.getChildren().add(localItem("myCommentsTitle", "myhome/comments.jsf"));
    	model.addSubmenu(resources);
    	/*
    	<p:menuitem value="#{msg.myRatedResourcesTitle}" url="myhome/rated_resources.jsf" />
    	<p:menuitem value="#{msg.myTags}" url="myhome/tags.jsf" />	
    	<p:menuitem value="Activity Stream" url="myhome/activity.jsf" />	
    	<p:menuitem value="#{msg.addResource}" url="myhome/add_resource.jsf" />	
    	<p:menuitem value="#{msg.myNotifications}#{notificationReaderBean.howManyNewMessages}" url="myhome/notification.jsf" />
    		<p:menuitem value="Forum" url="#{userBean.user.courses[0].getForumUrl(userBean.user)}" target="_blank" rendered="#{(userBean.user.courses.size() == 1 and not empty userBean.user.courses[0].getForumUrl(userBean.user))}"/>	
    * /
    		
    		
    	System.out.println(System.currentTimeMillis() - millios);
    	return model;
    }
    */

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
	int userId = user == null ? 0 : user.getId();

	return "userId: " + userId;
    }
}

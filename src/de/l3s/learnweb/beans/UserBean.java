package de.l3s.learnweb.beans;

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
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.menu.DefaultSubMenu;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.GroupManager;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Organisation.Option;
import de.l3s.learnweb.User;
import de.l3s.util.BeanHelper;
import de.l3s.util.StringHelper;

@ManagedBean
@SessionScoped
public class UserBean implements Serializable
{
    private final static long serialVersionUID = -8577036953815676943L;
    private final static Logger log = Logger.getLogger(UserBean.class);

    private int userId = 0;
    private transient User userCache = null; // to avoid inconsistencies with the user cache the UserBean does not store the user itself
    private transient long userCacheTime = 0L; // stores when the userCache was refreshed the last time
    private transient User moderatorUser; // in this field we store a moderator account while the moderator is logged in in an other account

    private Locale locale;
    private transient PrettyTime localePrettyTime;

    private int activeCourseId = 0;
    /*
    private transient Course activeCourseCache = null;
    private transient long activeCourseCacheTime = 0L;
    */
    private transient List<Group> newGroups = null;

    private boolean cacheShowMessageJoinGroup = true;
    private boolean cacheShowMessageAddResource = true;

    private long groupsTreeCacheTime = 0L;
    private DefaultTreeNode groupsTree;
    private HashMap<String, String> anonymousPreferences = new HashMap<String, String>(); // preferences for users who are not logged in

    private String domain = "learnweb"; // variable is used to change the logo

    public UserBean()
    {
        locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();

        storeMetadataInSession(null);

        HttpServletRequest httpRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        if(FrontpageServlet.isArchiveWebRequest(httpRequest))
        {
            domain = "archiveweb";
            //setActiveCourseId(891); // enabled archive course when archiveweb.l3s.uni-hannover.de is used
        }
    }

    /**
     * This methods sets values which are required by the Download Servlet
     * and provides data which is shown on the Tomcat mananger session page
     */
    private void storeMetadataInSession(User user)
    {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) context.getRequest();

        String ipAddress = BeanHelper.getIp(request);
        String userAgent = request.getHeader("User-Agent");
        String userName = user == null ? "logged_out" : user.getRealUsername();

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
        cacheShowMessageJoinGroup = true;
        cacheShowMessageAddResource = true;

        if(user != null)
        {
            userId = user.getId();

            storeMetadataInSession(user);

            if(user.getId() == 2969) // paviamod set to dentists2015 // TODO this is only a quick fix
                activeCourseId = 884; // activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(884);
            else if(user.getId() == 5143) // yell set to yell // TODO this is only a quick fix
                activeCourseId = 505; //activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(505);
            else if(user.getId() == 8963) // LAbInt set active course to LabInt 2016
                activeCourseId = 1225; //activeCourseCache = Learnweb.getInstance().getCourseManager().getCourseById(505);
            else if(user.getOrganisationId() == 1249) // eu made 4 all
                activeCourseId = 1250;
            else
            {

                activeCourseId = user.getActiveCourseId();
            }

            user.setActiveCourseId(activeCourseId);

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
            user.onDestroy();
        }

        //log.debug("Session Destroyed;");
    }

    public String getPreference(String key)
    {
        if(isLoggedIn())
            return getUser().getPreferences().get(key);

        return anonymousPreferences.get(key);
    }

    public void setPreference(String key, String value)
    {
        HashMap<String, String> preferences = isLoggedIn() ? getUser().getPreferences() : anonymousPreferences;

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

    public boolean isMemberOfCourse(int courseId)
    {
        User user = getUser();

        if(user == null)
            return false;

        try
        {
            for(Course course : user.getCourses())
            {
                if(courseId == course.getId())
                    return true;
            }
        }
        catch(SQLException e)
        {
            log.error("sql error", e);
        }

        return false;
    }

    /**
     * returns true when the currently logged in user is allowed to moderate the given courses
     *
     * @param course
     * @return
     * @throws SQLException
     */
    public boolean canModerateCourse(Course course) throws SQLException
    {
        LinkedList<Course> courses = new LinkedList<Course>(); // create dummy list with single entry
        courses.add(course);

        return canModerateCourses(courses);
    }

    /**
     * returns true when the currently logged in user is allowed to moderate one of the given courses
     *
     * @param courses
     * @return
     * @throws SQLException
     */
    public boolean canModerateCourses(List<Course> courses) throws SQLException
    {
        User user = getUser();
        if(null == user)
            return false;

        if(user.isAdmin())
            return true;

        if(user.isModerator()) // check whether the user is moderator of one of the given courses
        {
            List<Course> moderatorsCourses = user.getCourses();

            for(Course course : courses)
            {
                if(moderatorsCourses.contains(course))
                    return true;
            }
        }

        return false;
    }

    public TimeZone getTimeZone()
    {
        User user = getUser();
        if(user == null)
            return TimeZone.getTimeZone("Europe/Berlin");

        return user.getTimeZone();
    }

    /**
     * Helper method to catch exceptions
     *
     * @return
     */
    @Deprecated
    private Course getActiveCourse()
    {
        if(isLoggedIn())
        {
            try
            {
                return getUser().getActiveCourse();
            }
            catch(SQLException e)
            {
                log.error("can't get active course of user: " + getUser().toString(), e);
            }
        }

        // in case of errors load public course
        return Learnweb.getInstance().getCourseManager().getCourseById(485);

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

        if(domain.equals("archiveweb"))
            selectCourse = Learnweb.getInstance().getCourseManager().getCourseById(891);

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
        return getActiveCourse().getOption(Course.Option.Search_History_log_enabled);
    }

    public boolean isGoogleDocsSignInEnabled()
    {
        return getActiveCourse().getOption(Course.Option.Course_Google_Docs_Sign_In_enabled);
    }

    public boolean isLanguageSwitchEnabled()
    {
        return !getActiveCourse().getOption(Course.Option.Users_Hide_language_switch);
    }

    /**
     * Model for the group menu
     *
     * @return
     */
    public LinkedList<DefaultSubMenu> getGroupMenu()
    {
        LinkedList<DefaultSubMenu> menu = new LinkedList<DefaultSubMenu>();
        Integer groupId = ApplicationBean.getParameterInt("group_id");

        try
        {
            for(Group group : getUser().getGroups())// getActiveCourse().getGroupsFilteredByUser(getUser()))
            {
                DefaultSubMenu submenu = new DefaultSubMenu();
                submenu.setLabel(group.getLongTitle());
                submenu.setId(Integer.toString(group.getId()));

                if(groupId != null && groupId.equals(group.getId()))
                {
                    submenu.setExpanded(true);
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

    public boolean isGroupResourcesPage()
    {
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        return viewId.contains("group/resources.xhtml");
    }

    /**
     * returns the groups tree where the user can add resources to
     *
     * @return
     * @throws SQLException
     */
    public TreeNode getWriteAbleGroupsTree() throws SQLException
    {
        if(!isLoggedIn())
            return null;

        if(null == groupsTree || groupsTreeCacheTime + 10000L < System.currentTimeMillis())
        {
            groupsTreeCacheTime = System.currentTimeMillis();
            groupsTree = new DefaultTreeNode("WriteAbleGroups");

            GroupManager gm = Learnweb.getInstance().getGroupManager();
            Group myResources = new Group();
            myResources.setId(0);
            myResources.setTitle(UtilBean.getLocaleMessage("myPrivateResources"));
            TreeNode myResourcesNode = new DefaultTreeNode("group", myResources, groupsTree);
            myResourcesNode.setSelected(true);

            for(Group group : getUser().getWriteAbleGroups())
            {
                TreeNode groupNode = new DefaultTreeNode("group", group, groupsTree);
                gm.getChildNodesRecursively(group.getId(), 0, groupNode, 0);
            }
        }

        //log.debug("getWriteAbleGroupsTree in " + (System.currentTimeMillis() - start) + "ms");

        return groupsTree;
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

        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        if(viewId.contains("register.xhtml")) // don't show any tooltips on the registration page
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

    public User getModeratorUser()
    {
        return moderatorUser;
    }

    public void setModeratorUser(User moderatorUser)
    {
        this.moderatorUser = moderatorUser;
    }

    /**
     *
     * @param url
     * @return Returns the given url proxied through WAPS.io if enabled for the current organization
     */
    public String getUrlProxied(String url)
    {
        User user = getUser();
        if(user == null || !user.getOrganisation().getOption(Option.Misc_Proxy_enabled))
            return url;

        return "http://waps.io/open?u=" + StringHelper.urlEncode(url) + "&c=2&i=" + user.getId();
    }
}

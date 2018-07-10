package de.l3s.learnweb.user;

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
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.ocpsoft.prettytime.PrettyTime;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.menu.DefaultSubMenu;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.FrontpageServlet;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.user.Organisation.Option;
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

    private transient List<Group> newGroups = null;

    private boolean cacheShowMessageJoinGroup = true;
    private boolean cacheShowMessageAddResource = true;

    private long groupsTreeCacheTime = 0L;
    private DefaultTreeNode groupsTree;
    private HashMap<String, String> anonymousPreferences = new HashMap<String, String>(); // preferences for users who are not logged in

    private Organisation activeOrganisation;

    public UserBean()
    {
        // get preferred language
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        locale = viewRoot != null ? viewRoot.getLocale() : externalContext.getRequestLocale();

        HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();

        if(FrontpageServlet.isArchiveWebRequest(httpRequest))
            activeOrganisation = Learnweb.getInstance().getOrganisationManager().getOrganisationById(848); // load archiveweb
        else
            activeOrganisation = Learnweb.getInstance().getOrganisationManager().getDefaultOrganisation();

        refreshLocale();
        storeMetadataInSession();
    }

    /**
     * This method sets values which are required by the Download Servlet
     * and provides data which is shown on the Tomcat mananger session page
     */
    private void storeMetadataInSession()
    {
        User user = getUser();
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
        session.setAttribute("learnweb_user_id", new Integer(userId)); // required by DownloadServlet
    }

    public boolean isLoggedIn()
    {
        return userId != 0;
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
        userId = user.getId();
        activeOrganisation = user.getOrganisation();

        //clear caches
        newGroups = null;
        cacheShowMessageJoinGroup = true;
        cacheShowMessageAddResource = true;
        userCache = null;

        refreshLocale();
        storeMetadataInSession();
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
     * example "de"
     *
     * @return
     */
    public String getLocaleCode()
    {
        return locale.getLanguage();
    }

    /**
     * After construction and login/logout we need to check if a default language has to be set
     */
    private void refreshLocale()
    {
        String localeCode = activeOrganisation.getDefaultLanguage() != null ? activeOrganisation.getDefaultLanguage() : locale.getLanguage();
        setLocaleCode(localeCode);
    }

    public String setLocaleCode(String localeCode)
    {
        String languageVariant = activeOrganisation.getLanguageVariant();
        log.debug("set locale " + localeCode);

        if(localeCode.equals("de"))
            locale = new Locale("de", "DE", languageVariant);
        else if(localeCode.equals("en"))
            locale = new Locale("en", "UK", languageVariant);
        else if(localeCode.equals("it"))
            locale = new Locale("it", "IT", languageVariant);
        else if(localeCode.equals("pt"))
            locale = new Locale("pt", "BR", languageVariant);
        else if(localeCode.equals("xx")) // only for translation editors
            locale = new Locale("", "", "KEYS");
        else
        {
            locale = new Locale("en", "UK");
            log.error("Unsupported language: " + localeCode);
        }
        log.debug("Locale set: " + locale + ";");

        localePrettyTime = null; // reset date formatter

        FacesContext facesContext = FacesContext.getCurrentInstance();
        // facesContext.getViewRoot().setLocale(locale);

        UIViewRoot viewRoot = facesContext.getViewRoot();
        if(viewRoot == null)
            return null;
        return viewRoot.getViewId() + "?faces-redirect=true&includeViewParams=true";
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
     * Returns the css code for the banner image of the active organization or an empty string if no image is defined
     *
     * @return
     * @throws SQLException
     */
    public String getBannerImage() throws SQLException
    {
        return activeOrganisation.getBannerImage();
    }

    public String getBannerLink() throws SQLException
    {
        return activeOrganisation.getWelcomePage();
    }

    /**
     * Returns a list of groups that have been created since the last login of this user
     *
     * @return
     * @throws SQLException
     */
    public List<Group> getNewGroups() throws SQLException
    {
        if(null == newGroups && getUser() != null)
        {
            newGroups = Learnweb.getInstance().getGroupManager().getGroupsByCourseId(getUser().getCourses(), getUser().getLastLoginDate());
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
        if(user == null || !user.getOrganisation().getOption(Option.Privacy_Proxy_enabled))
            return url;

        if(user.getOrganisationId() == 1369) // fakenews project
        {
            if(url.startsWith("https://via.hypothes.is"))
                return url;
            return "https://via.hypothes.is/" + url;
        }
        else
        {
            if(url.startsWith("https://waps.io") || url.startsWith("http://waps.io"))
                return url;
            return "http://waps.io/open?u=" + StringHelper.urlEncode(url) + "&c=2&i=" + user.getId();
        }

    }

    public boolean isOptionContentAnnotationFieldEnabled()
    {
        return activeOrganisation.getOption(Option.Resource_Show_Content_Annotation_Field);
    }

    public boolean isStarRatingEnabled()
    {
        return !activeOrganisation.getOption(Option.Resource_Hide_Star_rating);
    }

    public boolean isThumbRatingEnabled()
    {
        return !activeOrganisation.getOption(Option.Resource_Hide_Thumb_rating);
    }

    public boolean isLoggingEnabled()
    {
        return !activeOrganisation.getOption(Option.Privacy_Logging_disabled);
    }

    public boolean isTrackingEnabled()
    {
        return !activeOrganisation.getOption(Option.Privacy_Tracker_disabled);
    }

    public boolean isLanguageSwitchEnabled()
    {
        return !activeOrganisation.getOption(Option.Users_Hide_language_switch);
    }

}

package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.ocpsoft.prettytime.PrettyTime;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.DynamicMenuModel;
import org.primefaces.model.menu.MenuModel;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.FrontpageServlet;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.component.ActiveSubMenu;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.util.BeanHelper;
import de.l3s.util.StringHelper;

@Named
@SessionScoped
public class UserBean implements Serializable
{
    private final static long serialVersionUID = -8577036953815676943L;
    private final static Logger log = Logger.getLogger(UserBean.class);

    private int userId = 0;
    private transient User user; // to avoid inconsistencies with the user cache the UserBean does not store the user itself
    private transient User moderatorUser; // in this field we store a moderator account while the moderator is logged in on an other account

    private Locale locale;
    private transient PrettyTime localePrettyTime;

    private transient List<Group> newGroups = null;

    private boolean cacheShowMessageJoinGroup = true;
    private boolean cacheShowMessageAddResource = true;

    private long groupsTreeCacheTime = 0L;
    private DefaultTreeNode groupsTree;
    private DynamicMenuModel sidebarMenuModel;
    private long sidebarMenuModelCacheTime = 0L;
    private HashMap<String, String> anonymousPreferences = new HashMap<>(); // preferences for users who are not logged in

    private int activeOrganisationId = 0;
    private transient Organisation activeOrganisation;

    public UserBean()
    {
        // get preferred language
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        locale = viewRoot != null ? viewRoot.getLocale() : externalContext.getRequestLocale();

        HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();

        if(FrontpageServlet.isArchiveWebRequest(httpRequest))
            activeOrganisationId = 848; // archiveweb
        else
            activeOrganisationId = 478; // public

        refreshLocale();
        storeMetadataInSession();
    }

    /**
     * This method sets values which are required by the Download Servlet
     * and provides data which is shown on the Tomcat manager session page
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
        session.setAttribute("learnweb_user_id", userId); // required by DownloadServlet
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

        if(user == null)
        {
            try
            {
                user = Learnweb.getInstance().getUserManager().getUser(userId);
            }
            catch(SQLException e)
            {
                log.fatal("Can't retrieve user " + userId, e);
            }
        }
        return user;
    }

    /**
     * Use this function to log in a user.
     *
     * @param user
     */
    public void setUser(User user)
    {
        this.userId = user.getId();
        this.user = user;
        this.activeOrganisation = user.getOrganisation();

        //clear caches
        this.newGroups = null;
        this.cacheShowMessageJoinGroup = true;
        this.cacheShowMessageAddResource = true;

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
        String localeCode = getActiveOrganisation().getDefaultLanguage() != null ? activeOrganisation.getDefaultLanguage() : locale.getLanguage();
        setLocaleCode(localeCode);
    }

    public String setLocaleCode(String localeCode)
    {
        String languageVariant = getActiveOrganisation().getLanguageVariant();
        //log.debug("set locale " + localeCode);

        switch(localeCode)
        {
        case "de":
            locale = new Locale("de", "DE", languageVariant);
            break;
        case "en":
            locale = new Locale("en", "UK", languageVariant);
            break;
        case "it":
            locale = new Locale("it", "IT", languageVariant);
            break;
        case "pt":
            locale = new Locale("pt", "BR", languageVariant);
            break;
        case "es":
            locale = new Locale("es", "ES", languageVariant);
            break;
        case "xx":
            // only for translation editors
            locale = new Locale("xx");
            break;
        default:
            locale = new Locale("en", "UK");
            log.error("Unsupported language: " + localeCode);
            break;
        }

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
        List<Course> courses = new ArrayList<>(1); // create dummy list with single entry
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
        return getActiveOrganisation().getBannerImage();
    }

    public String getBannerLink() throws SQLException
    {

        return Learnweb.getInstance().getSecureServerUrl() + getActiveOrganisation().getWelcomePage();
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
        LinkedList<DefaultSubMenu> menu = new LinkedList<>();
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
            Group myResources = new Group(0, UtilBean.getLocaleMessage("myPrivateResources"));
            TreeNode myResourcesNode = new DefaultTreeNode("group", myResources, groupsTree);
            myResourcesNode.setSelected(true);

            for(Group group : getUser().getWriteAbleGroups())
            {
                // group /lw/myhome/resources.jsf?group_id=#{node.id}">#{node.title}
                // folder /lw/myhome/resources.jsf?folder_id=#{node.folderId}&amp;resource_id=0&amp;group_id=#{node.groupId}"
                TreeNode groupNode = new DefaultTreeNode("group", group, groupsTree);
                gm.getChildNodesRecursively(group.getId(), 0, groupNode, 0);
            }
        }

        //log.debug("getWriteAbleGroupsTree in " + (System.currentTimeMillis() - start) + "ms");

        return groupsTree;
    }

    public MenuModel getSidebarMenuModel() throws SQLException
    {
        if(!isLoggedIn())
            return null;

        if(null == sidebarMenuModel || sidebarMenuModelCacheTime + 10000L < System.currentTimeMillis())
        {
            long start = System.currentTimeMillis();
            final String su = Learnweb.getInstance().getServerUrl();
            DynamicMenuModel model = new DynamicMenuModel();

            // My resources
            ActiveSubMenu myResources = new ActiveSubMenu(UtilBean.getLocaleMessage("myResourcesTitle"), null, su + "/lw/myhome/resources.jsf");
            myResources.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("myPrivateResources"), "fa fa-fw fa-folder", su + "/lw/myhome/resources.jsf"));
            myResources.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("myCommentsTitle"), "fa fa-fw fa-comments", su + "/lw/myhome/comments.jsf"));
            myResources.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("myTagsTitle"), "fa fa-fw fa-tags", su + "/lw/myhome/tags.jsf"));
            myResources.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("Submission.my_submissions"), "fa fa-fw fa-credit-card-alt", su + "/lw/myhome/submission_overview.jsf"));
            model.addElement(myResources);

            // My groups
            ActiveSubMenu myGroups = new ActiveSubMenu(UtilBean.getLocaleMessage("myGroups"), null, su + "/lw/myhome/groups.jsf");
            for(Group group : getUser().getGroups())
            {
                ActiveSubMenu theGroup = new ActiveSubMenu(group.getTitle(), null, su + "/lw/group/overview.jsf?group_id=" + group.getId());
                theGroup.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("overview"), "fa fa-fw fa-list-ul", su + "/lw/group/overview.jsf?group_id=" + group.getId()));
                theGroup.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("resources"), "fa fa-fw fa-folder-open", su + "/lw/group/resources.jsf?group_id=" + group.getId()));
                theGroup.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("members"), "fa fa-fw fa-users", su + "/lw/group/members.jsf?group_id=" + group.getId()));
                if(!group.getLinks().isEmpty() || !group.getDocumentLinks().isEmpty())
                {
                    theGroup.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("links"), "fa fa-fw fa-link", su + "/lw/group/links.jsf?group_id=" + group.getId()));
                }
                theGroup.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("forum"), "fa fa-fw fa-comments-o", su + "/lw/group/forum.jsf?group_id=" + group.getId()));
                if(group.getCourse().isModerator(getUser()) || group.isLeader(getUser()))
                {
                    theGroup.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("options"), "fa fa-fw fa-sliders", su + "/lw/group/options.jsf?group_id=" + group.getId()));
                }
                theGroup.setStyleClass("ui-menuitem-group");
                myGroups.addElement(theGroup);
            }
            model.addElement(myGroups);

            // Moderator (menu hidden for user EUMADE4ALL; can be removed in 2020)
            if(getUser().isModerator() && getUser().getId() != 12476)
            {
                DefaultSubMenu moderatorSubmenu = new DefaultSubMenu(UtilBean.getLocaleMessage("moderator"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("send_notification"), "fa fa-fw fa-envelope-open", su + "/lw/admin/notification.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("users"), "fa fa-fw fa-users", su + "/lw/admin/users.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("courses"), "fa fa-fw fa-graduation-cap", su + "/lw/admin/courses.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("organisation"), "fa fa-fw fa-sitemap", su + "/lw/admin/organisation.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("text_analysis"), "fa fa-fw fa-area-chart", su + "/lw/admin/text_analysis.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("statistics"), "fa fa-fw fa-line-chart", su + "/lw/admin/statistics.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("detailed_transcript_log"), "fa fa-fw fa-language", su + "/lw/admin/detailed_transcript_log.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("simple_transcript_log"), "fa fa-fw fa-language", su + "/lw/admin/simple_transcript_log.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("transcript_summaries"), "fa fa-fw fa-language", su + "/lw/admin/transcript_summary.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("glossary_dashboard"), "fa fa-fw fa-bar-chart", su + "/lw/admin/dashboard/glossary.jsf"));
                moderatorSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("activity_dashboard"), "fa fa-fw fa-line-chart", su + "/lw/admin/dashboard/activity.jsf"));
                model.addElement(moderatorSubmenu);
            }

            // Admin (menu hidden for user EUMADE4ALL; can be removed in 2020)
            if(getUser().isAdmin() && getUser().getId() != 12476)
            {
                DefaultSubMenu adminSubmenu = new DefaultSubMenu(UtilBean.getLocaleMessage("admin"));
                adminSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("organisations"), "fa fa-fw fa-sitemap", su + "/lw/admin/organisations.jsf"));
                adminSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("banlist"), "fa fa-fw fa-area-chart", su + "/lw/admin/banlist.jsf"));
                adminSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("ip_requests"), "fa fa-fw fa-line-chart", su + "/lw/admin/requests.jsf"));
                adminSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("system_tools"), "fa fa-fw fa-language", su + "/lw/admin/systemtools.jsf"));
                adminSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("admin_messages_Title"), "fa fa-fw fa-language", su + "/lw/admin/adminmsg.jsf"));
                adminSubmenu.addElement(new DefaultMenuItem(UtilBean.getLocaleMessage("news"), "fa fa-fw fa-language", su + "/lw/admin/adminnews.jsf"));
                model.addElement(adminSubmenu);
            }

            sidebarMenuModel = model;
            sidebarMenuModelCacheTime = System.currentTimeMillis();
            long elapsedMs = System.currentTimeMillis() - start;
            log.info("Total time to build menu: " + elapsedMs + "ms.");
        }

        return sidebarMenuModel;
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
            return "http://waps.io/open?c=2" +
                    "&u=" + StringHelper.urlEncode(url) +
                    "&i=" + user.getId() +
                    "&t=" + Learnweb.getInstance().getProperties().getProperty("tracker.key");
        }

    }

    public boolean isOptionContentAnnotationFieldEnabled()
    {
        return getActiveOrganisation().getOption(Option.Resource_Show_Content_Annotation_Field);
    }

    public boolean isStarRatingEnabled()
    {
        return !getActiveOrganisation().getOption(Option.Resource_Hide_Star_rating);
    }

    public boolean isThumbRatingEnabled()
    {
        return !getActiveOrganisation().getOption(Option.Resource_Hide_Thumb_rating);
    }

    public boolean isLoggingEnabled()
    {
        return !getActiveOrganisation().getOption(Option.Privacy_Logging_disabled);
    }

    public boolean isTrackingEnabled()
    {
        return !getActiveOrganisation().getOption(Option.Privacy_Tracker_disabled);
    }

    public boolean isLanguageSwitchEnabled()
    {
        return !getActiveOrganisation().getOption(Option.Users_Hide_language_switch);
    }

    private Organisation getActiveOrganisation()
    {
        if(null == activeOrganisation)
        {
            activeOrganisation = Learnweb.getInstance().getOrganisationManager().getOrganisationById(activeOrganisationId);
        }
        return activeOrganisation;
    }
}

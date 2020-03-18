package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.model.menu.BaseMenuModel;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.component.ActiveSubMenu;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.util.StringHelper;
import de.l3s.util.bean.BeanHelper;

@Named
@SessionScoped
public class UserBean implements Serializable
{
    private static final long serialVersionUID = -8577036953815676943L;
    private static final Logger log = Logger.getLogger(UserBean.class);

    private int userId = 0;
    private transient User user; // to avoid inconsistencies with the user cache the UserBean does not store the user itself
    private transient User moderatorUser; // in this field we store a moderator account while the moderator is logged in on an other account

    private String searchQuery;
    private Locale locale;

    private transient List<Group> newGroups = null;

    private boolean cacheShowMessageJoinGroup = true;
    private boolean cacheShowMessageAddResource = true;

    private transient BaseMenuModel sidebarMenuModel;
    private transient Instant sidebarMenuModelUpdate;
    private HashMap<String, String> anonymousPreferences = new HashMap<>(); // preferences for users who are not logged in

    private int activeOrganisationId = 0;
    private transient Organisation activeOrganisation;

    public UserBean()
    {
        // get preferred language
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        locale = viewRoot != null ? viewRoot.getLocale() : externalContext.getRequestLocale();

        activeOrganisationId = 478; // public

        refreshLocale();
        storeMetadataInSession();

        log.debug("created UserBean");
    }

    public String getSearchQuery()
    {
        return searchQuery;
    }

    public void setSearchQuery(final String searchQuery)
    {
        this.searchQuery = searchQuery;
    }

    public String search()
    {
        return "/lw/search.xhtml?query=" + searchQuery + "&action=" + getPreference("SEARCH_ACTION", "text") + "&faces-redirect=true";
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
        this.sidebarMenuModel = null;
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

    public String getPreference(String key, String defaultValue)
    {
        String obj = getPreference(key);
        return obj == null ? defaultValue : obj;
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
        setSidebarMenuModel(null);
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
        case "uk":
            locale = new Locale("uk", "UA", languageVariant);
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
     * Returns the css code for the banner image of the active organization or an empty string if no image is defined
     *
     * @return
     * @throws SQLException
     */
    public String getBannerImage() throws SQLException
    {
        String bannerImage = null;

        if(isLoggedIn())
            bannerImage = getActiveOrganisation().getBannerImage();

        if(StringUtils.isNotEmpty(bannerImage))
            return bannerImage;

        return "logos/logo_learnweb.png";
    }

    public String getBannerLink() throws SQLException
    {
        if(!isLoggedIn())
            return Learnweb.getInstance().getServerUrl();

        return Learnweb.getInstance().getServerUrl() + getActiveOrganisation().getWelcomePage();
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

    public MenuModel getSidebarMenuModel() throws SQLException
    {
        if(!isLoggedIn())
            return null;

        if(null == sidebarMenuModel || sidebarMenuModelUpdate.isBefore(Instant.now().minus(Duration.ofMinutes(10))))
        {
            long start = System.currentTimeMillis();
            final String su = Learnweb.getInstance().getServerUrl();
            BaseMenuModel model = new BaseMenuModel();

            // My resources
            ActiveSubMenu myResources = ActiveSubMenu.builder()
                    .label(UtilBean.getLocaleMessage("myResourcesTitle"))
                    .url(su + "/lw/myhome/resources.jsf")
                    .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("myPrivateResources")).icon("fa fa-fw fa-folder").url(su + "/lw/myhome/resources.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("myCommentsTitle")).icon("fa fa-fw fa-comments").url(su + "/lw/myhome/comments.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("myTags")).icon("fa fa-fw fa-tags").url(su + "/lw/myhome/tags.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("myRatedResourcesTitle")).icon("fa fa-fw fa-tags").url(su + "/lw/myhome/rated_resources.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("Submission.my_submissions")).icon("fa fa-fw fa-credit-card-alt").url(su + "/lw/myhome/submission_overview.jsf").build())
                    .build();
            model.getElements().add(myResources);

            // My groups
            ActiveSubMenu.Builder groupsBuilder = ActiveSubMenu.builder().label(UtilBean.getLocaleMessage("myGroups")).url(su + "/lw/myhome/groups.jsf");
            for(Group group : getUser().getGroups())
            {
                ActiveSubMenu.Builder groupBuilder = ActiveSubMenu.builder().label(group.getTitle()).url(su + "/lw/group/overview.jsf?group_id=" + group.getId()).styleClass("ui-menuitem-group");
                groupBuilder.addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("overview")).icon("fa fa-fw fa-list-ul").url(su + "/lw/group/overview.jsf?group_id=" + group.getId()).build());
                groupBuilder.addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("resources")).icon("fa fa-fw fa-folder-open").url(su + "/lw/group/resources.jsf?group_id=" + group.getId()).build());
                if(!group.getLinks().isEmpty() || !group.getDocumentLinks().isEmpty())
                {
                    groupBuilder.addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("links")).icon("fa fa-fw fa-link").url(su + "/lw/group/links.jsf?group_id=" + group.getId()).build());
                }
                groupBuilder.addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("forum")).icon("fa fa-fw fa-comments-o").url(su + "/lw/group/forum.jsf?group_id=" + group.getId()).build());
                if(group.getCourse().isModerator(getUser()) || group.isLeader(getUser()))
                {
                    groupBuilder.addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("options")).icon("fa fa-fw fa-sliders").url(su + "/lw/group/options.jsf?group_id=" + group.getId()).build());
                }
                groupsBuilder.addElement(groupBuilder.build());
            }
            model.getElements().add(groupsBuilder.build());

            // Moderator submenu
            if(getUser().isModerator())
            {
                ActiveSubMenu moderatorSubmenu = ActiveSubMenu.builder()
                        .label(UtilBean.getLocaleMessage("moderator"))
                        .url(su + "/lw/moderator.jsf")
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("send_notification")).icon("fa fa-fw fa-envelope-open").url(su + "/lw/admin/notification.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("users")).icon("fa fa-fw fa-users").url(su + "/lw/admin/users.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("courses")).icon("fa fa-fw fa-graduation-cap").url(su + "/lw/admin/courses.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("organisation")).icon("fa fa-fw fa-sitemap").url(su + "/lw/admin/organisation.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("text_analysis")).icon("fa fa-fw fa-area-chart").url(su + "/lw/admin/text_analysis.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("statistics")).icon("fa fa-fw fa-line-chart").url(su + "/lw/admin/statistics.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("detailed_transcript_log")).icon("fa fa-fw fa-language").url(su + "/lw/admin/detailed_transcript_log.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("simple_transcript_log")).icon("fa fa-fw fa-language").url(su + "/lw/admin/simple_transcript_log.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("transcript_summaries")).icon("fa fa-fw fa-language").url(su + "/lw//admin/transcript_summary.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("glossary_dashboard")).icon("fa fa-fw fa-bar-chart").url(su + "/lw/dashboard/glossary.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("Activity.dashboard")).icon("fa fa-fw fa-line-chart").url(su + "/lw/dashboard/activity.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("Tracker.dashboard")).icon("fa fa-fw fa-database").url(su + "/lw/dashboard/tracker.jsf").build())
                        .build();
                model.getElements().add(moderatorSubmenu);
            }

            // Admin submenu
            if(getUser().isAdmin())
            {
                ActiveSubMenu adminSubmenu = ActiveSubMenu.builder()
                        .label(UtilBean.getLocaleMessage("admin"))
                        .url(su + "/lw/admin.jsf")
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("organisations")).icon("fa fa-fw fa-sitemap").url(su + "/lw/admin/organisations.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("banlist")).icon("fa fa-fw fa-area-chart").url(su + "/lw/admin/banlist.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("ip_requests")).icon("fa fa-fw fa-line-chart").url(su + "/lw/admin/requests.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("system_tools")).icon("fa fa-fw fa-language").url(su + "/lw/admin/systemtools.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("announcements")).icon("fa fa-fw fa-language").url(su + "/lw/admin/announcements.jsf").build())
                        .addElement(DefaultMenuItem.builder().value(UtilBean.getLocaleMessage("survey.survey_overview")).icon("fa fa-question-circle").url(su + "/lw/survey/templates.jsf").build())
                        .build();
                model.getElements().add(adminSubmenu);
            }

            sidebarMenuModel = model;
            sidebarMenuModelUpdate = Instant.now();
            long elapsedMs = System.currentTimeMillis() - start;
            log.info("Total time to build menu: " + elapsedMs + "ms.");
        }

        return sidebarMenuModel;
    }

    public void setSidebarMenuModel(final BaseMenuModel sidebarMenuModel)
    {
        this.sidebarMenuModel = sidebarMenuModel;
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
            return "https://waps.io/open?c=2" +
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

    public boolean isHideSidebarMenu()
    {
        return "true".equals(getPreference("HIDE_SIDEBAR"));
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

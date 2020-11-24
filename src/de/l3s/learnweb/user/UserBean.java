package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;
import org.primefaces.model.menu.BaseMenuModel;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.component.ActiveSubMenu;
import de.l3s.learnweb.component.ActiveSubMenu.Builder;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.util.StringHelper;
import de.l3s.util.bean.BeanHelper;

@Named
@SessionScoped
public class UserBean implements Serializable {
    private static final long serialVersionUID = -8577036953815676943L;
    private static final Logger log = LogManager.getLogger(UserBean.class);

    private static final int PUBLIC_ORGANISATION_ID = 478;

    private int userId = 0;
    private transient User user; // to avoid inconsistencies with the user cache the UserBean does not store the user itself
    private transient User moderatorUser; // in this field we store a moderator account while the moderator is logged in on an other account

    private String searchQuery;
    private Locale locale;
    private transient List<Group> newGroups;
    private boolean cacheShowMessageJoinGroup = true;
    private boolean cacheShowMessageAddResource = true;

    private transient BaseMenuModel sidebarMenuModel;
    private transient Instant sidebarMenuModelUpdate;
    private final HashMap<String, String> anonymousPreferences = new HashMap<>(); // preferences for users who are not logged in

    private transient Organisation activeOrganisation;

    private boolean guided; // indicates that the user has started one of the guides

    public UserBean() {
        // get preferred language
        locale = Faces.getLocale();
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(final String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String search() {
        return "/lw/search.xhtml?query=" + searchQuery + "&action=" + getPreference("SEARCH_ACTION", "text") + "&faces-redirect=true";
    }

    /**
     * This method sets values which are required by the Download Servlet
     * and provides data which is shown on the Tomcat manager session page.
     *
     * @return userName | ipAddress | userAgent for the current request;
     */
    public String storeMetadataInSession(HttpServletRequest request) {
        String userName = user == null ? "logged_out" : user.getRealUsername();
        String info = userName + " | " + Servlets.getRemoteAddr(request) + " | " + request.getHeader("User-Agent");

        // store the user also in the session so that it is accessible by DownloadServlet and TomcatManager
        HttpSession session = request.getSession(true);
        session.setAttribute("userName", info); // set only to display it in Tomcat manager app
        session.setAttribute("Locale", locale); // set only to display it in Tomcat manager app
        session.setAttribute("learnweb_user_id", userId); // required by DownloadServlet
        return info;
    }

    public boolean isLoggedIn() {
        return userId != 0;
    }

    /**
     * The currently logged in user.
     */
    public User getUser() {
        if (userId == 0) {
            return null;
        }

        if (user == null) {
            try {
                user = Learnweb.getInstance().getUserManager().getUser(userId);
            } catch (SQLException e) {
                log.fatal("Can't retrieve user {}", userId, e);
            }
        }
        return user;
    }

    /**
     * Use this function to log in a user.
     */
    public void setUser(User user) {
        setUser(user, Faces.getRequest());
    }

    /**
     * Use this function to log in a user.
     */
    public void setUser(User user, HttpServletRequest request) {
        this.userId = user.getId();
        this.user = user;
        this.activeOrganisation = user.getOrganisation();

        // clear caches
        this.sidebarMenuModel = null;
        this.newGroups = null;
        this.cacheShowMessageJoinGroup = true;
        this.cacheShowMessageAddResource = true;

        refreshLocale();
        String clientInfo = storeMetadataInSession(request);
        log.debug("Session started: {}", clientInfo);
    }

    @PreDestroy
    public void onDestroy() {
        // persist user preferences in database
        if (null != user) {
            user.setLocale(locale);
            user.onDestroy();
        }
    }

    public String getPreference(String key, String defaultValue) {
        String obj = getPreference(key);
        return obj == null ? defaultValue : obj;
    }

    public String getPreference(String key) {
        if (isLoggedIn()) {
            return getUser().getPreferences().get(key);
        }

        return anonymousPreferences.get(key);
    }

    public void setPreference(String key, String value) {
        HashMap<String, String> preferences = isLoggedIn() ? getUser().getPreferences() : anonymousPreferences;

        preferences.put(key, value);
    }

    public void commandSetPreference() {
        Map<String, String> params = Faces.getRequestParameterMap();
        String key = params.get("key");
        String value = params.get("value");

        setPreference(key, value);
    }

    public Locale getLocale() {
        return locale;
    }

    /**
     * example "de".
     */
    public String getLocaleCode() {
        return locale.getLanguage();
    }

    /**
     * After construction and login/logout we need to check if a default language has to be set.
     */
    private void refreshLocale() {
        if (isLoggedIn() && getUser().getLocale() != null) {
            setLocaleCode(getUser().getLocale().getLanguage());
        } else {
            String defaultLanguage = getActiveOrganisation().getDefaultLanguage();
            String localeCode = defaultLanguage != null ? defaultLanguage : locale.getLanguage();
            setLocaleCode(localeCode);
        }
    }

    public String setLocaleCode(String localeCode) {
        setSidebarMenuModel(null);
        String languageVariant = getActiveOrganisation().getLanguageVariant();
        //log.debug("set locale " + localeCode);

        switch (localeCode) {
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
                log.error("Unsupported language: {}", localeCode);
                break;
        }

        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getViewRoot() == null) {
            return null;
        }

        return fc.getViewRoot().getViewId() + "?faces-redirect=true&includeViewParams=true";
    }

    public boolean isAdmin() {
        User user = getUser();
        if (null == user) {
            return false;
        }

        return user.isAdmin();
    }

    public boolean isModerator() {
        User user = getUser();
        if (null == user) {
            return false;
        }

        return user.isModerator();
    }

    public boolean isMemberOfCourse(int courseId) {
        User user = getUser();

        if (user == null) {
            return false;
        }

        try {
            for (Course course : user.getCourses()) {
                if (courseId == course.getId()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error("sql error", e);
        }

        return false;
    }

    public String getTimeZone() {
        User user = getUser();
        if (user == null) {
            return "Europe/Berlin";
        }

        return user.getTimeZone().getId();
    }

    @Override
    public String toString() {
        User user = getUser();
        if (user == null) {
            return "not logged in";
        }

        return "userId: " + user.getId() + " name: " + user.getUsername();
    }

    // -------------------- Frontend ---------------------------

    /**
     * Returns the css code for the banner image of the active organization or an empty string if no image is defined.
     */
    public String getBannerImage() throws SQLException {
        String bannerImage = null;

        if (isLoggedIn()) {
            bannerImage = getActiveOrganisation().getBannerImage();
        }

        if (StringUtils.isNotEmpty(bannerImage)) {
            return bannerImage;
        }

        return "logos/logo_learnweb.png";
    }

    public String getBannerLink() throws SQLException {
        if (!isLoggedIn()) {
            return Learnweb.getInstance().getServerUrl();
        }

        return Learnweb.getInstance().getServerUrl() + getActiveOrganisation().getWelcomePage();
    }

    /**
     * Returns a list of groups that have been created since the last login of this user.
     */
    public List<Group> getNewGroups() throws SQLException {
        if (null == newGroups && getUser() != null) {
            newGroups = Learnweb.getInstance().getGroupManager().getGroupsByCourseId(getUser().getCourses(), getUser().getLastLoginDate());
        }

        return newGroups;
    }

    public int getNewGroupsCount() {
        try {
            return getNewGroups().size();
        } catch (SQLException e) {
            log.error(BeanHelper.getRequestSummary(), e);
        }

        return 0;
    }

    public MenuModel getSidebarMenuModel() throws SQLException {
        if (!isLoggedIn()) {
            return null;
        }

        if (null == sidebarMenuModel || sidebarMenuModelUpdate.isBefore(Instant.now().minus(Duration.ofMinutes(10)))) {
            long start = System.currentTimeMillis();
            final String su = Learnweb.getInstance().getServerUrl();
            BaseMenuModel model = new BaseMenuModel();
            ResourceBundle msg = LanguageBundle.getLanguageBundle(getLocale());

            // My resources
            Builder myResources = ActiveSubMenu.builder()
                .label(msg.getString("myResourcesTitle"))
                .styleClass("guide-my-resources")
                .url(su + "/lw/myhome/resources.jsf")
                .addElement(DefaultMenuItem.builder().value(msg.getString("myPrivateResources")).icon("fa fa-fw fa-folder").url(su + "/lw/myhome/resources.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("myCommentsTitle")).icon("fa fa-fw fa-comments").url(su + "/lw/myhome/comments.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("myTags")).icon("fa fa-fw fa-tags").url(su + "/lw/myhome/tags.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("myRatedResourcesTitle")).icon("fa fa-fw fa-star").url(su + "/lw/myhome/rated_resources.jsf").build());

            if (!user.getActiveSubmissions().isEmpty()) {
                myResources.addElement(DefaultMenuItem.builder().value(msg.getString("Submission.my_submissions")).icon("fa fa-fw fa-credit-card-alt").url(su + "/lw/myhome/submission_overview.jsf").build());
            }

            model.getElements().add(myResources.build());

            // My groups
            ActiveSubMenu.Builder groupsBuilder = ActiveSubMenu.builder().label(msg.getString("myGroups")).url(su + "/lw/myhome/groups.jsf").styleClass("guide-my-groups");
            for (Group group : getUser().getGroups()) {
                ActiveSubMenu.Builder groupBuilder = ActiveSubMenu.builder().label(group.getTitle()).url(su + "/lw/group/overview.jsf?group_id=" + group.getId()).styleClass("ui-menuitem-group");
                groupBuilder.addElement(DefaultMenuItem.builder().value(msg.getString("overview")).icon("fa fa-fw fa-list-ul").url(su + "/lw/group/overview.jsf?group_id=" + group.getId()).build());
                groupBuilder.addElement(DefaultMenuItem.builder().value(msg.getString("resources")).icon("fa fa-fw fa-folder-open").url(su + "/lw/group/resources.jsf?group_id=" + group.getId()).build());
                groupBuilder.addElement(DefaultMenuItem.builder().value(msg.getString("forum")).icon("fa fa-fw fa-comments-o").url(su + "/lw/group/forum.jsf?group_id=" + group.getId()).build());
                groupBuilder.addElement(DefaultMenuItem.builder().value(msg.getString("members")).icon("fa fa-fw fa-users").url(su + "/lw/group/members.jsf?group_id=" + group.getId()).build());
                groupBuilder.addElement(DefaultMenuItem.builder().value(msg.getString("options")).icon("fa fa-fw fa-sliders").url(su + "/lw/group/options.jsf?group_id=" + group.getId()).build());
                groupsBuilder.addElement(groupBuilder.build());
            }
            model.getElements().add(groupsBuilder.build());

            // Moderator submenu
            if (getUser().isModerator()) {
                ActiveSubMenu moderatorSubmenu = ActiveSubMenu.builder()
                    .label(msg.getString("moderator"))
                    .url(su + "/lw/moderator.jsf")
                    .addElement(DefaultMenuItem.builder().value(msg.getString("send_notification")).icon("fa fa-fw fa-envelope-open").url(su + "/lw/admin/notification.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("users")).icon("fa fa-fw fa-user").url(su + "/lw/admin/users.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("courses")).icon("fa fa-fw fa-graduation-cap").url(su + "/lw/admin/courses.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("organisation")).icon("fa fa-fw fa-sitemap").url(su + "/lw/admin/organisation.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("text_analysis")).icon("fa fa-fw fa-area-chart").url(su + "/lw/admin/text_analysis.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("statistics")).icon("fa fa-fw fa-line-chart").url(su + "/lw/admin/statistics.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("transcript")).icon("fa fa-fw fa-language").url(su + "/lw/admin/transcript.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("glossary_dashboard")).icon("fa fa-fw fa-bar-chart").url(su + "/lw/dashboard/glossary.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("Activity.dashboard")).icon("fa fa-fw fa-line-chart").url(su + "/lw/dashboard/activity.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("Tracker.dashboard")).icon("fa fa-fw fa-database").url(su + "/lw/dashboard/tracker.jsf").build())
                    .build();
                model.getElements().add(moderatorSubmenu);
            }

            // Admin submenu
            if (getUser().isAdmin()) {
                ActiveSubMenu adminSubmenu = ActiveSubMenu.builder()
                    .label(msg.getString("admin"))
                    .url(su + "/lw/admin/index.jsf")
                    .addElement(DefaultMenuItem.builder().value(msg.getString("organisations")).icon("fa fa-fw fa-sitemap").url(su + "/lw/admin/organisations.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("banlist")).icon("fa fa-fw fa-area-chart").url(su + "/lw/admin/banlist.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("ip_requests")).icon("fa fa-fw fa-line-chart").url(su + "/lw/admin/requests.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("system_tools")).icon("fa fa-fw fa-language").url(su + "/lw/admin/systemtools.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("announcements")).icon("fa fa-fw fa-language").url(su + "/lw/admin/announcements.jsf").build())
                    .addElement(DefaultMenuItem.builder().value(msg.getString("survey.survey_overview")).icon("fa fa-question-circle").url(su + "/lw/survey/surveys.jsf").build())
                    .build();
                model.getElements().add(adminSubmenu);
            }

            sidebarMenuModel = model;
            sidebarMenuModelUpdate = Instant.now();
            long elapsedMs = System.currentTimeMillis() - start;

            if (elapsedMs > 20) {
                log.warn("Total time to build menu: {} ms.", elapsedMs);
            }
        }

        return sidebarMenuModel;
    }

    public void setSidebarMenuModel(final BaseMenuModel sidebarMenuModel) {
        this.sidebarMenuModel = sidebarMenuModel;
    }

    /**
     * Returns true when there is any tooltip message to show.
     */
    public boolean isShowMessageAny() throws SQLException {
        if (!isLoggedIn()) {
            return false;
        }

        String viewId = Faces.getViewId();
        if (viewId.contains("register.xhtml")) { // don't show any tooltips on the registration page
            return false;
        }

        return isShowMessageJoinGroup() || isShowMessageAddResource();
    }

    public boolean isShowMessageJoinGroup() throws SQLException {
        if (cacheShowMessageJoinGroup) { // check until the user has joined a group
            User user = getUser();
            if (null == user) {
                return false;
            }
            cacheShowMessageJoinGroup = getUser().getGroupCount() == 0;
        }
        return cacheShowMessageJoinGroup;
    }

    public boolean isShowMessageJoinGroupInHeader() throws SQLException {
        String viewId = Faces.getViewId();
        if (viewId.contains("groups.xhtml")) {
            return false;
        }

        return isShowMessageJoinGroup();
    }

    public boolean isShowMessageAddResourceInHeader() throws SQLException {
        String viewId = Faces.getViewId();
        if (viewId.contains("overview.xhtml") || viewId.contains("resources.xhtml") || viewId.contains("welcome.xhtml")) {
            return false;
        }

        return isShowMessageAddResource();
    }

    public boolean isShowMessageAddResource() throws SQLException {
        if (cacheShowMessageAddResource) { // check until the user has added a resource
            if (isShowMessageJoinGroup()) {
                return false;
            }

            User user = getUser();
            if (null == user) {
                return false;
            }

            cacheShowMessageAddResource = getUser().getResourceCount() == 0;
        }
        return cacheShowMessageAddResource;
    }

    /**
     * @return in this field we store a moderator account while the moderator is logged in on an other account
     */
    public User getModeratorUser() {
        return moderatorUser;
    }

    /**
     * @param moderatorUser in this field we store a moderator account while the moderator is logged in on an other account
     */
    public void setModeratorUser(User moderatorUser) {
        this.moderatorUser = moderatorUser;
    }

    /**
     * @return the resources url proxied through WAPS.io if enabled for the current organization
     */
    public String getUrlProxied(Resource resource) {
        return getUrlProxied(resource.getUrl());
    }

    /**
     * @return the resources url proxied through WAPS.io if enabled for the current organization
     */
    public String getUrlProxied(ResourceDecorator resource) {
        return getUrlProxied(resource.getUrl());
    }

    /**
     * @return Returns the given url proxied through WAPS.io if enabled for the current organization
     */
    public String getUrlProxied(String url) {
        User user = getUser();
        if (url == null || user == null || !user.getOrganisation().getOption(Option.Privacy_Proxy_enabled)) {
            return url;
        }

        if (user.getOrganisationId() == 1369) { // fakenews project
            if (url.startsWith("https://via.hypothes.is")) {
                return url;
            }
            return "https://via.hypothes.is/" + url;
        } else {
            if (url.startsWith("https://waps.io") || url.startsWith("http://waps.io")) {
                return url;
            }
            return "https://waps.io/open?c=2" +
                "&u=" + StringHelper.urlEncode(url) +
                "&i=" + user.getId() +
                "&t=" + Learnweb.getInstance().getProperties().getProperty("TRACKER_API_KEY");
        }

    }

    public boolean isStarRatingEnabled() {
        return !getActiveOrganisation().getOption(Option.Resource_Hide_Star_rating);
    }

    public boolean isThumbRatingEnabled() {
        return !getActiveOrganisation().getOption(Option.Resource_Hide_Thumb_rating);
    }

    public boolean isLoggingEnabled() {
        return !getActiveOrganisation().getOption(Option.Privacy_Logging_disabled);
    }

    public boolean isTrackingEnabled() {
        return !getActiveOrganisation().getOption(Option.Privacy_Tracker_disabled);
    }

    public boolean isLanguageSwitchEnabled() {
        return !getActiveOrganisation().getOption(Option.Users_Hide_language_switch);
    }

    public boolean isHideSidebarMenu() {
        return "true".equals(getPreference("HIDE_SIDEBAR"));
    }

    private Organisation getActiveOrganisation() {
        if (null == activeOrganisation) {
            activeOrganisation = Learnweb.getInstance().getOrganisationManager().getOrganisationById(PUBLIC_ORGANISATION_ID);
        }
        return activeOrganisation;
    }

    /**
     * This is a workaround to keep old `hasPermission` param functionality.
     * We need to create our own tag or find another way, this method is called on every page, but it is not needed for pages with `viewAction`
     */
    public void checkAccessPermission(Boolean hasAccess) {
        if (!isLoggedIn() && (hasAccess == null || !hasAccess)) {
            throw new UnauthorizedHttpException();
        } else if (isLoggedIn() && (hasAccess != null && !hasAccess)) {
            throw new ForbiddenHttpException();
        }
    }

    public boolean isGuided() {
        return guided;
    }

    public void setGuided(final boolean guided) {
        this.guided = guided;
    }

    /**
     * Make sure that only admins login to moderator accounts.
     */
    public boolean canLoginToAccount(User targetUser) {
        if (user.isAdmin()) {
            return true;
        }

        if (targetUser.isModerator()) {
            return false;
        }

        if (user.isModerator()) {
            return true;
        }

        return false;
    }
}

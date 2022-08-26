package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;
import org.primefaces.model.menu.BaseMenuModel;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.component.ActiveSubmenu;
import de.l3s.learnweb.component.ActiveSubmenu.Builder;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.util.HasId;
import de.l3s.util.StringHelper;

@Named
@SessionScoped
public class UserBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -8577036953815676943L;
    private static final Logger log = LogManager.getLogger(UserBean.class);

    private int userId = 0;
    private transient User user; // to avoid inconsistencies with the user cache the UserBean does not store the user itself
    private transient User moderatorUser; // in this field we store a moderator account while the moderator is logged in on an other account

    private Locale locale;
    private transient LanguageBundle bundle;
    private transient List<Group> newGroups;
    private boolean cacheShowMessageJoinGroup = true;
    private boolean cacheShowMessageAddResource = true;

    private transient BaseMenuModel sidebarMenuModel;
    private transient Instant sidebarMenuModelUpdate;
    private final HashMap<String, String> anonymousPreferences = new HashMap<>(); // preferences for users who are not logged in

    private transient Organisation activeOrganisation;

    private boolean guided; // indicates that the user has started one of the guides

    @PostConstruct
    public void init() {
        // get preferred language
        locale = Faces.getLocale();
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
        if (user == null && userId != 0) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
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
            Learnweb.dao().getUserDao().save(user);
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

    public LanguageBundle getBundle() {
        if (bundle == null) {
            bundle = LanguageBundle.getBundle(locale);
        }
        return bundle;
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
        //log.debug("set locale " + localeCode);
        locale = getLocaleByLocaleCode(localeCode);

        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getViewRoot() == null) {
            return null;
        }

        return fc.getViewRoot().getViewId() + "?faces-redirect=true&includeViewParams=true";
    }

    private Locale getLocaleByLocaleCode(String localeCode) {
        String languageVariant = getActiveOrganisation().getLanguageVariant();

        return switch (localeCode) {
            case "de" -> new Locale("de", "DE", languageVariant);
            case "en" -> new Locale("en", "UK", languageVariant);
            case "it" -> new Locale("it", "IT", languageVariant);
            case "pt" -> new Locale("pt", "BR", languageVariant);
            case "es" -> new Locale("es", "ES", languageVariant);
            case "uk" -> new Locale("uk", "UA", languageVariant);
            case "xx" -> new Locale("xx"); // only for translation editors
            default -> {
                log.error("Unsupported language: {}", localeCode);
                yield new Locale("en", "UK");
            }
        };
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

        for (Course course : user.getCourses()) {
            if (courseId == course.getId()) {
                return true;
            }
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
     * Returns the css code for the banner image of the active organisation or an empty string if no image is defined.
     */
    public String getBannerImage() {
        String bannerImage = null;
        if (isLoggedIn()) {
            bannerImage = getActiveOrganisation().getBannerImageUrl();
        }

        return StringUtils.firstNonBlank(bannerImage, "/resources/images/learnweb_logo.png");
    }

    public String getBannerLink() {
        if (isLoggedIn()) {
            return "./" + getActiveOrganisation().getWelcomePage();
        } else {
            return "./";
        }
    }

    /**
     * Returns a list of groups that have been created since the last login of this user.
     */
    public List<Group> getNewGroups() {
        if (null == newGroups && getUser() != null) {
            newGroups = Learnweb.dao().getGroupDao().findByCourseIds(HasId.collectIds(getUser().getCourses()), getUser().getLastLoginDate());
        }

        return newGroups;
    }

    public int getNewGroupsCount() {
        return getNewGroups().size();
    }

    public MenuModel getSidebarMenuModel() {
        if (!isLoggedIn()) {
            return null;
        }

        if (null == sidebarMenuModel || sidebarMenuModelUpdate.isBefore(Instant.now().minus(Duration.ofMinutes(10)))) {
            long start = System.currentTimeMillis();
            sidebarMenuModel = createMenuModel(getBundle(), getUser());
            sidebarMenuModelUpdate = Instant.now();
            long elapsedMs = System.currentTimeMillis() - start;

            if (elapsedMs > 20) {
                log.warn("Total time to build menu: {} ms.", elapsedMs);
            }
        }

        return sidebarMenuModel;
    }

    private static BaseMenuModel createMenuModel(LanguageBundle msg, User user) {
        BaseMenuModel model = new BaseMenuModel();

        // My resources
        Builder mySubmenu = ActiveSubmenu.builder()
            .label(msg.getString("myResourcesTitle"))
            .styleClass("guide-my-resources")
            .url("myhome/resources.jsf")
            .addElement(DefaultMenuItem.builder().value(msg.getString("myPrivateResources")).icon("fas fa-folder-minus").url("myhome/resources.jsf").build())
            .addElement(DefaultMenuItem.builder().value(msg.getString("myCommentsTitle")).icon("fas fa-comments").url("myhome/comments.jsf").build())
            .addElement(DefaultMenuItem.builder().value(msg.getString("myTags")).icon("fas fa-tags").url("myhome/tags.jsf").build())
            .addElement(DefaultMenuItem.builder().value(msg.getString("myRatedResourcesTitle")).icon("fas fa-star-half-alt").url("myhome/rated_resources.jsf").build());

        if (!user.getActiveSubmissions().isEmpty()) {
            mySubmenu.addElement(DefaultMenuItem.builder().value(msg.getString("Submission.my_submissions")).icon("fas fa-calendar-check").url("myhome/submission_overview.jsf").build());
        }

        model.getElements().add(mySubmenu.build());

        // My groups
        Builder groupsSubmenuBuilder = ActiveSubmenu.builder().label(msg.getString("myGroups")).url("myhome/groups.jsf").styleClass("guide-my-groups");
        for (Group group : user.getGroups()) {
            Builder gm = ActiveSubmenu.builder().label(group.getTitle()).url("group/overview.jsf?group_id=" + group.getId()).styleClass("ui-menuitem-group");
            gm.addElement(DefaultMenuItem.builder().value(msg.getString("overview")).icon("fas fa-layer-group").url("group/overview.jsf?group_id=" + group.getId()).build());
            gm.addElement(DefaultMenuItem.builder().value(msg.getString("resources")).icon("fas fa-folder-open").url("group/resources.jsf?group_id=" + group.getId()).build());
            gm.addElement(DefaultMenuItem.builder().value(msg.getString("forum")).icon("fas fa-comment-dots").url("group/forum.jsf?group_id=" + group.getId()).build());
            gm.addElement(DefaultMenuItem.builder().value(msg.getString("members")).icon("fas fa-users").url("group/members.jsf?group_id=" + group.getId()).build());
            gm.addElement(DefaultMenuItem.builder().value(msg.getString("options")).icon("fas fa-sliders-h").url("group/options.jsf?group_id=" + group.getId()).build());
            groupsSubmenuBuilder.addElement(gm.build());
        }
        model.getElements().add(groupsSubmenuBuilder.build());

        // Moderator submenu
        if (user.isModerator()) {
            ActiveSubmenu moderatorSubmenu = ActiveSubmenu.builder()
                .label(msg.getString("moderator"))
                .url("moderator.jsf")
                .addElement(DefaultMenuItem.builder().value(msg.getString("send_notification")).icon("fas fa-envelope-open-text").url("admin/notification.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("users")).icon("fas fa-user-friends").url("admin/users.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("courses")).icon("fas fa-graduation-cap").url("admin/courses.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("organisation")).icon("fas fa-university").url("admin/organisation.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("text_analysis")).icon("fas fa-spell-check").url("admin/text_analysis.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("statistics")).icon("fas fa-chart-line").url("admin/statistics.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("transcript")).icon("fas fa-language").url("admin/transcript.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("glossary_dashboard")).icon("fas fa-chart-bar").url("dashboard/glossary.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("Activity.dashboard")).icon("fas fa-chart-line").url("dashboard/activity.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("Tracker.dashboard")).icon("fas fa-mouse-pointer").url("dashboard/tracker.jsf").build())
                .build();
            model.getElements().add(moderatorSubmenu);
        }

        // Admin submenu
        if (user.isAdmin()) {
            ActiveSubmenu adminSubmenu = ActiveSubmenu.builder()
                .label(msg.getString("admin"))
                .url("admin/index.jsf")
                .addElement(DefaultMenuItem.builder().value(msg.getString("organisations")).icon("fas fa-sitemap").url("admin/organisations.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("banlist")).icon("fas fa-ban").url("admin/banlist.jsf").build())
                .addElement(DefaultMenuItem.builder().value("Requests").icon("fas fa-chart-area").url("admin/requests.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("system_tools")).icon("fas fa-tools").url("admin/systemtools.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("announcements")).icon("fas fa-bullhorn").url("admin/announcements.jsf").build())
                .addElement(DefaultMenuItem.builder().value(msg.getString("survey.survey_overview")).icon("fas fa-poll-h").url("survey/surveys.jsf").build())
                .addElement(DefaultMenuItem.builder().value("Status (XML)").icon("fas fa-wave-square").url("status.jsf").build())
                .build();
            model.getElements().add(adminSubmenu);
        }

        return model;
    }

    public void setSidebarMenuModel(final BaseMenuModel sidebarMenuModel) {
        this.sidebarMenuModel = sidebarMenuModel;
    }

    /**
     * Returns true when there is any tooltip message to show.
     */
    public boolean isShowMessageAny() {
        if (!isLoggedIn()) {
            return false;
        }

        String viewId = Faces.getViewId();
        if (viewId.contains("register.xhtml")) { // don't show any tooltips on the registration page
            return false;
        }

        return isShowMessageJoinGroup() || isShowMessageAddResource();
    }

    public boolean isShowMessageJoinGroup() {
        if (cacheShowMessageJoinGroup) { // check until the user has joined a group
            User user = getUser();
            if (null == user) {
                return false;
            }
            cacheShowMessageJoinGroup = getUser().getGroupCount() == 0;
        }
        return cacheShowMessageJoinGroup;
    }

    public boolean isShowMessageJoinGroupInHeader() {
        String viewId = Faces.getViewId();
        if (viewId.contains("groups.xhtml")) {
            return false;
        }

        return isShowMessageJoinGroup();
    }

    public boolean isShowMessageAddResourceInHeader() {
        String viewId = Faces.getViewId();
        if (viewId.contains("overview.xhtml") || viewId.contains("resources.xhtml") || viewId.contains("welcome.xhtml")) {
            return false;
        }

        return isShowMessageAddResource();
    }

    public boolean isShowMessageAddResource() {
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
     * @return Returns the given url proxied through WAPS.io if enabled for the current organisation
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
                "&t=" + getTrackerApiKey();
        }
    }

    public String getTrackerApiKey() {
        if (StringUtils.isNotEmpty(getActiveOrganisation().getTrackerApiKey())) {
            return getActiveOrganisation().getTrackerApiKey();
        }

        return Learnweb.config().getProperty("tracker_api_key");
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
            activeOrganisation = Learnweb.dao().getOrganisationDao().findDefault();
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
        return user.isAdmin();
    }
}
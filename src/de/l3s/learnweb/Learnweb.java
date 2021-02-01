package de.l3s.learnweb;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import com.zaxxer.hikari.HikariDataSource;

import de.l3s.interwebj.client.InterWeb;
import de.l3s.learnweb.dashboard.activity.ActivityDashboardManager;
import de.l3s.learnweb.dashboard.glossary.GlossaryDashboardManager;
import de.l3s.learnweb.dashboard.tracker.TrackerDashboardManager;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.logging.LogManager;
import de.l3s.learnweb.resource.FileManager;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourceMetadataExtractor;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.archive.ArchiveUrlManager;
import de.l3s.learnweb.resource.archive.TimelineManager;
import de.l3s.learnweb.resource.archive.WaybackCapturesLogger;
import de.l3s.learnweb.resource.archive.WaybackUrlManager;
import de.l3s.learnweb.resource.glossary.GlossaryManager;
import de.l3s.learnweb.resource.office.HistoryManager;
import de.l3s.learnweb.resource.search.SearchLogManager;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.learnweb.resource.submission.SubmissionManager;
import de.l3s.learnweb.resource.survey.SurveyManager;
import de.l3s.learnweb.resource.ted.TedManager;
import de.l3s.learnweb.searchhistory.SearchHistoryManager;
import de.l3s.learnweb.user.CourseManager;
import de.l3s.learnweb.user.OrganisationManager;
import de.l3s.learnweb.user.UserManager;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.util.Misc;
import de.l3s.util.PropertiesBundle;
import de.l3s.util.UrlHelper;
import de.l3s.util.email.BounceManager;

public final class Learnweb {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Learnweb.class);

    public static final String SALT_1 = "ff4a9ff19306ee0407cf69d592";
    public static final String SALT_2 = "3a129713cc1b33650816d61450";

    private static Learnweb learnweb;
    private static boolean learnwebIsLoading = false;

    // Manager (Data Access Objects):
    private final ResourceManager resourceManager;
    private final OrganisationManager organisationManager;
    private final CourseManager courseManager;
    private final GroupManager groupManager;
    private final UserManager userManager;
    private final FileManager fileManager;
    private final TedManager tedManager; //For logging transcript actions by users
    private final ArchiveUrlManager archiveUrlManager; //For creating archive pages of resources saved to LearnWeb
    private final TimelineManager timelineManager; //DAO for resource archive versions
    private final SolrClient solrClient;
    private final ResourcePreviewMaker resourcePreviewMaker;
    private final ResourceMetadataExtractor resourceMetadataExtractor;
    private final JobScheduler jobScheduler;
    private final SurveyManager surveyManager;
    private final SubmissionManager submissionManager;
    private final WaybackCapturesLogger waybackCapturesLogger;
    private final SearchLogManager searchLogManager;
    private final WaybackUrlManager waybackUrlManager;
    private final GlossaryManager glossaryManager;
    private final HistoryManager historyManager;
    private final SearchHistoryManager searchHistoryManager;
    private final RequestManager requestManager;
    private final ProtectionManager protectionManager;
    private final BounceManager bounceManager;
    private final LogManager logManager;
    private final AnnouncementsManager announcementsManager;
    private final ActivityDashboardManager activityDashboardManager;
    private final GlossaryDashboardManager glossaryDashboardManager;
    private final TrackerDashboardManager trackerDashboardManager;

    private final InterWeb interweb;

    private PropertiesBundle properties;
    private Connection dbConnection;
    private HikariDataSource dataSource;
    private Jdbi jdbi;
    private String serverUrl;

    private long lastCheck = 0L;

    /**
     * @param serverUrl The servername + contextPath. For the default installation this is: https://learnweb.l3s.uni-hannover.de
     */
    private Learnweb(String serverUrl) throws SQLException {
        loadProperties();

        // load server URL from config file or guess it
        String propertiesServerUrl = properties.getProperty("SERVER_URL");

        if ("auto".equalsIgnoreCase(propertiesServerUrl) && StringUtils.isNotEmpty(serverUrl)) {
            setServerUrl(serverUrl);
        } else if (null != propertiesServerUrl && propertiesServerUrl.startsWith("http")) {
            setServerUrl(propertiesServerUrl);
        } else {
            setServerUrl("https://learnweb.l3s.uni-hannover.de");
            log.error("We could not guess the server name. Will use by default: " + this.serverUrl + "; on Machine: " + Misc.getSystemDescription());
        }

        dataSource = createDataSource();
        dbConnection = dataSource.getConnection(); // TODO: remove old connection methods
        // migrateDatabase();

        interweb = new InterWeb(properties.getProperty("INTERWEBJ_API_URL"), properties.getProperty("INTERWEBJ_API_KEY"), properties.getProperty("INTERWEBJ_API_SECRET"));

        resourceManager = new ResourceManager(this);
        organisationManager = new OrganisationManager(this);
        courseManager = new CourseManager(this);
        groupManager = new GroupManager(this);
        userManager = new UserManager(this);
        fileManager = new FileManager(this);
        solrClient = SolrClient.getInstance(this);
        resourcePreviewMaker = new ResourcePreviewMaker(this);
        resourceMetadataExtractor = new ResourceMetadataExtractor(this);
        tedManager = new TedManager(this);
        archiveUrlManager = ArchiveUrlManager.getInstance(this);
        timelineManager = new TimelineManager(this);
        jobScheduler = new JobScheduler(this);
        waybackCapturesLogger = new WaybackCapturesLogger(this);
        searchLogManager = new SearchLogManager(this);
        surveyManager = new SurveyManager(this);
        submissionManager = new SubmissionManager(this);
        waybackUrlManager = WaybackUrlManager.getInstance(this);
        glossaryManager = new de.l3s.learnweb.resource.glossary.GlossaryManager(this);
        logManager = LogManager.getInstance(this);
        announcementsManager = new AnnouncementsManager(this);
        activityDashboardManager = new ActivityDashboardManager(this);
        glossaryDashboardManager = new GlossaryDashboardManager(this);
        trackerDashboardManager = new TrackerDashboardManager(this);

        historyManager = new HistoryManager(this);
        searchHistoryManager = new SearchHistoryManager(this);

        requestManager = new RequestManager(this);
        protectionManager = new ProtectionManager(this);
        bounceManager = new BounceManager(this);
    }

    private void loadProperties() {
        try {
            Properties fallbackProperties = new Properties();
            InputStream defaultProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb.properties");
            fallbackProperties.load(defaultProperties);
            properties = new PropertiesBundle(fallbackProperties);

            InputStream testProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb_test.properties");
            if (testProperties != null) {
                properties.load(testProperties);
                log.warn("Test properties loaded.");
            }

            InputStream localProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb_local.properties");
            if (localProperties != null) {
                properties.load(localProperties);
                log.warn("Local properties loaded.");
            }
        } catch (IOException e) {
            log.error("Property error", e);
        }
    }

    private HikariDataSource createDataSource() {
        HikariDataSource ds = new HikariDataSource();
        // Configuration docs https://github.com/brettwooldridge/HikariCP
        ds.setDriverClassName("org.mariadb.jdbc.Driver");
        ds.setJdbcUrl(properties.getProperty("mysql_url") + "?log=false");
        ds.setUsername(properties.getProperty("mysql_user"));
        ds.setPassword(properties.getProperty("mysql_password"));
        ds.setMaximumPoolSize(3);
        ds.setConnectionTimeout(60000); // 1 min
        return ds;
    }

    private void migrateDatabase() {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .load();

        flyway.migrate();
    }

    /**
     * initialize stuff which are only required in server mode.
     */
    public void initLearnwebServer() {
        log.debug("Init LearnwebServer");

        // We should run jobScheduler only on one server, otherwise they are conflicting
        boolean isRootInstance = "https://learnweb.l3s.uni-hannover.de".equals(serverUrl);
        if (isRootInstance) {
            jobScheduler.startAllJobs();
            waybackCapturesLogger.start();
        } else {
            log.warn("JobScheduler not started, because this instance is not recognized as the main instance.");
        }
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public OrganisationManager getOrganisationManager() {
        return organisationManager;
    }

    public CourseManager getCourseManager() {
        return courseManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public ResourcePreviewMaker getResourcePreviewMaker() {
        return resourcePreviewMaker;
    }

    public ResourceMetadataExtractor getResourceMetadataExtractor() {
        return resourceMetadataExtractor;
    }

    private void checkConnection() throws SQLException {
        synchronized (dbConnection) {
            // exit if last check was one or less seconds ago
            if (lastCheck > System.currentTimeMillis() - 1000) {
                return;
            }

            if (!dbConnection.isValid(1)) {
                log.warn("Database connection invalid try to reconnect");

                try {
                    dbConnection.close();
                } catch (SQLException ignored) {
                }

                dbConnection = dataSource.getConnection();
            }

            lastCheck = System.currentTimeMillis();
        }
    }

    public Connection getConnection() throws SQLException {
        checkConnection();

        return dbConnection;
    }

    public Jdbi getJdbi() {
        if (jdbi == null) {
            // add configuration and register mappers if needed http://jdbi.org/
            jdbi = Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin());
        }
        return jdbi;
    }

    public Handle openHandle() {
        return getJdbi().open();
    }

    /**
     * Returns an instance of Interweb for the user anonymous.
     */
    public InterWeb getInterweb() {
        return interweb;
    }

    public PropertiesBundle getProperties() {
        return properties;
    }

    /**
     * This method should be called before the system shuts down.
     */
    public void onDestroy() {
        log.info("Shutting down Learnweb");

        jobScheduler.stopAllJobs();
        archiveUrlManager.onDestroy();
        waybackCapturesLogger.stop();
        dataSource.close();

        log.info("Shutdown Learnweb completed");
    }

    /**
     * @return Returns the servername + contextPath. For the default installation this is: https://learnweb.l3s.uni-hannover.de
     */
    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        serverUrl = UrlHelper.removeTrailingSlash(serverUrl);

        if (this.serverUrl != null && this.serverUrl.equals(serverUrl)) {
            return; // ignore new serverUrl
        }

        // enforce HTTPS on the production server
        if (serverUrl.startsWith("http://") && isForceHttps()) {
            log.info("Forcing HTTPS schema.");
            serverUrl = "https://" + serverUrl.substring(7);
        }

        if (fileManager != null) {
            fileManager.setServerUrl(serverUrl);
        }

        this.serverUrl = serverUrl;
        log.debug("Server url updated: {}", serverUrl);
    }

    public SolrClient getSolrClient() {
        return solrClient;
    }

    public TedManager getTedManager() {
        return tedManager;
    }

    public ArchiveUrlManager getArchiveUrlManager() {
        return archiveUrlManager;
    }

    public TimelineManager getTimelineManager() {
        return timelineManager;
    }

    /**
     * You should never call this.
     */
    public void resetCaches() throws SQLException {
        organisationManager.resetCache();
        userManager.resetCache();
        resourceManager.resetCache();
        groupManager.resetCache();
        courseManager.resetCache();
    }

    public WaybackCapturesLogger getWaybackCapturesLogger() {
        return waybackCapturesLogger;
    }

    public SearchLogManager getSearchLogManager() {
        return searchLogManager;
    }

    public WaybackUrlManager getWaybackUrlManager() {
        return waybackUrlManager;
    }

    public SurveyManager getSurveyManager() {
        return surveyManager;
    }

    public SubmissionManager getSubmissionManager() {
        return submissionManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public SearchHistoryManager getSearchHistoryManager() {
        return searchHistoryManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public BounceManager getBounceManager() {
        return bounceManager;
    }

    public GlossaryManager getGlossaryManager() {
        return glossaryManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public AnnouncementsManager getAnnouncementsManager() {
        return announcementsManager;
    }

    public ActivityDashboardManager getActivityDashboardManager() {
        return activityDashboardManager;
    }

    public GlossaryDashboardManager getGlossaryDashboardManager() {
        return glossaryDashboardManager;
    }

    public TrackerDashboardManager getTrackerDashboardManager() {
        return trackerDashboardManager;
    }

    /**
     * Use createInstance() first.
     */
    public static Learnweb getInstance() {
        if (null == learnweb) {
            log.warn("Learnweb is not initialized correctly. You should call createInstance() first", new Exception());
            // throw new RuntimeException("Learnweb is not initialized correctly. Check log files. Or you have to use createInstance(String serverUrl)");
        }
        return learnweb;
    }

    /**
     * The same as getInstance() but as Optional and without logging warnings.
     */
    public static Optional<Learnweb> getInstanceOptional() {
        return Optional.ofNullable(learnweb);
    }

    /**
     * This method will use https://learnweb.l3s.uni-hannover.de as server URL if no other URL is specified in the properties file.
     */
    public static Learnweb createInstance() throws ClassNotFoundException, SQLException {
        return createInstance(null);
    }

    /**
     * This method should be used to create a Learnweb instance.
     *
     * @param serverUrl https://learnweb.l3s.uni-hannover.de or http://localhost:8080/Learnweb-Tomcat
     */
    public static Learnweb createInstance(String serverUrl) throws ClassNotFoundException, SQLException {
        try {
            if (learnweb == null) {
                if (learnwebIsLoading) {
                    log.warn("Learnweb instance requested while it was still loading. Happens mostly because of connection or config problems");

                    return null; // to avoid infinite loop
                }

                learnwebIsLoading = true;
                learnweb = new Learnweb(serverUrl);
                learnwebIsLoading = false;
            } else {
                learnweb.setServerUrl(serverUrl);
            }

            return learnweb;
        } catch (Exception e) {
            learnwebIsLoading = false;
            log.fatal("fatal error: ", e);
            throw e;
        }
    }

    private static boolean isForceHttps() {
        return "true".equalsIgnoreCase(System.getenv("LEARNWEB_FORCE_HTTPS"));
    }
}

package de.l3s.learnweb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.forum.ForumManager;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.group.LinkManager;
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
import de.l3s.learnweb.resource.office.ConverterService;
import de.l3s.learnweb.resource.office.HistoryManager;
import de.l3s.learnweb.resource.search.SearchLogManager;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.learnweb.resource.submission.SubmissionManager;
import de.l3s.learnweb.resource.survey.SurveyManager;
import de.l3s.learnweb.resource.ted.TedManager;
import de.l3s.learnweb.user.CourseManager;
import de.l3s.learnweb.user.OrganisationManager;
import de.l3s.learnweb.user.UserManager;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.searchHistoryTest.SearchHistoryManager;
import de.l3s.searchHistoryTest.SearchSessionEdgeComputator;
import de.l3s.util.Misc;
import de.l3s.util.PropertiesBundle;
import de.l3s.util.email.BounceManager;

public class Learnweb
{
    public final static String salt1 = "ff4a9ff19306ee0407cf69d592";
    public final static String salt2 = "3a129713cc1b33650816d61450";
    private static final Logger log = Logger.getLogger(Learnweb.class);

    private Connection dbConnection;
    private InterWeb interweb;

    private PropertiesBundle properties;
    private String serverUrl;

    // Manager (Data Access Objects):
    private final ForumManager forumManager;
    private final ResourceManager resourceManager;
    private final OrganisationManager organisationManager;
    private final CourseManager courseManager;
    private final GroupManager groupManager;
    private final UserManager userManager;
    private final LinkManager linkManager;
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
    private final GlossaryManager glossaryManager; //new Glossary Manager
    private final HistoryManager historyManager;
    private final SearchHistoryManager searchHistoryManager;
    private final SearchSessionEdgeComputator searchSessionEdgeComputator;
    private final RequestManager requestManager;
    private final ProtectionManager protectionManager;
    private final BounceManager bounceManager;
    private final ConverterService serviceConverter;
    private final LogManager logManager;
    private final AnnouncementsManager announcementsManager;

    private static Learnweb learnweb = null;
    private static boolean learnwebIsLoading = false;
    private static boolean developmentMode = true; //  true if run on Localhost, disables email logger

    /**
     * Use createInstance() first
     *
     * @return
     */
    public static Learnweb getInstance()
    {
        if(null == learnweb)
        {
            log.warn("Learnweb is not initialized correctly. You should call createInstance() first", new Exception());
            // throw new RuntimeException("Learnweb is not initialized correctly. Check log files. Or you have to use createInstance(String serverUrl)");
        }
        return learnweb;
    }

    /**
     * The same as getInstance() but as Optional and without logging warnings.
     * 
     * @return
     */
    public static Optional<Learnweb> getInstanceOptional()
    {
        return Optional.ofNullable(learnweb);
    }

    public static Learnweb createInstance() throws ClassNotFoundException, SQLException
    {
        return createInstance(null);
    }

    /**
     * This method should be used to create a Learnweb instance
     *
     * @param serverUrl https://learnweb.l3s.uni-hannover.de or http://localhost:8080/Learnweb-Tomcat
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static Learnweb createInstance(String serverUrl) throws ClassNotFoundException, SQLException
    {
        try
        {
            if(learnweb == null)
            {
                if(learnwebIsLoading)
                {
                    log.warn("Learnweb instance requested while it was still loading. Happens mostly because of connection or config problems");

                    return null; // to avoid infinite loop
                }

                learnweb = new Learnweb(serverUrl);
            }
            else
                learnweb.setServerUrl(serverUrl);

            return learnweb;
        }
        catch(Exception e)
        {
            learnwebIsLoading = false;
            log.fatal("fatal error: ", e);
            throw e;
        }
    }

    private void loadProperties()
    {
        // TODO: retrieve mode from web.xml configuration
        String workingDirectory = new File(".").getAbsolutePath();
        log.debug("workingDirectory: " + workingDirectory);
        if(workingDirectory.startsWith("/home/learnweb_user"))
        {
            developmentMode = false;
        }

        try
        {
            Properties fallbackProperties = new Properties();
            InputStream defaultProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb.properties");
            fallbackProperties.load(defaultProperties);
            properties = new PropertiesBundle(fallbackProperties);

            InputStream localProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb_local.properties");
            if(localProperties != null)
            {
                properties.load(localProperties);
                log.debug("Local properties loaded.");
            }

            InputStream testProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb_test.properties");
            if(testProperties != null)
            {
                properties.load(testProperties);
                log.debug("Test properties loaded.");
            }
        }
        catch(IOException e)
        {
            log.error("Property error", e);
        }
    }

    /**
     *
     * @param serverUrl The servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private Learnweb(String serverUrl) throws ClassNotFoundException, SQLException
    {
        learnwebIsLoading = true;

        loadProperties();

        // load server URL from config file or guess it
        String propertiesServerUrl = properties.getProperty("SERVER_URL");

        if("auto".equalsIgnoreCase(propertiesServerUrl) && StringUtils.isNotEmpty(serverUrl))
        {
            setServerUrl(serverUrl);
        }
        else if(null != propertiesServerUrl && propertiesServerUrl.startsWith("http"))
        {
            setServerUrl(propertiesServerUrl);
        }
        else
        {
            setServerUrl("https://learnweb.l3s.uni-hannover.de");
            log.error("We could not guess the server name. Will use by default: " + this.serverUrl + "; on Machine: " + Misc.getSystemDescription());
        }

        Class.forName("org.mariadb.jdbc.Driver");
        connect();

        interweb = new InterWeb(properties.getProperty("INTERWEBJ_API_URL"), properties.getProperty("INTERWEBJ_API_KEY"), properties.getProperty("INTERWEBJ_API_SECRET"));

        resourceManager = new ResourceManager(this);
        forumManager = new ForumManager(this);
        organisationManager = new OrganisationManager(this);
        courseManager = new CourseManager(this);
        groupManager = new GroupManager(this);
        userManager = new UserManager(this);
        linkManager = new LinkManager(this);
        fileManager = new FileManager(this);
        solrClient = SolrClient.getInstance(this);
        resourcePreviewMaker = new ResourcePreviewMaker(this);
        resourceMetadataExtractor = new ResourceMetadataExtractor(this);
        serviceConverter = new ConverterService(this);
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

        learnwebIsLoading = false;

        historyManager = new HistoryManager(this);
        searchHistoryManager = new SearchHistoryManager(this);
        searchSessionEdgeComputator = SearchSessionEdgeComputator.getInstance(this);

        //Managers added by Kate
        requestManager = RequestManager.getInstance(this);
        protectionManager = new ProtectionManager(this);
        bounceManager = new BounceManager(this);

    }

    /**
     * initialize stuff which are only required in server mode
     */
    public void initLearnwebServer()
    {
        log.debug("Init LearnwebServer");

        if(!isInDevelopmentMode())
            jobScheduler.startAllJobs();
        else
            log.debug("JobScheduler not started; development mode=" + isInDevelopmentMode());
    }

    public FileManager getFileManager()
    {
        return fileManager;
    }

    public ResourceManager getResourceManager()
    {
        return resourceManager;
    }

    public OrganisationManager getOrganisationManager()
    {
        return organisationManager;
    }

    public CourseManager getCourseManager()
    {
        return courseManager;
    }

    public GroupManager getGroupManager()
    {
        return groupManager;
    }

    public UserManager getUserManager()
    {
        return userManager;
    }

    public LinkManager getLinkManager()
    {
        return linkManager;
    }

    public ResourcePreviewMaker getResourcePreviewMaker()
    {
        return resourcePreviewMaker;
    }

    public ResourceMetadataExtractor getResourceMetadataExtractor()
    {
        return resourceMetadataExtractor;
    }

    private void connect() throws SQLException
    {
        dbConnection = DriverManager.getConnection(properties.getProperty("mysql_url") + "?log=false", properties.getProperty("mysql_user"), properties.getProperty("mysql_password"));
        dbConnection.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'ONLY_FULL_GROUP_BY', '')");
    }

    private long lastCheck = 0L;

    private void checkConnection() throws SQLException
    {
        synchronized(dbConnection)
        {
            // exit if last check was one or less seconds ago
            if(lastCheck > System.currentTimeMillis() - 1000)
                return;

            if(!dbConnection.isValid(1))
            {
                log.warn("Database connection invalid try to reconnect");

                try
                {
                    dbConnection.close();
                }
                catch(SQLException e)
                {
                }

                connect();
            }

            lastCheck = System.currentTimeMillis();
        }
    }

    /**
     * Returns an instance of Interweb for the user anonymous
     *
     * @return
     */
    public InterWeb getInterweb()
    {
        return interweb;
    }

    public PropertiesBundle getProperties()
    {
        return properties;
    }

    /**
     * This method should be called before the system shuts down
     */
    public void onDestroy()
    {
        log.info("Shutting down Learnweb");

        jobScheduler.stopAllJobs();
        archiveUrlManager.onDestroy();
        waybackCapturesLogger.stop();
        searchLogManager.stop();
        logManager.onDestroy();

        try
        {
            dbConnection.close();
        }
        catch(SQLException e)
        {
        } // ignore

        log.info("Shutdown Learnweb completed");
    }

    //should be used instead of the static method
    public Connection getConnection() throws SQLException
    {
        checkConnection();

        return dbConnection;
    }

    /**
     *
     * @return Returns the servername + contextpath. For the default installation this is: https://learnweb.l3s.uni-hannover.de
     */
    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        if(this.serverUrl == null)
            this.serverUrl = serverUrl;
        else if(this.serverUrl.startsWith("http") || this.serverUrl.equals(serverUrl))
            return; // ignore new serverUrl

        this.serverUrl = serverUrl;

        // enforce HTTPS on the production server
        if(serverUrl.startsWith("http://") && !Learnweb.isInDevelopmentMode())
            this.serverUrl = "https://" + serverUrl.substring(7);

        if(fileManager != null)
            fileManager.setServerUrl(serverUrl);

        log.debug("Server base url updated: " + serverUrl);
    }

    public SolrClient getSolrClient()
    {
        return solrClient;
    }

    public TedManager getTedManager()
    {
        return tedManager;
    }

    public ArchiveUrlManager getArchiveUrlManager()
    {
        return archiveUrlManager;
    }

    public TimelineManager getTimelineManager()
    {
        return timelineManager;
    }

    public ForumManager getForumManager()
    {
        return forumManager;
    }

    /**
     * You should never call this
     *
     * @throws SQLException
     */
    public void resetCaches() throws SQLException
    {
        organisationManager.resetCache();
        userManager.resetCache();
        resourceManager.resetCache();
        groupManager.resetCache();
        courseManager.resetCache();
    }

    public WaybackCapturesLogger getWaybackCapturesLogger()
    {
        return waybackCapturesLogger;
    }

    /**
     *
     * @return true if it is not run on the Learnweb server
     */
    public static boolean isInDevelopmentMode()
    {
        return developmentMode;
    }

    public SearchLogManager getSearchLogManager()
    {
        return searchLogManager;
    }

    public WaybackUrlManager getWaybackUrlManager()
    {
        return waybackUrlManager;
    }

    public SurveyManager getSurveyManager()
    {
        return surveyManager;
    }

    public ConverterService getConverterService()
    {
        return serviceConverter;
    }

    public SubmissionManager getSubmissionManager()
    {
        return submissionManager;
    }

    public HistoryManager getHistoryManager()
    {
        return historyManager;
    }

    public SearchHistoryManager getSearchHistoryManager()
    {
        return searchHistoryManager;
    }

    public SearchSessionEdgeComputator getSearchSessionEdgeComputator()
    {
        return searchSessionEdgeComputator;
    }

    public ProtectionManager getProtectionManager()
    {
        return protectionManager;
    }

    public RequestManager getRequestManager()
    {
        return requestManager;
    }

    public BounceManager getBounceManager()
    {
        return bounceManager;
    }

    public GlossaryManager getGlossaryManager()
    {
        return glossaryManager;
    }

    public LogManager getLogManager()
    {
        return logManager;
    }

    public AnnouncementsManager getAnnouncementsManager()
    {
        return announcementsManager;
    }
}

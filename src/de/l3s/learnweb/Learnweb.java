package de.l3s.learnweb;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.dashboard.ActivityDashboardManager;
import de.l3s.learnweb.dashboard.DashboardManager;
import de.l3s.learnweb.forum.ForumManager;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.group.LinkManager;
import de.l3s.learnweb.logging.LogManager;
import de.l3s.learnweb.resource.FileManager;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourceMetadataExtractor;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.glossary.GlossaryManager;
import de.l3s.learnweb.resource.office.ConverterService;
import de.l3s.learnweb.resource.office.HistoryManager;
import de.l3s.learnweb.resource.peerAssessment.PeerAssessmentManager;
import de.l3s.learnweb.resource.search.SearchLogManager;
import de.l3s.learnweb.resource.search.SuggestionLogger;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.learnweb.resource.submission.SubmissionManager;
import de.l3s.learnweb.resource.survey.SurveyManager;
import de.l3s.learnweb.resource.ted.TedManager;
import de.l3s.learnweb.resource.yellMetadata.AudienceManager;
import de.l3s.learnweb.resource.yellMetadata.CategoryManager;
import de.l3s.learnweb.resource.yellMetadata.ExtendedMetadataManager;
import de.l3s.learnweb.resource.yellMetadata.LangLevelManager;
import de.l3s.learnweb.resource.yellMetadata.PurposeManager;
import de.l3s.learnweb.user.CourseManager;
import de.l3s.learnweb.user.OrganisationManager;
import de.l3s.learnweb.user.UserManager;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.searchHistoryTest.SearchHistoryManager;
import de.l3s.searchHistoryTest.SearchSessionEdgeComputator;
import de.l3s.util.PropertiesBundle;
import de.l3s.util.email.BounceManager;

public class Learnweb
{
    public final static String salt1 = "ff4a9ff19306ee0407cf69d592";
    public final static String salt2 = "3a129713cc1b33650816d61450";
    private final static Logger log = Logger.getLogger(Learnweb.class);

    private Connection dbConnection;
    private InterWeb interweb;

    private PropertiesBundle properties;
    private String serverUrl;
    private String secureServerUrl;

    // list of Learnweb installations
    public enum SERVICE
    {
        LEARNWEB,
        AMA
    };

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
    private final MementoClient mementoClient;
    private final SolrClient solrClient;
    private final LoroManager loroManager;
    private final ResourcePreviewMaker resourcePreviewMaker;
    private final ResourceMetadataExtractor resourceMetadataExtractor;
    private final JobScheduler jobScheduler;
    private final GlossaryManager glossariesManager; //old Manager
    private final SuggestionLogger suggestionLogger;
    private final SurveyManager surveyManager;
    private final SubmissionManager submissionManager;
    private final WaybackCapturesLogger waybackCapturesLogger;
    private final SearchLogManager searchLogManager;
    private final WaybackUrlManager waybackUrlManager;
    private final de.l3s.learnweb.resource.glossaryNew.GlossaryManager glossaryManager; //new Glossary Manager
    private final HistoryManager historyManager;
    private final SearchHistoryManager searchHistoryManager;
    private final SearchSessionEdgeComputator searchSessionEdgeComputator;
    private final RequestManager requestManager;
    private final ProtectionManager protectionManager;
    private final BounceManager bounceManager;
    private final DashboardManager dashboardManager;
    private final ConverterService serviceConverter;
    private final LogManager logManager;
    private final PeerAssessmentManager peerAssessmentManager;
    private final ActivityDashboardManager activityDashboardManager;

    //added by Chloe
    private final AudienceManager audienceManager;
    private final CategoryManager categoryManager;
    private final ExtendedMetadataManager extendedMetadataManager;
    private final LangLevelManager langLevelManager;
    private final PurposeManager purposeManager;

    private static Learnweb learnweb = null;
    private static boolean learnwebIsLoading = false;
    private static boolean developmentMode = true; //  true if run on Localhost, disables email logger
    private final SERVICE service; // describes whether this instance runs for Learnweb or AMA

    /**
     * Use createInstance() first
     *
     * @return
     */
    public static Learnweb getInstance()
    {
        if(null == learnweb)
        {
            //log.warn("Learnweb is not initialized correctly. You should call createInstance() first", new Exception());

            try
            {
                ServletContext servletContext = (ServletContext) UtilBean.getExternalContext().getContext();

                return createInstance(servletContext.getContextPath());
            }
            catch(Exception e)
            {
                throw new RuntimeException("Learnweb is not initialized correctly. Check log files. Or you have to use createInstance(String serverUrl)");
            }
        }
        return learnweb;
    }

    /**
     * This method should be used to create a Learnweb instance
     *
     * @param serverUrl http://learnweb.l3s.uni-hannover.de or http://localhost:8080/Learnweb-Tomcat
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

    /**
     * Returns the properties file to use depending on the machine Learnweb is running on.
     *
     * @return
     */
    private static String getPropertiesFileName()
    {
        String propertiesFileName = "lw_local_other";

        String workingDirectory = new File(".").getAbsolutePath();
        log.debug("workingDirectory: " + workingDirectory);

        // if you need to override values in learnweb.properties file for local testing, do it in a separate properties file and add it here:
        if(workingDirectory.startsWith("/home/learnweb_user"))
        {
            propertiesFileName = "learnweb";
            developmentMode = false;
        }
        else if(workingDirectory.startsWith("/home/ama_user"))
        {
            propertiesFileName = "ama";
            developmentMode = false;
        }
        else if((new File("/Users/chloe0502/Documents/workspace/learnweb/learnwebFiles")).exists())
            propertiesFileName = "lw_local_chloe";
        else if((new File("C:\\programmieren\\philipp.ama")).exists())
            propertiesFileName = "ama_local_philipp";
        else if((new File("C:\\programmieren\\philipp.lw")).exists())
            propertiesFileName = "lw_local_philipp";
        else if((new File("C:\\programmieren\\philipp_uni.txt")).exists())
            propertiesFileName = "lw_local_philipp_uni";
        else if((new File("/home/fernando/trevor.txt").exists()))
            propertiesFileName = "lw_local_trevor_uni";
        else if((new File("/Users/trevor").exists()))
            propertiesFileName = "lw_local_trevor";
        else if((new File("/home/kalyani").exists()))
            propertiesFileName = "lw_local_rishita";
        else if(new File("/Users/Rishita/").exists())
            propertiesFileName = "lw_local_rishita";
        else if((new File("C:\\Users\\Tetiana").exists()))
            propertiesFileName = "lw_local_tetiana";
        else if((new File("C:\\Users\\astappev").exists()))
            propertiesFileName = "lw_local_oleh";
        else if((new File("/Users/user").exists()))
            propertiesFileName = "lw_local_luyan";
        else if((new File("F:\\workspace\\lwresources").exists()))
            propertiesFileName = "lw_local_mariia";
        else if((new File("D:\\DevEnv\\Learnweb_resources").exists()))
            propertiesFileName = "lw_local_kateryna";
        else
            developmentMode = false;

        return propertiesFileName;
    }

    /**
     *
     * @param guessedServerUrl The servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private Learnweb(String guessedServerUrl) throws ClassNotFoundException, SQLException
    {
        learnwebIsLoading = true;

        try
        {
            Properties fallbackProperties = new Properties();
            fallbackProperties.load(getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb.properties"));

            this.properties = new PropertiesBundle(fallbackProperties);

            String propertiesFileName = getPropertiesFileName();
            log.debug("Load config file: " + propertiesFileName);

            properties.load(getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/" + propertiesFileName + ".properties"));
        }
        catch(IOException e)
        {
            log.error("Property error", e);
        }

        // load server URL from config file or guess it
        String propertiesServerUrl = properties.getProperty("SERVER_URL");

        if(null == propertiesServerUrl || propertiesServerUrl.startsWith("/"))
        {
            setServerUrl("http://learnweb.l3s.uni-hannover.de");
            log.error("You haven't provided an absolute base server url; Will use by default: " + this.serverUrl);
        }
        else if(propertiesServerUrl.startsWith("http"))
            setServerUrl(propertiesServerUrl);
        else if(propertiesServerUrl.equalsIgnoreCase("auto") && StringUtils.isNotEmpty(guessedServerUrl))
            setServerUrl(guessedServerUrl);
        else
        {
            setServerUrl("https://learnweb.l3s.uni-hannover.de");
            log.error("We could not guess the server name. Will use by default: " + this.serverUrl);
        }

        Class.forName("org.mariadb.jdbc.Driver");
        connect();

        service = SERVICE.valueOf(properties.getProperty("SERVICE"));
        if(service == null)
            throw new IllegalArgumentException("invalid property: SERVICE=" + properties.getProperty("SERVICE"));

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
        mementoClient = new MementoClient(this);
        loroManager = new LoroManager(this);
        jobScheduler = new JobScheduler(this);
        suggestionLogger = new SuggestionLogger(this);
        waybackCapturesLogger = new WaybackCapturesLogger(this);
        glossariesManager = new GlossaryManager(this); //old Manager
        searchLogManager = new SearchLogManager(this);
        surveyManager = new SurveyManager(this);
        submissionManager = new SubmissionManager(this);
        waybackUrlManager = WaybackUrlManager.getInstance(this);
        dashboardManager = DashboardManager.getInstance(this);
        peerAssessmentManager = new PeerAssessmentManager(this);
        activityDashboardManager = new ActivityDashboardManager(this);
        glossaryManager = new de.l3s.learnweb.resource.glossaryNew.GlossaryManager(this);
        logManager = LogManager.getInstance(this);

        learnwebIsLoading = false;

        historyManager = new HistoryManager(this);
        searchHistoryManager = new SearchHistoryManager(this);
        searchSessionEdgeComputator = SearchSessionEdgeComputator.getInstance(this);

        //new managers added by Chloe
        audienceManager = new AudienceManager(this);
        categoryManager = new CategoryManager(this);
        extendedMetadataManager = new ExtendedMetadataManager(this);
        langLevelManager = new LangLevelManager(this);
        purposeManager = new PurposeManager(this);

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

        if(!isInDevelopmentMode() || getService() != SERVICE.LEARNWEB)
            jobScheduler.startAllJobs();
        else
            log.debug("JobScheduler not started for service=" + SERVICE.LEARNWEB + "; development mode=" + isInDevelopmentMode());
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

    //getters and setters for newly added managers (Chloe)

    public AudienceManager getAudienceManager()
    {
        return audienceManager;
    }

    public CategoryManager getCategoryManager()
    {
        return categoryManager;
    }

    public ExtendedMetadataManager getExtendedMetadataManager()
    {
        return extendedMetadataManager;
    }

    public LangLevelManager getLangLevelManager()
    {
        return langLevelManager;
    }

    public PurposeManager getPurposeManager()
    {
        return purposeManager;
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
        suggestionLogger.stop();
        waybackCapturesLogger.stop();
        searchLogManager.stop();
        logManager.onDestroy();

        try
        {
            dbConnection.close();
            //querydbConnection.close();
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
     * @return Returns the servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     */
    public String getServerUrl()
    {
        return serverUrl;
    }

    /**
     *
     * @return Returns the HTTPS version of getServerUrl(). Returns HTTP if Learnweb is run in development mode.
     *         For the default installation this is: https://learnweb.l3s.uni-hannover.de
     */
    public String getSecureServerUrl()
    {
        return secureServerUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        if(this.serverUrl == null)
            this.serverUrl = serverUrl;
        else if(this.serverUrl.startsWith("http") || this.serverUrl.equals(serverUrl))
            return; // ignore new serverUrl

        this.serverUrl = serverUrl;

        if(serverUrl.startsWith("http://") && !Learnweb.isInDevelopmentMode())
            this.secureServerUrl = "https://" + serverUrl.substring(7);
        else
            this.secureServerUrl = this.serverUrl;

        if(fileManager != null)
            fileManager.setServerUrl(secureServerUrl);

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

    public LoroManager getLoroManager()
    {
        return loroManager;
    }

    public ArchiveUrlManager getArchiveUrlManager()
    {
        return archiveUrlManager;
    }

    public MementoClient getMementoClient()
    {
        return mementoClient;
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

    public SuggestionLogger getSuggestionLogger()
    {
        return suggestionLogger;
    }

    public WaybackCapturesLogger getWaybackCapturesLogger()
    {
        return waybackCapturesLogger;
    }

    public GlossaryManager getGlossariesManager()
    {
        return glossariesManager;
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

    /**
     * describes whether this instance runs for Learnweb or AMA
     *
     * @return
     */
    public SERVICE getService()
    {
        return service;
    }

    public DashboardManager getDashboardManager()
    {
        return dashboardManager;
    }

    public PeerAssessmentManager getPeerAssessmentManager()
    {
        return peerAssessmentManager;
    }

    public de.l3s.learnweb.resource.glossaryNew.GlossaryManager getGlossaryManager()
    {
        return glossaryManager;
    }

    public ActivityDashboardManager getActivityDashboardManager()
    {
        return activityDashboardManager;
    }

    public LogManager getLogManager()
    {
        return logManager;
    }
}

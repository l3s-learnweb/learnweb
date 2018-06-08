package de.l3s.learnweb;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import de.l3s.learnweb.user.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.dashboard.DashboardManager;
import de.l3s.learnweb.forum.ForumManager;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.group.LinkManager;
import de.l3s.learnweb.group.SummaryOverview;
import de.l3s.learnweb.resource.FileManager;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourceMetadataExtractor;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.glossary.GlossaryManager;
import de.l3s.learnweb.resource.peerAssessment.PeerAssessmentManager;
import de.l3s.learnweb.resource.search.SearchLogManager;
import de.l3s.learnweb.resource.search.SuggestionLogger;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.learnweb.resource.submission.SubmissionManager;
import de.l3s.learnweb.resource.survey.SurveyManager;
import de.l3s.learnweb.resource.survey.createSurveyManager;
import de.l3s.learnweb.resource.ted.TedManager;
import de.l3s.learnweb.resource.yellMetadata.AudienceManager;
import de.l3s.learnweb.resource.yellMetadata.CategoryManager;
import de.l3s.learnweb.resource.yellMetadata.ExtendedMetadataManager;
import de.l3s.learnweb.resource.yellMetadata.LanglevelManager;
import de.l3s.learnweb.resource.yellMetadata.PurposeManager;
import de.l3s.learnweb.user.loginProtection.FrequencyProtectionManager;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.office.ConverterService;
import de.l3s.office.HistoryManager;
import de.l3s.searchHistoryTest.SearchHistoryManager;
import de.l3s.searchHistoryTest.SearchSessionEdgeComputator;
import de.l3s.searchlogclient.SearchLogClient;
import de.l3s.util.PropertiesBundle;
import de.l3s.util.StringHelper;

public class Learnweb
{
    public final static String salt1 = "ff4a9ff19306ee0407cf69d592";
    public final static String salt2 = "3a129713cc1b33650816d61450";
    private final static Logger log = Logger.getLogger(Learnweb.class);

    private Connection dbConnection;
    private InterWeb interweb;

    private PreparedStatement pstmtLog;
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
    private final SearchLogClient searchlogClient; //For Search History Tools
    private final MementoClient mementoClient;
    private final SolrClient solrClient;
    private final LoroManager loroManager;
    private final ResourcePreviewMaker resourcePreviewMaker;
    private final ResourceMetadataExtractor resourceMetadataExtractor;
    private final JobScheduler jobScheduler;
    private final GlossaryManager glossariesManager;
    private final SuggestionLogger suggestionLogger;
    private final SurveyManager surveyManager;
    private final SubmissionManager submissionManager;
    private final createSurveyManager createSurveyManager;
    private final WaybackCapturesLogger waybackCapturesLogger;
    private final SearchLogManager searchLogManager;
    private final WaybackUrlManager waybackUrlManager;

    private final HistoryManager historyManager;
    private final SearchHistoryManager searchHistoryManager;
    private final SearchSessionEdgeComputator searchSessionEdgeComputator;
    private final ProtectionManager protectionManager;
    private final RequestManager requestManager;
    private final DashboardManager dashboardManager;

    //added by Chloe
    private final AudienceManager audienceManager;
    private final CategoryManager categoryManager;
    private final ExtendedMetadataManager extendedMetadataManager;
    private final LanglevelManager langlevelManager;
    private final PurposeManager purposeManager;
    private final ConverterService serviceConverter;

    private static Learnweb learnweb = null;
    private static boolean learnwebIsLoading = false;
    private static boolean developmentMode = true; //  true if run on Localhost, disables email logger
    private final SERVICE service; // describes whether this instance runs for Learnweb or AMA
    private PeerAssessmentManager peerAssessmentManager;

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
    private static String getPropteriesFileName()
    {
        String propteriesFileName = "lw_local_other";

        String workingDirectory = new File(".").getAbsolutePath();
        log.debug("workingDirectory: " + workingDirectory);

        // if you need to override values in learnweb.properties file for local testing, do it in a separate properties file and add it here:
        if(workingDirectory.startsWith("/home/learnweb_user"))
        {
            propteriesFileName = "learnweb";
            developmentMode = false;
        }
        else if(workingDirectory.startsWith("/home/ama_user"))
        {
            propteriesFileName = "ama";
            developmentMode = false;
        }
        else if((new File("/Users/chloe0502/Documents/workspace/learnweb/learnwebFiles")).exists())
            propteriesFileName = "lw_local_chloe";
        else if((new File("C:\\programmieren\\philipp.txt")).exists())
            propteriesFileName = "lw_local_philipp";
        else if((new File("C:\\programmieren\\philipp_uni.txt")).exists())
            propteriesFileName = "lw_local_philipp_uni";
        else if((new File("/home/fernando/trevor.txt").exists()))
            propteriesFileName = "lw_local_trevor_uni";
        else if((new File("/Users/trevor").exists()))
            propteriesFileName = "lw_local_trevor";
        else if((new File("/home/kalyani").exists()))
            propteriesFileName = "lw_local_rishita";
        else if(new File("/Users/Rishita/").exists())
            propteriesFileName = "lw_local_rishita";
        else if((new File("C:\\Users\\Tetiana").exists()))
            propteriesFileName = "lw_local_tetiana";
        else if((new File("C:\\Users\\astappev").exists()))
            propteriesFileName = "lw_local_oleh";
        else if((new File("/Users/user").exists()))
            propteriesFileName = "lw_local_luyan";
        else if((new File("F:\\workspace\\lwresources").exists()))
            propteriesFileName = "lw_local_mariia";
        else if((new File("C:\\Users\\Kate\\Documents\\LEARNWEB").exists()))
            propteriesFileName = "lw_local_kate";
        else
            developmentMode = false;

        return propteriesFileName;
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

            String propteriesFileName = getPropteriesFileName();
            log.debug("Load config file: " + propteriesFileName);

            properties.load(getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/" + propteriesFileName + ".properties"));
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
            throw new IllegalArgumentException("invalid propertie: SERVICE=" + properties.getProperty("SERVICE"));

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
        searchlogClient = new SearchLogClient(this);
        tedManager = new TedManager(this);
        archiveUrlManager = ArchiveUrlManager.getInstance(this);
        timelineManager = new TimelineManager(this);
        mementoClient = new MementoClient(this);
        loroManager = new LoroManager(this);
        jobScheduler = new JobScheduler(this);
        suggestionLogger = new SuggestionLogger(this);
        waybackCapturesLogger = new WaybackCapturesLogger(this);
        glossariesManager = new GlossaryManager(this);
        searchLogManager = new SearchLogManager(this);
        surveyManager = new SurveyManager(this);
        submissionManager = new SubmissionManager(this);
        createSurveyManager = new createSurveyManager(this);
        waybackUrlManager = WaybackUrlManager.getInstance(this);
        dashboardManager = DashboardManager.getInstance(this);
        peerAssessmentManager = new PeerAssessmentManager(this);

        learnwebIsLoading = false;

        historyManager = new HistoryManager(this);
        searchHistoryManager = new SearchHistoryManager(this);
        searchSessionEdgeComputator = SearchSessionEdgeComputator.getInstance(this);

        //new managers added by Chloe
        audienceManager = new AudienceManager(this);
        categoryManager = new CategoryManager(this);
        extendedMetadataManager = new ExtendedMetadataManager(this);
        langlevelManager = new LanglevelManager(this);
        purposeManager = new PurposeManager(this);

        //Managers added by Kate
        protectionManager = new FrequencyProtectionManager(this);
        requestManager = RequestManager.getInstance(this);
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
        // ?useUnicode=true
        dbConnection = DriverManager.getConnection(properties.getProperty("mysql_url") + "?log=false", properties.getProperty("mysql_user"), properties.getProperty("mysql_password"));
        dbConnection.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'ONLY_FULL_GROUP_BY', '')");

        pstmtLog = dbConnection.prepareStatement("INSERT DELAYED INTO `lw_user_log` (`user_id`, `session_id`, `action`, `target_id`, `params`, `group_id`, timestamp, execution_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

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

    public LanglevelManager getLanglevelManager()
    {
        return langlevelManager;
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

        try
        {
            if(logBatchSize > 0)
            {
                pstmtLog.executeBatch();
                logBatchSize = 0;
            }
        }
        catch(Exception e)
        {
            log.warn(e);
        }

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

    private int logBatchSize = 0;

    /**
     * @param user
     * @param action
     * @param groupId the group this action belongs to; null if no group
     * @param targetId optional value; should be 0 if not required
     * @param params
     * @param sessionId
     * @param executionTime how long did the action need to execute (in milliseconds)
     */
    public void log(User user, LogEntry.Action action, int groupId, int targetId, String params, String sessionId, int executionTime)
    {
        int userId = 0;
        if (user != null) {
            userId = user.getId();

            if (user.getOrganisation().getOption(Organisation.Option.Misc_Logging_disabled)) {
                // TODO Oleh: Should we disable it completely or anonymize it?
                log.warn("Log ignored.");
                return;
            }
        }

        if(groupId == -1)
            groupId = (null == user) ? 0 : user.getActiveGroupId();

        log(userId, action, groupId, targetId, params, sessionId, executionTime);
    }

    /**
     * Logs a user action. The parameters "targetId" and "params" depend on the
     * logged action. Look at the code of LogEntry.Action for explanation.
     *
     * @param userId
     * @param action
     * @param groupId the group this action belongs to; null if no group
     * @param targetId optional value; should be 0 if not required
     * @param params
     * @param sessionId
     * @param executionTime how long did the action need to execute (in milliseconds)
     */
    private void log(int userId, LogEntry.Action action, int groupId, int targetId, String params, String sessionId, int executionTime)
    {
        if(null == action)
            throw new IllegalArgumentException();

        params = StringHelper.shortnString(params, 250);

        try
        {
            checkConnection();

            synchronized(pstmtLog)
            {
                pstmtLog.setInt(1, userId);
                pstmtLog.setString(2, sessionId);
                pstmtLog.setInt(3, action.ordinal());
                pstmtLog.setInt(4, targetId);
                pstmtLog.setString(5, params);
                pstmtLog.setInt(6, groupId);
                pstmtLog.setTimestamp(7, new Timestamp(new Date().getTime()));
                pstmtLog.setInt(8, executionTime);
                pstmtLog.addBatch();

                logBatchSize++;

                if(logBatchSize > 10)
                {
                    pstmtLog.executeBatch();
                    logBatchSize = 0;
                }
            }
        }
        catch(SQLException e)
        {
            log.error("Can't store log entry: " + action + "; Target: " + targetId + "; User: " + userId, e);
        }
    }

    private final static String LOG_SELECT = "SELECT user_id, u.username, action, target_id, params, timestamp, ul.group_id, r.title AS resource_title, g.title AS group_title, u.image_file_id FROM lw_user_log ul JOIN lw_user u USING(user_id) LEFT JOIN lw_resource r ON action IN(0,1,2,3,15,14,19,21,32,11,54,55,6,8) AND target_id = r.resource_id LEFT JOIN lw_group g ON ul.group_id = g.group_id";
    private final static Action[] LOG_DEFAULT_FILTER = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description,
            Action.group_changing_leader, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource,
            Action.group_deleting_link, Action.changing_resource, Action.forum_post_added, Action.forum_reply_message };

    /**
     *
     * @param userId
     * @param actions if actions is null the default filter is used
     * @param limit
     * @param limit if limit is -1 all log entrys are returned
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getLogsByUser(int userId, Action[] actions, int limit) throws SQLException
    {
        LinkedList<LogEntry> log = new LinkedList<LogEntry>();

        if(null == actions)
            actions = LOG_DEFAULT_FILTER;

        StringBuilder sb = new StringBuilder();
        for(Action action : actions)
        {
            sb.append(",");
            sb.append(action.ordinal());
        }
        PreparedStatement select = getConnection().prepareStatement(LOG_SELECT + " WHERE user_id = ? AND action IN(" + sb.toString().substring(1) + ") ORDER BY timestamp DESC LIMIT " + limit);
        select.setInt(1, userId);

        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            log.add(new LogEntry(rs));
        }
        select.close();

        return log;
    }

    /**
     *
     * @param groupId
     * @param actions if actions is null the default filter is used
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getLogsByGroup(int groupId, LogEntry.Action[] actions) throws SQLException
    {
        return getLogsByGroup(groupId, actions, -1);
    }

    /**
     *
     * @param groupId
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entrys are returned
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getLogsByGroup(int groupId, LogEntry.Action[] actions, int limit) throws SQLException
    {
        LinkedList<LogEntry> log = new LinkedList<LogEntry>();

        if(null == actions)
            actions = LOG_DEFAULT_FILTER;

        StringBuilder sb = new StringBuilder();
        for(Action action : actions)
        {
            sb.append(",");
            sb.append(action.ordinal());
        }

        String limitStr = "";
        if(limit > 0)
            limitStr = "LIMIT " + limit;

        try(PreparedStatement select = getConnection().prepareStatement(LOG_SELECT + " WHERE ul.group_id = ? AND user_id != 0 AND action IN(" + sb.toString().substring(1) + ") ORDER BY timestamp DESC " + limitStr))
        {
            select.setInt(1, groupId);

            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                log.add(new LogEntry(rs));
            }
        }
        return log;
    }

    public SummaryOverview getLogsByGroup(int groupId, List<Action> actions, LocalDateTime from, LocalDateTime to) throws SQLException
    {
        SummaryOverview summary = null;
        String actionsString = actions.stream()
                .map(a -> String.valueOf(a.ordinal()))
                .collect(Collectors.joining(","));
        try(PreparedStatement select = getConnection().prepareStatement(
                LOG_SELECT + " WHERE ul.group_id = ? AND user_id != 0 AND action IN(" + actionsString + ") and timestamp between ? AND ? ORDER BY timestamp DESC "))
        {
            select.setInt(1, groupId);
            select.setTimestamp(2, Timestamp.valueOf(from));
            select.setTimestamp(3, Timestamp.valueOf(to));
            ResultSet rs = select.executeQuery();
            if(rs.next())
            {
                summary = new SummaryOverview();
            }
            while(rs.next())
            {
                LogEntry logEntry = new LogEntry(rs);
                switch(logEntry.getAction())
                {
                case deleting_resource:
                    summary.getDeletedResources().add(logEntry);
                    break;
                case adding_resource:
                    Resource resource = logEntry.getResource();
                    if(resource != null)
                    {
                        summary.getAddedResources().add(logEntry);
                    }
                    break;
                case forum_post_added:
                case forum_reply_message:
                    summary.getForumsInfo().add(logEntry);
                    break;
                case group_joining:
                case group_leaving:
                    summary.getMembersInfo().add(logEntry);
                    break;
                case changing_resource:
                    Resource logEntryResource = logEntry.getResource();
                    if(logEntryResource != null)
                    {
                        if(summary.getUpdatedResources().keySet().contains(logEntryResource))
                        {
                            summary.getUpdatedResources().get(logEntryResource).add(logEntry);
                        }
                        else
                        {
                            summary.getUpdatedResources().put(logEntryResource, new LinkedList<>(Arrays.asList(logEntry)));
                        }
                    }
                    break;
                default:
                    break;
                }
            }
        }
        return summary;
    }

    public List<LogEntry> getActivityLogOfUserGroups(int userId, LogEntry.Action[] actions, int limit) throws SQLException
    {
        LinkedList<LogEntry> log = new LinkedList<LogEntry>();

        if(null == actions)
            actions = LOG_DEFAULT_FILTER;

        StringBuilder sb = new StringBuilder();
        for(Action action : actions)
        {
            sb.append(",");
            sb.append(action.ordinal());
        }

        String limitStr = "";
        if(limit > 0)
            limitStr = "LIMIT " + limit;

        PreparedStatement select = getConnection().prepareStatement(LOG_SELECT + " WHERE ul.group_id IN(SELECT group_id FROM lw_group_user WHERE user_id=?) AND user_id != 0 AND user_id!=? AND action IN(" + sb.toString().substring(1) + ") ORDER BY timestamp DESC " + limitStr);
        select.setInt(1, userId);
        select.setInt(2, userId);

        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            log.add(new LogEntry(rs));
        }
        select.close();

        return log;
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

    public SearchLogClient getSearchlogClient()
    {
        return searchlogClient;
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

    public static void main(String[] args)
    {
        Learnweb.getInstance();
    }

    public GlossaryManager getGlossariesManager()
    {
        return glossariesManager;
    }

    /**
     *
     * @return true if it is not run onthe Learnweb server
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

    public createSurveyManager getCreateSurveyManager()
    {
        return createSurveyManager;
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

    public void setPeerAssessmentManager(PeerAssessmentManager peerAssessmentManager)
    {
        this.peerAssessmentManager = peerAssessmentManager;
    }

}

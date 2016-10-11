package de.l3s.learnweb;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.solrClient.SolrClient;
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

    private PreparedStatement pstmtGetChangeLog;
    private PreparedStatement pstmtLog;
    private PropertiesBundle properties;
    private String contextUrl;

    // Manager (Data Access Objects):

    private final ForumManager forumManager;
    private final ResourceManager resourceManager;
    private final PresentationManager presentationManager;
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
    private final YovistoManager yovistoManager;
    private final JobScheduler jobScheduler;
    private final GlossaryManager glossaryManager;
    private final SuggestionLogger suggestionLogger;
    private final WaybackCapturesLogger waybackCapturesLogger;

    private static Learnweb learnweb = null;
    private static boolean learnwebIsLoading = false;

    /**
     * The Same as getInstanceRaw() but hides all exceptions
     * 
     * @return
     */
    public static Learnweb getInstance()
    {
	try
	{
	    return getInstanceRaw();
	}
	catch(Exception e)
	{
	    throw new RuntimeException(e);
	}
    }

    /**
     * In comparison to getInstance() this method throws exceptions
     * 
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static Learnweb getInstanceRaw() throws ClassNotFoundException, SQLException
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

		learnweb = new Learnweb();
	    }
	    return learnweb;
	}
	catch(Exception e)
	{
	    learnwebIsLoading = false;
	    //learnweb = null;
	    log.fatal(e);
	    throw e;
	}
    }

    /**
     * Returns the properties file to use depending on the machine Learnweb is running on.
     * 
     * @return
     */
    public static String getPropteriesFileName()
    {
	String propteriesFileName = "lw_local_other";

	// if you need to override values in learnweb.properties file for local testing, do it in a separate properties file and add it here:
	if((new File("/home/learnweb_user")).exists())
	    propteriesFileName = "learnweb";
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
	else if((new File("C:\\Users\\astappev").exists()))
	    propteriesFileName = "lw_local_oleg";

	return propteriesFileName;
    }

    /**
     * 
     * @param contextUrl The servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     * 
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private Learnweb() throws ClassNotFoundException, SQLException
    {
	learnwebIsLoading = true;
	contextUrl = "http://learnweb.l3s.uni-hannover.de";
	//learnweb = this;

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

	Class.forName("org.mariadb.jdbc.Driver");
	connect();

	interweb = new InterWeb(properties.getProperty("INTERWEBJ_API_URL"), properties.getProperty("INTERWEBJ_API_KEY"), properties.getProperty("INTERWEBJ_API_SECRET"));

	resourceManager = new ResourceManager(this);
	presentationManager = new PresentationManager(this);
	forumManager = new ForumManager(this);
	organisationManager = new OrganisationManager(this);
	courseManager = new CourseManager(this);
	groupManager = new GroupManager(this);
	userManager = new UserManager(this);
	linkManager = new LinkManager(this);
	fileManager = new FileManager(this);
	solrClient = SolrClient.getInstance(this);
	resourcePreviewMaker = new ResourcePreviewMaker(this);
	searchlogClient = new SearchLogClient(this);
	tedManager = new TedManager(this);
	archiveUrlManager = ArchiveUrlManager.getInstance(this);
	timelineManager = new TimelineManager(this);
	mementoClient = new MementoClient(this);
	loroManager = new LoroManager(this);
	jobScheduler = new JobScheduler(this);
	yovistoManager = new YovistoManager(this);
	glossaryManager = new GlossaryManager(this);
	suggestionLogger = new SuggestionLogger(this);
	waybackCapturesLogger = new WaybackCapturesLogger(this);

	learnwebIsLoading = false;
    }

    /**
     * initialize stuff which are only required in server mode
     */
    public void initLearnwebServer()
    {
	log.debug("Init LearnwebServer");

	if(getContextUrl().equalsIgnoreCase("http://learnweb.l3s.uni-hannover.de"))
	    jobScheduler.startAllJobs();
	else
	    log.debug("JobScheduler not started for context: " + getContextUrl());
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

    private void connect() throws SQLException
    {
	dbConnection = DriverManager.getConnection(properties.getProperty("mysql_url"), properties.getProperty("mysql_user"), properties.getProperty("mysql_password"));

	dbConnection.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'ONLY_FULL_GROUP_BY', '')");
	pstmtLog = dbConnection.prepareStatement("INSERT DELAYED INTO `lw_user_log` (`user_id`, `session_id`, `action`, `target_id`, `params`, `group_id`, timestamp, execution_time, client_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 2)");
	pstmtGetChangeLog = dbConnection.prepareStatement("SELECT * FROM  `admin_change_log` ORDER BY  `admin_change_log`.`log_entry_num` DESC LIMIT 0 , 30");
    }

    private long lastCheck = 0L;

    private void checkConnection() throws SQLException
    {
	// exit if last check was two or less seconds ago
	if(lastCheck > System.currentTimeMillis() - 2000)
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
	    e.printStackTrace();
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

    /**
     * Will be deprecated in the future
     * 
     * @return
     * @throws SQLException
     */
    public static Connection getConnectionStatic() throws SQLException
    {
	Learnweb lw = getInstance();
	lw.checkConnection();

	return lw.dbConnection;
    }

    //should be used instead of the static method
    public Connection getConnection() throws SQLException
    {
	checkConnection();

	return dbConnection;
    }

    private int logBatchSize = 0;

    /**
     * Logs a user action. The parameters "targetId" and "params" depend on the
     * logged action. Look at the code of LogEntry.Action for explanation.
     * 
     * @param user
     * @param action
     * @param targetId optional value; should be 0 if not required
     * @param group the group this action belongs to; null if no group
     * @param params
     * @param sessionId
     * @param executionTime in milliseconds
     * @throws SQLException
     */
    public void log(User user, LogEntry.Action action, int targetId, String params, String sessionId, int executionTime)
    {
	log(user, action, -1, targetId, params, sessionId, executionTime);
    }

    public void log(User user, LogEntry.Action action, int groupId, int targetId, String params, String sessionId, int executionTime)
    {
	if(null == action)
	    throw new IllegalArgumentException();

	params = StringHelper.shortnString(params, 250);

	int userId = (null == user) ? 0 : user.getId();

	if(groupId == -1)
	    groupId = (null == user) ? 0 : user.getActiveGroupId();

	synchronized(pstmtLog)
	{
	    try
	    {
		checkConnection();

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

		if(logBatchSize > 0)
		{
		    pstmtLog.executeBatch();
		    logBatchSize = 0;
		}
	    }
	    catch(SQLException e)
	    {
		log.error("Can't store log entry: " + action + "; Target: " + targetId + "; User: " + userId, e);
	    }
	}
    }

    private final static String LOG_SELECT = "SELECT user_id, u.username, action, target_id, params, timestamp, ul.group_id, r.title AS resource_title, g.title AS group_title, u.image_file_id FROM lw_user_log ul JOIN lw_user u USING(user_id) LEFT JOIN lw_resource r ON action IN(0,1,2,3,15,19,21,32) AND target_id = r.resource_id LEFT JOIN lw_group g ON ul.group_id = g.group_id";
    private final static Action[] LOG_DEFAULT_FILTER = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description,
	    Action.group_changing_leader, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource,
	    Action.group_deleting_link };

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

	checkConnection();

	if(null == actions)
	    actions = LOG_DEFAULT_FILTER;

	StringBuilder sb = new StringBuilder();
	for(Action action : actions)
	{
	    sb.append(",");
	    sb.append(action.ordinal());
	}
	PreparedStatement select = dbConnection.prepareStatement(LOG_SELECT + " WHERE user_id = ? AND action IN(" + sb.toString().substring(1) + ") ORDER BY timestamp DESC LIMIT " + limit);
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

	checkConnection();

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

	PreparedStatement select = dbConnection.prepareStatement(LOG_SELECT + " WHERE ul.group_id = ? AND user_id != 0 AND action IN(" + sb.toString().substring(1) + ") ORDER BY timestamp DESC " + limitStr);
	select.setInt(1, groupId);

	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    log.add(new LogEntry(rs));
	}
	select.close();

	return log;
    }

    public List<LogEntry> getActivityLogOfUserGroups(int userId, LogEntry.Action[] actions, int limit) throws SQLException
    {
	LinkedList<LogEntry> log = new LinkedList<LogEntry>();

	checkConnection();

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

	PreparedStatement select = dbConnection.prepareStatement(LOG_SELECT + " WHERE ul.group_id IN(SELECT group_id FROM lw_group_user WHERE user_id=?) AND user_id != 0 AND user_id!=? AND action IN(" + sb.toString().substring(1) + ") ORDER BY timestamp DESC " + limitStr);
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

    private String adminMessage;

    public String getAdminMessage() throws SQLException
    {
	return adminMessage;
    }

    public void setAdminMessage(String adminMessage)
    {
	this.adminMessage = adminMessage;
    }

    public List<String> getChangeLog() throws SQLException
    {
	checkConnection();
	List<String> messages = new LinkedList<String>();
	ResultSet rs = pstmtGetChangeLog.executeQuery();
	String msg = null;
	while(rs.next())
	{
	    java.sql.Timestamp ts = rs.getTimestamp(3);
	    msg = rs.getString(2);
	    msg = ts.toString() + " : " + msg;
	    messages.add(msg);
	}
	return messages;
    }

    /**
     * 
     * @return Returns the servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     */
    public String getContextUrl()
    {
	return contextUrl; // because we don't use httpS we can cache the url, change it if you want to use httpS too
    }

    /**
     * This has to be set as soon as possible.
     * servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     * 
     * @param contextUrl
     * @throws SQLException
     */
    public void setContextUrl(String contextUrl)
    {
	if(null == contextUrl)
	    throw new IllegalArgumentException("contextUrl must no be null");

	fileManager.setContextUrl(contextUrl);

	this.contextUrl = contextUrl;
    }

    public PresentationManager getPresentationManager()
    {
	return presentationManager;
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

    public YovistoManager getYovistoManager()
    {
	return yovistoManager;
    }

    public GlossaryManager getGlossaryManager()
    {
	return glossaryManager;
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
}

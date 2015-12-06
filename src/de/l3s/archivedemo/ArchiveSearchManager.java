package de.l3s.archivedemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.archiveSearch.QueryCompletor;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.util.PropertiesBundle;

public class ArchiveSearchManager
{
    private final static Logger log = Logger.getLogger(ArchiveSearchManager.class);
    private final static String QUERY_COLUMNS = "query_id, query_string, timestamp, loaded_results, requested_results";

    private PropertiesBundle properties;
    private Connection dbConnection;
    private long lastCheck;
    private SimpleDateFormat waybackDateFormat;
    private QueryCompletor queryCompletorEnglish;
    private QueryCompletor queryCompletorGerman;

    public ArchiveSearchManager(Learnweb learnweb) throws SQLException
    {
	this.properties = learnweb.getProperties();

	this.dbConnection = DriverManager.getConnection(properties.getProperty("mysql_archive_url"), properties.getProperty("mysql_archive_user"), properties.getProperty("mysql_archive_password"));
	this.waybackDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");

	//loadQueryCompletor();
    }

    /**
     * loads the query completors asynchronously to memory
     */
    public void loadQueryCompletor()
    {
	new Thread()
	{
	    @Override
	    public void run()
	    {
		queryCompletorEnglish = new QueryCompletor("de/l3s/archiveSearch/main_pages_en.csv");
		log.debug("Loaded english entities");
		queryCompletorGerman = new QueryCompletor("de/l3s/archiveSearch/main_pages_de.csv");
		log.debug("Loaded german entities");
	    }
	}.start();

    }

    public List<String> getQueryCompletionsFromDB(String market, String query, int count) throws SQLException
    {
	String table = "main_pages_" + market.substring(0, 2);

	List<String> suggestions = new ArrayList<String>(count);
	PreparedStatement select = getConnection().prepareStatement("SELECT title FROM `" + table + "` WHERE title LIKE ? ORDER BY views DESC LIMIT ?");
	select.setString(1, query + "%");
	select.setInt(2, count);
	ResultSet rs = select.executeQuery();

	while(rs.next())
	{
	    suggestions.add(rs.getString(1));
	}
	select.close();
	return suggestions;
    }

    public List<String> getQueryCompletionsFromMem(String market, String query, int count) throws SQLException
    {
	// when the index isn't loaded yet use database
	//if(null == queryCompletorEnglish)
	//    return getQueryCompletionsFromDB(market, query, count);

	if(market.startsWith("en") && null != queryCompletorEnglish)
	    return queryCompletorEnglish.getSuggestions(query, count);
	else if(market.startsWith("de") && null != queryCompletorGerman)
	    return queryCompletorGerman.getSuggestions(query, count);

	return getQueryCompletionsFromDB(market, query, count);
    }

    public List<String> getQuerySuggestionsWikiLink(String market, String query, int count) throws SQLException
    {
	ArrayList<String> suggestions = new ArrayList<String>(count);
	PreparedStatement select = getConnection().prepareStatement("SELECT query_id FROM `pw_query` WHERE market = ? AND `query_string` LIKE ?");
	select.setString(1, market);
	select.setString(2, query);
	ResultSet rs = select.executeQuery();
	int queryId = 0;
	if(rs.next())
	    queryId = rs.getInt(1);
	select.close();

	PreparedStatement select_suggestions = getConnection().prepareStatement("SELECT query_string FROM `pw_query_suggestions` WHERE query_id = ?");
	select_suggestions.setInt(1, queryId);
	ResultSet rs_suggest = select_suggestions.executeQuery();
	while(rs_suggest.next())
	{
	    suggestions.add(rs_suggest.getString(1));
	}
	select_suggestions.close();
	return suggestions;
    }

    /**
     * 
     * @param market
     * @param queryString
     * @return the query id of this entity or -1 if queryString is no entity
     * @throws SQLException
     */
    public int getQueryIdByEntity(String market, String queryString) throws SQLException
    {
	String table = "main_pages_" + market.substring(0, 2);
	int queryId = -1;

	PreparedStatement select = getConnection().prepareStatement("SELECT query_id FROM `" + table + "` WHERE title = ?");
	select.setString(1, queryString);
	ResultSet rs = select.executeQuery();
	if(rs.next())
	{
	    queryId = rs.getInt(1);
	}

	return queryId;
    }

    public Query getQueryByQueryString(String market, String queryString) throws SQLException
    {
	Query query = null;
	PreparedStatement select = getConnection().prepareStatement("SELECT " + QUERY_COLUMNS + " FROM pw_query WHERE market LIKE ? AND query_string LIKE ? ORDER BY loaded_results DESC LIMIT 1");
	select.setString(1, market.substring(0, 2) + "%");
	select.setString(2, queryString);
	log.debug(select);
	ResultSet rs = select.executeQuery();
	if(rs.next())
	{
	    query = createQuery(rs);
	}

	select.close();
	return query;
    }
    /*
    public Query getQueryByQueryId(int queryId) throws SQLException
    {
    	Query query = null;
    	PreparedStatement select = getConnection().prepareStatement("SELECT " + QUERY_COLUMNS + " FROM pw_query WHERE market = ? AND query_string = ? ORDER BY loaded_results DESC LIMIT 1");
    	select.setInt(1, market);
    	ResultSet rs = select.executeQuery();
    	if(rs.next())
    	{
    	    query = createQuery(rs);
    	}
    
    	select.close();
    	return query;
    }*/

    private Query createQuery(ResultSet rs) throws SQLException
    {
	Query query = new Query();
	query.setId(rs.getInt("query_id"));
	query.setQueryString(rs.getString("query_string"));
	query.setTimestamp(new Date(rs.getTimestamp("timestamp").getTime()));
	query.setLoadedResultCount(rs.getInt("loaded_results"));
	query.setRequestedResultCount(rs.getInt("requested_results"));

	return query;
    }

    public void saveQuery(Query query) throws SQLException
    {
	PreparedStatement replace = getConnection().prepareStatement("REPLACE INTO `pw_query` (query_id, query_string, results, loaded_results, requested_results, type, market, disable_query_alteration, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
		Statement.RETURN_GENERATED_KEYS);

	if(query.getId() < 0) // the tag is not yet stored at the database
	    replace.setNull(1, java.sql.Types.INTEGER);
	else
	    replace.setInt(1, query.getId());
	replace.setString(2, query.getQueryString());
	replace.setInt(3, query.getEstimatedResultCount());
	replace.setInt(4, query.getLoadedResultCount());
	replace.setInt(5, query.getRequestedResultCount());
	replace.setString(6, "web");
	replace.setString(7, query.getMarket());
	replace.setString(8, Boolean.toString(query.isDisableQueryAlterations()));
	replace.setTimestamp(9, new Timestamp(query.getTimestamp().getTime()));
	replace.executeUpdate();

	if(query.getId() < 0) // get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    query.setId(rs.getInt(1));
	}

	replace.close();
    }

    private String formatDate(Timestamp timestamp)
    {
	if(timestamp == null || timestamp.getTime() == 0L)
	    return "";

	return waybackDateFormat.format(new Date(timestamp.getTime()));
    }

    public List<ResourceDecorator> getResultsByQueryId(int queryId) throws SQLException
    {
	List<ResourceDecorator> results = new ArrayList<ResourceDecorator>();

	PreparedStatement select = getConnection()
		.prepareStatement("SELECT `rank`, `url_captures`, `first_timestamp`, `last_timestamp`, url, title, description, UNIX_TIMESTAMP(crawl_time) as crawl_time2 FROM pw_result LEFT JOIN `url_captures_count_2` USING (query_id, rank) WHERE `query_id` = ? ORDER BY rank");
	select.setInt(1, queryId);
	ResultSet rs = select.executeQuery();

	while(rs.next())
	{
	    String url = rs.getString("url");

	    Resource resource = new Resource();
	    resource.setUrl(url);
	    resource.setDescription(rs.getString("description"));
	    resource.setTitle(rs.getString("title"));
	    resource.setMetadataValue("query_id", Integer.toString(queryId));
	    resource.setMetadataValue("url_captures", rs.getString("url_captures"));
	    resource.setMetadataValue("first_timestamp", formatDate(rs.getTimestamp("first_timestamp")));
	    resource.setMetadataValue("last_timestamp", formatDate(rs.getTimestamp("last_timestamp")));
	    resource.setMetadataValue("crawl_time", rs.getString("crawl_time2"));

	    ResourceDecorator decoratedResource = new ResourceDecorator(resource);
	    decoratedResource.setSnippet(rs.getString("description"));
	    decoratedResource.setRankAtService(rs.getInt("rank"));
	    decoratedResource.setTitle(rs.getString("title"));
	    results.add(decoratedResource);
	}

	select.close();

	return results;
    }

    private void prefetchResults(String market, int page) throws SQLException
    {
	Calendar minCrawlTime = Calendar.getInstance();
	minCrawlTime.add(Calendar.DATE, -60);
	final CDXClient cdxClient = new CDXClient(minCrawlTime.getTime());
	final int resultsToFetch = 10;

	// statistic counters
	int checkedResources = 0;
	int checkedEntities = 0;
	int limit = 100;

	PreparedStatement select = getConnection().prepareStatement("SELECT query_id FROM `main_pages_" + market + "` ORDER BY `main_pages_en`.`views` DESC LIMIT " + (page * limit) + "," + limit);
	ResultSet rs = select.executeQuery();

	log.info("start check");
	while(rs.next())
	{
	    int queryId = rs.getInt(1);
	    int archivedResources = 0;
	    List<ResourceDecorator> results = getResultsByQueryId(queryId);

	    for(ResourceDecorator resource : results)
	    {
		checkedResources++;

		if(cdxClient.isArchived(resource))
		    archivedResources++;

		if(cdxClient.getWaybackAPIerrors() > 0)
		{
		    sleep(9000);
		    cdxClient.resetAPICounters();
		}

		if(archivedResources == resultsToFetch) // have found enough archived resources for this entity -> continue with next entity
		    break;

		sleep(20);
	    }

	    checkedEntities++;
	    log.info("query_id: " + queryId + "; entity: " + checkedEntities + "; checked Resources: " + checkedResources);

	}

	log.info("checked Resources: " + checkedResources + "; avg: " + (checkedResources / checkedEntities));
    }

    private static void sleep(long millis)
    {
	try
	{
	    Thread.sleep(millis);
	}
	catch(InterruptedException e)
	{
	}
    }

    public static void main(String[] args) throws SQLException
    {
	for(int page = 609; page < 10000; page++)
	{
	    log.info("page: " + page);
	    Learnweb.getInstance().getArchiveSearchManager().prefetchResults("en", page);
	}
    }

    private void checkConnection() throws SQLException
    {
	// exit if last check was two or less seconds ago
	if(lastCheck > System.currentTimeMillis() - 2000)
	    return;

	if(!dbConnection.isValid(1))
	{
	    System.err.println("Database connection invalid try to reconnect");

	    try
	    {
		dbConnection.close();
	    }
	    catch(SQLException e)
	    {
	    }

	    dbConnection = DriverManager.getConnection(properties.getProperty("mysql_archive_url"), properties.getProperty("mysql_archive_user"), properties.getProperty("mysql_archive_password"));

	}

	lastCheck = System.currentTimeMillis();
    }

    public Connection getConnection() throws SQLException
    {
	checkConnection();

	return dbConnection;
    }

    public void cacheCaptureCount(int queryId, int rank, Date firstCapture, Date lastCapture, int captures) throws SQLException
    {
	//log.debug("cacheCaptureCount" + queryId + ", " + rank + ", " + firstCapture);

	PreparedStatement insert = getConnection().prepareStatement("REPLACE DELAYED INTO `archive_bing_big`.`url_captures_count_2` (`query_id`, `rank`, `url_captures`, `first_timestamp`, `last_timestamp`, `crawl_time`) VALUES (?,?,?,?,?,?)");
	insert.setInt(1, queryId);
	insert.setInt(2, rank);
	insert.setInt(3, captures);
	insert.setTimestamp(4, firstCapture == null ? null : new Timestamp(firstCapture.getTime()));
	insert.setTimestamp(5, lastCapture == null ? null : new Timestamp(lastCapture.getTime()));
	insert.setTimestamp(6, new Timestamp(new Date().getTime()));
	insert.executeUpdate();
    }

    public void insertResult(int queryId, int rank, String title, String description, String url) throws SQLException
    {
	//log.debug("cacheCaptureCount" + queryId + ", " + rank + ", " + firstCapture);

	PreparedStatement insert = getConnection().prepareStatement("INSERT DELAYED INTO `pw_result` (`query_id`, `rank`, `title`, description, url) VALUES (?,?,?,?,?)");
	insert.setInt(1, queryId);
	insert.setInt(2, rank);
	insert.setString(3, title);
	insert.setString(4, description);
	insert.setString(5, url);
	insert.executeUpdate();
    }

    public void logQuery(String queryString, String sessionId, String language)
    {
	if(queryString.length() > 255)
	    queryString = queryString.substring(0, 255);

	try
	{
	    PreparedStatement insert = getConnection().prepareStatement("INSERT DELAYED INTO `log_search` (`query`, `session_id`, `language`) VALUES(?, ?, ?)");
	    insert.setString(1, queryString);
	    insert.setString(2, sessionId);
	    insert.setString(3, language);
	    insert.executeUpdate();
	}
	catch(SQLException e)
	{
	    log.fatal("Can't log query: " + queryString, e);
	}
    }

    /**
     * Log every click on a related entity
     * 
     * @param queryString
     * @param sessionId
     * @param language
     * @param relatedEntity
     * @param rank
     * @param method The method which was used to created this related entity suggestion
     */
    public void logRelatedEntityClick(String queryString, String sessionId, String language, String relatedEntity, int rank, String method)
    {
	if(queryString.length() > 255)
	    queryString = queryString.substring(0, 255);
	if(relatedEntity.length() > 255)
	    queryString = relatedEntity.substring(0, 255);

	try
	{
	    PreparedStatement insert = getConnection().prepareStatement("INSERT DELAYED INTO `log_related` (`query`, `session_id`, `language`, related_entity, rank, method) VALUES(?, ?, ?, ?, ?, ?)");
	    insert.setString(1, queryString);
	    insert.setString(2, sessionId);
	    insert.setString(3, language);
	    insert.setString(4, relatedEntity);
	    insert.setInt(5, rank);
	    insert.setString(6, method);
	    insert.executeUpdate();
	}
	catch(SQLException e)
	{
	    log.fatal("Can't log related entity for: " + queryString + "; " + relatedEntity, e);
	}
    }

    public void logClick(String queryString, String language, int rank, int type, String sessionId)
    {
	try
	{
	    PreparedStatement insert = getConnection().prepareStatement("INSERT DELAYED INTO `log_click` (query, language, `rank`, `type`, `session_id`) VALUES(?, ?, ?, ?, ?)");
	    insert.setString(1, queryString);
	    insert.setString(2, language);
	    insert.setInt(3, rank);
	    insert.setInt(4, type);
	    insert.setString(5, sessionId);
	    insert.executeUpdate();
	}
	catch(SQLException e)
	{
	    log.fatal("Can't log click", e);
	}
    }
}

package de.l3s.archivedemo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.util.PropertiesBundle;

public class ArchiveSearchManager
{
    public static Logger log = Logger.getLogger(ArchiveSearchManager.class);
    private PropertiesBundle properties;
    private Connection dbConnection;
    private long lastCheck;
    private HttpSolrServer solr;

    public ArchiveSearchManager(Learnweb learnweb) throws SQLException
    {
	this.properties = learnweb.getProperties();

	this.dbConnection = DriverManager.getConnection(properties.getProperty("mysql_archive_url"), properties.getProperty("mysql_archive_user"), properties.getProperty("mysql_archive_password"));
	solr = new HttpSolrServer("http://prometheus.kbs.uni-hannover.de:8984/solr/WebpageIndex");

    }

    public List<String> getQueryCompletions(String query, int count) throws SQLException
    {
	ArrayList<String> suggestions = new ArrayList<String>(count);
	PreparedStatement select = getConnection().prepareStatement("SELECT DISTINCT query_string FROM `pw_query` WHERE `query_string` LIKE ? AND loaded_results > 0 LIMIT ?");
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

    @SuppressWarnings("unchecked")
    public List<String> getQuerySuggestions(String query, int count) throws SQLException, SolrServerException, IOException
    {
	SolrQuery solrQuery = new SolrQuery();
	solrQuery.setQuery(query);
	solrQuery.setFields("query_id");
	solrQuery.setStart(0);

	QueryResponse response = solr.query(solrQuery);
	SolrDocumentList results = response.getResults();
	Set<Long> set = new HashSet<Long>();
	for(int i = 0; i < results.size(); i++)
	{
	    set.add(((ArrayList<Long>) results.get(i).get("query_id")).get(0));
	    if(set.size() == count)
		break;
	}

	ArrayList<String> suggestions = new ArrayList<String>();

	PreparedStatement select = getConnection().prepareStatement("SELECT query_string FROM pw_query WHERE query_id = ?");

	Iterator<Long> it = set.iterator();
	while(it.hasNext())
	{
	    select.setInt(1, it.next().intValue());
	    ResultSet rs = select.executeQuery();
	    if(rs.next())
	    {
		suggestions.add(rs.getString(1));
	    }
	}
	select.close();

	return suggestions;
    }

    private final static String QUERY_COLUMNS = "query_id, query_string, timestamp";

    public Query getQueryByQueryString(String queryString) throws SQLException
    {
	Query query = null;
	PreparedStatement select = getConnection().prepareStatement("SELECT " + QUERY_COLUMNS + " FROM pw_query WHERE query_string = ? ORDER BY loaded_results DESC LIMIT 1");
	select.setString(1, queryString);
	ResultSet rs = select.executeQuery();
	if(rs.next())
	{
	    query = createQuery(rs);
	}

	select.close();
	return query;
    }

    private Query createQuery(ResultSet rs) throws SQLException
    {
	Query query = new Query();
	query.setId(rs.getInt("query_id"));
	query.setQueryString(rs.getString("query_string"));
	query.setTimestamp(new Date(rs.getTimestamp("timestamp").getTime()));

	return query;
    }

    private String formatDate(Timestamp timestamp, DateFormat df)
    {
	if(timestamp == null || timestamp.getTime() == 0L)
	    return "01.01.70";

	return df.format(new Date(timestamp.getTime()));
    }

    public List<ResourceDecorator> getResultsByQueryId(int queryId) throws SQLException
    {
	List<ResourceDecorator> results = new LinkedList<ResourceDecorator>();
	DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");//DateFormat.getDateInstance(DateFormat.SHORT);//, UtilBean.getUserBean().getLocale());

	PreparedStatement select = getConnection().prepareStatement(
		"SELECT `rank`, `url_captures`, `first_timestamp`, `last_timestamp`, url, title, description FROM `url_captures_count_2` JOIN pw_result USING (query_id, rank) WHERE `query_id` = ? and url_captures > 0 ORDER BY rank");
	select.setInt(1, queryId);
	ResultSet rs = select.executeQuery();
	int counter = 1;
	while(rs.next())
	{
	    Resource resource = new Resource();
	    resource.setUrl(rs.getString("url"));
	    resource.setDescription(rs.getString("description"));
	    resource.setTitle(rs.getString("title"));
	    resource.setMetadataValue("rank", rs.getString("rank"));
	    resource.setMetadataValue("url_captures", rs.getString("url_captures"));
	    resource.setMetadataValue("first_timestamp", formatDate(rs.getTimestamp("first_timestamp"), df));
	    resource.setMetadataValue("last_timestamp", formatDate(rs.getTimestamp("last_timestamp"), df));

	    //	    System.out.println(resource.getMetadataValue("rank") + " - " + resource.getTitle() + " - " + resource.getMetadataValue("last_timestamp"));

	    ResourceDecorator decoratedResource = new ResourceDecorator(resource);
	    decoratedResource.setTempId(counter++);
	    decoratedResource.setSnippet(rs.getString("description"));

	    results.add(decoratedResource);
	}

	select.close();

	return results;
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

    public static void main(String[] args) throws Exception
    {

	ArchiveSearchManager lm = Learnweb.getInstance().getArchiveSearchManager();

	lm.getResultsByQueryId(65158);

	System.exit(0);
    }

}

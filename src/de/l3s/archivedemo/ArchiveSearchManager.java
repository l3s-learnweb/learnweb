package de.l3s.archivedemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.PropertiesBundle;

public class ArchiveSearchManager
{
    public static Logger log = Logger.getLogger(ArchiveSearchManager.class);
    private PropertiesBundle properties;
    private Connection dbConnection;
    private long lastCheck;

    public ArchiveSearchManager(Learnweb learnweb) throws SQLException
    {
	this.properties = learnweb.getProperties();

	this.dbConnection = DriverManager.getConnection(properties.getProperty("mysql_archive_url"), properties.getProperty("mysql_archive_user"), properties.getProperty("mysql_archive_password"));

    }

    public List<String> getQuerySuggestions(String query, int count) throws SQLException
    {
	ArrayList<String> suggestions = new ArrayList<String>(count);
	PreparedStatement select = getConnection().prepareStatement("SELECT DISTINCT query_string FROM `pw_query` WHERE `query_string` LIKE ? LIMIT ?");
	select.setString(1, query + "%");
	select.setInt(2, count);
	ResultSet rs = select.executeQuery();

	while(rs.next())
	{
	    suggestions.add(rs.getString(1));
	}

	return suggestions;
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

	System.exit(0);
    }

}

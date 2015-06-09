package de.l3s.learnweb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class YovistoManager
{
    public static Logger log = Logger.getLogger(YovistoManager.class);
    private final Learnweb learnweb;
    private final static String DB_User = "learnweb_crawler";
    private final static String DB_Password = "***REMOVED***";
    private final static String DB_Connection = "jdbc:mysql://prometheus.kbs.uni-hannover.de:3306/learnweb_crawler?characterEncoding=utf8";
    private Connection DBConnection;

    public YovistoManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;

    }

    private void getConnection()
    {
	try
	{
	    Class.forName("com.mysql.jdbc.Driver");
	    java.util.Properties connProperties = new java.util.Properties();
	    connProperties.setProperty("user", DB_User);
	    connProperties.setProperty("password", DB_Password);
	    DBConnection = DriverManager.getConnection(DB_Connection, connProperties);

	    System.out.println("Connected to the database....");

	}
	catch(SQLException e)
	{
	    log.error("SQL Exception in getConnection ", e);
	}
	catch(ClassNotFoundException e)
	{
	    log.error("getConnection ", e);
	}

    }

    private void getYovistoData()
    {

	String fetchData = "SELECT  FROM yovisto_object o, yovisto_subject s, yovisto_predicate p";
	try
	{
	    PreparedStatement preparedstmnt = DBConnection.prepareStatement(fetchData);
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }

    public void saveYovistoResource()
    {
	getConnection();
	getYovistoData();

    }

    private void addTagToResource(Resource resource, String tagName, User user) throws Exception
    {
	ResourceManager rsm = Learnweb.getInstance().getResourceManager();
	Tag tag = rsm.getTag(tagName);

	if(tag == null)
	    tag = rsm.addTag(tagName);

	rsm.tagResource(resource, tag, user);
    }

    public static void main(String[] args) throws Exception
    {

	YovistoManager lm = Learnweb.getInstance().getYovistoManager();
	lm.saveYovistoResource();

	//DBConnection.close();
	System.exit(0);
    }

}

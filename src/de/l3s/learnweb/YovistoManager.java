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

    public void mapToLearnwebSchema(String resourceURI) throws SQLException
    {
	String title = null, tags = null, keywords = null, speaker = null, thumbnail_url = null, category = null, description = null, alternativeTitle = null;
	String language = null, organization = null;
	int yovistoId = 0;

	int modelId = checkInObjectTable(resourceURI);
	if(modelId < 1)
	{
	    log.error("Resource URI: " + resourceURI + " not found in the table.");
	    // continue;
	}
	String getData = "SELECT o.object, p.predicate, s.subject FROM yovisto_object o, yovisto_subject s, yovisto_predicate p WHERE o.model_id = " + modelId + " AND o.subject_id = s.subject_id AND o.predicate_id = p.predicate_id";
	PreparedStatement preparedstmnt = DBConnection.prepareStatement("");
	java.sql.ResultSet resultFromTriple = preparedstmnt.executeQuery();
	while(resultFromTriple.next())
	{
	    //set Yovisto identifier
	    if(resultFromTriple.getString("predicate").contains("identifier"))
	    {
		String identifier = resultFromTriple.getString("object");
		identifier.replace("video/", "");
		yovistoId = Integer.parseInt(identifier);
	    }
	    //Set title of video and category
	    if(resultFromTriple.getString("predicate").contains("title"))
	    {
		if(resultFromTriple.getString("subject").contains("category"))
		    if(category == null)
			category = resultFromTriple.getString("object");
		    else
			category += ", " + resultFromTriple.getString("object");
		else if(resultFromTriple.getString("subject").contains("http://www.yovisto.com/resource/video/") && !resultFromTriple.getString("subject").contains("#"))
		    title = resultFromTriple.getString("object");
	    }
	    //Get keywords
	    else if(resultFromTriple.getString("predicate").contains("keywords"))
		keywords = resultFromTriple.getString("object");
	    else if(resultFromTriple.getString("predicate").contains("thumbnail"))
		thumbnail_url = resultFromTriple.getString("object"); //thumbnail_url
	    else if(resultFromTriple.getString("predicate").contains("description"))
		description = resultFromTriple.getString("object");
	    //Set alternative Title for a video. Can be more than one
	    else if(resultFromTriple.getString("predicate").contains("alternative"))
	    {
		if(alternativeTitle == null)
		    alternativeTitle = resultFromTriple.getString("object");
		else
		    alternativeTitle += ", " + resultFromTriple.getString("object");
	    }
	    //set languages. Could be more than one.
	    else if(resultFromTriple.getString("predicate").contains("alternative"))
	    {
		if(language == null)
		    language = resultFromTriple.getString("object");
		else
		    language += ", " + resultFromTriple.getString("object");

	    }
	    //Set tags
	    else if(resultFromTriple.getString("predicate").contains("http://www.holygoat.co.uk/owl/redwood/0.1/tags/name"))
	    {
		if(tags == null)
		    tags = resultFromTriple.getString("object");
		else
		    tags += ", " + resultFromTriple.getString("object");
	    }
	}
    }

    private int checkInObjectTable(String resourceURI)
    {
	// TODO Auto-generated method stub
	return 0;
    }

    public void saveYovistoResource()
    {
	getConnection();
	getYovistoData();

    }

    public static void main(String[] args) throws Exception
    {

	YovistoManager lm = Learnweb.getInstance().getYovistoManager();
	lm.saveYovistoResource();

	//DBConnection.close();
	System.exit(0);
    }

}

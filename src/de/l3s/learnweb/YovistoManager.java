package de.l3s.learnweb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.solrClient.SolrClient;

public class YovistoManager
{
    public static Logger log = Logger.getLogger(YovistoManager.class);
    private final Learnweb learnweb;
    private final static String DB_User = "learnweb_crawler";
    private final static String DB_Password = "***REMOVED***";
    private final static String DB_Connection = "jdbc:mysql://prometheus.kbs.uni-hannover.de:3306/learnweb_crawler?characterEncoding=utf8";
    private Connection DBConnection;
    private static long lastCheck = 0L;

    public YovistoManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;

    }

    public void saveYovistoResource() throws SQLException
    {
	ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
	SolrClient solr = learnweb.getSolrClient();
	Group tedGroup = learnweb.getGroupManager().getGroupById(862);
	User admin = learnweb.getUserManager().getUser(7727);
	connect();
	ResultSet result = null;
	try
	{
	    PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM yovisto_video");
	    result = preparedStmnt.executeQuery();
	}
	catch(SQLException e)
	{
	    log.error("Error in fetching data from yovisto_video table", e);
	}
	while(result.next())
	{
	    if(result.getString("organization").contains("TED Conference - Technology, Entertainment, Design") || result.getString("organization").contains("TEDxSanJoseCA 2012 "))
		continue;
	    int learnwebResourceId = result.getInt("resource_id");

	    Resource yovistoVideo = createResource(result, learnwebResourceId);
	    int yovistoId = Integer.parseInt(yovistoVideo.getIdAtService());

	}

    }

    private Resource createResource(ResultSet result, int learnwebResourceId) throws SQLException
    {
	Resource resource = new Resource();

	if(learnwebResourceId != 0) // the video is already stored and will be updated
	    resource = learnweb.getResourceManager().getResource(learnwebResourceId);

	resource.setTitle(result.getString("title"));
	String description = result.getString("description");
	resource.setUrl("http://www.yovisto.com/video/" + result.getInt("yovisto_id"));
	resource.setSource("Yovsito");
	resource.setLocation("Yovisto");
	resource.setType("Video");
	resource.setFormat(result.getString("format"));
	if(!result.getString("keywords").isEmpty())
	    description = description + " \nKeywords: " + result.getString("keywords");
	if(!result.getString("language").isEmpty())
	{
	    String lang = result.getString("language");
	    if(lang.contains("nl"))
		lang += "Dutch, ";
	    if(lang.contains("de"))
		lang += "German, ";
	    if(lang.contains("en"))
		lang += "English, ";
	    if(lang.contains("fr"))
		lang += "French, ";
	    if(lang.contains("pt"))
		lang += "Portuguese, ";
	    if(lang.contains("es"))
		lang += "Spanish, ";
	    if(lang.contains("da"))
		lang += "Danish, ";
	    if(lang.contains("it"))
		lang += "Italian, ";
	    if(lang.contains("th"))
		lang += "Thai, ";
	    if(lang.contains("zh"))
		lang += "Chinese, ";
	    if(lang.contains("fi"))
		lang += "Finnish, ";
	    if(lang.length() > 0)
	    {
		lang.subSequence(0, lang.lastIndexOf(","));
	    }
	    description += " \nLanguage: " + lang;
	}
	resource.setDescription(description);

	//resource.setDuration(result.getTime("duration").toString());
	resource.setMaxImageUrl(result.getString("thumbnail_url"));
	resource.setIdAtService(Integer.toString(result.getInt("yovisto_id")));

	//resource.setEmbeddedRaw("<iframe src=\"http://embed.ted.com/talks/" + rs.getString("slug") + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");

	return resource;
    }

    private void connect()
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

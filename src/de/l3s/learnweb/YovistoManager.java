package de.l3s.learnweb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.solrClient.FileInspector;
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

    public void saveYovistoResource() throws SQLException
    {
	getConnection();
	ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
	SolrClient solr = learnweb.getSolrClient();
	Group yovistoGroup = learnweb.getGroupManager().getGroupById(918);
	User admin = learnweb.getUserManager().getUser(7727);
	connect();
	ResultSet result = null;
	try
	{
	    PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM yovisto_video WHERE `yovisto_id`=8670");
	    result = preparedStmnt.executeQuery();
	}
	catch(SQLException e)
	{
	    log.error("Error in fetching data from yovisto_video table", e);
	}
	PreparedStatement update = Learnweb.getInstance().getConnection().prepareStatement("UPDATE yovisto_video SET resource_id = ? WHERE yovisto_id = ?");
	while(result.next())
	{
	    if(result.getString("organization").contains("TED Conference - Technology, Entertainment, Design") || result.getString("organization").contains("TEDxSanJoseCA 2012 "))
	    {
		int id = result.getInt("yovisto_id");
		log.info("Video with id: " + id + "not included as it is a TED video");
		continue;
	    }
	    if(VideoDoesNotExist(result.getInt("yovisto_id")))
	    {
		int id = result.getInt("yovisto_id");
		log.info("Video with id: " + id + "not included as it does not exist.");
		continue;
	    }
	    int learnwebResourceId = result.getInt("resource_id");

	    Resource yovistoVideo = createResource(result, learnwebResourceId);
	    int yovistoId = Integer.parseInt(yovistoVideo.getIdAtService());

	    yovistoVideo.setOwner(admin);

	    if(learnwebResourceId == 0) // not yet stored in Learnweb

	    {
		try
		{
		    rpm.processImage(yovistoVideo, FileInspector.openStream(yovistoVideo.getMaxImageUrl()));
		}
		catch(IOException e)
		{
		    log.error("Error in creating preview image for video with id: " + yovistoId, e);
		    e.printStackTrace();
		}

		update.setInt(1, yovistoVideo.getId());
		update.setInt(2, yovistoId);
		update.executeUpdate();

		admin.addResource(yovistoVideo);
		yovistoGroup.addResource(yovistoVideo, admin);

		//solr.indexResource(yovistoVideo);
		yovistoVideo.save();

	    }
	    else
		yovistoVideo.save();
	    log.debug("Processed; lw: " + learnwebResourceId + " ted: " + yovistoId + " title:" + yovistoVideo.getTitle());

	}

    }

    private boolean VideoDoesNotExist(int yovistoId)
    {
	try
	{
	    HttpURLConnection.setFollowRedirects(false);
	    HttpURLConnection con = (HttpURLConnection) new URL("http://www.yovisto.com/streams/" + yovistoId + ".mp4").openConnection();
	    con.setRequestMethod("HEAD");

	    if(con.getResponseCode() == 404)
		return true;

	    /*try
	    {
	        Thread.sleep(10000 * (int) Math.pow(2, i));
	    }
	    catch(InterruptedException e)
	    {
	        log.error("Failed due to some interrupt exception on the thread that fetches from the LORO", e);
	    }*/

	}
	catch(Exception e)
	{
	    log.error("Failed because there was a problem in establishing connection.", e);
	    e.printStackTrace();
	    return true;
	}
	return false;
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
	if(!result.getString("speaker").isEmpty())
	    description = description + " \nSpeaker: " + result.getString("speaker");
	if(!result.getString("language").isEmpty())
	{
	    String lang = "";
	    if(result.getString("language").contains("de"))
		lang += "German, ";
	    if(result.getString("language").contains("en"))
		lang += "English, ";
	    if(result.getString("language").contains("pt"))
		lang += "Portuguese, ";
	    if(result.getString("language").contains("fr"))
		lang += "French, ";
	    if(result.getString("language").contains("it"))
		lang += "Italian, ";
	    if(result.getString("language").contains("da"))
		lang += "Danish, ";
	    if(result.getString("language").contains("fi"))
		lang += "Finnish, ";
	    if(result.getString("language").contains("es"))
		lang += "Spanish, ";
	    else if(lang.isEmpty() && !lang.contains("none"))
		lang += result.getString("language") + ", ";

	}

	if(!result.getString("alternative_title").isEmpty())
	    description = "Alternative Title: " + result.getString("alternative_title") + " \n" + description;
	resource.setDescription(description);
	resource.setDuration(result.getInt("durationInSec"));
	resource.setMaxImageUrl(result.getString("thumbnail_url"));
	resource.setIdAtService(Integer.toString(result.getInt("yovisto_id")));
	resource.setFileUrl("http://www.yovisto.com/streams/" + result.getInt("yovisto_id") + ".mp4");
	resource.setEmbeddedRaw("<object id=\"embPlayer"
		+ result.getInt("yovisto_id")
		+ " classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" codebase=\"http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=7,0,19,0\" width=\"100%\" height=\"100%\"> <param name=\"movie\" value=\"http://www.yovisto.com/yoexply.swf?vid="
		+ result.getInt("yovisto_id")
		+ "&amp;url=http://www.yovisto.com/streams/"
		+ result.getInt("yovisto_id")
		+ ".mp4&amp;prev=\""
		+ result.getString("thumbnail_url")
		+ "\"></param><param name=\"quality\" value=\"high\"></param><param name=\"scale\" value=\"exactfit\"></param><param name=\"allowFullScreen\" value=\"true\"></param><param name=\"wmode\" value=\"transparent\"></param><embed id=\"embPlayer"
		+ result.getInt("yovisto_id")
		+ "\" src=\"http://www.yovisto.com/yoexply.swf?vid="
		+ result.getInt("yovisto_id")
		+ "&amp;url=http://www.yovisto.com/streams/"
		+ result.getInt("yovisto_id")
		+ ".mp4&amp;prev=\""
		+ result.getString("thumbnail_url")
		+ "\" scale=\"exactfit\" quality=\"high\" name=\"FlashMovie\" swliveconnect=\"true\" allowFullScreen=\"true\" wmode=\"transparent\" pluginspage=\"http://www.macromedia.com/go/getflashplayer\" type=\"application/x-shockwave-flash\" width=\"100%\" height=\"100%\" flashvars=\"var1=0&amp;enablejs=true\"></embed> </object>");

	resource.setAuthor(result.getString("organization"));

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

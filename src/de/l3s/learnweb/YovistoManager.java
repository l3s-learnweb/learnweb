package de.l3s.learnweb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import de.l3s.learnweb.solrClient.SolrClient;

public class YovistoManager
{
    public static Logger log = Logger.getLogger(YovistoManager.class);
    private final Learnweb learnweb;
    private final static String DB_User = "learnweb_crawler";
    private final static String DB_Password = "***REMOVED***";
    private final static String DB_Connection = "jdbc:mysql://prometheus.kbs.uni-hannover.de:3306/learnweb_crawler?characterEncoding=utf8";
    private static Connection DBConnection;
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
	ResourceManager resourceManager = learnweb.getResourceManager();
	connect();
	ResultSet result = null;
	/*User rishita = learnweb.getUserManager().getUser(7727);

	for(Resource resource : yovistoGroup.getResources())
	{

	    resourceManager.deleteResourcePermanent(resource.getId());

	}

	System.exit(0);*/
	try
	{
	    PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM yovisto_video WHERE yovisto_id = 10027");
	    result = preparedStmnt.executeQuery();
	}
	catch(SQLException e)
	{
	    log.error("Error in fetching data from yovisto_video table", e);
	}
	PreparedStatement update = Learnweb.getInstance().getConnection().prepareStatement("UPDATE yovisto_video SET resource_id = ? WHERE yovisto_id = ?");
	while(result.next())
	{
	    //To avoid duplicates between TED videos included through TED API and through Yovisto. Change this later when there are more TED videos available through API.
	    if(result.getString("organization").contains("TED Conference - Technology, Entertainment, Design") || result.getString("organization").contains("TEDxSanJoseCA 2012 "))
	    {
		int id = result.getInt("yovisto_id");
		log.info("Video with id: " + id + " not included as it is a TED video");
		continue;
	    }
	    //Check whether Video exists or not by HEAD request
	    if(VideoDoesNotExist(result.getInt("yovisto_id")))
	    {
		int id = result.getInt("yovisto_id");
		log.info("Video with id: " + id + " not included as it does not exist.");
		continue;
	    }
	    int learnwebResourceId = result.getInt("resource_id");

	    Resource yovistoVideo = createResource(result, learnwebResourceId);
	    int yovistoId = Integer.parseInt(yovistoVideo.getIdAtService());

	    yovistoVideo.setOwner(admin);

	    if(learnwebResourceId == 0) // not yet stored in LearnWeb

	    {
		try
		{
		    rpm.processVideo(yovistoVideo);

		}
		catch(IOException e)
		{
		    log.error("Error in creating preview image for video with id: " + yovistoId, e);
		    e.printStackTrace();
		}
		admin.addResource(yovistoVideo);
		yovistoGroup.addResource(yovistoVideo, admin);
		try
		{
		    update.setInt(1, yovistoVideo.getId());
		    update.setInt(2, yovistoId);

		    update.executeUpdate();
		}
		catch(SQLException e)
		{
		    log.error(e);
		    e.printStackTrace();

		}

		/*try
		{
		    solr.indexResource(yovistoVideo);
		}
		catch(IOException e)
		{
		    log.error("Error in indexing the video with yovisto ID: " + yovistoId, e);
		    e.printStackTrace();
		}
		catch(SolrServerException e)
		{
		    log.error("Error in indexing the video with yovisto ID: " + yovistoId, e);
		    e.printStackTrace();
		}*/
		yovistoVideo.save();

	    }
	    else
		yovistoVideo.save();
	    //Add the tags to the resource
	    try
	    {
		Set<String> tag = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER); //Tags from the yovisto_video table
		tag.addAll(Arrays.asList(result.getString("user_tag").split(",")));

		OwnerList<Tag, User> tagsFromResource = resourceManager.getTagsByResource(learnwebResourceId); //Tags already added to this resource
		StringBuilder out = new StringBuilder();
		for(Tag tags : tagsFromResource)
		{
		    if(out.length() != 0)
			out.append(", ");
		    out.append(tags.getName());
		}
		String output = out.toString();
		Set<String> tagsFromLw = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		tagsFromLw.addAll(Arrays.asList(output.split(", ")));
		tag.removeAll(tagsFromLw); //Remove already added tags to avoid duplicate tags

		for(String tagName : tag)
		{
		    try
		    {
			addTagToResource(yovistoVideo, tagName, admin);
		    }
		    catch(Exception e)
		    {
			log.error("Error in adding tags " + tagName, e);
			e.printStackTrace();
		    }
		}
	    }
	    catch(NullPointerException e)
	    {
		log.info("No tags available for resource with id" + yovistoId, e);
	    }
	    log.debug("Processed; lw: " + learnwebResourceId + " yovisto: " + yovistoId + " title:" + yovistoVideo.getTitle());

	}

    }

    private boolean VideoDoesNotExist(int yovistoId)
    {
	try
	{
	    HttpURLConnection.setFollowRedirects(false);
	    HttpURLConnection con = (HttpURLConnection) new URL("http://www.yovisto.com/streams/" + yovistoId + ".mp4").openConnection();
	    con.setRequestMethod("HEAD");

	    //Video does not exist
	    if(con.getResponseCode() == 404)
		return true;

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
	    //Decode the language through its representation
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
	    if(!lang.isEmpty())
		description = description + "\nLanguage: " + lang;

	}

	if(!result.getString("alternative_title").isEmpty())
	    description = " \nAlternative Title: " + result.getString("alternative_title") + "\n" + description;
	resource.setDescription(description);
	resource.setDuration(result.getInt("durationInSec"));
	resource.setMaxImageUrl(result.getString("thumbnail_url"));
	int yovistoId = result.getInt("yovisto_id");
	resource.setIdAtService(Integer.toString(yovistoId));
	resource.setFileUrl("http://www.yovisto.com/streams/" + result.getInt("yovisto_id") + ".mp4");
	resource.setEmbeddedRaw("<iframe id=\"embPlayer"
		+ yovistoId
		+ "\"  src=\"http://www.yovisto.com/yoexply.swf?vid="
		+ yovistoId
		+ "&amp;url=http://www.yovisto.com/streams/"
		+ yovistoId
		+ ".mp4&amp;prev="
		+ result.getString("thumbnail_url")
		+ "\" scale=\"exactfit\" quality=\"high\" name=\"FlashMovie\" swliveconnect=\"true\" allowFullScreen=\"true\" wmode=\"transparent\" pluginspage=\"http://www.macromedia.com/go/getflashplayer\" type=\"application/x-shockwave-flash\" style=\"height:100%; width:100%;\" flashvars=\"var1=0&amp;enablejs=true\"></iframe>");

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

	DBConnection.close();
	System.exit(0);
    }

}

package de.l3s.learnweb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.solrClient.SolrClient;

public class YovistoManager
{
    public static Logger log = Logger.getLogger(YovistoManager.class);
    private final Learnweb learnweb;

    public YovistoManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;

    }

    public void saveYovistoResource() throws SQLException
    {
	//connect();
	ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
	SolrClient solr = learnweb.getSolrClient();
	Group yovistoGroup = learnweb.getGroupManager().getGroupById(918);
	User admin = learnweb.getUserManager().getUser(7727);
	ResourceManager resourceManager = learnweb.getResourceManager();

	ResultSet result = null;
	//User rishita = learnweb.getUserManager().getUser(7727);

	/*for(Resource resource : yovistoGroup.getResources())
	{
	    int id = -1;
	    try
	    {
	
		id = resource.getThumbnail0().getFileId();
	    }
	    catch(Exception e)
	    {
		log.error("Error in getting thumbnail id", e);
	    }
	    if(id < 0)
		continue;
	    File fileName = new File("uploaded_files/" + id + ".dat");
	    if(fileName.exists())
	    {
		double size = fileName.length();
	
		if(size < 500.0)
		{
		    log.debug(fileName);
		    try
		    {
			rpm.processVideo(resource);
		    }
		    catch(Exception e)
		    {
			log.error("Black and white images not reset for the resource.", e);
	
		    }
		    try
		    {
			Thread.sleep(1000);
		    }
		    catch(InterruptedException e)
		    {
			log.error(e);
		    }
		    resource.save();
	
		}
	    }
	
	}
	log.debug("Exit");
	System.exit(0);*/
	try
	{

	    PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM yovisto_video");
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

		    yovistoVideo.setGroup(yovistoGroup);
		    admin.addResource(yovistoVideo);
		    try
		    {
			update.setInt(1, yovistoVideo.getId());
			update.setInt(2, yovistoId);

			update.executeUpdate();
		    }
		    catch(SQLException e)
		    {
			log.error(e);
		    }

		    try
		    {
			solr.indexResource(yovistoVideo);
		    }
		    catch(IOException | SolrServerException e)
		    {
			log.error("Error in indexing the video with yovisto ID: " + yovistoId, e);

		    }
		    yovistoVideo.save();
		}
		catch(IOException | IllegalArgumentException e)
		{
		    log.error("Error in creating preview image for video with id: " + yovistoId, e);
		}

	    }
	    else
		yovistoVideo.save();
	    //Add the tags to the resource
	    try
	    {
		Set<String> tag = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER); //Tags from the yovisto_video table
		tag.addAll(Arrays.asList(result.getString("user_tag").split(",")));

		OwnerList<Tag, User> tagsFromResource = resourceManager.getTagsByResource(yovistoVideo.getId()); //Tags already added to this resource
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
	    return true;
	}
	return false;
    }

    private Resource createResource(ResultSet result, int learnwebResourceId) throws SQLException
    {

	Date date = new Date();
	Resource resource = new Resource();

	if(learnwebResourceId != 0) // the video is already stored and will be updated
	    resource = learnweb.getResourceManager().getResource(learnwebResourceId);
	else
	    resource.setCreationDate(date);

	resource.setTitle(result.getString("title"));
	String description = result.getString("description");
	resource.setUrl("http://www.yovisto.com/video/" + result.getInt("yovisto_id"));
	resource.setSource("Yovisto");
	resource.setLocation("Yovisto");
	resource.setType("Video");
	resource.setFormat(result.getString("format"));
	if(!result.getString("keywords").isEmpty())
	    description = description + " \nKeywords: " + result.getString("keywords");
	if(!result.getString("speaker").isEmpty())
	    description = description + " \nSpeaker: " + result.getString("speaker");
	if(!result.getString("category").isEmpty())
	    description = description + "\nCategory: " + result.getString("category");
	if(!result.getString("language").isEmpty())
	{
	    //Decode the language through its representation
	    String lang = result.getString("language").toLowerCase();
	    lang = lang.replace("none", "").trim();
	    if(lang.endsWith(","))
	    {
		lang.substring(0, lang.lastIndexOf(","));
	    }

	    if(!lang.isEmpty())
	    {
		String finalLanguage = "";
		String[] languages = lang.split(",");
		for(String language : languages)
		{
		    resource.setLanguage(language.trim());
		    Locale locale = new Locale(language.trim());
		    finalLanguage += locale.getDisplayLanguage() + ", ";
		}

		finalLanguage = finalLanguage.substring(0, finalLanguage.lastIndexOf(","));
		description = description + "\nLanguage: " + finalLanguage.trim();
	    }

	}

	if(!result.getString("alternative_title").isEmpty())
	    description = "Alternative Title: " + result.getString("alternative_title") + "\n" + description;
	resource.setDescription(description);
	resource.setDuration(result.getInt("durationInSec"));
	resource.setMaxImageUrl(result.getString("thumbnail_url"));
	int yovistoId = result.getInt("yovisto_id");
	resource.setIdAtService(Integer.toString(yovistoId));
	resource.setFileUrl("http://www.yovisto.com/streams/" + result.getInt("yovisto_id") + ".mp4");
	resource.setEmbeddedRaw("<embed id=\"embPlayer" + yovistoId + "\"  src=\"http://www.yovisto.com/yoexply.swf?vid=" + yovistoId + "&amp;url=http://www.yovisto.com/streams/" + yovistoId + ".mp4&amp;prev=" + result.getString("thumbnail_url")
		+ "\" scale=\"exactfit\" quality=\"high\" name=\"FlashMovie\" swliveconnect=\"true\" allowFullScreen=\"true\" wmode=\"transparent\" pluginspage=\"http://www.macromedia.com/go/getflashplayer\" type=\"application/x-shockwave-flash\" style=\"height:100%; width:100%;\" flashvars=\"var1=0&amp;enablejs=true\"></embed>");

	resource.setAuthor(result.getString("organization"));

	return resource;
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

	System.exit(0);
    }

}

package de.l3s.tedapi.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnwebBeans.AddResourceBean;

public class BaseTedApiCrawler
{

    protected Properties properties;
    private Client tedClient;
    static String tedTalksURL;
    static String tedTranscriptsBaseURL;

    //logging 
    public final static Logger log = Logger.getLogger(BaseTedApiCrawler.class);

    public BaseTedApiCrawler()
    {

	try
	{
	    //Client used to retrieve the TED data using the api
	    tedClient = Client.create();

	    properties = new Properties();
	    String propertiesFileName = "de/l3s/learnweb/config/ted.properties";
	    properties.load(getClass().getClassLoader().getResourceAsStream(propertiesFileName));

	    tedTalksURL = properties.getProperty("ted_talks_url");
	    tedTranscriptsBaseURL = properties.getProperty("ted_transcript_base_url");

	}
	catch(IOException e)
	{
	    log.error("Failed to load the config properties file", e);
	}
    }

    public void pushTranscriptToDb(int tedId, int lwResourceId, String langCode, String langName, String dbAction)
    {

	//tedTranscriptsURL to retrieve the subtitles for english as of now
	String tedTranscriptsURL = tedTranscriptsBaseURL + tedId + "/subtitles.json";
	String apiKey = properties.getProperty("ted_apikey_1");
	MultivaluedMap<String, String> transcriptParams = new MultivaluedMapImpl();
	transcriptParams.add("api-key", apiKey);
	transcriptParams.add("language", langCode);

	String jsonSubtitle = getJsonData(tedTranscriptsURL, transcriptParams);

	String transcript = "";

	try
	{
	    //TODO: Remove this section once ted_transcripts table is removed
	    /*String dbStmt = dbAction + " `ted_transcripts`(`resource_id`, `language_code`, `language`, `json`) VALUES (?,?,?,?)";
	    PreparedStatement pStmt2 = Learnweb.getInstance().getConnection().prepareStatement(dbStmt);
	    
	    pStmt2.setInt(1, lwResourceId);
	    pStmt2.setString(2, langCode);
	    pStmt2.setString(3, langName);
	    pStmt2.setString(4, jsonSubtitle);
	    
	    int dbReturnValue = pStmt2.executeUpdate();
	    pStmt2.close();
	    if(dbReturnValue == 1)
	    {
	    log.info(tedId + " ted_transcript " + langName + " json inserted successfully");
	    }
	    else
	    log.info(tedId + " ted_transcript " + langName + "  json already inserted");*/

	    //Storing languge code to language mapping 
	    String dbStmt = "REPLACE INTO ted_transcripts_lang_mapping(`language_code`,`language`) VALUES (?,?)";
	    PreparedStatement pStmt2 = Learnweb.getInstance().getConnection().prepareStatement(dbStmt);
	    pStmt2.setString(1, langCode);
	    pStmt2.setString(2, langName);
	    pStmt2.executeUpdate();

	    JSONParser parser = new JSONParser();
	    JSONObject jsonSubtitleObject;

	    jsonSubtitleObject = (JSONObject) parser.parse(jsonSubtitle);

	    JSONObject meta = (JSONObject) jsonSubtitleObject.get("_meta");
	    int preRollOffset = 0;

	    if(meta != null)
	    {
		preRollOffset = Integer.parseInt(meta.get("preroll_offset").toString());
		int captionId = 0, startTime;
		boolean isStartTimeSet = false;

		dbStmt = dbAction + " `ted_transcripts_paragraphs`(`resource_id`, `language`, `starttime`, `paragraph`) VALUES (?,?,?,?)";
		PreparedStatement pStmt3 = Learnweb.getInstance().getConnection().prepareStatement(dbStmt);
		pStmt3.setInt(1, lwResourceId);
		pStmt3.setString(2, langCode);

		JSONObject jsonCaptionId, jsonCaption;
		String content;

		for(;; captionId++)
		{
		    jsonCaptionId = (JSONObject) jsonSubtitleObject.get(Integer.toString(captionId));

		    if(jsonCaptionId == null)
			break;

		    jsonCaption = (JSONObject) jsonCaptionId.get("caption");

		    if(Boolean.parseBoolean(jsonCaption.get("startOfParagraph").toString()))
		    {

			if(captionId > 0)
			{
			    pStmt3.setString(4, transcript);
			    pStmt3.addBatch();
			    transcript = "";
			}

			startTime = Integer.parseInt(jsonCaption.get("startTime").toString());
			startTime += preRollOffset;
			pStmt3.setInt(3, startTime);
			isStartTimeSet = true;

		    }
		    if(captionId == 0 && !isStartTimeSet)
		    {
			startTime = Integer.parseInt(jsonCaption.get("startTime").toString());
			startTime += preRollOffset;
			pStmt3.setInt(3, startTime);
		    }
		    content = jsonCaption.get("content").toString();
		    content = content.replace("\n", " ");
		    transcript += content + " ";
		}
		pStmt3.setString(4, transcript);
		pStmt3.addBatch();
		pStmt3.executeBatch();
		pStmt3.close();
	    }
	    else
		log.info("Subtitle not found (HTTP 404 error)");

	}
	catch(ParseException e)
	{
	    log.error("Failed to parse the subtitles json returned", e);
	}
	catch(SQLException e)
	{
	    log.error("Failed due to some error in inserting into the database", e);
	}

    }

    public String getJsonData(String url, MultivaluedMap<String, String> params)
    {
	ClientResponse resp;
	String jsonData = "{}";

	int exponent = 2;
	for(int i = 1; i < 100;)
	{

	    resp = getTedData(url, params);
	    if(resp.getStatus() == 200)
	    {
		jsonData = resp.getEntity(String.class);
		break;
	    }
	    else if(resp.getStatus() == 404)
	    {
		jsonData = resp.getEntity(String.class);
		break;
	    }
	    else if(resp.getStatus() == 504)
	    {
		try
		{
		    Thread.sleep(10000 * i);
		}
		catch(InterruptedException e)
		{
		    log.error("Failed due to some interrupt exception on the thread that fetches from the ted api in case of gateway error 504", e);
		}
		i = (int) Math.pow(2, exponent++);
	    }
	}

	return jsonData;
    }

    public ClientResponse getTedData(String url, MultivaluedMap<String, String> params)
    {

	WebResource web = tedClient.resource(url);

	ClientResponse resp = web.queryParams(params).get(ClientResponse.class);

	if(resp.getStatus() != 200)
	{
	    log.info("Failed : HTTP error code : " + resp.getStatus());
	}

	try
	{
	    Thread.sleep(1000);
	}
	catch(InterruptedException e)
	{
	    log.error("Failed due to some interrupt exception on the thread that fetches from the ted api", e);
	}
	return resp;
    }

    public static void main(String[] args) throws SQLException, MalformedURLException, IOException, ParseException
    {
	BaseTedApiCrawler tedApiCrawler = new BaseTedApiCrawler();
	//new CheckUpdatedTedVideos().run();
	PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement("Select slug from ted_video WHERE ted_id < 200 ORDER BY resource_id DESC");
	ResultSet rs = pStmt.executeQuery();
	String prepareStmt = "UPDATE `ted_video` SET `title`=?, description=?, slug=? WHERE `ted_id`=?";
	PreparedStatement pStmt2 = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);
	PreparedStatement pStmt3 = Learnweb.getInstance().getConnection().prepareStatement("SELECT resource_id FROM ted_video WHERE ted_id = ?");

	while(rs.next())
	{
	    String url = "http://www.ted.com/talks/" + rs.getString(1);
	    //Document doc = Jsoup.parse(new URL(url), 10000);
	    String changedurl = AddResourceBean.checkUrl(url);
	    if(changedurl != null && !changedurl.equals(url))
	    {
		System.out.println(url);
		System.out.println(changedurl);
		String apiKey = tedApiCrawler.properties.getProperty("ted_apikey_2");
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("api-key", apiKey);
		params.add("filter", "slug:" + changedurl.split("talks/")[1]);
		String jsonTalks = tedApiCrawler.getJsonData(tedTalksURL, params);
		JSONParser jsonParser = new JSONParser();
		JSONObject jsonTalksObject = (JSONObject) jsonParser.parse(jsonTalks);

		// get an array from the JSON object talks
		JSONArray talks = (JSONArray) jsonTalksObject.get("talks");
		JSONObject jsonTempObj, jsonTalkObj;
		if(talks != null)
		{
		    Iterator i = talks.iterator();

		    while(i.hasNext())
		    {

			jsonTempObj = (JSONObject) i.next();
			jsonTalkObj = (JSONObject) jsonTempObj.get("talk");

			//talkJson = jsonTalkObj.toString();
			int tedId = Integer.parseInt(jsonTalkObj.get("id").toString());
			String title = jsonTalkObj.get("name").toString();
			String description = jsonTalkObj.get("description").toString();
			String slug = jsonTalkObj.get("slug").toString();
			System.out.println("title: " + title);
			System.out.println("description: " + description);
			System.out.println("slug: " + slug);
			pStmt2.setString(1, title);
			pStmt2.setString(2, description);
			pStmt2.setString(3, slug);
			pStmt2.setInt(4, tedId);
			int dbReturnVal = pStmt2.executeUpdate();
			if(dbReturnVal == 1)
			{
			    log.info("updated video " + tedId);
			    pStmt3.setInt(1, tedId);
			    ResultSet rsResourceId = pStmt3.executeQuery();
			    if(rsResourceId.next())
			    {
				int lwResourceId = rsResourceId.getInt(1);
				Resource r = Learnweb.getInstance().getResourceManager().getResource(lwResourceId);
				r.setTitle(title);
				r.setDescription(description);
				r.setUrl(changedurl);
				r.save();
			    }
			}

		    }
		}

	    }
	    System.out.println("url :" + url);
	}
	pStmt.close();
	pStmt2.close();
	pStmt3.close();

	System.exit(0);
    }

}

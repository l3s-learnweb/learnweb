package de.l3s.tedapi.crawler;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.l3s.learnweb.Learnweb;

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
	    String dbStmt = dbAction + " `ted_transcripts`(`resource_id`, `language_code`, `language`, `json`) VALUES (?,?,?,?)";
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
		log.info(tedId + " ted_transcript " + langName + "  json already inserted");

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

			//startTime = (preRollOffset + startTime)/1000;

			//long second = TimeUnit.SECONDS.toSeconds(startTime) - TimeUnit.SECONDS.toMinutes(startTime)*60;
			//long minute = TimeUnit.SECONDS.toMinutes(startTime) - TimeUnit.SECONDS.toHours(startTime)*60;
			//transcript += String.format("%d:%02d",minute,second) + "\t";
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

    public static void main(String[] args) throws SQLException
    {
	BaseTedApiCrawler tedApiCrawler = new BaseTedApiCrawler();

	String fields = "updated_at,media";

	String dbStmt = "SELECT * FROM `ted_video` WHERE duration=0";
	String dbStmt2 = "UPDATE lw_resource SET embeddedRaw=? WHERE resource_id = ?";
	PreparedStatement pStmt2 = Learnweb.getInstance().getConnection().prepareStatement(dbStmt);
	ResultSet rs = pStmt2.executeQuery();
	JSONParser parser = new JSONParser();
	JSONObject jsonSubtitleObject;
	String jsonSubtitle;

	while(rs.next())
	{
	    try
	    {
		String tedTranscriptsURL = "https://api.ted.com/v1/talks/" + rs.getInt("ted_id") + ".json";
		String apiKey = tedApiCrawler.properties.getProperty("ted_apikey_1");
		MultivaluedMap<String, String> transcriptParams = new MultivaluedMapImpl();
		transcriptParams.add("api-key", apiKey);
		transcriptParams.add("fields", fields);

		jsonSubtitle = tedApiCrawler.getJsonData(tedTranscriptsURL, transcriptParams);

		jsonSubtitleObject = (JSONObject) parser.parse(jsonSubtitle);
		JSONObject talk = (JSONObject) jsonSubtitleObject.get("talk");
		JSONObject media = (JSONObject) talk.get("media");
		JSONObject external = (JSONObject) media.get("external");
		PreparedStatement pStmt3 = Learnweb.getInstance().getConnection().prepareStatement(dbStmt2);
		pStmt3.setString(1, "<iframe src=\"https://www.youtube.com/embed/" + external.get("code") + "\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");
		pStmt3.setInt(2, rs.getInt("resource_id"));
		pStmt3.executeUpdate();
		log.debug(external.get("code"));
	    }
	    catch(ParseException | NullPointerException e)
	    {
		log.error(e);
	    }

	}

    }

}

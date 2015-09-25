package de.l3s.tedapi.crawler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.ws.rs.core.MultivaluedMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.l3s.learnweb.Learnweb;

@SuppressWarnings("rawtypes")
public class CheckUpdatedTedVideos extends BaseTedApiCrawler implements Runnable
{

    public CheckUpdatedTedVideos()
    {
	super();
    }

    public void updateTalksInDb(String jsonTalks)
    {

	try
	{

	    JSONParser jsonParser = new JSONParser();
	    JSONObject jsonTalksObject = (JSONObject) jsonParser.parse(jsonTalks);

	    // get an array from the JSON object talks
	    JSONArray talks = (JSONArray) jsonTalksObject.get("talks");

	    Iterator i = talks.iterator();

	    JSONObject jsonTempObj, jsonTalkObj;
	    String talkUpdatedAt, langCode, prepareStmt;
	    int tedId, lwResourceId, viewedCount, dbReturnVal;
	    boolean updateTranscripts;
	    PreparedStatement pStmt = null;

	    ArrayList<String> langCodesFromDb = new ArrayList<String>();
	    // take each value from the json array separately
	    while(i.hasNext())
	    {
		updateTranscripts = true;
		langCodesFromDb.clear();
		jsonTempObj = (JSONObject) i.next();
		jsonTalkObj = (JSONObject) jsonTempObj.get("talk");

		tedId = Integer.parseInt(jsonTalkObj.get("id").toString());
		viewedCount = Integer.parseInt(jsonTalkObj.get("viewed_count").toString());
		talkUpdatedAt = jsonTalkObj.get("updated_at").toString();

		prepareStmt = "select viewed_count, DATE_FORMAT(talk_updated_at,'%Y-%m-%d %H:%i:%s') as talk_updated_at from ted_video where ted_id=?";
		pStmt = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);
		pStmt.setInt(1, tedId);
		pStmt.executeQuery();
		java.sql.ResultSet rs = pStmt.getResultSet();

		if(rs.next())
		{
		    String updatedAt = rs.getString("talk_updated_at");
		    int viewedCountFromDb = rs.getInt("viewed_count");

		    if(viewedCount != viewedCountFromDb)
		    {
			prepareStmt = "UPDATE `ted_video` SET `viewed_count`=? WHERE `ted_id`=?";
			pStmt = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);
			pStmt.setInt(1, viewedCount);
			pStmt.setInt(2, tedId);
			dbReturnVal = pStmt.executeUpdate();
			if(dbReturnVal == 1)
			    log.info("Changed updated viewed count from " + viewedCountFromDb + "to " + viewedCount);
		    }

		    if(!talkUpdatedAt.equals(updatedAt))
		    {
			prepareStmt = "SELECT resource_id FROM ted_video WHERE ted_id = ?";
			pStmt = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);
			pStmt.setInt(1, tedId);
			ResultSet rsResourceId = pStmt.executeQuery();
			if(rsResourceId.next())
			{
			    lwResourceId = rsResourceId.getInt(1);
			    prepareStmt = "SELECT language_code FROM ted_transcripts WHERE `resource_id`=?";
			    pStmt = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);
			    pStmt.setInt(1, lwResourceId);
			    ResultSet rsLangCode = pStmt.executeQuery();
			    while(rsLangCode.next())
			    {
				langCodesFromDb.add(rsLangCode.getString("language_code"));
			    }

			    JSONArray languages = (JSONArray) jsonTalkObj.get("languages");
			    Iterator langs = languages.iterator();

			    while(langs.hasNext())
			    {
				JSONObject innerlangs = (JSONObject) langs.next();
				JSONObject lang = (JSONObject) innerlangs.get("language");
				langCode = lang.get("language_code").toString();
				if(!langCodesFromDb.contains(langCode))
				{
				    pushTranscriptToDb(tedId, lwResourceId, langCode, lang.get("name").toString(), "INSERT IGNORE INTO");
				    updateTranscripts = false;
				    log.info("Inserted ted transcript for " + langCode);
				}

			    }

			    if(updateTranscripts)
			    {
				langs = languages.iterator();
				while(langs.hasNext())
				{
				    JSONObject innerlangs = (JSONObject) langs.next();
				    JSONObject lang = (JSONObject) innerlangs.get("language");
				    langCode = lang.get("language_code").toString();
				    if(langCodesFromDb.contains(langCode))
				    {
					pushTranscriptToDb(tedId, lwResourceId, langCode, lang.get("name").toString(), "REPLACE INTO");
					log.info("Updated ted transcript for ted video: " + tedId + " and language: " + langCode);
				    }
				}
			    }
			    prepareStmt = "UPDATE `ted_video` SET `talk_updated_at`=? WHERE `ted_id`=?";
			    pStmt = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);
			    pStmt.setString(1, talkUpdatedAt);
			    pStmt.setInt(2, tedId);
			    dbReturnVal = pStmt.executeUpdate();
			    if(dbReturnVal == 1)
				log.info("Changed updated at from " + updatedAt + "to " + talkUpdatedAt);

			}
			else
			{
			    log.error("database error: no resource_id for for TED video ID:" + tedId + ", So couldn't update the video.");
			}
		    }
		    else
		    {
			log.info("Video " + tedId + " hasn't been updated");
		    }
		}
		else
		{
		    log.info("Ted Video " + tedId + "Not yet added to the database");
		}

	    }
	    pStmt.close();
	}
	catch(ParseException ex)
	{
	    log.error("Failed to parse the languages from the talk json data that was retrieved", ex);
	}
	catch(NullPointerException ex)
	{
	    log.error("Failed due to null pointer exception", ex);
	}
	catch(SQLException e)
	{
	    log.error("Failed to update the viewed count or updated at for the existing ted videos to the database", e);
	}

    }

    @Override
    public void run()
    {

	log.info("Entering check updated videos thread at " + new Date());
	log.info(Thread.currentThread().getName());
	String fields = "updated_at,viewed_count,languages";
	int limit = 100, offset = 0;
	String apiKey = properties.getProperty("ted_apikey_2");
	MultivaluedMap<String, String> params = new MultivaluedMapImpl();
	params.add("api-key", apiKey);
	params.add("fields", fields);
	params.add("limit", Integer.toString(limit));

	params.add("offset", Integer.toString(offset));

	String jsonTalks = getJsonData(tedTalksURL, params);
	updateTalksInDb(jsonTalks);

	try
	{

	    JSONParser talkCountsParser = new JSONParser();
	    JSONObject talksObject = (JSONObject) talkCountsParser.parse(jsonTalks);
	    JSONObject countsObject = (JSONObject) talksObject.get("counts");
	    int total = Integer.parseInt(countsObject.get("total").toString());

	    int pages = (total / 100) + 1;
	    offset += limit;
	    params.remove("offset");

	    for(int k = 1; k < pages; k++)
	    {

		params.add("offset", Integer.toString(offset));
		jsonTalks = getJsonData(tedTalksURL, params);
		updateTalksInDb(jsonTalks);
		params.remove("offset");
		offset += limit;
	    }

	}
	catch(ParseException e)
	{
	    log.error("Failed to parse the total count of the new ted videos", e);
	}
	log.info("End time of update videos thread:" + new Date());

    }

}

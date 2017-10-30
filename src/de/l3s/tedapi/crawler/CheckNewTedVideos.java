package de.l3s.tedapi.crawler;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.User;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.SolrClient;

@SuppressWarnings("rawtypes")
public class CheckNewTedVideos extends BaseTedApiCrawler implements Runnable
{

    private final static String TED_VIDEO_COLUMNS = "`ted_id`, `resource_id`, `title`, `description`, `slug`, `viewed_count`, `published_at`, `talk_updated_at`, `photo1_url`, `photo1_width`, `photo1_height`, `photo2_url`, `photo2_width`, `photo2_height`, `tags`, `duration`, `json`";

    public CheckNewTedVideos()
    {
        super();
    }

    public void pushTalksToDb(String jsonTalks)
    {

        try
        {
            ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
            SolrClient solr = Learnweb.getInstance().getSolrClient();
            Group tedGroup = Learnweb.getInstance().getGroupManager().getGroupById(862);
            User admin = Learnweb.getInstance().getUserManager().getUser(7727);

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonTalksObject = (JSONObject) jsonParser.parse(jsonTalks);

            // get an array from the JSON object talks
            JSONArray talks = (JSONArray) jsonTalksObject.get("talks");

            Iterator i = talks.iterator(), pics;

            JSONObject jsonTempObj, jsonTalkObj, picObj, mediaObj, tags;
            JSONArray picsUrl;
            String talkJson, title, description, slug, publishedAt, talkUpdatedAt, size, langCode, tagsString, prepareStmt, duration, youtubeId = "";
            String[] size_dimensions;
            int tedId, viewedCount, noOfTags, j, dbReturnVal, totalDuration = 0, hours, mins, secs;
            ArrayList<Integer> picsWidth = new ArrayList<Integer>();
            ArrayList<Integer> picsHeight = new ArrayList<Integer>();
            ArrayList<String> picsURL = new ArrayList<String>();
            Set<Map.Entry<String, String>> tagEntries;
            PreparedStatement pStmt = null;
            // take each value from the json array separately
            while(i.hasNext())
            {

                jsonTempObj = (JSONObject) i.next();
                jsonTalkObj = (JSONObject) jsonTempObj.get("talk");

                talkJson = jsonTalkObj.toString();
                tedId = Integer.parseInt(jsonTalkObj.get("id").toString());
                viewedCount = Integer.parseInt(jsonTalkObj.get("viewed_count").toString());
                talkUpdatedAt = jsonTalkObj.get("updated_at").toString();

                title = jsonTalkObj.get("name").toString();
                description = jsonTalkObj.get("description").toString();
                slug = jsonTalkObj.get("slug").toString();
                publishedAt = jsonTalkObj.get("published_at").toString();

                mediaObj = (JSONObject) jsonTalkObj.get("media");
                if(mediaObj != null)
                {
                    duration = mediaObj.get("duration").toString();
                    String[] durationSplit = duration.split(":");
                    hours = Integer.parseInt(durationSplit[0]);
                    mins = Integer.parseInt(durationSplit[1]);
                    secs = Integer.parseInt(durationSplit[2]);
                    totalDuration = hours * 3600 + mins * 60 + secs;
                }
                else
                {
                    youtubeId = getYoutubeId(Integer.toString(tedId));
                }
                picsUrl = (JSONArray) jsonTalkObj.get("photo_urls");
                pics = picsUrl.iterator();
                picsWidth.clear();
                picsHeight.clear();
                picsURL.clear();

                while(pics.hasNext())
                {
                    picObj = (JSONObject) pics.next();
                    size = picObj.get("size").toString();
                    size_dimensions = size.split("x");

                    picsWidth.add(Integer.parseInt(size_dimensions[0]));
                    picsHeight.add(Integer.parseInt(size_dimensions[1]));
                    picsURL.add(picObj.get("url").toString());
                }

                //langCode = jsonTalkObj.get("native_language_code").toString();
                //pushTranscriptToDb(tedId, langCode, "English");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Resource tedResource = new Resource();
                tedResource.setTitle(title);
                tedResource.setDescription(description);
                tedResource.setUrl("http://www.ted.com/talks/" + slug);
                tedResource.setSource("TED");
                tedResource.setType(Resource.ResourceType.video);
                tedResource.setDuration(totalDuration);
                tedResource.setIdAtService(Integer.toString(tedId));
                tedResource.setMaxImageUrl(picsURL.get(1));
                tedResource.setCreationDate(df.parse(publishedAt));
                if(mediaObj != null)
                    tedResource.setEmbeddedRaw("<iframe src=\"http://embed.ted.com/talks/" + slug + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");
                else
                    tedResource.setEmbeddedRaw("<iframe src=\"https://www.youtube.com/embed/" + youtubeId + "\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");

                tedResource.setTranscript("");
                rpm.processImage(tedResource, FileInspector.openStream(tedResource.getMaxImageUrl()));
                tedResource.setGroup(tedGroup);
                admin.addResource(tedResource);

                solr.indexResource(tedResource);
                /*prepareStmt = "REPLACE INTO `lw_resource`(`title`, `description`, `url`, `source`, `type`, `duration`, `id_at_service`, `max_image_url`, `embeddedRaw`, `storage_type`, `rights`, `author`, `format`, `owner_user_id`, `rating`, `rate_number`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                pStmt = getDbcon().prepareStatement(prepareStmt, Statement.RETURN_GENERATED_KEYS);
                pStmt.setString(1, title);
                pStmt.setString(2, description);
                pStmt.setString(3, "http://www.ted.com/talks/" + slug);
                pStmt.setString(4, "TED");
                pStmt.setString(5, "Video");
                pStmt.setInt(6, totalDuration);
                pStmt.setString(7, Integer.toString(tedId));
                pStmt.setString(8, picsURL.get(1));
                pStmt.setString(9, "<iframe src=\"http://embed.ted.com/talks/" + slug + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");
                pStmt.setInt(10, 2);
                pStmt.setInt(11, 0);
                pStmt.setString(12, "");
                pStmt.setString(13, "");
                pStmt.setInt(14, 0);
                pStmt.setInt(15, 0);
                pStmt.setInt(16, 0);
                pStmt.executeUpdate();
                
                ResultSet rs = pStmt.getGeneratedKeys();
                if(!rs.next())
                {
                    log.error("database error: no id generated for TED video ID:" + tedId + "and TED video URL:" + slug);
                    continue;
                }
                lwResourceId = rs.getInt(1);*/

                JSONArray languages = (JSONArray) jsonTalkObj.get("languages");
                Iterator langs = languages.iterator();
                String languagesString = "";
                while(langs.hasNext())
                {
                    JSONObject innerlangs = (JSONObject) langs.next();
                    JSONObject lang = (JSONObject) innerlangs.get("language");
                    langCode = lang.get("language_code").toString();
                    pushTranscriptToDb(tedId, tedResource.getId(), langCode, lang.get("name").toString(), "INSERT IGNORE INTO");
                    languagesString += langCode + ", ";
                }
                log.info("languages:" + languagesString);

                if(!youtubeId.isEmpty())
                {
                    Learnweb.getInstance().getTedManager().fetchTedXTranscripts(youtubeId, tedResource.getId());
                }

                tags = (JSONObject) jsonTalkObj.get("tags");

                tagsString = "";

                tagEntries = tags.entrySet();
                noOfTags = tagEntries.size();
                j = 0;

                for(Map.Entry<String, String> tagEntry : tagEntries)
                {
                    if(j == noOfTags - 1)
                    {
                        tagsString += tagEntry.getValue();
                        break;
                    }
                    if(j < noOfTags)
                        tagsString += tagEntry.getValue() + ", ";
                    j++;
                }

                prepareStmt = "INSERT IGNORE INTO `ted_video`(" + TED_VIDEO_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                pStmt = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);

                pStmt.setInt(1, tedId);
                pStmt.setInt(2, tedResource.getId());
                pStmt.setString(3, title);
                pStmt.setString(4, description);
                pStmt.setString(5, slug);
                pStmt.setInt(6, viewedCount);
                pStmt.setString(7, publishedAt);
                pStmt.setString(8, talkUpdatedAt);
                pStmt.setString(9, picsURL.get(0));
                pStmt.setInt(10, picsWidth.get(0));
                pStmt.setInt(11, picsHeight.get(0));
                pStmt.setString(12, picsURL.get(1));
                pStmt.setInt(13, picsWidth.get(1));
                pStmt.setInt(14, picsHeight.get(1));
                pStmt.setString(15, tagsString);
                pStmt.setInt(16, totalDuration);
                pStmt.setString(17, talkJson);

                dbReturnVal = pStmt.executeUpdate();

                if(dbReturnVal == 1)
                {
                    log.info(tedId + " video successfully inserted");
                }
                else
                    log.info(tedId + " video failed to insert");
            }
            pStmt.close();
        }
        catch(ParseException ex)
        {
            log.error("Failed to parse the talk json data that was retrieved", ex);
        }
        catch(NullPointerException ex)
        {
            log.error("Failed due to null pointer exception", ex);
        }
        catch(SQLException e)
        {
            log.error("Failed due to the new ted video to the database", e);
        }
        catch(java.text.ParseException e)
        {
            log.error("Failed due to parsing of published date", e);
        }
        catch(IOException | SolrServerException e)
        {
            log.error("Failed while trying to index ted resource in Solr", e);
        }

    }

    public String getYoutubeId(String tedId)
    {
        String apiKey = properties.getProperty("ted_apikey_2");
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("api-key", apiKey);
        String jsonTalk = getJsonData(tedTranscriptsBaseURL + tedId + ".json", params);
        String youtubeId = "";
        if(jsonTalk.contains("talk"))
        {
            JSONParser parser = new JSONParser();
            JSONObject talkObject;
            try
            {
                talkObject = (JSONObject) parser.parse(jsonTalk);
                JSONObject media = (JSONObject) talkObject.get("media");
                if(media != null)
                {
                    JSONObject external = (JSONObject) talkObject.get("external");
                    if(external != null)
                        youtubeId = external.get("code").toString();
                }
            }
            catch(ParseException e)
            {
                log.error("Error while parsing ted talk json for youtube ted video");
            }
        }
        return youtubeId;
    }

    @Override
    public void run()
    {

        log.debug("Entering new videos thread at " + new Date());
        String fields = "updated_at,viewed_count,languages,media,tags,photo_urls";
        int limit = 100, offset = 0;
        String apiKey = properties.getProperty("ted_apikey_2");
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("api-key", apiKey);
        params.add("fields", fields);
        params.add("limit", Integer.toString(limit));

        PreparedStatement pStmt = null;
        String jsonTalks = "";
        String publishedAt = "published_at:>";
        String prepareStmt = "SELECT published_at FROM `ted_video` ORDER BY published_at DESC LIMIT 1 ";
        try
        {
            pStmt = Learnweb.getInstance().getConnection().prepareStatement(prepareStmt);
            pStmt.executeQuery();
            ResultSet rs = pStmt.getResultSet();
            if(rs.next())
            {
                publishedAt += rs.getString("published_at");
            }

            params.add("filter", publishedAt);

            jsonTalks = getJsonData(tedTalksURL, params);

            if(jsonTalks.contains("talks"))
                pushTalksToDb(jsonTalks);

            JSONParser talkCountsParser = new JSONParser();
            JSONObject talksObject = (JSONObject) talkCountsParser.parse(jsonTalks);
            JSONObject countsObject = (JSONObject) talksObject.get("counts");
            int total = Integer.parseInt(countsObject.get("total").toString());

            int pages = (total / 100) + 1;
            offset += limit;
            params.remove("offset");
            params.remove("filter");

            for(int k = 1; k < pages; k++)
            {
                params.add("offset", Integer.toString(offset));
                jsonTalks = getJsonData(tedTalksURL, params);
                pushTalksToDb(jsonTalks);
                params.remove("offset");
                offset += limit;
            }

        }
        catch(SQLException e)
        {
            log.error("Failed in retrieving the published at value of the last inserted Ted video", e);
        }
        catch(ParseException e)
        {
            log.error("Failed to parse the total count of the new ted videos", e);
        }
        log.info("End time of new videos thread:" + new Date());
    }

}

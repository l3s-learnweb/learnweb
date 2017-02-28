package de.l3s.learnweb;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.l3s.interwebj.IllegalResponseException;
import de.l3s.interwebj.SearchQuery;
import de.l3s.learnweb.Transcript.Paragraph;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.SolrClient;
import de.l3s.util.StringHelper;

public class TedManager
{
    private final static Logger log = Logger.getLogger(TedManager.class);

    private final static String TRANSCRIPT_COLUMNS = "`course_id`,`user_id`,`resource_id`,`words_selected`,`user_annotation`,`action`,`timestamp`";
    private final static String TRANSCRIPT_SELECTION_COLUMNS = "`resource_id`,`words_selected`,`user_annotation`,`start_offset`,`end_offset`";
    //private final static String RESOURCE_COLUMNS = "r.resource_id, r.title, r.description, r.url, r.storage_type, r.rights, r.source, r.type, r.format, r.owner_user_id, r.rating, r.rate_number, r.embedded_size1, r.embedded_size2, r.embedded_size3, r.embedded_size4, r.filename, r.max_image_url, r.query, r.original_resource_id, r.author, r.access, r.thumbnail0_url, r.thumbnail0_file_id, r.thumbnail0_width, r.thumbnail0_height, r.thumbnail1_url, r.thumbnail1_file_id, r.thumbnail1_width, r.thumbnail1_height, r.thumbnail2_url, r.thumbnail2_file_id, r.thumbnail2_width, r.thumbnail2_height, r.thumbnail3_url, r.thumbnail3_file_id, r.thumbnail3_width, r.thumbnail3_height, r.thumbnail4_url, r.thumbnail4_file_id, r.thumbnail4_width, r.thumbnail4_height, r.embeddedRaw, r.transcript, r.online_status";

    private final Learnweb learnweb;

    public enum SummaryType
    {
        SHORT,
        LONG,
        DETAILED
    }

    protected TedManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public void saveSummaryText(int courseId, int userId, int resourceId, String summaryText, SummaryType summaryType) throws SQLException
    {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("REPLACE INTO lw_transcript_summary(course_id, user_id,resource_id,summary_type,summary_text) VALUES (?,?,?,?,?)");
        pStmt.setInt(1, courseId);
        pStmt.setInt(2, userId);
        pStmt.setInt(3, resourceId);
        pStmt.setString(4, summaryType.name());
        pStmt.setString(5, summaryText);
        pStmt.executeUpdate();
        pStmt.close();
    }

    public void saveTranscriptLog(TranscriptLog transcriptLog) throws SQLException
    {

        PreparedStatement saveTranscript = learnweb.getConnection().prepareStatement("INSERT into lw_transcript_actions(" + TRANSCRIPT_COLUMNS + ") VALUES (?,?,?,?,?,?,?)");
        saveTranscript.setInt(1, transcriptLog.getCourseId());
        saveTranscript.setInt(2, transcriptLog.getUserId());
        saveTranscript.setInt(3, transcriptLog.getResourceId());
        saveTranscript.setString(4, transcriptLog.getWordsSelected());
        saveTranscript.setString(5, transcriptLog.getUserAnnotation());
        saveTranscript.setString(6, transcriptLog.getAction());
        saveTranscript.setTimestamp(7, new java.sql.Timestamp(transcriptLog.getTimestamp().getTime()));
        saveTranscript.executeUpdate();
        saveTranscript.close();

    }

    public void saveTranscriptSelection(String transcript, int resourceId) throws SQLException
    {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("INSERT into lw_transcript_selections(" + TRANSCRIPT_SELECTION_COLUMNS + ") VALUES (?,?,?,?,?)");
        if(transcript != null && transcript != "")
        {
            Document doc = Jsoup.parse(transcript);
            Elements elements = doc.select("span");
            for(Element element : elements)
            {
                int start = 0, end = 0;
                if(!element.attr("data-start").equals(""))
                    start = Integer.parseInt(element.attr("data-start"));
                if(!element.attr("data-end").equals(""))
                    end = Integer.parseInt(element.attr("data-end"));

                pStmt.setInt(1, resourceId);
                pStmt.setString(2, element.text());
                pStmt.setString(3, element.attr("data-title"));
                pStmt.setInt(4, start);
                pStmt.setInt(5, end);
                pStmt.addBatch();
            }
        }
        pStmt.executeBatch();
        pStmt.close();
    }

    public List<Transcript> getTransscripts(int resourceId) throws SQLException
    {
        List<Transcript> transcripts = new LinkedList<Transcript>();
        String selectTranscripts = "SELECT DISTINCT(language) as language_code FROM ted_transcripts_paragraphs WHERE resource_id = ?";
        String selectTranscriptParagraphs = "SELECT starttime, paragraph FROM ted_transcripts_paragraphs WHERE resource_id = ? AND language = ?";

        PreparedStatement ipStmt = Learnweb.getInstance().getConnection().prepareStatement(selectTranscriptParagraphs);

        PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement(selectTranscripts);
        pStmt.setInt(1, resourceId);
        ResultSet rs = pStmt.executeQuery(), rsParagraphs;
        while(rs.next())
        {
            Transcript transcript = new Transcript();
            String languageCode = rs.getString("language_code");
            transcript.setLanguageCode(languageCode);

            ipStmt.setInt(1, resourceId);
            ipStmt.setString(2, languageCode);
            ipStmt.executeQuery();

            rsParagraphs = ipStmt.getResultSet();
            while(rsParagraphs.next())
            {
                transcript.addParagraph(rsParagraphs.getInt("starttime"), rsParagraphs.getString("paragraph"));
            }

            transcripts.add(transcript);
        }
        rs.close();
        pStmt.close();
        ipStmt.close();

        return transcripts;
    }

    public int getTedVideoResourceId(String url) throws SQLException
    {
        int tedVideoResourceId = 0;
        String slug = url.substring(url.lastIndexOf("/") + 1, url.length());
        PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement("SELECT resource_id FROM ted_video WHERE slug = ?");
        pStmt.setString(1, slug);
        ResultSet rs = pStmt.executeQuery();
        if(rs.next())
        {
            tedVideoResourceId = rs.getInt("resource_id");
        }

        return tedVideoResourceId;
    }

    public int getTedXVideoResourceId(String url) throws SQLException
    {
        int tedxVideoResourceId = 0;
        PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement("SELECT resource_id FROM lw_resource WHERE url = ? and owner_user_id = 7727");
        pStmt.setString(1, url);
        ResultSet rs = pStmt.executeQuery();
        if(rs.next())
        {
            tedxVideoResourceId = rs.getInt("resource_id");
        }

        return tedxVideoResourceId;
    }

    public Map<String, String> getLangList(int resourceId) throws SQLException
    {
        String langFromPropFile;
        Map<String, String> langList = new HashMap<String, String>();
        PreparedStatement getLangList = learnweb.getConnection().prepareStatement("SELECT DISTINCT(t1.language) as language_code, t2.language FROM `ted_transcripts_paragraphs` t1 JOIN ted_transcripts_lang_mapping t2 ON t1.language=t2.language_code WHERE resource_id=?");
        getLangList.setInt(1, resourceId);
        ResultSet rs = getLangList.executeQuery();

        while(rs.next())
        {
            langFromPropFile = UtilBean.getLocaleMessage("language_" + rs.getString("language_code"));
            if(langFromPropFile == null)
                langList.put(rs.getString("language"), rs.getString("language_code"));
            else
                langList.put(langFromPropFile, rs.getString("language_code"));
        }
        rs.close();
        getLangList.close();
        return langList;
    }

    public String getTranscript(int resourceId, String language) throws SQLException
    {
        String selectTranscript = "SELECT `starttime`, `paragraph` FROM ted_transcripts_paragraphs where resource_id = ? AND `language` = ?";
        String transcript = "";

        PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement(selectTranscript);
        pStmt.setInt(1, resourceId);
        pStmt.setString(2, language);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while(rs.next())
        {
            int startTime = rs.getInt("starttime") / 1000;
            String para = rs.getString("paragraph");
            transcript += StringHelper.getDurationInMinutes(startTime) + "\t";
            transcript += para + "\n";
        }

        return transcript;
    }

    public List<TranscriptSummary> getTranscriptSummaries(int courseId) throws SQLException
    {
        List<TranscriptSummary> transcriptSummaries = new ArrayList<TranscriptSummary>();
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM lw_transcript_summary WHERE course_id = ? ORDER BY user_id");
        pStmt.setInt(1, courseId);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while(rs.next())
        {
            TranscriptSummary transcriptSummary = new TranscriptSummary(rs.getInt("user_id"), rs.getInt("resource_id"), rs.getString("summary_type"), rs.getString("summary_text"));
            transcriptSummaries.add(transcriptSummary);
        }
        pStmt.close();

        return transcriptSummaries;
    }

    public HashMap<SummaryType, String> getTranscriptSummariesForResource(int resourceId) throws SQLException
    {
        HashMap<SummaryType, String> transcriptSummaries = new HashMap<SummaryType, String>();
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT summary_type, summary_text FROM lw_transcript_summary WHERE resource_id = ?");
        pStmt.setInt(1, resourceId);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while(rs.next())
        {
            transcriptSummaries.put(SummaryType.valueOf(rs.getString(1)), rs.getString(2));
        }
        pStmt.close();

        return transcriptSummaries;

    }

    public List<TranscriptLog> getTranscriptLogs(int courseId, boolean showDeleted) throws SQLException
    {
        List<TranscriptLog> transcriptLogs = new ArrayList<TranscriptLog>();
        String pStmtString;
        if(showDeleted)
            pStmtString = "SELECT " + TRANSCRIPT_COLUMNS + " FROM lw_transcript_actions WHERE course_id = ? ORDER BY user_id, timestamp DESC";
        else
            pStmtString = "SELECT " + TRANSCRIPT_COLUMNS + " FROM lw_transcript_actions JOIN lw_resource USING(resource_id) WHERE course_id = ? and deleted = 0 ORDER BY user_id, timestamp DESC";
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(pStmtString);
        pStmt.setInt(1, courseId);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while(rs.next())
        {
            TranscriptLog transcriptLog = new TranscriptLog(rs.getInt("course_id"), rs.getInt("user_id"), rs.getInt("resource_id"), rs.getString("words_selected"), rs.getString("user_annotation"), rs.getString("action"), rs.getTimestamp("timestamp"));
            transcriptLogs.add(transcriptLog);
        }
        pStmt.close();

        return transcriptLogs;
    }

    public List<SimpleTranscriptLog> getSimpleTranscriptLogs(int courseId, boolean showDeleted) throws SQLException
    {
        List<SimpleTranscriptLog> simpleTranscriptLogs = new LinkedList<SimpleTranscriptLog>();

        PreparedStatement getUsers = learnweb.getConnection().prepareStatement("SELECT t1.user_id FROM `lw_user_course` t1 WHERE t1.course_id = ? AND t1.user_id !=7727");
        getUsers.setInt(1, courseId);
        ResultSet rs = getUsers.executeQuery();
        int userId;
        while(rs.next())
        {
            userId = rs.getInt("user_id");
            String pStmtString;
            if(showDeleted)
                pStmtString = "SELECT t1.resource_id,title, SUM(action = 'selection') as selcount, SUM(action = 'deselection') as deselcount, SUM(user_annotation != '') as uacount FROM lw_resource t1 LEFT JOIN lw_transcript_actions t2 ON t1.resource_id = t2.resource_id WHERE (action = 'selection' OR action = 'deselection' OR user_annotation != '' OR action IS NULL) AND t1.owner_user_id = ? GROUP BY t1.resource_id";
            else
                pStmtString = "SELECT t1.resource_id,title, SUM(action = 'selection') as selcount, SUM(action = 'deselection') as deselcount, SUM(user_annotation != '') as uacount FROM lw_resource t1 LEFT JOIN lw_transcript_actions t2 ON t1.resource_id = t2.resource_id WHERE (action = 'selection' OR action = 'deselection' OR user_annotation != '' OR action IS NULL) AND t1.owner_user_id = ? AND t1.deleted = 0 GROUP BY t1.resource_id";
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement(pStmtString);
            pStmt.setInt(1, userId);
            ResultSet rs2 = pStmt.executeQuery();
            while(rs2.next())
            {
                SimpleTranscriptLog simpleTranscriptLog = new SimpleTranscriptLog(userId, rs2.getInt("resource_id"), rs2.getInt("selcount"), rs2.getInt("deselcount"), rs2.getInt("uacount"));
                simpleTranscriptLogs.add(simpleTranscriptLog);
            }
        }
        return simpleTranscriptLogs;
    }

    //For saving crawled ted videos into lw_resource table
    public void saveTedResource() throws SQLException, IOException, SolrServerException
    {
        ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
        SolrClient solr = learnweb.getSolrClient();
        Group tedGroup = learnweb.getGroupManager().getGroupById(862);
        User admin = learnweb.getUserManager().getUser(7727);

        PreparedStatement update = Learnweb.getInstance().getConnection().prepareStatement("UPDATE ted_video SET resource_id = ? WHERE ted_id = ?");

        PreparedStatement getTedVideos = Learnweb.getInstance().getConnection().prepareStatement("SELECT ted_id, title, description, slug, photo2_url, duration, resource_id, published_at FROM ted_video");
        getTedVideos.executeQuery();

        ResultSet rs = getTedVideos.getResultSet();
        while(rs.next())
        {
            int learnwebResourceId = rs.getInt("resource_id");

            Resource tedVideo = createResource(rs, learnwebResourceId);
            int tedId = Integer.parseInt(tedVideo.getIdAtService());

            tedVideo.setMachineDescription(concatenateTranscripts(learnwebResourceId));
            tedVideo.setOwner(admin);

            if(learnwebResourceId == 0) // not yet stored in Learnweb

            {
                rpm.processImage(tedVideo, FileInspector.openStream(tedVideo.getMaxImageUrl()));

                update.setInt(1, tedVideo.getId());
                update.setInt(2, tedId);
                update.executeUpdate();

                tedVideo.setGroup(tedGroup);
                admin.addResource(tedVideo);

                solr.indexResource(tedVideo);

            }
            else if(tedVideo.getOwnerUserId() == 0)
            {
                rpm.processImage(tedVideo, FileInspector.openStream(tedVideo.getMaxImageUrl()));
                tedVideo.setGroup(tedGroup);
                admin.addResource(tedVideo);
                solr.indexResource(tedVideo);
            }
            else
                tedVideo.save();

            log.debug("Processed; lw: " + learnwebResourceId + " ted: " + tedId + " title:" + tedVideo.getTitle());
        }

    }

    private String concatenateTranscripts(int learnwebResourceId) throws SQLException
    {
        StringBuilder sb = new StringBuilder();

        for(Transcript transcript : getTransscripts(learnwebResourceId))
        {
            if(transcript.getLanguageCode().equals("en") || transcript.getLanguageCode().equals("fr") || transcript.getLanguageCode().equals("de") || transcript.getLanguageCode().equals("es") || transcript.getLanguageCode().equals("it"))
                for(Paragraph paragraph : transcript.getParagraphs())
                {
                    sb.append(paragraph.getText());
                    sb.append("\n\n");
                }
        }

        return sb.toString();
    }

    private Resource createResource(ResultSet rs, int learnwebResourceId) throws SQLException
    {
        Resource resource = new Resource();

        if(learnwebResourceId != 0) // the video is already stored and will be updated
            resource = learnweb.getResourceManager().getResource(learnwebResourceId);

        resource.setTitle(rs.getString("title"));
        resource.setDescription(rs.getString("description"));
        resource.setUrl("http://www.ted.com/talks/" + rs.getString("slug"));
        resource.setSource("TED");
        resource.setLocation("TED");
        resource.setType("Video");
        resource.setDuration(rs.getInt("duration"));
        resource.setMaxImageUrl(rs.getString("photo2_url"));
        resource.setIdAtService(Integer.toString(rs.getInt("ted_id")));
        resource.setCreationDate(rs.getTimestamp("published_at"));
        resource.setEmbeddedRaw("<iframe src=\"http://embed.ted.com/talks/" + rs.getString("slug") + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");
        resource.setTranscript("");
        return resource;
    }

    public void fetchTedX() throws IOException, IllegalResponseException, SQLException
    {
        ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();

        //Group tedxGroup = learnweb.getGroupManager().getGroupById(921);
        Group tedxTrentoGroup = learnweb.getGroupManager().getGroupById(922);
        User admin = learnweb.getUserManager().getUser(7727);

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT resource_id FROM lw_resource WHERE url = ?");

        TreeMap<String, String> params = new TreeMap<String, String>();
        params.put("media_types", "video");
        params.put("services", "YouTube");
        params.put("number_of_results", "50");
        params.put("timeout", "500");

        List<ResourceDecorator> resources;
        int page = 1; // you have to start at page one due to youtupe api limitations

        do
        {
            params.put("page", Integer.toString(page));

            SearchQuery interwebResponse = learnweb.getInterweb().search("user::TEDxTalks tedxtrento", params);
            //log.debug(interwebResponse.getResultCountAtService());
            resources = interwebResponse.getResults();

            for(ResourceDecorator decoratedResource : resources)
            {
                Resource resource = decoratedResource.getResource();

                resource.setSource("TEDx");
                resource.setLocation("TEDx");

                String[] title = resource.getTitle().split("\\|");
                if(title.length == 3 && title[2].startsWith(" TEDx"))
                {
                    resource.setAuthor(title[1].trim());
                    resource.setTitle(title[0] + "|" + title[2]);
                }

                // check if resources is already stored in Learnweb
                select.setString(1, resource.getUrl());
                ResultSet rs = select.executeQuery();

                if(rs.next()) // it is already stored
                {
                    int resourceId = rs.getInt(1);
                    Resource learnwebResource = learnweb.getResourceManager().getResource(resourceId);

                    if(learnwebResource.getIdAtService() == null || learnwebResource.getIdAtService().length() == 0)
                    {
                        learnwebResource.setIdAtService(resource.getIdAtService());

                        if(learnwebResource.getGroupId() == tedxTrentoGroup.getId())
                        {
                            log.error("resource is already part of the group");
                        }
                        else
                        {
                            learnwebResource.setGroup(tedxTrentoGroup);
                        }
                        learnwebResource.save();
                    }

                    log.debug("Already stored: " + resource);

                    resource = learnwebResource;
                }
                else
                {
                    rpm.processImage(resource, FileInspector.openStream(resource.getMaxImageUrl().replace("hqdefault", "mqdefault")));
                    resource.setGroup(tedxTrentoGroup);
                    admin.addResource(resource);

                    log.debug("new video added");
                }

                //check if new transcripts are available for "resource" variable
                fetchTedXTranscripts(resource.getIdAtService(), resource.getId());
            }

            page++;
            log.debug("page: " + page);
            // break;
        }
        while(resources.size() > 0 && page < 6);
    }

    public void insertTedXTranscripts(String resourceIdAtService, int resourceId, JSONObject transcriptItem) throws JSONException, SQLException
    {
        String langCode = transcriptItem.getString("lang_code");
        String langName = transcriptItem.getString("lang_translated");

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM `ted_transcripts_paragraphs` WHERE `resource_id` = ? AND `language` = ?");
        select.setInt(1, resourceId);
        select.setString(2, langCode);
        ResultSet rs = select.executeQuery();
        if(rs.next())
        {
            log.info("Transcript :" + langCode + " for Ted video: " + resourceId + " already inserted.");
            return; // transcript is already part of the database
        }

        ClientResponse resp = getTedxData("http://video.google.com/timedtext?lang=" + langCode + "&v=", resourceIdAtService);
        if(resp.getStatus() != 200 || resp.getLength() == 0)
        {
            log.info("Transcript :" + langCode + " for resource ID: " + resourceIdAtService + " does not exist.");
            return; //no transcript available for this language code
        }

        JSONObject transcriptJSON = XML.toJSONObject(resp.getEntity(String.class));

        //TODO:To remove once ted_transcripts table is deleted
        /*String dbStmt = "REPLACE INTO `ted_transcripts`(`resource_id`, `language_code`, `language`, `json`) VALUES (?,?,?,?)";
        PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement(dbStmt);
        pStmt2.setInt(1, resourceId);
        pStmt2.setString(2, langCode);
        pStmt2.setString(3, langName);
        pStmt2.setString(4, transcriptJSON.toString());
        pStmt2.executeUpdate();
        pStmt2.close();*/
        String dbStmt = "REPLACE INTO `ted_transcripts_lang_mapping`(`language_code`,`language`) VALUES (?,?)";
        PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement(dbStmt);
        pStmt2.setString(1, langCode);
        pStmt2.setString(2, langName);
        pStmt2.executeUpdate();
        pStmt2.close();

        dbStmt = "REPLACE INTO `ted_transcripts_paragraphs`(`resource_id`, `language`, `starttime`, `paragraph`) VALUES (?,?,?,?)";
        PreparedStatement pStmt3 = Learnweb.getInstance().getConnection().prepareStatement(dbStmt);
        pStmt3.setInt(1, resourceId);
        pStmt3.setString(2, langCode);

        JSONObject transcript = transcriptJSON.getJSONObject("transcript");
        JSONArray text = transcript.getJSONArray("text");
        for(int j = 0; j < text.length(); j++)
        {
            JSONObject contentObject = text.getJSONObject(j);
            String paragraph = contentObject.getString("content").replace("\n", " ");
            double startTime = contentObject.getDouble("start");
            int startTimeInt = (int) (startTime * 1000);

            pStmt3.setInt(3, startTimeInt);
            pStmt3.setString(4, paragraph);
            pStmt3.addBatch();
        }
        pStmt3.executeBatch();
        pStmt3.close();
    }

    public void fetchTedXTranscripts(String resourceIdAtService, int resourceId) throws SQLException
    {
        ClientResponse resp = getTedxData("http://video.google.com/timedtext?type=list&v=", resourceIdAtService);

        if(resp.getStatus() != 200)
        {
            log.error("Failed to get list of transcripts for video: " + resourceIdAtService + "and HTTP error code : " + resp.getStatus());
            return;
        }

        String response = resp.getEntity(String.class);

        try
        {
            JSONObject xmlJSONObj = XML.toJSONObject(response);
            JSONObject transcriptList = xmlJSONObj.getJSONObject("transcript_list");
            Object track = transcriptList.get("track");
            JSONArray trackJSONArray = null;

            if(track instanceof JSONArray)
            {
                trackJSONArray = (JSONArray) track;
                for(int i = 0; i < trackJSONArray.length(); i++)
                {
                    insertTedXTranscripts(resourceIdAtService, resourceId, trackJSONArray.getJSONObject(i));
                }
            }
            else
            {
                insertTedXTranscripts(resourceIdAtService, resourceId, (JSONObject) track);

            }
        }
        catch(JSONException je)
        {
            log.error(je.toString());
        }

    }

    public ClientResponse getTedxData(String url, String videoId)
    {
        Client tedxClient = Client.create();
        WebResource web = tedxClient.resource(url + videoId);

        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        return resp;
    }

    public static void main(String[] args) throws IOException, IllegalResponseException, SQLException
    {
        TedManager tm = Learnweb.getInstance().getTedManager();
        tm.fetchTedX(); //saveTedResource();
    }
}

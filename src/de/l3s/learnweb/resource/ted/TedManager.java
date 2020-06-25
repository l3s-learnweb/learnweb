package de.l3s.learnweb.resource.ted;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import de.l3s.interwebj.client.model.SearchResponse;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.InterwebResultsWrapper;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.resource.ted.Transcript.Paragraph;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class TedManager {
    private static final Logger log = LogManager.getLogger(TedManager.class);

    private static final String TRANSCRIPT_COLUMNS = "`user_id`,`resource_id`,`words_selected`,`user_annotation`,`action`,`timestamp`";
    private static final String TRANSCRIPT_SELECTION_COLUMNS = "`resource_id`,`words_selected`,`user_annotation`,`start_offset`,`end_offset`";

    public enum SummaryType {
        SHORT,
        LONG,
        DETAILED
    }

    private final Learnweb learnweb;

    public TedManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public void saveSummaryText(int userId, int resourceId, String summaryText, SummaryType summaryType) throws SQLException {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("REPLACE INTO lw_transcript_summary(user_id,resource_id,summary_type,summary_text) VALUES (?,?,?,?)");
        pStmt.setInt(1, userId);
        pStmt.setInt(2, resourceId);
        pStmt.setString(3, summaryType.name());
        pStmt.setString(4, summaryText);
        pStmt.executeUpdate();
        pStmt.close();
    }

    public void saveTranscriptLog(TranscriptLog transcriptLog) throws SQLException {
        PreparedStatement saveTranscript = learnweb.getConnection().prepareStatement("INSERT into lw_transcript_actions(" + TRANSCRIPT_COLUMNS + ") VALUES (?,?,?,?,?,?)");
        //saveTranscript.setInt(1, transcriptLog.getCourseId());
        saveTranscript.setInt(1, transcriptLog.getUserId());
        saveTranscript.setInt(2, transcriptLog.getResourceId());
        saveTranscript.setString(3, transcriptLog.getWordsSelected());
        saveTranscript.setString(4, transcriptLog.getUserAnnotation());
        saveTranscript.setString(5, transcriptLog.getAction());
        saveTranscript.setTimestamp(6, new java.sql.Timestamp(transcriptLog.getTimestamp().getTime()));
        saveTranscript.executeUpdate();
        saveTranscript.close();
    }

    public void saveTranscriptSelection(String transcript, int resourceId) throws SQLException {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("INSERT into lw_transcript_selections(" + TRANSCRIPT_SELECTION_COLUMNS + ") VALUES (?,?,?,?,?)");
        if (StringUtils.isNotEmpty(transcript)) {
            Document doc = Jsoup.parse(transcript);
            Elements elements = doc.select("span");
            for (Element element : elements) {
                int start = 0;
                int end = 0;
                if (!element.attr("data-start").isEmpty()) {
                    start = Integer.parseInt(element.attr("data-start"));
                }
                if (!element.attr("data-end").isEmpty()) {
                    end = Integer.parseInt(element.attr("data-end"));
                }

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

    public List<Transcript> getTranscripts(int resourceId) throws SQLException {
        List<Transcript> transcripts = new LinkedList<>();
        String selectTranscripts = "SELECT DISTINCT(language) as language_code FROM ted_transcripts_paragraphs WHERE resource_id = ?";
        String selectTranscriptParagraphs = "SELECT starttime, paragraph FROM ted_transcripts_paragraphs WHERE resource_id = ? AND language = ?";

        PreparedStatement ipStmt = learnweb.getConnection().prepareStatement(selectTranscriptParagraphs);

        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(selectTranscripts);
        pStmt.setInt(1, resourceId);
        ResultSet rs = pStmt.executeQuery();
        ResultSet rsParagraphs;
        while (rs.next()) {
            Transcript transcript = new Transcript();
            String languageCode = rs.getString("language_code");
            transcript.setLanguageCode(languageCode);

            ipStmt.setInt(1, resourceId);
            ipStmt.setString(2, languageCode);
            ipStmt.executeQuery();

            rsParagraphs = ipStmt.getResultSet();
            while (rsParagraphs.next()) {
                transcript.addParagraph(rsParagraphs.getInt("starttime"), rsParagraphs.getString("paragraph"));
            }

            transcripts.add(transcript);
        }
        rs.close();
        pStmt.close();
        ipStmt.close();

        return transcripts;
    }

    public int getTedVideoResourceId(String url) throws SQLException {
        int tedVideoResourceId = 0;
        String slug = url.substring(url.lastIndexOf('/') + 1);
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT resource_id FROM ted_video WHERE slug = ?");
        pStmt.setString(1, slug);
        ResultSet rs = pStmt.executeQuery();
        if (rs.next()) {
            tedVideoResourceId = rs.getInt("resource_id");
        }

        return tedVideoResourceId;
    }

    public int getTedXVideoResourceId(String url) throws SQLException {
        int tedxVideoResourceId = 0;
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT resource_id FROM lw_resource WHERE url = ? and owner_user_id = 7727");
        pStmt.setString(1, url);
        ResultSet rs = pStmt.executeQuery();
        if (rs.next()) {
            tedxVideoResourceId = rs.getInt("resource_id");
        }

        return tedxVideoResourceId;
    }

    public Map<String, String> getLangList(int resourceId) throws SQLException {
        Map<String, String> langList = new HashMap<>();
        PreparedStatement getLangList = learnweb.getConnection().prepareStatement("SELECT DISTINCT(t1.language) as language_code, t2.language FROM `ted_transcripts_paragraphs` t1 JOIN ted_transcripts_lang_mapping t2 ON t1.language=t2.language_code WHERE resource_id=?");
        getLangList.setInt(1, resourceId);
        ResultSet rs = getLangList.executeQuery();

        while (rs.next()) {
            langList.put(rs.getString("language"), rs.getString("language_code"));
        }
        rs.close();
        getLangList.close();
        return langList;
    }

    public String getTranscript(int resourceId, String language) throws SQLException {
        String selectTranscript = "SELECT `starttime`, `paragraph` FROM ted_transcripts_paragraphs where resource_id = ? AND `language` = ?";
        StringBuilder transcript = new StringBuilder();

        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(selectTranscript);
        pStmt.setInt(1, resourceId);
        pStmt.setString(2, language);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while (rs.next()) {
            int startTime = rs.getInt("starttime") / 1000;
            String para = rs.getString("paragraph");
            transcript.append(StringHelper.getDurationInMinutes(startTime)).append("\t");
            transcript.append(para).append("\n");
        }

        return transcript.toString();
    }

    public List<TranscriptSummary> getTranscriptSummaries(TreeSet<Integer> selectedUserIds) throws SQLException {
        String userIdString = StringUtils.join(selectedUserIds, ",");

        List<TranscriptSummary> transcriptSummaries = new ArrayList<>();

        if (selectedUserIds.isEmpty()) {
            return transcriptSummaries;
        }

        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM lw_transcript_summary WHERE user_id IN (" + userIdString + ") ORDER BY user_id");
        //pStmt.setInt(1, courseId);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while (rs.next()) {
            TranscriptSummary transcriptSummary = new TranscriptSummary(rs.getInt("user_id"), rs.getInt("resource_id"), rs.getString("summary_type"), rs.getString("summary_text"));
            transcriptSummaries.add(transcriptSummary);
        }
        pStmt.close();

        return transcriptSummaries;
    }

    public HashMap<SummaryType, String> getTranscriptSummariesForResource(int resourceId) throws SQLException {
        HashMap<SummaryType, String> transcriptSummaries = new HashMap<>();
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT summary_type, summary_text FROM lw_transcript_summary WHERE resource_id = ?");
        pStmt.setInt(1, resourceId);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while (rs.next()) {
            transcriptSummaries.put(SummaryType.valueOf(rs.getString(1)), rs.getString(2));
        }
        pStmt.close();

        return transcriptSummaries;

    }

    public List<TranscriptLog> getTranscriptLogs(TreeSet<Integer> selectedUserIds, boolean showDeleted) throws SQLException {
        String userIdString = StringUtils.join(selectedUserIds, ",");

        List<TranscriptLog> transcriptLogs = new ArrayList<>();

        if (selectedUserIds.isEmpty()) {
            return transcriptLogs;
        }

        String pStmtString;
        if (showDeleted) {
            //pStmtString = "SELECT " + TRANSCRIPT_COLUMNS + " FROM lw_transcript_actions WHERE course_id = ? ORDER BY user_id, timestamp DESC";
            pStmtString = "SELECT " + TRANSCRIPT_COLUMNS + " FROM lw_transcript_actions WHERE user_id IN(" + userIdString + ") ORDER BY user_id, timestamp DESC";
        } else {
            pStmtString = "SELECT " + TRANSCRIPT_COLUMNS + " FROM lw_transcript_actions JOIN lw_resource USING(resource_id) WHERE user_id IN(" + userIdString + ") and deleted = 0 ORDER BY user_id, timestamp DESC";
        }
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(pStmtString);
        //pStmt.setInt(1, courseId);
        pStmt.executeQuery();

        ResultSet rs = pStmt.getResultSet();
        while (rs.next()) {
            TranscriptLog transcriptLog = new TranscriptLog(rs.getInt("user_id"), rs.getInt("resource_id"),
                rs.getString("words_selected"), rs.getString("user_annotation"), rs.getString("action"), rs.getTimestamp("timestamp"));
            transcriptLogs.add(transcriptLog);
        }
        pStmt.close();

        return transcriptLogs;
    }

    public List<SimpleTranscriptLog> getSimpleTranscriptLogs(TreeSet<Integer> selectedUserIds, boolean showDeleted) throws SQLException {
        List<SimpleTranscriptLog> simpleTranscriptLogs = new LinkedList<>();

        //PreparedStatement getUsers = learnweb.getConnection().prepareStatement("SELECT t1.user_id FROM `lw_user_course` t1 WHERE t1.course_id = ? AND t1.user_id !=7727");
        //getUsers.setInt(1, courseId);
        //ResultSet rs = getUsers.executeQuery();
        //int userId;
        //while(rs.next())
        for (Integer userId : selectedUserIds) {
            //userId = rs.getInt("user_id");
            String pStmtString;
            if (showDeleted) {
                pStmtString = "SELECT t1.resource_id,title, SUM(action = 'selection') as selcount, SUM(action = 'deselection') as deselcount, SUM(user_annotation != '') as uacount FROM lw_resource t1 LEFT JOIN lw_transcript_actions t2 ON t1.resource_id = t2.resource_id WHERE (action = 'selection' OR action = 'deselection' OR user_annotation != '' OR action IS NULL) AND t1.owner_user_id = ? GROUP BY t1.resource_id";
            } else {
                pStmtString = "SELECT t1.resource_id,title, SUM(action = 'selection') as selcount, SUM(action = 'deselection') as deselcount, SUM(user_annotation != '') as uacount FROM lw_resource t1 LEFT JOIN lw_transcript_actions t2 ON t1.resource_id = t2.resource_id WHERE (action = 'selection' OR action = 'deselection' OR user_annotation != '' OR action IS NULL) AND t1.owner_user_id = ? AND t1.deleted = 0 GROUP BY t1.resource_id";
            }
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement(pStmtString);
            pStmt.setInt(1, userId);
            ResultSet rs2 = pStmt.executeQuery();
            while (rs2.next()) {
                SimpleTranscriptLog simpleTranscriptLog = new SimpleTranscriptLog(userId, rs2.getInt("resource_id"), rs2.getInt("selcount"), rs2.getInt("deselcount"), rs2.getInt("uacount"));
                simpleTranscriptLogs.add(simpleTranscriptLog);
            }
        }
        return simpleTranscriptLogs;
    }

    //For saving crawled ted videos into lw_resource table
    public void saveTedResource() throws SQLException, IOException, SolrServerException {
        ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
        Group tedGroup = learnweb.getGroupManager().getGroupById(862);
        User admin = learnweb.getUserManager().getUser(7727);

        PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE ted_video SET resource_id = ? WHERE ted_id = ?");

        PreparedStatement getTedVideos = learnweb.getConnection().prepareStatement("SELECT ted_id, title, description, slug, photo2_url, duration, resource_id, published_at FROM ted_video");
        getTedVideos.executeQuery();

        ResultSet rs = getTedVideos.getResultSet();
        while (rs.next()) {
            int learnwebResourceId = rs.getInt("resource_id");

            Resource tedVideo = createResource(rs, learnwebResourceId);
            int tedId = Integer.parseInt(tedVideo.getIdAtService());

            tedVideo.setMachineDescription(concatenateTranscripts(learnwebResourceId));
            tedVideo.setUser(admin);

            if (learnwebResourceId == 0 || tedVideo.getUserId() == 0) { // not yet stored in Learnweb
                rpm.processImage(tedVideo, FileInspector.openStream(tedVideo.getMaxImageUrl()));

                tedVideo.setGroup(tedGroup);
                tedVideo.setUser(admin);
                tedVideo.save();

                if (learnwebResourceId == 0) {
                    update.setInt(1, tedVideo.getId());
                    update.setInt(2, tedId);
                    update.executeUpdate();
                }

            } else if (tedVideo.getUserId() == 0) {
                rpm.processImage(tedVideo, FileInspector.openStream(tedVideo.getMaxImageUrl()));
                tedVideo.setGroup(tedGroup);
                tedVideo.setUser(admin);
                tedVideo.save();
            } else {
                tedVideo.save();
            }

            log.debug("Processed; lw: " + learnwebResourceId + " ted: " + tedId + " title:" + tedVideo.getTitle());
        }

    }

    private String concatenateTranscripts(int learnwebResourceId) throws SQLException {
        StringBuilder sb = new StringBuilder();

        for (Transcript transcript : getTranscripts(learnwebResourceId)) {
            if (transcript.getLanguageCode().equals("en") || transcript.getLanguageCode().equals("fr")
                || transcript.getLanguageCode().equals("de") || transcript.getLanguageCode().equals("es") || transcript.getLanguageCode().equals("it")) {
                for (Paragraph paragraph : transcript.getParagraphs()) {
                    sb.append(paragraph.getText());
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    private Resource createResource(ResultSet rs, int learnwebResourceId) throws SQLException {
        Resource resource = new Resource();

        if (learnwebResourceId != 0) { // the video is already stored and will be updated
            resource = learnweb.getResourceManager().getResource(learnwebResourceId);
        }

        resource.setTitle(rs.getString("title"));
        resource.setDescription(rs.getString("description"));
        resource.setUrl("https://www.ted.com/talks/" + rs.getString("slug"));
        resource.setSource(ResourceService.ted);
        resource.setLocation("TED");
        resource.setType(ResourceType.video);
        resource.setDuration(rs.getInt("duration"));
        resource.setMaxImageUrl(rs.getString("photo2_url"));
        resource.setIdAtService(Integer.toString(rs.getInt("ted_id")));
        resource.setCreationDate(rs.getTimestamp("published_at"));
        // player code is created on the fly in Resource.getEmbedded
        //resource.setEmbeddedRaw("<iframe src=\"https://embed.ted.com/talks/" + rs.getString("slug") + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" allowfullscreen></iframe>");
        resource.setTranscript("");
        return resource;
    }

    public void fetchTedX() throws IOException, IllegalArgumentException, SQLException {
        ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();

        //Group tedxGroup = learnweb.getGroupManager().getGroupById(921);
        //Group tedxTrentoGroup = learnweb.getGroupManager().getGroupById(922);
        Group tedEdGroup = learnweb.getGroupManager().getGroupById(1291);
        User admin = learnweb.getUserManager().getUser(7727);

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT resource_id FROM lw_resource WHERE url = ?");

        TreeMap<String, String> params = new TreeMap<>();
        params.put("media_types", "video");
        params.put("services", "YouTube");
        params.put("per_page", "50");
        params.put("timeout", "500");

        List<ResourceDecorator> resources;
        int page = 1; // you have to start at page one due to youtube api limitations

        do {
            params.put("page", Integer.toString(page));

            //SearchQuery interwebResponse = learnweb.getInterweb().search("user::TEDx tedxtrento", params);

            //To fetch youtube videos from TED-Ed user
            SearchResponse interwebResponse = learnweb.getInterweb().search("user::TEDEducation", params);
            InterwebResultsWrapper interwebResults = new InterwebResultsWrapper(interwebResponse);
            //log.debug(interwebResponse.getResultCountAtService());
            resources = interwebResults.getResources();

            for (ResourceDecorator decoratedResource : resources) {
                Resource resource = decoratedResource.getResource();

                resource.setSource(ResourceService.teded);
                resource.setLocation("teded");

                //Regex for setting the title and author for TEDx videos
                /*String[] title = resource.getTitle().split("\\|");
                if(title.length == 3 && title[2].startsWith(" TEDx"))
                {
                    resource.setAuthor(title[1].trim());
                    resource.setTitle(title[0] + "|" + title[2]);
                }*/

                //Regex for setting the title and author for TED-Ed videos
                String[] title = resource.getTitle().split("-");
                if (title.length == 2 && !title[1].startsWith("Ed")) {
                    resource.setAuthor(title[1].trim());
                    resource.setTitle(title[0]);
                }

                // check if resources is already stored in Learnweb
                select.setString(1, resource.getUrl());
                ResultSet rs = select.executeQuery();

                if (rs.next()) { // it is already stored
                    int resourceId = rs.getInt(1);
                    Resource learnwebResource = learnweb.getResourceManager().getResource(resourceId);

                    if (learnwebResource.getIdAtService() == null || learnwebResource.getIdAtService().isEmpty()) {
                        learnwebResource.setIdAtService(resource.getIdAtService());

                        if (learnwebResource.getGroupId() == tedEdGroup.getId()) {
                            log.error("resource is already part of the group");
                        } else {
                            learnwebResource.setGroup(tedEdGroup);
                        }
                        learnwebResource.save();
                    }

                    log.debug("Already stored: " + resource);

                } else {
                    rpm.processImage(resource, FileInspector.openStream(resource.getMaxImageUrl().replace("hqdefault", "mqdefault")));
                    resource.setGroup(tedEdGroup);
                    resource.setUser(admin);
                    resource.save();

                    log.debug("new video added");
                }

                //check if new transcripts are available for "resource" variable
                //fetchTedXTranscripts(resource.getIdAtService(), resource.getId());
            }

            page++;
            log.debug("page: " + page + " total results: " + resources.size());
            // break;
        } while (!resources.isEmpty() && page < 25);
    }

    public void insertTedXTranscripts(int resourceId, String resourceIdAtService, String langCode, String langName) throws SQLException {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM `ted_transcripts_paragraphs` WHERE `resource_id` = ? AND `language` = ?");
        select.setInt(1, resourceId);
        select.setString(2, langCode);
        ResultSet rs = select.executeQuery();
        if (rs.next()) {
            log.info("Transcript :" + langCode + " for Ted video: " + resourceId + " already inserted.");
            return; // transcript is already part of the database
        }

        String respXml = getTedxData("https://www.youtube.com/api/timedtext?lang=" + langCode + "&v=" + resourceIdAtService);
        if (respXml == null) {
            log.info("Transcript :" + langCode + " for resource ID: " + resourceIdAtService + " does not exist.");
            return; // no transcript available for this language code
        }

        PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement("REPLACE INTO `ted_transcripts_lang_mapping`(`language_code`,`language`) VALUES (?,?)");
        pStmt2.setString(1, langCode);
        pStmt2.setString(2, langName);
        pStmt2.executeUpdate();
        pStmt2.close();

        PreparedStatement pStmt3 = learnweb.getConnection().prepareStatement("REPLACE INTO `ted_transcripts_paragraphs`(`resource_id`, `language`, `starttime`, `paragraph`) VALUES (?,?,?,?)");
        pStmt3.setInt(1, resourceId);
        pStmt3.setString(2, langCode);

        Document doc = Jsoup.parse(respXml, "", Parser.xmlParser());
        Elements texts = doc.select("transcript text");

        if (!texts.isEmpty()) {
            for (Element text : texts) {
                double start = Double.parseDouble(text.attr("start"));
                // double duration = Double.parseDouble(text.attr("dur"));
                String paragraph = text.text().replace("\n", " ");

                pStmt3.setInt(3, (int) (start * 1000));
                pStmt3.setString(4, paragraph);
                pStmt3.addBatch();
            }
        }

        pStmt3.executeBatch();
        pStmt3.close();
    }

    public void fetchTedXTranscripts(String resourceIdAtService, int resourceId) throws SQLException {
        String respXml = getTedxData("https://www.youtube.com/api/timedtext?type=list&v=" + resourceIdAtService);
        if (respXml == null) {
            log.error("Failed to get list of transcripts for video: {}", resourceIdAtService);
            return; // no transcript available for this language code
        }

        Document doc = Jsoup.parse(respXml, "", Parser.xmlParser());
        Elements tracks = doc.select("transcript_list track");

        if (!tracks.isEmpty()) {
            for (Element track : tracks) {
                String langCode = track.attr("lang_code");
                String langName = track.attr("lang_translated");

                insertTedXTranscripts(resourceId, resourceIdAtService, langCode, langName);
            }
        }
    }

    public static String getTedxData(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Accept", "application/xml").build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200 && !resp.body().isEmpty()) {
                return resp.body();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Unexpected error during request for transcript", e);
        }

        return null;
    }

    //Remove duplicate TED Resources from group 862 starting from the resourceId
    public void removeDuplicateTEDResources(int startFromResourceId) throws SQLException {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_resource` WHERE group_id = 862 AND owner_user_id = 7727 AND deleted = 0 AND resource_id > ?");
        pStmt.setInt(1, startFromResourceId);
        ResultSet rs = pStmt.executeQuery();
        while (rs.next()) {
            int resId = rs.getInt("resource_id");
            PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement("SELECT * FROM ted_video WHERE resource_id = ?");
            pStmt2.setInt(1, resId);
            ResultSet rs2 = pStmt2.executeQuery();
            boolean existsInTedVideo = false;
            if (rs2.next()) {
                existsInTedVideo = true;
            }
            pStmt2.close();

            if (!existsInTedVideo) {
                //log.debug(learnweb.getResourceManager().getResource(resId));

                learnweb.getResourceManager().deleteResource(resId);

                PreparedStatement pStmt3 = learnweb.getConnection().prepareStatement("DELETE FROM ted_transcripts_paragraphs WHERE resource_id = ?");
                pStmt3.setInt(1, resId);
                int deleted = pStmt3.executeUpdate();
                log.info("Deleted(" + deleted + ") transcripts for duplicate TED video: " + resId);
                pStmt3.close();
            }
        }
        pStmt.close();
    }

    //Link existing resources to TED resources in the original TED group
    public void linkResourcesToTEDResources() throws SQLException {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT resource_id FROM lw_resource WHERE url LIKE '%ted.com/talks%' AND source != 'TED'");
        ResultSet rs = pStmt.executeQuery();
        while (rs.next()) {
            Resource r = learnweb.getResourceManager().getResource(rs.getInt("resource_id"));
            int tedVideoResourceId = getTedVideoResourceId(r.getUrl());
            log.info(tedVideoResourceId);
            if (tedVideoResourceId > 0) {
                Resource r2 = learnweb.getResourceManager().getResource(tedVideoResourceId);

                r.setSource(ResourceService.ted);
                r.setMaxImageUrl(r2.getMaxImageUrl());
                List<File> files = learnweb.getFileManager().getFilesByResource(r.getId());
                for (File f : files) {
                    learnweb.getFileManager().delete(f.getId());
                }
                r.setThumbnail0(r2.getThumbnail0());
                r.setThumbnail1(r2.getThumbnail1());
                r.setThumbnail2(r2.getThumbnail2());
                r.setThumbnail3(r2.getThumbnail3());
                r.setThumbnail4(r2.getThumbnail4());
                r.setDuration(r2.getDuration());
                r.setIdAtService(r2.getIdAtService());
                r.setType(r2.getType());
                r.setFormat(r2.getFormat());
                r.save();
            }
        }
        pStmt.close();
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        Learnweb lw = Learnweb.createInstance();
        TedManager tm = lw.getTedManager();
        tm.removeDuplicateTEDResources(215353);
        lw.onDestroy();
    }
}

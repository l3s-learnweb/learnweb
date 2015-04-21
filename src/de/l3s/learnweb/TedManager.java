package de.l3s.learnweb;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.Transcript.Paragraph;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.SolrClient;
import de.l3s.util.StringHelper;

public class TedManager
{
    private final static Logger log = Logger.getLogger(TedManager.class);

    private final static String TRANSCRIPT_COLUMNS = "`course_id`,`user_id`,`resource_id`,`words_selected`,`user_annotation`,`action`,`timestamp`";
    //private final static String RESOURCE_COLUMNS = "r.resource_id, r.title, r.description, r.url, r.storage_type, r.rights, r.source, r.type, r.format, r.owner_user_id, r.rating, r.rate_number, r.embedded_size1, r.embedded_size2, r.embedded_size3, r.embedded_size4, r.filename, r.max_image_url, r.query, r.original_resource_id, r.author, r.access, r.thumbnail0_url, r.thumbnail0_file_id, r.thumbnail0_width, r.thumbnail0_height, r.thumbnail1_url, r.thumbnail1_file_id, r.thumbnail1_width, r.thumbnail1_height, r.thumbnail2_url, r.thumbnail2_file_id, r.thumbnail2_width, r.thumbnail2_height, r.thumbnail3_url, r.thumbnail3_file_id, r.thumbnail3_width, r.thumbnail3_height, r.thumbnail4_url, r.thumbnail4_file_id, r.thumbnail4_width, r.thumbnail4_height, r.embeddedRaw, r.transcript, r.online_status";

    private final Learnweb learnweb;

    public TedManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;
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

    public List<Transcript> getTransscripts(int tedId) throws SQLException
    {
	List<Transcript> transcripts = new ArrayList<Transcript>();
	String selectTranscripts = "SELECT language_code FROM ted_transcripts WHERE ted_id = ?";
	String selectTranscriptParagraphs = "SELECT starttime, paragraph FROM ted_transcripts_paragraphs WHERE ted_id = ? AND language = ?";

	PreparedStatement ipStmt = Learnweb.getInstance().getConnection().prepareStatement(selectTranscriptParagraphs);

	PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement(selectTranscripts);
	pStmt.setInt(1, tedId);
	ResultSet rs = pStmt.executeQuery(), rsParagraphs;
	while(rs.next())
	{
	    Transcript transcript = new Transcript();
	    String languageCode = rs.getString("language_code");
	    transcript.setLanguageCode(languageCode);
	    transcript.setParagraphs(new ArrayList<Transcript.Paragraph>());

	    ipStmt.setInt(1, tedId);
	    ipStmt.setString(2, languageCode);
	    ipStmt.executeQuery();

	    rsParagraphs = ipStmt.getResultSet();
	    while(rsParagraphs.next())
	    {
		transcript.getParagraphs().add(transcript.new Paragraph(rsParagraphs.getInt("starttime"), rsParagraphs.getString("paragraph")));
	    }

	    transcripts.add(transcript);
	}
	rs.close();
	pStmt.close();
	ipStmt.close();

	return transcripts;
    }

    public int getTedId(String url) throws SQLException
    {

	int tedId = 0;

	String slug = url.substring(url.lastIndexOf("/") + 1, url.length());
	PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement("SELECT ted_id FROM ted_video WHERE slug = ?");
	pStmt.setString(1, slug);
	ResultSet rs = pStmt.executeQuery();
	if(rs.next())
	{
	    tedId = rs.getInt("ted_id");
	}

	return tedId;
    }

    public Map<String, String> getLangList(int tedId) throws SQLException
    {
	String langFromPropFile;
	Map<String, String> langList = new HashMap<String, String>();
	PreparedStatement getLangList = learnweb.getConnection().prepareStatement("SELECT language_code, language FROM ted_transcripts WHERE ted_id=?");
	getLangList.setInt(1, tedId);
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

    public String getTranscript(int tedId, String language) throws SQLException
    {
	String selectTranscript = "SELECT `starttime`, `paragraph` FROM ted_transcripts_paragraphs where ted_id = ? AND `language` = ?";
	String transcript = "";

	PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement(selectTranscript);
	pStmt.setInt(1, tedId);
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

    public List<TranscriptLog> getTranscriptLogs(int courseId) throws SQLException
    {
	List<TranscriptLog> transcriptLogs = new ArrayList<TranscriptLog>();

	PreparedStatement getTranscriptsLog = learnweb.getConnection().prepareStatement("SELECT " + TRANSCRIPT_COLUMNS + " FROM lw_transcript_actions WHERE course_id = ? ORDER BY user_id, timestamp DESC");
	getTranscriptsLog.setInt(1, courseId);
	getTranscriptsLog.executeQuery();

	ResultSet rs = getTranscriptsLog.getResultSet();
	while(rs.next())
	{
	    TranscriptLog transcriptLog = new TranscriptLog(rs.getInt("course_id"), rs.getInt("user_id"), rs.getInt("resource_id"), rs.getString("words_selected"), rs.getString("user_annotation"), rs.getString("action"), rs.getTimestamp("timestamp"));
	    transcriptLogs.add(transcriptLog);
	}
	getTranscriptsLog.close();

	return transcriptLogs;
    }

    //For saving crawled ted videos into lw_resource table
    public void saveTedResource() throws SQLException, IOException, SolrServerException
    {
	ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
	SolrClient solr = learnweb.getSolrClient();
	Group tedGroup = learnweb.getGroupManager().getGroupById(862);
	User admin = learnweb.getUserManager().getUser(7727);

	PreparedStatement update = Learnweb.getInstance().getConnection().prepareStatement("UPDATE ted_video SET resource_id = ? WHERE ted_id = ?");

	PreparedStatement getTedVideos = Learnweb.getInstance().getConnection().prepareStatement("SELECT ted_id, title, description, slug, photo2_url, duration, resource_id FROM ted_video ");
	getTedVideos.executeQuery();

	ResultSet rs = getTedVideos.getResultSet();
	while(rs.next())
	{
	    int learnwebResourceId = rs.getInt("resource_id");

	    Resource tedVideo = createResource(rs, learnwebResourceId);
	    int tedId = Integer.parseInt(tedVideo.getIdAtService());

	    tedVideo.setMachineDescription(concatenateTranscripts(tedId));
	    tedVideo.setOwner(admin);

	    if(learnwebResourceId == 0) // not yet stored in Learnweb

	    {
		rpm.processImage(tedVideo, FileInspector.openStream(tedVideo.getMaxImageUrl()));

		//tedVideo.save(); TODO test if this caused problems

		update.setInt(1, tedVideo.getId());
		update.setInt(2, tedId);
		update.executeUpdate();

		admin.addResource(tedVideo);
		tedGroup.addResource(tedVideo, admin);

		solr.indexResource(tedVideo);

	    }
	    else
		tedVideo.save();

	    log.debug("Processed; lw: " + learnwebResourceId + " ted: " + tedId + " title:" + tedVideo.getTitle());
	}

    }

    private String concatenateTranscripts(int tedId) throws SQLException
    {
	StringBuilder sb = new StringBuilder();

	for(Transcript transcript : getTransscripts(tedId))
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

	if(learnwebResourceId != 0) // the video is alreay stored and will be updated
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

	resource.setEmbeddedRaw("<iframe src=\"http://embed.ted.com/talks/" + rs.getString("slug") + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");
	resource.setTranscript("");

	return resource;
    }

    public static void main(String[] args) throws Exception
    {

	TedManager tm = Learnweb.getInstance().getTedManager();
	tm.saveTedResource();
    }
}

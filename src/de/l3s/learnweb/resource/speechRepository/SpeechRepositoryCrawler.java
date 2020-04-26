package de.l3s.learnweb.resource.speechRepository;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;

public class SpeechRepositoryCrawler implements Runnable
{
    private static final Logger log = LogManager.getLogger(SpeechRepositoryCrawler.class);

    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("\\(([a-z]{2,3})\\)");
    private static final Pattern DATE_PATTERN = Pattern.compile("([^\\s]+)$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);
    private static final int TIMEOUT = 60 * 1000;

    private Learnweb learnweb;

    public SpeechRepositoryCrawler()
    {

    }

    public void initialize()
    {
        learnweb = Learnweb.getInstance();
    }

    public void start()
    {
        try
        {
            String nextUrl = "https://webgate.ec.europa.eu/sr/search-speeches?entity%5B0%5D=&language=All&level=All&use=All&domain=All&type=All&title=&combine=&combine_1=&video_reference=&order=nid&sort=desc";

            int pageNumber = 0;
            while(nextUrl != null)
            {
                log.info("Getting page " + pageNumber++);
                nextUrl = visitCategoryPage(nextUrl);
            }
        }
        catch(Exception e)
        {
            log.error("Error while fetching speech repository page:1", e);
        }
    }

    /**
     * Extract individual speech repository URLs from the menu page
     */
    private String visitCategoryPage(String categoryPageUrl)
    {
        try
        {
            Document doc = Jsoup.connect(categoryPageUrl).timeout(TIMEOUT).get();
            Element content = doc.select("#block-system-main").first();
            Element paginationElement = content.select(".item-list > .pager").first();

            Element nextCategoryPage = paginationElement.select(".pager-next a").first();
            String nextCategoryPageUrl = nextCategoryPage == null ? null : nextCategoryPage.attr("href");

            Element tableElement = content.select(".view-content table").first();
            Elements tableRows = tableElement.select("tbody > tr");

            for(Element tableRow : tableRows)
            {
                final int pageId = Integer.parseInt(tableRow.select(".views-field-nid").text());
                final String pageUrl = tableRow.select(".views-field-title a").attr("href");

                try
                {
                    if (!isPageSaved(pageId)) {
                        visitPage(pageUrl);
                        TimeUnit.SECONDS.sleep(5);
                    }
                }
                catch(Exception e)
                {
                    log.error("Error while fetching speech repository page: " + pageUrl, e);
                }
            }

            return nextCategoryPageUrl;
        }
        catch(IOException e)
        {
            log.error("Error while fetching speech repository page: " + categoryPageUrl, e);
        }

        return null;
    }

    /**
     * Extract data about a particular speech repository given the URL
     * Update the data if speech already exists, if not - insert the new speech repository to the database
     */
    private void visitPage(final String pageUrl) throws IOException, SQLException
    {
        Document doc = Jsoup.connect(pageUrl).timeout(TIMEOUT).get();
        Element content = doc.select("#content > .content-inner").first();
        Element speechElement = content.select("#content-area .node-speech").first();
        Element speechDetailsElement = speechElement.select("#node-speech-full-group-speech-details").first();

        SpeechRepositoryEntity speechEntity = new SpeechRepositoryEntity();
        speechEntity.setUrl(pageUrl);
        speechEntity.setTitle(content.select("#content-header h1").text());
        speechEntity.setRights(speechElement.select(".field-name-field-rights").text());
        speechEntity.setDate(speechElement.select(".field-name-field-date").text());
        speechEntity.setDescription(speechElement.select(".field-name-body .field-items").text());
        speechEntity.setNotes(speechElement.select(".field-name-field-notes .field-items").text());

        // extracting details
        for(Element element : speechDetailsElement.select(".field"))
        {
            String key = element.select(".field-label").text()
                    .replace(":", "").replace("\u00a0", " ").trim();
            String value = element.select(".field-items").text();

            if(key.contains("Duration"))
            {
                speechEntity.setDuration(value);
            }
            else if(key.contains("Language"))
            {
                speechEntity.setLanguage(value);
            }
            else if(key.contains("Level"))
            {
                speechEntity.setLevel(value);
            }
            else if(key.contains("Use"))
            {
                speechEntity.setUse(value);
            }
            else if(key.contains("Type"))
            {
                speechEntity.setType(value);
            }
            else if(key.contains("Domains"))
            {
                speechEntity.setDomains(value);
            }
            else if(key.contains("Terminology"))
            {
                speechEntity.setTerminology(value);
            }
        }

        // extracting video url
        for(Element element : doc.select("script").not("[src]"))
        {
            String scriptData = element.data();
            if(scriptData != null && !scriptData.isEmpty() && scriptData.contains("jQuery.extend(Drupal.settings"))
            {
                scriptData = scriptData.substring(scriptData.indexOf('{'), scriptData.lastIndexOf('}') + 1);

                JSONObject jsonObject = new JSONObject(scriptData);
                JSONObject mediaPlayer = jsonObject.getJSONObject("ecspTranscodingPlayers").getJSONObject("ecsp-media-player");

                if(mediaPlayer.has("image"))
                {
                    speechEntity.setImageLink(mediaPlayer.getString("image"));
                }

                if(mediaPlayer.has("entity_id"))
                {
                    speechEntity.setId(mediaPlayer.getString("entity_id"));
                }

                for(Object source : mediaPlayer.getJSONArray("sources"))
                {
                    JSONObject objectSource = (JSONObject) source;
                    if(!"auto".equals(objectSource.getString("label")))
                    {
                        speechEntity.setVideoLink(objectSource.getString("file"));
                        break;
                    }
                }
            }
        }

        Resource resource = createResource(speechEntity);
        speechEntity.setLearnwebResourceId(resource.getId());

        savePage(speechEntity);
    }

    private boolean isPageSaved(final int pageId) throws SQLException
    {
        // check the database to identify if the video has already been crawled
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT DISTINCT id FROM speechrepository_video WHERE id = ?");
        pStmt.setInt(1, pageId);
        ResultSet rs = pStmt.executeQuery();
        return rs.next();
    }

    /**
     * Extract data about a particular speech repository given the URL
     * Update the data if speech already exists, if not - insert the new speech repository to the database
     */
    private void savePage(final SpeechRepositoryEntity speechEntity) throws SQLException
    {
        PreparedStatement preparedStatement = learnweb.getConnection()
                .prepareStatement("INSERT INTO speechrepository_video (id, title, url, rights, date, description, notes, image_link, video_link, duration, language, level, `use`, type, domains, terminology, learnweb_resource_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        preparedStatement.setInt(1, speechEntity.getId());
        preparedStatement.setString(2, speechEntity.getTitle());
        preparedStatement.setString(3, speechEntity.getUrl());
        preparedStatement.setString(4, speechEntity.getRights());
        preparedStatement.setString(5, speechEntity.getDate());
        preparedStatement.setString(6, speechEntity.getDescription());
        preparedStatement.setString(7, speechEntity.getNotes());
        preparedStatement.setString(8, speechEntity.getImageLink());
        preparedStatement.setString(9, speechEntity.getVideoLink());
        preparedStatement.setInt(10, speechEntity.getDuration());
        preparedStatement.setString(11, speechEntity.getLanguage());
        preparedStatement.setString(12, speechEntity.getLevel());
        preparedStatement.setString(13, speechEntity.getUse());
        preparedStatement.setString(14, speechEntity.getType());
        preparedStatement.setString(15, speechEntity.getDomains());
        preparedStatement.setString(16, speechEntity.getTerminology());
        preparedStatement.setInt(17, speechEntity.getLearnwebResourceId());

        int val = preparedStatement.executeUpdate();
        if(val != 1)
            log.error("Inserting speech repository video resource was not successful: {}", speechEntity.getId());
        else {
            log.info("New Speechrepository video was added: {} - {}", speechEntity.getId(), speechEntity.getTitle());
        }
    }

    private Resource createResource(final SpeechRepositoryEntity speechEntity) throws SQLException, IOException
    {
        if (speechEntity.getLearnwebResourceId() > 0)
            return learnweb.getResourceManager().getResource(speechEntity.getLearnwebResourceId());

        Resource resource = new Resource();
        resource.setTitle(speechEntity.getTitle());
        resource.setDescription(speechEntity.getDescription() + "\\n<br/>\n" + speechEntity.getNotes());
        resource.setUrl(speechEntity.getUrl());
        resource.setMaxImageUrl(speechEntity.getImageLink());
        resource.setFileUrl(speechEntity.getVideoLink());
        resource.setSource(ResourceService.speechrepository);
        resource.setType(ResourceType.video);
        resource.setDuration(speechEntity.getDuration());
        resource.setIdAtService(String.valueOf(speechEntity.getId()));

        resource.setMetadataValue("language_level", speechEntity.getLevel());
        resource.setMetadataValue("use", speechEntity.getUse());
        resource.setMetadataValue("type", speechEntity.getType());
        resource.setMetadataValue("domains", speechEntity.getDomains());
        resource.setMetadataValue("terminology", speechEntity.getTerminology());
        //resource.setTranscript("");

        // parse language, example: ???
        if(StringUtils.isNotBlank(speechEntity.getLanguage()))
        {
            Matcher matcher = LANGUAGE_PATTERN.matcher(speechEntity.getLanguage());
            if (matcher.find())
                resource.setLanguage(matcher.group(1));
            else
                log.error("Did not expect this lang value {}, speechEntity {}", speechEntity.getLanguage(), speechEntity.getId());
        }

        // parse date, example: Bruxelles, 09/04/2018
        if(StringUtils.isNotBlank(speechEntity.getDate()))
        {
            String dateStr = speechEntity.getDate();
            Matcher matcher = DATE_PATTERN.matcher(dateStr);
            if(matcher.find())
                resource.setCreationDate(Date.from(LocalDate.parse(matcher.group(1), DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC)));
            else
                log.error("Did not expect this date value {}, speechEntity {}", speechEntity.getDate(), speechEntity.getId());
        }

        saveResource(resource);
        return resource;
    }

    private void saveResource(final Resource resource) throws SQLException, IOException
    {
        resource.setGroupId(1401);
        resource.setUserId(7727);

        // save preview images
        learnweb.getResourcePreviewMaker().processImage(resource, FileInspector.openStream(resource.getMaxImageUrl()));

        resource.save();
    }

    @Override
    public void run()
    {
        initialize();
        start();
    }

    private void createResourcesForExistingSpeechrepositoryEntities() throws SQLException, IOException
    {
        ResultSet rs = learnweb.getConnection().createStatement().executeQuery("SELECT * FROM speechrepository_video WHERE learnweb_resource_id = 0");
        while(rs.next())
        {
            SpeechRepositoryEntity speechEntity = createSpeechRepositoryEntity(rs);
            log.debug("process entry id {}", speechEntity.getId());

            // create Learnweb resource for given speechEntity
            createResource(speechEntity);

            // save resource_id
            PreparedStatement updateId = learnweb.getConnection().prepareStatement("UPDATE speechrepository_video SET learnweb_resource_id = ? WHERE id = ?");
            updateId.setInt(1, speechEntity.getLearnwebResourceId());
            updateId.setInt(2, speechEntity.getId());
            updateId.executeUpdate();
            updateId.close();
        }
        rs.close();
        learnweb.onDestroy();
    }

    private SpeechRepositoryEntity createSpeechRepositoryEntity(ResultSet rs) throws SQLException
    {
        SpeechRepositoryEntity speechEntity = new SpeechRepositoryEntity();
        speechEntity.setId(rs.getInt("id"));
        speechEntity.setTitle(rs.getString("title"));
        speechEntity.setUrl(rs.getString("url"));
        speechEntity.setRights(rs.getString("rights"));
        speechEntity.setDate(rs.getString("date"));
        speechEntity.setDescription(rs.getString("description"));
        speechEntity.setNotes(rs.getString("notes"));
        speechEntity.setImageLink(rs.getString("image_link"));
        speechEntity.setVideoLink(rs.getString("video_link"));
        speechEntity.setDuration(rs.getInt("duration"));
        speechEntity.setLanguage(rs.getString("language"));
        speechEntity.setLevel(rs.getString("level"));
        speechEntity.setUse(rs.getString("use"));
        speechEntity.setType(rs.getString("type"));
        speechEntity.setDomains(rs.getString("domains"));
        speechEntity.setTerminology(rs.getString("terminology"));
        speechEntity.setLearnwebResourceId(rs.getInt("learnweb_resource_id"));
        return speechEntity;
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException
    {
        @SuppressWarnings("unused") // required for Learnweb.getInstance() to work properly inside .initialize() method
        Learnweb learnweb = Learnweb.createInstance();

        SpeechRepositoryCrawler speechRepositoryCrawler = new SpeechRepositoryCrawler();
        speechRepositoryCrawler.initialize();
        speechRepositoryCrawler.run();

        speechRepositoryCrawler.createResourcesForExistingSpeechrepositoryEntities();

        System.exit(0);
    }
}

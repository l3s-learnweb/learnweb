package de.l3s.learnweb.resource.speechRepository;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.learnweb.Learnweb;

public class SpeechRepositoryCrawlerSimple implements Runnable
{
    private static final Logger log = Logger.getLogger(SpeechRepositoryCrawlerSimple.class);

    private static final int TIMEOUT = 60 * 1000;

    private Learnweb learnweb;

    public SpeechRepositoryCrawlerSimple()
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
    private void visitPage(final String pageUrl) throws IOException, JSONException, SQLException
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
                    if(!objectSource.getString("label").equals("auto"))
                    {
                        speechEntity.setVideoLink(objectSource.getString("file"));
                        break;
                    }
                }
            }
        }

        savePage(speechEntity);
    }

    private boolean isPageSaved(final int pageId) throws JSONException, SQLException
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
    private void savePage(final SpeechRepositoryEntity speechEntity) throws JSONException, SQLException
    {
        PreparedStatement preparedStatement = learnweb.getConnection()
                .prepareStatement("INSERT INTO speechrepository_video (id, title, url, rights, date, description, notes, image_link, video_link, duration, language, level, `use`, type, domains, terminology) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
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

        int val = preparedStatement.executeUpdate();
        if(val != 1)
            log.error("Inserting speech repository video resource was not successful: " + speechEntity.getId());
        else {
            log.info("New Speechrepository video was added: " + speechEntity.getId() + " - " + speechEntity.getTitle());
        }
    }

    @Override
    public void run()
    {
        initialize();
        start();
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException
    {
        @SuppressWarnings("unused") // required for Learnweb.getInstance() to work properly inside .initialize() method
        Learnweb learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");

        SpeechRepositoryCrawlerSimple speechRepositoryCrawler = new SpeechRepositoryCrawlerSimple();
        speechRepositoryCrawler.run();
        System.exit(0);
    }
}

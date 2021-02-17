package de.l3s.learnweb.resource.ted;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.User;
import de.l3s.util.Misc;

public class TedCrawlerSimple implements Runnable {
    private static final Logger log = LogManager.getLogger(TedCrawlerSimple.class);

    private ResourcePreviewMaker rpm;
    private Group tedGroup;
    private User admin;

    private Learnweb learnweb;

    public TedCrawlerSimple() {

    }

    public void initialize() {
        try {
            learnweb = Learnweb.getInstance();
            rpm = learnweb.getResourcePreviewMaker();
            tedGroup = learnweb.getGroupManager().getGroupById(862);
            admin = learnweb.getUserManager().getUser(7727);
        } catch (SQLException e) {
            log.error("Error while initializing TED crawler", e);
        }
    }

    /**
     * Extract the transcript corresponding to a particular TED talk (TED id) and language.
     */
    public void extractTranscript(String tedId, int resourceId, String language) {
        try {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("INSERT INTO learnweb_large.ted_transcripts_paragraphs (`resource_id`, `language`, `starttime`, `paragraph`) VALUES (?,?,?,?)");
            pStmt.setInt(1, resourceId);
            pStmt.setString(2, language);

            InputStream inputStream = new URL("https://www.ted.com/talks/" + tedId + "/transcript.json?language=" + language).openStream();
            String transcriptJSONStr = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            JsonObject transcriptJsonObj = JsonParser.parseString(transcriptJSONStr).getAsJsonObject();
            JsonArray paragraphs = transcriptJsonObj.get("paragraphs").getAsJsonArray();

            for (JsonElement paragraph1 : paragraphs) {
                JsonObject paragraph = paragraph1.getAsJsonObject();
                //'cues' is a json array to split the paragraph into text segments to highlight in parallel while watching the video
                JsonArray cues = paragraph.get("cues").getAsJsonArray();
                JsonObject firstCue = cues.get(0).getAsJsonObject();
                long startTime = firstCue.get("time").getAsLong();
                StringBuilder paragraphText = new StringBuilder(firstCue.get("text").getAsString());
                for (int j = 1, len = cues.size(); j < len; j++) {
                    JsonObject cue = cues.get(j).getAsJsonObject();
                    paragraphText.append(" ").append(cue.get("text").getAsString());
                }
                paragraphText = new StringBuilder(paragraphText.toString().replace("\n", " "));
                //log.info("start time: " + startTime + " paragraph: " + paragraphText);
                pStmt.setInt(3, Math.toIntExact(startTime));
                pStmt.setString(4, paragraphText.toString());
                pStmt.executeUpdate();
            }

            pStmt.close();
        } catch (IOException e) {
            log.warn("Error while fetching transcript (" + language + ") for ted talk: " + resourceId, e);
        } catch (JsonParseException e) {
            log.error("Error while parsing transcript json for ted talk: " + resourceId + " and language: " + language, e);
        } catch (SQLException e) {
            log.error("Error while inserting transcript for ted talk: " + resourceId + " and language: " + language, e);

        }
    }

    public void start() {
        try {
            String tedTalksURLPrefix = "http://www.ted.com/talks?";
            Document doc = Jsoup.parse(new URL(tedTalksURLPrefix + "page=1"), 10000);

            Element lastBrowsingPageEl = doc.select("a.pagination__item").last();
            String lastPage = lastBrowsingPageEl.text();
            int totalPages = Integer.parseInt(lastPage);
            log.info("Total no. of pages: " + totalPages);

            for (int i = 1; i <= totalPages; i++) {
                visitTedTalksPage(tedTalksURLPrefix + "page=" + i);
            }
        } catch (IOException e) {
            log.error("Error while fetching TED talks page:1", e);
        }
    }

    /**
     * Extract individual TED talk URLs from the talks menu page.
     */
    public void visitTedTalksPage(String tedTalksPageUrl) {
        try {
            Document doc = Jsoup.parse(new URL(tedTalksPageUrl), 10000);

            Elements tedTalkURLs = doc.select("div.talk-link div.media__message a");
            for (Element tedTalkUrlEl : tedTalkURLs) {
                String tedTalkURL = tedTalkUrlEl.attr("abs:href");
                visit(tedTalkURL);
                TimeUnit.SECONDS.sleep(5);
            }
        } catch (IOException | IllegalStateException e) {
            log.error("Error while fetching ted talks page: " + tedTalksPageUrl, e);
        } catch (InterruptedException e) {
            log.error("Interrupted execution while visiting ted talks page: " + tedTalksPageUrl, e);
        }
    }

    public int checkTEDIdExists(int tedId) {
        int resourceId = -1;
        try {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT resource_id FROM learnweb_large.ted_video WHERE ted_id = ?");
            pStmt.setInt(1, tedId);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                resourceId = rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error while fetching resource id for ted id: " + tedId, e);
        }

        return resourceId;
    }

    public void updateResourceData(int resourceId, String slugFromCrawl, String title, String description) {
        try {
            Resource r = learnweb.getResourceManager().getResource(resourceId);
            String slug = r.getUrl().split("talks/")[1];
            if (!slug.equals(slugFromCrawl)) {
                r.setTitle(title);
                r.setDescription(description);
                r.setUrl("http://www.ted.com/talks/" + slugFromCrawl);
                r.save();

                PreparedStatement pStmt = learnweb.getConnection().prepareStatement("UPDATE learnweb_large.ted_video SET `title` = ?, description = ?, slug = ? WHERE `resource_id` = ?");
                pStmt.setString(1, title);
                pStmt.setString(2, description);
                pStmt.setString(3, slugFromCrawl);
                pStmt.setInt(4, resourceId);
                int dbReturnVal = pStmt.executeUpdate();
                if (dbReturnVal == 1) {
                    log.info("Updated existing ted video: " + resourceId);
                }
            }
        } catch (SQLException e) {
            log.error("Error while updating existing resource: " + resourceId, e);
        }
    }

    /**
     * Extract data about a particular TED talk given the URL.
     * Update the data if talk already exists if not insert the new TED talk to the database.
     */
    public void visit(String url) {
        HashSet<String> languageSet = new HashSet<>();
        HashSet<String> languageListFromDatabase = new HashSet<>();

        StringBuilder keywords = new StringBuilder();
        String title;
        String description;
        //String url = page.getWebURL().getURL();

        String[] sub = url.split("talks/");
        String slug = sub[1];
        int resourceId = -1;
        String tedId;

        try {
            // check the database to identify if the video has already been crawled or if any new transcripts are added to the video
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT DISTINCT resource_id, language FROM learnweb_large.ted_video JOIN learnweb_large.ted_transcripts_paragraphs USING(resource_id) WHERE slug = ?");
            pStmt.setString(1, slug);
            ResultSet rs = pStmt.executeQuery();

            while (rs.next()) {
                resourceId = rs.getInt(1);
                String languageCode = rs.getString(2);
                if (languageCode != null) {
                    languageListFromDatabase.add(languageCode);
                }
            }

        } catch (SQLException e) {
            log.error("Error while checking if ted video exists for slug: " + slug, e);
        }

        Document doc = null;
        Response response = null;
        int retries = 3; // number of retries in case the connection fails
        Exception lastException = null;

        while (retries > 0) {
            try {
                retries--;

                response = Jsoup.connect(url).timeout(10000).execute();
                doc = response.parse(); // Jsoup.parse(new URL(url), 10000);

                break;
            } catch (IOException e) {
                if (response != null) {
                    log.warn("Error while fetching ted talks page: " + slug + "; response: " + response.statusCode() + ", " + response.statusMessage(), e);
                }

                lastException = e;
                Misc.sleep(60000);
            }
        }

        if (null == doc) {
            log.error("Can't fetch ted talks page: " + slug, lastException);
            return;
        }

        //Since there is no explicit meta property for ted id, it is extracted like below in order to be able to get transcripts
        Element iosURLEl = doc.select("meta[property=al:ios:url]").first();
        if (iosURLEl != null) {
            tedId = iosURLEl.attr("content").split("ted://talks/")[1];
            tedId = tedId.replace("?source=facebook", "");
        } else {
            return; //Few TED talks have broken links and it redirects it to the homepage
        }

        //Checking again if TED video exists since sometimes the slug of an existing video can change
        if (resourceId == -1) {
            resourceId = checkTEDIdExists(Integer.parseInt(tedId));
        }

        Element titleEl = doc.select("meta[name=title]").first();
        title = titleEl.attr("content");

        Element descriptionEl = doc.select("meta[name=description]").first();
        description = descriptionEl.attr("content");

        //if the videos are new, crawl for the basic attributes such as title, speaker, transcripts
        if (resourceId == -1) {
            log.info("Crawling new ted talk: " + slug);
            log.info("ted id: " + tedId);

            Element totalViewsEl = doc.select("meta[itemprop=interactionCount]").first();
            String totalViews = totalViewsEl.attr("content");
            log.info("total views: " + totalViews);

            log.info("title: " + title);

            //Element keywordsEl = doc.select("meta[name=keywords]").first();
            //keywords = keywordsEl.attr("content");
            //log.info("keywords: " + keywords);

            Element imgLinkEl = doc.select("meta[property=og:image]").first();
            String maxImageUrl = imgLinkEl.attr("content");
            log.info("Max Image URL: " + maxImageUrl);

            Element imageHeightElement = doc.select("meta[property=og:image:height]").first();
            int imageHeight = Integer.parseInt(imageHeightElement.attr("content"));

            Element imageWidthElement = doc.select("meta[property=og:image:width]").first();
            int imageWidth = Integer.parseInt(imageWidthElement.attr("content"));

            log.info("Image height: " + imageHeight + "; Image width: " + imageWidth);

            Element durationEl = doc.select("meta[property=og:video:duration]").first();
            int duration = (int) Float.parseFloat(durationEl.attr("content"));
            log.info("Duration: " + duration);

            Element releaseDateEl = doc.select("meta[property=og:video:release_date]").first();
            Date publishedAt = new Date(Integer.parseInt(releaseDateEl.attr("content")) * 1000L);

            Elements tags = doc.select("meta[property=og:video:tag]");
            for (Element tag : tags) {
                keywords.append(tag.attr("content")).append(",");
            }

            if (keywords.length() > 0) {
                keywords = new StringBuilder(keywords.substring(0, keywords.length() - 1));
            }
            log.info("keywords: " + keywords);

            Resource tedResource = new Resource();
            tedResource.setTitle(title);
            tedResource.setDescription(description);
            tedResource.setUrl("https://www.ted.com/talks/" + slug);
            tedResource.setSource(ResourceService.ted);
            tedResource.setType(ResourceType.video);
            tedResource.setDuration(duration);
            tedResource.setMaxImageUrl(maxImageUrl);
            tedResource.setCreationDate(publishedAt);
            tedResource.setIdAtService(tedId);
            tedResource.setTranscript("");

            try {
                rpm.processImage(tedResource, FileInspector.openStream(tedResource.getMaxImageUrl()));
                tedResource.setGroup(tedGroup);
                tedResource.setUser(admin);
                tedResource.save();

                //save new TED resource ID in order to use it later for saving transcripts
                resourceId = tedResource.getId();

                String insertStmt = "INSERT INTO learnweb_large.ted_video (`ted_id`,`resource_id`,`slug`, `title`, `description`, `viewed_count`, `published_at`, `photo1_url`, `photo1_width`,`photo1_height`,`tags`,`duration`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
                PreparedStatement pStmt = learnweb.getConnection().prepareStatement(insertStmt);
                pStmt.setInt(1, Integer.parseInt(tedId));
                pStmt.setInt(2, tedResource.getId());
                pStmt.setString(3, slug);
                pStmt.setString(4, title);
                pStmt.setString(5, description);
                pStmt.setString(6, totalViews);
                pStmt.setTimestamp(7, new Timestamp(publishedAt.getTime()));
                pStmt.setString(8, maxImageUrl);
                pStmt.setInt(9, imageWidth);
                pStmt.setInt(10, imageHeight);
                pStmt.setString(11, keywords.toString());
                pStmt.setInt(12, duration);
                int val = pStmt.executeUpdate();
                if (val != 1) {
                    log.error("Inserting ted video resource was not successful: " + tedResource.getId());
                }
            } catch (SQLException | IOException e) {
                log.error("Error while processing ted video resource for ted_video: " + tedResource.getId(), e);
            }
        }

        //if video already added, check if slug has changed and then update basic attributes if so
        if (resourceId > 0) {
            updateResourceData(resourceId, slug, title, description);
        } else {
            return;
        }

        //if the videos are already added, crawl for new transcripts
        //log.info("Extracting transcripts for existing ted video: " + resourceId);
        Elements transcriptLinkElements = doc.select("link[rel=alternate]");
        if (transcriptLinkElements != null && !transcriptLinkElements.isEmpty()) {
            for (Element transcriptLinkElement : transcriptLinkElements) {
                String hrefLang = transcriptLinkElement.attr("hreflang");
                if (hrefLang != null && !hrefLang.isEmpty() && !hrefLang.equals("x-default")) {
                    languageSet.add(hrefLang);
                }
            }

            if (!languageSet.equals(languageListFromDatabase)) {
                languageSet.removeAll(languageListFromDatabase);
                for (String langCode : languageSet) {
                    log.info("inserting transcript for resource id: " + resourceId + "; language code: " + langCode);
                    extractTranscript(tedId, resourceId, langCode);
                }
            }
        }

    }

    @Override
    public void run() {
        initialize();
        start();
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        Learnweb.createInstance();
        TedCrawlerSimple tedCrawler = new TedCrawlerSimple();
        tedCrawler.run();
        System.exit(0);
    }
}

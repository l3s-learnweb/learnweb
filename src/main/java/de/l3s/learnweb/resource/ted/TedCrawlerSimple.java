package de.l3s.learnweb.resource.ted;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

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

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.Misc;
import de.l3s.util.UrlHelper;

public class TedCrawlerSimple implements Runnable {
    private static final Logger log = LogManager.getLogger(TedCrawlerSimple.class);

    private Group tedGroup;
    private User admin;

    @Inject
    private UserDao userDao;

    @Inject
    private GroupDao groupDao;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private TedTranscriptDao tedTranscriptDao;

    public void initialize() {
        tedGroup = groupDao.findByIdOrElseThrow(862);
        admin = userDao.findByIdOrElseThrow(7727);
    }

    /**
     * Extract the transcript corresponding to a particular TED talk (TED id) and language.
     */
    public void extractTranscript(int tedId, int resourceId, String language) {
        try {
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
                tedTranscriptDao.saveTranscriptParagraphs(resourceId, language, Math.toIntExact(startTime), paragraphText.toString());
            }
        } catch (IOException e) {
            log.warn("Error while fetching transcript ({}) for ted talk: {}", language, resourceId, e);
        } catch (JsonParseException e) {
            log.error("Error while parsing transcript json for ted talk: {} and language: {}", resourceId, language, e);
        }
    }

    public void start() {
        try {
            String tedTalksURLPrefix = "http://www.ted.com/talks?";
            Document doc = Jsoup.parse(new URL(tedTalksURLPrefix + "page=1"), 10000);

            Element lastBrowsingPageEl = doc.select("a.pagination__item").last();
            String lastPage = lastBrowsingPageEl.text();
            int totalPages = Integer.parseInt(lastPage);
            log.info("Total no. of pages: {}", totalPages);

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
            log.error("Error while fetching ted talks page: {}", tedTalksPageUrl, e);
        } catch (InterruptedException e) {
            log.error("Interrupted execution while visiting ted talks page: {}", tedTalksPageUrl, e);
        }
    }

    public void updateResourceData(int resourceId, String slugFromCrawl, String title, String description) {
        Resource resource = resourceDao.findByIdOrElseThrow(resourceId);
        String slug = resource.getUrl().split("talks/")[1];
        if (!slug.equals(slugFromCrawl)) {
            resource.setTitle(title);
            resource.setDescription(description);
            resource.setUrl("http://www.ted.com/talks/" + slugFromCrawl);
            resource.save();

            int dbReturnVal = tedTranscriptDao.updateTedVideo(title, description, slugFromCrawl, resourceId);
            if (dbReturnVal == 1) {
                log.info("Updated existing ted video: {}", resourceId);
            }
        }
    }

    private Document getPageContent(String url) {
        Response response = null;
        int retries = 3; // number of retries in case the connection fails
        Exception lastException = null;

        while (retries > 0) {
            try {
                retries--;

                response = Jsoup.connect(url).timeout(10000).execute();
                return response.parse(); // Jsoup.parse(new URL(url), 10000);
            } catch (IOException e) {
                if (response != null) {
                    log.warn("Error while fetching ted talks page: {}; response: {}, {}", url, response.statusCode(), response.statusMessage(), e);
                }

                lastException = e;
                Misc.sleep(60000);
            }
        }

        log.error("Can't fetch ted talks page: {}", url, lastException);
        return null;
    }

    /**
     * Extract data about a particular TED talk given the URL.
     * Update the data if talk already exists if not insert the new TED talk to the database.
     */
    public void visit(String url) {
        HashSet<String> languageSet = new HashSet<>();
        HashSet<String> languageListFromDatabase = new HashSet<>();

        String slug = url.split("talks/")[1];

        // check the database to identify if the video has already been crawled or if any new transcripts are added to the video
        Optional<Integer> resourceId = tedTranscriptDao.findResourceIdBySlug(slug);
        resourceId.ifPresent(integer -> languageListFromDatabase.addAll(tedTranscriptDao.findLanguagesByResourceId(integer)));

        Document doc = getPageContent(url);

        if (null == doc) {
            return;
        }

        TedVideo tedVideo = new TedVideo();
        tedVideo.setSlug(slug);

        //Since there is no explicit meta property for ted id, it is extracted like below in order to be able to get transcripts
        Element iosURLEl = doc.select("meta[property=al:ios:url]").first();
        if (iosURLEl != null) {
            String tedId = iosURLEl.attr("content").split("ted://talks/")[1];
            tedId = tedId.replace("?source=facebook", "");
            tedVideo.setTedId(Integer.parseInt(tedId));
        } else {
            return; //Few TED talks have broken links and it redirects it to the homepage
        }

        if (resourceId.isEmpty()) {
            resourceId = tedTranscriptDao.findResourceIdByTedId(tedVideo.getTedId());
        }

        Element titleEl = doc.select("meta[name=title]").first();
        tedVideo.setTitle(titleEl.attr("content"));

        Element descriptionEl = doc.select("meta[name=description]").first();
        tedVideo.setDescription(descriptionEl.attr("content"));

        //if the videos are new, crawl for the basic attributes such as title, speaker, transcripts
        if (resourceId.isEmpty()) {
            Element totalViewsEl = doc.select("meta[itemprop=interactionCount]").first();
            tedVideo.setViewedCount(Integer.parseInt(totalViewsEl.attr("content")));

            Element imgLinkEl = doc.select("meta[property=og:image]").first();
            tedVideo.setPhotoUrl(imgLinkEl.attr("content"));

            Element imageHeightElement = doc.select("meta[property=og:image:height]").first();
            tedVideo.setPhotoHeight(Integer.parseInt(imageHeightElement.attr("content")));

            Element imageWidthElement = doc.select("meta[property=og:image:width]").first();
            tedVideo.setPhotoWidth(Integer.parseInt(imageWidthElement.attr("content")));

            Element durationEl = doc.select("meta[property=og:video:duration]").first();
            int duration = (int) Float.parseFloat(durationEl.attr("content"));
            tedVideo.setDuration(duration);

            Element releaseDateEl = doc.select("meta[property=og:video:release_date]").first();
            tedVideo.setPublishedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(Integer.parseInt(releaseDateEl.attr("content"))), ZoneId.systemDefault()));

            StringJoiner tags = new StringJoiner(",");
            Elements tagEl = doc.select("meta[property=og:video:tag]");
            for (Element tag : tagEl) {
                tags.add(tag.attr("content"));
            }

            if (tags.length() > 0) {
                tedVideo.setTags(tags.toString());
            }

            log.info("TedVideo: {}", tedVideo);

            Resource tedResource = new Resource(Resource.StorageType.WEB, ResourceType.video, ResourceService.ted);
            tedResource.setTitle(tedVideo.getTitle());
            tedResource.setDescription(tedVideo.getDescription());
            tedResource.setUrl("https://www.ted.com/talks/" + tedVideo.getSlug());
            tedResource.setDuration(duration);
            tedResource.setWidth(tedVideo.getPhotoWidth());
            tedResource.setHeight(tedVideo.getPhotoHeight());
            tedResource.setMaxImageUrl(tedVideo.getPhotoUrl());
            tedResource.setCreatedAt(tedVideo.getPublishedAt());
            tedResource.setIdAtService(String.valueOf(tedVideo.getTedId()));
            tedResource.setTranscript("");

            try {
                Learnweb.getInstance().getResourcePreviewMaker().processImage(tedResource, UrlHelper.getInputStream(tedResource.getMaxImageUrl()));
                tedResource.setGroup(tedGroup);
                tedResource.setUser(admin);
                tedResource.save();

                tedVideo.setResourceId(tedResource.getId());

                //save new TED resource ID in order to use it later for saving transcripts
                resourceId = Optional.of(tedResource.getId());
                tedTranscriptDao.saveTedVideo(tedVideo);

                log.error("Inserting ted video resource was not successful: {}", tedResource.getId());
            } catch (IOException e) {
                log.error("Error while processing ted video resource for ted_video: {}", tedResource.getId(), e);
            }
        }

        //if video already added, check if slug has changed and then update basic attributes if so
        if (resourceId.isPresent()) {
            updateResourceData(resourceId.get(), slug, tedVideo.getTitle(), tedVideo.getDescription());
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
                    log.info("inserting transcript for resource id: {}; language code: {}", resourceId, langCode);
                    extractTranscript(tedVideo.getTedId(), resourceId.get(), langCode);
                }
            }
        }

    }

    @Override
    public void run() {
        initialize();
        start();
    }
}

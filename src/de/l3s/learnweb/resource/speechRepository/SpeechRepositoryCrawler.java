package de.l3s.learnweb.resource.speechRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.util.UrlHelper;

public class SpeechRepositoryCrawler implements Runnable {
    private static final Logger log = LogManager.getLogger(SpeechRepositoryCrawler.class);

    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("\\(([a-z]{2,3})\\)");
    private static final Pattern DATE_PATTERN = Pattern.compile("([^\\s]+)$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);
    private static final int TIMEOUT = 60 * 1000;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private SpeechRepositoryDao speechRepositoryDao;

    /**
     * Extract individual speech repository URLs from the menu page.
     */
    private String visitCategoryPage(String categoryPageUrl) {
        try {
            Document doc = Jsoup.connect(categoryPageUrl).timeout(TIMEOUT).get();
            Element content = doc.select("#block-system-main").first();
            Element paginationElement = content.select(".item-list > .pager").first();

            Element nextCategoryPage = paginationElement.select(".pager-next a").first();
            String nextCategoryPageUrl = nextCategoryPage == null ? null : nextCategoryPage.attr("href");

            Element tableElement = content.select(".view-content table").first();
            Elements tableRows = tableElement.select("tbody > tr");

            for (Element tableRow : tableRows) {
                final int pageId = Integer.parseInt(tableRow.select(".views-field-nid").text());
                final String pageUrl = tableRow.select(".views-field-title a").attr("href");

                try {
                    if (!speechRepositoryDao.isExists(pageId)) {
                        visitPage(pageUrl);
                        TimeUnit.SECONDS.sleep(5);
                    }
                } catch (Exception e) {
                    log.error("Error while fetching speech repository page: {}", pageUrl, e);
                }
            }

            return nextCategoryPageUrl;
        } catch (IOException e) {
            log.error("Error while fetching speech repository page: {}", categoryPageUrl, e);
        }

        return null;
    }

    /**
     * Extract data about a particular speech repository given the URL.
     * Update the data if speech already exists, if not - insert the new speech repository to the database.
     */
    private void visitPage(final String pageUrl) throws IOException {
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
        for (Element element : speechDetailsElement.select(".field")) {
            String key = element.select(".field-label").text()
                .replace(":", "").replace("\u00a0", " ").trim();
            String value = element.select(".field-items").text();

            if (key.contains("Duration")) {
                speechEntity.setDuration(value);
            } else if (key.contains("Language")) {
                speechEntity.setLanguage(value);
            } else if (key.contains("Level")) {
                speechEntity.setLevel(value);
            } else if (key.contains("Use")) {
                speechEntity.setUse(value);
            } else if (key.contains("Type")) {
                speechEntity.setType(value);
            } else if (key.contains("Domains")) {
                speechEntity.setDomains(value);
            } else if (key.contains("Terminology")) {
                speechEntity.setTerminology(value);
            }
        }

        // extracting video url
        for (Element element : doc.select("script").not("[src]")) {
            String scriptData = element.data();
            if (scriptData != null && !scriptData.isEmpty() && scriptData.contains("jQuery.extend(Drupal.settings")) {
                scriptData = scriptData.substring(scriptData.indexOf('{'), scriptData.lastIndexOf('}') + 1);

                JsonObject jsonObject = JsonParser.parseString(scriptData).getAsJsonObject();
                JsonObject mediaPlayer = jsonObject.getAsJsonObject("ecspTranscodingPlayers").getAsJsonObject("ecsp-media-player");

                if (mediaPlayer.has("image")) {
                    speechEntity.setImageLink(mediaPlayer.get("image").getAsString());
                }

                if (mediaPlayer.has("entity_id")) {
                    speechEntity.setId(mediaPlayer.get("entity_id").getAsString());
                }

                for (JsonElement source : mediaPlayer.getAsJsonArray("sources")) {
                    JsonObject objectSource = source.getAsJsonObject();
                    if (!"auto".equals(objectSource.get("label").getAsString())) {
                        speechEntity.setVideoLink(objectSource.get("file").getAsString());
                        break;
                    }
                }
            }
        }

        Resource resource = createResource(speechEntity);
        speechEntity.setLearnwebResourceId(resource.getId());

        speechRepositoryDao.save(speechEntity);
    }

    private Resource createResource(final SpeechRepositoryEntity speechEntity) throws IOException {
        if (speechEntity.getLearnwebResourceId() != 0) {
            return resourceDao.findByIdOrElseThrow(speechEntity.getLearnwebResourceId());
        }

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
        if (StringUtils.isNotBlank(speechEntity.getLanguage())) {
            Matcher matcher = LANGUAGE_PATTERN.matcher(speechEntity.getLanguage());
            if (matcher.find()) {
                resource.setLanguage(matcher.group(1));
            } else {
                log.error("Did not expect this lang value {}, speechEntity {}", speechEntity.getLanguage(), speechEntity.getId());
            }
        }

        // parse date, example: Bruxelles, 09/04/2018
        if (StringUtils.isNotBlank(speechEntity.getDate())) {
            String dateStr = speechEntity.getDate();
            Matcher matcher = DATE_PATTERN.matcher(dateStr);
            if (matcher.find()) {
                resource.setCreationDate(LocalDate.parse(matcher.group(1), DATE_FORMATTER).atStartOfDay());
            } else {
                log.error("Did not expect this date value {}, speechEntity {}", speechEntity.getDate(), speechEntity.getId());
            }
        }

        saveResource(resource);
        return resource;
    }

    private void saveResource(final Resource resource) throws IOException {
        resource.setGroupId(1401);
        resource.setUserId(7727);

        // save preview images
        Learnweb.getInstance().getResourcePreviewMaker().processImage(resource, UrlHelper.getInputStream(resource.getMaxImageUrl()));

        resource.save();
    }

    @Override
    public void run() {
        try {
            String nextUrl = "https://webgate.ec.europa.eu/sr/search-speeches?entity%5B0%5D=&language=All&level=All&use=All&domain=All&type=All&title=&combine=&combine_1=&video_reference=&order=nid&sort=desc";

            int pageNumber = 0;
            while (nextUrl != null) {
                log.info("Getting page {}", pageNumber++);
                nextUrl = visitCategoryPage(nextUrl);
            }
        } catch (Exception e) {
            log.error("Error while fetching speech repository page:1", e);
        }
    }
}

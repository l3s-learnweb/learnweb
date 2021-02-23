package de.l3s.learnweb.resource.ted;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import de.l3s.interwebj.client.model.SearchResponse;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.InterwebResultsWrapper;
import de.l3s.learnweb.resource.ted.Transcript.Paragraph;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.UrlHelper;

public class TedManager {
    private static final Logger log = LogManager.getLogger(TedManager.class);

    public enum SummaryType {
        SHORT,
        LONG,
        DETAILED
    }

    @Inject
    private UserDao userDao;

    @Inject
    private GroupDao groupDao;

    @Inject
    private FileDao fileDao;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private TedTranscriptDao tedTranscriptDao;

    //For saving crawled ted videos into lw_resource table
    public void saveTedResource() throws IOException {
        Group tedGroup = groupDao.findById(862);
        User admin = userDao.findById(7727);

        List<TedVideo> tedVideos = tedTranscriptDao.findAllTedVideos();

        for (TedVideo tedVideo : tedVideos) {
            Resource resource = createResource(tedVideo);
            int tedId = Integer.parseInt(resource.getIdAtService());

            resource.setMachineDescription(concatenateTranscripts(tedVideo.getResourceId()));
            resource.setUser(admin);

            if (tedVideo.getResourceId() == 0 || resource.getUserId() == 0) { // not yet stored in Learnweb
                Learnweb.getInstance().getResourcePreviewMaker().processImage(resource, UrlHelper.getInputStream(resource.getMaxImageUrl()));
                resource.setGroup(tedGroup);
                resource.setUser(admin);
                resource.save();

                if (tedVideo.getResourceId() == 0) {
                    tedTranscriptDao.updateResourceIdByTedId(resource.getId(), tedId);
                }
            } else {
                resource.save();
            }

            log.debug("Processed; lw: {} ted: {} title:{}", tedVideo.getResourceId(), tedId, resource.getTitle());
        }
    }

    private String concatenateTranscripts(int learnwebResourceId) {
        StringBuilder sb = new StringBuilder();

        for (Transcript transcript : tedTranscriptDao.findTranscriptsByResourceId(learnwebResourceId)) {
            if (StringUtils.equalsAny(transcript.getLanguageCode(), "en", "fr", "de", "es", "it")) {
                for (Paragraph paragraph : transcript.getParagraphs()) {
                    sb.append(paragraph.getText());
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    private Resource createResource(TedVideo tedVideo) {
        Resource resource = new Resource();

        if (tedVideo.getResourceId() != 0) { // the video is already stored and will be updated
            resource = resourceDao.findById(tedVideo.getResourceId());
        }

        resource.setTitle(tedVideo.getTitle());
        resource.setDescription(tedVideo.getDescription());
        resource.setUrl("https://www.ted.com/talks/" + tedVideo.getSlug());
        resource.setSource(ResourceService.ted);
        resource.setLocation("TED");
        resource.setType(ResourceType.video);
        resource.setDuration(tedVideo.getDuration());
        resource.setMaxImageUrl(tedVideo.getPhotoUrl());
        resource.setIdAtService(Integer.toString(tedVideo.getTedId()));
        resource.setCreationDate(tedVideo.getPublishedAt());
        resource.setTranscript("");
        return resource;
    }

    public void fetchTedX() throws IOException {
        //Group tedxGroup = learnweb.getGroupManager().getGroupById(921);
        //Group tedxTrentoGroup = learnweb.getGroupManager().getGroupById(922);
        Group tedEdGroup = groupDao.findById(1291);
        User admin = userDao.findById(7727);

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
            SearchResponse interwebResponse = Learnweb.getInstance().getInterweb().search("user::TEDEducation", params);
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

                Optional<Resource> learnwebResource = resourceDao.findByUrl(resource.getUrl());

                if (learnwebResource.isPresent()) { // it is already stored
                    if (learnwebResource.get().getIdAtService() == null || learnwebResource.get().getIdAtService().isEmpty()) {
                        learnwebResource.get().setIdAtService(resource.getIdAtService());

                        if (learnwebResource.get().getGroupId() == tedEdGroup.getId()) {
                            log.error("resource is already part of the group");
                        } else {
                            learnwebResource.get().setGroup(tedEdGroup);
                        }
                        learnwebResource.get().save();
                    }

                    log.debug("Already stored: {}", resource);

                } else {
                    Learnweb.getInstance().getResourcePreviewMaker().processImage(resource, UrlHelper.getInputStream(resource.getMaxImageUrl().replace("hqdefault", "mqdefault")));
                    resource.setGroup(tedEdGroup);
                    resource.setUser(admin);
                    resource.save();

                    log.debug("new video added");
                }

                //check if new transcripts are available for "resource" variable
                //fetchTedXTranscripts(resource.getIdAtService(), resource.getId());
            }

            page++;
            log.debug("page: {} total results: {}", page, resources.size());
            // break;
        } while (!resources.isEmpty() && page < 25);
    }

    public void insertTedXTranscripts(int resourceId, String resourceIdAtService, String langCode, String langName) {
        List<Paragraph> paragraphs = tedTranscriptDao.findTranscriptsParagraphs(resourceId, langCode);
        if (!paragraphs.isEmpty()) {
            log.info("Transcript :{} for Ted video: {} already inserted.", langCode, resourceId);
            return; // transcript is already part of the database
        }

        String respXml = getTedxData("https://www.youtube.com/api/timedtext?lang=" + langCode + "&v=" + resourceIdAtService);
        if (respXml == null) {
            log.info("Transcript :{} for resource ID: {} does not exist.", langCode, resourceIdAtService);
            return; // no transcript available for this language code
        }

        tedTranscriptDao.saveTranscriptLangMapping(langCode, langName);

        Document doc = Jsoup.parse(respXml, "", Parser.xmlParser());
        Elements texts = doc.select("transcript text");

        if (!texts.isEmpty()) {
            for (Element text : texts) {
                double start = Double.parseDouble(text.attr("start"));
                // double duration = Double.parseDouble(text.attr("dur"));
                String paragraph = text.text().replace("\n", " ");

                tedTranscriptDao.saveTranscriptParagraphs(resourceId, langCode, (int) (start * 1000), paragraph);
            }
        }
    }

    public void fetchTedXTranscripts(String resourceIdAtService, int resourceId) {
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

    // Remove duplicate TED Resources from group 862 starting from the resourceId
    public void removeDuplicateTEDResources(int startFromResourceId) {
        List<Resource> resources = Learnweb.dao().getJdbi().withHandle(handle -> handle
            .select("SELECT * FROM lw_resource WHERE group_id = 862 AND owner_user_id = 7727 AND deleted = 0 AND resource_id > ?", startFromResourceId)
            .map(new ResourceDao.ResourceMapper()).list());

        for (Resource resource : resources) {
            boolean existsInTedVideo = false;
            if (tedTranscriptDao.findTedVideoByResourceId(resource.getId()).isPresent()) {
                existsInTedVideo = true;
            }

            if (!existsInTedVideo) {
                // log.debug(resource);
                resourceDao.deleteSoft(resource.getId());

                int deleted = tedTranscriptDao.deleteTranscriptParagraphs(resource.getId());
                log.info("Deleted({}) transcripts for duplicate TED video: {}", deleted, resource.getId());
            }
        }
    }

    //Link existing resources to TED resources in the original TED group
    public void linkResourcesToTEDResources() {
        List<Resource> resources = Learnweb.dao().getJdbi().withHandle(handle -> handle
            .select("SELECT * FROM lw_resource WHERE url LIKE '%ted.com/talks%' AND source != 'TED'")
            .map(new ResourceDao.ResourceMapper()).list());

        for (Resource resource : resources) {
            Optional<Integer> tedVideoResourceId = tedTranscriptDao.findResourceIdBySlug(resource.getUrl());
            log.info(tedVideoResourceId);

            if (tedVideoResourceId.isPresent()) {
                Resource r2 = resourceDao.findById(tedVideoResourceId.get());

                resource.setSource(ResourceService.ted);
                resource.setMaxImageUrl(r2.getMaxImageUrl());
                for (File file : resource.getFiles().values()) {
                    fileDao.deleteSoft(file);
                }
                resource.setThumbnail0(r2.getThumbnail0());
                resource.setThumbnail1(r2.getThumbnail1());
                resource.setThumbnail2(r2.getThumbnail2());
                resource.setThumbnail3(r2.getThumbnail3());
                resource.setThumbnail4(r2.getThumbnail4());
                resource.setDuration(r2.getDuration());
                resource.setIdAtService(r2.getIdAtService());
                resource.setType(r2.getType());
                resource.setFormat(r2.getFormat());
                resource.save();
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
}

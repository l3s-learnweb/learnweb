package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.interweb.client.InterwebException;
import de.l3s.interweb.core.describe.DescribeResults;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.File.FileType;
import de.l3s.learnweb.resource.Resource.OnlineStatus;
import de.l3s.learnweb.resource.office.FileUtility;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;
import de.l3s.util.StringHelper;
import de.l3s.util.UrlHelper;

/**
 * Helper for extract metadata from a Resource.
 *
 * @author Oleh Astappiev
 */
public class ResourceMetadataExtractor {
    private static final Logger log = LogManager.getLogger(ResourceMetadataExtractor.class);

    private static final int DESCRIPTION_LIMIT = 1400;

    private final FileInspector fileInspector;

    public ResourceMetadataExtractor(final SolrClient solrClient) {
        this.fileInspector = new FileInspector(solrClient);
    }

    public void processResource(Resource resource) {
        if (resource.isWebResource()) {
            this.processWebResource(resource);
        } else {
            this.processFileResource(resource);
        }
    }

    public void processWebResource(Resource resource) {
        if (StringUtils.isEmpty(resource.getUrl())) {
            throw new IllegalArgumentException("Given resource doesn't have a url!");
        }

        try {
            DescribeResults describeResults = Learnweb.getInstance().getInterweb().describe(resource.getUrl());

            if (describeResults.getEntity() != null) {
                resource.setOnlineStatus(OnlineStatus.ONLINE);
                resource.setType(ResourceType.fromContentType(describeResults.getEntity().getType()));
                resource.setService(ResourceService.parse(describeResults.getService()));
                resource.setIdAtService(describeResults.getEntity().getId());

                if (StringUtils.isEmpty(resource.getTitle()) && describeResults.getEntity().getTitle() != null) {
                    resource.setTitle(describeResults.getEntity().getTitle());
                }
                if (StringUtils.isEmpty(resource.getDescription()) && describeResults.getEntity().getDescription() != null) {
                    resource.setDescription(StringHelper.shortnString(describeResults.getEntity().getDescription(), DESCRIPTION_LIMIT));
                }
                if (StringUtils.isEmpty(resource.getAuthor()) && describeResults.getEntity().getAuthor() != null) {
                    resource.setAuthor(describeResults.getEntity().getAuthor());
                }
                if (resource.getDuration() == 0 && describeResults.getEntity().getDuration() != null) {
                    resource.setDuration(Math.toIntExact(describeResults.getEntity().getDuration()));
                }
                if (StringUtils.isEmpty(resource.getMaxImageUrl()) && describeResults.getEntity().getLargestThumbnail() != null) {
                    resource.setMaxImageUrl(describeResults.getEntity().getLargestThumbnail().getUrl());
                }
            }

            if (resource.getUrl().startsWith("https://webgate.ec.europa.eu/")) {
                resource.setType(ResourceType.video);
                resource.setService(ResourceService.speechrepository);
                processSpeechRepositoryResource(resource);
                return;
            }

            resource.setType(ResourceType.website);
            FileInfo fileInfo = getFileInfo(resource.getUrl());

            processFileResource(resource, fileInfo);

            if (resource.getType() == ResourceType.website) {
                /*
                try
                {
                    //ArticleExtractor.INSTANCE.getText(arg0)
                    String htmlContent = hh.process(new URL(resource.getUrl()), extractor);
                    Document jsoupDoc = Jsoup.parse(htmlContent);
                    jsoupDoc.outputSettings(new OutputSettings().prettyPrint(false));
                    jsoupDoc.select("br").after("\\n");
                    jsoupDoc.select("p").before("\\n");
                    String str = jsoupDoc.html().replaceAll("\\\\n", "\n");

                    log.debug("machine:" + resource.getMachineDescription());
                    log.debug("boiler :" + str);
                    resource.setTranscript(Jsoup.clean(str, "", Safelist.none(), new OutputSettings().prettyPrint(false)));

                }
                catch(IOException | BoilerpipeProcessingException | SAXException e)
                {
                    Level logLevel = e.getMessage().contains("HTTP response code: 40") ? Level.WARN : Level.ERROR;
                    log.log(logLevel, "Can't extract content body (id: " + resource.getId() + ", url: " + resource.getUrl() + ") from " + resource.getSource() + " source.", e);
                }
                */
                resource.setTranscript(resource.getMachineDescription());

            }
        } catch (JsonParseException | IOException | InterwebException e) {
            resource.setOnlineStatus(OnlineStatus.UNKNOWN); // most probably offline
            log.error("Can't get more details about resource (id: {}, url: {}) from {} source.", resource.getId(), resource.getUrl(), resource.getService(), e);
        }
    }

    private void processSpeechRepositoryResource(Resource resource) throws IOException {
        Document document = Jsoup.connect(resource.getUrl()).get();
        Element content = document.select("#content > .content-inner").first();

        String title = content.select("#content-header h1").text();
        Element speechElement = content.select("#content-area .node-speech").first();
        String rights = speechElement.select(".field-name-field-rights").text();
        String date = speechElement.select(".field-name-field-date").text();
        String body = speechElement.select(".field-name-body .field-items").text();
        String notes = speechElement.select(".field-name-field-notes .field-items").text();

        if (StringUtils.isEmpty(resource.getTitle())) {
            resource.setTitle(title);
        }
        if (StringUtils.isEmpty(resource.getAuthor())) {
            resource.setAuthor(rights);
        }

        StringBuilder description = new StringBuilder();
        description.append(rights).append('\n');
        description.append(date).append('\n');
        description.append("Description: ").append(body).append('\n');
        if (!StringUtils.isEmpty(notes)) {
            description.append("Notes: ").append(notes).append('\n');
        }

        description.append("Speech details:").append('\n');
        Element speechDetailsElement = speechElement.select("#node-speech-full-group-speech-details").first();
        for (Element element : speechDetailsElement.select(".field")) {
            String key = element.select(".field-label").text().replace(":", "").replace("\u00a0", " ").trim();
            String value = element.select(".field-items").text();
            if ("Duration".equals(key)) {
                String[] tokens = value.split(":");
                int duration = 0;
                int multiply = 0;
                for (int i = tokens.length - 1; i >= 0; --i) {
                    duration += Integer.parseInt(tokens[i]) * (int) Math.pow(60, multiply++);
                }
                resource.setDuration(duration);
            } else if (!"Speech number".equals(key)) {
                description.append('\t').append(key).append(": ").append(value).append('\n');
            }
        }

        if (StringUtils.isEmpty(resource.getDescription())) {
            resource.setDescription(description.toString());
        }

        // extracting video url
        for (Element element : document.select("script").not("[src]")) {
            String scriptData = element.data();
            if (scriptData.contains("jQuery.extend(Drupal.settings")) {
                scriptData = scriptData.substring(scriptData.indexOf('{'), scriptData.lastIndexOf('}') + 1);

                JsonObject jsonObject = JsonParser.parseString(scriptData).getAsJsonObject();
                JsonObject mediaPlayer = jsonObject.getAsJsonObject("ecspTranscodingPlayers").getAsJsonObject("ecsp-media-player");

                if (mediaPlayer.has("image")) {
                    resource.setMaxImageUrl(mediaPlayer.get("image").getAsString());
                }

                // TODO @astappiev: remove entity_id from description. Line below can be replaced for extracting it from Speech details
                if (mediaPlayer.has("entity_id")) {
                    resource.setIdAtService(mediaPlayer.get("entity_id").getAsString());
                }

                JsonArray sourcesJsonArray = mediaPlayer.getAsJsonArray("sources");
                for (int i = 0, len = sourcesJsonArray.size(); i < len; ++i) {
                    JsonObject objectSource = sourcesJsonArray.get(i).getAsJsonObject();
                    if (!"auto".equals(objectSource.get("label").getAsString())) {
                        resource.setEmbeddedUrl(objectSource.get("file").getAsString());
                        break;
                    }
                }
            }
        }
    }

    public void processFileResource(Resource resource) {
        File mainFile = resource.getFile(FileType.MAIN);
        FileInfo fileInfo = this.getFileInfo(mainFile.getInputStream(), mainFile.getName());
        processFileResource(resource, fileInfo);
    }

    public void processFileResource(Resource resource, FileInfo fileInfo) {
        resource.setFormat(fileInfo.getMimeType());
        resource.setTypeFromFormat(fileInfo.getMimeType());

        if (StringUtils.isNotEmpty(fileInfo.getTitle()) && StringUtils.isEmpty(resource.getTitle()) && !fileInfo.getTitle().equalsIgnoreCase("unknown")) {
            resource.setTitle(fileInfo.getTitle());
        }

        if (StringUtils.isNotEmpty(fileInfo.getAuthor()) && StringUtils.isEmpty(resource.getAuthor())) {
            resource.setAuthor(fileInfo.getAuthor());
        }

        if (StringUtils.isNotEmpty(fileInfo.getDescription()) && StringUtils.isEmpty(resource.getDescription())) {
            resource.setDescription(StringHelper.shortnString(fileInfo.getDescription(), DESCRIPTION_LIMIT));
        }

        if (StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getMachineDescription())) {
            resource.setMachineDescription(fileInfo.getTextContent());
        }

        if (StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getDescription())) {
            resource.setDescription(StringHelper.shortnString(fileInfo.getTextContent(), DESCRIPTION_LIMIT));
        }
        /*
        if(StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getTranscript()) && resource.getType() == ResourceType.website)
            resource.setTranscript(fileInfo.getTextContent());
            */
    }

    /**
     * DANGER! This method will close inputStream :/
     * ^ Add + if you get an error because of this.
     */
    public FileInfo getFileInfo(InputStream inputStream, String fileName) {
        return fileInspector.inspect(inputStream, fileName);
    }

    public FileInfo getFileInfo(String url) throws IOException {
        String fileName = FileUtility.getFileName(url);
        InputStream inputStream = UrlHelper.getInputStream(url);
        return fileInspector.inspect(inputStream, fileName);
    }
}

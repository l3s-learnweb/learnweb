package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource.OnlineStatus;
import de.l3s.learnweb.resource.office.FileUtility;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.util.StringHelper;
import de.l3s.util.UrlHelper;

/**
 * Helper for extract metadata from a Resource
 *
 * @author Oleh Astappiev
 */
public class ResourceMetadataExtractor
{
    private static final Logger log = LogManager.getLogger(ResourceMetadataExtractor.class);

    private static final String YOUTUBE_PATTERN = "https?://(?:[0-9A-Z-]+\\.)?(?:youtu\\.be/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|</a>))[?=&+%\\w]*";
    private static final String VIMEO_PATTERN = "https?://(?:www\\.)?(?:player\\.)?vimeo\\.com/(?:[a-z]*/)*([0-9]{6,11})[?]?.*";
    private static final String FLICKR_PATTERN = "https?://(?:www\\.)?flickr\\.com/(?:photos/[^/]+/(\\d+))";
    private static final String FLICKR_SHORT_PATTERN = "https?://(?:www\\.)?(?:flic\\.kr/p/|flickr\\.com/photo\\.gne\\?short=)(\\w+)";
    private static final String IPERNITY_PATTERN = "https?://(?:www\\.)?ipernity\\.com/(?:doc/[^/]+/(\\d+))";

    private static final String YOUTUBE_API_REQUEST = "https://www.googleapis.com/youtube/v3/videos?key=***REMOVED***&part=snippet&id=";
    private static final String VIMEO_API_REQUEST = "http://vimeo.com/api/v2/video/";
    private static final String FLICKR_API_REQUEST = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&api_key=***REMOVED***&format=json&nojsoncallback=1&photo_id=";
    private static final String IPERNITY_API_REQUEST = "http://api.ipernity.com/api/doc.get/json?api_key=***REMOVED***&extra=tags&doc_id=";

    private static final String base58alphabetString = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final int DESCRIPTION_LIMIT = 1400;

    private final FileInspector fileInspector;

    public ResourceMetadataExtractor(Learnweb learnweb)
    {
        this.fileInspector = new FileInspector(learnweb);
    }

    public void processResource(Resource resource) throws IOException, SQLException
    {
        if(resource.getStorageType() == Resource.LEARNWEB_RESOURCE)
            this.processFileResource(resource);
        else if(resource.getStorageType() == Resource.WEB_RESOURCE)
            this.processWebResource(resource);
        else
            log.error("Unknown resource's storage type: " + resource.getStorageType());
    }

    public void processFileResource(Resource resource) throws IOException, SQLException
    {
        File mainFile = resource.getFile(TYPE.FILE_MAIN);
        FileInfo fileInfo = this.getFileInfo(mainFile.getInputStream(), mainFile.getName());
        processFileResource(resource, fileInfo);
    }

    public void processWebResource(Resource resource)
    {
        if(StringUtils.isEmpty(resource.getUrl()))
            throw new IllegalArgumentException("Given resource doesn't have a url!");

        try
        {
            resource.setOnlineStatus(OnlineStatus.ONLINE);

            Pattern compYouTubePattern = Pattern.compile(YOUTUBE_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher youTubeMatcher = compYouTubePattern.matcher(resource.getUrl());
            if(youTubeMatcher.find())
            {
                resource.setType(ResourceType.video);
                resource.setSource(ResourceService.youtube);
                resource.setIdAtService(youTubeMatcher.group(1));
                processYoutubeResource(resource);
                return;
            }

            Pattern compVimeoPattern = Pattern.compile(VIMEO_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher vimeoMatcher = compVimeoPattern.matcher(resource.getUrl());
            if(vimeoMatcher.find())
            {
                resource.setType(ResourceType.video);
                resource.setSource(ResourceService.vimeo);
                resource.setIdAtService(vimeoMatcher.group(1));
                processVimeoResource(resource);
                return;
            }

            Pattern compFlickrPattern = Pattern.compile(FLICKR_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher flickrMatcher = compFlickrPattern.matcher(resource.getUrl());
            if(flickrMatcher.find())
            {
                resource.setType(ResourceType.image);
                resource.setSource(ResourceService.flickr);
                resource.setIdAtService(flickrMatcher.group(1));
                processFlickrResource(resource);
                return;
            }

            Pattern compFlickrShortPattern = Pattern.compile(FLICKR_SHORT_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher flickrShortMatcher = compFlickrShortPattern.matcher(resource.getUrl());
            if(flickrShortMatcher.find())
            {
                resource.setType(ResourceType.image);
                resource.setSource(ResourceService.flickr);
                resource.setIdAtService(base58_decode(flickrShortMatcher.group(1)));
                processFlickrResource(resource);
                return;
            }

            Pattern compIpernityPattern = Pattern.compile(IPERNITY_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher ipernityMatcher = compIpernityPattern.matcher(resource.getUrl());
            if(ipernityMatcher.find())
            {
                resource.setType(ResourceType.image);
                resource.setSource(ResourceService.ipernity);
                resource.setIdAtService(ipernityMatcher.group(1));
                processIpernityResource(resource);
                return;
            }

            if(resource.getUrl().startsWith("https://webgate.ec.europa.eu/"))
            {
                resource.setType(ResourceType.video);
                resource.setSource(ResourceService.speechrepository);
                processSpeechRepositoryResource(resource);
                return;
            }

            resource.setType(ResourceType.website);
            FileInfo fileInfo = getFileInfo(resource.getUrl());

            processFileResource(resource, fileInfo);

            if(resource.getType() == ResourceType.website)
            {
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
                    resource.setTranscript(Jsoup.clean(str, "", Whitelist.none(), new OutputSettings().prettyPrint(false)));
                
                }
                catch(IOException | BoilerpipeProcessingException | SAXException e)
                {
                    Level logLevel = e.getMessage().contains("HTTP response code: 40") ? Level.WARN : Level.ERROR;
                    log.log(logLevel, "Can't extract content body (id: " + resource.getId() + ", url: " + resource.getUrl() + ") from " + resource.getSource() + " source.", e);
                }
                */
                resource.setTranscript(resource.getMachineDescription());

            }
        }
        catch(JSONException | IOException e)
        {
            resource.setOnlineStatus(OnlineStatus.UNKNOWN); // most probably offline
            log.error("Can't get more details about resource (id: " + resource.getId() + ", url: " + resource.getUrl() + ") from " + resource.getSource() + " source.", e);
        }
    }

    private void processSpeechRepositoryResource(Resource resource) throws IOException, JSONException
    {
        Document document = Jsoup.connect(resource.getUrl()).get();
        Element content = document.select("#content > .content-inner").first();

        String title = content.select("#content-header h1").text();
        Element speechElement = content.select("#content-area .node-speech").first();
        String rights = speechElement.select(".field-name-field-rights").text();
        String date = speechElement.select(".field-name-field-date").text();
        String body = speechElement.select(".field-name-body .field-items").text();
        String notes = speechElement.select(".field-name-field-notes .field-items").text();

        if(StringUtils.isEmpty(resource.getTitle()))
            resource.setTitle(title);
        if(StringUtils.isEmpty(resource.getAuthor()))
            resource.setAuthor(rights);

        StringBuilder description = new StringBuilder();
        description.append(rights).append('\n');
        description.append(date).append('\n');
        description.append("Description: ").append(body).append('\n');
        if(!StringUtils.isEmpty(notes))
            description.append("Notes: ").append(notes).append('\n');

        description.append("Speech details:").append('\n');
        Element speechDetailsElement = speechElement.select("#node-speech-full-group-speech-details").first();
        for(Element element : speechDetailsElement.select(".field"))
        {
            String key = element.select(".field-label").text()
                    .replace(":", "").replace("\u00a0", " ").trim();
            String value = element.select(".field-items").text();
            if(key.equals("Duration"))
            {
                String[] tokens = value.split(":");
                int duration = 0, multiply = 0;
                for(int i = tokens.length - 1; i >= 0; --i)
                {
                    duration += Integer.parseInt(tokens[i]) * Math.pow(60, multiply++);
                }
                resource.setDuration(duration);
            }
            else if(key.equals("Speech number"))
            {
                // ignore
            }
            else
            {
                description.append('\t').append(key).append(": ").append(value).append('\n');
            }
        }

        if(StringUtils.isEmpty(resource.getDescription()))
            resource.setDescription(description.toString());

        // extracting video url
        for(Element element : document.select("script").not("[src]"))
        {
            String scriptData = element.data();
            if(scriptData != null && !scriptData.isEmpty() && scriptData.contains("jQuery.extend(Drupal.settings"))
            {
                scriptData = scriptData.substring(scriptData.indexOf('{'), scriptData.lastIndexOf('}') + 1);

                JSONObject jsonObject = new JSONObject(scriptData);
                JSONObject mediaPlayer = jsonObject.getJSONObject("ecspTranscodingPlayers").getJSONObject("ecsp-media-player");

                if(mediaPlayer.has("image"))
                {
                    resource.setMaxImageUrl(mediaPlayer.getString("image"));
                }

                // TODO Tetiana: remove entity_id from description. Line below can be replaced for extracting it from Speech details
                if(mediaPlayer.has("entity_id"))
                {
                    resource.setIdAtService(mediaPlayer.getString("entity_id"));
                }

                JSONArray sourcesJsonArray = mediaPlayer.getJSONArray("sources");
                for(int i = 0, l = sourcesJsonArray.length(); i < l; ++i)
                {
                    JSONObject objectSource = sourcesJsonArray.getJSONObject(i);
                    if(!objectSource.getString("label").equals("auto"))
                    {
                        resource.setFileUrl(objectSource.getString("file"));
                        break;
                    }
                }
            }
        }
    }

    private void processYoutubeResource(Resource resource) throws IOException, JSONException
    {
        JSONObject json = readJsonObjectFromUrl(YOUTUBE_API_REQUEST + resource.getIdAtService());
        JSONArray items = json.getJSONArray("items");
        if(!items.isEmpty())
        {
            JSONObject snippet = items.getJSONObject(0).getJSONObject("snippet");
            if(StringUtils.isEmpty(resource.getTitle()))
                resource.setTitle(snippet.getString("title"));
            if(StringUtils.isEmpty(resource.getDescription()))
                resource.setDescription(StringHelper.shortnString(snippet.getString("description"), DESCRIPTION_LIMIT));
            if(StringUtils.isEmpty(resource.getAuthor()))
                resource.setAuthor(snippet.getString("channelTitle"));

            // TODO Oleh: save tags for resource
            /*JSONArray tags = (JSONArray) snippet.get("tags");
            if(tags != null && tags.length() > 0)
            {
            for(int i = 0, len = tags.length(); i < len; i++)
            {
                resource.addTag(tags.get(i).toString(), null);
            }
            }*/

            JSONObject thumbnails = snippet.getJSONObject("thumbnails");
            Optional<String> size = Arrays.stream(new String[] { "maxres", "standard", "high", "medium", "default" }).filter(thumbnails::has).findFirst();
            size.ifPresent(s -> resource.setMaxImageUrl(thumbnails.getJSONObject(s).getString("url")));
        }
    }

    private void processVimeoResource(Resource resource) throws IOException, JSONException
    {
        JSONObject json = readJsonArrayFromUrl(VIMEO_API_REQUEST + resource.getIdAtService() + ".json").getJSONObject(0);
        if(json != null)
        {
            if(resource.getTitle() == null || resource.getTitle().isEmpty())
                resource.setTitle(json.getString("title"));
            if(StringUtils.isEmpty(resource.getDescription()))
                resource.setDescription(StringHelper.shortnString(json.getString("description"), DESCRIPTION_LIMIT));
            if(StringUtils.isEmpty(resource.getAuthor()))
                resource.setAuthor(json.getString("user_name"));
            if(resource.getDuration() == 0)
                resource.setDuration(json.getInt("duration"));

            // TODO Oleh: save tags for resource
            /*String tags = object.get("tags").toString();
            for(String tag : Arrays.asList(tags.split(", ")))
            {
            resource.addTag(tag, null);
            }*/

            Optional<String> size = Arrays.stream(new String[] { "thumbnail_large", "thumbnail_medium", "thumbnail_small" }).filter(json::has).findFirst();
            size.ifPresent(s -> resource.setMaxImageUrl(json.getString(s)));
        }
    }

    private void processFlickrResource(Resource resource) throws IOException, JSONException
    {
        JSONObject json = readJsonObjectFromUrl(FLICKR_API_REQUEST + resource.getIdAtService()).getJSONObject("photo");
        if(json != null)
        {
            if(StringUtils.isEmpty(resource.getTitle()))
                resource.setTitle(json.getJSONObject("title").getString("_content"));
            if(StringUtils.isEmpty(resource.getDescription()))
                resource.setDescription(StringHelper.shortnString(json.getJSONObject("description").getString("_content"), DESCRIPTION_LIMIT));
            if(StringUtils.isEmpty(resource.getAuthor()))
            {
                JSONObject owner = json.getJSONObject("owner");
                String realname = owner.getString("realname");
                resource.setAuthor(StringUtils.isNotEmpty(realname) ? realname : owner.getString("username"));
            }

            // TODO Oleh: save tags for resource
            /*JSONArray tags = (JSONArray) ((JSONObject) photo.get("tags")).get("tag");
            if(tags != null && tags.length() > 0)
            {
            for(int i = 0, len = tags.length(); i < len; i++)
            {
                resource.addTag(((JSONObject) tags.get(i)).get("raw").toString(), null);
            }
            }*/

            String thumbnailUrl = "https://farm" + json.getString("farm") + ".staticflickr.com/" + json.getString("server") + "/" + json.getString("id") + "_" + json.getString("secret") + ".jpg";
            resource.setMaxImageUrl(thumbnailUrl);
        }
    }

    private void processIpernityResource(Resource resource) throws IOException, JSONException
    {
        JSONObject json = readJsonObjectFromUrl(IPERNITY_API_REQUEST + resource.getIdAtService()).getJSONObject("doc");
        if(json != null)
        {
            if(StringUtils.isEmpty(resource.getTitle()))
                resource.setTitle(json.getString("title"));
            if(StringUtils.isEmpty(resource.getDescription()))
                resource.setDescription(StringHelper.shortnString(json.getString("description"), DESCRIPTION_LIMIT));
            if(StringUtils.isEmpty(resource.getAuthor()))
                resource.setAuthor(json.getJSONObject("owner").getString("username"));

            // TODO Oleh: save tags for resource
            /*
            JSONArray tags = (JSONArray) ((JSONObject) json.get("tags")).get("tag");
            if(tags != null && tags.length() > 0)
            {
            for(int i = 0, len = tags.length(); i < len; i++)
            {
                resource.addTag(((JSONObject) tags.get(i)).get("tag").toString(), null);
            }
            }*/

            JSONArray thumbnails = json.getJSONObject("thumbs").getJSONArray("thumb");
            if(thumbnails != null && !thumbnails.isEmpty())
            {
                String thumbnailUrl = null;
                for(int i = 0, len = thumbnails.length(), weight = 0; i < len; i++)
                {
                    JSONObject th = thumbnails.getJSONObject(i);
                    int newWeight = Integer.parseInt(th.getString("w"));

                    if(newWeight > weight)
                    {
                        weight = newWeight;
                        thumbnailUrl = th.getString("url");
                    }
                }

                if(thumbnailUrl != null)
                    resource.setMaxImageUrl(thumbnailUrl);
            }
        }
    }

    public void processFileResource(Resource resource, FileInfo fileInfo)
    {
        resource.setFormat(fileInfo.getMimeType());
        resource.setTypeFromFormat(resource.getFormat());
        resource.setFileName(fileInfo.getFileName());
        resource.setFileUrl(resource.getUrl());

        if(StringUtils.isNotEmpty(fileInfo.getTitle()) && StringUtils.isEmpty(resource.getTitle()) && !fileInfo.getTitle().equalsIgnoreCase("unknown"))
            resource.setTitle(fileInfo.getTitle());

        if(StringUtils.isNotEmpty(fileInfo.getAuthor()) && StringUtils.isEmpty(resource.getAuthor()))
            resource.setAuthor(fileInfo.getAuthor());

        if(StringUtils.isNotEmpty(fileInfo.getDescription()) && StringUtils.isEmpty(resource.getDescription()))
            resource.setDescription(StringHelper.shortnString(fileInfo.getDescription(), DESCRIPTION_LIMIT));

        if(StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getMachineDescription()))
            resource.setMachineDescription(fileInfo.getTextContent());

        if(StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getDescription()))
            resource.setDescription(StringHelper.shortnString(fileInfo.getTextContent(), DESCRIPTION_LIMIT));
        /*
        if(StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getTranscript()) && resource.getType() == ResourceType.website)
            resource.setTranscript(fileInfo.getTextContent());
            */
    }

    private String base58_decode(String snipCode)
    {
        long result = 0;
        long multi = 1;
        while(!snipCode.isEmpty())
        {
            String digit = snipCode.substring(snipCode.length() - 1);
            result += multi * base58alphabetString.lastIndexOf(digit);
            multi *= base58alphabetString.length();
            snipCode = snipCode.substring(0, snipCode.length() - 1);
        }
        return Long.toString(result);
    }

    /**
     * DANGER! This method will close inputStream :/
     * ^ Add + if you get an error because of this.
     */
    public FileInfo getFileInfo(InputStream inputStream, String fileName) throws IOException
    {
        return fileInspector.inspect(inputStream, fileName);
    }

    public FileInfo getFileInfo(String url) throws IOException
    {
        String fileName = FileUtility.getFileName(url);
        InputStream inputStream = FileInspector.openStream(url);
        return fileInspector.inspect(inputStream, fileName);
    }

    private static JSONObject readJsonObjectFromUrl(String url) throws IOException, JSONException
    {
        return new JSONObject(IOUtils.toString(new URL(url), StandardCharsets.UTF_8));
    }

    private static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException
    {
        return new JSONArray(IOUtils.toString(new URL(url), StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException
    {
        String url = "https://flic.kr/p/tcT8oi";
        url = UrlHelper.validateUrl(url);

        Resource resource = new Resource();
        resource.setStorageType(Resource.WEB_RESOURCE);
        resource.setUrl(url);

        ResourceMetadataExtractor rme = new ResourceMetadataExtractor(Learnweb.getInstance());
        rme.processResource(resource);

        log.debug(resource.getType());
        log.debug(resource.getSource());
        log.debug(resource.getIdAtService());
        log.debug(resource.getTitle());
        log.debug(resource.getDescription());
        log.debug(resource.getAuthor());
        log.debug(resource.getMaxImageUrl());
        System.exit(0);
    }
}

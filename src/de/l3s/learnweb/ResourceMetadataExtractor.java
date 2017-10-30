package de.l3s.learnweb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.l3s.office.FileUtility;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.Resource.OnlineStatus;
import de.l3s.learnweb.beans.AddResourceBean;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
import de.l3s.util.StringHelper;

/**
 * Helper for extract metadata from a Resource
 *
 * @author Oleh Astappiev
 */
public class ResourceMetadataExtractor
{
    private final static Logger log = Logger.getLogger(ResourceMetadataExtractor.class);

    private final static String YOUTUBE_PATTERN = "https?://(?:[0-9A-Z-]+\\.)?(?:youtu\\.be/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|</a>))[?=&+%\\w]*";
    private final static String VIMEO_PATTERN = "https?://(?:www\\.)?(?:player\\.)?vimeo\\.com/(?:[a-z]*/)*([0-9]{6,11})[?]?.*";
    private final static String FLICKR_PATTERN = "https?://(?:www\\.)?flickr\\.com/(?:photos/[^/]+/(\\d+))";
    private final static String FLICKR_SHORT_PATTERN = "https?://(?:www\\.)?(?:flic\\.kr/p/|flickr\\.com/photo\\.gne\\?short=)(\\w+)";
    private final static String IPERNITY_PATTERN = "https?://(?:www\\.)?ipernity\\.com/(?:doc/[^/]+/(\\d+))";

    private static final String YOUTUBE_API_REQUEST = "https://www.googleapis.com/youtube/v3/videos?key=***REMOVED***&part=snippet&id=";
    private static final String VIMEO_API_REQUEST = "http://vimeo.com/api/v2/video/";
    private static final String FLICKR_API_REQUEST = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&api_key=***REMOVED***&format=json&nojsoncallback=1&photo_id=";
    private static final String IPERNITY_API_REQUEST = "http://api.ipernity.com/api/doc.get/json?api_key=***REMOVED***&extra=tags&doc_id=";

    private static final String base58alphabetString = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final int DESCRIPTION_LIMIT = 1400;

    private final Learnweb learnweb;
    private final FileManager fileManager;
    private final FileInspector fileInspector;

    public ResourceMetadataExtractor(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        this.fileManager = learnweb.getFileManager();
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
        log.debug("Get the mime type and extract text if possible");
        FileInfo fileInfo = this.getFileInfo(mainFile.getInputStream(), resource.getFileName());
        processFileResource(resource, fileInfo);

        // TODO Oleh: move it somewhere close to converting video to mp4
        if(shouldBeConverted(fileInfo.getFileName()))
        {
            File fileForConversion = createFileForConversion(mainFile);

            InputStream inputStream = learnweb.getServiceConverter().convert(fileInfo.getFileName(), fileForConversion.getUrl());
            fileManager.delete(mainFile);

            if(resource.getType().equals(Resource.ResourceType.document))
                resource.setFormat("vnd.openxmlformats-officedocument.wordprocessingml.document");
            else if(resource.getType().equals(Resource.ResourceType.spreadsheet))
                resource.setFormat("vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            else if(resource.getType().equals(Resource.ResourceType.presentation))
                resource.setFormat("vnd.openxmlformats-officedocument.presentationml.presentation");

            fileForConversion.setMimeType(resource.getFormat());
            fileManager.save(fileForConversion, inputStream);
        }
    }

    private File createFileForConversion(File source)
    {
        File destination = new File();
        String fileType = FileUtility.getFileType(source.getName());
        String internalFileExt = FileUtility.getInternalExtension(fileType);
        destination.setName(source.getName().substring(0, source.getName().indexOf(".")) + internalFileExt);
        destination.setUrl(source.getUrl());
        destination.setType(source.getType());
        destination.setMimeType(source.getMimeType());
        destination.setDownloadLogActivated(true);
        return destination;
    }

    private boolean shouldBeConverted(String fileName)
    {
        return learnweb.getProperties().getProperty("FILES.DOCSERVICE.CONVERT-DOCS").contains(fileName.substring(fileName.lastIndexOf(".")));
    }

    public void processWebResource(Resource resource)
    {
        if(StringUtils.isEmpty(resource.getUrl()))
            throw new RuntimeException("Given resource doesn't have a url!");

        try
        {
            resource.setOnlineStatus(OnlineStatus.ONLINE);

            Pattern compYouTubePattern = Pattern.compile(YOUTUBE_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher youTubeMatcher = compYouTubePattern.matcher(resource.getUrl());
            if(youTubeMatcher.find())
            {
                resource.setType(Resource.ResourceType.video);
                resource.setSource("Youtube");
                resource.setIdAtService(youTubeMatcher.group(1));
                processYoutubeResource(resource);
                return;
            }

            Pattern compVimeoPattern = Pattern.compile(VIMEO_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher vimeoMatcher = compVimeoPattern.matcher(resource.getUrl());
            if(vimeoMatcher.find())
            {
                resource.setType(Resource.ResourceType.video);
                resource.setSource("Vimeo");
                resource.setIdAtService(vimeoMatcher.group(1));
                processVimeoResource(resource);
                return;
            }

            Pattern compFlickrPattern = Pattern.compile(FLICKR_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher flickrMatcher = compFlickrPattern.matcher(resource.getUrl());
            if(flickrMatcher.find())
            {
                resource.setType(Resource.ResourceType.image);
                resource.setSource("Flickr");
                resource.setIdAtService(flickrMatcher.group(1));
                processFlickrResource(resource);
                return;
            }

            Pattern compFlickrShortPattern = Pattern.compile(FLICKR_SHORT_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher flickrShortMatcher = compFlickrShortPattern.matcher(resource.getUrl());
            if(flickrShortMatcher.find())
            {
                resource.setType(Resource.ResourceType.image);
                resource.setSource("Flickr");
                resource.setIdAtService(base58_decode(flickrShortMatcher.group(1)));
                processFlickrResource(resource);
                return;
            }

            Pattern compIpernityPattern = Pattern.compile(IPERNITY_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher ipernityMatcher = compIpernityPattern.matcher(resource.getUrl());
            if(ipernityMatcher.find())
            {
                resource.setType(Resource.ResourceType.image);
                resource.setSource("Ipernity");
                resource.setIdAtService(ipernityMatcher.group(1));
                processIpernityResource(resource);
                return;
            }

            resource.setType(Resource.ResourceType.website);
            resource.setSource("Internet");
            FileInfo fileInfo = getFileInfo(FileInspector.openStream(resource.getUrl()), "unknown");
            processFileResource(resource, fileInfo);
        }
        catch(JSONException | IOException e)
        {
            resource.setOnlineStatus(OnlineStatus.UNKNOWN); // most probably offline
            log.error("Can't get more details about resource (url: " + resource.getUrl() + ") from " + resource.getSource() + " source.");
        }
    }

    private void processYoutubeResource(Resource resource) throws IOException, JSONException
    {
        JSONObject json = readJsonObjectFromUrl(YOUTUBE_API_REQUEST + resource.getIdAtService());
        JSONArray items = json.getJSONArray("items");
        if(items.length() > 0)
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
            Optional<String> size = Arrays.stream(new String[] {"maxres", "standard", "high", "medium", "default"}).filter(thumbnails::has).findFirst();

            if (size.isPresent())
                resource.setMaxImageUrl(thumbnails.getJSONObject(size.get()).getString("url"));
        }
    }

    private void processVimeoResource(Resource resource) throws IOException, JSONException
    {
        JSONObject json = readJsonArrayFromUrl(VIMEO_API_REQUEST + resource.getIdAtService() + ".json").getJSONObject(0);
        if(json != null)
        {
            if(resource.getTitle() == null || resource.getTitle().length() == 0)
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

            Optional<String> size = Arrays.stream(new String[] {"thumbnail_large", "thumbnail_medium", "thumbnail_small"}).filter(json::has).findFirst();

            if (size.isPresent())
                resource.setMaxImageUrl(json.getString(size.get()));
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
            if(StringUtils.isEmpty(resource.getAuthor())) {
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
            if(thumbnails != null && thumbnails.length() > 0)
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

        if(StringUtils.isNotEmpty(fileInfo.getTitle()) && StringUtils.isEmpty(resource.getTitle()))
            resource.setTitle(fileInfo.getTitle());

        if(StringUtils.isNotEmpty(fileInfo.getAuthor()) && StringUtils.isEmpty(resource.getAuthor()))
            resource.setAuthor(fileInfo.getAuthor());

        if(StringUtils.isNotEmpty(fileInfo.getDescription()) && StringUtils.isEmpty(resource.getDescription()))
            resource.setDescription(StringHelper.shortnString(fileInfo.getDescription(), DESCRIPTION_LIMIT));

        if(StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getMachineDescription()))
            resource.setMachineDescription(fileInfo.getTextContent());

        if(StringUtils.isNotEmpty(fileInfo.getTextContent()) && StringUtils.isEmpty(resource.getDescription()))
            resource.setDescription(StringHelper.shortnString(fileInfo.getTextContent(), DESCRIPTION_LIMIT));
    }

    private String base58_decode(String snipcode)
    {
        Long result = 0L;
        long multi = 1;
        while (snipcode.length() > 0) {
            String digit = snipcode.substring(snipcode.length()-1);
            result = result + multi * base58alphabetString.lastIndexOf(digit);
            multi = multi * base58alphabetString.length();
            snipcode = snipcode.substring(0, snipcode.length()-1);
        }
        return result.toString();
    }

    public FileInfo getFileInfo(InputStream inputStream, String fileName) throws IOException
    {
        return fileInspector.inspect(inputStream, fileName);
    }

    private static JSONObject readJsonObjectFromUrl(String url) throws IOException, JSONException
    {
        return new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));
    }

    private static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException
    {
        return new JSONArray(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException
    {
        String url = "https://flic.kr/p/tcT8oi";
        url = AddResourceBean.checkUrl(url);

        Resource resource = new Resource();
        resource.setStorageType(Resource.WEB_RESOURCE);
        resource.setUrl(url);

        ResourceMetadataExtractor rme = new ResourceMetadataExtractor(Learnweb.createInstance(""));
        rme.processResource(resource);

        log.debug(resource.getType());
        log.debug(resource.getSource());
        log.debug(resource.getIdAtService());
        log.debug(resource.getTitle());
        log.debug(resource.getDescription());
        log.debug(resource.getAuthor());
        log.debug(resource.getMaxImageUrl());
    }
}

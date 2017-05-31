package de.l3s.learnweb;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.learnweb.Resource.OnlineStatus;
import de.l3s.learnweb.beans.AddResourceBean;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
import de.l3s.util.StringHelper;

/**
 * Helper for extract metadata from Resource
 * 
 * @author Oleh Astappiev
 */
public class ResourceMetadataExtractor
{
    private final static Logger log = Logger.getLogger(ResourceMetadataExtractor.class);

    private final static String YOUTUBE_PATTERN = "https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*";
    private final static String VIMEO_PATTERN = "https?:\\/\\/(?:www\\.)?(?:player\\.)?vimeo\\.com\\/(?:[a-z]*\\/)*([0-9]{6,11})[?]?.*";
    private final static String FLICKR_PATTERN = "https?:\\/\\/(?:www\\.)?flickr\\.com\\/(?:photos\\/[^/]+\\/(\\d+))";
    private final static String IPERNITY_PATTERN = "https?:\\/\\/(?:www\\.)?ipernity\\.com\\/(?:doc\\/[^/]+\\/(\\d+))";

    private static final String YOUTUBE_API_REQUEST = "https://www.googleapis.com/youtube/v3/videos?key=***REMOVED***&part=snippet&id=";
    private static final String VIMEO_API_REQUEST = "http://vimeo.com/api/v2/video/";
    private static final String FLICKR_API_REQUEST = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&api_key=***REMOVED***&format=json&nojsoncallback=1&photo_id=";
    private static final String IPERNITY_API_REQUEST = "http://api.ipernity.com/api/doc.get/json?api_key=***REMOVED***&extra=tags&doc_id=";

    private static final int DESCRIPTION_LIMIT = 1000;

    private Resource resource;
    private FileInfo fileinfo;

    public ResourceMetadataExtractor(Resource res)
    {
        this.resource = res;
    }

    public Resource getResource()
    {
        return resource;
    }

    public FileInfo getFileInfo() throws IOException
    {
        if(fileinfo == null)
        {
            fileinfo = new FileInspector(Learnweb.getInstance()).inspect(FileInspector.openStream(resource.getUrl()), "unknown");
        }

        return fileinfo;
    }

    public void process()
    {
        if(resource.getStorageType() == Resource.FILE_RESOURCE)
        {
            processFileResource();
        }
        else if(resource.getStorageType() == Resource.WEB_RESOURCE)
        {
            processWebResource();
        }
        else
        {
            throw new RuntimeException();
        }
    }

    public void processFileResource()
    {
        // TODO: move from addResourceBean
    }

    public void processWebResource()
    {
        if(resource.getUrl() != null)
        {
            resource.setUrl(resource.getUrl());
            extractWebSource();
            extractMetadata();

            if(resource.getThumbnail4() == null)
                makePreview();
        }
        else
        {
            throw new RuntimeException("Web resources have to have url");
        }
    }

    public void extractMetadata()
    {
        ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();

        if(resource.getSource().equalsIgnoreCase("youtube") && resource.getIdAtService() != null)
        {
            try
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
                    resource.setEmbeddedRaw("<iframe src=\"https://www.youtube.com/embed/" + resource.getIdAtService() + "\" frameborder=\"0\" allowfullscreen></iframe>");

                    // TODO: save tags for resource
                    /*JSONArray tags = (JSONArray) snippet.get("tags");
                    if(tags != null && tags.length() > 0)
                    {
                    for(int i = 0, len = tags.length(); i < len; i++)
                    {
                        resource.addTag(tags.get(i).toString(), null);
                    }
                    }*/

                    JSONObject thumbnails = snippet.getJSONObject("thumbnails");
                    String thumbnailUrl = null;

                    if(thumbnails.has("maxres"))
                    {
                        thumbnailUrl = thumbnails.getJSONObject("maxres").getString("url");
                    }
                    else if(thumbnails.has("standard"))
                    {
                        thumbnailUrl = thumbnails.getJSONObject("standard").getString("url");
                    }
                    else if(thumbnails.has("high"))
                    {
                        thumbnailUrl = thumbnails.getJSONObject("high").getString("url");
                    }
                    else if(thumbnails.has("medium"))
                    {
                        thumbnailUrl = thumbnails.getJSONObject("medium").getString("url");
                    }
                    else if(thumbnails.has("default"))
                    {
                        thumbnailUrl = thumbnails.getJSONObject("default").getString("url");
                    }

                    if(thumbnailUrl != null)
                    {
                        rpm.processImage(resource, FileInspector.openStream(thumbnailUrl));
                    }
                }
            }
            catch(Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if(resource.getSource().equalsIgnoreCase("vimeo") && resource.getIdAtService() != null)
        {
            try
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
                    resource.setDuration(json.getInt("duration"));
                    resource.setEmbeddedRaw("<iframe src=\"//player.vimeo.com/video/" + resource.getIdAtService() + "\" frameborder=\"0\" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>");

                    // TODO: save tags for resource
                    /*String tags = object.get("tags").toString();
                    for(String tag : Arrays.asList(tags.split(", ")))
                    {
                    resource.addTag(tag, null);
                    }*/

                    String thumbnail = null;

                    if(json.has("thumbnail_large"))
                    {
                        thumbnail = json.getString("thumbnail_large");
                    }
                    else if(json.has("thumbnail_medium"))
                    {
                        thumbnail = json.getString("thumbnail_medium");
                    }
                    else if(json.has("thumbnail_small"))
                    {
                        thumbnail = json.getString("thumbnail_small");
                    }

                    if(thumbnail != null)
                    {
                        rpm.processImage(resource, FileInspector.openStream(thumbnail));
                    }
                }
            }
            catch(Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if(resource.getSource().equalsIgnoreCase("flickr") && resource.getIdAtService() != null)
        {
            try
            {
                JSONObject json = readJsonObjectFromUrl(FLICKR_API_REQUEST + resource.getIdAtService()).getJSONObject("photo");
                if(json != null)
                {
                    if(StringUtils.isEmpty(resource.getTitle()))
                        resource.setTitle(json.getJSONObject("title").getString("_content"));
                    if(StringUtils.isEmpty(resource.getDescription()))
                        resource.setDescription(StringHelper.shortnString(json.getJSONObject("description").getString("_content"), DESCRIPTION_LIMIT));
                    JSONObject owner = json.getJSONObject("owner");
                    String author = owner.getString("realname");
                    if(author == null || author.isEmpty())
                    {
                        author = owner.getString("username");
                    }
                    if(StringUtils.isEmpty(resource.getAuthor()))
                        resource.setAuthor(author);

                    String thumbnail = "https://farm" + json.getString("farm") + ".staticflickr.com/" + json.getString("server") + "/" + json.getString("id") + "_" + json.getString("secret") + ".jpg";
                    rpm.processImage(resource, FileInspector.openStream(thumbnail));

                    // TODO: save tags for resource
                    /*JSONArray tags = (JSONArray) ((JSONObject) photo.get("tags")).get("tag");
                    if(tags != null && tags.length() > 0)
                    {
                    for(int i = 0, len = tags.length(); i < len; i++)
                    {
                        resource.addTag(((JSONObject) tags.get(i)).get("raw").toString(), null);
                    }
                    }*/
                }
            }
            catch(Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if(resource.getSource().equalsIgnoreCase("ipernity") && resource.getIdAtService() != null)
        {
            try
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

                    // TODO: save tags for resource
                    /*
                    JSONArray tags = (JSONArray) ((JSONObject) json.get("tags")).get("tag");
                    if(tags != null && tags.length() > 0)
                    {
                    for(int i = 0, len = tags.length(); i < len; i++)
                    {
                        resource.addTag(((JSONObject) tags.get(i)).get("tag").toString(), null);
                    }
                    }*/

                    JSONArray thumb = json.getJSONObject("thumbs").getJSONArray("thumb");
                    String thumbnailUrl = null;

                    if(thumb != null && thumb.length() > 0)
                    {
                        for(int i = 0, len = thumb.length(), weight = 0; i < len; i++)
                        {
                            JSONObject th = thumb.getJSONObject(i);
                            int newWeight = Integer.parseInt(th.getString("w"));

                            if(newWeight > weight)
                            {
                                weight = newWeight;
                                thumbnailUrl = th.getString("url");
                            }
                        }
                    }

                    if(thumbnailUrl != null)
                    {
                        rpm.processImage(resource, FileInspector.openStream(thumbnailUrl));
                    }
                }
            }
            catch(Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            extractFileInfo();
        }
    }

    private void extractFileInfo()
    {
        try
        {
            if(resource.getType().equalsIgnoreCase("text") || resource.getType().equalsIgnoreCase("unknown"))
            {
                FileInfo info = getFileInfo();

                String type = info.getMimeType().startsWith("application/") ? info.getMimeType().substring(info.getMimeType().indexOf("/") + 1) : info.getMimeType();

                if(StringUtils.isEmpty(resource.getTitle()))
                    resource.setTitle(info.getTitle());

                if(type.equals("text/html") || type.equals("text/plain") || type.equals("xhtml+xml") || type.equals("octet-stream") || type.equals("blog-post") || type.equals("x-gzip"))
                {
                    resource.setType("text");
                    if(StringUtils.isEmpty(resource.getAuthor()))
                        resource.setAuthor(info.getAuthor());
                    if(StringUtils.isEmpty(resource.getDescription()))
                        resource.setDescription(info.getDescription());
                    resource.setMachineDescription(info.getTextContent());
                    resource.setOnlineStatus(OnlineStatus.ONLINE);
                    extractWebSource();
                }
                else if(type.equals("pdf"))
                {
                    resource.setType(type);
                    if(StringUtils.isEmpty(resource.getDescription()))
                        resource.setDescription(StringHelper.shortnString(info.getTextContent(), 1400));
                    resource.setMachineDescription(info.getTextContent());
                }
                else if(type.startsWith("image/"))
                {
                    resource.setType("image");
                    resource.setFormat(info.getMimeType());
                }
                else
                {
                    log.error("Can't create thumbnail for mimetype: " + info.getMimeType());
                }
            }
        }
        catch(Exception e)
        {

            log.error(e);
            resource.setOnlineStatus(OnlineStatus.UNKNOWN); // most probably offline
        }
    }

    public void makePreview()
    {
        try
        {
            ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();

            if(resource.getType().equalsIgnoreCase("text") || resource.getType().equalsIgnoreCase("unknown"))
            {
                rpm.processWebsite(resource);
            }
            else if(resource.getType().equalsIgnoreCase("image"))
            {
                rpm.processImage(resource, FileInspector.openStream(resource.getUrl()));
            }
            else if(resource.getType().equalsIgnoreCase("pdf"))
            {
                rpm.processPdf(resource, FileInspector.openStream(resource.getUrl()));
            }
            else if(resource.getStorageType() == Resource.WEB_RESOURCE && resource.getMaxImageUrl() != null && resource.getMaxImageUrl().length() > 4)
            {
                rpm.processImage(resource, FileInspector.openStream(resource.getMaxImageUrl()));
            }
            else
            {
                log.error("Can't create thumbnail. Don't know how to handle resource " + resource.getId());
            }
        }
        catch(IOException | SQLException e)
        {
            log.error("error while creating thumbnails for resource: " + resource, e);
        }
    }

    public void extractWebSource()
    {
        Pattern compYouTubePattern = Pattern.compile(YOUTUBE_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher youTubeMatcher = compYouTubePattern.matcher(resource.getUrl());

        Pattern compVimeoPattern = Pattern.compile(VIMEO_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher vimeoMatcher = compVimeoPattern.matcher(resource.getUrl());

        Pattern compFlickrPattern = Pattern.compile(FLICKR_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher flickrMatcher = compFlickrPattern.matcher(resource.getUrl());

        Pattern compIpernityPattern = Pattern.compile(IPERNITY_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher ipernityMatcher = compIpernityPattern.matcher(resource.getUrl());

        if(youTubeMatcher.find())
        {
            //resource.setEmbeddedRaw(embeddedRaw);
            resource.setType("video");
            resource.setSource("Youtube");
            resource.setIdAtService(youTubeMatcher.group(1));
            resource.setEmbeddedRaw("<iframe src=\"https://www.youtube.com/embed/" + resource.getIdAtService() + "\" frameborder=\"0\" allowfullscreen></iframe>");
        }
        else if(vimeoMatcher.find())
        {
            resource.setType("video");
            resource.setSource("Vimeo");
            resource.setIdAtService(vimeoMatcher.group(1));
        }
        else if(flickrMatcher.find())
        {
            // TODO: add pattern for short urls encoded with base58
            resource.setType("image");
            resource.setSource("Flickr");
            resource.setIdAtService(flickrMatcher.group(1));
        }
        else if(ipernityMatcher.find())
        {
            resource.setType("image");
            resource.setSource("Ipernity");
            resource.setIdAtService(ipernityMatcher.group(1));
        }
        else
        {
            resource.setType("text");
            resource.setSource("Internet");
        }
    }

    private static JSONObject readJsonObjectFromUrl(String url) throws IOException, JSONException
    {
        return new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));
    }

    private static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException
    {
        return new JSONArray(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));
    }

    public static void main(String[] args)
    {
        String url = "https://www.youtube.com/watch?v=nLsbIxKDKE4";
        url = AddResourceBean.checkUrl(url);

        Resource resource = new Resource();
        resource.setStorageType(Resource.WEB_RESOURCE);
        resource.setUrl(url);

        ResourceMetadataExtractor rme = new ResourceMetadataExtractor(resource);
        rme.extractWebSource();
        rme.extractMetadata();

        log.debug(resource.getType());
        log.debug(resource.getSource());
        log.debug(resource.getIdAtService());
        log.debug(resource.getTitle());
        log.debug(resource.getDescription());
        log.debug(resource.getAuthor());
    }
}

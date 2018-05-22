package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.yellMetadata.ExtendedMetadata;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class Resource extends AbstractResource implements Serializable // AbstractResultItem,
{
    private static final long serialVersionUID = -8486919346993051937L;
    private final static Logger log = Logger.getLogger(Resource.class);

    public enum OnlineStatus
    {
        UNKNOWN,
        ONLINE,
        OFFLINE,
        PROCESSING // e.g. while a document/video is converted
    }

    public enum ResourceType
    {
        text,
        video,
        image,
        audio,
        pdf,
        website,
        spreadsheet,
        presentation,
        document,
        file, // applications, archives, etc

        // learnweb types
        survey,
        glossary,
        ;

        @Override
        public String toString()
        {
            switch(this)
            {
            case text:
                return "Document";
            default:
                return super.toString();
            }
        }
    }

    public enum ResourceViewRights
    {
        DEFAULT_RIGHTS, //inherits group rights
        SUBMISSION_READABLE, // the submitter of the resource (stored in the original resource id) and assessors can view the resource
        LEARNWEB_READABLE, //all learnweb users can view resource given url
        WORLD_READABLE, //all internet users with access to url can view resource
    }

    public static final int LEARNWEB_RESOURCE = 1;
    public static final int WEB_RESOURCE = 2;

    private int id = -1; // default id, that indicates that this resource is not stored at fedora
    private int groupId = 0;
    private int folderId = 0;
    private String title;
    private String description = "";
    private String url;
    private int storageType = WEB_RESOURCE;
    private ResourceViewRights rights = ResourceViewRights.DEFAULT_RIGHTS;
    private String source = ""; // The place where the resource was found
    private String location = ""; // The location where the resource content (e.g. video) is stored; for example Learnweb, Flickr, Youtube ...
    private String language; // language code
    private String author = "";
    private ResourceType type;
    private String format = ""; // original mineType of the resource
    private int duration;
    private int ownerUserId;
    private String idAtService = "";
    private int ratingSum;
    private int rateNumber;
    private String fileName; // stores the file name of uploaded resource
    private String fileUrl;
    private String maxImageUrl; // an url to the largest image preview of this resource
    private String query; // the query which was used to find this resource
    private int originalResourceId = 0; // if the resource was copied from an existing Learnweb resource this field stores the id of the original resource
    private String machineDescription;
    private Thumbnail thumbnail0;
    private Thumbnail thumbnail1;
    private Thumbnail thumbnail2;
    private Thumbnail thumbnail2b;
    private Thumbnail thumbnail2c;
    private Thumbnail thumbnail3;
    private Thumbnail thumbnail4;
    private String embeddedRaw; // stored in the database
    private String embeddedCode; // derived from type or embedded raw. Does not need to be stored in DB
    private String transcript; //To store the English transcripts for TED videos and saved articles
    private boolean readOnlyTranscript = false; //indicates resource transcript is read only for TED videos
    private OnlineStatus onlineStatus = OnlineStatus.UNKNOWN;
    private boolean restricted = false;
    private Date resourceTimestamp = null;
    private Date creationDate = new Date();
    private Map<String, String> metadata = new HashMap<>(); // field_name : field_value

    private boolean deleted = false; // indicates whether this resource has been deleted
    private LogEntry thumbnailUpdateInfo = null;

    private int thumbUp = -1;
    private int thumbDown = -1;
    private HashMap<Integer, Boolean> isThumbRatedByUser = new HashMap<>(); // userId : hasRated
    private HashMap<Integer, Boolean> isRatedByUser = new HashMap<>(); // userId : hasRated
    private LinkedHashMap<Integer, File> files = new LinkedHashMap<>(); // resource_file_number : file

    //glossary information
    @Deprecated
    private String languageOne; // TODO remove
    @Deprecated
    private String languageTwo; // TODO remove

    // caches
    private transient OwnerList<Tag, User> tags = null;
    private transient List<Comment> comments;
    private transient User owner;
    private transient LinkedList<ArchiveUrl> archiveUrls = null; //To store the archived URLs
    private transient String path = null;
    private transient String prettyPath = null;
    private transient MetadataMapWrapper metadataWrapper; // includes static fields like title, description and author into the map
    private transient MetadataMultiValueMapWrapper metadataMultiValue;

    //extended metadata (Chloe)
    private transient ExtendedMetadata extendedMetadata = null;
    private String mtype;
    private String msource;
    private String[] selectedMtypes;

    /**
     * Do nothing constructor
     */
    public Resource()
    {
    }

    /**
     * Called by the ResorceManager after all setters have been called.
     *
     * @throws SQLException
     *
     */
    protected void postConstruct() throws SQLException
    {
        setDefaultThumbnailIfNull();
    }

    /**
     * This constructor is used to create resources when returned from the learnweb resources table in order
     * to re-visit a previous result set of a query posted in the past.
     */
    public Resource(int id, String description, String title, String source, int thumbnail_height, int thumbnail_width, String thumbnail_url, int thumbnail4_height, int thumbnail4_width, String thumbnail4_url, String url, String type)
    {
        this.id = id;
        this.description = description;
        this.title = title;
        this.source = source;
        this.url = url;
        this.setType(type);
        setThumbnail2(new Thumbnail(thumbnail_url, thumbnail_width, thumbnail_height));
        setThumbnail4(new Thumbnail(thumbnail4_url, thumbnail4_width, thumbnail4_height));
    }

    /**
     * If no thumbnails have been assigend this method will create default thumbnails for the small thumbnails
     */
    public void setDefaultThumbnailIfNull()
    {
        if(null == thumbnail0 || null == thumbnail1 || null == thumbnail2)
        {
            Thumbnail dummyImage = new Thumbnail("https://learnweb.l3s.uni-hannover.de/javax.faces.resource/icon/grain.png.jsf?ln=lightbox", 200, 200);

            if(null == thumbnail0)
                setThumbnail0(dummyImage.resize(150, 120));
            if(null == thumbnail1)
                setThumbnail1(dummyImage.resize(150, 150));
            if(null == thumbnail2)
                setThumbnail2(dummyImage);
        }
    }

    public void addTag(String tagName, User user) throws SQLException
    {
        if(tagName.length() > 250)
            throw new IllegalArgumentException("tag is to long");

        ResourceManager rsm = Learnweb.getInstance().getResourceManager();
        Tag tag = rsm.getTag(tagName);

        if(tag == null)
            tag = rsm.addTag(tagName);

        rsm.tagResource(this, tag, user);

        if(null != tags)
        {
            tags.add(tag, user, new Date());
            Collections.sort(tags);
        }

        Learnweb.getInstance().getSolrClient().indexTag(tag, this);
    }

    public void deleteTag(Tag tag) throws SQLException
    {
        Learnweb.getInstance().getResourceManager().deleteTag(tag, this);
        tags.remove(tag);

        Learnweb.getInstance().getSolrClient().deleteFromIndex(tag, this);
    }

    public void deleteComment(Comment comment) throws Exception
    {
        Learnweb.getInstance().getResourceManager().deleteComment(comment);
        comments.remove(comment);

        Learnweb.getInstance().getSolrClient().deleteFromIndex(comment);
    }

    public List<Comment> getComments() throws SQLException
    {
        if(id != -1 && comments == null)
        {
            comments = Learnweb.getInstance().getResourceManager().getCommentsByResourceId(id);
            //Collections.sort(comments);
        }

        return comments;
    }

    public Comment addComment(String text, User user) throws SQLException
    {
        Comment comment = Learnweb.getInstance().getResourceManager().commentResource(text, user, this);

        getComments(); // make sure comments are loaded before adding a new one
        comments.add(0, comment);

        Learnweb.getInstance().getSolrClient().indexComment(comment);

        return comment;
    }

    @Override
    public int getId()
    {
        return id;
    }

    @Override
    public int getGroupId()
    {
        return groupId;
    }

    @Override
    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    @Override
    public Group getGroup() throws SQLException
    {
        if(groupId == 0)
            return null;

        return Learnweb.getInstance().getGroupManager().getGroupById(groupId);
    }

    @Override
    public int getUserId()
    {
        return ownerUserId;
    }

    @Override
    public void setUserId(int userId)
    {
        this.ownerUserId = userId;
        this.owner = null;
    }

    @Override
    public User getUser() throws SQLException
    {
        if(null == owner && -1 != ownerUserId)
            owner = Learnweb.getInstance().getUserManager().getUser(ownerUserId);
        return owner;
    }

    @Override
    public void setUser(User user)
    {
        this.owner = user;
        this.ownerUserId = owner.getId();
    }

    @Deprecated
    public User getOwnerUser() throws SQLException
    {
        return getUser();
    }

    public Group getOriginalGroup() throws SQLException
    {
        if(originalResourceId == 0)
            return null;

        Resource originalResource = Learnweb.getInstance().getResourceManager().getResource(originalResourceId);
        if(originalResource != null)
            return Learnweb.getInstance().getGroupManager().getGroupById(originalResource.getGroupId());
        else
            return null;
    }

    public void setGroup(Group group)
    {
        this.groupId = group.getId();
    }

    public int getFolderId()
    {
        return folderId;
    }

    public void setFolderId(int folderId)
    {
        this.folderId = folderId;
    }

    public Folder getFolder() throws SQLException
    {
        if(folderId == 0)
            return null;

        return Learnweb.getInstance().getGroupManager().getFolder(folderId);
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public String getDescriptionHTML()
    {
        return description.replace("\n", "<br/>");
    }

    public int getStorageType()
    {
        return storageType;
    }

    public boolean isOfficeResource()
    {
        if(source.equals("SlideShare"))
            return false;
        return Resource.ResourceType.document.equals(type) || Resource.ResourceType.spreadsheet.equals(type) || Resource.ResourceType.presentation.equals(type);
    }

    public boolean isOnline()
    {
        return OnlineStatus.ONLINE.equals(onlineStatus);
    }

    public boolean isOffline()
    {
        return OnlineStatus.OFFLINE.equals(onlineStatus);
    }

    public boolean isProcessing()
    {
        return OnlineStatus.PROCESSING.equals(onlineStatus);
    }

    public String getStringStorageType()
    {
        if(storageType == Resource.LEARNWEB_RESOURCE)
            return "Learnweb";
        else if(storageType == Resource.WEB_RESOURCE)
            return UtilBean.getLocaleMessage("web");
        else
            throw new RuntimeException();
    }

    public void setStorageType(int type)
    {
        if(type != LEARNWEB_RESOURCE && type != WEB_RESOURCE)
            throw new IllegalArgumentException("Unknown storageType of the resource: " + id);
        this.storageType = type;
    }

    public int getRights()
    {
        return rights.ordinal();
    }

    public void setRights(int rights)
    {
        //this.rights = rights;
        switch(rights)
        {
        case 0:
            this.rights = ResourceViewRights.DEFAULT_RIGHTS;
            break;
        case 1:
            this.rights = ResourceViewRights.SUBMISSION_READABLE;
            break;
        case 2:
            this.rights = ResourceViewRights.LEARNWEB_READABLE;
            break;
        case 3:
            this.rights = ResourceViewRights.WORLD_READABLE;
        }
    }

    public void setRights(ResourceViewRights rights)
    {
        this.rights = rights;
    }

    /**
     * The location where the resource (metadata) is stored
     *
     * @return for example Learnweb, Flickr, Youtube ...
     */
    public String getLocation()
    {
        return location;
    }

    public ResourceType getType()
    {
        return type;
    }

    public String getFormat()
    {
        return format;
    }

    public double getStarRating()
    {
        return ratingSum == 0 ? 0.0 : (double) ratingSum / (double) rateNumber;
    }

    public int getStarRatingRounded()
    {
        return ratingSum == 0 ? 0 : getRatingSum() / getRateNumber();
    }

    public void setStarRatingRounded(int value)
    {
        // dummy method, is required by p:rating
    }

    public void setRatingSum(int rating)
    {
        this.ratingSum = rating;
    }

    public void setRateNumber(int rateNumber)
    {
        this.rateNumber = rateNumber;
    }

    public int getRateNumber()
    {
        return rateNumber;
    }

    public int getRatingSum()
    {
        return ratingSum;
    }

    /**
     * @return Returns a comma separated list of tags
     */
    public String getTagsAsString() throws SQLException
    {
        return getTagsAsString(", ");
    }

    public String getTagsAsString(String delim) throws SQLException
    {
        tags = getTags();

        StringBuilder out = new StringBuilder();
        for(Tag tag : tags)
        {
            if(out.length() != 0)
                out.append(delim);
            out.append(tag.getName());
        }
        return out.toString();
    }

    public OwnerList<Tag, User> getTags() throws SQLException
    {
        if(null == tags || id != -1)
            tags = Learnweb.getInstance().getResourceManager().getTagsByResource(id);
        return tags;
    }

    public void setTags(OwnerList<Tag, User> tags)
    {
        this.tags = tags;
    }

    public void setComments(List<Comment> comments)
    {
        this.comments = comments;
    }

    @Override
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * Creates a copy of a resource.<br/>
     * Ratings and comments are not copied
     */
    @Override
    public Resource clone()
    {
        return new Resource(this);
    }

    /**
     * Copy constructor
     *
     * @param old
     */
    public Resource(Resource old)
    {
        setId(-1);
        setGroupId(old.groupId);
        setFolderId(old.folderId);
        setTitle(old.title);
        setDescription(old.description);
        setUrl(old.url);
        setStorageType(old.storageType);
        setRights(old.rights.ordinal());
        setLocation(old.location);
        setSource(old.source);
        setAuthor(old.author);
        setType(old.type);
        setFormat(old.format);
        setUserId(old.ownerUserId);
        setMaxImageUrl(old.maxImageUrl);
        setFileName(old.fileName);
        setFileUrl(old.fileUrl);
        setQuery(old.query);
        setThumbnail0(old.thumbnail0);
        setThumbnail1(old.thumbnail1);
        setThumbnail2(old.thumbnail2);
        setThumbnail3(old.thumbnail3);
        setThumbnail4(old.thumbnail4);
        setEmbeddedRaw(old.embeddedRaw);
        setDuration(old.duration);
        setMachineDescription(old.machineDescription);
        setFileName(old.fileName);
        setTranscript(old.transcript);
        setOnlineStatus(old.onlineStatus);
        setIdAtService(old.idAtService);
        setRestricted(old.restricted);
        setResourceTimestamp(old.resourceTimestamp);
        setCreationDate(old.creationDate);
        setArchiveUrls(old.getArchiveUrls());
        setDeleted(old.deleted);
        setReadOnlyTranscript(old.readOnlyTranscript);
        // sets the originalResourceId to the id of the source resource
        if(old.originalResourceId == 0)
            setOriginalResourceId(old.id);
        else
            setOriginalResourceId(old.originalResourceId);

        /*
        for(File file :files)
        {
            // TODO Philipp: copy files too. The DB layout doesn't support this right now

        }
        */
    }

    /**
     * rate this resource
     *
     * @param value the rating 1-5
     * @param user the user who rates
     */
    public void rate(int value, User user) throws SQLException
    {
        Learnweb.getInstance().getResourceManager().rateResource(id, user.getId(), value);

        rateNumber++;
        ratingSum += value;

        isRatedByUser.put(user.getId(), true);
    }

    public boolean isRatedByUser(int userId) throws SQLException
    {
        Boolean value = isRatedByUser.get(userId);
        if(null != value) // the answer is cached
            return value;

        // the answer isn't cached we have to query the database
        value = Learnweb.getInstance().getResourceManager().isResourceRatedByUser(id, userId);
        isRatedByUser.put(userId, value); // cache answer

        return value;
    }

    /**
     * Stores all made changes in the database and reindexes the resource at solr
     */
    @Override
    public Resource save() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().saveResource(this);
    }

    @Override
    public void setTitle(String title)
    {
        this.title = StringUtils.isNotEmpty(title) ? StringHelper.shortnString(StringEscapeUtils.unescapeHtml4(Jsoup.clean(title, Whitelist.none())), 980) : "no title";
    }

    public void setDescription(String description)
    {
        this.description = description == null ? "" : StringEscapeUtils.unescapeHtml4(StringHelper.clean(description, Whitelist.simpleText()));
    }

    public void setUrl(String url)
    {
        if(url != null && url.length() > 4000)
            throw new IllegalArgumentException("url is too long: " + url.length() + "; " + url);

        this.url = url;
    }

    /**
     * The location where the resource (metadata) is stored
     *
     * @param location for example Learnweb, Flickr, Youtube ...
     */
    public void setLocation(String location)
    {
        this.location = location;
    }

    public void setType(ResourceType type)
    {
        this.type = type;
    }

    public void setType(String type)
    {
        try
        {
            this.type = ResourceType.valueOf(type.toLowerCase());
        }
        catch(IllegalArgumentException e)
        {
            this.setTypeFromFormat(format);
        }
    }

    public void setTypeFromFormat(String format)
    {
        if(StringUtils.isEmpty(format))
        {
            log.error("Resource: " + getId() + "; Given format is empty: " + format, new Exception());
            return;
        }

        if(format.equals("text/html") || format.equals("application/xhtml+xml"))
            this.type = ResourceType.website;
        else if(format.startsWith("text/"))
            this.type = ResourceType.text;
        else if(format.startsWith("image/"))
            this.type = ResourceType.image;
        else if(format.startsWith("video/"))
            this.type = ResourceType.video;
        else if(format.startsWith("audio/"))
            this.type = ResourceType.audio;
        else if(format.equals("application/pdf"))
            this.type = ResourceType.pdf;
        else if(format.contains("ms-excel") || format.contains("spreadsheet"))
            this.type = ResourceType.spreadsheet;
        else if(format.contains("ms-powerpoint") || format.contains("presentation"))
            this.type = ResourceType.presentation;
        else if(format.contains("msword") || format.contains("ms-word") || format.contains("wordprocessing") || format.contains("opendocument.text") || format.equals("application/rtf"))
            this.type = ResourceType.document;
        else if(Arrays.asList("application/x-msdownload", "application/x-ms-dos-executable", "application/octet-stream", "application/x-gzip", "application/x-rar-compressed", "application/zip", "application/x-shockwave-flash", "message/rfc822").contains(format))
            // handle known types of downloadable resources
            this.type = ResourceType.file;
        else
        {
            // if we do not know the format, then  log it and set it to downloadable
            log.error("Unknown type for format: " + format + "; resourceId: " + getId(), new Exception());
            this.type = ResourceType.file;
        }
    }

    /**
     * Set the mime type
     *
     * @param format mime type
     */
    public void setFormat(String format)
    {
        this.format = format;
    }

    public int getThumbUp() throws SQLException
    {
        if(thumbUp < 0)
        {
            Learnweb.getInstance().getResourceManager().loadThumbRatings(this);
        }
        return thumbUp;
    }

    public int getThumbDown() throws SQLException
    {
        if(thumbDown < 0)
        {
            Learnweb.getInstance().getResourceManager().loadThumbRatings(this);
        }
        return thumbDown;
    }

    public void setThumbUp(int thumbUp)
    {
        this.thumbUp = thumbUp;
    }

    public void setThumbDown(int thumbDown)
    {
        this.thumbDown = thumbDown;
    }

    public void thumbRate(User user, int direction) throws IllegalAccessError, SQLException
    {
        if(direction != 1 && direction != -1)
            throw new IllegalArgumentException("Illegal value [" + direction + "] for direction. Valid values are 1 and -1");

        if(isThumbRatedByUser(user.getId()))
            throw new IllegalAccessError("You have already rated this resource");

        if(direction == 1)
            thumbUp++;
        else
            thumbDown++;

        Learnweb.getInstance().getResourceManager().thumbRateResource(id, user.getId(), direction);

        isThumbRatedByUser.put(user.getId(), true);
    }

    public boolean isThumbRatedByUser(int userId) throws SQLException
    {
        Boolean value = isThumbRatedByUser.get(userId);
        if(null != value) // the answer is cached
            return value;

        // the answer isn't cached we have to ask fedora

        value = Learnweb.getInstance().getResourceManager().isResourceThumbRatedByUser(id, userId);
        isThumbRatedByUser.put(userId, value); // cache answer

        return value;
    }

    public String getUrl()
    {
        return url;
    }

    /**
     * Returns the url of this resource but proxied through WAPS.io if enabled
     *
     * @return
     */
    public String getUrlProxied()
    {
        return UtilBean.getUserBean().getUrlProxied(getUrl());
    }

    public String getLearnwebUrl() throws SQLException
    {
        if(getId() != -1) // && getGroupId() != 0)
            return "group/resources.jsf?group_id=" + getGroupId() + "&resource_id=" + getId();

        return getUrlProxied();
    }

    public String getServiceIcon()
    {
        if(getId() != -1) // is stored at fedora
            return "/resources/icon/services/learnweb.gif";

        String format = ".gif";
        if(getLocation().equalsIgnoreCase("youtube") || getLocation().equalsIgnoreCase("flickr") || getLocation().equalsIgnoreCase("ipernity"))
            format = ".png";

        return "/resources/icon/services/" + getLocation().toLowerCase() + format;
    }

    public static Comparator<Resource> createIdComparator()
    {
        return new Comparator<Resource>()
        {
            @Override
            public int compare(Resource o1, Resource o2)
            {
                return new Integer(o1.getId()).compareTo(o2.getId());
            }
        };
    }

    public static Comparator<Resource> createTitleComparator()
    {
        return new Comparator<Resource>()
        {
            @Override
            public int compare(Resource o1, Resource o2)
            {
                if(null == o1 || null == o2)
                    return 0;
                return o1.getTitle().compareTo(o2.getTitle());
            }
        };
    }

    public static Comparator<Resource> createSourceComparator()
    {
        return new Comparator<Resource>()
        {
            @Override
            public int compare(Resource o1, Resource o2)
            {
                return o1.getLocation().compareTo(o2.getLocation());
            }
        };
    }

    public static Comparator<Resource> createTypeComparator()
    {
        return new Comparator<Resource>()
        {
            @Override
            public int compare(Resource o1, Resource o2)
            {
                return o1.getType().compareTo(o2.getType());
            }
        };
    }

    /**
     * html code, only image or text<br/>
     * max width and max height 100px
     */
    @Deprecated
    public String getEmbeddedSize1()
    {
        /*
        if(embeddedSize1 != null)
            return embeddedSize1;
        */
        if(getThumbnail1() != null)
            return getThumbnail1().getHtml();
        if(getThumbnail2() != null)
            return getThumbnail2().resize(150, 150).getHtml();
        if(getThumbnail3() != null)
            return getThumbnail3().resize(150, 150).getHtml();

        return "<img src=\"../resources/resources/img/website-140.png\" width=\"100\" height=\"100\" />";
    }

    /**
     * Url to the best (high resolution) available preview image.<br/>
     * Only available for interweb search results + ResourceMetadataExtractor save thumbnail url to the field
     */
    public String getMaxImageUrl()
    {
        return maxImageUrl;
    }

    /**
     * Url to the best (high resolution) available preview image.<br/>
     * Only available for interweb search results
     *
     * @param imageUrl
     */
    public void setMaxImageUrl(String imageUrl)
    {
        this.maxImageUrl = imageUrl;
    }

    public String getShortDescription()
    {
        return Jsoup.clean(StringHelper.shortnString(description, 200), Whitelist.simpleText());
    }

    /**
     * @return the file name of uploaded resource
     */
    public String getFileName()
    {
        if(fileName == null)
            return "";

        return fileName;
    }

    /**
     * @param fileName the file name of uploaded resource
     */
    public void setFileName(String fileName)
    {
        if(fileName != null && fileName.length() > 200)
            throw new IllegalArgumentException("file name is too long: " + fileName.length() + "; " + fileName);

        this.fileName = fileName;
    }

    public void setFiles(LinkedHashMap<Integer, File> files)
    {
        this.files = files;
    }

    /**
     * @return the query which was used to find this resource
     */
    public String getQuery()
    {
        if(query == null)
            return "none";
        return query;
    }

    /**
     * @param query the query which was used to find this resource
     */
    public void setQuery(String query)
    {
        this.query = query;
    }

    /**
     * @return if the resource was copied from an older Learnweb resource this returns the id of the original resource <b>0</b> otherwise
     */
    public int getOriginalResourceId()
    {
        return originalResourceId;
    }

    /**
     * @param originalResourceId if the resource was copied from an older Learnweb resource this stores the id of the original resource
     */
    public void setOriginalResourceId(int originalResourceId)
    {
        this.originalResourceId = originalResourceId;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * The place where the resource was found. Example: Flickr or Youtube or Desktop ...
     *
     * @return
     */
    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        /*
        SERVICE service = SERVICE.valueOf(source.toLowerCase().replace("-", ""));
        if(service == null)
            log.warn("Invalid source: " + source + " resource " + this);
        // TODO use  enum instead of string
        */
        /*
        if(null == source || source.length() == 0)
            log.info("Resource: " + id + "; source set to null");
        */
        this.source = source;
    }

    public LinkedHashMap<Integer, File> getFiles()
    {
        return files;
    }

    /**
     * This method does not persist the changes.<br/>
     * see: FileManager.addFileToResource(file, resource);
     */
    public void addFile(File file) throws SQLException
    {
        files.put(file.getType().ordinal(), file);
        file.setResourceId(id);

        if(id > 0) // the resource is already stored, the new file needs to be added to the database
        {
            FileManager fm = Learnweb.getInstance().getFileManager();
            fm.addFileToResource(file, this);
        }

    }

    public File getFile(File.TYPE fileType)
    {
        return files.get(fileType.ordinal());
    }

    /**
     *
     * @return If the uploaded file was modified (e.g. a video or office document) we keep a copy of the original file
     */
    public File getOriginalFile()
    {
        return getFile(TYPE.FILE_ORIGINAL);
    }

    /**
     *
     * @return If the uploaded file was modified (e.g. a video or office document) we keep a copy of the original file
     */
    public File getMainFile()
    {
        return getFile(TYPE.FILE_MAIN);
    }

    /**
     * @return Text that has been automatically extracted from the source file/url
     */
    public String getMachineDescription()
    {
        return machineDescription;
    }

    /**
     * @param machineDescription Text that has been automatically extracted from the source file/url
     */
    public void setMachineDescription(String machineDescription)
    {
        this.machineDescription = machineDescription;
    }

    /**
     * maximum width/height : 150 / 120
     */
    public Thumbnail getThumbnail0()
    {
        return thumbnail0;
    }

    /**
     * height and width = 150px
     */
    public Thumbnail getThumbnail1()
    {
        return thumbnail1;
    }

    /**
     * maximum width/height : 300 / 220
     */
    public Thumbnail getThumbnail2()
    {
        return thumbnail2;
    }

    /**
     * returns thumbnail2 but down scaled to a maximum size of 240 * 128
     */
    public Thumbnail getThumbnail2b()
    {
        return thumbnail2b;
    }

    /**
     * returns thumbnail2 but down scaled to a maximum size of 171 * 128
     */
    public Thumbnail getThumbnail2c()
    {
        return thumbnail2c;
    }

    /**
     * maximum width/height : 500 / 600
     */
    public Thumbnail getThumbnail3()
    {
        /*
        if(null == thumbnail3)
            return getThumbnail2();
        */
        return thumbnail3;
    }

    /**
     * maximum width/height : 1280 / 1024
     */
    public Thumbnail getThumbnail4()
    {
        if(null == thumbnail4)
            return getThumbnail3();

        return thumbnail4;
    }

    public Thumbnail getLargestThumbnail()
    {
        if(null != thumbnail4)
            return thumbnail4;
        if(null != thumbnail3)
            return thumbnail3;
        if(null != thumbnail2)
            return thumbnail2;
        if(null != thumbnail1)
            return thumbnail1;
        if(null != thumbnail0)
            return thumbnail0;

        return null;
    }

    public void setThumbnail0(Thumbnail thumbnail0)
    {
        this.thumbnail0 = thumbnail0;
    }

    public void setThumbnail1(Thumbnail thumbnail1)
    {
        this.thumbnail1 = thumbnail1;
    }

    public void setThumbnail2(Thumbnail thumbnail2)
    {
        this.thumbnail2 = thumbnail2;
        if(thumbnail2 != null)
        {
            this.thumbnail2b = thumbnail2.resize(240, 128);
            this.thumbnail2c = thumbnail2.resize(171, 128);
        }
    }

    public void setThumbnail3(Thumbnail thumbnail3)
    {
        this.thumbnail3 = thumbnail3;
    }

    public void setThumbnail4(Thumbnail thumbnail4)
    {
        this.thumbnail4 = thumbnail4;
    }

    public String getEmbedded()
    {
        if(embeddedCode == null)
        {
            if(StringUtils.isNoneEmpty(getEmbeddedRaw()))
            {
                // if the embedded code was explicitly defined then use it. Is necessary for Slideshare resources.
            }
            else if(getType().equals(ResourceType.image))
            {
                // first the small thumbnail is shown. The large image is loaded async through JS
                Thumbnail large = getLargestThumbnail();
                embeddedCode = "<img src=\"" + getThumbnail2().getUrl() + "\" height=\"" + large.getHeight() + "\" width=\"" + large.getWidth() + "\" original-src=\"" + large.getUrl() + "\"/>";
            }
            else if(getType().equals(ResourceType.website))
            {
                if(thumbnail4 != null)
                {
                    // first the small thumbnail is shown. The large image is loaded async through JS
                    Thumbnail large = thumbnail4;
                    embeddedCode = "<img src=\"" + getThumbnail2().getUrl() + "\" height=\"" + large.getHeight() + "\" width=\"" + large.getWidth() + "\" original-src=\"" + large.getUrl() + "\"/>";
                }
                else
                    embeddedCode = "<iframe src=\"" + getUrl() + "\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" />";
            }
            else if(getType().equals(ResourceType.video))
            {
                if(getSource().equalsIgnoreCase("loro") || getSource().equals("Yovisto") || getSource().equalsIgnoreCase("desktop"))
                    embeddedCode = "<iframe src=\"video.jsf?resource_id=" + id + "\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>";
                else if(getSource().equalsIgnoreCase("ted"))
                    embeddedCode = "<iframe src=\"" + getUrl().replace("http://www", "//embed").replace("https://www", "//embed") + "\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\"  webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>";
                else if(getSource().equalsIgnoreCase("youtube"))
                    embeddedCode = "<iframe src=\"https://youtube.com/embed/" + getIdAtService() + "\" width=\"100%\" height=\"100%\" frameborder=\"0\" allowfullscreen></iframe>";
                else if(getSource().equalsIgnoreCase("vimeo"))
                    embeddedCode = "<iframe src=\"https://player.vimeo.com/video/" + getIdAtService() + "\" width=\"100%\" height=\"100%\" frameborder=\"0\" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>";

                if(isProcessing())
                {
                    String response = "<h3 style='padding: 2rem; color: red; position: absolute; width: 100%; box-sizing: border-box;'>We are converting this video. If your browser can't display it, try again in a few minutes.</h3>" + embeddedCode;
                    embeddedCode = null; // do not cache the temporal warning
                    return response;
                }
            }

            // if no rule above works

            if(embeddedCode == null)
            {
                log.warn("can't create embeddedCode for resource: " + toString());
            }

        }

        return embeddedCode;
    }

    public int getDuration()
    {
        return duration;
    }

    public String getDurationInMinutes()
    {
        return StringHelper.getDurationInMinutes(duration);
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    /**
     * Embedded code that can't be created on the fly. For example videos and slideshows
     * Normally you should not call this function.
     * Use getEmbedded() instead.
     */
    public String getEmbeddedRaw()
    {
        return embeddedRaw;
    }

    public void setEmbeddedRaw(String embeddedRaw)
    {
        this.embeddedRaw = embeddedRaw;
    }

    @Override
    public String toString()
    {
        return "Resource [id=" + id + ", title=" + title + ", url=" + url + ", storageType=" + storageType + ", source=" + source + ", type=" + type + ", format=" + format + ", date=" + getCreationDate() + "]";
    }

    public String getTranscript()
    {
        return transcript;
    }

    public void setTranscript(String transcript)
    {
        this.transcript = transcript;
    }

    public OnlineStatus getOnlineStatus()
    {
        return onlineStatus;
    }

    public void setOnlineStatus(OnlineStatus onlineStatus)
    {
        this.onlineStatus = onlineStatus;
    }

    public String getIdAtService()
    {
        return idAtService;
    }

    public void setIdAtService(String idAtService)
    {
        this.idAtService = idAtService;
    }

    public LinkedList<ArchiveUrl> getArchiveUrls()
    {
        if(id != -1 && archiveUrls == null)
        {
            ResourceManager rm = Learnweb.getInstance().getResourceManager();
            try
            {
                archiveUrls = rm.getArchiveUrlsByResourceId(id);
                archiveUrls.addAll(rm.getArchiveUrlsByResourceUrl(url));
            }
            catch(SQLException e)
            {
                log.error("Error while retrieving archive urls for resource: ", e);
            }
        }

        return archiveUrls;
    }

    public HashMap<String, List<ArchiveUrl>> getArchiveUrlsAsYears()
    {
        HashMap<String, List<ArchiveUrl>> versions = new LinkedHashMap<String, List<ArchiveUrl>>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy");
        for(ArchiveUrl a : archiveUrls)
        {
            String year = df.format(a.getTimestamp());
            if(versions.containsKey(year))
                versions.get(year).add(a);
            else
            {
                versions.put(year, new ArrayList<ArchiveUrl>());
                versions.get(year).add(a);
            }
        }
        return versions;
    }

    public void addArchiveUrl(ArchiveUrl archiveUrl)
    {
        // TODO really add archive url; until then clean cache:
        archiveUrls = null;
    }

    public boolean isArchived()
    {
        return getArchiveUrls() != null && archiveUrls.size() > 0;
    }

    public ArchiveUrl getFirstArchivedObject()
    {
        return archiveUrls.getFirst();
    }

    public ArchiveUrl getLastArchivedObject()
    {
        return archiveUrls.getLast();
    }

    public boolean isRestricted()
    {
        return restricted;
    }

    public void setRestricted(boolean restricted)
    {
        this.restricted = restricted;
    }

    public String getFileUrl()
    {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl)
    {
        if(fileUrl != null && fileUrl.length() > 4000)
            throw new IllegalArgumentException("url is too long: " + fileUrl.length() + "; " + fileUrl);

        this.fileUrl = fileUrl;
    }

    /**
     * @return comma separated list of language codes
     */
    public String getLanguage()
    {
        if(null == language)
            return "";
        return language;
    }

    /**
     * @param language comma separated list of language codes
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    public Date getResourceTimestamp()
    {
        return resourceTimestamp;
    }

    public void setResourceTimestamp(Date resourceTimestamp)
    {
        this.resourceTimestamp = resourceTimestamp;
    }

    public Date getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
    }

    public void setArchiveUrls(LinkedList<ArchiveUrl> archiveUrls)
    {
        this.archiveUrls = archiveUrls;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    public boolean isReadOnlyTranscript()
    {
        return readOnlyTranscript;
    }

    public void setReadOnlyTranscript(boolean readOnlyTranscript)
    {
        this.readOnlyTranscript = readOnlyTranscript;
    }

    public Resource moveTo(int newGroupId, int newFolderId) throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().moveResource(this, newGroupId, newFolderId);
    }

    @Override
    public void delete() throws SQLException
    {
        //this.setGroupId(0);
        //this.setFolderId(0);
        setDeleted(true);
        this.save();
    }

    /**
     * returns a string representation of the resources path
     */
    @Override
    public String getPath() throws SQLException
    {
        if(null == path)
        {
            Folder folder = getFolder();
            if(folder != null)
                path = folder.getPath();
        }
        return path;
    }

    /**
     * returns a string representation of the resources path
     */
    @Override
    public String getPrettyPath() throws SQLException
    {
        if(null == prettyPath)
        {
            Folder folder = getFolder();
            if(folder != null)
                prettyPath = folder.getPrettyPath();
        }
        return prettyPath;
    }

    protected void clearCaches()
    {
        path = null;
        prettyPath = null;
        tags = null;
        comments = null;
        owner = null;
        archiveUrls = null;
        metadataWrapper = null;
        metadataMultiValue = null;
    }

    @Override
    public boolean canViewResource(User user) throws SQLException
    {
        //admins, moderators and resource owners can always view the resource
        if(user != null && (user.isModerator() || getUserId() == user.getId()))
            return true;

        switch(rights)
        {
        case WORLD_READABLE:
            return true;
        case LEARNWEB_READABLE:
            return user != null;

        case SUBMISSION_READABLE: // the submitter of the resource (stored in the original resource id) and assessors can view the resource
            Resource originalResource = Learnweb.getInstance().getResourceManager().getResource(originalResourceId);
            if(originalResource != null && originalResource.getUserId() == user.getId())
                return true; // the submitter can view his resource

            // check if the current user can assess this resource
            return Learnweb.getInstance().getPeerAssessmentManager().canAssessResource(user, this);

        case DEFAULT_RIGHTS: // if the resource is part of the group the group permissions are used
            Group group = getGroup();
            if(group != null)
                return group.canViewResources(user);
        }

        return false;
    }

    public boolean canModerateResource(User user)
    {
        if(user == null) // not logged in
            return false;

        // TODO check whether the resource belongs to a group that this moderator is allowed to control ... propably very inefficient

        return user.isModerator();
    }

    public boolean canAnnotateResource(User user) throws SQLException
    {
        if(user == null) // not logged in
            return false;

        Group group = getGroup();

        if(group != null)
            return group.canAnnotateResources(user);

        if(user.isAdmin() || ownerUserId == user.getId())
            return true;

        return false;
    }

    public LogEntry getThumbnailUpdateInfo() throws SQLException
    {
        if(thumbnailUpdateInfo == null)
        {
            thumbnailUpdateInfo = Learnweb.getInstance().getResourceManager().loadThumbnailUpdateInfo(getId());
        }
        return thumbnailUpdateInfo;
    }

    /**
     * @return the previous value associated with key, or null if there was no mapping for key. (A null return can also indicate that the map
     *         previously associated null with key.)
     */
    public String setMetadataValue(String key, String value)
    {
        return metadata.put(key, value);
    }

    public Set<String> getMetadataKeys()
    {
        return metadata.keySet();
    }

    public Set<Entry<String, String>> getMetadataEntries()
    {
        return metadata.entrySet();
    }

    public String getMetadataValue(String key)
    {
        return metadata.get(key);
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public MetadataMapWrapper getMetadataWrapper()
    {
        if(null == metadataWrapper)
            metadataWrapper = new MetadataMapWrapper(metadata);
        return metadataWrapper;
    }

    public Map<String, String[]> getMetadataMultiValue()
    {
        if(null == metadataMultiValue)
            metadataMultiValue = new MetadataMultiValueMapWrapper(getMetadataWrapper());
        return metadataMultiValue;
    }

    public void setMetadata(Object metadataObj)
    {
        if(metadataObj instanceof HashMap<?, ?>)
        {
            @SuppressWarnings("unchecked")
            HashMap<String, String> hashMap = (HashMap<String, String>) metadataObj;
            metadata = hashMap;
        }
        else
        {
            metadata = new HashMap<>();
            log.error("resource = " + getId() + "unknown metadata format: " + metadataObj.getClass().getName());
        }

        //clear wrapper
        metadataWrapper = null;
        metadataMultiValue = null;
    }

    /**
     * A map wrapper to support multi valued input fields
     *
     * @author Kemkes
     *
     */
    public class MetadataMapWrapper implements Map<String, String>, Serializable
    {
        private static final long serialVersionUID = -7357288281713761896L;
        private Map<String, String> wrappedMap;

        public MetadataMapWrapper(Map<String, String> wrappedMap)
        {
            this.wrappedMap = wrappedMap;
        }

        public Map<String, String> getWrappedMap()
        {
            return wrappedMap;
        }

        @Override
        public String get(Object key)
        {
            if(!key.getClass().equals(String.class))
                throw new IllegalArgumentException("key must be a string");

            String keyString = ((String) key).toLowerCase();

            switch(keyString)
            {
            case "title":
                return getTitle();
            case "author":
                return getAuthor();
            case "description":
                return getDescription();
            case "language":
                return getLanguage();
            }
            return wrappedMap.get(key);
        }

        @Override
        public String put(String key, String value)
        {
            switch(key)
            {
            case "title":
                setTitle(value);
                return value;
            case "author":
                setAuthor(value);
                return value;
            case "description":
                setDescription(value);
                return value;
            case "language":
                setLanguage(value);
                return value;
            }

            return wrappedMap.put(key, value);
        }

        @Override
        public void clear()
        {
            wrappedMap.clear();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return wrappedMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            return false;
        }

        @Override
        public Set<java.util.Map.Entry<String, String>> entrySet()
        {
            return wrappedMap.entrySet();
        }

        @Override
        public boolean isEmpty()
        {
            return wrappedMap.isEmpty();
        }

        @Override
        public Set<String> keySet()
        {
            return wrappedMap.keySet();
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m)
        {
            wrappedMap.putAll(m);
        }

        @Override
        public String remove(Object key)
        {
            return wrappedMap.remove(key);
        }

        @Override
        public int size()
        {
            return wrappedMap.size();
        }

        @Override
        public Collection<String> values()
        {
            return wrappedMap.values();
        }
    }

    /**
     * A map wrapper to support multi valued input fields
     *
     * @author Kemkes
     *
     */
    private class MetadataMultiValueMapWrapper implements Map<String, String[]>, Serializable
    {
        private static final long serialVersionUID = 1514209886446380743L;
        private static final String SPLITTER = ",\t";
        private Map<String, String> wrappedMap;

        public MetadataMultiValueMapWrapper(Map<String, String> wrappedMap)
        {
            this.wrappedMap = wrappedMap;
        }

        @Override
        public String[] get(Object key)
        {
            String value = wrappedMap.get(key);
            String[] result = (value == null || value.length() == 0) ? null : value.split(SPLITTER);
            return result;
        }

        @Override
        public String[] put(String key, String[] value)
        {
            wrappedMap.put(key, StringUtils.join(value, SPLITTER));

            return null;
        }

        @Override
        public void clear()
        {
            wrappedMap.clear();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return wrappedMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            return false;
        }

        @Override
        public Set<java.util.Map.Entry<String, String[]>> entrySet()
        {
            return null;
        }

        @Override
        public boolean isEmpty()
        {
            return wrappedMap.isEmpty();
        }

        @Override
        public Set<String> keySet()
        {
            return wrappedMap.keySet();
        }

        @Override
        public void putAll(Map<? extends String, ? extends String[]> m)
        {
        }

        @Override
        public String[] remove(Object key)
        {
            wrappedMap.remove(key);
            return null;
        }

        @Override
        public int size()
        {
            return wrappedMap.size();
        }

        @Override
        public Collection<String[]> values()
        {
            return null;
        }
    }

    //setter and getter for new resource columns mtype and msource
    public String getMtype()
    {
        return mtype;
    }

    public void setMtype(String mtype)
    {
        this.mtype = mtype;
    }

    public String getMsource()
    {
        return msource;
    }

    public void setMsource(String msource)
    {
        this.msource = msource;
    }

    //extended metadata setter and getter (chloe)
    public ExtendedMetadata getExtendedMetadata() throws SQLException
    {
        if(extendedMetadata == null)
        {
            extendedMetadata = Learnweb.getInstance().getExtendedMetadataManager().getMetadataByResourceId(id);
        }
        return extendedMetadata;

    }

    public void setExtendedMetadata(ExtendedMetadata extendedMetadata)
    {
        this.extendedMetadata = extendedMetadata;
    }

    //
    public String[] getSelectedMtypes()
    {
        return selectedMtypes;
    }

    /**
     * the setter will convert selectedMtypes to mtype
     *
     * @param selectedMtypes
     */
    public void setSelectedMtypes(String[] selectedMtypes)
    {
        String mt = "";
        this.selectedMtypes = selectedMtypes;
        for(int i = 0; i < selectedMtypes.length; i++)
        {
            if(i < selectedMtypes.length - 1)
            {
                mt += selectedMtypes[i] + ", ";
            }
            else
            {
                mt += selectedMtypes[i];
            }
        }
        this.mtype = mt;

        this.selectedMtypes = null; //once setting value for mtype, reset selectedMtypes
    }

    //new methods to add new metadata to given resource
    public void addNewLevels(String[] selectedLevels, User user) throws SQLException
    {
        ResourceManager rsm = Learnweb.getInstance().getResourceManager();
        rsm.saveLanglevelResource(this, selectedLevels, user);
        extendedMetadata = null; // invalidate cache
        selectedLevels = null; //invalidate cache
    }

    public void addNewTargets(String[] selectedTargets, User user) throws SQLException
    {
        ResourceManager rsm = Learnweb.getInstance().getResourceManager();
        rsm.saveTargetResource(this, selectedTargets, user);
        extendedMetadata = null; // invalidate cache
        selectedTargets = null; //invalidate cache
    }

    public void addNewPurposes(String[] selectedPurposes, User user) throws SQLException
    {
        ResourceManager rsm = Learnweb.getInstance().getResourceManager();
        rsm.savePurposeResource(this, selectedPurposes, user);
        extendedMetadata = null; // invalidate cache
        selectedPurposes = null; //invalidate cache
    }

    public void cloneComments(List<Comment> comments) throws SQLException
    {
        for(Comment comment : comments)
            addComment(comment.getText(), comment.getUser());
    }

    public void cloneTags(OwnerList<Tag, User> tags) throws SQLException
    {
        for(Tag tag : tags)
            addTag(tag.getName(), tags.getElementOwner(tag));
    }

    public void addNewCategory(String selectedTopcat, String selectedMidcat, String selectedBotcat, User user) throws SQLException
    {

        ResourceManager rsm = Learnweb.getInstance().getResourceManager();

        rsm.saveCategoryResource(this, selectedTopcat, selectedMidcat, selectedBotcat, user);

        extendedMetadata = null;

    }

    public String getLanguageOne()
    {
        return languageOne;
    }

    public void setLanguageOne(String languageOne)
    {
        this.languageOne = languageOne;
    }

    public String getLanguageTwo()
    {
        return languageTwo;
    }

    public void setLanguageTwo(String languageTwo)
    {
        this.languageTwo = languageTwo;
    }

}

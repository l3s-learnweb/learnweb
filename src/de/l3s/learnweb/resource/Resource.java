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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;
import de.l3s.util.StringHelper;

public class Resource extends AbstractResource implements Serializable, Cloneable {
    private static final long serialVersionUID = -8486919346993051937L;
    private static final Logger log = LogManager.getLogger(Resource.class);

    public static final char METADATA_SEPARATOR = ';';

    public static final int LEARNWEB_RESOURCE = 1;
    public static final int WEB_RESOURCE = 2;

    public enum OnlineStatus {
        UNKNOWN,
        ONLINE,
        OFFLINE,
        PROCESSING // e.g. while a document/video is converted
    }

    public enum ResourceViewRights {
        DEFAULT_RIGHTS, //inherits group rights
        SUBMISSION_READABLE, // the submitter of the resource (stored in the original resource id) and assessors can view the resource
        LEARNWEB_READABLE, //all learnweb users can view resource given url
        WORLD_READABLE, //all internet users with access to url can view resource
    }

    private int id = -1; // default id, that indicates that this resource is not stored at fedora
    private int groupId = 0;
    private int folderId = 0;
    private String title;
    private String description = "";
    private String url;
    private int storageType = WEB_RESOURCE;
    private ResourceViewRights rights = ResourceViewRights.DEFAULT_RIGHTS;
    private ResourceService source; // The place where the resource was found
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
    private Thumbnail thumbnail0; // 150 / 120
    private Thumbnail thumbnail1; // 150px
    private Thumbnail thumbnail2; // 300 / 220
    private Thumbnail thumbnail3; // 500 / 600
    private Thumbnail thumbnail4; // 1280 / 1024
    private String embeddedRaw; // stored in the database
    private String embeddedCode; // derived from type or embedded raw. Does not need to be stored in DB
    private String transcript; //To store the English transcripts for TED videos and saved articles
    private boolean readOnlyTranscript = false; //indicates resource transcript is read only for TED videos
    private OnlineStatus onlineStatus = OnlineStatus.UNKNOWN;
    private boolean restricted = false;
    private Date resourceTimestamp;
    private Date creationDate = new Date();
    private Map<String, String> metadata = new HashMap<>(); // field_name : field_value

    private boolean deleted = false; // indicates whether this resource has been deleted

    private int thumbUp = -1;
    private int thumbDown = -1;
    private final HashMap<Integer, Integer> thumbRateByUser = new HashMap<>(); // userId : direction, null if not rated
    private final HashMap<Integer, Integer> rateByUser = new HashMap<>(); // userId : rate, null if not rated
    private LinkedHashMap<Integer, File> files = new LinkedHashMap<>(); // resource_file_number : file

    // caches
    private transient OwnerList<Tag, User> tags;
    private transient List<Comment> comments;
    private transient User owner;
    private transient LinkedList<ArchiveUrl> archiveUrls; //To store the archived URLs
    private transient String path;
    private transient String prettyPath;
    private transient MetadataMapWrapper metadataWrapper; // includes static fields like title, description and author into the map
    private transient MetadataMultiValueMapWrapper metadataMultiValue;

    /**
     * Do nothing constructor.
     */
    public Resource() {
    }

    /**
     * Copy constructor.
     */
    public Resource(Resource old) {
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
        setResourceTimestamp(new Date());
        setCreationDate(new Date());
        setArchiveUrls(new LinkedList<>(old.getArchiveUrls()));
        setDeleted(old.deleted);
        setReadOnlyTranscript(old.readOnlyTranscript);
        // sets the originalResourceId to the id of the source resource
        if (old.originalResourceId == 0) {
            setOriginalResourceId(old.id);
        } else {
            setOriginalResourceId(old.originalResourceId);
        }

        setMetadata(new HashMap<>(old.getMetadata()));
    }

    /**
     * Called by the ResourceManager after all setters have been called.
     */
    protected void postConstruct() throws SQLException {
        setDefaultThumbnailIfNull();
    }

    /**
     * If no thumbnails have been assigned this method will create default thumbnails for the small thumbnails.
     */
    public void setDefaultThumbnailIfNull() throws SQLException {
        if (null == thumbnail0 || null == thumbnail1 || null == thumbnail2) {
            // TODO @astappiev: find better images.
            // the glossary icon is loaded in GlossaryResource.save()
            if (type == ResourceType.survey) {
                Resource iconResource = Learnweb.getInstance().getResourceManager().getResource(204095); // TODO avoid this, load from resource folder
                setThumbnail0(iconResource.getThumbnail0());
                setThumbnail1(iconResource.getThumbnail1());
                setThumbnail2(iconResource.getThumbnail2());
                setThumbnail3(iconResource.getThumbnail3());
                setThumbnail4(iconResource.getThumbnail4());
                return;
            }

            String serverUrl = Learnweb.getInstance().getServerUrl();
            Thumbnail dummyImage;

            switch (type) {
                case audio:
                case document:
                case image:
                case presentation:
                case spreadsheet:
                case text:
                case video:
                case website:
                    dummyImage = new Thumbnail(serverUrl + "/resources/default-thumbnails/" + type.name() + "-file.png", 128, 128);
                    break;
                default:
                    dummyImage = new Thumbnail(serverUrl + "/resources/default-thumbnails/grain.png", 200, 200);
            }

            if (null == thumbnail0) {
                setThumbnail0(dummyImage.resize(150, 120));
            }
            if (null == thumbnail1) {
                setThumbnail1(dummyImage.resize(150, 150));
            }
            if (null == thumbnail2) {
                setThumbnail2(dummyImage);
            }
        }
    }

    public void addTag(String tagName, User user) throws SQLException {
        if (tagName.length() > 250) {
            throw new IllegalArgumentException("tag is to long");
        }

        ResourceManager rsm = Learnweb.getInstance().getResourceManager();
        Tag tag = rsm.getTag(tagName);

        if (tag == null) {
            tag = rsm.addTag(tagName);
        }

        if (tags != null && !tags.contains(tag)) {
            rsm.tagResource(this, tag, user);

            if (null != tags) {
                tags.add(tag, user, new Date());
                Collections.sort(tags);
            }

            Learnweb.getInstance().getSolrClient().indexTag(tag, this);
        }
    }

    public void deleteTag(Tag tag) throws SQLException {
        Learnweb.getInstance().getResourceManager().deleteTag(tag, this);
        tags.remove(tag);

        Learnweb.getInstance().getSolrClient().deleteFromIndex(tag, this);
    }

    public void deleteComment(Comment comment) throws Exception {
        Learnweb.getInstance().getResourceManager().deleteComment(comment);
        comments.remove(comment);

        Learnweb.getInstance().getSolrClient().deleteFromIndex(comment);
    }

    public List<Comment> getComments() throws SQLException {
        if (id != -1 && comments == null) {
            comments = Learnweb.getInstance().getResourceManager().getCommentsByResourceId(id);
            //Collections.sort(comments);
        }

        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Comment addComment(String text, User user) throws SQLException {
        Comment comment = Learnweb.getInstance().getResourceManager().commentResource(text, user, this);

        getComments(); // make sure comments are loaded before adding a new one
        comments.add(0, comment);

        Learnweb.getInstance().getSolrClient().indexComment(comment);

        return comment;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    @Override
    public Group getGroup() throws SQLException {
        if (groupId == 0) {
            return null;
        }

        return Learnweb.getInstance().getGroupManager().getGroupById(groupId);
    }

    public void setGroup(Group group) {
        this.groupId = HasId.getIdOrDefault(group, 0);
    }

    @Override
    public int getUserId() {
        return ownerUserId;
    }

    @Override
    public void setUserId(int userId) {
        this.ownerUserId = userId;
        this.owner = null;
    }

    @Override
    public User getUser() throws SQLException {
        if (null == owner && -1 != ownerUserId) {
            owner = Learnweb.getInstance().getUserManager().getUser(ownerUserId);
        }
        return owner;
    }

    @Override
    public void setUser(User user) {
        this.owner = user;
        this.ownerUserId = owner.getId();
    }

    public Group getOriginalGroup() throws SQLException {
        if (originalResourceId == 0) {
            return null;
        }

        Resource originalResource = Learnweb.getInstance().getResourceManager().getResource(originalResourceId);
        if (originalResource != null) {
            return Learnweb.getInstance().getGroupManager().getGroupById(originalResource.getGroupId());
        } else {
            return null;
        }
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public Folder getFolder() throws SQLException {
        if (folderId == 0) {
            return null;
        }

        return Learnweb.getInstance().getGroupManager().getFolder(folderId);
    }

    public void setFolder(Folder folder) {
        this.folderId = HasId.getIdOrDefault(folder, 0);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = StringUtils.isNotEmpty(title) ? StringHelper.shortnString(Jsoup.clean(title, Whitelist.none()), 980) : "no title";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtils.isNotEmpty(description) ? (StringHelper.clean(description, Whitelist.simpleText())) : "";
    }

    public String getDescriptionHTML() {
        return description.replace("\n", "<br/>");
    }

    public int getStorageType() {
        return storageType;
    }

    public void setStorageType(int type) {
        if (type != LEARNWEB_RESOURCE && type != WEB_RESOURCE) {
            throw new IllegalArgumentException("Unknown storageType of the resource: " + id);
        }
        this.storageType = type;
    }

    public boolean isOfficeResource() {
        if (getSource() == ResourceService.slideshare) {
            return false;
        }

        return ResourceType.document == type || ResourceType.spreadsheet == type || ResourceType.presentation == type;
    }

    public boolean isOnline() {
        return OnlineStatus.ONLINE == onlineStatus;
    }

    public boolean isOffline() {
        return OnlineStatus.OFFLINE == onlineStatus;
    }

    public boolean isProcessing() {
        return OnlineStatus.PROCESSING == onlineStatus;
    }

    public int getRights() {
        return rights.ordinal();
    }

    public void setRights(int rights) {
        switch (rights) {
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
                break;
            default:
                log.error("Unknown rights value {}", rights);
        }
    }

    public void setRights(ResourceViewRights rights) {
        this.rights = rights;
    }

    /**
     * The location where the resource (metadata) is stored.
     *
     * @return for example Learnweb, Flickr, Youtube ...
     */
    public String getLocation() {
        return location;
    }

    /**
     * The location where the resource (metadata) is stored.
     *
     * @param location for example Learnweb, Flickr, Youtube ...
     */
    public void setLocation(String location) {
        this.location = location;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    /**
     * Set the mime type.
     *
     * @param format mime type
     */
    public void setFormat(String format) {
        this.format = format;
    }

    public double getStarRating() {
        return ratingSum == 0 ? 0.0 : (double) ratingSum / (double) rateNumber;
    }

    public int getStarRatingRounded() {
        return ratingSum == 0 ? 0 : getRatingSum() / getRateNumber();
    }

    public void setStarRatingRounded(int value) {
        // dummy method, is required by p:rating
    }

    public int getRateNumber() {
        return rateNumber;
    }

    public void setRateNumber(int rateNumber) {
        this.rateNumber = rateNumber;
    }

    public int getRatingSum() {
        return ratingSum;
    }

    public void setRatingSum(int rating) {
        this.ratingSum = rating;
    }

    /**
     * @return Returns a comma separated list of tags
     */
    public String getTagsAsString() throws SQLException {
        return getTagsAsString(", ");
    }

    public String getTagsAsString(String delim) throws SQLException {
        StringBuilder out = new StringBuilder();
        for (Tag tag : getTags()) {
            if (out.length() != 0) {
                out.append(delim);
            }
            out.append(tag.getName());
        }
        return out.toString();
    }

    public OwnerList<Tag, User> getTags() throws SQLException {
        if (null == tags || id != -1) {
            tags = Learnweb.getInstance().getResourceManager().getTagsByResource(id);
        }
        return tags;
    }

    public void setTags(OwnerList<Tag, User> tags) {
        this.tags = tags;
    }

    /**
     * Creates a copy of a resource.
     * Ratings and comments are not copied.
     * (Ids are set to default this the Object isn't persisted yet).
     */
    @Override
    public Resource clone() {
        return new Resource(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Resource) {
            return ((Resource) o).getId() == getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    /**
     * Rate this resource.
     *
     * @param value the rating 1-5
     * @param user the user who rates
     */
    public void rate(int value, User user) throws SQLException {
        Learnweb.getInstance().getResourceManager().rateResource(id, user.getId(), value);

        rateNumber++;
        ratingSum += value;

        rateByUser.put(user.getId(), value);
    }

    public Integer getRateByUser(int userId) {
        // get value from cache, or query database if absent
        return rateByUser.computeIfAbsent(userId, key -> Learnweb.getInstance().getResourceManager().getResourceRateByUser(id, key));
    }

    public boolean isRatedByUser(int userId) {
        Integer value = getRateByUser(userId);
        return value != null;
    }

    /**
     * Stores all made changes in the database and reindexes the resource at solr.
     */
    @Override
    public Resource save() throws SQLException {
        return Learnweb.getInstance().getResourceManager().saveResource(this);
    }

    public void setTypeFromFormat(String format) {
        if (StringUtils.isEmpty(format)) {
            log.error("Resource: " + getId() + "; Given format is empty: " + format, new Exception());
            return;
        }

        if (format.equals("text/html") || format.equals("application/xhtml+xml")) {
            this.type = ResourceType.website;
        } else if (format.startsWith("text/")) {
            this.type = ResourceType.text;
        } else if (format.startsWith("image/")) {
            this.type = ResourceType.image;
        } else if (format.startsWith("video/")) {
            this.type = ResourceType.video;
        } else if (format.startsWith("audio/")) {
            this.type = ResourceType.audio;
        } else if (format.equals("application/pdf")) {
            this.type = ResourceType.pdf;
        } else if (format.contains("ms-excel") || format.contains("spreadsheet")) {
            this.type = ResourceType.spreadsheet;
        } else if (format.contains("ms-powerpoint") || format.contains("presentation")) {
            this.type = ResourceType.presentation;
        } else if (format.contains("msword") || format.contains("ms-word") || format.contains("wordprocessing") || format.contains("opendocument.text") || format.equals("application/rtf")) {
            this.type = ResourceType.document;
        } else if (Arrays.asList("application/x-msdownload", "application/x-ms-dos-executable", "application/octet-stream", "application/x-gzip", "application/gzip", "application/x-rar-compressed", "application/zip", "application/x-shockwave-flash", "message/rfc822")
            .contains(format)) {
            // handle known types of downloadable resources
            this.type = ResourceType.file;
        } else {
            // if we do not know the format, then  log it and set it to downloadable
            log.error("Unknown type for format: " + format + "; resourceId: " + getId(), new Exception());
            this.type = ResourceType.file;
        }
    }

    public int getThumbUp() throws SQLException {
        if (thumbUp < 0) {
            Learnweb.getInstance().getResourceManager().loadThumbRatings(this);
        }
        return thumbUp;
    }

    public void setThumbUp(int thumbUp) {
        this.thumbUp = thumbUp;
    }

    public int getThumbDown() throws SQLException {
        if (thumbDown < 0) {
            Learnweb.getInstance().getResourceManager().loadThumbRatings(this);
        }
        return thumbDown;
    }

    public void setThumbDown(int thumbDown) {
        this.thumbDown = thumbDown;
    }

    public void thumbRate(User user, int direction) throws IllegalAccessError, SQLException {
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("Illegal value [" + direction + "] for direction. Valid values are 1 and -1");
        }

        if (isThumbRatedByUser(user.getId())) {
            throw new IllegalAccessError("You have already rated this resource");
        }

        if (direction == 1) {
            thumbUp++;
        } else {
            thumbDown++;
        }

        Learnweb.getInstance().getResourceManager().thumbRateResource(id, user.getId(), direction);

        thumbRateByUser.put(user.getId(), direction);
    }

    public Integer getThumbRateByUser(int userId) {
        // get value from cache, or query database if absent
        return thumbRateByUser.computeIfAbsent(userId, key -> Learnweb.getInstance().getResourceManager().getResourceThumbRateByUser(id, key));
    }

    public boolean isThumbRatedByUser(int userId) {
        Integer value = getThumbRateByUser(userId);
        return value != null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url != null && url.length() > 4000) {
            throw new IllegalArgumentException("url is too long: " + url.length() + "; " + url);
        }

        this.url = url;
    }

    public String getServiceIcon() {
        if (getId() != -1) { // is stored in Learnweb
            return "/resources/images/services/learnweb.png";
        }

        return "/resources/images/services/" + getLocation().toLowerCase() + ".png";
    }

    /**
     * Url to the best (high resolution) available preview image.
     * Only available for interweb search results + ResourceMetadataExtractor save thumbnail url to the field
     */
    public String getMaxImageUrl() {
        return maxImageUrl;
    }

    /**
     * Url to the best (high resolution) available preview image.
     * Only available for interweb search results
     */
    public void setMaxImageUrl(String imageUrl) {
        this.maxImageUrl = imageUrl;
    }

    public String getShortDescription() {
        return Jsoup.clean(StringHelper.shortnString(description, 200), Whitelist.simpleText());
    }

    /**
     * @return the file name of uploaded resource
     */
    public String getFileName() {
        if (fileName == null) {
            return "";
        }

        return fileName;
    }

    /**
     * @param fileName the file name of uploaded resource
     */
    public void setFileName(String fileName) {
        if (fileName != null && fileName.length() > 200) {
            throw new IllegalArgumentException("file name is too long: " + fileName.length() + "; " + fileName);
        }

        this.fileName = fileName;
    }

    /**
     * @return the query which was used to find this resource
     */
    public String getQuery() {
        if (query == null) {
            return "none";
        }
        return query;
    }

    /**
     * @param query the query which was used to find this resource
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return if the resource was copied from an older Learnweb resource this returns the id of the original resource <b>0</b> otherwise
     */
    public int getOriginalResourceId() {
        return originalResourceId;
    }

    /**
     * @param originalResourceId if the resource was copied from an older Learnweb resource this stores the id of the original resource
     */
    public void setOriginalResourceId(int originalResourceId) {
        this.originalResourceId = originalResourceId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * The place where the resource was found. Example: Flickr or Youtube or Desktop ...
     */
    public ResourceService getSource() {
        return source;
    }

    public void setSource(ResourceService source) {
        Validate.notNull(source);
        this.source = source;
    }

    /**
     * better use setSource(SERVICE source).
     */
    public void setSource(String source) {
        Validate.notEmpty(source);

        try {
            this.source = ResourceService.valueOf(source.toLowerCase().replace("-", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid source: " + source + " resource " + this);
        }
    }

    public LinkedHashMap<Integer, File> getFiles() {
        return files;
    }

    public void setFiles(LinkedHashMap<Integer, File> files) {
        this.files = files;
    }

    /**
     * This method does not persist the changes.
     * see: FileManager.addFileToResource(file, resource);
     */
    public void addFile(File file) throws SQLException {
        files.put(file.getType().ordinal(), file);

        if (id > 0 && file.getResourceId() != id) { // the resource is already stored, the new file needs to be added to the database
            file.setResourceId(id);
            Learnweb.getInstance().getFileManager().addFileToResource(file, this);
        }
    }

    public File getFile(File.TYPE fileType) {
        return files.get(fileType.ordinal());
    }

    /**
     * @return If the uploaded file was modified (e.g. a video or office document) we keep a copy of the original file
     */
    public File getOriginalFile() {
        return getFile(TYPE.FILE_ORIGINAL);
    }

    /**
     * @return If the uploaded file was modified (e.g. a video or office document) we keep a copy of the original file
     */
    public File getMainFile() {
        return getFile(TYPE.FILE_MAIN);
    }

    /**
     * @return Text that has been automatically extracted from the source file/url
     */
    public String getMachineDescription() {
        return machineDescription;
    }

    /**
     * @param machineDescription Text that has been automatically extracted from the source file/url
     */
    public void setMachineDescription(String machineDescription) {
        this.machineDescription = machineDescription;
    }

    public Thumbnail getThumbnail0() {
        return thumbnail0;
    }

    public void setThumbnail0(Thumbnail thumbnail0) {
        this.thumbnail0 = thumbnail0;
    }

    public Thumbnail getThumbnail1() {
        return thumbnail1;
    }

    public void setThumbnail1(Thumbnail thumbnail1) {
        this.thumbnail1 = thumbnail1;
    }

    public Thumbnail getThumbnail2() {
        return thumbnail2;
    }

    public void setThumbnail2(Thumbnail thumbnail2) {
        this.thumbnail2 = thumbnail2;
    }

    public Thumbnail getThumbnail3() {
        return thumbnail3;
    }

    public void setThumbnail3(Thumbnail thumbnail3) {
        this.thumbnail3 = thumbnail3;
    }

    public Thumbnail getThumbnail4() {
        return thumbnail4;
    }

    public void setThumbnail4(Thumbnail thumbnail4) {
        this.thumbnail4 = thumbnail4;
    }

    /**
     * Get combined thumbnail.
     */
    public Thumbnail getSmallThumbnail() {
        if (thumbnail0 != null) {
            return thumbnail0;
        }

        return thumbnail1;
    }

    /**
     * Get combined medium thumbnail.
     */
    public Thumbnail getMediumThumbnail() {
        if (thumbnail2 != null) {
            return thumbnail2;
        }

        return thumbnail3;
    }

    /**
     * Get combined large (largest) thumbnail.
     */
    public Thumbnail getLargestThumbnail() {
        if (null != thumbnail4) {
            return thumbnail4;
        }
        if (null != thumbnail3) {
            return thumbnail3;
        }
        if (null != thumbnail2) {
            return thumbnail2;
        }
        if (null != thumbnail1) {
            return thumbnail1;
        }
        if (null != thumbnail0) {
            return thumbnail0;
        }

        return null;
    }

    public String getEmbedded() {
        if (embeddedCode == null) {
            if (getType() == ResourceType.video) {
                if (isProcessing()) {
                    // return immediately, do not cache the temporal warning
                    return "<h3 style='padding: 2rem; color: red; position: absolute; width: 100%; box-sizing: border-box;'>We are converting this video. If your browser can't display it, try again in a few minutes.</h3>" + embeddedCode;
                }

                String iframeUrl = null;

                if (getSource() == ResourceService.ted) {
                    iframeUrl = getUrl().replace("http://www", "//embed").replace("https://www", "//embed");
                } else if (getSource() == ResourceService.youtube || getSource() == ResourceService.teded || getSource() == ResourceService.tedx) {
                    iframeUrl = "https://www.youtube-nocookie.com/embed/" + getIdAtService();
                } else if (getSource() == ResourceService.vimeo) {
                    iframeUrl = "https://player.vimeo.com/video/" + getIdAtService() + "?dnt=1";
                }

                if (null != iframeUrl) {
                    embeddedCode = "<iframe src=\"" + iframeUrl + "\" allowfullscreen referrerpolicy=\"origin\">Your browser has blocked this iframe</iframe>";
                }
            }

            // if no rule above works
            if (embeddedCode == null && StringUtils.isNoneEmpty(getEmbeddedRaw())) {
                // if the embedded code was explicitly defined then use it. Is necessary for Slideshare resources.
                embeddedCode = getEmbeddedRaw();
            }
        }

        return embeddedCode;
    }

    public int getDuration() {
        return duration;
    }

    /**
     * @param duration The duration in seconds
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDurationInMinutes() {
        return StringHelper.getDurationInMinutes(duration);
    }

    /**
     * Embedded code that can't be created on the fly. For example videos and slideshows
     * Normally you should not call this function.
     * Use getEmbedded() instead.
     */
    public String getEmbeddedRaw() {
        return embeddedRaw;
    }

    public void setEmbeddedRaw(String embeddedRaw) {
        this.embeddedRaw = embeddedRaw;
    }

    @Override
    public String toString() {
        return "Resource [id=" + id + ", title=" + title + ", url=" + url + ", storageType=" + storageType + ", source=" + source + ", type=" + type + ", format=" + format + ", date=" + getCreationDate() + "]";
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public OnlineStatus getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(OnlineStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getIdAtService() {
        return idAtService;
    }

    public void setIdAtService(String idAtService) {
        this.idAtService = idAtService;
    }

    public LinkedList<ArchiveUrl> getArchiveUrls() {
        if (id != -1 && archiveUrls == null) {
            ResourceManager rm = Learnweb.getInstance().getResourceManager();
            try {
                archiveUrls = rm.getArchiveUrlsByResourceId(id);
                archiveUrls.addAll(rm.getArchiveUrlsByResourceUrl(url));
            } catch (SQLException e) {
                log.error("Error while retrieving archive urls for resource: ", e);
            }
        }

        return archiveUrls;
    }

    public void setArchiveUrls(LinkedList<ArchiveUrl> archiveUrls) {
        this.archiveUrls = archiveUrls;
    }

    public HashMap<String, List<ArchiveUrl>> getArchiveUrlsAsYears() {
        HashMap<String, List<ArchiveUrl>> versions = new LinkedHashMap<>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy");
        for (ArchiveUrl a : archiveUrls) {
            String year = df.format(a.getTimestamp());
            if (!versions.containsKey(year)) {
                versions.put(year, new ArrayList<>());
            }
            versions.get(year).add(a);
        }
        return versions;
    }

    public void addArchiveUrl(ArchiveUrl archiveUrl) {
        archiveUrls = null;
    }

    public boolean isArchived() {
        return getArchiveUrls() != null && !archiveUrls.isEmpty();
    }

    public ArchiveUrl getFirstArchivedObject() {
        return archiveUrls.getFirst();
    }

    public ArchiveUrl getLastArchivedObject() {
        return archiveUrls.getLast();
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        if (fileUrl != null && fileUrl.length() > 4000) {
            throw new IllegalArgumentException("url is too long: " + fileUrl.length() + "; " + fileUrl);
        }

        this.fileUrl = fileUrl;
    }

    /**
     * @return comma separated list of language codes
     */
    public String getLanguage() {
        if (null == language) {
            return "";
        }
        return language;
    }

    /**
     * @param language comma separated list of language codes
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    public Date getResourceTimestamp() {
        return resourceTimestamp;
    }

    public void setResourceTimestamp(Date resourceTimestamp) {
        this.resourceTimestamp = resourceTimestamp;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isReadOnlyTranscript() {
        return readOnlyTranscript;
    }

    public void setReadOnlyTranscript(boolean readOnlyTranscript) {
        this.readOnlyTranscript = readOnlyTranscript;
    }

    @Override
    public void moveTo(int newGroupId, int newFolderId) throws SQLException {
        Learnweb.getInstance().getGroupManager().moveResource(this, newGroupId, newFolderId);
    }

    @Override
    public void delete() throws SQLException {
        setDeleted(true);
        this.save();
    }

    public void deleteHard() throws SQLException {
        Learnweb.getInstance().getResourceManager().deleteResourceHard(getId());
    }

    /**
     * returns a string representation of the resources path.
     */
    @Override
    public String getPath() throws SQLException {
        if (null == path) {
            Folder folder = getFolder();
            if (folder != null) {
                path = folder.getPath();
            }
        }
        return path;
    }

    /**
     * returns a string representation of the resources path.
     */
    @Override
    public String getPrettyPath() throws SQLException {
        if (null == prettyPath) {
            Folder folder = getFolder();
            if (folder != null) {
                prettyPath = folder.getPrettyPath();
            }
        }
        return prettyPath;
    }

    protected void clearCaches() {
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
    public boolean canViewResource(User user) throws SQLException {
        if (isDeleted()) {
            return false;
        }

        //admins, moderators and resource owners can always view the resource
        if (user != null && (user.isModerator() || getUserId() == user.getId())) {
            return true;
        }

        switch (rights) {
            case WORLD_READABLE:
                return true;
            case LEARNWEB_READABLE:
                return user != null;
            case SUBMISSION_READABLE: // the submitter of the resource (stored in the original resource id) can view the resource
                Resource originalResource = Learnweb.getInstance().getResourceManager().getResource(originalResourceId);
                return originalResource != null && originalResource.getUserId() == user.getId(); // the submitter can view his resource
            case DEFAULT_RIGHTS: // if the resource is part of the group the group permissions are used
                Group group = getGroup();
                if (group != null) {
                    return group.canViewResources(user);
                }
                return false;
            default:
                return false;
        }
    }

    public boolean canModerateResource(User user) {
        if (user == null || isDeleted()) {
            return false;
        }

        if (user.isAdmin()) {
            return true;
        }

        if (user.isModerator()) {
            try {
                if (getGroupId() == 0) { // check permission for a private resource
                    return user.canModerateUser(getUser());
                } else { // check group access permissions
                    return getGroup().getCourse().isModerator(user);
                }
            } catch (SQLException e) {
                log.error("user " + user + " can not moderate resource " + this);
            }
        }
        return false;
    }

    public boolean canAnnotateResource(User user) throws SQLException {
        if (user == null || isDeleted()) {
            return false;
        }

        Group group = getGroup();

        if (group != null) {
            return group.canAnnotateResources(user);
        }

        if (user.isAdmin() || ownerUserId == user.getId()) {
            return true;
        }

        return false;
    }

    public LogEntry getThumbnailUpdateInfo() throws SQLException {
        /*
         This method returned a LogEntry for the following query:
         select * FROM lw_user_log WHERE action=45 AND target_id = ? ORDER BY timestamp DESC LIMIT 1
         setInt(1, getResource_id)
         But the implementation was bad and it was very rarely used. Let's see if someone misses it. - @kemkes 02.04.2020
         */
        return null;
    }

    /**
     * @return the previous value associated with key, or null if there was no mapping for key. (A null return can also indicate that the map
     * previously associated null with key.)
     */
    public String setMetadataValue(String key, String value) {
        if (key == null || value == null) {
            log.warn("Invalid arguments: key=" + key + "; value=" + value + "; Metadata not added");
            return null;
        }
        return metadata.put(key, value.replace(METADATA_SEPARATOR, ','));
    }

    public Set<String> getMetadataKeys() {
        return metadata.keySet();
    }

    public Set<Entry<String, String>> getMetadataEntries() {
        return metadata.entrySet();
    }

    public String getMetadataValue(String key) {
        return metadata.get(key);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadataObj) {
        if (metadataObj instanceof HashMap<?, ?>) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> hashMap = (HashMap<String, String>) metadataObj;
            metadata = hashMap;
        } else {
            metadata = new HashMap<>();
            log.error("resource = " + getId() + " unknown metadata format: " + metadataObj.getClass().getName());
        }

        //clear wrapper
        metadataWrapper = null;
        metadataMultiValue = null;
    }

    public MetadataMapWrapper getMetadataWrapper() {
        if (null == metadataWrapper) {
            metadataWrapper = new MetadataMapWrapper(metadata);
        }
        return metadataWrapper;
    }

    public MetadataMultiValueMapWrapper getMetadataMultiValue() {
        if (null == metadataMultiValue) {
            metadataMultiValue = new MetadataMultiValueMapWrapper(getMetadataWrapper());
        }
        return metadataMultiValue;
    }

    public void cloneComments(List<Comment> comments) throws SQLException {
        for (Comment comment : comments) {
            addComment(comment.getText(), comment.getUser());
        }
    }

    public void cloneTags(OwnerList<Tag, User> tags) throws SQLException {
        for (Tag tag : tags) {
            addTag(tag.getName(), tags.getElementOwner(tag));
        }
    }

    public List<LogEntry> getLogs() throws SQLException {
        return Learnweb.getInstance().getLogManager().getLogsByResource(this);
    }

    /**
     * Is called when a Resource object is deserialized.
     * To make sure that there exists only one instance of each resource we interfere the deserialization process
     * if there exists a cached version of the resource we will return this instance
     */
    protected Object readResolve() {
        log.debug("Deserialize resource: " + id);
        try {
            if (Learnweb.getInstanceOptional().isPresent()) {
                return Learnweb.getInstanceOptional().get().getResourceManager().getResource(id);
            }
        } catch (RuntimeException e) {
            if (!e.getMessage().startsWith("Learnweb is not initialized correctly.")) { // ignore this error
                log.fatal("Can't load resource:  " + id, e);
            }
        } catch (Exception e) {
            log.fatal("Can't load resource:  " + id, e);
        }

        return this;
    }

    public static Comparator<Resource> createIdComparator() {
        return Comparator.comparingInt(Resource::getId);
    }

    public static Comparator<Resource> createTitleComparator() {
        return Comparator.comparing(Resource::getTitle);
    }

    public static Comparator<Resource> createTypeComparator() {
        return Comparator.comparing(Resource::getType);
    }

    /**
     * A map wrapper to support multi valued input fields.
     *
     * @author Philipp Kemkes
     */
    public static final class MetadataMultiValueMapWrapper implements Map<String, String[]>, Serializable {
        private static final long serialVersionUID = 1514209886446380743L;
        private final Map<String, String> wrappedMap;

        private MetadataMultiValueMapWrapper(Map<String, String> wrappedMap) {
            this.wrappedMap = wrappedMap;
        }

        @Override
        public String[] get(Object key) {
            return StringUtils.split(wrappedMap.get(key), METADATA_SEPARATOR);
        }

        @Override
        public String[] put(String key, String[] value) {
            wrappedMap.put(key, StringUtils.join(StringHelper.remove(value, METADATA_SEPARATOR), METADATA_SEPARATOR));
            return null;
        }

        @Override
        public void clear() {
            wrappedMap.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            return wrappedMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Set<java.util.Map.Entry<String, String[]>> entrySet() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return wrappedMap.isEmpty();
        }

        @Override
        public Set<String> keySet() {
            return wrappedMap.keySet();
        }

        @Override
        public void putAll(Map<? extends String, ? extends String[]> m) {
        }

        @Override
        public String[] remove(Object key) {
            wrappedMap.remove(key);
            return null;
        }

        @Override
        public int size() {
            return wrappedMap.size();
        }

        @Override
        public Collection<String[]> values() {
            return null;
        }
    }

    /**
     * A map wrapper to add the hard coded metadata fields (title, author, desc and language) to the metadata map.
     *
     * @author Philipp Kemkes
     */
    public class MetadataMapWrapper implements Map<String, String>, Serializable {
        private static final long serialVersionUID = -7357288281713761896L;
        private final Map<String, String> wrappedMap;

        public MetadataMapWrapper(Map<String, String> wrappedMap) {
            this.wrappedMap = wrappedMap;
        }

        public Map<String, String> getWrappedMap() {
            return wrappedMap;
        }

        @Override
        public String get(Object key) {
            // TODO @kemkes: why the type of key can't be changed to String?
            if (!key.getClass().equals(String.class)) {
                throw new IllegalArgumentException("key must be a string");
            }

            String keyString = ((String) key).toLowerCase();

            switch (keyString) {
                case "title":
                    return getTitle();
                case "author":
                    return getAuthor();
                case "description":
                    return getDescription();
                case "language":
                    return getLanguage();
                default:
                    return wrappedMap.get(key);
            }
        }

        @Override
        public String put(String key, String value) {
            switch (key) {
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
                default:
                    return wrappedMap.put(key, value);
            }
        }

        @Override
        public void clear() {
            wrappedMap.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            return wrappedMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Set<java.util.Map.Entry<String, String>> entrySet() {
            return wrappedMap.entrySet();
        }

        @Override
        public boolean isEmpty() {
            return wrappedMap.isEmpty();
        }

        @Override
        public Set<String> keySet() {
            return wrappedMap.keySet();
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            wrappedMap.putAll(m);
        }

        @Override
        public String remove(Object key) {
            return wrappedMap.remove(key);
        }

        @Override
        public int size() {
            return wrappedMap.size();
        }

        @Override
        public Collection<String> values() {
            return wrappedMap.values();
        }
    }

}

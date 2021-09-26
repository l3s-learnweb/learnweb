package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.File.FileType;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.user.User;
import de.l3s.util.Expirable;
import de.l3s.util.HasId;
import de.l3s.util.StringHelper;

public class Resource extends AbstractResource implements Serializable {
    @Serial
    private static final long serialVersionUID = -8486919346993051937L;
    private static final Logger log = LogManager.getLogger(Resource.class);

    public static final char METADATA_SEPARATOR = ';';

    public enum StorageType {
        LEARNWEB,
        WEB
    }

    public enum OnlineStatus {
        UNKNOWN,
        ONLINE,
        OFFLINE,
        PROCESSING // e.g. while a document/video is converted
    }

    public enum DefaultTab {
        SCREENSHOT,
        LIVE,
        ARCHIVED
    }

    /**
     * Who can view the resource?
     */
    public enum PolicyView { // be careful when adding options. The new option must be added to the lw_resource table too
        DEFAULT_RIGHTS, // inherits group rights
        SUBMISSION_READABLE, // the submitter of the resource (stored in the original resource id) and assessors can view the resource
        LEARNWEB_READABLE, // all learnweb users can view resource given url
        WORLD_READABLE, // all internet users with access to url can view resource
    }

    private int id; // default id, that indicates that this resource is not stored at fedora
    private boolean deleted = false; // indicates whether this resource has been deleted
    private int groupId;
    private int folderId;
    private int ownerUserId;
    private String title;
    private String description;
    private String url; // `website` resources stores external link here, also `video` resources stores link to source (like YouTube page)
    private StorageType storageType = StorageType.LEARNWEB;
    private PolicyView policyView = PolicyView.DEFAULT_RIGHTS;
    private ResourceService service; // The place where the resource was found TODO: what is better naming `source` vs `service`?
    private String language; // language code
    private String author;
    private ResourceType type;
    private String format; // original mimeType of the resource
    private int duration;
    private int width;
    private int height;
    private String idAtService;
    private int ratingSum;
    private int rateNumber;
    private String maxImageUrl; // an url to the largest image preview of this resource
    private String query; // the query which was used to find this resource
    private String embeddedUrl; // stored in the database
    private String embeddedCode; // derived from type or embedded raw. Does not need to be stored in DB
    private String transcript; // To store the English transcripts for TED videos and saved articles
    private boolean readOnlyTranscript = false; // indicates resource transcript is read only for TED videos
    private OnlineStatus onlineStatus = OnlineStatus.UNKNOWN;
    private int originalResourceId; // if the resource was copied from an existing Learnweb resource this field stores the id of the original resource
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private String machineDescription;
    private HashMap<String, String> metadata = new HashMap<>(); // field_name : field_value
    private DefaultTab defaultTab = DefaultTab.SCREENSHOT;

    // rating
    private int thumbUp = -1;
    private int thumbDown = -1;
    private final HashMap<Integer, Integer> thumbRateByUser = new HashMap<>(); // userId : direction, null if not rated
    private final HashMap<Integer, Integer> rateByUser = new HashMap<>(); // userId : rate, null if not rated
    private EnumMap<FileType, File> files; // type : file
    private final HashSet<File> addedFiles = new HashSet<>(); // files that were added to the resource since last save
    private final HashSet<File> deletedFiles = new HashSet<>(); // files that were deleted from the resource since last save

    // caches
    private transient String thumbnailSmall; // cropped to 160 x 120 px - smallest thumbnail used on website
    private transient String thumbnailMedium; // resized <= 280 x 210 px - resource preview image size
    private transient String thumbnailLarge; // resized <= 2048 x 1536 px - FHD image size, used on resource page if other media type is not available
    private transient OwnerList<Tag, User> tags;
    private transient List<Comment> comments;
    private transient User owner;
    private transient LinkedList<ArchiveUrl> archiveUrls; //To store the archived URLs
    private transient String path;
    private transient String prettyPath;
    private transient MetadataMapWrapper metadataWrapper; // includes static fields like title, description and author into the map
    private transient MetadataMultiValueMapWrapper metadataMultiValue;
    private transient Expirable<List<LogEntry>> logs;

    /**
     * Do nothing constructor.
     */
    public Resource() {
    }

    public Resource(StorageType storageType, ResourceType type, ResourceService service) {
        this.storageType = storageType;
        this.type = type;
        this.service = service;
    }

    /**
     * Copy constructor.
     */
    public Resource(Resource old) {
        setGroupId(old.groupId);
        setFolderId(old.folderId);
        setTitle(old.title);
        setDescription(old.description);
        setUrl(old.url);
        setStorageType(old.storageType);
        setPolicyView(old.policyView);
        setService(old.service);
        setAuthor(old.author);
        setType(old.type);
        setFormat(old.format);
        setUserId(old.ownerUserId);
        setMaxImageUrl(old.maxImageUrl);
        setQuery(old.query);
        setEmbeddedUrl(old.embeddedUrl);
        setDuration(old.duration);
        setWidth(old.width);
        setHeight(old.height);
        setMachineDescription(old.machineDescription);
        setTranscript(old.transcript);
        setOnlineStatus(old.onlineStatus);
        setIdAtService(old.idAtService);
        setUpdatedAt(LocalDateTime.now());
        setCreatedAt(LocalDateTime.now());
        if (old.getArchiveUrls() != null) setArchiveUrls(new LinkedList<>(old.getArchiveUrls()));
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
     * Creates appropriate Resource instances based on the resource type.
     * Necessary since some resource types extend the normal Resource class.
     */
    public static Resource ofType(String storageType, String type, String source) {
        ResourceType resourceType = ResourceType.valueOf(type);

        return switch (resourceType) {
            case survey -> new SurveyResource();
            case glossary -> new GlossaryResource();
            default -> new Resource(StorageType.valueOf(storageType), resourceType, ResourceService.valueOf(source));
        };
    }

    /**
     * Called by the ResourceManager after all setters have been called.
     */
    protected void postConstruct() {
    }

    public DefaultTab getDefaultTab() {
        return defaultTab;
    }

    public DefaultTab[] getDefaultTabs() {
        return DefaultTab.values();
    }

    public void setDefaultTab(DefaultTab tab) {
        this.defaultTab = tab;
    }

    public void addTag(String tagName, User user) {
        if (tagName.length() > 250) {
            throw new IllegalArgumentException("tag is to long");
        }

        Tag tag = Learnweb.dao().getTagDao().findByName(tagName).orElseGet(() -> {
            Tag newTag = new Tag(0, tagName);
            Learnweb.dao().getTagDao().save(newTag);
            return newTag;
        });

        if (tags != null && !tags.contains(tag)) {
            Learnweb.dao().getResourceDao().insertTag(this, user, tag);

            if (null != tags) {
                tags.add(tag, user, LocalDateTime.now());
                Collections.sort(tags);
            }

            Learnweb.getInstance().getSolrClient().reIndexResource(this);
        }
    }

    public void deleteTag(Tag tag) {
        Learnweb.dao().getResourceDao().deleteTag(this, tag);
        tags.remove(tag);

        Learnweb.getInstance().getSolrClient().reIndexResource(this);
    }

    public void deleteComment(Comment comment) {
        comments.remove(comment);
        Learnweb.dao().getCommentDao().delete(comment);
        Learnweb.getInstance().getSolrClient().reIndexResource(this);
    }

    public List<Comment> getComments() {
        if (comments == null && id != 0) {
            comments = Learnweb.dao().getCommentDao().findByResourceId(id);
        }

        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Comment addComment(String text, User user) {
        Comment comment = new Comment(text, this, user);
        Learnweb.dao().getCommentDao().save(comment);

        getComments(); // make sure comments are loaded before adding a new one
        comments.add(0, comment);

        Learnweb.getInstance().getSolrClient().reIndexResource(this);
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
    public Group getGroup() {
        if (groupId == 0) {
            return null;
        }

        return Learnweb.dao().getGroupDao().findByIdOrElseThrow(groupId);
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
    public User getUser() {
        if (null == owner && ownerUserId != 0) {
            owner = Learnweb.dao().getUserDao().findByIdOrElseThrow(ownerUserId);
        }
        return owner;
    }

    @Override
    public void setUser(User user) {
        this.owner = user;
        this.ownerUserId = owner.getId();
    }

    public Group getOriginalGroup() {
        if (originalResourceId == 0) {
            return null;
        }

        return Learnweb.dao().getResourceDao().findById(originalResourceId).map(Resource::getGroup).orElse(null);
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public Folder getFolder() {
        if (folderId == 0) {
            return null;
        }

        return Learnweb.dao().getFolderDao().findByIdOrElseThrow(folderId);
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
        this.title = StringUtils.isNotEmpty(title) ? StringHelper.shortnString(Jsoup.clean(title, Safelist.none()), 980) : "no title";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtils.isNotEmpty(description) ? (StringHelper.clean(description, Safelist.simpleText())) : "";
    }

    public String getDescriptionHTML() {
        return description.replace("\n", "<br/>");
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public boolean isWebResource() {
        return storageType == StorageType.WEB;
    }

    public boolean isOfficeResource() {
        if (service == ResourceService.slideshare) {
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

    public PolicyView getPolicyView() {
        return policyView;
    }

    public void setPolicyView(PolicyView policyView) {
        this.policyView = policyView;
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
    public String getTagsAsString() {
        return getTagsAsString(", ");
    }

    public String getTagsAsString(String delimiter) {
        StringBuilder out = new StringBuilder();
        for (Tag tag : getTags()) {
            if (!out.isEmpty()) {
                out.append(delimiter);
            }
            out.append(tag.getName());
        }
        return out.toString();
    }

    public OwnerList<Tag, User> getTags() {
        if (tags == null && id != 0) {
            tags = Learnweb.dao().getTagDao().findByResourceId(id);
        }
        return tags;
    }

    public void setTags(OwnerList<Tag, User> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Resource) {
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
    public void rate(int value, User user) {
        Learnweb.dao().getResourceDao().insertResourceRating(id, user.getId(), value);

        rateNumber++;
        ratingSum += value;

        rateByUser.put(user.getId(), value);
    }

    public Integer getRateByUser(int userId) {
        // get value from cache, or query database if absent
        return rateByUser.computeIfAbsent(userId, key -> Learnweb.dao().getResourceDao().findResourceRating(id, key).orElse(null));
    }

    public boolean isRatedByUser(int userId) {
        Integer value = getRateByUser(userId);
        return value != null;
    }

    /**
     * Stores all made changes in the database and reindexes the resource at solr.
     */
    @Override
    public Resource save() {
        Learnweb.dao().getResourceDao().save(this);

        // TODO @astappiev: this has to be moved to the save method of WebResource.class, which has to be created
        if (CollectionUtils.isNotEmpty(getArchiveUrls())) {
            try {
                // To copy archive versions of a resource if it exists
                Learnweb.dao().getArchiveUrlDao().insertArchiveUrl(getId(), getArchiveUrls());
            } catch (Exception e) {
                log.error("Can't save archiveUrls", e);
            }
        }

        return this;
    }

    public void setTypeFromFormat(String format) {
        if (StringUtils.isEmpty(format)) {
            log.error("Resource: {}; Given format is empty: {}", getId(), format, new Exception());
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
        } else if (StringUtils.containsAny(format, "ms-excel", "spreadsheet")) {
            this.type = ResourceType.spreadsheet;
        } else if (StringUtils.containsAny(format, "ms-powerpoint", "presentation")) {
            this.type = ResourceType.presentation;
        } else if (StringUtils.containsAny(format, "msword", "ms-word", "wordprocessing", "opendocument.text", "application/rtf")) {
            this.type = ResourceType.document;
        } else if (StringUtils.equalsAny(format, "application/x-msdownload", "application/x-ms-dos-executable", "application/octet-stream",
            "application/x-gzip", "application/gzip", "application/x-rar-compressed", "application/zip", "application/x-shockwave-flash", "message/rfc822")) {
            // handle known types of downloadable resources
            this.type = ResourceType.file;
        } else {
            // if we do not know the format, then  log it and set it to downloadable
            log.error("Unknown type for format: {}; resourceId: {}", format, getId(), new Exception());
            this.type = ResourceType.file;
        }
    }

    public int getThumbUp() {
        if (thumbUp < 0) {
            Learnweb.dao().getResourceDao().findThumbRatings(this).ifPresent(pair -> {
                setThumbUp(pair.left);
                setThumbDown(pair.right);
            });
        }
        return thumbUp;
    }

    public void setThumbUp(int thumbUp) {
        this.thumbUp = thumbUp;
    }

    public int getThumbDown() {
        if (thumbDown < 0) {
            Learnweb.dao().getResourceDao().findThumbRatings(this).ifPresent(pair -> {
                setThumbUp(pair.left);
                setThumbDown(pair.right);
            });
        }
        return thumbDown;
    }

    public void setThumbDown(int thumbDown) {
        this.thumbDown = thumbDown;
    }

    public void thumbRate(User user, int direction) throws IllegalAccessError {
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("Illegal value [" + direction + "] for direction. Valid values are 1 and -1");
        }

        if (isThumbRatedByUser(user)) {
            throw new IllegalAccessError("You have already rated this resource");
        }

        if (direction == 1) {
            thumbUp++;
        } else {
            thumbDown++;
        }

        Learnweb.dao().getResourceDao().insertThumbRate(this, user, direction);

        thumbRateByUser.put(user.getId(), direction);
    }

    public Integer getThumbRateByUser(User user) {
        // get value from cache, or query database if absent
        return thumbRateByUser.computeIfAbsent(user.getId(), key -> Learnweb.dao().getResourceDao().findThumbRate(this, user).orElse(null));
    }

    public boolean isThumbRatedByUser(User user) {
        Integer value = getThumbRateByUser(user);
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
        if (id != 0 || service == null) { // is stored in Learnweb
            return "/resources/images/services/learnweb.png";
        }

        return "/resources/images/services/" + service.name() + ".png";
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
        return Jsoup.clean(StringHelper.shortnString(description, 200), Safelist.simpleText());
    }

    /**
     * @return the query which was used to find this resource
     */
    public String getQuery() {
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
    public ResourceService getService() {
        return service;
    }

    public void setService(ResourceService service) {
        this.service = service;
    }

    public EnumMap<FileType, File> getFiles() {
        if (files == null) {
            files = new EnumMap<>(FileType.class);

            if (id != 0) {
                List<File> loadedFiles = Learnweb.dao().getFileDao().findByResourceId(id);

                for (File file : loadedFiles) {
                    files.put(file.getType(), file);
                }
            }
        }

        return files;
    }

    public Collection<File> getAddedFiles() {
        return addedFiles;
    }

    public Collection<File> getDeletedFiles() {
        return deletedFiles;
    }

    /**
     * This method does not persist the changes immediately. You should call `resource.save()` to do so.
     */
    public void deleteFile(FileType fileType) {
        if (getFiles().containsKey(fileType)) {
            deletedFiles.add(getFiles().get(fileType));
            getFiles().remove(fileType);
        }
    }

    /**
     * This method does not persist the changes immediately. You should call `resource.save()` to do so.
     */
    public void addFile(File file) {
        if (file != null) {
            deleteFile(file.getType());
            addedFiles.add(file);
            getFiles().put(file.getType(), file);

            // clear caches
            if (file.getType() == FileType.THUMBNAIL_SMALL) {
                thumbnailSmall = null;
            } else if (file.getType() == FileType.THUMBNAIL_MEDIUM) {
                thumbnailMedium = null;
            } else if (file.getType() == FileType.THUMBNAIL_LARGE) {
                thumbnailLarge = null;
            }
        }
    }

    public File getFile(FileType fileType) {
        return getFiles().get(fileType);
    }

    /**
     * @return If the uploaded file was modified (e.g. a video or office document) we keep a copy of the original file
     */
    public File getOriginalFile() {
        return getFile(FileType.ORIGINAL);
    }

    /**
     * @return If the uploaded file was modified (e.g. a video or office document) we keep a copy of the original file
     */
    public File getMainFile() {
        return getFile(FileType.MAIN);
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

    public String getThumbnailSmall() {
        if (thumbnailSmall == null && getFile(FileType.THUMBNAIL_SMALL) != null) {
            if (id == 0) { // when resource is uploaded, but not yet saved it has no id
                thumbnailSmall = getFile(FileType.THUMBNAIL_SMALL).getSimpleUrl();
            } else {
                thumbnailSmall = getFile(FileType.THUMBNAIL_SMALL).getResourceUrl(id);
            }
        }
        return thumbnailSmall;
    }

    public void setThumbnailSmall(String thumbnailSmall) {
        this.thumbnailSmall = thumbnailSmall;
    }

    public String getThumbnailMedium() {
        if (thumbnailMedium == null && getFile(FileType.THUMBNAIL_MEDIUM) != null) {
            thumbnailMedium = getFile(FileType.THUMBNAIL_MEDIUM).getResourceUrl(id);
        }
        return thumbnailMedium;
    }

    public void setThumbnailMedium(String thumbnailMedium) {
        this.thumbnailMedium = thumbnailMedium;
    }

    public String getThumbnailLarge() {
        if (thumbnailLarge == null && getFile(FileType.THUMBNAIL_LARGE) != null) {
            thumbnailLarge = getFile(FileType.THUMBNAIL_LARGE).getResourceUrl(id);
        }
        return thumbnailLarge;
    }

    public void setThumbnailLarge(String thumbnailLarge) {
        this.thumbnailLarge = thumbnailLarge;
    }

    /**
     * Get combined largest thumbnail.
     */
    public String getThumbnailLargest() {
        return ObjectUtils.firstNonNull(getThumbnailLarge(), getThumbnailMedium(), getThumbnailSmall());
    }

    public void copyThumbnails(Resource resource) {
        addFile(resource.getFile(FileType.THUMBNAIL_SMALL));
        addFile(resource.getFile(FileType.THUMBNAIL_MEDIUM));
        addFile(resource.getFile(FileType.THUMBNAIL_LARGE));
    }

    public String getEmbeddedCode() {
        if (embeddedCode == null) {
            if (isProcessing()) {
                // return immediately, do not cache the temporal warning
                return "<h3 class=\"processing\">We are processing this resource. If your browser can't display it, try again in a few minutes.</h3>";
            }

            String iframeUrl = null;
            if (embeddedUrl != null) {
                iframeUrl = embeddedUrl;
            } else if (type == ResourceType.video) {
                iframeUrl = switch (service) {
                    case ted ->  getUrl().replace("//www.", "//embed.").replace("http://", "https://");
                    case youtube, teded, tedx -> "https://www.youtube-nocookie.com/embed/" + getIdAtService();
                    case vimeo -> "https://player.vimeo.com/video/" + getIdAtService() + "?dnt=1";
                    default -> null;
                };
            }

            if (null != iframeUrl) {
                embeddedCode = "<iframe src=\"" + iframeUrl + "\" allowfullscreen referrerpolicy=\"origin\">Your browser has blocked this iframe</iframe>";
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

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    /**
     * Embedded code that can't be created on the fly. For example videos and slideshows
     * Normally you should not call this function.
     * Use getEmbedded() instead.
     */
    public String getEmbeddedUrl() {
        return embeddedUrl;
    }

    public void setEmbeddedUrl(String embeddedUrl) {
        this.embeddedUrl = embeddedUrl;
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
        if (archiveUrls == null && id != 0) {
            archiveUrls = new LinkedList<>(Learnweb.dao().getArchiveUrlDao().findByResourceId(id));
            archiveUrls.addAll(Learnweb.dao().getWaybackUrlDao().findByUrl(url));
        }

        return archiveUrls;
    }

    public void setArchiveUrls(LinkedList<ArchiveUrl> archiveUrls) {
        this.archiveUrls = archiveUrls;
    }

    public HashMap<Integer, List<ArchiveUrl>> getArchiveUrlsAsYears() {
        HashMap<Integer, List<ArchiveUrl>> versions = new LinkedHashMap<>();
        for (ArchiveUrl url : archiveUrls) {
            int year = url.timestamp().getYear();
            if (!versions.containsKey(year)) {
                versions.put(year, new ArrayList<>());
            }
            versions.get(year).add(url);
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

    public String getDownloadUrl() {
        if (service == ResourceService.speechrepository) {
            return embeddedUrl;
        }

        File mainFile = getMainFile();
        if (mainFile != null) {
            return mainFile.getResourceUrl(id);
        }

        return null;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
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
    public void moveTo(int newGroupId, int newFolderId) {
        if (getGroupId() == newGroupId && getFolderId() == newFolderId) {
            return; // if move to the same folder
        }

        setGroupId(newGroupId);
        setFolderId(newFolderId);
        save();
    }

    @Override
    public void delete() {
        Learnweb.dao().getResourceDao().deleteSoft(this);
    }

    /**
     * Returns a string representation of the resources path.
     */
    @Override
    public String getPath() {
        if (null == path) {
            Folder folder = getFolder();
            if (folder != null) {
                path = folder.getPath();
            }
        }
        return path;
    }

    /**
     * Returns a string representation of the resources path.
     */
    @Override
    public String getPrettyPath() {
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
    public boolean canViewResource(User user) {
        if (isDeleted()) {
            return false;
        }

        //admins, moderators and resource owners can always view the resource
        if (user != null && (user.isModerator() || getUserId() == user.getId())) {
            return true;
        }

        switch (policyView) {
            case WORLD_READABLE:
                return true;
            case LEARNWEB_READABLE:
                return user != null;
            case SUBMISSION_READABLE: // the submitter of the resource (stored in the original resource id) can view the resource
                return Learnweb.dao().getResourceDao().findById(originalResourceId).map(resource -> resource.getUserId() == user.getId()).orElse(false);
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
            if (getGroupId() == 0) { // check permission for a private resource
                return user.canModerateUser(getUser());
            } else { // check group access permissions
                return getGroup().getCourse().isModerator(user);
            }
        }
        return false;
    }

    public boolean canAnnotateResource(User user) {
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

    /**
     * @return the previous value associated with key, or null if there was no mapping for key. (A null return can also indicate that the map
     * previously associated null with key.)
     */
    public String setMetadataValue(String key, String value) {
        if (key == null || value == null) {
            log.warn("Invalid arguments: key={}; value={}; Metadata not added", key, value);
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

    public HashMap<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(HashMap<String, String> metadata) {
        this.metadata = metadata;

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

    public void copyComments(List<Comment> comments) {
        for (Comment comment : comments) {
            addComment(comment.getText(), comment.getUser());
        }
    }

    public void copyTags(OwnerList<Tag, User> tags) {
        for (Tag tag : tags) {
            addTag(tag.getName(), tags.getElementOwner(tag));
        }
    }

    public List<LogEntry> getLogs() {
        if (logs == null) {
            logs = new Expirable<>(Duration.of(10, ChronoUnit.SECONDS), () -> {
                Instant start = Instant.now();
                List<LogEntry> logs = Learnweb.dao().getLogDao()
                    .findByGroupIdAndTargetId(this.getGroupId(), this.getId(), Action.collectOrdinals(Action.LOGS_RESOURCE_FILTER));

                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > 100) {
                    log.warn("getLogs took {}ms; resourceId: {};", duration, id);
                }
                return logs;
            });
        }

        return logs.get();
    }

    /**
     * Is called when a Resource object is deserialized.
     * To make sure that there exists only one instance of each resource we interfere the deserialization process
     * if there exists a cached version of the resource we will return this instance
     */
    @Serial
    protected Object readResolve() {
        log.debug("Deserialize resource: {}", id);
        try {
            return Learnweb.dao().getResourceDao().findByIdOrElseThrow(id);
        } catch (Exception e) {
            log.fatal("Can't load resource: {}", id, e);
        }
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("title", title)
            .append("url", url)
            .append("storageType", storageType)
            .append("service", service)
            .append("type", type)
            .toString();
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
        @Serial
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
        public void putAll(Map<? extends String, ? extends String[]> map) {
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
        @Serial
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
            if (!key.getClass().equals(String.class)) {
                throw new IllegalArgumentException("key must be a string");
            }

            String keyString = ((String) key).toLowerCase();

            return switch (keyString) {
                case "title" -> getTitle();
                case "author" -> getAuthor();
                case "description" -> getDescription();
                case "language" -> getLanguage();
                default -> wrappedMap.get(key);
            };
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
        public void putAll(Map<? extends String, ? extends String> map) {
            wrappedMap.putAll(map);
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

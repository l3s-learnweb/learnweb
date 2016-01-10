package de.l3s.learnweb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.util.HasId;
import de.l3s.util.StringHelper;

public class Resource implements HasId, Serializable // AbstractResultItem,
{
    private static final long serialVersionUID = -8486919346993051937L;
    private final static Logger log = Logger.getLogger(Resource.class);

    public enum OnlineStatus
    {
	UNKNOWN,
	ONLINE,
	OFFLINE
    };

    public static final int FILE_RESOURCE = 1;
    public static final int WEB_RESOURCE = 2;

    private int id = -1; // default id, that indicates that this resource is not stored at fedora
    private int groupId;
    private int folderId;
    private String title;
    private String description = "";
    private String url;
    private int storageType = WEB_RESOURCE;
    private int rights = 0;
    private String source = ""; // The place where the resource was found
    private String location = ""; // The location where the resource (metadata) is stored; for example Learnweb, Flickr, Youtube ...
    private String language = ""; // 2-letter language code
    private String author = "";
    private String type = "";
    private String format = "";
    private int duration;
    private int ownerUserId;
    private String idAtService = "";
    private int ratingSum;
    private int rateNumber;
    private String embeddedSize1;
    private String embeddedSize3;
    private String embeddedSize4;
    private String embeddedSize1Raw;
    private String embeddedSize3Raw;
    private String embeddedSize4Raw;
    private String fileName; // stores the file name of uploaded resource
    private String fileUrl;
    private String maxImageUrl; // an url to the largest image preview of this resource
    private String query; // the query which was used to find this resource
    private int originalResourceId = 0; // if the resource was copied from an older fedora resource this stores the id of the original resource
    private String machineDescription;
    private Thumbnail thumbnail0;
    private Thumbnail thumbnail1;
    private Thumbnail thumbnail2;
    private Thumbnail thumbnail2b;
    private Thumbnail thumbnail2c;
    private Thumbnail thumbnail3;
    private Thumbnail thumbnail4;
    private String embeddedRaw;
    private String transcript; //To store the English transcripts for TED videos
    private OnlineStatus onlineStatus = OnlineStatus.UNKNOWN;
    private boolean restricted = false;
    private Date creationDate = new Date();
    private HashMap<String, String> metadata = new HashMap<String, String>(); // userId : hasRated

    private int views;
    private int thumbUp;
    private int thumbDown;
    private HashMap<Integer, Boolean> isThumbRatedByUser = new HashMap<Integer, Boolean>(); // userId : hasRated
    private HashMap<Integer, Boolean> isRatedByUser = new HashMap<Integer, Boolean>(); // userId : hasRated
    private LinkedHashMap<Integer, File> files = new LinkedHashMap<Integer, File>(); // resource_file_number : file

    // caches
    private transient OwnerList<Tag, User> tags = null;
    private transient List<Comment> comments;
    private transient User owner;
    private transient LinkedList<ArchiveUrl> archiveUrls = null;//To store the archived URLs 
    private transient String path = null;
    private transient String prettyPath = null;

    protected void clearCaches()
    {
	path = null;
    }

    /**
     * Do nothing constructor
     */
    public Resource()
    {
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
	this.type = type;
	setThumbnail2(new Thumbnail(thumbnail_url, thumbnail_width, thumbnail_height));
	setThumbnail4(new Thumbnail(thumbnail4_url, thumbnail4_width, thumbnail4_height));
    }

    @Deprecated
    public void prepareEmbeddedCodes()
    {
	if(isRestricted())
	{
	    embeddedSize1 = "<img src=\"../resources/resources/img/RestrictedAccess.jpg\" width=\"300\" height=\"214\" />";

	    // TODO find a better solution; don't set a fixed error image
	    Thumbnail restrictedImage = new Thumbnail("../resources/resources/img/RestrictedAccess.jpg", 300, 214);
	    setThumbnail0(restrictedImage.resize(150, 120));
	    setThumbnail1(restrictedImage.resize(150, 150));
	    setThumbnail2(restrictedImage);
	    setThumbnail3(restrictedImage);
	    setThumbnail4(restrictedImage);
	}
	else if(getOnlineStatus().equals(OnlineStatus.OFFLINE))
	{
	    embeddedSize1 = "<img src=\"../resources/resources/img/page_no_longer_available.jpg\" width=\"300\" height=\"300\" />";

	    // TODO find a better solution; don't set a fixed error image
	    Thumbnail pageNotFoundImage = new Thumbnail("../resources/resources/img/page_no_longer_available.jpg", 300, 300);
	    setThumbnail0(null);
	    setThumbnail1(pageNotFoundImage.resize(150, 150));
	    setThumbnail2(pageNotFoundImage);
	    setThumbnail3(pageNotFoundImage);
	    setThumbnail4(pageNotFoundImage);

	}
	else if(null == embeddedSize1 || null == embeddedSize3)
	{

	    if(source.equalsIgnoreCase("YouTube"))
	    {
		Pattern pattern = Pattern.compile("v[/=]([^&]+)");
		Matcher matcher = pattern.matcher(url);

		if(matcher.find())
		{
		    String videoId = matcher.group(1);
		    if(null == embeddedSize1)
			this.embeddedSize1 = "<img src=\"http://img.youtube.com/vi/" + videoId + "/default.jpg\" width=\"100\" height=\"75\" />";
		    if(null == embeddedSize3)
			this.embeddedSize3 = "<embed pluginspage=\"http://www.adobe.com/go/getflashplayer\" src=\"http://www.youtube.com/v/" + videoId + "\" type=\"application/x-shockwave-flash\" height=\"375\" width=\"500\"></embed>";
		    this.format = "application/x-shockwave-flash";
		}
	    }
	    else if(source.equals("Google") && type.equals("Video"))
	    {
		Pattern pattern = Pattern.compile("youtube.com/watch%3Fv%3D([^&]+)");
		Matcher matcher = pattern.matcher(url);

		if(matcher.find())
		{
		    String videoId = matcher.group(1);
		    this.embeddedSize1 = "<img src=\"http://img.youtube.com/vi/" + videoId + "/default.jpg\" width=\"100\" height=\"75\" />";
		    this.embeddedSize3 = "<embed pluginspage=\"http://www.adobe.com/go/getflashplayer\" src=\"http://www.youtube.com/v/" + videoId + "\" type=\"application/x-shockwave-flash\" height=\"375\" width=\"500\"></embed>";

		    this.format = "application/x-shockwave-flash";
		    this.source = "YouTube";
		    this.url = "https://www.youtube.com/watch?v=" + videoId;

		}
	    }
	    else if(source.equalsIgnoreCase("Vimeo"))
	    {
		Pattern pattern = Pattern.compile("vimeo\\.com/([^&]+)");
		Matcher matcher = pattern.matcher(url);

		if(matcher.find())
		{
		    String videoId = matcher.group(1);
		    this.embeddedSize3 = "<object width=\"500\" height=\"375\"><param name=\"allowfullscreen\" value=\"true\" /><param name=\"allowscriptaccess\" value=\"always\" />" + "<param name=\"movie\" value=\"http://vimeo.com/moogaloop.swf?clip_id=" + videoId
			    + "&amp;server=vimeo.com&amp;show_title=1&amp;show_byline=1&amp;show_portrait=0&amp;color=&amp;fullscreen=1\" /><embed src=\"http://vimeo.com/moogaloop.swf?clip_id=" + videoId
			    + "&amp;server=vimeo.com&amp;show_title=1&amp;show_byline=1&amp;show_portrait=0&amp;color=&amp;fullscreen=1\" type=\"application/x-shockwave-flash\" allowfullscreen=\"true\" allowscriptaccess=\"always\" width=\"500\" height=\"375\"></embed></object>";
		    this.format = "application/x-shockwave-flash";
		}

	    }
	    else if(source.equals("Ipernity") && embeddedSize1 != null)
	    {

		if(type.equals("Image"))
		    embeddedSize3 = embeddedSize1.replace(".100.", ".500.");
		else
		    embeddedSize3 = "<a href=\"" + url + "\">" + url + "</a>";
	    }
	    else if(source.equals("Flickr") && type.equals("Image") && embeddedSize1 != null)
	    {

		if(null == embeddedSize3)
		    embeddedSize3 = embeddedSize1.replace("_t.", ".");
	    }

	    if(embeddedSize1 == null || embeddedSize1.length() < 3)
	    {
		if(type.equalsIgnoreCase("audio"))
		    embeddedSize1 = "<img src=\"../resources/resources/img/audio.png\" width=\"100\" height=\"100\" />";
		else if(format.startsWith("application/vnd.") || format.startsWith("application/ms"))
		    embeddedSize1 = "<img src=\"../resources/resources/img/document.png\" width=\"100\" height=\"100\" />";
		else if(storageType == WEB_RESOURCE)
		    embeddedSize1 = "<img src=\"../resources/resources/img/website-140.png\" width=\"100\" height=\"100\" />";
		else if(format.startsWith("text/"))
		    embeddedSize1 = "<img src=\"../resources/resources/img/document.png\" width=\"100\" height=\"100\" />";
	    }

	}
	else
	{
	    for(File file : files.values())
	    {
		embeddedSize1 = replacePlaceholder(embeddedSize1, file);
		embeddedSize3 = replacePlaceholder(embeddedSize3, file);
	    }
	}
    }

    public void addTag(String tagName, User user) throws Exception
    {
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

    public void deleteTag(Tag tag) throws Exception
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

    public Comment addComment(String text, User user) throws Exception
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

    public int getGroupId()
    {
	return groupId;
    }

    public void setGroupId(int groupId)
    {
	this.groupId = groupId;
    }

    public Group getGroup() throws SQLException
    {
	if(groupId == 0)
	    return null;

	return Learnweb.getInstance().getGroupManager().getGroupById(groupId);
    }

    public Group getOriginalGroup() throws SQLException
    {
	if(originalResourceId == 0)
	    return null;

	Resource originalResource = Learnweb.getInstance().getResourceManager().getResource(originalResourceId);
	return Learnweb.getInstance().getGroupManager().getGroupById(originalResource.getGroupId());
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

    public String getUrl()
    {
	return url;
    }

    public int getStorageType()
    {
	return storageType;
    }

    public void setStorageType(int type)
    {
	if(type != FILE_RESOURCE && type != WEB_RESOURCE)
	    throw new IllegalArgumentException();
	this.storageType = type;
    }

    public int getRights()
    {
	return rights;
    }

    public void setRights(int rights)
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

    public String getType()
    {
	return type;
    }

    public String getFormat()
    {
	return format;
    }

    public User getOwnerUser() throws SQLException
    {
	if(null == owner && -1 != ownerUserId)
	    owner = Learnweb.getInstance().getUserManager().getUser(ownerUserId);
	return owner;
    }

    public int getOwnerUserId()
    {
	return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId)
    {
	this.ownerUserId = ownerUserId;
	this.owner = null;
    }

    public double getRating()
    {
	return ratingSum == 0 ? 0.0 : ratingSum / rateNumber;
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
     * 
     * @return Returns a comma separated list of tags
     * @throws SQLException
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

    public void setId(int id)
    {
	this.id = id;
    }

    public void setOwner(User user)
    {
	this.owner = user;
	this.ownerUserId = owner.getId();
    }

    /**
     * Creates a copy of a resource.<br/>
     * Ratings and comments are not copied
     */
    @Override
    public Resource clone()
    {

	Resource r = new Resource();
	r.setId(-1);
	r.setGroupId(groupId);
	r.setFolderId(folderId);
	r.setTitle(title);
	r.setDescription(description);
	r.setUrl(url);
	r.setStorageType(storageType);
	r.setRights(rights);
	r.setLocation(location);
	r.setSource(source);
	r.setAuthor(author);
	r.setType(type);
	r.setFormat(format);
	r.setOwnerUserId(ownerUserId);
	r.setEmbeddedSize1Raw(embeddedSize1);
	r.setEmbeddedSize3Raw(embeddedSize3);
	r.setEmbeddedSize4Raw(embeddedSize4);
	r.setMaxImageUrl(maxImageUrl);
	r.setFileName(fileName);
	r.setFileUrl(fileUrl);
	r.setQuery(query);
	r.setThumbnail0(thumbnail0);
	r.setThumbnail1(thumbnail1);
	r.setThumbnail2(thumbnail2);
	r.setThumbnail3(thumbnail3);
	r.setThumbnail4(thumbnail4);
	r.setEmbeddedRaw(embeddedRaw);
	r.setDuration(duration);
	r.setMachineDescription(machineDescription);
	r.setFileName(fileName);
	r.setTranscript(transcript);
	r.setOnlineStatus(onlineStatus);
	r.setIdAtService(idAtService);
	r.setRestricted(restricted);
	r.setCreationDate(creationDate);
	r.setArchiveUrls(getArchiveUrls());
	// sets the originalResourceId to the id of the source resource
	if(originalResourceId == 0)
	    r.setOriginalResourceId(id);
	else
	    r.setOriginalResourceId(originalResourceId);

	return r;
    }

    /**
     * rate this resource
     * 
     * @param value
     *            the rating 1-5
     * @param user
     *            the user who rates
     * @throws KRSMException
     * @throws SQLException
     */
    public void rate(int value, User user) throws Exception
    {
	Learnweb.getInstance().getResourceManager().rateResource(id, user.getId(), value);

	rateNumber++;
	ratingSum += value;

	isRatedByUser.put(user.getId(), true);
    }

    public boolean isRatedByUser(int userId) throws Exception
    {
	Boolean value = isRatedByUser.get(userId);
	if(null != value) // the answer is cached
	    return value;

	// the answer isn't cached we have to ask fedora

	value = Learnweb.getInstance().getResourceManager().isResourceRatedByUser(id, userId);
	isRatedByUser.put(userId, value); // cache answer

	return value;
    }

    /**
     * Stores all made changes at fedora
     * 
     * @throws KRSMException
     * @throws SQLException
     */
    public void save() throws SQLException
    {
	Learnweb.getInstance().getResourceManager().saveResource(this);
    }

    public void setTitle(String title)
    {
	this.title = StringUtils.isNotEmpty(title) ? Jsoup.clean(title, Whitelist.none()) : "no title";
    }

    public void setDescription(String description)
    {
	this.description = description == null ? "" : StringHelper.clean(description, Whitelist.simpleText());
    }

    public void setUrl(String url)
    {
	this.url = url;
    }

    /**
     * The location where the resource (metadata) is stored
     * 
     * @param location
     *            for example Learnweb, Flickr, Youtube ...
     */
    public void setLocation(String location)
    {
	this.location = location;
    }

    public void setType(String type)
    {
	/*
	if(null == type || type.length() == 0)
	    log.info("Resource: " + id + "; type set to null", new Exception());
	*/
	if(type.equalsIgnoreCase("videos") || type.equalsIgnoreCase("video"))
	    this.type = "Video";
	else if(type.equalsIgnoreCase("photos") || type.equalsIgnoreCase("image"))
	    this.type = "Image";
	else if(null == type || type.length() == 0)
	    this.type = "Unknown";
	else
	    this.type = type;
    }

    /**
     * set the mime type
     * 
     * @param format
     */
    public void setFormat(String format)
    {
	this.format = format;
    }

    public int getThumbUp()
    {
	return thumbUp;
    }

    public int getThumbDown()
    {
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

    // the following functions are JLearnweb specific and only for convenience included in this class

    public String getLearnwebUrl() throws SQLException
    {
	if(getId() != -1)
	    return "group/resources.jsf?group_id=" + getGroup().getId() + "&resource_id=" + getId();

	return getUrl();
    }

    public String getServiceIcon()
    {
	if(getId() != -1) // is stored at fedora
	    return "/resources/icon/services/fedora.gif";

	return "/resources/icon/services/" + getLocation().toLowerCase() + ".gif";
    }

    public String getOriginalServiceIcon()
    {
	if(getLocation().equalsIgnoreCase("desktop"))
	    return "/resources/icon/upload.gif";
	return "/resources/icon/services/" + getLocation().toLowerCase() + ".gif";
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
     * 
     * @return
     */
    @Deprecated
    public String getEmbeddedSize1()
    {

	if(getThumbnail1() != null)
	    return getThumbnail1().toHTML();
	if(getThumbnail2() != null)
	    return getThumbnail2().resize(150, 150).toHTML();
	if(getThumbnail3() != null)
	    return getThumbnail3().resize(150, 150).toHTML();

	return embeddedSize1;
    }

    /**
     * html code, only image or text<br/>
     * max width and max height 100px
     */
    @Deprecated
    public void setEmbeddedSize1Raw(String embeddedSize1)
    {
	this.embeddedSize1 = embeddedSize1;
	this.embeddedSize1Raw = embeddedSize1;
    }

    /**
     * html code, may contain flash<br/>
     * max width 500px and max height 600px
     * 
     * @param embedded
     */
    @Deprecated
    public void setEmbeddedSize3Raw(String embedded)
    {

	this.embeddedSize3 = embedded;
	this.embeddedSize3Raw = embedded;
    }

    /**
     * html code, may contain flash<br/>
     * max width 500px and max height 600px
     * 
     * @return
     */
    @Deprecated
    public String getEmbeddedSize3()
    {
	if(getThumbnail3() != null)
	    return getThumbnail3().toHTML();

	return embeddedSize3;
    }

    /**
     * html code, may contain flash<br/>
     * max width and max height 100%
     */
    @Deprecated
    public String getEmbeddedSize4()
    {
	return embeddedSize4;
    }

    /**
     * html code, may contain flash<br/>
     * max width and max height 100%
     */
    @Deprecated
    public void setEmbeddedSize4Raw(String embeddedSize4)
    {
	this.embeddedSize4 = embeddedSize4;
	this.embeddedSize4Raw = embeddedSize4;
    }

    /**
     * Contains placeholders for the files
     * 
     * @return
     */
    @Deprecated
    public String getEmbeddedSize1Raw()
    {
	return embeddedSize1Raw;
    }

    /**
     * Contains placeholders for the files
     * 
     * @return
     */
    @Deprecated
    public String getEmbeddedSize3Raw()
    {
	return embeddedSize3Raw;
    }

    /**
     * Contains placeholders for the files
     * 
     * @return
     */
    @Deprecated
    public String getEmbeddedSize4Raw()
    {
	return embeddedSize4Raw;
    }

    /**
     * Url to the best (high resolution) available preview image.<br/>
     * Only available for interweb search results
     * 
     * @return
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
	return fileName;
    }

    /**
     * @param fileName
     *            the file name of uploaded resource
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
     * 
     * @return the query which was used to find this resource
     */
    public String getQuery()
    {
	if(query == null)
	    return "none";
	return query;
    }

    /**
     * 
     * @param query
     *            the query which was used to find this resource
     */
    public void setQuery(String query)
    {
	this.query = query;
    }

    /**
     * @return if the resource was copied from an older fedora resource this returns the id of the original resource <b>0</b> otherwise
     */
    public int getOriginalResourceId()
    {
	return originalResourceId;
    }

    /**
     * 
     * @param originalResourceId
     *            if the resource was copied from an older fedora resource this stores the id of the original resource
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
	if(null == source || source.length() == 0)
	    log.info("Resource: " + id + "; source set to null");

	this.source = source;
    }

    public LinkedHashMap<Integer, File> getFiles()
    {
	// TODO add lazy loading ass soon as embedded images are removed
	return files;
    }

    /**
     * This method does not persist the changes.<br/>
     * see: FileManager.addFileToResource(file, resource);
     * 
     * @param file
     * @throws SQLException
     */
    public void addFile(File file) throws SQLException
    {
	files.put(file.getResourceFileNumber(), file);

	if(id > 0) // the resource is already stored 
	{
	    FileManager fm = Learnweb.getInstance().getFileManager();
	    fm.addFileToResource(file, this);
	}

    }

    public File getFile(int fileNumber)
    {
	return files.get(fileNumber);
    }

    @Deprecated
    public static String createPlaceholder(int fileNumber)
    {
	return "{learnweb_file_" + fileNumber + "}";
    }

    @Deprecated
    private static String replacePlaceholder(String embeddedCode, File file)
    {
	return embeddedCode.replace("{learnweb_file_" + file.getResourceFileNumber() + "}", file.getUrl());
    }

    /**
     * @return Text that has been automatically extracted from the source file/url
     */
    public String getMachineDescription()
    {
	return machineDescription;
    }

    /**
     * @param machineDescription
     *            Text that has been automatically extracted from the source file/url
     */
    public void setMachineDescription(String machineDescription)
    {
	this.machineDescription = machineDescription;
    }

    /**
     * maximum width/height : 150 / 120
     * 
     * @return
     */
    public Thumbnail getThumbnail0()
    {
	return thumbnail0;
    }

    /**
     * height and width = 150px
     * 
     * @return
     */
    public Thumbnail getThumbnail1()
    {
	return thumbnail1;
    }

    /**
     * maximum width/height : 300 / 220
     * 
     * @return
     */
    public Thumbnail getThumbnail2()
    {
	return thumbnail2;
    }

    /**
     * returns thumbnail2 but down scaled to a maximum size of 240 * 128
     * 
     * @return
     */
    public Thumbnail getThumbnail2b()
    {
	return thumbnail2b;
    }

    /**
     * returns thumbnail2 but down scaled to a maximum size of 171 * 128
     * 
     * @return
     */
    public Thumbnail getThumbnail2c()
    {
	return thumbnail2c;
    }

    /**
     * maximum width/height : 500 / 600
     * 
     * @return
     */
    public Thumbnail getThumbnail3()
    {
	if(null == thumbnail3)
	    return getThumbnail2();

	return thumbnail3;
    }

    /**
     * maximum width/height : 1280 / 1024
     * 
     * @return
     */
    public Thumbnail getThumbnail4()
    {
	if(null == thumbnail4)
	    return getThumbnail3();

	return thumbnail4;
    }

    protected void setThumbnail0(Thumbnail thumbnail0)
    {
	this.thumbnail0 = thumbnail0;
    }

    protected void setThumbnail1(Thumbnail thumbnail1)
    {
	this.thumbnail1 = thumbnail1;
    }

    protected void setThumbnail2(Thumbnail thumbnail2)
    {
	this.thumbnail2 = thumbnail2;
	if(thumbnail2 != null)
	{
	    this.thumbnail2b = thumbnail2.resize(240, 128);
	    this.thumbnail2c = thumbnail2.resize(171, 128);
	}
    }

    protected void setThumbnail3(Thumbnail thumbnail3)
    {
	this.thumbnail3 = thumbnail3;
    }

    protected void setThumbnail4(Thumbnail thumbnail4)
    {
	this.thumbnail4 = thumbnail4;
    }

    public String getEmbedded()
    {
	Thumbnail large = getThumbnail4();

	if(getType().equalsIgnoreCase("image"))
	    return "<img src=\"" + getThumbnail2().getUrl() + "\" height=\"" + large.getHeight() + "\" width=\"" + large.getWidth() + "\" original-src=\"" + large.getUrl() + "\"/>";
	else if(getType().equalsIgnoreCase("text"))
	    return "<iframe src=\"" + getUrl() + "\" />";
	else if(getType().equalsIgnoreCase("video"))
	{
	    if(getSource().equalsIgnoreCase("loro"))
	    {
		return "<iframe src=\"video.jsf?resource_id=" + id + "\" width=\"100%\" height=\"100%\" />";
		//log.debug("" + getFileUrl());
		//return "<video class=\"video-js vjs-default-skin vjs-big-play-centered\" controls=\"preload=none\" width=\"100%\" height=\"100%\" data-setup=\"{}\"><source src=\"" + getFileUrl() + "\" /></video>";
		//+ "<link href=\"http://vjs.zencdn.net/4.12/video-js.css\" rel=\"stylesheet\"/><script src=\"http://vjs.zencdn.net/4.12/video.js\"></script>";

	    }
	}
	if(getEmbeddedRaw() != null)
	    return getEmbeddedRaw();

	if(getEmbeddedSize4() != null)
	    return getEmbeddedSize4();

	return getEmbeddedSize3();
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
     * 
     * @return
     */
    public String getEmbeddedRaw()
    {
	return embeddedRaw;
    }

    public void setEmbeddedRaw(String embeddedRaw)
    {
	this.embeddedRaw = embeddedRaw;
    }

    public int getViews()
    {
	return views;
    }

    public void setViews(int views)
    {
	this.views = views;
    }

    @Override
    public String toString()
    {
	return "Resource [id=" + id + ", title=" + title + ", url=" + url + ", storageType=" + storageType + ", source=" + source + ", type=" + type + ", format=" + format + "]";
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
	    try
	    {
		archiveUrls = Learnweb.getInstance().getResourceManager().getArchiveUrlsByResourceId(id);
	    }
	    catch(SQLException e)
	    {
		log.error("Error while retrieving archive urls for resource: ", e);
	    }
	}

	return archiveUrls;
    }

    public void addArchiveUrl(ArchiveUrl archiveUrl)
    {
	// TODO really add archive url; until then clean cache:	
	archiveUrls = null;
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
	if(fileUrl != null && fileUrl.length() > 500)
	    throw new IllegalArgumentException("url is too long: " + fileUrl.length() + "; " + fileUrl);

	this.fileUrl = fileUrl;
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException
    {
	inputStream.defaultReadObject();

	// restore transient objects
	//log.debug("deserialize: " + id);
    }

    /**
     * 
     * @return 2-letter language code ISO 639-1
     */
    public String getLanguage()
    {
	return language;
    }

    /**
     * 
     * @param language 2-letter language code ISO 639-1
     */
    public void setLanguage(String language)
    {
	if(null == language)
	    language = "";
	else if(language.length() != 0 && language.length() != 2)
	    throw new IllegalArgumentException("expected 2-letter language code");
	else
	    this.language = language.toLowerCase();
    }

    public Date getCreationDate()
    {
	return creationDate;
    }

    public void setCreationDate(Date creationDate)
    {
	this.creationDate = creationDate;
    }

    /**
     * 
     * @param key
     * @param value
     * @return the previous value associated with key, or null if there was no mapping for key. (A null return can also indicate that the map
     *         previously associated null with key.)
     */
    public String setMetadataValue(String key, String value)
    {
	key = key.toLowerCase();
	if(key.equals("author"))
	    throw new IllegalArgumentException(key + " is a reserved name");

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

    public HashMap<String, String> getMetadata()
    {
	return metadata;
    }

    public void setMetadata(HashMap<String, String> metadata)
    {
	this.metadata = metadata;
    }

    public void setArchiveUrls(LinkedList<ArchiveUrl> archiveUrls)
    {
	this.archiveUrls = archiveUrls;
    }

    /**
     * returns a string representation of the resources path
     * 
     * @return
     * @throws SQLException
     */
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
     * 
     * @return
     * @throws SQLException
     */
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
}

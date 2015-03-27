package de.l3s.learnweb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Resource.OnlineStatus;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.StringHelper;

public class ResourceManager
{
    private final static String COMMENT_COLUMNS = "`comment_id`, `resource_id`, `user_id`, `text`, `date`";
    private final static String RESOURCE_COLUMNS = "r.resource_id, r.title, r.description, r.url, r.storage_type, r.rights, r.source, r.type, r.format, r.owner_user_id, r.rating, r.rate_number, r.embedded_size1, r.embedded_size2, r.embedded_size3, r.embedded_size4, r.filename, r.max_image_url, r.query, r.original_resource_id, r.author, r.access, r.thumbnail0_url, r.thumbnail0_file_id, r.thumbnail0_width, r.thumbnail0_height, r.thumbnail1_url, r.thumbnail1_file_id, r.thumbnail1_width, r.thumbnail1_height, r.thumbnail2_url, r.thumbnail2_file_id, r.thumbnail2_width, r.thumbnail2_height, r.thumbnail3_url, r.thumbnail3_file_id, r.thumbnail3_width, r.thumbnail3_height, r.thumbnail4_url, r.thumbnail4_file_id, r.thumbnail4_width, r.thumbnail4_height, r.embeddedRaw, r.transcript, r.online_status, r.id_at_service, r.duration, r.archive_url";
    private final static Logger log = Logger.getLogger(ResourceManager.class);

    private final Learnweb learnweb;

    private ICache<Resource> cache;

    public ResourceManager(Learnweb learnweb)
    {
	Properties properties = learnweb.getProperties();
	int cacheSize = Integer.parseInt(properties.getProperty("RESOURCE_CACHE"));

	this.learnweb = learnweb;
	this.cache = cacheSize == 0 ? new DummyCache<Resource>() : new Cache<Resource>(cacheSize);
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getResourcesByUserId(int)
     */

    public List<Resource> getResourcesByUserId(int userId) throws SQLException
    {
	return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE owner_user_id = ? AND deleted = 0", null, userId);
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getResourcesByTagId(int)
     */

    public List<Resource> getResourcesByTagId(int tagId) throws SQLException
    {
	return getResourcesByTagId(tagId, 1000);
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getResourcesByTagId(int, int)
     */

    public List<Resource> getResourcesByTagId(int tagId, int maxResults) throws SQLException
    {
	return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_tag USING ( resource_id ) WHERE tag_id = ? AND deleted = 0 LIMIT ? ", null, tagId, maxResults);
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getRatedResourcesByUserId(int)
     */

    public List<Resource> getRatedResourcesByUserId(int userId) throws SQLException
    {
	return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_rating USING ( resource_id ) WHERE user_id = ? AND deleted = 0 ", null, userId);
    }

    /**
     * Returns all resources (which were not deleted)
     * 
     * @param userId
     * @return
     * @throws SQLException
     */
    public List<Resource> getResourcesWithoutThumbnail() throws SQLException
    {
	return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE `deleted` = 0 AND `thumbnail2_url` IS NULL AND  `max_image_url` IS NULL", null);
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#isResourceRatedByUser(int, int)
     */

    public boolean isResourceRatedByUser(int resourceId, int userId) throws Exception
    {
	PreparedStatement stmt = learnweb.getConnection().prepareStatement("SELECT 1 FROM lw_resource_rating WHERE resource_id =  ? AND user_id = ?");
	stmt.setInt(1, resourceId);
	stmt.setInt(2, userId);
	ResultSet rs = stmt.executeQuery();
	boolean response = rs.next();
	stmt.close();
	return response;
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#rateResource(int, int, int)
     */

    protected void rateResource(int resourceId, int userId, int value) throws Exception
    {
	PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO lw_resource_rating (`resource_id`, `user_id`, `rating`) VALUES(?, ?, ?)");
	replace.setInt(1, resourceId);
	replace.setInt(2, userId);
	replace.setInt(3, value);
	replace.executeUpdate();
	replace.close();

	PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE lw_resource SET rating = rating + ?, rate_number = rate_number + 1 WHERE resource_id = ?");
	update.setInt(1, value);
	update.setInt(2, resourceId);
	update.executeUpdate();
	update.close();
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#thumbRateResource(int, int, int)
     */

    protected void thumbRateResource(int resourceId, int userId, int direction) throws SQLException
    {
	if(direction != 1 && direction != -1)
	    throw new IllegalArgumentException("Illegal value [" + direction + "] for direction. Valid values are 1 and -1");

	PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_thumb` (`resource_id` ,`user_id` ,`direction`) VALUES (?,?,?)");
	insert.setInt(1, resourceId);
	insert.setInt(2, userId);
	insert.setInt(3, direction);
	insert.executeUpdate();
	insert.close();
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#isResourceThumbRatedByUser(int, int)
     */

    public boolean isResourceThumbRatedByUser(int resourceId, int userId) throws SQLException
    {
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM lw_thumb WHERE resource_id = ? AND user_id = ?");
	select.setInt(1, resourceId);
	select.setInt(2, userId);
	ResultSet rs = select.executeQuery();
	boolean isRated = rs.next();
	select.close();

	return isRated;
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getResource(int, boolean)
     */

    private Resource getResource(int resourceId, boolean useCache) throws SQLException
    {
	Resource resource = cache.get(resourceId);

	if(null != resource && useCache)
	    return resource;

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + RESOURCE_COLUMNS + " FROM `lw_resource` r WHERE resource_id = ? and deleted = 0");
	select.setInt(1, resourceId);
	ResultSet rs = select.executeQuery();

	if(!rs.next())
	    return null;

	resource = createResource(rs);
	select.close();

	resource.prepareEmbeddedCodes();

	return cache.put(resource);
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getResource(int)
     */

    public Resource getResource(int resourceId) throws SQLException
    {
	return getResource(resourceId, true);
    }

    public void deleteResource(int resourceId) throws SQLException
    {
	// delete resource from SOLR index
	try
	{
	    learnweb.getSolrClient().deleteFromIndex(resourceId);
	}
	catch(Exception e)
	{
	    log.error("Couldn't delete resource " + resourceId + " from SOLR", e);
	}

	// delete the resource from all groups
	PreparedStatement delete = Learnweb.getConnectionStatic().prepareStatement("DELETE FROM `lw_group_resource` WHERE `resource_id` = ?");
	delete.setInt(1, resourceId);
	delete.executeUpdate();
	delete.close();

	// flag the resource as deleted
	PreparedStatement update = Learnweb.getConnectionStatic().prepareStatement("UPDATE `lw_resource` SET deleted = 1 WHERE `resource_id` = ?");
	update.setInt(1, resourceId);
	update.executeUpdate();
	update.close();

	update = Learnweb.getConnectionStatic().prepareStatement("UPDATE `lw_file` SET deleted = 1 WHERE `resource_id` = ?");
	update.setInt(1, resourceId);
	update.executeUpdate();
	update.close();

	// remove resource from cache
	cache.remove(resourceId);
    }

    /**
     * Don't use this function.
     * Usually you have to call deleteResource()
     * 
     * @param resourceId
     * @throws SQLException
     */
    protected void deleteResourcePermanent(int resourceId) throws SQLException
    {
	deleteResource(resourceId);

	Connection connection = Learnweb.getConnectionStatic();

	// delete the resource
	PreparedStatement delete = connection.prepareStatement("DELETE FROM `lw_resource` WHERE `resource_id` = ?");
	delete.setInt(1, resourceId);
	delete.executeUpdate();
	delete.close();

	// delete the comments
	delete = connection.prepareStatement("DELETE FROM `lw_comment` WHERE `resource_id` = ?");
	delete.setInt(1, resourceId);
	delete.executeUpdate();
	delete.close();

	// delete the ratings
	delete = connection.prepareStatement("DELETE FROM `lw_thumb` WHERE `resource_id` = ?");
	delete.setInt(1, resourceId);
	delete.executeUpdate();
	delete.close();

	// delete the resource
	delete = connection.prepareStatement("DELETE FROM `lw_resource_rating` WHERE `resource_id` = ?");
	delete.setInt(1, resourceId);
	delete.executeUpdate();
	delete.close();

	// delete files?

	// remove resource from cache
	cache.remove(resourceId);
    }

    public void saveResource(Resource resource) throws SQLException
    {
	PreparedStatement replace = learnweb
		.getConnection()
		.prepareStatement(
			"REPLACE INTO `lw_resource` (`resource_id` ,`title` ,`description` ,`url` ,`storage_type` ,`rights` ,`source` ,`type` ,`format` ,`owner_user_id` ,`rating` ,`rate_number` ,`query`, embedded_size1, embedded_size2, embedded_size3, embedded_size4, filename,	max_image_url, original_resource_id, machine_description, author, access, thumbnail0_url, thumbnail0_file_id, thumbnail0_width, thumbnail0_height, thumbnail1_url, thumbnail1_file_id, thumbnail1_width, thumbnail1_height, thumbnail2_url, thumbnail2_file_id, thumbnail2_width, thumbnail2_height, thumbnail3_url, thumbnail3_file_id, thumbnail3_width, thumbnail3_height, thumbnail4_url, thumbnail4_file_id, thumbnail4_width, thumbnail4_height, embeddedRaw, transcript, online_status, id_at_service, duration, archive_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
			Statement.RETURN_GENERATED_KEYS);

	if(resource.getId() < 0) // the Resource is not yet stored at the database
	    replace.setNull(1, java.sql.Types.INTEGER);
	else
	    replace.setInt(1, resource.getId());
	replace.setString(2, resource.getTitle());
	replace.setString(3, resource.getDescription());
	replace.setString(4, resource.getUrl());
	replace.setInt(5, resource.getStorageType());
	replace.setInt(6, resource.getRights());
	replace.setString(7, resource.getSource());
	replace.setString(8, resource.getType());
	replace.setString(9, resource.getFormat());
	replace.setInt(10, resource.getOwnerUserId());
	replace.setInt(11, resource.getRatingSum());
	replace.setInt(12, resource.getRateNumber());
	replace.setString(13, resource.getQuery());
	replace.setString(14, resource.getEmbeddedSize1Raw());
	replace.setString(15, resource.getEmbeddedSize1Raw());
	replace.setString(16, resource.getEmbeddedSize3Raw());
	replace.setString(17, resource.getEmbeddedSize4Raw());
	replace.setString(18, resource.getFileName());
	replace.setString(19, resource.getMaxImageUrl());
	replace.setInt(20, resource.getOriginalResourceId());
	replace.setString(21, resource.getMachineDescription());
	replace.setString(22, resource.getAuthor());
	replace.setString(23, resource.getAccess());

	Thumbnail[] thumbnails = { resource.getThumbnail0(), resource.getThumbnail1(), resource.getThumbnail2(), resource.getThumbnail3(), resource.getThumbnail4() };

	for(int i = 0, m = 24; i < 5; i++)
	{
	    String url = null;
	    int fileId = 0;
	    int width = 0;
	    int height = 0;

	    Thumbnail tn = thumbnails[i];
	    if(tn != null) // a thumbnail is defined
	    {
		if(tn.getFileId() == 0)
		    url = tn.getUrl();
		fileId = tn.getFileId();
		width = tn.getWidth();
		height = tn.getHeight();
	    }
	    replace.setString(m++, url);
	    replace.setInt(m++, fileId);
	    replace.setInt(m++, width);
	    replace.setInt(m++, height);
	}

	replace.setString(44, resource.getEmbeddedRaw());
	replace.setString(45, resource.getTranscript());
	replace.setString(46, resource.getOnlineStatus().name());
	replace.setString(47, resource.getIdAtService());
	replace.setInt(48, resource.getDuration());
	replace.setString(49, resource.getArchiveUrl());

	replace.executeUpdate();

	if(resource.getId() < 0) // get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    resource.setId(rs.getInt(1));
	    resource.setLocation("Learnweb");
	    cache.put(resource);

	    // persist the relation between the resource and its files
	    learnweb.getFileManager().addFilesToResource(resource.getFiles().values(), resource);
	}
	else
	// edited resource needs to be updated in the cache
	{
	    cache.remove(resource.getId());
	    cache.put(resource);
	}

	replace.close();
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#addResource(de.l3s.learnweb.Resource, de.l3s.learnweb.User, java.io.InputStream)
     */

    protected Resource addResource(Resource resource, User user) throws SQLException
    {
	resource.setOwner(user);

	saveResource(resource);

	resource = cache.put(resource);

	// replaces the file placeholders with their urls
	resource.prepareEmbeddedCodes();

	return resource;
    }

    private Comment createComment(ResultSet rs) throws SQLException
    {
	Comment comment = new Comment();
	comment.setId(rs.getInt("comment_id"));
	comment.setResourceId(rs.getInt("resource_id"));
	comment.setUserId(rs.getInt("user_id"));
	comment.setText(rs.getString("text"));
	comment.setDate(new Date(rs.getTimestamp("date").getTime()));
	return comment;
    }

    private static String COMMENT_SELECT = "comment_id, resource_id, user_id, text, date";

    /**
     * @see de.l3s.learnweb.ResourceManager#getCommentsByUserId(int)
     */

    public List<Comment> getCommentsByUserId(int userId) throws SQLException
    {
	List<Comment> comments = new LinkedList<Comment>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_SELECT + " FROM `lw_comment` JOIN lw_resource USING(resource_id) WHERE `user_id` = ? AND deleted = 0");
	select.setInt(1, userId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    comments.add(createComment(rs));
	}
	select.close();

	return comments;
    }

    public List<Comment> getCommentsByUserIds(Collection<Integer> userIds) throws SQLException
    {
	String userIdString = StringHelper.implodeInt(userIds, ",");

	List<Comment> comments = new LinkedList<Comment>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_SELECT + " FROM `lw_comment` JOIN lw_resource USING(resource_id) WHERE `user_id` IN(" + userIdString + ") AND deleted = 0");

	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    comments.add(createComment(rs));
	}
	select.close();

	return comments;
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getCommentsByResourceId(int)
     */

    public List<Comment> getCommentsByResourceId(int id) throws SQLException
    {

	List<Comment> comments = new LinkedList<Comment>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_SELECT + " FROM `lw_comment` WHERE `resource_id` = ?");
	select.setInt(1, id);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    comments.add(createComment(rs));
	}
	select.close();

	return comments;
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getTag(java.lang.String)
     */

    public Tag getTag(String tagName) throws Exception
    {
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name FROM lw_tag WHERE name LIKE ?");
	select.setString(1, tagName);
	ResultSet rs = select.executeQuery();
	if(!rs.next())
	    return null;

	Tag tag = new Tag(rs.getInt("tag_id"), rs.getString("name"));
	select.close();
	return tag;
    }

    protected void deleteTag(Tag tag, Resource resource) throws Exception
    {
	PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_resource_tag WHERE resource_id = ? AND tag_id = ?");
	delete.setInt(1, resource.getId());
	delete.setInt(2, tag.getId());
	System.out.println(delete);
	delete.executeUpdate();
	delete.close();
    }

    protected void deleteComment(Comment comment) throws Exception
    {
	PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_comment WHERE comment_id = ?");
	delete.setInt(1, comment.getId());
	delete.executeUpdate();
	delete.close();
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#getTagsByUserId(int)
     */

    public List<Tag> getTagsByUserId(int userId) throws Exception
    {
	LinkedList<Tag> tags = new LinkedList<Tag>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name FROM `lw_resource_tag` JOIN lw_tag USING(tag_id) JOIN lw_resource USING(resource_id) WHERE `user_id` = ? AND deleted = 0");
	select.setInt(1, userId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    tags.add(new Tag(rs.getInt("tag_id"), rs.getString("name")));
	}
	select.close();
	return tags;
    }

    public OwnerList<Tag, User> getTagsByResource(int resourceId) throws SQLException
    {
	UserManager um = learnweb.getUserManager();
	OwnerList<Tag, User> tags = new OwnerList<Tag, User>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name, user_id, timestamp FROM `lw_resource_tag` JOIN lw_tag USING(tag_id) JOIN lw_resource USING(resource_id) WHERE `resource_id` = ?");
	select.setInt(1, resourceId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    tags.add(new Tag(rs.getInt("tag_id"), rs.getString("name")), um.getUser(rs.getInt("user_id")), rs.getDate("timestamp"));
	}
	select.close();
	return tags;
    }

    /**
     * @throws SQLException
     * @see de.l3s.learnweb.ResourceManager#addTag(java.lang.String)
     */

    protected Tag addTag(String tagName) throws Exception
    {
	Tag tag = new Tag(-1, tagName);

	saveTag(tag);

	return tag;
    }

    /**
     * @throws SQLException
     * @see de.l3s.learnweb.ResourceManager#tagResource(de.l3s.learnweb.Resource, de.l3s.learnweb.Tag, de.l3s.learnweb.User)
     */

    protected void tagResource(Resource resource, Tag tag, User user) throws SQLException
    {
	PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_tag` (`resource_id`, `tag_id`, `user_id`) VALUES (?, ?, ?)");
	replace.setInt(1, null == resource ? 0 : resource.getId());
	replace.setInt(2, tag.getId());
	replace.setInt(3, null == user ? 0 : user.getId());
	replace.executeUpdate();
	replace.close();
    }

    /**
     * @see de.l3s.learnweb.ResourceManager#commentResource(java.lang.String, de.l3s.learnweb.User, de.l3s.learnweb.Resource)
     */

    protected Comment commentResource(String text, User user, Resource resource) throws Exception
    {
	Comment c = new Comment(-1, text, new Date(), resource, user);
	saveComment(c);

	return c;
    }

    private void saveTag(Tag tag) throws SQLException
    {
	PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_tag` (tag_id, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);

	if(tag.getId() < 0) // the tag is not yet stored at the database
	    replace.setNull(1, java.sql.Types.INTEGER);
	else
	    replace.setInt(1, tag.getId());
	replace.setString(2, tag.getName());
	replace.executeUpdate();

	if(tag.getId() < 0) // get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    tag.setId(rs.getInt(1));
	}

	replace.close();
    }

    public void saveComment(Comment comment) throws SQLException
    {
	PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_comment` (" + COMMENT_COLUMNS + ") VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

	if(comment.getId() < 0) // the comment is not yet stored at the database
	    replace.setNull(1, java.sql.Types.INTEGER);
	else
	    replace.setInt(1, comment.getId());
	replace.setInt(2, comment.getResourceId());
	replace.setInt(3, comment.getUserId());
	replace.setString(4, comment.getText());
	replace.setTimestamp(5, new java.sql.Timestamp(comment.getDate().getTime()));
	replace.executeUpdate();

	if(comment.getId() < 0) // get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    comment.setId(rs.getInt(1));
	}

	replace.close();
    }

    public List<Resource> search(String terms, String type, int resultsPerPage, int page) throws Exception
    {
	String typeQuery = "";

	if(type != null)
	{
	    if(type.equals("image") || type.equals("video"))
		typeQuery = "AND type = '" + type + "' AND thumbnail2_width > 0";
	    else if(type.equals("text"))
		typeQuery = "AND type != 'video ' AND type != 'image'";
	    else
		throw new IllegalArgumentException("Invalid type:" + type);
	}

	StringBuilder query = new StringBuilder();
	String[] termsArray = terms.split(" ");
	for(String term : termsArray)
	{
	    term = term.trim();
	    if(term.length() == 0)
		continue;
	    query.append(" +");
	    query.append(term);
	}

	int limit = (page - 1) * resultsPerPage;

	return getResources("SELECT * FROM lw_resource WHERE resource_id > 20000 AND deleted = 0 " + typeQuery + " AND MATCH (title,description,machine_description,filename) AGAINST (? IN BOOLEAN MODE) LIMIT ?, ?", query.toString(), limit, limit + resultsPerPage);
    }

    protected boolean addResourceToGroup(Resource resource, Group targetGroup, User user) throws SQLException
    {
	// TODO prüfen ob schon vorhanden

	PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_group_resource` (`group_id` , `resource_id`, user_id) VALUES (?, ?, ?)");
	insert.setInt(1, targetGroup.getId());
	insert.setInt(2, resource.getId());
	insert.setInt(3, user.getId());
	insert.executeUpdate();
	insert.close();

	resource.clearCaches();
	targetGroup.clearCaches();
	user.clearCaches();

	return true;
    }

    public boolean moveResourceToGroup(Resource resource, Group targetGroup, Group sourceGroup, User user) throws SQLException
    {
	// TODO prüfen ob schon vorhanden
	PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_group_resource` SET `group_id` = ?, user_id = ? WHERE `group_id` = ? AND `resource_id` = ?");
	update.setInt(1, targetGroup.getId());
	update.setInt(2, user.getId());
	update.setInt(3, sourceGroup.getId());
	update.setInt(4, resource.getId());
	update.executeUpdate();
	update.close();

	resource.clearCaches();
	targetGroup.clearCaches();
	user.clearCaches();
	return true;
    }

    protected void removeResourceFromGroup(Resource resource, Group group, User user) throws SQLException
    {
	PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_resource` WHERE `group_id` = ? AND `resource_id` = ?");
	delete.setInt(1, group.getId());
	delete.setInt(2, resource.getId());
	delete.executeUpdate();
	delete.close();

	resource.clearCaches();
	group.clearCaches();
	user.clearCaches();
    }

    public OwnerList<Resource, User> getResourcesByGroupId(int groupId) throws SQLException
    {
	UserManager um = learnweb.getUserManager();

	OwnerList<Resource, User> resources = new OwnerList<Resource, User>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT resource_id, user_id, timestamp FROM `lw_group_resource` WHERE `group_id` = ? ORDER BY resource_id ASC");
	select.setInt(1, groupId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    int userId = rs.getInt(2);
	    Resource r = getResource(rs.getInt(1));
	    User user = userId == 0 ? null : um.getUser(userId);

	    if(null != r)
		resources.add(r, user, rs.getDate(3));
	}
	select.close();

	return resources;
    }

    private Resource createResource(ResultSet rs) throws SQLException
    {
	int id = rs.getInt("resource_id");
	Resource resource = cache.get(id);

	if(null == resource)
	{
	    resource = new Resource();
	    resource.setId(id);
	    resource.setTitle(rs.getString("title"));
	    resource.setDescription(rs.getString("description"));
	    resource.setUrl(rs.getString("url"));
	    resource.setStorageType(rs.getInt("storage_type"));
	    resource.setRights(rs.getInt("rights"));
	    resource.setSource(rs.getString("source"));
	    resource.setAuthor(rs.getString("author"));
	    resource.setType(rs.getString("type"));
	    resource.setFormat(rs.getString("format"));
	    resource.setOwnerUserId(rs.getInt("owner_user_id"));
	    resource.setRatingSum(rs.getInt("rating"));
	    resource.setRateNumber(rs.getInt("rate_number"));
	    resource.setEmbeddedSize1Raw(rs.getString("embedded_size1"));
	    if(resource.getEmbeddedSize1Raw() == null || resource.getEmbeddedSize1Raw().length() < 5)
		resource.setEmbeddedSize1Raw(rs.getString("embedded_size2"));
	    resource.setEmbeddedSize3Raw(rs.getString("embedded_size3"));
	    resource.setEmbeddedSize4Raw(rs.getString("embedded_size4"));
	    resource.setFileName(rs.getString("filename"));
	    resource.setMaxImageUrl(rs.getString("max_image_url"));
	    resource.setQuery(rs.getString("query"));
	    resource.setOriginalResourceId(rs.getInt("original_resource_id"));
	    resource.setAccess(rs.getString("access"));
	    resource.setThumbnail0(createThumbnail(rs, 0));
	    resource.setThumbnail1(createThumbnail(rs, 1));
	    resource.setThumbnail2(createThumbnail(rs, 2));
	    resource.setThumbnail3(createThumbnail(rs, 3));
	    resource.setThumbnail4(createThumbnail(rs, 4));
	    resource.setEmbeddedRaw(rs.getString("embeddedRaw"));
	    resource.setTranscript(rs.getString("transcript"));
	    resource.setOnlineStatus(Resource.OnlineStatus.valueOf(rs.getString("online_status")));
	    resource.setIdAtService(rs.getString("id_at_service"));
	    resource.setDuration(rs.getInt("duration"));
	    resource.setArchiveUrl(rs.getString("archive_url"));
	    if(resource.getSource().equals("TED")) // This must be set manually because we store all TED videos in Learnweb/Solr
		resource.setLocation("TED");
	    else
		resource.setLocation("Learnweb");

	    List<File> files = learnweb.getFileManager().getFilesByResource(resource.getId());
	    for(File file : files)
	    {
		resource.addFile(file);
		if(file.getResourceFileNumber() == File.ORIGINAL_FILE)
		    resource.setUrl(file.getUrl());
	    }

	    resource.prepareEmbeddedCodes();
	    resource = cache.put(resource);
	}
	return resource;
    }

    private Thumbnail createThumbnail(ResultSet rs, int thumbnailSize) throws SQLException
    {
	String prefix = "thumbnail" + thumbnailSize + "_";
	String url = rs.getString(prefix + "url");
	int fileId = rs.getInt(prefix + "file_id");

	if(fileId != 0)
	{
	    File file = learnweb.getFileManager().getFileById(fileId);
	    url = file.getUrl();
	}
	else if(url == null)
	{
	    return null;
	}

	return new Thumbnail(url, rs.getInt(prefix + "width"), rs.getInt(prefix + "height"), fileId);
    }

    /**
     * 
     * @param query
     * @param param1 set to null if no parameter
     * @param params
     * @return
     * @throws SQLException
     */
    public List<Resource> getResources(String query, String param1, int... params) throws SQLException
    {
	List<Resource> resources = new LinkedList<Resource>();
	PreparedStatement select = learnweb.getConnection().prepareStatement(query);

	int i = 1;
	if(null != param1)
	    select.setString(i++, param1);

	for(int param : params)
	    select.setInt(i++, param);

	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    resources.add(createResource(rs));
	}
	select.close();

	return resources;
    }

    public static void main(String[] args) throws Exception
    {
	createThumbnailsForTEDVideos();
	//createThumbnailsForWebResources();
    }

    public static void createThumbnailsForWebResources() throws Exception
    {

	Learnweb lw = Learnweb.getInstance();
	ResourceManager rm = new ResourceManager(lw);
	// ResourcePreviewMaker pm = lw.getResourcePrevewMaker();

	List<Resource> resources = rm
		.getResources(
			"SELECT "
				+ RESOURCE_COLUMNS
				+ " FROM `lw_resource` r where  `deleted` = 0 AND `storage_type` = 2 AND `type` NOT IN ('image','video') and r.`resource_id` > 20000 and type !='pdf' and source != 'SlideShare' and thumbnail2_file_id=0 and online_status = 'unknown' ORDER BY `resource_id` DESC limit 1000",
			null);

	ResourcePreviewMaker rpm = lw.getResourcePreviewMaker();
	for(Resource resource : resources)
	{
	    System.out.println(resource);
	    String url = FileInspector.checkUrl(resource.getUrl());

	    if(null == url)
	    {
		System.err.println("invalid url");

		resource.setOnlineStatus(OnlineStatus.OFFLINE); // offline
		resource.save();
		continue;
	    }

	    if(url.contains("ted.com") || url.contains("youtube") || url.contains("vimeo") || url.contains("slideshare") || url.contains("flickr") || resource.getId() == 71989 || resource.getId() == 71536 || resource.getId() == 71100)
	    {
		System.err.println("skipeed: " + url);
		continue;
	    }

	    //System.out.println(url);

	    //System.out.println(IOUtils.toString(FileInspector.openStream(url)));
	    try
	    {
		FileInfo info = new FileInspector().inspect(FileInspector.openStream(url), "unknown");

		if(info.getMimeType().equals("text/html") || info.getMimeType().equals("text/plain") || info.getMimeType().equals("application/xhtml+xml") || info.getMimeType().equals("application/octet-stream") || info.getMimeType().equals("blog-post"))
		{
		    resource.setMachineDescription(info.getTextContent());
		    resource.setUrl(url);

		    rpm.processWebsite(resource);
		    resource.setOnlineStatus(OnlineStatus.ONLINE);
		    if(resource.getSource() == null)
			resource.setSource("Internet");

		    resource.save();

		}
		else if(info.getMimeType().equals("application/pdf"))
		{
		    System.out.println("process " + info.getMimeType());
		    resource.setMachineDescription(info.getTextContent());

		    rpm.processFile(resource, FileInspector.openStream(url), info);
		    resource.save();
		}
		else if(info.getMimeType().startsWith("image/"))
		{
		    rpm.processImage(resource, FileInspector.openStream(url));
		    resource.setFormat(info.getMimeType());
		    resource.setType("Image");
		    resource.save();
		}
		else
		    System.err.println(info.getMimeType());

		System.out.println("--------------");
	    }
	    catch(Exception e)
	    {

		log.error(e);

		resource.setOnlineStatus(OnlineStatus.OFFLINE); // offline
		resource.save();
	    }
	    //

	    //lw.getFileManager().delete(null);
	}
    }

    public static void createThumbnailsForTEDVideos() throws Exception
    {

	Learnweb lw = Learnweb.getInstance();
	ResourceManager rm = new ResourceManager(lw);
	// ResourcePreviewMaker pm = lw.getResourcePrevewMaker();

	List<Resource> resources = rm.getResources("SELECT " + RESOURCE_COLUMNS + " FROM `lw_resource` r where `deleted` = 0 AND  source = 'ted' and thumbnail2_file_id=0  ORDER BY `resource_id` DESC limit 10", null);

	ResourcePreviewMaker rpm = lw.getResourcePreviewMaker();
	for(Resource resource : resources)
	{
	    System.out.println(resource.getUrl());
	    if(resource.getType().equals("Video") || resource.getType().equals("Image"))
	    {
		if(resource.getThumbnail4().getUrl() != null)
		    rpm.processImage(resource, FileInspector.openStream(resource.getThumbnail4().getUrl()));

		resource.save();
	    }
	    else
		System.out.println(resource.getType());
	}
    }
}

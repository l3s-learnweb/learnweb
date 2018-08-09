package de.l3s.learnweb.resource;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.interwebj.jaxb.SearchResultEntity;
import de.l3s.interwebj.jaxb.ThumbnailEntity;
import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource.ResourceType;
import de.l3s.learnweb.resource.glossaryNew.GlossaryResource;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.resource.yellMetadata.AudienceManager;
import de.l3s.learnweb.resource.yellMetadata.Category;
import de.l3s.learnweb.resource.yellMetadata.CategoryManager;
import de.l3s.learnweb.resource.yellMetadata.LangLevelManager;
import de.l3s.learnweb.resource.yellMetadata.PurposeManager;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.PropertiesBundle;
import de.l3s.util.Sql;
import de.l3s.util.StringHelper;

public class ResourceManager
{
    private final static String COMMENT_COLUMNS = "`comment_id`, `resource_id`, `user_id`, `text`, `date`";
    public final static String RESOURCE_COLUMNS = "r.read_only_transcript, r.deleted, r.resource_id, r.title, r.description, r.url, r.storage_type, r.rights, r.source, r.language, r.type, r.format, r.owner_user_id, r.rating, r.rate_number, r.filename, r.max_image_url, r.query, r.original_resource_id, r.author, r.file_url, r.thumbnail0_url, r.thumbnail0_file_id, r.thumbnail0_width, r.thumbnail0_height, r.thumbnail1_url, r.thumbnail1_file_id, r.thumbnail1_width, r.thumbnail1_height, r.thumbnail2_url, r.thumbnail2_file_id, r.thumbnail2_width, r.thumbnail2_height, r.thumbnail3_url, r.thumbnail3_file_id, r.thumbnail3_width, r.thumbnail3_height, r.thumbnail4_url, r.thumbnail4_file_id, r.thumbnail4_width, r.thumbnail4_height, r.embeddedRaw, r.transcript, r.online_status, r.id_at_service, r.duration, r.restricted, r.resource_timestamp, r.creation_date, r.metadata, r.group_id, r.folder_id, r.mtype, r.msource";

    private final static Logger log = Logger.getLogger(ResourceManager.class);

    private final Learnweb learnweb;

    private ICache<Resource> cache;

    private boolean reindexMode = false; // if this flag is true some performance optimizations for reindexing all resources are enabled

    public enum Order
    {
        TITLE,
        TYPE,
        DATE
    }

    public ResourceManager(Learnweb learnweb)
    {
        PropertiesBundle properties = learnweb.getProperties();
        int cacheSize = properties.getPropertyIntValue("RESOURCE_CACHE");

        this.learnweb = learnweb;
        this.cache = cacheSize == 0 ? new DummyCache<>() : new Cache<>(cacheSize);

    }

    public int getResourceCount() throws SQLException
    {
        Long count = (Long) Sql.getSingleResult("SELECT COUNT(*) FROM lw_resource WHERE deleted = 0");
        return count.intValue();
    }

    public int getResourceCountByUserId(int userId) throws SQLException
    {
        Long count = (Long) Sql.getSingleResult("SELECT COUNT(*) FROM lw_resource r WHERE owner_user_id = " + userId + " AND deleted = 0");
        return count.intValue();
    }

    public int getResourceCountByGroupId(int groupId) throws SQLException
    {
        Long count = (Long) Sql.getSingleResult("SELECT COUNT(*) FROM lw_resource r WHERE group_id = " + groupId + " AND deleted = 0");
        return count.intValue();
    }

    public List<Resource> getResourcesByUserId(int userId) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE owner_user_id = ? AND deleted = 0", null, userId);
    }

    public List<Resource> getGlossaryResourcesByUserId(int userId) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE owner_user_id = ? AND deleted = 0 AND type = 'glossary'", null, userId);
    }

    public List<Resource> getResourcesByTagId(int tagId) throws SQLException
    {
        return getResourcesByTagId(tagId, 1000);
    }

    /**
     * @see de.l3s.learnweb.resource.ResourceManager#getResourcesByTagId(int, int)
     */

    public List<Resource> getResourcesByTagId(int tagId, int maxResults) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_tag USING ( resource_id ) WHERE tag_id = ? AND deleted = 0 LIMIT ? ", null, tagId, maxResults);
    }

    public List<Resource> getRatedResourcesByUserId(int userId) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_rating USING ( resource_id ) WHERE user_id = ? AND deleted = 0 ", null, userId);
    }

    /**
     * Returns all resources (that were not deleted)
     */
    public List<Resource> getResourcesAll(int page, int pageSize) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE `deleted` = 0 ORDER BY resource_id LIMIT " + (page * pageSize) + "," + pageSize + "", null);
    }

    public boolean isResourceRatedByUser(int resourceId, int userId) throws SQLException
    {
        PreparedStatement stmt = learnweb.getConnection().prepareStatement("SELECT 1 FROM lw_resource_rating WHERE resource_id =  ? AND user_id = ?");
        stmt.setInt(1, resourceId);
        stmt.setInt(2, userId);
        ResultSet rs = stmt.executeQuery();
        boolean response = rs.next();
        stmt.close();
        return response;
    }

    protected void rateResource(int resourceId, int userId, int value) throws SQLException
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

    protected void loadThumbRatings(Resource resource) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT SUM(IF(direction=1,1,0)) as positive, SUM(IF(direction=-1,1,0)) as negative FROM `lw_thumb` WHERE `resource_id` = ?");
        select.setInt(1, resource.getId());
        ResultSet rs = select.executeQuery();

        if(rs.next())
        {
            resource.setThumbUp(rs.getInt(1));
            resource.setThumbDown(rs.getInt(2));
        }
        else
            log.warn("no results for id: " + resource.getId());

        select.close();
    }

    public LogEntry loadThumbnailUpdateInfo(int resourceId) throws SQLException
    {
        LogEntry e = null;

        PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT user_id, u.username, action, target_id, params, timestamp, ul.group_id, r.title AS resource_title, g.title AS group_title, u.image_file_id FROM lw_user_log ul JOIN lw_user u USING(user_id) JOIN lw_resource r ON action=45 AND target_id = r.resource_id JOIN lw_group g ON ul.group_id = g.group_id WHERE r.resource_id = ? ORDER BY timestamp DESC LIMIT 1");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();

        if(rs.next())
        {
            e = new LogEntry(rs);
        }

        return e;
    }

    /**
     * @see de.l3s.learnweb.resource.ResourceManager#isResourceThumbRatedByUser(int, int)
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

    private Resource getResource(int resourceId, boolean useCache) throws SQLException
    {
        Resource resource = cache.get(resourceId);

        if(null != resource && useCache)
            return resource;

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + RESOURCE_COLUMNS + " FROM `lw_resource` r WHERE resource_id = ?"); //  and deleted = 0
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
            return null;

        resource = createResource(rs);
        select.close();

        return resource;
    }

    public Resource getResource(int resourceId) throws SQLException
    {
        return getResource(resourceId, true);
    }

    public void deleteResource(Resource resource) throws SQLException
    {
        deleteResource(resource.getId());
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

        // flag the resource as deleted
        PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_resource` SET deleted = 1 WHERE `resource_id` = ?");
        update.setInt(1, resourceId);
        update.executeUpdate();
        update.close();

        /* this causes problems because a thumbnail can be used by multiple resources when they were copied
        *
        update = Learnweb.getConnectionStatic().prepareStatement("UPDATE `lw_file` SET deleted = 1 WHERE `resource_id` = ?");
        update.setInt(1, resourceId);
        update.executeUpdate();
        update.close();
        */

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
    public void deleteResourcePermanent(int resourceId) throws SQLException
    {
        deleteResource(resourceId);

        Connection connection = learnweb.getConnection();

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

        // delete archived versions
        delete = connection.prepareStatement("DELETE FROM `lw_resource_archiveurl` WHERE `resource_id` = ?");
        delete.setInt(1, resourceId);
        delete.executeUpdate();
        delete.close();

        // delete files?

        // remove resource from cache
        cache.remove(resourceId);
    }

    protected Resource saveResource(Resource resource) throws SQLException
    {
        if(resource.getUserId() <= 0)
            throw new IllegalArgumentException("Resource has no owner");

        String query = "REPLACE INTO `lw_resource` (`resource_id` ,`title` ,`description` ,`url` ,`storage_type` ,`rights` ,`source` ,`type` ,`format` ,`owner_user_id` ,`rating` ,`rate_number` ,`query`, filename, max_image_url, original_resource_id, machine_description, author, file_url, thumbnail0_url, thumbnail0_file_id, thumbnail0_width, thumbnail0_height, thumbnail1_url, thumbnail1_file_id, thumbnail1_width, thumbnail1_height, thumbnail2_url, thumbnail2_file_id, thumbnail2_width, thumbnail2_height, thumbnail3_url, thumbnail3_file_id, thumbnail3_width, thumbnail3_height, thumbnail4_url, thumbnail4_file_id, thumbnail4_width, thumbnail4_height, embeddedRaw, transcript, online_status, id_at_service, duration, restricted, language, creation_date, metadata, group_id, folder_id, deleted, read_only_transcript, mtype, msource) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement replace = learnweb.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        if(resource.getId() < 0) // the Resource is not yet stored at the database
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, resource.getId());
        replace.setString(2, resource.getTitle());
        replace.setString(3, resource.getDescription());
        replace.setString(4, resource.getUrl());
        replace.setInt(5, resource.getStorageType());
        replace.setInt(6, resource.getRights());
        replace.setString(7, resource.getSource().name());
        replace.setString(8, resource.getType().name());
        replace.setString(9, resource.getFormat());
        replace.setInt(10, resource.getUserId());
        replace.setInt(11, resource.getRatingSum());
        replace.setInt(12, resource.getRateNumber());
        replace.setString(13, resource.getQuery());
        replace.setString(14, resource.getFileName());
        replace.setString(15, resource.getMaxImageUrl());
        replace.setInt(16, resource.getOriginalResourceId());
        replace.setString(17, resource.getMachineDescription());
        replace.setString(18, resource.getAuthor());
        replace.setString(19, resource.getFileUrl());

        Thumbnail[] thumbnails = { resource.getThumbnail0(), resource.getThumbnail1(), resource.getThumbnail2(), resource.getThumbnail3(), resource.getThumbnail4() };

        for(int i = 0, m = 20; i < 5; i++)
        {
            String url = null;
            int fileId = 0;
            int width = 0;
            int height = 0;

            Thumbnail tn = thumbnails[i];
            if(tn != null) // a thumbnail is defined
            {
                if(tn.getFileId() == 0) // TODO in the future don't save urls
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

        replace.setString(40, resource.getEmbeddedRaw());
        replace.setString(41, resource.getTranscript());
        replace.setString(42, resource.getOnlineStatus().name());
        replace.setString(43, resource.getIdAtService());
        replace.setInt(44, resource.getDuration());
        replace.setInt(45, resource.isRestricted() ? 1 : 0);
        replace.setString(46, resource.getLanguage());
        replace.setTimestamp(47, resource.getCreationDate() == null ? null : new java.sql.Timestamp(resource.getCreationDate().getTime()));
        Sql.setSerializedObject(replace, 48, resource.getMetadata());
        replace.setInt(49, resource.getGroupId());
        replace.setInt(50, resource.getFolderId());
        replace.setInt(51, resource.isDeleted() ? 1 : 0);
        replace.setInt(52, resource.isReadOnlyTranscript() ? 1 : 0);
        //added by Chloe: adding mtype and msource
        replace.setString(53, resource.getMtype());
        replace.setString(54, resource.getMsource());

        replace.executeUpdate();

        if(resource.getId() < 0) // get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            resource.setId(rs.getInt(1));
            resource.setLocation(getLocation(resource));
            cache.put(resource);

            // persist the relation between the resource and its files
            learnweb.getFileManager().addFilesToResource(resource.getFiles().values(), resource);
        }
        else // edited resources need to be updated in the cache
        {
            cache.remove(resource.getId());
            cache.put(resource);
            resource.clearCaches();
        }

        replace.close();

        learnweb.getSolrClient().reIndexResource(resource);

        return resource;
    }

    public Resource addResource(Resource resource, User user) throws SQLException
    {
        resource.setUser(user);

        saveResource(resource);

        try
        {
            //To copy archive versions of a resource if it exists
            saveArchiveUrlsByResourceId(resource.getId(), resource.getArchiveUrls());
        }
        catch(Exception e)
        {
            log.error("Can't save archiveUrls", e);
        }
        resource = cache.put(resource);

        resource.setDefaultThumbnailIfNull();

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
     * @see de.l3s.learnweb.resource.ResourceManager#getCommentsByUserId(int)
     */

    public List<Comment> getCommentsByUserId(int userId) throws SQLException
    {
        List<Comment> comments = new LinkedList<>();

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

        List<Comment> comments = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_SELECT + " FROM `lw_comment` JOIN lw_resource USING(resource_id) WHERE `user_id` IN(" + userIdString + ") AND deleted = 0");

        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            comments.add(createComment(rs));
        }
        select.close();

        return comments;
    }

    public Comment getComment(int commentId) throws SQLException
    {
        Comment comment = null;

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_SELECT + " FROM `lw_comment` WHERE `comment_id` = ?");
        select.setInt(1, commentId);
        ResultSet rs = select.executeQuery();

        if(rs.next())
        {
            comment = createComment(rs);
        }
        select.close();

        return comment;
    }

    public List<Comment> getCommentsByResourceId(int id) throws SQLException
    {
        List<Comment> comments = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_SELECT + " FROM `lw_comment` WHERE `resource_id` = ? ORDER BY date DESC");
        select.setInt(1, id);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            comments.add(createComment(rs));
        }
        select.close();

        return comments;
    }

    public Tag getTag(String tagName) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name FROM lw_tag WHERE name LIKE ? ORDER BY tag_id LIMIT 1");
        select.setString(1, tagName);
        ResultSet rs = select.executeQuery();
        if(!rs.next())
            return null;

        Tag tag = new Tag(rs.getInt("tag_id"), rs.getString("name"));
        select.close();
        return tag;
    }

    protected void deleteTag(Tag tag, Resource resource) throws SQLException
    {
        PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_resource_tag WHERE resource_id = ? AND tag_id = ?");
        delete.setInt(1, resource.getId());
        delete.setInt(2, tag.getId());
        delete.executeUpdate();
        delete.close();
    }

    protected void deleteComment(Comment comment) throws SQLException
    {
        PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_comment WHERE comment_id = ?");
        delete.setInt(1, comment.getId());
        delete.executeUpdate();
        delete.close();
    }

    /**
     * @see de.l3s.learnweb.resource.ResourceManager#getTagsByUserId(int)
     */

    public List<Tag> getTagsByUserId(int userId) throws SQLException
    {
        LinkedList<Tag> tags = new LinkedList<>();

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
        OwnerList<Tag, User> tags = new OwnerList<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name, user_id, timestamp FROM `lw_resource_tag` JOIN lw_tag USING(tag_id) JOIN lw_resource USING(resource_id) WHERE `resource_id` = ?"))
        {
            select.setInt(1, resourceId);
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                tags.add(new Tag(rs.getInt("tag_id"), rs.getString("name")), um.getUser(rs.getInt("user_id")), new Date(rs.getTimestamp("timestamp").getTime()));
            }
        }
        return tags;

    }

    public Tag addTag(String tagName) throws SQLException
    {
        Tag tag = new Tag(-1, tagName);

        saveTag(tag);

        return tag;
    }

    public void tagResource(Resource resource, Tag tag, User user) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_tag` (`resource_id`, `tag_id`, `user_id`) VALUES (?, ?, ?)");
        replace.setInt(1, null == resource ? 0 : resource.getId());
        replace.setInt(2, tag.getId());
        replace.setInt(3, null == user ? 0 : user.getId());
        replace.executeUpdate();
        replace.close();
    }

    protected Comment commentResource(String text, User user, Resource resource) throws SQLException
    {
        Comment c = new Comment(text, new Date(), resource, user);
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

    /**
     * Extracts the plain resources from a list of decorated resources
     *
     * @param resources
     * @return
     */
    public static List<Resource> convertDecoratedResources(List<ResourceDecorator> resources)
    {
        ArrayList<Resource> output = new ArrayList<>(resources.size());

        for(ResourceDecorator decoratedResource : resources)
            output.add(decoratedResource.getResource());

        return output;
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

    public LinkedList<ArchiveUrl> getArchiveUrlsByResourceId(int id) throws SQLException
    {

        LinkedList<ArchiveUrl> archiveUrls = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT archive_url, timestamp FROM `lw_resource_archiveurl` WHERE `resource_id` = ? ORDER BY timestamp");
        select.setInt(1, id);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            archiveUrls.add(new ArchiveUrl(rs.getString("archive_url"), rs.getTimestamp("timestamp")));
        }
        select.close();
        return archiveUrls;
    }

    public LinkedList<ArchiveUrl> getArchiveUrlsByResourceUrl(String url) throws SQLException
    {
        SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        LinkedList<ArchiveUrl> archiveUrls = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT url_id FROM `wb_url` WHERE `url` = ?");
        select.setString(1, url);
        ResultSet rs = select.executeQuery();
        if(rs.next())
        {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT timestamp FROM `wb_url_capture` WHERE `url_id` = ? ORDER BY timestamp");
            pStmt.setInt(1, rs.getInt(1));
            ResultSet rs2 = pStmt.executeQuery();
            while(rs2.next())
            {
                Date timestamp = new Date(rs2.getTimestamp(1).getTime());
                archiveUrls.add(new ArchiveUrl("https://web.archive.org/web/" + waybackDateFormat.format(timestamp) + "/" + url, timestamp));
            }
            pStmt.close();
        }
        select.close();
        return archiveUrls;
    }

    public void saveArchiveUrlsByResourceId(int resourceId, List<ArchiveUrl> archiveUrls) throws SQLException
    {
        for(ArchiveUrl version : archiveUrls)
        {
            PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("INSERT into lw_resource_archiveurl(`resource_id`,`archive_url`,`timestamp`) VALUES (?,?,?)");
            prepStmt.setInt(1, resourceId);
            prepStmt.setString(2, version.getArchiveUrl());
            prepStmt.setTimestamp(3, new java.sql.Timestamp(version.getTimestamp().getTime()));
            prepStmt.executeUpdate();
            prepStmt.close();
        }
    }

    /*
     * not used
     *
    public AbstractPaginator getResourcesByGroupId(int groupId, Order order) throws SQLException
    {
        int results = getResourceCountByGroupId(groupId);
    
        return new GroupPaginator(results, groupId, order);
    }
    
    private static class GroupPaginator extends AbstractPaginator
    {
        private static final long serialVersionUID = 399863025926697377L;
        private final int groupId;
        private final Order order;
    
        public GroupPaginator(int totalResults, int groupId, Order order)
        {
            super(totalResults);
            this.groupId = groupId;
            this.order = order;
        }
    
        @Override
        public List<ResourceDecorator> getCurrentPage() throws SQLException, SolrServerException
        {
            return Learnweb.getInstance().getResourceManager().getResourcesByGroupId(groupId, getPageIndex(), PAGE_SIZE, order);
        }
    }
    */

    public List<Resource> getResourcesByGroupId(int groupId) throws SQLException
    {
        List<Resource> resources = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE `group_id` = ? and deleted = 0");
        select.setInt(1, groupId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            Resource resource = createResource(rs);

            if(null != resource)
                resources.add(resource);
        }
        select.close();

        return resources;
    }

    /*
    public OwnerList<Resource, User> getResourcesByGroupId(int groupId, int page, int pageSize, Order order) throws SQLException
    {
    OwnerList<Resource, User> resources = new OwnerList<Resource, User>();
    
    PreparedStatement select = learnweb.getConnection().prepareStatement(
    	"SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE `group_id` = ? ORDER BY resource_id ASC LIMIT ? OFFSET ? ");
    select.setInt(1, groupId);
    select.setInt(2, pageSize);
    select.setInt(3, page * pageSize);
    ResultSet rs = select.executeQuery();
    while(rs.next())
    {
        Resource resource = createResource(rs);
    
        if(null != resource)
    	resources.add(resource.getOwnerUser(), resource.getCreationDate());
    }
    select.close();
    
    return resources;
    }
    */

    public List<ResourceDecorator> getResourcesByGroupId(int groupId, int page, int pageSize, Order order) throws SQLException
    {
        List<ResourceDecorator> resources = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE `group_id` = ? ORDER BY resource_id ASC LIMIT ? OFFSET ? ");
        select.setInt(1, groupId);
        select.setInt(2, pageSize);
        select.setInt(3, page * pageSize);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            Resource resource = createResource(rs);

            ResourceDecorator decoratedResource = new ResourceDecorator(resource);

            resources.add(decoratedResource);
            /*
            if(null != resource)
            resources.add(resource, resource.getOwnerUser(), resource.getCreationDate());
            */
        }
        select.close();

        return resources;
    }

    /**
     * Returns the the location were a resource is stored. Necessary because some external sources are indexed in our Solr instance
     *
     * @param resource
     * @return
     */
    private static String getLocation(Resource resource)
    {
        SERVICE source = resource.getSource();

        if(null == source)
        {
            log.warn("Empty source for resource: " + resource, new IllegalArgumentException());
            return "Learnweb";
        }

        switch(source)
        {
        case teded:
        case ted:
        case tedx:
        case yovisto:
        case archiveit:
        case factcheck:
            return source.toString();
        default:
            return "Learnweb";
        }
    }

    /**
     * Creates appropriate Resource instances based on the resource type.
     * Necessary since some resource types extend the normal Resource class.
     *
     * @param type
     * @return
     */
    private static Resource newResource(ResourceType type)
    {
        switch(type)
        {
        case survey:
            return new SurveyResource();
        case glossary2:
            return new GlossaryResource();
        default:
            return new Resource();
        }
    }

    private Resource createResource(ResultSet rs) throws SQLException
    {
        int id = rs.getInt("resource_id");
        Resource resource = cache.get(id);

        if(null == resource)
        {
            ResourceType type = ResourceType.parse(rs.getString("type"));

            resource = newResource(type);
            resource.setId(id);
            resource.setFormat(rs.getString("format"));
            resource.setType(type);
            resource.setTitle(rs.getString("title"));
            resource.setDescription(rs.getString("description"));
            resource.setUrl(rs.getString("url"));
            resource.setStorageType(rs.getInt("storage_type"));
            resource.setRights(rs.getInt("rights"));
            resource.setSource(rs.getString("source"));
            resource.setAuthor(rs.getString("author"));
            resource.setUserId(rs.getInt("owner_user_id"));
            resource.setRatingSum(rs.getInt("rating"));
            resource.setRateNumber(rs.getInt("rate_number"));
            resource.setFileName(rs.getString("filename"));
            resource.setMaxImageUrl(rs.getString("max_image_url"));
            resource.setQuery(rs.getString("query"));
            resource.setOriginalResourceId(rs.getInt("original_resource_id"));
            resource.setFileUrl(rs.getString("file_url"));
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
            resource.setLanguage(rs.getString("language"));
            resource.setRestricted(rs.getInt("restricted") == 1);
            resource.setResourceTimestamp(rs.getTimestamp("resource_timestamp"));
            resource.setCreationDate(rs.getTimestamp("creation_date") == null ? null : new Date(rs.getTimestamp("creation_date").getTime()));
            resource.setGroupId(rs.getInt("group_id"));
            resource.setFolderId(rs.getInt("folder_id"));
            resource.setDeleted(rs.getInt("deleted") == 1);
            resource.setReadOnlyTranscript(rs.getInt("read_only_transcript") == 1);
            resource.setMtype(rs.getString("mtype"));
            resource.setMsource(rs.getString("msource"));

            // This must be set manually because we stored some external sources in Learnweb/Solr
            resource.setLocation(getLocation(resource));

            // TODO move to glossaryResource class
            if(resource.getType() != null && resource.getType().equals(ResourceType.glossary))
                resource.setUrl(learnweb.getServerUrl() + "/lw/showGlossary.jsf?resource_id=" + Integer.toString(resource.getId()));

            if(resource.isDeleted())
                log.debug("resource " + resource.getId() + " was requested but is deleted");
            else if(!isReindexMode())
            {
                List<File> files = learnweb.getFileManager().getFilesByResource(resource.getId());

                for(File file : files)
                {
                    resource.addFile(file);
                    if(file.getType().equals(TYPE.FILE_MAIN))
                    {
                        resource.setFileUrl(file.getUrl());
                        resource.setFileName(file.getName());

                        if(resource.getStorageType() == Resource.LEARNWEB_RESOURCE)
                            resource.setUrl(file.getUrl());
                    }
                }
            }

            // deserialize metadata
            byte[] metadataBytes = rs.getBytes("metadata");

            if(metadataBytes != null && metadataBytes.length > 0)
            {
                ByteArrayInputStream metadataBAIS = new ByteArrayInputStream(metadataBytes);

                try
                {
                    ObjectInputStream metadataOIS = new ObjectInputStream(metadataBAIS);

                    // re-create the object
                    Object metadata = metadataOIS.readObject();

                    if(metadata != null)
                        resource.setMetadata(metadata);
                }
                catch(Exception e)
                {
                    log.error("Couldn't load metadata for resource " + resource.getId(), e);
                }
            }

            resource.postConstruct();
            resource = cache.put(resource);
        }
        return resource;
    }

    private Thumbnail createThumbnail(ResultSet rs, int thumbnailSize) throws SQLException
    {
        String prefix = "thumbnail" + thumbnailSize;
        String url = rs.getString(prefix + "_url");
        int fileId = rs.getInt(prefix + "_file_id");

        if(fileId != 0)
        {
            url = learnweb.getFileManager().createUrl(fileId, prefix + ".png");
        }
        else if(url == null)
        {
            return null;
        }

        return new Thumbnail(url, rs.getInt(prefix + "_width"), rs.getInt(prefix + "_height"), fileId);
    }

    /**
     * @param query
     * @param param1 set to null if no parameter
     * @param params
     * @return
     * @throws SQLException
     */
    public List<Resource> getResources(String query, String param1, int... params) throws SQLException
    {
        List<Resource> resources = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement(query);

        int i = 1;
        if(null != param1)
            select.setString(i++, param1);

        for(int param : params)
            select.setInt(i++, param);

        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            //log.debug(rs.getInt("resource_id"));
            resources.add(createResource(rs));
        }
        select.close();

        return resources;
    }

    public void resetCache()
    {
        for(Resource resource : cache.getValues())
            resource.clearCaches();

        cache.clear();
    }

    /**
     * @return number of cached objects
     */
    public int getCacheSize()
    {
        return cache.size();
    }

    public static Resource getResourceFromInterwebResult(SearchResultEntity searchResult)
    {
        Resource resource = new Resource();
        resource.setType(ResourceType.valueOf(searchResult.getType()));
        resource.setTitle(searchResult.getTitle());
        resource.setLocation(searchResult.getService());
        resource.setSource(searchResult.getService());
        //resource.setViews(searchResult.getNumberOfViews());
        resource.setIdAtService(searchResult.getIdAtService());
        resource.setDuration(searchResult.getDuration());
        resource.setDescription(searchResult.getDescription());
        resource.setUrl(StringHelper.urlDecode(searchResult.getUrl()));

        if(resource.getTitle().equals(resource.getDescription())) // delete description when equal to title
            resource.setDescription("");

        if(!resource.getType().equals(Resource.ResourceType.image))
        {
            resource.setEmbeddedRaw(searchResult.getEmbeddedSize4());
            if(null == resource.getEmbeddedRaw())
                resource.setEmbeddedRaw(searchResult.getEmbeddedSize3());
        }

        ThumbnailEntity biggestThumbnail = null;
        int biggestThumbnailHeight = 0;

        List<ThumbnailEntity> thumbnails = searchResult.getThumbnailEntities();

        for(ThumbnailEntity thumbnailElement : thumbnails)
        {
            String url = thumbnailElement.getUrl();

            int height = thumbnailElement.getHeight();
            int width = thumbnailElement.getWidth();

            if(height > biggestThumbnailHeight)
            {
                biggestThumbnailHeight = height;
                biggestThumbnail = thumbnailElement;
            }
            // ipernity api doesn't return largest available thumbnail, so we have to guess it
            if(searchResult.getService().equals("Ipernity") && url.contains(".560."))
            {
                if(width == 560 || height == 560)
                {
                    double ratio = 640.0 / 560.;
                    width *= ratio;
                    height *= ratio;

                    url = url.replace(".560.", ".640.");
                }
            }

            Thumbnail thumbnail = new Thumbnail(url, width, height);

            if(thumbnail.getHeight() <= 100 && thumbnail.getWidth() <= 100)
                resource.setThumbnail0(thumbnail);
            else if(thumbnail.getHeight() < 170 && thumbnail.getWidth() < 170)
            {
                thumbnail = thumbnail.resize(120, 100);
                resource.setThumbnail1(thumbnail);
            }
            else if(thumbnail.getHeight() < 500 && thumbnail.getWidth() < 500)
            {
                resource.setThumbnail2(thumbnail.resize(300, 220));
            }
            else
            //if(thumbnail.getHeight() < 600 && thumbnail.getWidth() < 600)
            {
                resource.setThumbnail4(thumbnail);
            }
        }

        // remove old bing images first
        if(biggestThumbnail != null)
        {
            resource.setMaxImageUrl(biggestThumbnail.getUrl());

            if(resource.getThumbnail2() == null)
                resource.setThumbnail2(new Thumbnail(biggestThumbnail.getUrl(), biggestThumbnail.getWidth(), biggestThumbnail.getHeight()));
        }
        else if(!searchResult.getType().equals("website"))
        {
            log.warn("no image url for: " + searchResult.toString());
        }
        return resource;
    }

    public List<Resource> getResourcesByFolderId(int folderId) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r  WHERE folder_id = ? AND deleted = 0", null, folderId);
    }

    public List<Resource> getFolderResourcesByUserId(int groupId, int folderId, int userId, int limit) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r  WHERE group_id = ? AND folder_id = ? AND owner_user_id = ? AND deleted = 0 LIMIT ?", null, groupId, folderId, userId, limit);
    }

    /*
     * New resource sql queries for extended metadata (Chloe)
     */

    //queries regarding table: lw_rm_audience and lw_resource_audience
    public List<Resource> getResourcesByAudienceId(int audienceId) throws SQLException
    {
        return getResourcesByAudienceId(audienceId, 1000);
    }

    public List<Resource> getResourcesByAudienceId(int audienceId, int maxResults) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_audience USING ( resource_id ) WHERE audience_id = ? AND deleted = 0 LIMIT ? ", null, audienceId, maxResults);
    }

    //queries regarding table: lw_rm_langlevel and lw_resource_langlevel
    public List<Resource> getResourcesByLangLevelId(int langLevelId) throws SQLException
    {
        return getResourcesByAudienceId(langLevelId, 1000);
    }

    public List<Resource> getResourcesByLangLevelId(int langlevelId, int maxResults) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_langlevel USING ( resource_id ) WHERE langlevel_id = ? AND deleted = 0 LIMIT ? ", null, langlevelId, maxResults);
    }

    //queries regarding table: lw_rm_purpose and lw_resource_purpose
    public List<Resource> getResourcesByPurposeId(int purposeId) throws SQLException
    {
        return getResourcesByAudienceId(purposeId, 1000);
    }

    public List<Resource> getResourcesByPurposeId(int purposeId, int maxResults) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_purpose USING ( resource_id ) WHERE purpose_id = ? AND deleted = 0 LIMIT ? ", null, purposeId, maxResults);
    }

    //queries regarding table: lw_rm_catbot and lw_resource_category
    public List<Resource> getResourcesByCatbotId(int catbotId) throws SQLException
    {
        return getResourcesByCatbotId(catbotId, 1000);
    }

    public List<Resource> getResourcesByCatbotId(int catbotId, int maxResults) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_category USING ( resource_id ) WHERE cat_bot_id = ? AND deleted = 0 LIMIT ? ", null, catbotId, maxResults);
    }

    //queries regarding table: lw_rm_catmid and lw_resource_category
    public List<Resource> getResourcesByCatmidId(int catmidId) throws SQLException
    {
        return getResourcesByCatmidId(catmidId, 1000);
    }

    public List<Resource> getResourcesByCatmidId(int catmidId, int maxResults) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_category USING ( resource_id ) WHERE cat_mid_id = ? AND deleted = 0 LIMIT ? ", null, catmidId, maxResults);
    }

    //queries regarding table: lw_rm_cattop and lw_resource_category
    public List<Resource> getResourcesByCattopId(int cattopId) throws SQLException
    {
        return getResourcesByCattopId(cattopId, 1000);
    }

    public List<Resource> getResourcesByCattopId(int cattopId, int maxResults) throws SQLException
    {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_category USING ( resource_id ) WHERE cat_top_id = ? AND deleted = 0 LIMIT ? ", null, cattopId, maxResults);
    }

    //save new resource_category
    protected void saveCategoryResource(Resource resource, Category category, User user) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_category` (`resource_id`, `user_id`, `cat_top_id`, `cat_mid_id`, `cat_bot_id`) VALUES (?, ?, ?, ?, ?)");
        replace.setInt(1, null == resource ? 0 : resource.getId());
        replace.setInt(2, null == user ? 0 : user.getId());
        replace.setInt(3, null == category ? 0 : category.getCatTop().getId());
        replace.setInt(4, null == category ? 0 : category.getCatMid().getId());
        replace.setInt(5, null == category ? 0 : category.getCatBot().getId());
        replace.executeUpdate();
        replace.close();
    }

    //save new resource_langlevel
    protected void saveLangLevelResource(Resource resource, String[] langLevels, User user) throws SQLException
    {
        LangLevelManager llm = Learnweb.getInstance().getLangLevelManager();
        for(final String langLevel : langLevels)
        {
            //find id of the lang level first
            int llevelId = llm.getLangLevelIdByLangLevelName(langLevel);
            if(llevelId > 0)
            {
                PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_langlevel` (`resource_id`, `user_id`, `langlevel_id`) VALUES (?, ?, ?)");
                replace.setInt(1, null == resource ? 0 : resource.getId());
                replace.setInt(2, null == user ? 0 : user.getId());
                replace.setInt(3, null == langLevels ? 0 : llevelId);
                replace.executeUpdate();
                replace.close();
            }
        }
    }

    //save new resource_audience
    protected void saveTargetResource(Resource resource, String[] targets, User user) throws SQLException
    {
        AudienceManager am = Learnweb.getInstance().getAudienceManager();
        for(final String target : targets)
        {
            //find id of the audience first
            int targetId = am.getAudienceIdByAudienceName(target.toLowerCase());
            if(targetId > 0)
            {
                PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_audience` (`resource_id`, `user_id`, `audience_id`) VALUES (?, ?, ?)");
                replace.setInt(1, null == resource ? 0 : resource.getId());
                replace.setInt(2, null == user ? 0 : user.getId());
                replace.setInt(3, null == targets ? 0 : targetId);
                replace.executeUpdate();
                replace.close();
            }
        }
    }

    //save new resource_purpose
    protected void savePurposeResource(Resource resource, String[] purposes, User user) throws SQLException
    {
        PurposeManager pm = Learnweb.getInstance().getPurposeManager();
        for(String purpose : purposes)
        {
            //find id of the lang level first
            int purposeId = pm.getPurposeIdByPurposeName(purpose);
            if(purposeId <= 0)
                purposeId = pm.addPurpose(purpose).getId();

            log.debug(purpose + " " + purposeId + " " + resource.getId());
            if(purposeId > 0)
            {
                PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_purpose` (`resource_id`, `user_id`, `purpose_id`) VALUES (?, ?, ?)");
                replace.setInt(1, null == resource ? 0 : resource.getId());
                replace.setInt(2, null == user ? 0 : user.getId());
                replace.setInt(3, null == purposes ? 0 : purposeId);
                replace.executeUpdate();
                replace.close();
            }
        }
    }

    //save new resource_category
    protected void saveCategoryResource(Resource resource, String topcat, String midcat, String botcat, User user) throws SQLException
    {
        int topcatId = 0;
        int midcatId = 0;
        int botcatId = 0;
        //need cat_top_id, cat_mid_id, cat_bot_id to save
        //need to save bottom cat if it does not exist yet (need midcat id to save)
        CategoryManager cm = Learnweb.getInstance().getCategoryManager();
        topcatId = cm.getCategoryTopByName(topcat);
        if(topcatId > 0)
        {
            midcatId = cm.getCategoryMiddleByNameAndTopcatId(midcat, topcatId);
            if(midcatId > 0)
            {
                botcat = botcat.toLowerCase(); //to avoid creating a duplicate bottom category
                botcatId = cm.getCategoryBottomByNameAndMidcatId(botcat, midcatId);
                if(botcatId <= 0)
                {
                    botcatId = cm.saveNewBottomCategory(botcat, midcatId);
                }

                //save new resource_category entry
                PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_category` (`resource_id`, `user_id`, `cat_top_id`, `cat_mid_id`, `cat_bot_id`) VALUES (?, ?, ?, ?, ?)");
                replace.setInt(1, null == resource ? 0 : resource.getId());
                replace.setInt(2, null == user ? 0 : user.getId());
                replace.setInt(3, topcatId);
                replace.setInt(4, midcatId);
                replace.setInt(5, botcatId);
                replace.executeUpdate();
                replace.close();

            }
        }

    }

    /**
     * if this flag is true some performance optimizations for reindexing all resources are enabled
     *
     * @return
     */
    private boolean isReindexMode()
    {
        return reindexMode;
    }

    /**
     * this method enables some performance optimizations for reindexing all resources
     *
     * @param reindexMode
     */
    public void setReindexMode(boolean reindexMode)
    {
        this.reindexMode = reindexMode;
    }

}

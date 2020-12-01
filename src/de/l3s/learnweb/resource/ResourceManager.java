package de.l3s.learnweb.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.PropertiesBundle;
import de.l3s.util.database.Sql;

public class ResourceManager {
    private static final Logger log = LogManager.getLogger(ResourceManager.class);
    private static final String COMMENT_COLUMNS = "`comment_id`, `resource_id`, `user_id`, `text`, `date`";
    private static final String RESOURCE_COLUMNS = "r.read_only_transcript, r.deleted, r.resource_id, r.title, r.description, r.url, r.storage_type, r.rights, r.source, r.language, r.type, r.format, r.owner_user_id, r.rating, r.rate_number, r.filename, r.max_image_url, r.query, r.original_resource_id, r.author, r.file_url, r.thumbnail0_url, r.thumbnail0_file_id, r.thumbnail0_width, r.thumbnail0_height, r.thumbnail1_url, r.thumbnail1_file_id, r.thumbnail1_width, r.thumbnail1_height, r.thumbnail2_url, r.thumbnail2_file_id, r.thumbnail2_width, r.thumbnail2_height, r.thumbnail3_url, r.thumbnail3_file_id, r.thumbnail3_width, r.thumbnail3_height, r.thumbnail4_url, r.thumbnail4_file_id, r.thumbnail4_width, r.thumbnail4_height, r.embeddedRaw, r.transcript, r.online_status, r.id_at_service, r.duration, r.restricted, r.resource_timestamp, r.creation_date, r.metadata, r.group_id, r.folder_id";

    private final Learnweb learnweb;
    private final ICache<Resource> cache;

    private boolean reindexMode = false; // if this flag is true some performance optimizations for reindexing all resources are enabled

    public ResourceManager(Learnweb learnweb) {
        PropertiesBundle properties = learnweb.getProperties();
        int cacheSize = properties.getPropertyIntValue("RESOURCE_CACHE");

        this.learnweb = learnweb;
        this.cache = cacheSize == 0 ? new DummyCache<>() : new Cache<>(cacheSize);
    }

    public int getResourceCount() throws SQLException {
        Long count = (Long) Sql.getSingleResult("SELECT COUNT(*) FROM lw_resource WHERE deleted = 0");
        return count.intValue();
    }

    public int getResourceCountByUserId(int userId) throws SQLException {
        Long count = (Long) Sql.getSingleResult("SELECT COUNT(*) FROM lw_resource r WHERE owner_user_id = " + userId + " AND deleted = 0");
        return count.intValue();
    }

    public int getResourceCountByGroupId(int groupId) throws SQLException {
        Long count = (Long) Sql.getSingleResult("SELECT COUNT(*) FROM lw_resource r WHERE group_id = " + groupId + " AND deleted = 0");
        return count.intValue();
    }

    public Map<Integer, Integer> getResourceCountPerUserByGroup(int groupId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT owner_user_id, COUNT(*) as res_count "
            + "FROM lw_resource r WHERE group_id = ? AND deleted = 0 GROUP BY owner_user_id")) {
            select.setInt(1, groupId);

            try (ResultSet rs = select.executeQuery()) {
                Map<Integer, Integer> resourceCounts = new HashMap<>();
                while (rs.next()) {
                    int userId = rs.getInt("owner_user_id");
                    int resourceCount = rs.getInt("res_count");
                    resourceCounts.put(userId, resourceCount);
                }
                return resourceCounts;
            }
        }
    }

    public List<Resource> getResourcesByUserId(int userId) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE owner_user_id = ? AND deleted = 0", null, userId);
    }

    public List<Resource> getResourcesByUserIdAndType(List<Integer> userIds, ResourceType type) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE owner_user_id IN(" + StringUtils.join(userIds, ",") + ")  AND deleted = 0 AND type = ?", type.toString());
    }

    public List<Resource> getResourcesByTagId(int tagId) throws SQLException {
        return getResourcesByTagId(tagId, 1000);
    }

    public List<Resource> getResourcesByTagId(int tagId, int maxResults) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_tag USING ( resource_id ) WHERE tag_id = ? AND deleted = 0 LIMIT ? ", null, tagId, maxResults);
    }

    public List<Resource> getRatedResourcesByUserId(int userId) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_resource_rating USING ( resource_id ) WHERE user_id = ? AND deleted = 0 ", null, userId);
    }

    /**
     * Returns all resources (that were not deleted).
     */
    public List<Resource> getResourcesAll(int page, int pageSize) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE `deleted` = 0 ORDER BY resource_id LIMIT " + (page * pageSize) + "," + pageSize, null);
    }

    /**
     * @return a rate given to the resource by the user, or {@code null} when resource is not rated by the user.
     */
    public Integer getResourceRateByUser(int resourceId, int userId) {
        try (PreparedStatement stmt = learnweb.getConnection().prepareStatement("SELECT rating FROM lw_resource_rating WHERE resource_id =  ? AND user_id = ?")) {
            stmt.setInt(1, resourceId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rating");
                }
            }
        } catch (SQLException ignored) {
        }

        return null;
    }

    protected void rateResource(int resourceId, int userId, int value) throws SQLException {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO lw_resource_rating (`resource_id`, `user_id`, `rating`) VALUES(?, ?, ?)")) {
            replace.setInt(1, resourceId);
            replace.setInt(2, userId);
            replace.setInt(3, value);
            replace.executeUpdate();
        }

        try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE lw_resource SET rating = rating + ?, rate_number = rate_number + 1 WHERE resource_id = ?")) {
            update.setInt(1, value);
            update.setInt(2, resourceId);
            update.executeUpdate();
        }
    }

    protected void thumbRateResource(int resourceId, int userId, int direction) throws SQLException {
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("Illegal value [" + direction + "] for direction. Valid values are 1 and -1");
        }

        try (PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_thumb` (`resource_id` ,`user_id` ,`direction`) VALUES (?,?,?)")) {
            insert.setInt(1, resourceId);
            insert.setInt(2, userId);
            insert.setInt(3, direction);
            insert.executeUpdate();
        }
    }

    protected void loadThumbRatings(Resource resource) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT SUM(IF(direction=1,1,0)) as positive, SUM(IF(direction=-1,1,0)) as negative FROM `lw_thumb` WHERE `resource_id` = ?")) {
            select.setInt(1, resource.getId());
            ResultSet rs = select.executeQuery();

            if (rs.next()) {
                resource.setThumbUp(rs.getInt(1));
                resource.setThumbDown(rs.getInt(2));
            }
        }
    }

    public Integer getResourceThumbRateByUser(int resourceId, int userId) {
        try (PreparedStatement stmt = learnweb.getConnection().prepareStatement("SELECT direction FROM lw_thumb WHERE resource_id = ? AND user_id = ?")) {
            stmt.setInt(1, resourceId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("direction");
                }
            }
        } catch (SQLException ignored) {
        }

        return null;
    }

    public Resource getResource(int resourceId) throws SQLException {
        if (resourceId < 0) {
            return null;
        }

        Resource resource = cache.get(resourceId);

        if (null != resource) {
            return resource;
        }

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + RESOURCE_COLUMNS + " FROM `lw_resource` r WHERE resource_id = ?")) {
            select.setInt(1, resourceId);
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return createResource(rs);
        }
    }

    public void deleteResource(Resource resource) throws SQLException {
        deleteResource(resource.getId());
    }

    public void deleteResource(int resourceId) throws SQLException {
        // delete resource from SOLR index
        try {
            learnweb.getSolrClient().deleteFromIndex(resourceId);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't delete resource " + resourceId + " from SOLR", e);
        }

        // flag the resource as deleted
        try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_resource` SET deleted = 1 WHERE `resource_id` = ?")) {
            update.setInt(1, resourceId);
            update.executeUpdate();
        }

        // remove resource from cache
        cache.remove(resourceId);
    }

    /**
     * Don't use this function.
     * Usually you have to call deleteResource()
     */
    public void deleteResourceHard(int resourceId) throws SQLException {
        log.debug("Hard delete resource: " + resourceId);

        deleteResource(resourceId); // clear cache and remove resource from SOLR

        String[] tables = {"lw_comment", "lw_glossary_entry", "lw_glossary_resource", "lw_resource_archiveurl", "lw_resource_history",
            "lw_resource_rating", "lw_resource_tag", "lw_submission_resource", "lw_survey_answer", "lw_survey_resource", "lw_survey_resource_user",
            "lw_thumb", "lw_transcript_actions", "lw_transcript_final_sel", "lw_transcript_selections", "lw_transcript_summary",
            "ted_transcripts_paragraphs", "lw_resource"};

        for (String table : tables) {
            try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM " + table + " WHERE `resource_id` = ?")) {
                delete.setInt(1, resourceId);
                int numRowsAffected = delete.executeUpdate();
                if (numRowsAffected > 0) {
                    log.debug("Deleted " + numRowsAffected + " rows from " + table);
                }
            }
        }

        // TODO @astappiev: delete files (lw_file); but it's not possible yet because files are shared when a resource is copied
    }

    /**
     * Saves a resource but is shall be called only by Resource.save() because this method can be extended by resource subclasses.
     */
    protected Resource saveResource(Resource resource) throws SQLException {
        if (resource.getUserId() <= 0) {
            throw new IllegalArgumentException("Resource has no owner");
        }

        String query = "REPLACE INTO `lw_resource` (`resource_id` ,`title` ,`description` ,`url` ,`storage_type` ,`rights` ,`source` ,`type` ,`format` ,`owner_user_id` ,`rating` ,`rate_number` ,`query`, filename, max_image_url, original_resource_id, machine_description, author, file_url, thumbnail0_url, thumbnail0_file_id, thumbnail0_width, thumbnail0_height, thumbnail1_url, thumbnail1_file_id, thumbnail1_width, thumbnail1_height, thumbnail2_url, thumbnail2_file_id, thumbnail2_width, thumbnail2_height, thumbnail3_url, thumbnail3_file_id, thumbnail3_width, thumbnail3_height, thumbnail4_url, thumbnail4_file_id, thumbnail4_width, thumbnail4_height, embeddedRaw, transcript, online_status, id_at_service, duration, restricted, language, creation_date, metadata, group_id, folder_id, deleted, read_only_transcript) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            if (resource.getId() < 0) { // the Resource is not yet stored at the database
                replace.setNull(1, java.sql.Types.INTEGER);
            } else {
                replace.setInt(1, resource.getId());
            }
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

            Thumbnail[] thumbnails = {resource.getThumbnail0(), resource.getThumbnail1(), resource.getThumbnail2(), resource.getThumbnail3(), resource.getThumbnail4()};

            for (int i = 0, m = 20; i < 5; i++) {
                String url = null;
                int fileId = 0;
                int width = 0;
                int height = 0;

                Thumbnail tn = thumbnails[i];
                if (tn != null) { // a thumbnail is defined
                    if (tn.getFileId() == 0) {
                        url = tn.getUrl();
                    }
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
            replace.setTimestamp(47, Sql.convertDateTime(resource.getCreationDate()));
            Sql.setSerializedObject(replace, 48, resource.getMetadata());
            replace.setInt(49, resource.getGroupId());
            replace.setInt(50, resource.getFolderId());
            replace.setInt(51, resource.isDeleted() ? 1 : 0);
            replace.setInt(52, resource.isReadOnlyTranscript() ? 1 : 0);

            replace.executeUpdate();

            if (resource.getId() < 0) { // get the assigned id
                ResultSet rs = replace.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                resource.setId(rs.getInt(1));
                resource.setLocation(getLocation(resource));
                cache.put(resource);

                // persist the relation between the resource and its files
                learnweb.getFileManager().addFilesToResource(resource.getFiles().values(), resource);

                // TODO @astappiev: this has to be moved to the save method of WebResource.class, which has to be created
                if (CollectionUtils.isNotEmpty(resource.getArchiveUrls())) {
                    try {
                        // To copy archive versions of a resource if it exists
                        saveArchiveUrlsByResourceId(resource.getId(), resource.getArchiveUrls());
                    } catch (Exception e) {
                        log.error("Can't save archiveUrls", e);
                    }
                }
            } else { // edited resources need to be updated in the cache
                cache.remove(resource.getId());
                cache.put(resource);
                resource.clearCaches();
            }
        }
        learnweb.getSolrClient().reIndexResource(resource);

        return resource;
    }

    private Comment createComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getInt("comment_id"));
        comment.setResourceId(rs.getInt("resource_id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setText(rs.getString("text"));
        comment.setDate(new Date(rs.getTimestamp("date").getTime()));
        return comment;
    }

    public List<Comment> getCommentsByUserId(int userId) throws SQLException {
        List<Comment> comments = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_COLUMNS + " FROM `lw_comment` JOIN lw_resource USING(resource_id) WHERE `user_id` = ? AND deleted = 0");
        select.setInt(1, userId);
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            comments.add(createComment(rs));
        }
        select.close();

        return comments;
    }

    public List<Comment> getCommentsByUserIds(Collection<Integer> userIds) throws SQLException {
        String userIdString = StringUtils.join(userIds, ",");

        List<Comment> comments = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_COLUMNS + " FROM `lw_comment` JOIN lw_resource USING(resource_id) WHERE `user_id` IN(" + userIdString + ") AND deleted = 0");

        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            comments.add(createComment(rs));
        }
        select.close();

        return comments;
    }

    public Comment getComment(int commentId) throws SQLException {
        Comment comment = null;

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_COLUMNS + " FROM `lw_comment` WHERE `comment_id` = ?");
        select.setInt(1, commentId);
        ResultSet rs = select.executeQuery();

        if (rs.next()) {
            comment = createComment(rs);
        }
        select.close();

        return comment;
    }

    public List<Comment> getCommentsByResourceId(int id) throws SQLException {
        List<Comment> comments = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COMMENT_COLUMNS + " FROM `lw_comment` WHERE `resource_id` = ? ORDER BY date DESC");
        select.setInt(1, id);
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            comments.add(createComment(rs));
        }
        select.close();

        return comments;
    }

    public Tag getTag(String tagName) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name FROM lw_tag WHERE name LIKE ? ORDER BY tag_id LIMIT 1")) {
            select.setString(1, tagName);
            ResultSet rs = select.executeQuery();
            if (!rs.next()) {
                return null;
            }

            return new Tag(rs.getInt("tag_id"), rs.getString("name"));
        }
    }

    protected void deleteTag(Tag tag, Resource resource) throws SQLException {
        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_resource_tag WHERE resource_id = ? AND tag_id = ?")) {
            delete.setInt(1, resource.getId());
            delete.setInt(2, tag.getId());
            delete.executeUpdate();
        }
    }

    protected void deleteComment(Comment comment) throws SQLException {
        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_comment WHERE comment_id = ?")) {
            delete.setInt(1, comment.getId());
            delete.executeUpdate();
        }
    }

    public List<Tag> getTagsByUserId(int userId) throws SQLException {
        LinkedList<Tag> tags = new LinkedList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name FROM `lw_resource_tag` JOIN lw_tag USING(tag_id) JOIN lw_resource USING(resource_id) WHERE `user_id` = ? AND deleted = 0")) {
            select.setInt(1, userId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(rs.getInt("tag_id"), rs.getString("name")));
            }
        }
        return tags;
    }

    public OwnerList<Tag, User> getTagsByResource(int resourceId) throws SQLException {
        UserManager um = learnweb.getUserManager();
        OwnerList<Tag, User> tags = new OwnerList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT tag_id, name, user_id, timestamp FROM `lw_resource_tag` JOIN lw_tag USING(tag_id) JOIN lw_resource USING(resource_id) WHERE `resource_id` = ?")) {
            select.setInt(1, resourceId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(rs.getInt("tag_id"), rs.getString("name")), um.getUser(rs.getInt("user_id")), new Date(rs.getTimestamp("timestamp").getTime()));
            }
        }
        return tags;

    }

    public Tag addTag(String tagName) throws SQLException {
        Tag tag = new Tag(-1, tagName);

        saveTag(tag);

        return tag;
    }

    public void tagResource(Resource resource, Tag tag, User user) throws SQLException {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_tag` (`resource_id`, `tag_id`, `user_id`) VALUES (?, ?, ?)")) {
            replace.setInt(1, null == resource ? 0 : resource.getId());
            replace.setInt(2, tag.getId());
            replace.setInt(3, null == user ? 0 : user.getId());
            replace.executeUpdate();
        }
    }

    protected Comment commentResource(String text, User user, Resource resource) throws SQLException {
        Comment c = new Comment(text, new Date(), resource, user);
        saveComment(c);

        return c;
    }

    private void saveTag(Tag tag) throws SQLException {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_tag` (tag_id, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            if (tag.getId() < 0) { // the tag is not yet stored at the database
                replace.setNull(1, java.sql.Types.INTEGER);
            } else {
                replace.setInt(1, tag.getId());
            }
            replace.setString(2, tag.getName());
            replace.executeUpdate();

            if (tag.getId() < 0) { // get the assigned id
                ResultSet rs = replace.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                tag.setId(rs.getInt(1));
            }
        }
    }

    public void saveComment(Comment comment) throws SQLException {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_comment` (" + COMMENT_COLUMNS + ") VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            if (comment.getId() < 0) { // the comment is not yet stored at the database
                replace.setNull(1, java.sql.Types.INTEGER);
            } else {
                replace.setInt(1, comment.getId());
            }
            replace.setInt(2, comment.getResourceId());
            replace.setInt(3, comment.getUserId());
            replace.setString(4, comment.getText());
            replace.setTimestamp(5, Sql.convertDateTime(comment.getDate()));
            replace.executeUpdate();

            if (comment.getId() < 0) { // get the assigned id
                ResultSet rs = replace.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                comment.setId(rs.getInt(1));
            }
        }
    }

    public LinkedList<ArchiveUrl> getArchiveUrlsByResourceId(int id) throws SQLException {
        LinkedList<ArchiveUrl> archiveUrls = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT archive_url, timestamp FROM `lw_resource_archiveurl` WHERE `resource_id` = ? ORDER BY timestamp")) {
            select.setInt(1, id);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                archiveUrls.add(new ArchiveUrl(rs.getString("archive_url"), rs.getTimestamp("timestamp")));
            }
        }
        return archiveUrls;
    }

    public LinkedList<ArchiveUrl> getArchiveUrlsByResourceUrl(String url) throws SQLException {
        SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        LinkedList<ArchiveUrl> archiveUrls = new LinkedList<>();
        int urlId;
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT url_id FROM `wb_url` WHERE `url` = ?")) {
            select.setString(1, url);
            ResultSet rs = select.executeQuery();
            if (!rs.next()) {
                return archiveUrls;
            }

            urlId = rs.getInt(1);
        }

        try (PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
            "SELECT timestamp FROM `wb_url_capture` WHERE `url_id` = ? ORDER BY timestamp")) {
            pStmt.setInt(1, urlId);
            ResultSet rs2 = pStmt.executeQuery();
            while (rs2.next()) {
                Date timestamp = new Date(rs2.getTimestamp(1).getTime());
                archiveUrls.add(new ArchiveUrl("https://web.archive.org/web/" + waybackDateFormat.format(timestamp) + "/" + url, timestamp));
            }
        }
        return archiveUrls;
    }

    public void saveArchiveUrlsByResourceId(int resourceId, List<ArchiveUrl> archiveUrls) throws SQLException {
        try (PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("INSERT into lw_resource_archiveurl(`resource_id`,`archive_url`,`timestamp`) VALUES (?,?,?)")) {
            for (ArchiveUrl version : archiveUrls) {
                prepStmt.setInt(1, resourceId);
                prepStmt.setString(2, version.getArchiveUrl());
                prepStmt.setTimestamp(3, Sql.convertDateTime(version.getTimestamp()));
                prepStmt.executeUpdate();
            }
        }
    }

    private Resource createResource(ResultSet rs) throws SQLException {
        int id = rs.getInt("resource_id");
        Resource resource = cache.get(id);

        if (null == resource) {
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

            // This must be set manually because we stored some external sources in Learnweb/Solr
            resource.setLocation(getLocation(resource));

            if (resource.isDeleted()) {
                log.debug("resource " + resource.getId() + " was requested but is deleted");
            } else if (!isReindexMode()) {
                List<File> files = learnweb.getFileManager().getFilesByResource(resource.getId());

                for (File file : files) {
                    resource.addFile(file);
                    if (file.getType() == TYPE.FILE_MAIN) {
                        resource.setFileUrl(file.getUrl());
                        resource.setFileName(file.getName());

                        if (resource.getStorageType() == Resource.LEARNWEB_RESOURCE) {
                            resource.setUrl(file.getUrl());
                        }
                    }
                }
            }

            // deserialize metadata
            byte[] metadataBytes = rs.getBytes("metadata");

            if (metadataBytes != null && metadataBytes.length > 0) {
                ByteArrayInputStream metadataBAIS = new ByteArrayInputStream(metadataBytes);

                try {
                    ObjectInputStream metadataOIS = new ObjectInputStream(metadataBAIS);

                    // re-create the object
                    Object metadata = metadataOIS.readObject();

                    if (metadata != null) {
                        resource.setMetadata(metadata);
                    }
                } catch (Exception e) {
                    log.error("Couldn't load metadata for resource " + resource.getId(), e);
                }
            }

            resource.postConstruct();
            resource = cache.put(resource);
        }
        return resource;
    }

    private Thumbnail createThumbnail(ResultSet rs, int thumbnailSize) throws SQLException {
        String prefix = "thumbnail" + thumbnailSize;
        String url = rs.getString(prefix + "_url");
        int fileId = rs.getInt(prefix + "_file_id");

        if (fileId != 0) {
            url = learnweb.getFileManager().getThumbnailUrl(fileId, thumbnailSize);
        } else if (url == null) {
            return null;
        }

        return new Thumbnail(url, rs.getInt(prefix + "_width"), rs.getInt(prefix + "_height"), fileId);
    }

    /**
     * @param param1 set to null if no parameter
     */
    public List<Resource> getResources(String query, String param1, int... params) throws SQLException {
        List<Resource> resources = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement(query);

        int i = 1;
        if (null != param1) {
            select.setString(i++, param1);
        }

        for (int param : params) {
            select.setInt(i++, param);
        }

        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            //log.debug(rs.getInt("resource_id"));
            resources.add(createResource(rs));
        }
        select.close();

        return resources;
    }

    public void resetCache() {
        for (Resource resource : cache.getValues()) {
            resource.clearCaches();
        }

        cache.clear();
    }

    /**
     * @return number of cached objects
     */
    public int getCacheSize() {
        return cache.size();
    }

    public List<Resource> getResourcesByGroupId(int groupId) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r WHERE `group_id` = ? and deleted = 0", null, groupId);
    }

    public List<Resource> getResourcesByFolderId(int folderId) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r  WHERE folder_id = ? AND deleted = 0", null, folderId);
    }

    public List<Resource> getFolderResourcesByUserId(int groupId, int folderId, int userId, int limit) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r  WHERE group_id = ? AND folder_id = ? AND owner_user_id = ? AND deleted = 0 LIMIT ?", null, groupId, folderId, userId, limit);
    }

    /**
     * Returns all survey resources that exists in the groups of the given course.
     */
    public List<Resource> getSurveyResourcesByUserAndCourse(int courseId) throws SQLException {
        return getResources("SELECT " + RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_group g USING(group_id) WHERE r.type='survey' AND r.deleted=0 AND g.course_id=? ORDER BY r.title", null, courseId);
    }

    /**
     * if this flag is true some performance optimizations for reindexing all resources are enabled.
     */
    private boolean isReindexMode() {
        return reindexMode;
    }

    /**
     * this method enables some performance optimizations for reindexing all resources.
     */
    public void setReindexMode(boolean reindexMode) {
        this.reindexMode = reindexMode;
    }

    /**
     * Returns the the location were a resource is stored. Necessary because some external sources are indexed in our Solr instance.
     */
    private static String getLocation(Resource resource) {
        ResourceService source = resource.getSource();

        if (null == source) {
            log.warn("Empty source for resource: " + resource, new IllegalArgumentException());
            return "Learnweb";
        }

        switch (source) {
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

    public Resource copyResource(final Resource resource, final int targetGroupId, final int targetFolderId, final User user) throws SQLException {
        Resource copyResource = resource.clone();
        copyResource.setGroupId(targetGroupId);
        copyResource.setFolderId(targetFolderId);
        copyResource.setUser(user);
        saveResource(copyResource);

        copyFiles(copyResource, resource.getFiles().values());
        return resource;
    }

    private void copyFiles(final Resource resource, final Collection<File> originalFiles) throws SQLException {
        try {
            for (File file : originalFiles) {
                if (List.of(TYPE.THUMBNAIL_VERY_SMALL, TYPE.THUMBNAIL_SMALL, TYPE.THUMBNAIL_SQUARED, TYPE.THUMBNAIL_MEDIUM, TYPE.THUMBNAIL_LARGE,
                    TYPE.CHANGES, TYPE.HISTORY_FILE).contains(file.getType())) {
                    continue; // skip them
                }

                File copyFile = new File(file);
                copyFile.setResourceId(resource.getId());
                // TODO @astappiev: improve copy performance by using fs copy
                learnweb.getFileManager().save(copyFile, file.getInputStream());
                resource.addFile(copyFile);

                if (file.getType() == TYPE.FILE_MAIN) {
                    if (resource.getUrl().equals(file.getUrl())) {
                        resource.setUrl(copyFile.getUrl());
                    }

                    if (resource.getFileUrl().equals(file.getUrl())) {
                        resource.setFileUrl(copyFile.getUrl());
                    }

                    saveResource(resource);
                }
            }
        } catch (IOException e) {
            log.error("Error during copying resource files {}", resource, e);
        }
    }

    /**
     * Creates appropriate Resource instances based on the resource type.
     * Necessary since some resource types extend the normal Resource class.
     */
    private static Resource newResource(ResourceType type) {
        switch (type) {
            case survey:
                return new SurveyResource();
            case glossary2:
                return new GlossaryResource();
            default:
                return new Resource();
        }
    }
}

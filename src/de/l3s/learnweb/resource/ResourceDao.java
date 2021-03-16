package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.FetchSize;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.user.User;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(ResourceDao.ResourceMapper.class)
public interface ResourceDao extends SqlObject, Serializable {
    ICache<Resource> cache = new Cache<>(3000);

    @CreateSqlObject
    FileDao getFileDao();

    default Optional<Resource> findById(int resourceId) {
        return Optional.ofNullable(cache.get(resourceId))
            .or(() -> getHandle().select("SELECT * FROM lw_resource WHERE resource_id = ?", resourceId).mapTo(Resource.class).findOne());
    }

    default Resource findByIdOrElseThrow(int resourceId) {
        return findById(resourceId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_object_description"));
    }

    /**
     * Returns all resources (that were not deleted).
     */
    @FetchSize(1000)
    @SqlQuery("SELECT * FROM lw_resource r WHERE deleted = 0 ORDER BY resource_id")
    Stream<Resource> findAll();

    /**
     * Returns all resources (that were not deleted) using given limit and offset.
     */
    @SqlQuery("SELECT * FROM lw_resource r WHERE deleted = 0 ORDER BY resource_id LIMIT ? OFFSET ?")
    List<Resource> findAll(int limit, int offset);

    @SqlQuery("SELECT * FROM lw_resource r WHERE group_id = ? and deleted = 0")
    List<Resource> findByGroupId(int groupId);

    @SqlQuery("SELECT * FROM lw_resource r  WHERE folder_id = ? AND deleted = 0")
    List<Resource> findByFolderId(int folderId);

    @SqlQuery("SELECT * FROM lw_resource WHERE owner_user_id = ? AND deleted = 0")
    List<Resource> findByOwnerId(int userId);

    @SqlQuery("SELECT r.* FROM lw_resource r JOIN lw_resource_tag USING ( resource_id ) WHERE tag_id = ? AND deleted = 0")
    List<Resource> findByTagId(int tagId);

    @SqlQuery("SELECT r.* FROM lw_resource r JOIN lw_resource_tag USING ( resource_id ) WHERE tag_id = ? AND deleted = 0 LIMIT ?")
    List<Resource> findByTagId(int tagId, int limit);

    @SqlQuery("SELECT * FROM lw_resource WHERE url = ? AND deleted = 0 LIMIT 1")
    Optional<Resource> findByUrl(String url);

    @SqlQuery("SELECT r.* FROM lw_resource r JOIN lw_resource_rating USING ( resource_id ) WHERE user_id = ? AND deleted = 0")
    List<Resource> findRatedByUsedId(int userId);

    @SqlQuery("SELECT * FROM lw_resource r  WHERE group_id = ? AND folder_id = ? AND owner_user_id = ? AND deleted = 0 LIMIT ?")
    List<Resource> findByGroupIdAndFolderIdAndOwnerId(int groupId, int folderId, int userId, int limit);

    /**
     * Retrieves submitted resources of a user for a particular submission.
     */
    @SqlQuery("SELECT r.* FROM lw_resource r JOIN lw_submission_resource sr USING (resource_id) WHERE sr.submission_id = ? AND sr.user_id = ?")
    List<Resource> findBySubmissionIdAndUserId(int submissionId, int userId);

    /**
     * Returns all survey resources that exists in the groups of the given course.
     */
    @SqlQuery("SELECT r.* FROM lw_resource r JOIN lw_group g USING(group_id) WHERE r.type='survey' AND r.deleted=0 AND g.course_id=? ORDER BY r.title")
    List<Resource> findSurveysByCourseId(int courseId);

    @SqlQuery("SELECT * FROM lw_resource r WHERE owner_user_id IN (<userIds>) AND deleted = 0 AND type = :type")
    List<Resource> findByOwnerIdsAndType(@BindList("userIds") Collection<Integer> userIds, @Bind("type") ResourceType type);

    @SqlQuery("SELECT COUNT(*) FROM lw_resource WHERE deleted = 0")
    int countUndeleted();

    @SqlQuery("SELECT COUNT(*) FROM lw_resource WHERE owner_user_id = ? AND deleted = 0")
    int countByOwnerId(int userId);

    @SqlQuery("SELECT COUNT(*) FROM lw_resource WHERE group_id = ? AND deleted = 0")
    int countByGroupId(int groupId);

    @SqlQuery("SELECT owner_user_id, COUNT(*) as count FROM lw_resource WHERE group_id = ? AND deleted = 0 GROUP BY owner_user_id")
    @KeyColumn("owner_user_id")
    @ValueColumn("count")
    Map<Integer, Integer> countPerUserByGroupId(int groupId);

    /**
     * @return a rate given to the resource by the user.
     */
    @SqlQuery("SELECT rating FROM lw_resource_rating WHERE resource_id =  ? AND user_id = ?")
    Optional<Integer> findResourceRating(int resourceId, int userId);

    default void insertResourceRating(int resourceId, int userId, int value) {
        getHandle().execute("INSERT INTO lw_resource_rating (resource_id, user_id, rating) VALUES(?, ?, ?)", resourceId, userId, value);
        getHandle().execute("UPDATE lw_resource SET rating = rating + ?, rate_number = rate_number + 1 WHERE resource_id = ?", value, resourceId);
    }

    @SqlUpdate("INSERT INTO lw_thumb (resource_id ,user_id ,direction) VALUES (?, ?, ?)")
    void insertThumbRate(Resource resource, User user, int direction);

    /**
     * @return number of total thumb ups (left) and thumb downs (right).
     */
    default Optional<ImmutablePair<Integer, Integer>> findThumbRatings(Resource resource) {
        return getHandle()
            .select("SELECT SUM(IF(direction=1,1,0)) as positive, SUM(IF(direction=-1,1,0)) as negative FROM lw_thumb WHERE resource_id = ?", resource)
            .map((rs, ctx) -> new ImmutablePair<>(rs.getInt(1), rs.getInt(2))).findOne();
    }

    @SqlQuery("SELECT direction FROM lw_thumb WHERE resource_id = ? AND user_id = ?")
    Optional<Integer> findThumbRate(Resource resource, User user);

    @SqlUpdate("INSERT INTO lw_resource_tag (resource_id, user_id, tag_id) VALUES (?, ?, ?)")
    void insertTag(Resource resource, User user, Tag tag);

    @SqlUpdate("DELETE FROM lw_resource_tag WHERE resource_id = ? AND tag_id = ?")
    void deleteTag(Resource resource, Tag tag);

    default void copy(final Resource sourceResource, final int targetGroupId, final int targetFolderId, final User user) {
        Resource resource = new Resource(sourceResource);
        resource.setGroupId(targetGroupId);
        resource.setFolderId(targetFolderId);
        resource.setUser(user);
        save(resource);

        copyFiles(resource, sourceResource.getFiles().values());
    }

    default ImmutablePair<Integer, Long> copyFiles(final Resource resource, final Collection<File> files) {
        try {
            int filesCount = 0;
            long sizeBytes = 0;

            for (File file : files) {
                if (file.getType().in(File.FileType.DOC_CHANGES, File.FileType.DOC_HISTORY)) {
                    continue; // skip them
                }

                filesCount += 1;
                sizeBytes += FileUtils.sizeOf(file.getActualFile());

                File copyFile = new File(file);
                copyFile.setResourceId(resource.getId());
                // TODO @astappiev: improve copy performance by using fs copy
                getFileDao().save(copyFile, file.getInputStream());
                resource.addFile(copyFile);

                if (file.getType() == File.FileType.MAIN) {
                    resource.setFileId(file.getId());
                }

                if (file.getType() == File.FileType.THUMBNAIL_SMALL) {
                    resource.setThumbnailSmall(new Thumbnail(file));
                }

                if (file.getType() == File.FileType.THUMBNAIL_MEDIUM) {
                    resource.setThumbnailMedium(new Thumbnail(file));
                }

                if (file.getType() == File.FileType.THUMBNAIL_LARGE) {
                    resource.setThumbnailLarge(new Thumbnail(file));
                }
            }

            save(resource);
            return ImmutablePair.of(filesCount, sizeBytes);
        } catch (IOException e) {
            LogManager.getLogger(ResourceDao.class).error("Error during copying resource files {}", resource, e);
            return null;
        }
    }

    default void save(Resource resource) {
        resource.setUpdatedAt(SqlHelper.now());
        if (resource.getCreatedAt() == null) {
            resource.setCreatedAt(resource.getUpdatedAt());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("resource_id", SqlHelper.toNullable(resource.getId()));
        params.put("deleted", resource.isDeleted());
        params.put("title", resource.getTitle());
        params.put("description", SqlHelper.toNullable(resource.getDescription()));
        params.put("url", SqlHelper.toNullable(resource.getUrl()));
        params.put("storage_type", resource.getStorageType().name());
        params.put("policy_view", resource.getPolicyView().name());
        params.put("service", resource.getService().name());
        params.put("type", resource.getType().name());
        params.put("format", SqlHelper.toNullable(resource.getFormat()));
        params.put("owner_user_id", SqlHelper.toNullable(resource.getUserId()));
        params.put("rating", resource.getRatingSum());
        params.put("rate_number", resource.getRateNumber());
        params.put("query", SqlHelper.toNullable(resource.getQuery()));
        params.put("max_image_url", SqlHelper.toNullable(resource.getMaxImageUrl()));
        params.put("original_resource_id", SqlHelper.toNullable(resource.getOriginalResourceId()));
        params.put("machine_description", SqlHelper.toNullable(resource.getMachineDescription()));
        params.put("author", SqlHelper.toNullable(resource.getAuthor()));
        params.put("file_id", SqlHelper.toNullable(resource.getFileId()));
        params.put("file_name", resource.getFileName());
        params.put("embedded_url", resource.getEmbeddedUrl());
        params.put("transcript", resource.getTranscript());
        params.put("online_status", resource.getOnlineStatus().name());
        params.put("id_at_service", SqlHelper.toNullable(resource.getIdAtService()));
        params.put("duration", SqlHelper.toNullable(resource.getDuration()));
        params.put("width", SqlHelper.toNullable(resource.getWidth()));
        params.put("height", SqlHelper.toNullable(resource.getHeight()));
        params.put("language", SqlHelper.toNullable(resource.getLanguage()));
        params.put("created_at", resource.getCreatedAt());
        params.put("metadata", SerializationUtils.serialize(resource.getMetadata()));
        params.put("group_id", SqlHelper.toNullable(resource.getGroupId()));
        params.put("folder_id", SqlHelper.toNullable(resource.getFolderId()));
        params.put("read_only_transcript", resource.isReadOnlyTranscript());
        params.put("thumbnail0_file_id", resource.getThumbnailSmall() == null ? null : SqlHelper.toNullable(resource.getThumbnailSmall().getFileId()));
        params.put("thumbnail2_file_id", resource.getThumbnailMedium() == null ? null : SqlHelper.toNullable(resource.getThumbnailMedium().getFileId()));
        params.put("thumbnail4_file_id", resource.getThumbnailLarge() == null ? null : SqlHelper.toNullable(resource.getThumbnailLarge().getFileId()));

        Optional<Integer> resourceId = SqlHelper.handleSave(getHandle(), "lw_resource", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        resourceId.ifPresent(id -> {
            resource.setId(id);
            cache.put(resource);
        });

        // persist the relation between the resource and its files
        if (!resource.getFiles().isEmpty()) {
            getFileDao().updateResourceId(resource, resource.getFiles().values());
        }

        Learnweb.getInstance().getSolrClient().reIndexResource(resource);
    }

    default void deleteSoft(Resource resource) {
        getHandle().execute("UPDATE lw_resource SET deleted = 1 WHERE resource_id = ?", resource);
        resource.setDeleted(true);

        try {
            Learnweb.getInstance().getSolrClient().deleteFromIndex(resource.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't delete resource " + resource.getId() + " from Solr", e);
        }

        cache.remove(resource.getId());
    }

    /**
     * Don't use this function.
     * Usually you have to call deleteSoft()
     */
    default void deleteHard(Resource resource) {
        for (File file : getFileDao().findByResourceId(resource.getId())) {
            getFileDao().deleteSoft(file);
        }

        getHandle().execute("DELETE FROM lw_resource WHERE resource_id = ?", resource);

        try {
            Learnweb.getInstance().getSolrClient().deleteFromIndex(resource.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't delete resource " + resource.getId() + " from Solr", e);
        }

        cache.remove(resource.getId());
    }

    class ResourceMapper implements RowMapper<Resource> {
        @Override
        public Resource map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Resource resource = cache.get(rs.getInt("resource_id"));

            if (resource == null) {
                resource = Resource.ofType(rs.getString("storage_type"), rs.getString("type"), rs.getString("service"));
                resource.setDeleted(rs.getBoolean("deleted"));
                resource.setId(rs.getInt("resource_id"));
                resource.setFormat(rs.getString("format"));
                resource.setTitle(rs.getString("title"));
                resource.setDescription(rs.getString("description"));
                resource.setUrl(rs.getString("url"));
                resource.setPolicyView(Resource.PolicyView.valueOf(rs.getString("policy_view")));
                resource.setAuthor(rs.getString("author"));
                resource.setUserId(rs.getInt("owner_user_id"));
                resource.setRatingSum(rs.getInt("rating"));
                resource.setRateNumber(rs.getInt("rate_number"));
                resource.setMaxImageUrl(rs.getString("max_image_url"));
                resource.setQuery(rs.getString("query"));
                resource.setOriginalResourceId(rs.getInt("original_resource_id"));
                resource.setFileId(rs.getInt("file_id"));
                resource.setFileName(rs.getString("file_name"));
                resource.setThumbnailSmall(createThumbnail(rs, 0));
                resource.setThumbnailMedium(createThumbnail(rs, 2));
                resource.setThumbnailLarge(createThumbnail(rs, 4));
                resource.setEmbeddedUrl(rs.getString("embedded_url"));
                resource.setTranscript(rs.getString("transcript"));
                resource.setOnlineStatus(Resource.OnlineStatus.valueOf(rs.getString("online_status")));
                resource.setIdAtService(rs.getString("id_at_service"));
                resource.setDuration(rs.getInt("duration"));
                resource.setWidth(rs.getInt("width"));
                resource.setHeight(rs.getInt("height"));
                resource.setLanguage(rs.getString("language"));
                resource.setGroupId(rs.getInt("group_id"));
                resource.setFolderId(rs.getInt("folder_id"));
                resource.setReadOnlyTranscript(rs.getBoolean("read_only_transcript"));
                resource.setMetadata(SqlHelper.deserializeHashMap(rs.getBytes("metadata")));
                resource.setUpdatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("updated_at")));
                resource.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));

                resource.postConstruct();
                cache.put(resource);
            }
            return resource;
        }

        private static Thumbnail createThumbnail(ResultSet rs, int thumbnailSize) throws SQLException {
            int fileId = rs.getInt("thumbnail" + thumbnailSize + "_file_id");

            if (fileId != 0) {
                File file = new File();
                file.setId(fileId);
                file.setName("thumbnail" + thumbnailSize + ".png");
                file.setMimeType("image/png");
                return new Thumbnail(file);
            } else {
                return null;
            }
        }
    }
}

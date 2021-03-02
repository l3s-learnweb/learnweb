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
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.user.User;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.RsHelper;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(ResourceDao.ResourceMapper.class)
public interface ResourceDao extends SqlObject, Serializable {
    ICache<Resource> cache = new Cache<>(3000);

    @CreateSqlObject
    FileDao getFileDao();

    default Resource findById(int resourceId) {
        Resource resource = cache.get(resourceId);
        if (resource != null) {
            return resource;
        }

        return getHandle().select("SELECT * FROM lw_resource WHERE resource_id = ?", resourceId)
            .map(new ResourceMapper()).findOne().orElse(null);
    }

    /**
     * Returns all resources (that were not deleted).
     */
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

    @SqlQuery("SELECT * FROM lw_resource r JOIN lw_resource_tag USING ( resource_id ) WHERE tag_id = ? AND deleted = 0")
    List<Resource> findByTagId(int tagId);

    @SqlQuery("SELECT * FROM lw_resource r JOIN lw_resource_tag USING ( resource_id ) WHERE tag_id = ? AND deleted = 0 LIMIT ?")
    List<Resource> findByTagId(int tagId, int limit);

    @SqlQuery("SELECT * FROM lw_resource WHERE url = ? AND deleted = 0 LIMIT 1")
    Optional<Resource> findByUrl(String url);

    @SqlQuery("SELECT * FROM lw_resource r JOIN lw_resource_rating USING ( resource_id ) WHERE user_id = ? AND deleted = 0")
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
    @SqlQuery("SELECT * FROM lw_resource r JOIN lw_group g USING(group_id) WHERE r.type='survey' AND r.deleted=0 AND g.course_id=? ORDER BY r.title")
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
        Resource resource = sourceResource.clone();
        resource.setGroupId(targetGroupId);
        resource.setFolderId(targetFolderId);
        resource.setUser(user);
        save(resource);

        try {
            for (File file : sourceResource.getFiles().values()) {
                if (List.of(File.TYPE.THUMBNAIL_VERY_SMALL, File.TYPE.THUMBNAIL_SMALL, File.TYPE.THUMBNAIL_SQUARED, File.TYPE.THUMBNAIL_MEDIUM,
                    File.TYPE.THUMBNAIL_LARGE, File.TYPE.CHANGES, File.TYPE.HISTORY_FILE).contains(file.getType())) {
                    continue; // skip them
                }

                File copyFile = new File(file);
                copyFile.setResourceId(resource.getId());
                // TODO @astappiev: improve copy performance by using fs copy
                getFileDao().save(copyFile, file.getInputStream());
                resource.addFile(copyFile);

                if (file.getType() == File.TYPE.FILE_MAIN) {
                    if (resource.getUrl().equals(file.getUrl())) {
                        resource.setUrl(copyFile.getUrl());
                    }

                    if (resource.getFileUrl().equals(file.getUrl())) {
                        resource.setFileUrl(copyFile.getAbsoluteUrl());
                    }

                    Learnweb.dao().getResourceDao().save(resource);
                }
            }
        } catch (IOException e) {
            LogManager.getLogger(ResourceDao.class).error("Error during copying resource files {}", resource, e);
        }
    }

    default void save(Resource resource) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("resource_id", resource.getId() < 1 ? null : resource.getId());
        params.put("title", resource.getTitle());
        params.put("description", resource.getDescription());
        params.put("url", resource.getUrl());
        params.put("storage_type", resource.getStorageType());
        params.put("rights", resource.getRights());
        params.put("source", resource.getSource().name());
        params.put("type", resource.getType().name());
        params.put("format", resource.getFormat());
        params.put("owner_user_id", resource.getUserId() < 1 ? null : resource.getUserId());
        params.put("rating", resource.getRatingSum());
        params.put("rate_number", resource.getRateNumber());
        params.put("query", resource.getQuery());
        params.put("filename", resource.getFileName());
        params.put("max_image_url", resource.getMaxImageUrl());
        params.put("original_resource_id", resource.getOriginalResourceId());
        params.put("machine_description", resource.getMachineDescription());
        params.put("author", resource.getAuthor());
        params.put("file_url", resource.getFileUrl());
        params.put("embeddedRaw", resource.getEmbeddedRaw());
        params.put("transcript", resource.getTranscript());
        params.put("online_status", resource.getOnlineStatus().name());
        params.put("id_at_service", resource.getIdAtService());
        params.put("duration", resource.getDuration());
        params.put("restricted", resource.isRestricted());
        params.put("language", resource.getLanguage());
        params.put("creation_date", resource.getCreationDate());
        params.put("metadata", SerializationUtils.serialize(resource.getMetadata()));
        params.put("group_id", resource.getGroupId() == 0 ? null : resource.getGroupId());
        params.put("folder_id", resource.getFolderId() == 0 ? null : resource.getFolderId());
        params.put("deleted", resource.isDeleted());
        params.put("read_only_transcript", resource.isReadOnlyTranscript());

        if (resource.getThumbnail0() != null) {
            if (resource.getThumbnail0().getFileId() == null) {
                params.put("thumbnail0_url", resource.getThumbnail0().getUrl());
            }
            params.put("thumbnail0_file_id", resource.getThumbnail0().getFileId());
            params.put("thumbnail0_width", resource.getThumbnail0().getWidth());
            params.put("thumbnail0_height", resource.getThumbnail0().getHeight());
        }

        if (resource.getThumbnail1() != null) {
            if (resource.getThumbnail1().getFileId() == null) {
                params.put("thumbnail1_url", resource.getThumbnail1().getUrl());
            }
            params.put("thumbnail1_file_id", resource.getThumbnail1().getFileId());
            params.put("thumbnail1_width", resource.getThumbnail1().getWidth());
            params.put("thumbnail1_height", resource.getThumbnail1().getHeight());
        }

        if (resource.getThumbnail2() != null) {
            if (resource.getThumbnail2().getFileId() == null) {
                params.put("thumbnail2_url", resource.getThumbnail2().getUrl());
            }
            params.put("thumbnail2_file_id", resource.getThumbnail2().getFileId());
            params.put("thumbnail2_width", resource.getThumbnail2().getWidth());
            params.put("thumbnail2_height", resource.getThumbnail2().getHeight());
        }

        if (resource.getThumbnail3() != null) {
            if (resource.getThumbnail3().getFileId() == null) {
                params.put("thumbnail3_url", resource.getThumbnail3().getUrl());
            }
            params.put("thumbnail3_file_id", resource.getThumbnail3().getFileId());
            params.put("thumbnail3_width", resource.getThumbnail3().getWidth());
            params.put("thumbnail3_height", resource.getThumbnail3().getHeight());
        }

        if (resource.getThumbnail4() != null) {
            if (resource.getThumbnail4().getFileId() == null) {
                params.put("thumbnail4_url", resource.getThumbnail4().getUrl());
            }
            params.put("thumbnail4_file_id", resource.getThumbnail4().getFileId());
            params.put("thumbnail4_width", resource.getThumbnail4().getWidth());
            params.put("thumbnail4_height", resource.getThumbnail4().getHeight());
        }

        Optional<Integer> resourceId = SqlHelper.handleSave(getHandle(), "lw_resource", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        resourceId.ifPresent(id -> {
            resource.setId(id);
            cache.put(resource);
        });

        // TODO: do something with location
        // resource.setLocation(getLocation(resource));

        // persist the relation between the resource and its files
        getFileDao().updateResource(resource, resource.getFiles().values());

        Learnweb.getInstance().getSolrClient().reIndexResource(resource);
    }

    default void deleteSoft(int resourceId) {
        // delete resource from SOLR index
        try {
            Learnweb.getInstance().getSolrClient().deleteFromIndex(resourceId);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't delete resource " + resourceId + " from SOLR", e);
        }

        // flag the resource as deleted
        getHandle().execute("UPDATE lw_resource SET deleted = 1 WHERE resource_id = ?", resourceId);

        // remove resource from cache
        cache.remove(resourceId);
    }

    /**
     * Don't use this function.
     * Usually you have to call deleteSoft()
     */
    default void deleteHard(int resourceId) {
        // log.debug("Hard delete resource: " + resourceId);

        deleteSoft(resourceId); // clear cache and remove resource from SOLR
        getHandle().execute("DELETE FROM lw_resource WHERE resource_id = ?", resourceId);

        // TODO @astappiev: delete files (lw_file); but it's not possible yet because files are shared when a resource is copied
    }

    class ResourceMapper implements RowMapper<Resource> {
        @Override
        public Resource map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Resource resource = cache.get(rs.getInt("resource_id"));

            if (resource == null) {
                ResourceType type = ResourceType.parse(rs.getString("type"));

                resource = newResource(type);
                resource.setId(rs.getInt("resource_id"));
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
                resource.setOriginalResourceId(RsHelper.getInteger(rs, "original_resource_id"));
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
                resource.setRestricted(rs.getBoolean("restricted"));
                resource.setResourceTimestamp(RsHelper.getLocalDateTime(rs.getTimestamp("resource_timestamp")));
                resource.setCreationDate(RsHelper.getLocalDateTime(rs.getTimestamp("creation_date")));
                resource.setGroupId(rs.getInt("group_id"));
                resource.setFolderId(rs.getInt("folder_id"));
                resource.setDeleted(rs.getBoolean("deleted"));
                resource.setReadOnlyTranscript(rs.getBoolean("read_only_transcript"));

                // This must be set manually because we stored some external sources in Learnweb/Solr
                resource.setLocation(getLocation(resource));

                if (resource.isDeleted()) {
                    LogManager.getLogger(ResourceMapper.class).debug("resource {} was requested but is deleted", resource.getId());
                }

                // deserialize metadata
                byte[] metadataBytes = rs.getBytes("metadata");
                if (metadataBytes != null && metadataBytes.length > 0) {
                    try {
                        resource.setMetadata(SerializationUtils.deserialize(metadataBytes));
                    } catch (Exception e) {
                        LogManager.getLogger(ResourceMapper.class).error("Couldn't load metadata for resource {}", resource.getId(), e);
                    }
                }

                resource.postConstruct();
                cache.put(resource);
            }
            return resource;
        }

        /**
         * Creates appropriate Resource instances based on the resource type.
         * Necessary since some resource types extend the normal Resource class.
         */
        private static Resource newResource(ResourceType type) {
            switch (type) {
                case survey:
                    return new SurveyResource();
                case glossary:
                    return new GlossaryResource();
                default:
                    return new Resource();
            }
        }

        private static Thumbnail createThumbnail(ResultSet rs, int thumbnailSize) throws SQLException {
            String prefix = "thumbnail" + thumbnailSize;
            String url = rs.getString(prefix + "_url");
            Integer fileId = RsHelper.getInteger(rs, prefix + "_file_id");

            if (fileId != null) {
                url = "../download/" + fileId + "/thumbnail" + thumbnailSize + ".png";
            } else if (url == null) {
                return null;
            }

            return new Thumbnail(url, rs.getInt(prefix + "_width"), rs.getInt(prefix + "_height"), fileId);
        }

        /**
         * Returns the the location were a resource is stored. Necessary because some external sources are indexed in our Solr instance.
         */
        private static String getLocation(Resource resource) {
            ResourceService source = resource.getSource();

            if (null == source) {
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
    }
}

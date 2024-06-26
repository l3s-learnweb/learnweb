package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationUtils;
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
        if (resourceId == 0) {
            return Optional.empty();
        }
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

    @SqlQuery("SELECT * FROM lw_resource r WHERE group_id = ?")
    List<Resource> findByGroupId(int groupId, boolean ignored);

    @SqlQuery("SELECT * FROM lw_resource r WHERE folder_id = ? AND deleted = 0")
    List<Resource> findByFolderId(int folderId);

    @SqlQuery("SELECT * FROM lw_resource WHERE owner_user_id = ? AND deleted = 0")
    List<Resource> findByOwnerId(int userId);

    @SqlQuery("SELECT * FROM lw_resource WHERE owner_user_id = ?")
    List<Resource> findByOwnerId(int userId, boolean ignored);

    @SqlQuery("SELECT r.* FROM lw_resource r JOIN lw_resource_tag USING (resource_id) WHERE tag_id = ? AND deleted = 0")
    List<Resource> findByTagId(int tagId);

    @SqlQuery("SELECT r.* FROM lw_resource r JOIN lw_resource_tag USING (resource_id) WHERE tag_id = ? AND deleted = 0 LIMIT ?")
    List<Resource> findByTagId(int tagId, int limit);

    /**
     * Returns at most one (random) resource for the given URL. Note that the URL column isn't indexed. This method was added for a specific crawler and
     * shouldn't be used.
     */
    @SqlQuery("SELECT * FROM lw_resource WHERE group_id = ? AND url = ? AND deleted = 0 LIMIT 1")
    Optional<Resource> findByGroupIdAndUrl(int groupId, String url);

    @SqlQuery("SELECT * FROM lw_resource WHERE resource_id IN (SELECT DISTINCT resource_id FROM lw_resource_rating r WHERE r.user_id = ?) AND deleted = 0")
    List<Resource> findRatedByUsedId(int userId);

    @SqlQuery("SELECT * FROM lw_resource r WHERE group_id = ? AND folder_id = ? AND owner_user_id = ? AND deleted = 0 LIMIT ?")
    List<Resource> findByGroupIdAndFolderIdAndOwnerId(int groupId, int folderId, int userId, int limit);

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
    @SqlQuery("SELECT rating FROM lw_resource_rating WHERE resource_id = ? AND user_id = ? AND type = ?")
    Optional<Integer> findRating(int resourceId, int userId, String ratingType);

    default HashMap<String, ResourceRating> findRatings(Resource resource) {
        return getHandle().select("SELECT `user_id`, `type`, `rating` FROM lw_resource_rating WHERE resource_id = ?", resource.getId())
            .reduceRows(new HashMap<>(), (map, rowView) -> {
                ResourceRating rating = map.computeIfAbsent(rowView.getColumn("type", String.class), ResourceRating::new);
                rating.addRate(rowView.getColumn("user_id", Integer.class), rowView.getColumn("rating", Integer.class));
                return map;
            });
    }

    @SqlUpdate("INSERT INTO lw_resource_rating (resource_id, user_id, type, rating) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE rating = VALUES(rating)")
    void insertRating(Resource resource, User user, String ratingType, int value);

    @SqlUpdate("INSERT INTO lw_resource_tag (resource_id, user_id, tag_id) VALUES (?, ?, ?)")
    void insertTag(Resource resource, User user, Tag tag);

    @SqlUpdate("DELETE FROM lw_resource_tag WHERE resource_id = ? AND tag_id = ?")
    void deleteTag(Resource resource, Tag tag);

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
        params.put("query", SqlHelper.toNullable(resource.getQuery()));
        params.put("max_image_url", SqlHelper.toNullable(resource.getMaxImageUrl()));
        params.put("original_resource_id", SqlHelper.toNullable(resource.getOriginalResourceId()));
        params.put("machine_description", SqlHelper.toNullable(resource.getMachineDescription()));
        params.put("author", SqlHelper.toNullable(resource.getAuthor()));
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
        params.put("website_view_tab", resource.getDefaultTab().name());

        Optional<Integer> resourceId = SqlHelper.handleSave(getHandle(), "lw_resource", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (resourceId.isPresent() && resourceId.get() != 0) {
            resource.setId(resourceId.get());
            resource.setThumbnailSmall(null);
            cache.put(resource);
        }

        // persist the relation between the resource and its files
        getFileDao().deleteResourceFiles(resource, resource.getDeletedFiles());
        getFileDao().insertResourceFiles(resource, resource.getAddedFiles());

        Learnweb.getInstance().getSolrClient().reIndexResource(resource);
    }

    default void deleteSoft(Resource resource) {
        getHandle().execute("UPDATE lw_resource SET deleted = 1 WHERE resource_id = ?", resource);
        resource.setDeleted(true);

        try {
            Learnweb.getInstance().getSolrClient().deleteResource(resource);
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
        getHandle().execute("DELETE FROM lw_resource WHERE resource_id = ?", resource);

        try {
            Learnweb.getInstance().getSolrClient().deleteResource(resource);
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
                resource.setMaxImageUrl(rs.getString("max_image_url"));
                resource.setQuery(rs.getString("query"));
                resource.setOriginalResourceId(rs.getInt("original_resource_id"));
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
                resource.setDefaultTab(Resource.DefaultTab.valueOf(rs.getString("website_view_tab")));

                resource.postConstruct();
                cache.put(resource);
            }
            return resource;
        }
    }
}

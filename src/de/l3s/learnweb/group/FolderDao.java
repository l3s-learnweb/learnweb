package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(FolderDao.FolderMapper.class)
public interface FolderDao extends SqlObject, Serializable {
    ICache<Folder> cache = new Cache<>(10000);

    default Optional<Folder> findById(int folderId) {
        return Optional.ofNullable(cache.get(folderId))
            .or(() -> getHandle().select("SELECT * FROM lw_group_folder WHERE folder_id = ?", folderId).mapTo(Folder.class).findOne());
    }

    default Folder findByIdOrElseThrow(int fileId) {
        return findById(fileId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_object_description"));
    }

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id = ? AND parent_folder_id IS NULL")
    List<Folder> findByGroupAndRootFolder(int groupId);

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id = ? AND parent_folder_id = ?")
    List<Folder> findByGroupAndFolder(int groupId, int folderId);

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id IS NULL AND parent_folder_id IS NULL AND user_id = ?")
    List<Folder> findByPrivateGroupAndRootFolder(int userId);

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id IS NULL AND parent_folder_id = ? AND user_id = ?")
    List<Folder> findByPrivateGroupAndFolder(int folderId, int userId);

    default void deleteSoft(Folder folder) {
        getHandle().execute("UPDATE lw_group_folder SET deleted = 1 WHERE folder_id = ?", folder);

        folder.setDeleted(true);
        cache.remove(folder.getId());
    }

    default void deleteHard(Folder folder) {
        ResourceDao resourceDao = getHandle().attach(ResourceDao.class);
        for (Resource resource : folder.getResources()) {
            resourceDao.deleteHard(resource);
        }

        getHandle().execute("DELETE FROM lw_group_folder WHERE folder_id = ?", folder);
        cache.remove(folder.getId());
    }

    default void save(Folder folder) {
        if (folder.getCreatedAt() == null) {
            folder.setCreatedAt(SqlHelper.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("folder_id", SqlHelper.toNullable(folder.getId()));
        params.put("group_id", SqlHelper.toNullable(folder.getGroupId()));
        params.put("parent_folder_id", SqlHelper.toNullable(folder.getParentFolderId()));
        params.put("user_id", SqlHelper.toNullable(folder.getUserId()));
        params.put("deleted", folder.isDeleted());
        params.put("title", folder.getTitle());
        params.put("description", SqlHelper.toNullable(folder.getDescription()));
        params.put("created_at", folder.getCreatedAt());

        Optional<Integer> folderId = SqlHelper.handleSave(getHandle(), "lw_group_folder", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        folderId.ifPresent(id -> {
            folder.setId(id);
            cache.put(folder);
        });

        folder.clearCaches();
    }

    class FolderMapper implements RowMapper<Folder> {
        @Override
        public Folder map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Folder folder = cache.get(rs.getInt("folder_id"));

            if (folder == null) {
                folder = new Folder();
                folder.setId(rs.getInt("folder_id"));
                folder.setGroupId(rs.getInt("group_id"));
                folder.setParentFolderId(rs.getInt("parent_folder_id"));
                folder.setUserId(rs.getInt("user_id"));
                folder.setDeleted(rs.getBoolean("deleted"));
                folder.setTitle(rs.getString("title"));
                folder.setDescription(rs.getString("description"));
                folder.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
                cache.put(folder);
            }

            return folder;
        }
    }
}

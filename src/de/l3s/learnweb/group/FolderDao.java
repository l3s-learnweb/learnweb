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
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.resource.Folder;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.RsHelper;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(FolderDao.FolderMapper.class)
public interface FolderDao extends SqlObject, Serializable {
    ICache<Folder> cache = new Cache<>(10000);

    default Folder findById(int folderId) {
        Folder folder = cache.get(folderId);
        if (folder != null) {
            return folder;
        }

        return getHandle().select("SELECT * FROM lw_group_folder WHERE deleted = 0 AND folder_id = ?", folderId)
            .map(new FolderMapper()).findOne().orElse(null);
    }

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id = ? AND parent_folder_id IS NULL")
    List<Folder> findByGroupAndRootFolder(int groupId);

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id = ? AND parent_folder_id = ?")
    List<Folder> findByGroupAndFolder(int groupId, int folderId);

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id IS NULL AND parent_folder_id IS NULL AND user_id = ?")
    List<Folder> findByPrivateGroupAndRootFolder(int userId);

    @SqlQuery("SELECT * FROM lw_group_folder WHERE deleted = 0 AND group_id IS NULL AND parent_folder_id = ? AND user_id = ?")
    List<Folder> findByPrivateGroupAndFolder(int folderId, int userId);

    @SqlUpdate("UPDATE lw_group_folder SET deleted = 1 WHERE folder_id = ?")
    void deleteSoft(int folderId);

    default void save(Folder folder) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("folder_id", folder.getId() < 1 ? null : folder.getId());
        params.put("group_id", folder.getGroupId() == 0 ? null : folder.getGroupId());
        params.put("parent_folder_id", folder.getParentFolderId() == 0 ? null : folder.getParentFolderId());
        params.put("name", folder.getTitle());
        params.put("description", folder.getDescription());
        params.put("user_id", folder.getUserId());
        params.put("deleted", folder.isDeleted());

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
                folder.setTitle(rs.getString("name"));
                folder.setDescription(rs.getString("description"));
                folder.setUserId(RsHelper.getInteger(rs, "user_id"));
                folder.setDeleted(rs.getBoolean("deleted"));
                cache.put(folder);
            }

            return folder;
        }
    }
}
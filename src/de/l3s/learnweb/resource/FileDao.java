package de.l3s.learnweb.resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.Cache;
import de.l3s.util.HasId;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(FileDao.FileMapper.class)
public interface FileDao extends SqlObject, Serializable {
    ICache<File> cache = new Cache<>(3000);

    default File findById(int fileId) {
        File course = cache.get(fileId);
        if (course != null) {
            return course;
        }

        return getHandle().select("SELECT * FROM lw_file WHERE file_id = ?", fileId)
            .map(new FileMapper()).findOne().orElse(null);
    }

    @SqlQuery("SELECT * FROM lw_file WHERE deleted = 0 ORDER BY resource_id DESC")
    List<File> findAll();

    @SqlQuery("SELECT * FROM lw_file WHERE resource_id = ? AND deleted = 0 ORDER by resource_file_number, timestamp")
    List<File> findByResourceId(int resourceId);

    @SqlUpdate("UPDATE lw_file SET resource_id = ? WHERE file_id = ?")
    void updateResource(Resource resource, File file);

    default void updateResource(Resource resource, Collection<File> files) {
        if (files.isEmpty()) {
            return;
        }

        files.forEach(file -> file.setResourceId(resource.getId()));

        getHandle().createUpdate("UPDATE lw_file SET resource_id = :resId WHERE file_id IN(<fileIds>)")
            .bind("resId", resource.getId())
            .bindList("fileIds", HasId.collectIds(files))
            .execute();
    }

    default void deleteSoft(int fileId) {
        deleteSoft(findById(fileId));
    }

    default void deleteSoft(File file) {
        getHandle().execute("UPDATE lw_file SET deleted = 1 WHERE file_id = ?", file);
        cache.remove(file.getId());

        if (file.exists()) {
            try {
                file.getActualFile().delete();
            } catch (Throwable e) {
                LogManager.getLogger(FileDao.class).error("Could not delete file: {}", file.getId(), e);
            }
        }
    }

    /**
     * Saves the file to the database.
     * If the file is not yet stored at the database, a new record will be created and the returned file contains the new id.
     */
    default void save(File file, InputStream inputStream) throws IOException {
        if (file.getLastModified() == null) {
            file.setLastModified(LocalDateTime.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("file_id", SqlHelper.toNullable(file.getId()));
        params.put("resource_id", SqlHelper.toNullable(file.getResourceId()));
        params.put("resource_file_number", file.getType().ordinal());
        params.put("name", file.getName());
        params.put("mime_type", file.getMimeType());
        params.put("log_actived", file.isDownloadLogActivated());
        params.put("timestamp", file.getLastModified());

        Optional<Integer> fileId = SqlHelper.handleSave(getHandle(), "lw_file", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        fileId.ifPresent(id -> {
            file.setId(id);
            cache.put(file);
        });

        if (inputStream != null) {
            // copy the data into the file
            try (inputStream; OutputStream outputStream = new FileOutputStream(file.getActualFile())) {
                IOUtils.copy(inputStream, outputStream);
            }
        }
    }

    class FileMapper implements RowMapper<File> {
        @Override
        public File map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            File file = cache.get(rs.getInt("file_id"));
            if (file == null) {
                file = new File();
                file.setId(rs.getInt("file_id"));
                file.setResourceId(rs.getInt("resource_id"));
                file.setType(File.TYPE.values()[rs.getInt("resource_file_number")]);
                file.setName(rs.getString("name"));
                file.setMimeType(rs.getString("mime_type"));
                file.setDownloadLogActivated(rs.getBoolean("log_actived"));
                file.setLastModified(SqlHelper.getLocalDateTime(rs.getTimestamp("timestamp")));

                if (!file.getActualFile().exists()) {
                    LogManager.getLogger(FileMapper.class).warn("Can't find file '{}' for resource {}",
                        file.getActualFile().getAbsolutePath(), file.getResourceId());
                    file.setExists(false);
                }

                cache.put(file);
            }
            return file;
        }
    }
}

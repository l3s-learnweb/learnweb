package de.l3s.learnweb.resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.FetchSize;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(FileDao.FileMapper.class)
public interface FileDao extends SqlObject, Serializable {
    ICache<File> cache = new Cache<>(3000);

    default Optional<File> findById(int fileId) {
        return Optional.ofNullable(cache.get(fileId))
            .or(() -> getHandle().select("SELECT * FROM lw_file WHERE file_id = ?", fileId).mapTo(File.class).findOne());
    }

    default File findByIdOrElseThrow(int fileId) {
        return findById(fileId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_object_description"));
    }

    @FetchSize(1000)
    @SqlQuery("SELECT * FROM lw_file")
    Stream<File> findAll();

    @SqlQuery("SELECT f.* FROM lw_file f JOIN lw_resource_file rf USING (file_id) WHERE rf.resource_id = ?")
    List<File> findByResourceId(int resourceId);

    default void insertResourceFiles(Resource resource, Collection<File> files) {
        if (!files.isEmpty()) {
            PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_resource_file (resource_id, file_id) VALUES (?, ?) ON DUPLICATE KEY "
                + "UPDATE resource_id = VALUES(resource_id), file_id = VALUES(file_id)");
            for (File file : files) {
                batch.bind(0, resource.getId()).bind(1, file.getId()).add();
            }
            batch.execute();
        }
    }

    default void deleteResourceFiles(Resource resource, Collection<File> files) {
        if (!files.isEmpty()) {
            PreparedBatch batch = getHandle().prepareBatch("DELETE FROM lw_resource_file WHERE resource_id = ? AND file_id = ?");
            for (File file : files) {
                batch.bind(0, resource.getId()).bind(1, file.getId()).add();
            }
            batch.execute();
        }
    }

    default void deleteHard(File file) {
        getHandle().execute("DELETE FROM lw_file WHERE file_id = ?", file);
        cache.remove(file.getId());

        if (file.isExists()) {
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
    default void save(File file) {
        if (file.getCreatedAt() == null) {
            file.setCreatedAt(SqlHelper.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("file_id", SqlHelper.toNullable(file.getId()));
        params.put("type", file.getType().name());
        params.put("name", file.getName());
        params.put("mime_type", file.getMimeType());
        params.put("created_at", file.getCreatedAt());

        Optional<Integer> fileId = SqlHelper.handleSave(getHandle(), "lw_file", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        fileId.ifPresent(id -> {
            file.setId(id);
            cache.put(file);
        });
    }

    default void save(File file, InputStream inputStream) throws IOException {
        save(file);

        if (inputStream != null) {
            // copy the data into the file
            try (inputStream; OutputStream outputStream = new FileOutputStream(file.getActualFile())) {
                IOUtils.copy(inputStream, outputStream);
            }
        }
    }

    class FileMapper implements RowMapper<File> {
        private static final Logger log = LogManager.getLogger(FileMapper.class);

        @Override
        public File map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            File file = cache.get(rs.getInt("file_id"));
            if (file == null) {
                file = new File(File.FileType.valueOf(rs.getString("type")), rs.getString("name"), rs.getString("mime_type"));
                file.setId(rs.getInt("file_id"));
                file.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));

                if (!file.isExists()) {
                    log.warn("Can't find file {} at '{}'", file.getId(), file.getActualFile().getAbsolutePath());
                }

                cache.put(file);
            }
            return file;
        }
    }
}

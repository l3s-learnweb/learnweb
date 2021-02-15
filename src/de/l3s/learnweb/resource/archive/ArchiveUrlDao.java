package de.l3s.learnweb.resource.archive;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.RsHelper;

public interface ArchiveUrlDao extends SqlObject, Serializable {
    @RegisterRowMapper(ArchiveUrlMapper.class)
    @SqlQuery("SELECT * FROM lw_resource_archiveurl WHERE resource_id = ? ORDER BY timestamp")
    List<ArchiveUrl> findByResourceId(int resourceId);

    @RegisterRowMapper(ArchiveUrlMapper.class)
    @SqlQuery("SELECT * FROM lw_resource_archiveurl WHERE resource_id = ? AND DATE(timestamp) = DATE(?)")
    List<ArchiveUrl> findByResourceId(int resourceId, LocalDate timestamp);

    // TODO: `file_id` is not existing column
    @SqlUpdate("UPDATE lw_resource_archiveurl SET file_id = ?  WHERE resource_id=? and archive_url=?")
    void updateFIleId(int fileId, int resourceId, String archiveUrl);

    @SqlUpdate("INSERT into lw_resource_archiveurl(resource_id,archive_url,timestamp) VALUES (?, ?, ?)")
    void insertArchiveUrl(int resourceId, String archiveUrl, LocalDateTime timestamp);

    @SqlBatch("INSERT into lw_resource_archiveurl(resource_id,archive_url,timestamp) VALUES (:resourceId, :getArchiveUrl, :getTimestamp)")
    void insertArchiveUrl(@Bind("resourceId") int resourceId, @BindMethods Collection<ArchiveUrl> archiveUrls);

    class ArchiveUrlMapper implements RowMapper<ArchiveUrl> {
        @Override
        public ArchiveUrl map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new ArchiveUrl(rs.getString("archive_url"), RsHelper.getLocalDateTime(rs.getTimestamp("timestamp")));
        }
    }
}

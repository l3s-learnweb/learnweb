package de.l3s.learnweb.resource.archive;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;

public interface ArchiveUrlDao extends SqlObject, Serializable {
    @RegisterRowMapper(ArchiveUrlMapper.class)
    @SqlQuery("SELECT * FROM lw_resource_archiveurl WHERE resource_id = ? ORDER BY timestamp")
    List<ArchiveUrl> findByResourceId(int resourceId);

    @RegisterRowMapper(ArchiveUrlMapper.class)
    @SqlQuery("SELECT * FROM lw_resource_archiveurl WHERE resource_id = ? AND DATE(timestamp) = DATE(?)")
    List<ArchiveUrl> findByResourceId(int resourceId, LocalDate timestamp);

    @SqlUpdate("INSERT INTO lw_resource_archiveurl(resource_id, archive_url, timestamp) VALUES (?, ?, ?)")
    void insertArchiveUrl(int resourceId, String archiveUrl, LocalDateTime timestamp);

    @SqlBatch("INSERT INTO lw_resource_archiveurl(resource_id, archive_url, timestamp) VALUES (:resourceId, :archiveUrl, :timestamp)")
    void insertArchiveUrl(@Bind("resourceId") int resourceId, @BindMethods Collection<ArchiveUrl> archiveUrls);

    default TreeMap<LocalDate, Integer> countSnapshotsByDays(int resourceId) {
        return getHandle().select("""
                SELECT CAST(timestamp AS DATE) AS date, COUNT(*) AS count
                FROM lw_resource_archiveurl
                WHERE resource_id = ?
                GROUP BY YEAR(timestamp), MONTH(timestamp), DAY(timestamp)
                ORDER BY timestamp ASC
                """, resourceId)
            .reduceResultSet(new TreeMap<>(), (map, rs, ctx) -> {
                LocalDate date = SqlHelper.getLocalDate(rs.getDate("date"));
                map.put(date, rs.getInt("count"));
                return map;
            });
    }

    default TreeMap<LocalDate, Integer> countSnapshotsByMonths(int resourceId) {
        return getHandle().select("""
                SELECT CAST(DATE_FORMAT(timestamp, '%Y-%m-01') AS DATE) AS date, COUNT(*) AS count
                FROM lw_resource_archiveurl
                WHERE resource_id = ?
                GROUP BY YEAR(timestamp), MONTH(timestamp)
                ORDER BY timestamp ASC
                """, resourceId)
            .reduceResultSet(new TreeMap<>(), (map, rs, ctx) -> {
                LocalDate date = SqlHelper.getLocalDate(rs.getDate("date"));
                map.put(date, rs.getInt("count"));
                return map;
            });
    }

    class ArchiveUrlMapper implements RowMapper<ArchiveUrl> {
        @Override
        public ArchiveUrl map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new ArchiveUrl(rs.getString("archive_url"), SqlHelper.getLocalDateTime(rs.getTimestamp("timestamp")));
        }
    }
}

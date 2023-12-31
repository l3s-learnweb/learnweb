package de.l3s.learnweb.resource.archive;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;
import de.l3s.util.URL;

public interface WaybackUrlDao extends SqlObject, Serializable {
    DateTimeFormatter waybackDateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US);

    default Optional<ImmutablePair<String, String>> findFirstAndLastCapture(String url) {
        return getHandle().select("SELECT first_capture, last_capture FROM learnweb_large.wb2_url WHERE url = ?", url).map((rs, ctx) -> {
            LocalDateTime first = rs.getTimestamp("first_capture").toLocalDateTime();
            LocalDateTime last = rs.getTimestamp("last_capture").toLocalDateTime();
            return new ImmutablePair<>(waybackDateFormat.format(first), waybackDateFormat.format(last));
        }).findOne();
    }

    default Optional<Integer> findIdByUrl(String url) {
        return getHandle().select("SELECT url_id FROM learnweb_large.wb2_url WHERE url = ?", url).mapTo(Integer.class).findFirst();
    }

    @SqlQuery("SELECT timestamp FROM learnweb_large.wb2_url_capture WHERE url_id = ? ORDER BY timestamp")
    List<LocalDateTime> findUrlCaptures(int urlId);

    @SqlQuery("SELECT timestamp FROM learnweb_large.wb2_url_capture WHERE url_id = ? AND DATE(timestamp) = DATE(?) ORDER BY timestamp")
    List<LocalDateTime> findUrlCaptures(int urlId, LocalDate timestamp);

    @SqlUpdate("UPDATE learnweb_large.wb2_url SET all_captures_fetched = 1 WHERE url_id = ?")
    void updateMarkAllCapturesFetched(int urlId);

    @SqlUpdate("INSERT INTO learnweb_large.wb2_url (url, first_capture, last_capture) VALUES (?, ?, ?)")
    void insert(String url, LocalDateTime firstCapture, LocalDateTime lastCapture);

    @SqlBatch("INSERT INTO learnweb_large.wb2_url_capture (url_id,timestamp) VALUES(?, ?)")
    void insertCapture(int urlId, Collection<LocalDateTime> timestamp);

    @RegisterRowMapper(UrlRecordMapper.class)
    @SqlQuery("SELECT * FROM learnweb_large.wb_url WHERE url = ?")
    Optional<UrlRecord> findUrlRecordByUrl(String url);

    default void saveUrlRecord(UrlRecord record) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("url_id", SqlHelper.toNullable(record.getId()));
        params.put("url", record.getUrl());
        params.put("first_capture", record.getFirstCapture());
        params.put("last_capture", record.getLastCapture());
        params.put("crawl_time", record.getCrawlDate());
        params.put("all_captures_fetched", record.isAllCapturesFetched());
        params.put("status_code", record.getStatusCode());
        params.put("status_code_date", record.getStatusCodeDate());

        Optional<Long> urlId = SqlHelper.handleSave(getHandle(), "learnweb_large.wb_url", params)
            .executeAndReturnGeneratedKeys().mapTo(Long.class).findOne();

        if (urlId.isPresent() && urlId.get() != 0) {
            record.setId(urlId.get());
        }
    }

    default List<ArchiveUrl> findByUrl(String url) {
        List<ArchiveUrl> archiveUrls = new LinkedList<>();

        findIdByUrl(url).ifPresent(urlId ->
            findUrlCaptures(urlId).forEach(date ->
                archiveUrls.add(new ArchiveUrl("https://web.archive.org/web/" + waybackDateFormat.format(date) + "/" + url, date))));

        return archiveUrls;
    }

    default List<ArchiveUrl> findByUrl(String url, LocalDate timestamp) {
        List<ArchiveUrl> archiveUrls = new LinkedList<>();

        findIdByUrl(url).ifPresent(urlId ->
            findUrlCaptures(urlId, timestamp).forEach(date ->
                archiveUrls.add(new ArchiveUrl("https://web.archive.org/web/" + waybackDateFormat.format(date) + "/" + url, date))));

        return archiveUrls;
    }

    default TreeMap<LocalDate, Integer> countSnapshotsGroupedByMonths(int resourceId, String url) {
        TreeMap<LocalDate, Integer> monthlySeriesData = new TreeMap<>();

        getHandle().select("SELECT CAST(DATE_FORMAT(timestamp, '%Y-%m-01') as DATE) as date, count(*) as count FROM lw_resource_archiveurl "
            + "WHERE resource_id = ? GROUP BY year(timestamp), month(timestamp) ORDER BY timestamp ASC", resourceId)
            .map((rs, ctx) -> {
                monthlySeriesData.put(SqlHelper.getLocalDate(rs.getDate("date")), rs.getInt("count"));
                return null;
            }).list();

        Optional<Integer> urlId = findIdByUrl(url);
        if (urlId.isPresent()) {
            getHandle().select("SELECT CAST(DATE_FORMAT(timestamp, '%Y-%m-01') as DATE) as date, count(*) as count FROM learnweb_large.wb2_url_capture "
                + "WHERE url_id = ? GROUP BY year(timestamp), month(timestamp) ORDER BY timestamp ASC", resourceId)
                .map((rs, ctx) -> {
                    LocalDate timestamp = SqlHelper.getLocalDate(rs.getDate("date"));
                    if (monthlySeriesData.containsKey(timestamp)) {
                        monthlySeriesData.put(timestamp, monthlySeriesData.get(timestamp) + rs.getInt("count"));
                    } else {
                        monthlySeriesData.put(timestamp, rs.getInt("count"));
                    }
                    return null;
                }).list();
        }

        return monthlySeriesData;
    }

    default TreeMap<LocalDate, Integer> countSnapshotsGroupedByDays(int resourceId, String url) {
        TreeMap<LocalDate, Integer> monthlySeriesData = new TreeMap<>();

        getHandle().select("SELECT CAST(timestamp as DATE) as date, count(*) as count FROM lw_resource_archiveurl "
            + "WHERE resource_id = ? GROUP BY YEAR(timestamp),MONTH(timestamp),DAY(timestamp) ORDER BY timestamp ASC", resourceId)
            .map((rs, ctx) -> {
                monthlySeriesData.put(SqlHelper.getLocalDate(rs.getDate("date")), rs.getInt("count"));
                return null;
            }).list();

        Optional<Integer> urlId = findIdByUrl(url);
        if (urlId.isPresent()) {
            getHandle().select("SELECT CAST(timestamp as DATE) as date, count(*) as count FROM learnweb_large.wb2_url_capture "
                + "WHERE url_id = ? GROUP BY YEAR(timestamp),MONTH(timestamp),DAY(timestamp) ORDER BY timestamp ASC", resourceId)
                .map((rs, ctx) -> {
                    LocalDate timestamp = SqlHelper.getLocalDate(rs.getDate("date"));
                    if (monthlySeriesData.containsKey(timestamp)) {
                        monthlySeriesData.put(timestamp, monthlySeriesData.get(timestamp) + rs.getInt("count"));
                    } else {
                        monthlySeriesData.put(timestamp, rs.getInt("count"));
                    }
                    return null;
                }).list();
        }

        return monthlySeriesData;
    }

    class UrlRecordMapper implements RowMapper<UrlRecord> {
        @Override
        public UrlRecord map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            try {
                UrlRecord record = new UrlRecord(new URL(rs.getString("url")));
                record.setId(rs.getLong(1));
                record.setFirstCapture(SqlHelper.getLocalDateTime(rs.getTimestamp(2)));
                record.setLastCapture(SqlHelper.getLocalDateTime(rs.getTimestamp(3)));
                record.setAllCapturesFetched(rs.getBoolean(4));
                record.setCrawlDate(SqlHelper.getInstant(rs.getTimestamp(5)));
                record.setStatusCode(rs.getShort(6));
                record.setStatusCodeDate(SqlHelper.getInstant(rs.getTimestamp(7)));
                return record;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}

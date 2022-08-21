package de.l3s.learnweb.resource.ted;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.util.SqlHelper;

public interface TedTranscriptDao extends SqlObject, Serializable {

    @SqlQuery("SELECT resource_id FROM learnweb_large.ted_video WHERE ted_id = ?")
    Optional<Integer> findResourceIdByTedId(int tedId);

    @RegisterRowMapper(TedVideoMapper.class)
    @SqlQuery("SELECT * FROM learnweb_large.ted_video WHERE resource_id = ?")
    Optional<TedVideo> findTedVideoByResourceId(int resourceId);

    @RegisterRowMapper(TedVideoMapper.class)
    @SqlQuery("SELECT * FROM learnweb_large.ted_video ")
    List<TedVideo> findAllTedVideos();

    default Optional<Integer> findResourceIdBySlug(String url) {
        String slug = url.substring(url.lastIndexOf('/') + 1);
        return getHandle().select("SELECT resource_id FROM learnweb_large.ted_video WHERE slug = ?", slug).mapTo(Integer.class).findOne();
    }

    @SqlQuery("SELECT resource_id FROM lw_resource WHERE url = ? and owner_user_id = 7727")
    Optional<Integer> findResourceIdByTedXUrl(String url);

    @RegisterRowMapper(TranscriptLogMapper.class)
    @SqlQuery("SELECT a.* FROM lw_transcript_log a JOIN lw_resource USING(resource_id) WHERE user_id IN(<userIds>) and deleted = 0 ORDER BY user_id, created_at DESC")
    List<TranscriptLog> findTranscriptLogsByUserIds(@BindList("userIds") Collection<Integer> userIds);

    @RegisterRowMapper(SimpleTranscriptLogMapper.class)
    @SqlQuery("SELECT t1.owner_user_id, t1.resource_id, title, SUM(action = 'selection') as selcount, SUM(action = 'deselection') as deselcount, SUM(user_annotation != '') as uacount "
        + "FROM lw_resource t1 LEFT JOIN lw_transcript_log t2 ON t1.resource_id = t2.resource_id "
        + "WHERE (action = 'selection' OR action = 'deselection' OR user_annotation != '' OR action IS NULL) AND t1.owner_user_id IN (<userIds>) AND t1.deleted = 0 "
        + "GROUP BY t1.owner_user_id, t1.resource_id, title")
    List<SimpleTranscriptLog> findSimpleTranscriptLogs(@BindList("userIds") Collection<Integer> userIds);

    @SqlUpdate("DELETE FROM learnweb_large.ted_transcripts_paragraphs WHERE resource_id = ?")
    int deleteTranscriptParagraphs(int resourceId);

    @RegisterRowMapper(TranscriptSummaryMapper.class)
    @SqlQuery("SELECT * FROM lw_transcript_summary WHERE user_id IN (<userIds>) ORDER BY user_id")
    List<TranscriptSummary> findTranscriptSummariesByUserIds(@BindList("userIds") Collection<Integer> userIds);

    @SqlQuery("SELECT DISTINCT language FROM learnweb_large.ted_transcripts_paragraphs WHERE resource_id = ?")
    List<String> findLanguagesByResourceId(int resourceId);

    @SqlQuery("SELECT DISTINCT(t1.language) as language_code, t2.language FROM learnweb_large.ted_transcripts_paragraphs t1 JOIN learnweb_large.ted_transcripts_lang_mapping t2 ON t1.language=t2.language_code WHERE resource_id = ?")
    @KeyColumn("language")
    @ValueColumn("language_code")
    Map<String, String> findLanguages(int resourceId);

    @RegisterRowMapper(TranscriptSummaryMapper.class)
    @SqlQuery("SELECT * FROM lw_transcript_summary WHERE resource_id = ?")
    List<TranscriptSummary> findTranscriptSummariesByResourceId(int resourceId);

    default List<Transcript.Paragraph> findTranscriptsParagraphs(int resourceId, String language) {
        return getHandle().select("SELECT starttime, paragraph FROM learnweb_large.ted_transcripts_paragraphs WHERE resource_id = ? AND language = ?", resourceId, language)
            .map((rs, ctx) -> new Transcript.Paragraph(rs.getInt("starttime"), rs.getString("paragraph")))
            .list();
    }

    default List<Transcript> findTranscriptsByResourceId(int resourceId) {
        return getHandle().select("SELECT DISTINCT(language) as language_code FROM learnweb_large.ted_transcripts_paragraphs WHERE resource_id = ?", resourceId)
            .map((rs, ctx) -> {
                Transcript transcript = new Transcript();
                transcript.setLanguageCode(rs.getString("language_code"));
                transcript.setParagraphs(findTranscriptsParagraphs(resourceId, transcript.getLanguageCode()));
                return transcript;
            }).list();
    }

    @SqlUpdate("UPDATE learnweb_large.ted_video SET resource_id = ? WHERE ted_id = ?")
    void updateResourceIdByTedId(int resourceId, int tedId);

    @SqlUpdate("UPDATE learnweb_large.ted_video SET title = ?, description = ?, slug = ? WHERE resource_id = ?")
    int updateTedVideo(String title, String description, String slug, int resourceId);

    @SqlUpdate("INSERT INTO lw_transcript_summary (user_id, resource_id, summary_type, summary_text) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE summary_text = VALUES(summary_text)")
    void saveTranscriptSummary(int userId, int resourceId, TedManager.SummaryType summaryType, String summaryText);

    @SqlUpdate("INSERT INTO learnweb_large.ted_transcripts_paragraphs (resource_id, language, starttime, paragraph) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE starttime = VALUES(starttime), paragraph = VALUES(paragraph)")
    void saveTranscriptParagraphs(int resourceId, String langCode, int starttime, String paragraph);

    @SqlUpdate("INSERT INTO learnweb_large.ted_transcripts_lang_mapping (language_code,language) VALUES (?,?) ON DUPLICATE KEY UPDATE language_code = language_code")
    void saveTranscriptLangMapping(String langCode, String language);

    default void saveTranscriptLog(TranscriptLog transcriptLog) {
        getHandle().createUpdate("INSERT into lw_transcript_log(user_id,resource_id,words_selected,user_annotation,action,created_at) VALUES (?,?,?,?,?,?)")
            .bind(0, transcriptLog.getUserId())
            .bind(1, transcriptLog.getResourceId())
            .bind(2, transcriptLog.getWordsSelected())
            .bind(3, transcriptLog.getUserAnnotation())
            .bind(4, transcriptLog.getAction())
            .bind(5, transcriptLog.getTimestamp())
            .execute();
    }

    default void saveTranscriptSelection(String transcript, int resourceId) {
        if (StringUtils.isEmpty(transcript)) {
            return;
        }

        PreparedBatch batch = getHandle()
            .prepareBatch("INSERT INTO lw_transcript_selections(resource_id,words_selected,user_annotation,start_offset,end_offset) VALUES (?,?,?,?,?)");

        Elements elements = Jsoup.parse(transcript).select("span");
        for (Element element : elements) {
            int start = 0;
            int end = 0;
            if (!element.attr("data-start").isEmpty()) {
                start = Integer.parseInt(element.attr("data-start"));
            }
            if (!element.attr("data-end").isEmpty()) {
                end = Integer.parseInt(element.attr("data-end"));
            }

            batch.add(resourceId, element.text(), element.attr("data-title"), start, end);
        }

        batch.execute();
    }

    default void saveTedVideo(TedVideo video) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("ted_id", SqlHelper.toNullable(video.getTedId()));
        params.put("resource_id", SqlHelper.toNullable(video.getResourceId()));
        params.put("slug", video.getSlug());
        params.put("title", video.getTitle());
        params.put("description", video.getDescription());
        params.put("viewed_count", video.getViewedCount());
        params.put("published_at", video.getPublishedAt());
        params.put("photo1_url", video.getPhotoUrl());
        params.put("photo1_width", video.getPhotoWidth());
        params.put("photo1_height", video.getPhotoHeight());
        params.put("tags", video.getTags());
        params.put("duration", video.getDuration());

        Optional<Integer> videoId = SqlHelper.handleSave(getHandle(), "learnweb_large.ted_video", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (videoId.isPresent() && videoId.get() != 0) {
            video.setTedId(videoId.get());
        }
    }

    class TedVideoMapper implements RowMapper<TedVideo> {
        @Override
        public TedVideo map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            TedVideo video = new TedVideo();
            video.setTedId(rs.getInt("ted_id"));
            video.setResourceId(rs.getInt("resource_id"));
            video.setSlug(rs.getString("slug"));
            video.setTitle(rs.getString("title"));
            video.setDescription(rs.getString("description"));
            video.setViewedCount(rs.getInt("viewed_count"));
            video.setPublishedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("published_at")));
            video.setPhotoUrl(rs.getString("photo1_url"));
            video.setPhotoWidth(rs.getInt("photo1_width"));
            video.setPhotoHeight(rs.getInt("photo1_height"));
            video.setTags(rs.getString("tags"));
            video.setDuration(rs.getInt("duration"));
            return video;
        }
    }

    class TranscriptLogMapper implements RowMapper<TranscriptLog> {
        @Override
        public TranscriptLog map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new TranscriptLog(
                rs.getInt("user_id"),
                rs.getInt("resource_id"),
                rs.getString("words_selected"),
                rs.getString("user_annotation"),
                rs.getString("action"),
                rs.getTimestamp("created_at").toInstant()
            );
        }
    }

    class SimpleTranscriptLogMapper implements RowMapper<SimpleTranscriptLog> {
        @Override
        public SimpleTranscriptLog map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new SimpleTranscriptLog(
                rs.getInt("owner_user_id"),
                rs.getInt("resource_id"),
                rs.getInt("selcount"),
                rs.getInt("deselcount"),
                rs.getInt("uacount")
            );
        }
    }

    class TranscriptSummaryMapper implements RowMapper<TranscriptSummary> {
        @Override
        public TranscriptSummary map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new TranscriptSummary(
                rs.getInt("user_id"),
                rs.getInt("resource_id"),
                rs.getString("summary_type"),
                rs.getString("summary_text")
            );
        }
    }
}

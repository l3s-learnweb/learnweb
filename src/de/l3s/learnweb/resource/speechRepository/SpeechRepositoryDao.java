package de.l3s.learnweb.resource.speechRepository;

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

import de.l3s.util.SqlHelper;

@RegisterRowMapper(SpeechRepositoryDao.SpeechRepositoryEntityMapper.class)
public interface SpeechRepositoryDao extends SqlObject, Serializable {
    @SqlQuery("SELECT * FROM learnweb_large.speechrepository_video WHERE id = ?")
    Optional<SpeechRepositoryEntity> findById(int speechId);

    @SqlQuery("SELECT * FROM learnweb_large.speechrepository_video WHERE learnweb_resource_id = ?")
    List<SpeechRepositoryEntity> findByResourceId(int resourceId);

    @SqlQuery("SELECT DISTINCT id FROM learnweb_large.speechrepository_video WHERE id = ?")
    boolean isExists(int speechId);

    @SqlUpdate("UPDATE learnweb_large.speechrepository_video SET learnweb_resource_id = ? WHERE id = ?")
    void updateResourceId(int resourceId, int speechId);

    @SqlUpdate("DELETE FROM learnweb_large.speechrepository_video WHERE id = ?")
    void delete(int speechId);

    default void save(SpeechRepositoryEntity speech) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("id", speech.getId() < 1 ? null : speech.getId());
        params.put("title", speech.getTitle());
        params.put("url", speech.getUrl());
        params.put("rights", speech.getRights());
        params.put("date", speech.getDate());
        params.put("description", speech.getDescription());
        params.put("notes", speech.getNotes());
        params.put("image_link", speech.getImageLink());
        params.put("video_link", speech.getVideoLink());
        params.put("duration", speech.getDuration());
        params.put("language", speech.getLanguage());
        params.put("level", speech.getLevel());
        params.put("use", speech.getUse());
        params.put("type", speech.getType());
        params.put("domains", speech.getDomains());
        params.put("terminology", speech.getTerminology());
        params.put("learnweb_resource_id", speech.getLearnwebResourceId());

        Optional<Integer> speechId = SqlHelper.handleSave(getHandle(), "learnweb_large.speechrepository_video", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        speechId.ifPresent(speech::setId);
    }

    class SpeechRepositoryEntityMapper implements RowMapper<SpeechRepositoryEntity> {
        @Override
        public SpeechRepositoryEntity map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SpeechRepositoryEntity speech = new SpeechRepositoryEntity();
            speech.setId(rs.getInt("id"));
            speech.setTitle(rs.getString("title"));
            speech.setUrl(rs.getString("url"));
            speech.setRights(rs.getString("rights"));
            speech.setDate(rs.getString("date"));
            speech.setDescription(rs.getString("description"));
            speech.setNotes(rs.getString("notes"));
            speech.setImageLink(rs.getString("image_link"));
            speech.setVideoLink(rs.getString("video_link"));
            speech.setDuration(rs.getInt("duration"));
            speech.setLanguage(rs.getString("language"));
            speech.setLevel(rs.getString("level"));
            speech.setUse(rs.getString("use"));
            speech.setType(rs.getString("type"));
            speech.setDomains(rs.getString("domains"));
            speech.setTerminology(rs.getString("terminology"));
            speech.setLearnwebResourceId(rs.getInt("learnweb_resource_id"));
            return speech;
        }
    }
}

package de.l3s.learnweb.pkg;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.dbpedia.RecognisedEntity;
import de.l3s.learnweb.user.User;
import de.l3s.util.SqlHelper;

public interface PKGraphDao extends SqlObject, Serializable {

    @RegisterRowMapper(RdfObjectMapper.class)
    @SqlQuery("SELECT * FROM learnweb_large.sl_rdf WHERE user_id = ?")
    Optional<RdfObject> findRdfById(int userId);

    @SqlUpdate("INSERT INTO learnweb_large.sl_rdf (user_id, group_id, rdf_value) VALUES (?, ?, ?)")
    @GetGeneratedKeys("id")
    int insertRdf(int userId, int groupId, String rdfValue);

    @SqlUpdate("UPDATE learnweb_large.sl_rdf SET rdf_value = ? WHERE user_id = ?")
    void updateRdf(String rdfValue, int userId);


    @RegisterRowMapper(RecognisedEntityMapper.class)
    @SqlQuery("SELECT * FROM learnweb_large.sl_recognised_entity WHERE uri = ? AND type = ? AND user_id = ?")
    Optional<RecognisedEntity> findEntityByUriAndType(String uri, String type, int userId);

    @RegisterRowMapper(RecognisedEntityMapper.class)
    @SqlQuery("SELECT * FROM learnweb_large.sl_recognised_entity WHERE user_id = ? ORDER BY created_at")
    List<RecognisedEntity> findEntityByUser(int userId);

    default void saveEntity(RecognisedEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(SqlHelper.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("entity_uri", SqlHelper.toNullable(entity.getUriId()));
        params.put("type", entity.getType());
        params.put("uri", entity.getUri());
        params.put("input_id", entity.getInputStreams());
        params.put("surface_form", entity.getSurfaceForm());
        params.put("session_id", entity.getSessionId());
        params.put("user_id", entity.getUserId());
        params.put("confidence", entity.getConfidence());
        params.put("created_at", entity.getCreatedAt());

        Optional<Integer> entityId = SqlHelper.handleSave(getHandle(), "learnweb_large.sl_recognised_entity", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (entityId.isPresent() && entityId.get() != 0) {
            entity.setUriId(entityId.get());
        }
    }


    @RegisterRowMapper(JsonSharedObjectMapper.class)
    @SqlQuery("SELECT id, shared_object FROM learnweb_large.sl_shared_object WHERE group_id = ? AND user_id = ? AND application = ?")
    List<JsonSharedObject> findObjectsByUserId(int groupId, int userId, String application);

    @RegisterRowMapper(JsonSharedObjectMapper.class)
    @SqlQuery("SELECT id, shared_object FROM learnweb_large.sl_shared_object WHERE user_id = ? AND application = ?")
    List<JsonSharedObject> findObjectsByUserId(int userId, String application);

    @RegisterRowMapper(JsonSharedObjectMapper.class)
    @SqlQuery("SELECT id, shared_object FROM learnweb_large.sl_shared_object WHERE group_id = ? AND application = ?")
    List<JsonSharedObject> findObjectsByGroupId(int groupId, String application);

    @SqlUpdate("UPDATE learnweb_large.sl_shared_object SET shared_object = ?, created_at = ? WHERE user_id = ? AND group_id = ? AND application = ?")
    void updateSharedObject(String sharedObject, LocalDateTime createdAt, int userId, int groupId, String application);

    @SqlUpdate("INSERT INTO learnweb_large.sl_shared_object (user_id, group_id, application, shared_object, created_at) VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP())")
    @GetGeneratedKeys("id")
    int insertSharedObject(int userId, int groupId, String application, String sharedObject);


    @SqlUpdate("INSERT INTO learnweb_large.sl_suggested_query (user_id, reference_query, query, source, `index`, options, graph) VALUES (?, ?, ?, ?, ?, ?, ?)")
    void insertSuggestedQuery(User user, String referenceQuery, String query, String source, int index, String options, String graph);


    @SqlUpdate("INSERT INTO learnweb_large.sl_search_entity SET search_id = ?, entity_uri = ?")
    int insertQueryResult(int searchId, int uriId);

    @SqlQuery("SELECT search_id FROM learnweb_large.sl_search_entity WHERE entity_uri = ?")
    List<Integer> findSearchIdByResult(int uriId);


    @SqlUpdate("INSERT INTO learnweb_large.sl_input_stream (user_id, type, object_id, content, date_created) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP())")
    @GetGeneratedKeys("id")
    int insertInputStream(int userId, String type, int objectId, String content);

    @RegisterRowMapper(InputStreamRdfMapper.class)
    @SqlQuery("SELECT * FROM learnweb_large.sl_input_stream WHERE id IN (<inputIds>)")
    List<InputStreamRdf> findInputContentById(@Define("inputIds") String inputIds);


    class RecognisedEntityMapper implements RowMapper<RecognisedEntity> {
        @Override
        public RecognisedEntity map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            RecognisedEntity annotation = new RecognisedEntity();
            annotation.setUriId(rs.getInt("entity_uri"));
            annotation.setUri(rs.getString("uri"));
            annotation.setConfidence(rs.getDouble("confidence"));
            annotation.setSurfaceForm(rs.getString("surface_form"));
            annotation.setType(rs.getString("type"));
            annotation.setSessionId(rs.getString("session_id"));
            annotation.setUserId(rs.getInt("user_id"));
            annotation.setInputStreams(rs.getString("input_id"));
            annotation.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return annotation;
        }
    }

    class JsonSharedObjectMapper implements RowMapper<JsonSharedObject> {
        @Override
        public JsonSharedObject map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            JsonSharedObject sharedObject = new JsonSharedObject(rs.getString("shared_object"), true);
            sharedObject.setId(rs.getInt("id"));
            return sharedObject;
        }
    }

    class RdfObjectMapper implements RowMapper<RdfObject> {
        @Override
        public RdfObject map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            RdfObject obj = new RdfObject(rs.getInt("user_id"), rs.getString("rdf_value"));
            return obj;
        }
    }

    class InputStreamRdfMapper implements RowMapper<InputStreamRdf> {
        @Override
        public InputStreamRdf map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            InputStreamRdf obj = new InputStreamRdf(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("type"),
                rs.getInt("object_id"),
                rs.getString("content"),
                rs.getDate("date_created"));
            return obj;
        }
    }
}

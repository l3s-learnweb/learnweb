package de.l3s.learnweb.resource.office;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.office.history.model.History;
import de.l3s.learnweb.resource.office.history.model.HistoryData;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(ResourceHistoryDao.HistoryMapper.class)
@RegisterRowMapper(ResourceHistoryDao.HistoryDataMapper.class)
public interface ResourceHistoryDao extends SqlObject, Serializable {
    DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @SqlQuery("SELECT h.*, u.user_id, u.username FROM lw_resource_history h join lw_user u USING(user_id) WHERE resource_history_id = ?")
    Optional<History> findById(int resourceHistoryId);

    @SqlQuery("SELECT h.*, u.user_id, u.username FROM lw_resource_history h join lw_user u USING(user_id) WHERE resource_id = ? ORDER BY document_version")
    List<History> findByResourceId(int resourceId);

    @SqlQuery("SELECT document_version FROM lw_resource_history WHERE resource_id = ? ORDER BY document_version LIMIT 1")
    Optional<Integer> findLastVersionByResourceId(int resourceId);

    @SqlQuery("select * from lw_resource_history where resource_id = ? and document_version = ?")
    Optional<HistoryData> findByResourceIdAndVersion(int resourceId, int version);

    @SqlUpdate("UPDATE lw_resource_history SET file_id = ? WHERE resource_id = ? AND document_version = ?")
    void updateFileIdByResourceIdAndVersion(int prevFileId, int resourceId, int version);

    default void save(History history) {
        if (history.getCreated() == null) {
            history.setCreated(LocalDateTime.now().format(CREATED_FORMAT));
        }

        if (history.getVersion() == null) {
            Optional<Integer> prevVersion = findLastVersionByResourceId(history.getResourceId());
            history.setVersion(prevVersion.orElse(0) + 1);
            prevVersion.ifPresent(version -> updateFileIdByResourceIdAndVersion(history.getPrevFileId(), history.getResourceId(), version));
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("resource_history_id", history.getId() < 1 ? null : history.getId());
        params.put("resource_id", history.getResourceId());
        params.put("user_id", history.getUser().get("id").getAsInt());
        params.put("file_id", history.getFileId());
        params.put("prev_file_id", history.getPrevFileId());
        params.put("changes_file_id", history.getChangesFileId());
        params.put("server_version", history.getServerVersion());
        params.put("document_created", history.getCreated());
        params.put("document_key", history.getKey());
        params.put("document_version", history.getVersion());
        params.put("document_changes", history.getChanges() != null ? history.getChanges().toString() : null);

        Optional<Integer> commentId = SqlHelper.handleSave(getHandle(), "lw_resource_history", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        commentId.ifPresent(history::setId);
    }

    class HistoryMapper implements RowMapper<History> {
        @Override
        public History map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            History history = new History();
            history.setId(rs.getInt("resource_history_id"));
            history.setUser(rs.getString("user_id"), rs.getString("username"));
            history.setServerVersion(rs.getString("server_version"));
            history.setCreated(rs.getString("document_created"));
            history.setKey(rs.getString("document_key"));
            history.setVersion(rs.getInt("document_version"));
            history.setChanges(rs.getString("document_changes"));
            return history;
        }
    }

    class HistoryDataMapper implements RowMapper<HistoryData> {
        @Override
        public HistoryData map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            File file = Learnweb.dao().getFileDao().findById(rs.getInt("file_id"));
            File prevFile = Learnweb.dao().getFileDao().findById(rs.getInt("prev_file_id"));
            File changeFile = Learnweb.dao().getFileDao().findById(rs.getInt("changes_file_id"));

            HistoryData prevData = new HistoryData();
            prevData.setUrl(prevFile.getAbsoluteUrl());
            prevData.setKey(FileUtility.generateRevisionId(prevFile));

            HistoryData historyData = new HistoryData();
            historyData.setPrevious(prevData);
            historyData.setUrl(file.getAbsoluteUrl());
            historyData.setChangesUrl(changeFile.getAbsoluteUrl());
            historyData.setKey(rs.getString("document_key"));
            historyData.setVersion(rs.getInt("document_version"));
            return historyData;
        }
    }
}

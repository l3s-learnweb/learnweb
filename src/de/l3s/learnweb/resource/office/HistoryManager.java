package de.l3s.learnweb.resource.office;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.office.history.model.History;
import de.l3s.learnweb.resource.office.history.model.HistoryData;
import de.l3s.learnweb.resource.office.history.model.HistoryInfo;

public class HistoryManager {
    private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final String HISTORY_COLUMNS = "resource_history_id, resource_id, user_id, file_id, prev_file_id, changes_file_id, server_version, document_created, document_key, document_version, document_changes";

    private final Learnweb learnweb;

    public HistoryManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public HistoryInfo getHistoryInfo(int resourceId) throws SQLException {
        List<History> histories = Learnweb.getInstance().getHistoryManager().getHistoryForHistoryInfo(resourceId);
        HistoryInfo info = new HistoryInfo();
        info.setHistory(histories);
        return info;
    }

    private List<History> getHistoryForHistoryInfo(int resourceId) throws SQLException {
        List<History> histories = new ArrayList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT resource_history_id, server_version, document_created, document_key, document_version, document_changes, u.user_id, u.username "
                + "FROM lw_resource_history h join lw_user u USING(user_id) WHERE resource_id = ? order by document_version")) {
            select.setInt(1, resourceId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                histories.add(createHistoryForHistoryInfo(rs));
            }
            rs.close();
        }
        return histories;
    }

    private History createHistoryForHistoryInfo(final ResultSet rs) throws SQLException {
        History history = new History();
        history.setUser(rs.getString("user_id"), rs.getString("username"));
        history.setId(rs.getInt("resource_history_id"));
        history.setServerVersion(rs.getString("server_version"));
        history.setCreated(rs.getString("document_created"));
        history.setKey(rs.getString("document_key"));
        history.setVersion(rs.getInt("document_version"));
        history.setChanges(rs.getString("document_changes"));
        return history;
    }

    public History saveHistory(History history) throws SQLException {
        if (history.getId() == null) { // the file is not yet stored at the database
            if (history.getCreated() == null) {
                history.setCreated(LocalDateTime.now().format(CREATED_FORMAT));
            }

            if (history.getVersion() == null) {
                int prevVersion = getLastVersionOfResource(history.getResourceId());
                history.setVersion(prevVersion + 1);
                if (prevVersion > 0) {
                    updateHistoryFileId(history.getResourceId(), prevVersion, history.getPrevFileId());
                }
            }

            try (PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_history` (" + HISTORY_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setNull(1, java.sql.Types.INTEGER);
                insert.setInt(2, history.getResourceId());
                insert.setInt(3, history.getUser().get("id").getAsInt());
                insert.setInt(4, history.getFileId());
                insert.setInt(5, history.getPrevFileId());
                insert.setInt(6, history.getChangesFileId());
                insert.setString(7, history.getServerVersion());
                insert.setString(8, history.getCreated());
                insert.setString(9, history.getKey());
                insert.setInt(10, history.getVersion());
                insert.setString(11, history.getChanges() != null ? history.getChanges().toString() : null);
                insert.executeUpdate();

                // get the assigned id
                ResultSet rs = insert.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                history.setId(rs.getInt(1));
            }
            return history;
        } else {
            throw new NotImplementedException("UPDATE is not implemented!");
        }
    }

    private int getLastVersionOfResource(final int resourceId) throws SQLException {
        int version = 0;
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT document_version FROM lw_resource_history WHERE resource_id = ? ORDER BY document_version LIMIT 1")) {
            select.setInt(1, resourceId);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                version = rs.getInt("document_version");
            }
            rs.close();
        }
        return version;
    }

    private void updateHistoryFileId(final int resourceId, final int version, final int prevFileId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("UPDATE lw_resource_history SET file_id = ? WHERE resource_id = ? AND document_version = ?")) {
            select.setInt(1, prevFileId);
            select.setInt(2, resourceId);
            select.setInt(3, version);
            ResultSet rs = select.executeQuery();
            rs.close();
        }
    }

    public HistoryData getHistoryData(final int resourceId, final int version) throws SQLException {
        HistoryData historyData = new HistoryData();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("select resource_history_id, file_id, prev_file_id, changes_file_id, document_key, document_version from lw_resource_history where resource_id = ? and document_version = ?")) {
            select.setInt(1, resourceId);
            select.setInt(2, version);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                return createHistoryData(rs);
            }
        }
        return historyData;
    }

    private HistoryData createHistoryData(final ResultSet rs) throws SQLException {
        File file = Learnweb.getInstance().getFileManager().getFileById(rs.getInt("file_id"));
        File prevFile = Learnweb.getInstance().getFileManager().getFileById(rs.getInt("prev_file_id"));
        File changeFile = Learnweb.getInstance().getFileManager().getFileById(rs.getInt("changes_file_id"));

        HistoryData prevData = new HistoryData();
        prevData.setUrl(prevFile.getUrl());
        prevData.setKey(FileUtility.generateRevisionId(prevFile));

        HistoryData historyData = new HistoryData();
        historyData.setPrevious(prevData);
        historyData.setUrl(file.getUrl());
        historyData.setChangesUrl(changeFile.getUrl());
        historyData.setKey(rs.getString("document_key"));
        historyData.setVersion(rs.getInt("document_version"));
        return historyData;
    }
}

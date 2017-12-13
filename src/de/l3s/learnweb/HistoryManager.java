package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.l3s.office.FileUtility;
import de.l3s.office.history.model.Change;
import de.l3s.office.history.model.FileData;
import de.l3s.office.history.model.History;
import de.l3s.office.history.model.HistoryData;
import de.l3s.office.history.model.HistoryInfo;
import de.l3s.office.history.model.OfficeUser;

public class HistoryManager
{
    private final Learnweb learnweb;
    private final static String HISTORY_COLUMNS = "history_id, resource_id, prev_ver_file_id, changes_id, last_save_date, doc_key, server_version, user_id";
    private final static String CHANGE_COLUMNS = "history_change_id, user_id, resource_history_id, created_date";

    public HistoryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public HistoryInfo getHistoryInfo(Integer resourceId) throws SQLException
    {
        List<History> histories = Learnweb.getInstance().getHistoryManager().getHistoryForResource(resourceId);
        HistoryInfo info = new HistoryInfo();
        info.setHistory(histories);
        return info;
    }

    public List<History> getHistoryForResource(Integer resourceId) throws SQLException
    {
        List<History> histories = new ArrayList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT history_id, last_save_date, doc_key, server_version, u.user_id as userId, u.username as username, @curRank := @curRank + 1 as history_version FROM lw_resource_history h left join lw_user u on u.user_id = h.user_id, (SELECT @curRank := 0) r WHERE resource_id = "
                        + resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
            histories.add(createHistory(rs));
        select.close();
        return histories;
    }

    private History createHistory(ResultSet rs) throws SQLException
    {
        History history = new History();
        history.setId(rs.getInt("history_id"));
        history.setCreated(rs.getString("last_save_date"));
        history.setKey(rs.getString("doc_key"));
        history.setServerVersion(rs.getString("server_version"));
        history.setUser(new OfficeUser());
        history.getUser().setId(rs.getInt("userId"));
        history.getUser().setName(rs.getString("username"));
        history.setChanges(new ArrayList<>());
        history.setVersion(rs.getInt("history_version"));
        PreparedStatement selectChange = learnweb.getConnection().prepareStatement("SELECT created_date, u.user_id 'userid', u.username 'username' FROM lw_user u inner join lw_history_change h where u.user_id = h.user_id and resource_history_id = " + history.getId());
        ResultSet resultSet = selectChange.executeQuery();
        while(resultSet.next())
            history.getChanges().add(createChange(resultSet));
        selectChange.close();

        return history;

    }

    private Change createChange(ResultSet rs) throws SQLException
    {
        Change change = new Change();
        OfficeUser user = new OfficeUser();
        user.setId(rs.getInt("userid"));
        change.setId(null);
        user.setName(rs.getString("username"));
        change.setUser(user);
        change.setCreated(rs.getString("created_date"));
        return change;
    }

    public History saveHistory(History history) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_resource_history` (" + HISTORY_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        if(history.getId() == null) // the file is not yet stored at the database                      
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, history.getId());
        replace.setInt(2, history.getResourceId());
        replace.setInt(3, history.getPreviousVersionFileId());
        replace.setInt(4, history.getChangesFileId());
        replace.setString(5, history.getCreated());
        replace.setString(6, history.getKey());
        replace.setString(7, history.getServerVersion());
        replace.setInt(8, history.getUser().getId());
        replace.executeUpdate();
        if(history.getId() == null) // it's a new file -> get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            history.setId(rs.getInt(1));
        }
        replace.close();

        return history;
    }

    public Change saveChange(Change change) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_history_change` (" + CHANGE_COLUMNS + ") VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

        if(change.getId() == null) // the file is not yet stored at the database                      
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, change.getId());
        replace.setInt(2, change.getUser().getId());
        replace.setInt(3, change.getHistoryId());
        replace.setString(4, change.getCreated());

        replace.executeUpdate();

        if(change.getId() == null) // it's a new file -> get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            change.setId(rs.getInt(1));
        }
        replace.close();

        return change;

    }

    public void addChangeToHistory(History history, Change change) throws SQLException
    {
        PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_history_change` SET `resource_history_id` = ? WHERE history_change_id = ?");
        update.setInt(1, history.getId());
        update.setInt(2, change.getId());
        update.executeUpdate();
        update.close();
    }

    public HistoryData getHistoryData(Integer id) throws SQLException
    {
        HistoryData historyData = new HistoryData();
        PreparedStatement select = learnweb.getConnection().prepareStatement(
                "select f.file_id as prev_id , f.name as prev_name, f.timestamp as prev_timestamp, cf.file_id as change_id, cf.name as change_name, cf.timestamp as change_time from  lw_resource_history h  inner join lw_file f on f.file_id = h.prev_ver_file_id inner join lw_file cf on cf.file_id = h.changes_id  where history_id = "
                        + id);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            File prevFile = new File();
            prevFile.setId(rs.getInt("prev_id"));
            prevFile.setName(rs.getString("prev_name"));
            prevFile.setLastModified(rs.getDate("prev_timestamp"));
            File changeFile = new File();
            changeFile.setId(rs.getInt("change_id"));
            changeFile.setName(rs.getString("change_name"));
            changeFile.setLastModified(rs.getDate("change_time"));
            changeFile.setUrl(Learnweb.getInstance().getFileManager().createUrl(changeFile.getId(), changeFile.getName()));
            prevFile.setUrl(Learnweb.getInstance().getFileManager().createUrl(prevFile.getId(), prevFile.getName()));
            FileData prevData = new FileData();
            prevData.setUrl(prevFile.getUrl());
            prevData.setKey(FileUtility.generateRevisionId(prevFile));
            historyData.setPrevious(prevData);
            historyData.setChangesUrl(changeFile.getUrl());
            historyData.setKey(FileUtility.generateRevisionId(changeFile));
        }
        return historyData;
    }

}

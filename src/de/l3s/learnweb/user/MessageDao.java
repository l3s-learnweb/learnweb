package de.l3s.learnweb.user;

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

import de.l3s.util.RsHelper;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(MessageDao.MessageMapper.class)
public interface MessageDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM message g WHERE message_id = ?")
    Message findById(int messageId);

    @SqlQuery("SELECT * FROM message WHERE from_user = ? ORDER BY m_time DESC")
    List<Message> findOutgoing(User user);

    @SqlQuery("SELECT * FROM message WHERE to_user = ? ORDER BY m_time DESC")
    List<Message> findIncoming(User user);

    @SqlQuery("SELECT * FROM message WHERE to_user = ? ORDER BY m_time DESC LIMIT ?")
    List<Message> findIncoming(User user, int limit);

    @SqlUpdate("UPDATE message SET m_seen = 1 WHERE message_id = ?")
    void updateMarkSeen(Message message);

    @SqlUpdate("UPDATE message SET m_read = 1 WHERE message_id = ?")
    void updateMarkRead(Message message);

    @SqlUpdate("UPDATE message SET m_seen = 1 WHERE to_user = ?")
    void updateMarkSeenAll(User user);

    @SqlUpdate("UPDATE message SET m_read = 1 WHERE to_user = ?")
    void updateMarkReadAll(User user);

    @SqlQuery("SELECT count(*) FROM message WHERE to_user = ? AND m_seen = 0")
    int countNotSeen(User user);

    default void save(Message message) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("message_id", message.getId() < 1 ? null : message.getId());
        params.put("from_user", message.getFromUserId());
        params.put("to_user", message.getToUserId());
        params.put("m_title", message.getTitle());
        params.put("m_text", message.getText());
        params.put("m_seen", message.isSeen());
        params.put("m_read", message.isRead());
        params.put("m_time", message.getTime());

        Optional<Integer> messageId = SqlHelper.handleSave(getHandle(), "message", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        messageId.ifPresent(message::setId);
    }

    class MessageMapper implements RowMapper<Message> {
        @Override
        public Message map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Message message = new Message();
            message.setId(rs.getInt("message_id"));
            message.setFromUserId(rs.getInt("from_user"));
            message.setToUserId(rs.getInt("to_user"));
            message.setTitle(rs.getString("m_title"));
            message.setText(rs.getString("m_text"));
            message.setSeen(rs.getBoolean("m_seen"));
            message.setRead(rs.getBoolean("m_read"));
            message.setTime(RsHelper.getLocalDateTime(rs.getTimestamp("m_time")));
            return message;
        }
    }
}

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

import de.l3s.util.SqlHelper;

@RegisterRowMapper(MessageDao.MessageMapper.class)
public interface MessageDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_message g WHERE message_id = ?")
    Optional<Message> findById(int messageId);

    @SqlQuery("SELECT * FROM lw_message WHERE sender_user_id = ? ORDER BY created_at DESC")
    List<Message> findOutgoing(User user);

    @SqlQuery("SELECT * FROM lw_message WHERE recipient_user_id = ? ORDER BY created_at DESC")
    List<Message> findIncoming(User user);

    @SqlQuery("SELECT * FROM lw_message WHERE recipient_user_id = ? ORDER BY created_at DESC LIMIT ?")
    List<Message> findIncoming(User user, int limit);

    @SqlUpdate("UPDATE lw_message SET seen = 1 WHERE message_id = ?")
    void updateMarkSeen(Message message);

    @SqlUpdate("UPDATE lw_message SET seen = 1 WHERE recipient_user_id = ?")
    void updateMarkSeenAll(User user);

    @SqlQuery("SELECT count(*) FROM lw_message WHERE recipient_user_id = ? AND seen = 0")
    int countNotSeen(User user);

    default void save(Message message) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("message_id", SqlHelper.toNullable(message.getId()));
        params.put("sender_user_id", message.getSenderUserId());
        params.put("recipient_user_id", message.getRecipientUserId());
        params.put("title", message.getTitle());
        params.put("text", message.getText());
        params.put("seen", message.isSeen());
        params.put("created_at", message.getCreatedAt());

        Optional<Integer> messageId = SqlHelper.handleSave(getHandle(), "lw_message", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        messageId.ifPresent(message::setId);
    }

    class MessageMapper implements RowMapper<Message> {
        @Override
        public Message map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Message message = new Message();
            message.setId(rs.getInt("message_id"));
            message.setSenderUserId(rs.getInt("sender_user_id"));
            message.setRecipientUserId(rs.getInt("recipient_user_id"));
            message.setTitle(rs.getString("title"));
            message.setText(rs.getString("text"));
            message.setSeen(rs.getBoolean("seen"));
            message.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return message;
        }
    }
}

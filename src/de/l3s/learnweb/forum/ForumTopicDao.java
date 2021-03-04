package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.user.User;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(ForumTopicDao.ForumTopicMapper.class)
public interface ForumTopicDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_forum_topic WHERE topic_id = ?")
    Optional<ForumTopic> findById(int topicId);

    @SqlQuery("SELECT * FROM lw_forum_topic WHERE group_id = ? ORDER BY updated_at DESC")
    List<ForumTopic> findByGroupId(int groupId);

    default Map<Integer, List<ForumTopic>> findByNotificationFrequencies(List<User.NotificationFrequency> notificationFrequencies) {
        return getHandle().createQuery("SELECT gu.user_id as notification_user_id, ft.* "
            + "FROM lw_group_user gu JOIN lw_forum_topic ft USING(group_id) LEFT JOIN lw_forum_topic_user ftu ON gu.user_id = ftu.user_id AND ft.topic_id = ftu.topic_id "
            + "WHERE notification_frequency IN (<frequencies>) AND (ftu.last_visit IS NULL OR ftu.last_visit < updated_at) AND ft.updated_at "
            + "BETWEEN DATE_SUB(NOW(), INTERVAL CASE WHEN notification_frequency = 'MONTHLY' THEN 30 WHEN notification_frequency = 'WEEKLY' THEN 7 ELSE 1 END day) AND NOW() "
            + "ORDER BY notification_user_id").bindList("frequencies", notificationFrequencies)
            .reduceRows(new HashMap<>(), (results, rowView) -> {
                int userId = rowView.getColumn("notification_user_id", Integer.class);
                ForumTopic forumTopic = rowView.getRow(ForumTopic.class);

                results.computeIfAbsent(userId, id -> new ArrayList<>()).add(forumTopic);
                return results;
            });
    }

    @SqlUpdate("UPDATE lw_forum_topic SET views = views + 1 WHERE topic_id = ?")
    void updateIncreaseViews(int topicId);

    @SqlUpdate("UPDATE lw_forum_topic SET replies = replies + 1, last_post_id = :postId, updated_at = :time, last_post_user_id = :userId WHERE topic_id = :topicId AND views > 0")
    void updateIncreaseReplies(@Bind("topicId") int topicId, @Bind("postId") int postId, @Bind("userId") int userId, @Bind("time") LocalDateTime date);

    @SqlUpdate("DELETE FROM lw_forum_topic WHERE topic_id = ?")
    void delete(int topicId);

    @SqlUpdate("INSERT INTO lw_forum_topic_user (topic_id, user_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_visit = NOW()")
    void insertUserVisit(int topicId, int userId);

    default void save(ForumTopic topic) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("topic_id", SqlHelper.toNullable(topic.getId()));
        params.put("group_id", topic.getGroupId());
        params.put("deleted", topic.isDeleted());
        params.put("title", topic.getTitle());
        params.put("user_id", SqlHelper.toNullable(topic.getUserId()));
        params.put("views", topic.getViews());
        params.put("replies", topic.getReplies());
        params.put("last_post_id", SqlHelper.toNullable(topic.getLastPostId()));
        params.put("last_post_user_id", SqlHelper.toNullable(topic.getLastPostUserId()));
        params.put("updated_at", topic.getLastPostDate());
        params.put("created_at", topic.getDate());

        Optional<Integer> topicId = SqlHelper.handleSave(getHandle(), "lw_forum_topic", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        topicId.ifPresent(topic::setId);
    }

    class ForumTopicMapper implements RowMapper<ForumTopic> {
        @Override
        public ForumTopic map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            ForumTopic topic = new ForumTopic();
            topic.setId(rs.getInt("topic_id"));
            topic.setUserId(rs.getInt("user_id"));
            topic.setGroupId(rs.getInt("group_id"));
            topic.setTitle(rs.getString("title"));
            topic.setViews(rs.getInt("views"));
            topic.setReplies(rs.getInt("replies"));
            topic.setLastPostId(rs.getInt("last_post_id"));
            topic.setLastPostUserId(rs.getInt("last_post_user_id"));
            topic.setLastPostDate(SqlHelper.getLocalDateTime(rs.getTimestamp("updated_at")));
            topic.setDate(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return topic;
        }
    }
}

package de.l3s.learnweb.forum;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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
import de.l3s.util.RsHelper;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(ForumTopicDao.ForumTopicMapper.class)
public interface ForumTopicDao extends SqlObject {

    @SqlQuery("SELECT * FROM lw_forum_topic WHERE topic_id = ?")
    Optional<ForumTopic> findById(int topicId);

    @SqlQuery("SELECT * FROM lw_forum_topic WHERE group_id = ? ORDER BY topic_last_post_time DESC")
    List<ForumTopic> findByGroupId(int groupId);

    default Map<Integer, List<ForumTopic>> findByNotificationFrequencies(List<User.NotificationFrequency> notificationFrequencies) {
        return getHandle().createQuery("SELECT gu.user_id as notification_user_id, ft.* "
            + "FROM lw_group_user gu JOIN lw_forum_topic ft USING(group_id) LEFT JOIN lw_forum_topic_user ftu ON gu.user_id = ftu.user_id AND ft.topic_id = ftu.topic_id "
            + "WHERE notification_frequency IN (<frequencies>) AND (ftu.last_visit IS NULL OR ftu.last_visit < topic_last_post_time) "
            + "AND ft.topic_last_post_time BETWEEN DATE_SUB(NOW(), INTERVAL CASE WHEN notification_frequency = 'MONTHLY' THEN 30 WHEN notification_frequency = 'WEEKLY' THEN 7 ELSE 1 END day) AND NOW() "
            + "ORDER BY notification_user_id").bindList("frequencies", notificationFrequencies)
            .reduceRows(new HashMap<>(), (results, rowView) -> {
                int userId = rowView.getColumn("notification_user_id", Integer.class);
                ForumTopic forumTopic = rowView.getRow(ForumTopic.class);

                results.computeIfAbsent(userId, id -> new ArrayList<>()).add(forumTopic);
                return results;
            });
    }

    @SqlUpdate("UPDATE lw_forum_topic SET topic_views = topic_views + 1 WHERE topic_id = ?")
    void updateIncreaseViews(int topicId);

    @SqlUpdate("UPDATE lw_forum_topic SET topic_replies = topic_replies + 1, topic_last_post_id = :postId, topic_last_post_time = :time, topic_last_post_user_id = :userId WHERE topic_id = :topicId AND topic_views > 0")
    void updateIncreaseReplies(@Bind("topicId") int topicId, @Bind("postId") int postId, @Bind("userId") int userId, @Bind("time") Date date);

    @SqlUpdate("DELETE FROM lw_forum_topic WHERE topic_id = ?")
    void delete(int topicId);

    @SqlUpdate("INSERT INTO lw_forum_topic_user (topic_id, user_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_visit = NOW();")
    void insertUserVisit(int topicId, int userId);

    default void save(ForumTopic topic) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("topic_id", topic.getId() < 1 ? null : topic.getId());
        params.put("group_id", topic.getGroupId());
        params.put("deleted", topic.isDeleted());
        params.put("topic_title", topic.getTitle());
        params.put("user_id", topic.getUserId());
        params.put("topic_time", topic.getDate());
        params.put("topic_views", topic.getViews());
        params.put("topic_replies", topic.getReplies());
        params.put("topic_last_post_id", topic.getLastPostId());
        params.put("topic_last_post_time", topic.getLastPostDate());
        params.put("topic_last_post_user_id", topic.getLastPostUserId());

        Optional<Integer> topicId = SqlHelper.generateInsertQuery(getHandle(), "lw_forum_topic", params)
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
            topic.setTitle(rs.getString("topic_title"));
            topic.setDate(RsHelper.getDate(rs.getTimestamp("topic_time")));
            topic.setViews(rs.getInt("topic_views"));
            topic.setReplies(rs.getInt("topic_replies"));
            topic.setLastPostId(rs.getInt("topic_last_post_id"));
            topic.setLastPostDate(RsHelper.getDate(rs.getTimestamp("topic_last_post_time")));
            topic.setLastPostUserId(rs.getInt("topic_last_post_user_id"));
            return topic;
        }
    }
}

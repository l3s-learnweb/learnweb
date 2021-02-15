package de.l3s.learnweb.forum;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User.NotificationFrequency;
import de.l3s.util.SqlHelper;

public class ForumManager {
    //private static final Logger log = LogManager.getLogger(ForumManager.class);

    private final Learnweb learnweb;

    public ForumManager(Learnweb learnweb) throws SQLException {
        this.learnweb = learnweb;
    }

    /**
     * returns all topic of the defined group. Sorted by topic_last_post_time
     */
    public List<ForumTopic> getTopicsByGroup(int groupId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT * FROM `lw_forum_topic` WHERE group_id = ? ORDER BY topic_last_post_time DESC", groupId)
                .map(new ForumTopicMapper()).list();
        }
    }

    /**
     * @return number of posts per users of defined group
     */
    public Map<Integer, Integer> getPostCountPerUserByGroup(int groupId) throws SQLException {
        Map<Integer, Integer> postCounts = new HashMap<>();

        try (Handle handle = learnweb.openHandle()) {
            handle.select("SELECT p.user_id, COUNT(*) as count FROM lw_forum_post p JOIN lw_forum_topic t USING (topic_id) WHERE group_id = ? GROUP BY p.user_id", groupId)
                .map((rs, ctx) -> {
                    int userId = rs.getInt("user_id");
                    int postCount = rs.getInt("count");
                    postCounts.put(userId, postCount);
                    return null;
                });
        }

        return postCounts;
    }

    /**
     * @return null if not found
     */
    public ForumTopic getTopicById(int topicId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT * FROM `lw_forum_topic` WHERE topic_id = ?", topicId)
                .map(new ForumTopicMapper()).findOne().orElse(null);
        }
    }

    /**
     * Sorted by date DESC.
     */
    public List<ForumPost> getPostsBy(int topicId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT * FROM `lw_forum_post` WHERE topic_id = ? ORDER BY post_time", topicId)
                .map(new ForumPostMapper()).list();
        }
    }

    public ForumPost getPostById(int postId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT * FROM `lw_forum_post` WHERE post_id = ?", postId)
                .map(new ForumPostMapper()).findOne().orElse(null);
        }
    }

    public List<ForumPost> getPostsByUser(int userId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT * FROM `lw_forum_post` WHERE user_id = ? ORDER BY post_time DESC", userId)
                .map(new ForumPostMapper()).list();
        }
    }

    public int getPostCountByUser(int userId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT COUNT(*) FROM `lw_forum_post` WHERE user_id = ?", userId).mapTo(Integer.class).one();
        }
    }

    public ForumTopic save(ForumTopic topic) throws SQLException {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("topic_id", topic.getId() < 0 ? null : topic.getId());
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

        try (Handle handle = learnweb.openHandle()) {
            Optional<Integer> topicId = SqlHelper.generateInsertQuery(handle, "lw_forum_topic", params)
                .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

            if (topicId.isPresent()) {
                topic.setId(topicId.get());
            } else if (topic.getId() < 0) {
                throw new SQLException("database error: no id generated");
            }

            return topic;
        }
    }

    public ForumPost save(ForumPost post) throws SQLException {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("post_id", post.getId() < 0 ? null : post.getId());
        params.put("deleted", post.isDeleted());
        params.put("topic_id", post.getTopicId());
        params.put("user_id", post.getUserId());
        params.put("text,", post.getText());
        params.put("post_time", post.getDate());
        params.put("post_edit_time", post.getLastEditDate());
        params.put("post_edit_count", post.getEditCount());
        params.put("post_edit_user_id", post.getEditUserId());
        params.put("category", post.getCategory());

        try (Handle handle = learnweb.openHandle()) {
            Optional<Integer> postId = SqlHelper.generateInsertQuery(handle, "lw_forum_post", params)
                .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

            if (postId.isPresent()) {
                post.setId(postId.get());

                // updated view count and statistic of parent topic
                handle.execute("UPDATE lw_forum_topic SET topic_replies = topic_replies + 1, topic_last_post_id = ?, topic_last_post_time = ?, "
                        + "topic_last_post_user_id = ? WHERE topic_id = ? AND topic_views > 0",
                    post.getId(), post.getDate(), post.getUserId(), post.getTopicId());
                post.getUser().incForumPostCount();
            } else if (post.getId() < 0) {
                throw new SQLException("database error: no id generated");
            }

            return post;
        }
    }

    public void deleteTopic(ForumTopic topic) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("DELETE FROM `lw_forum_topic` WHERE topic_id = ?", topic.getId());
        }
    }

    /**
     * increment topic view counter.
     */
    public void incViews(int topicId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("UPDATE lw_forum_topic SET topic_views = topic_views+1 WHERE topic_id = ?", topicId);
        }
    }

    public void deletePost(ForumPost post) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("DELETE FROM lw_forum_post WHERE post_id = ?", post.getId());
        }
    }

    /**
     * @return list of topics, that were created in date interval
     */
    public List<ForumTopic> getTopicByPeriod(int userId, NotificationFrequency notificationFrequency) throws SQLException {
        if (notificationFrequency == NotificationFrequency.NEVER) {
            throw new IllegalArgumentException();
        }

        try (Handle handle = learnweb.openHandle()) {
            return handle.createQuery("SELECT ft.*, gu.notification_frequency, ftu.last_visit "
                + "FROM lw_forum_topic ft LEFT JOIN lw_group_user gu USING(group_id) LEFT JOIN lw_forum_topic_user ftu ON gu.user_id = ftu.user_id AND ft.topic_id = ftu.topic_id "
                + "WHERE ft.topic_last_post_time BETWEEN DATE_SUB(NOW(), INTERVAL ? DAY) AND NOW() AND g.user_id = ? AND g.notification_frequency = ? "
                + "AND (ftu.last_visit IS NULL OR ftu.last_visit < ft.topic_last_post_time) GROUP BY f.topic_id")
                .bind(0, notificationFrequency.getDays())
                .bind(1, userId)
                .bind(2, notificationFrequency.toString())
                .bind(3, notificationFrequency.getDays())
                .map(new ForumTopicMapper()).list();
        }
    }

    /**
     * Updates last_visit time when user open topic.
     */
    public void updatePostVisitTime(int topicId, int userId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("INSERT INTO `lw_forum_topic_user`(`topic_id`, `user_id`) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_visit = NOW();", topicId, userId);
        }
    }

    private static class ForumPostMapper implements RowMapper<ForumPost> {
        @Override
        public ForumPost map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            ForumPost post = new ForumPost();
            post.setId(rs.getInt("post_id"));
            post.setTopicId(rs.getInt("topic_id"));
            post.setUserId(rs.getInt("user_id"));
            post.setText(rs.getString("text"));
            post.setDate(new Date(rs.getTimestamp("post_time").getTime()));
            post.setLastEditDate(new Date(rs.getTimestamp("post_edit_time").getTime()));
            post.setEditCount(rs.getInt("post_edit_count"));
            post.setEditUserId(rs.getInt("post_edit_user_id"));
            post.setCategory(rs.getString("category"));
            return post;
        }
    }

    private static class ForumTopicMapper implements RowMapper<ForumTopic> {
        @Override
        public ForumTopic map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            ForumTopic topic = new ForumTopic();
            topic.setId(rs.getInt("topic_id"));
            topic.setUserId(rs.getInt("user_id"));
            topic.setGroupId(rs.getInt("group_id"));
            topic.setTitle(rs.getString("topic_title"));
            topic.setDate(new Date(rs.getTimestamp("topic_time").getTime()));
            topic.setViews(rs.getInt("topic_views"));
            topic.setReplies(rs.getInt("topic_replies"));
            topic.setLastPostId(rs.getInt("topic_last_post_id"));
            topic.setLastPostDate(new Date(rs.getTimestamp("topic_last_post_time").getTime()));
            topic.setLastPostUserId(rs.getInt("topic_last_post_user_id"));
            return topic;
        }
    }

    /**
     * @return list of topics, that users should be notified about
     */
    public Map<Integer, List<ForumTopic>> getTopicsByNotificationFrequencies(List<NotificationFrequency> notificationFrequencies) throws SQLException {
        StringBuilder frequencies = new StringBuilder();
        for (NotificationFrequency frequency : notificationFrequencies) {
            frequencies.append("'").append(frequency).append("'").append(",");
        }
        frequencies.setLength(frequencies.length() - 1);

        Map<Integer, List<ForumTopic>> topics = new HashMap<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT gu.user_id as notification_user_id, ft.* "
                + "FROM lw_group_user gu JOIN lw_forum_topic ft USING(group_id) LEFT JOIN lw_forum_topic_user ftu ON gu.user_id = ftu.user_id AND ft.topic_id = ftu.topic_id "
                + "WHERE notification_frequency IN (" + frequencies + ") AND (ftu.last_visit IS NULL OR ftu.last_visit < topic_last_post_time) "
                + "AND ft.topic_last_post_time BETWEEN DATE_SUB(NOW(), INTERVAL CASE WHEN notification_frequency = 'MONTHLY' THEN 30 WHEN notification_frequency = 'WEEKLY' THEN 7 ELSE 1 END day) AND NOW() "
                + "ORDER BY notification_user_id")) {
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("notification_user_id");
                ForumTopic forumTopic = createTopic(rs);
                List<ForumTopic> usersTopics = topics.computeIfAbsent(userId, id -> new ArrayList<>());
                usersTopics.add(forumTopic);
            }
        }
        return topics;
    }

}

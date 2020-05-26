package de.l3s.learnweb.forum;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User.NotificationFrequency;
import de.l3s.util.database.IColumn;

public class ForumManager {
    private static final Logger log = LogManager.getLogger(ForumManager.class);

    enum POST_COLUMNS implements IColumn {
        POST_ID,
        DELETED,
        TOPIC_ID,
        USER_ID,
        TEXT,
        POST_TIME,
        POST_EDIT_TIME,
        POST_EDIT_COUNT,
        POST_EDIT_USER_ID,
        CATEGORY
    };

    enum TOPIC_COLUMNS implements IColumn {
        TOPIC_ID,
        GROUP_ID,
        DELETED,
        TOPIC_TITLE,
        USER_ID,
        TOPIC_TIME,
        TOPIC_VIEWS,
        TOPIC_REPLIES,
        TOPIC_LAST_POST_ID,
        TOPIC_LAST_POST_TIME,
        TOPIC_LAST_POST_USER_ID
    };

    private static final String POST_COLUMNS = "post_id, topic_id, user_id, text, post_time, post_edit_time, post_edit_count, post_edit_user_id, category";
    private static final String TOPIC_COLUMNS = "topic_id, group_id, topic_title, user_id, topic_time, topic_views, topic_replies, topic_last_post_id, topic_last_post_time, topic_last_post_user_id";

    private final Learnweb learnweb;

    public ForumManager(Learnweb learnweb) throws SQLException {
        this.learnweb = learnweb;
    }

    /**
     * returns all topic of the defined group. Sorted by topic_last_post_time
     */
    public List<ForumTopic> getTopicsByGroup(int groupId) throws SQLException {
        LinkedList<ForumTopic> topics = new LinkedList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + TOPIC_COLUMNS + " FROM `lw_forum_topic` WHERE group_id = ? ORDER BY topic_last_post_time DESC")) {
            select.setInt(1, groupId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                topics.add(createTopic(rs));
            }
        }
        return topics;
    }

    /**
     * @return null if not found
     */
    public ForumTopic getTopicById(int topicId) throws SQLException {
        ForumTopic topic = null;

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + TOPIC_COLUMNS + " FROM `lw_forum_topic` WHERE topic_id = ?")) {
            select.setInt(1, topicId);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                topic = createTopic(rs);
            }
        }
        return topic;
    }

    /**
     * Sorted by date DESC.
     */
    public List<ForumPost> getPostsBy(int topicId) throws SQLException {
        LinkedList<ForumPost> posts = new LinkedList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `lw_forum_post` WHERE topic_id = ? ORDER BY post_time")) {
            select.setInt(1, topicId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                posts.add(createPost(rs));
            }
        }

        return posts;
    }

    public ForumPost getPostById(int postId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `lw_forum_post` WHERE post_id = ?")) {
            select.setInt(1, postId);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                return createPost(rs);
            }
        }
        return null;
    }

    public List<ForumPost> getPostsByUser(int userId) throws SQLException {
        LinkedList<ForumPost> posts = new LinkedList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `lw_forum_post` WHERE user_id = ? ORDER BY post_time DESC")) {
            select.setInt(1, userId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                posts.add(createPost(rs));
            }
        }

        return posts;
    }

    public int getPostCountByUser(int userId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT COUNT(*) FROM `lw_forum_post` WHERE user_id = ?")) {
            select.setInt(1, userId);
            ResultSet rs = select.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    public ForumTopic save(ForumTopic topic) throws SQLException {
        String sqlQuery = "REPLACE INTO `lw_forum_topic` (" + TOPIC_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS)) {
            if (topic.getId() < 0) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, topic.getId());
            }
            ps.setInt(2, topic.getGroupId());
            ps.setString(3, topic.getTitle());
            ps.setInt(4, topic.getUserId());
            ps.setTimestamp(5, new java.sql.Timestamp(topic.getDate().getTime()));
            ps.setInt(6, topic.getViews());
            ps.setInt(7, topic.getReplies());
            ps.setInt(8, topic.getLastPostId());
            ps.setTimestamp(9, new java.sql.Timestamp(topic.getLastPostDate().getTime()));
            ps.setInt(10, topic.getLastPostUserId());
            ps.executeUpdate();

            if (topic.getId() < 0) { // get the assigned id
                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                topic.setId(rs.getInt(1));
            }
        }
        return topic;
    }

    public ForumPost save(ForumPost post) throws SQLException {
        String sqlQuery = "REPLACE INTO `lw_forum_post` (" + POST_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS)) {
            if (post.getId() < 0) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, post.getId());
            }
            ps.setInt(2, post.getTopicId());
            ps.setInt(3, post.getUserId());
            ps.setString(4, post.getText());
            ps.setTimestamp(5, new java.sql.Timestamp(post.getDate().getTime()));
            ps.setTimestamp(6, new java.sql.Timestamp(post.getLastEditDate().getTime()));
            ps.setInt(7, post.getEditCount());
            ps.setInt(8, post.getEditUserId());
            ps.setString(9, post.getCategory());
            ps.executeUpdate();

            if (post.getId() < 0) { // get the assigned id
                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                post.setId(rs.getInt(1));

                // updated view count and statistic of parent topic
                sqlQuery = "UPDATE lw_forum_topic SET topic_replies = topic_replies + 1, topic_last_post_id = ?, topic_last_post_time = ?, topic_last_post_user_id = ? WHERE topic_id = ? AND topic_views > 0";
                try (PreparedStatement update = learnweb.getConnection().prepareStatement(sqlQuery)) {
                    update.setInt(1, post.getId());
                    update.setTimestamp(2, new Timestamp(post.getDate().getTime()));
                    update.setInt(3, post.getUserId());
                    update.setInt(4, post.getTopicId());
                    update.executeUpdate();
                }
                post.getUser().incForumPostCount();
            }
        }
        return post;
    }

    public void deleteTopic(ForumTopic topic) throws SQLException {
        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_forum_topic` WHERE topic_id = ?")) {
            delete.setInt(1, topic.getId());
            delete.executeUpdate();
        }
    }

    /**
     * increment topic view counter.
     */
    public void incViews(int topicId) throws SQLException {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("UPDATE lw_forum_topic SET topic_views = topic_views+1 WHERE topic_id = ?")) {
            ps.setInt(1, topicId);
            ps.executeUpdate();
        }
    }

    public ForumPost createPost(ResultSet rs) throws SQLException {
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

    public ForumTopic createTopic(ResultSet rs) throws SQLException {
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

    public void deletePost(ForumPost post) throws SQLException {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("DELETE FROM lw_forum_post WHERE post_id = ?")) {
            ps.setInt(1, post.getId());
            ps.executeUpdate();
        }
    }

    /**
     * @return list of topics, that were created in date interval
     */
    public List<ForumTopic> getTopicByPeriod(int userId, NotificationFrequency notificationFrequency) throws SQLException {
        int days = -1;
        switch (notificationFrequency) {
            case DAILY:
                days = 1;
                break;
            case WEEKLY:
                days = 7;
                break;
            case MONTHLY:
                days = 30;
                break;
            case NEVER:
                throw new IllegalArgumentException();
        }

        List<ForumTopic> topics = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT * FROM `lw_forum_topic` f JOIN `lw_group_user` g USING(`group_id`) LEFT JOIN `lw_forum_topic_user` ft ON g.`user_id` = ft.`user_id` "
                + "AND f.`topic_id` = ft.`topic_id` WHERE TIMESTAMPDIFF(DAY, topic_last_post_time, CURRENT_TIMESTAMP) < ? AND g.`user_id` = ? "
                + "AND g.`notification_frequency` = ? AND (TIMESTAMPDIFF(DAY, ft.`last_visit`, CURRENT_TIMESTAMP) IS NULL "
                + "OR TIMESTAMPDIFF(DAY, ft.`last_visit`, CURRENT_TIMESTAMP)>=?) GROUP BY f.`topic_id`")) {
            select.setInt(1, days);
            select.setInt(2, userId);
            select.setString(3, notificationFrequency.toString());
            select.setInt(4, days);
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                topics.add(createTopic(rs));
            }
        }
        return topics;
    }

    /**
     * @return update last_visit time when user open topic
     */
    public void updatePostVisitTime(int topicId, int userId) throws SQLException {
        try (PreparedStatement update = learnweb.getConnection().prepareStatement("INSERT INTO `lw_forum_topic_user`(`topic_id`, `user_id`) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_visit = NOW();")) {
            update.setInt(1, topicId);
            update.setInt(2, userId);
            update.executeUpdate();
        }
    }

}

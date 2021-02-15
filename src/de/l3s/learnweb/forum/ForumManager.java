package de.l3s.learnweb.forum;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User.NotificationFrequency;
import de.l3s.util.database.IColumn;
import de.l3s.util.database.Sql;

public class ForumManager {
    //private static final Logger log = LogManager.getLogger(ForumManager.class);

    enum PostColumns implements IColumn {
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
    }

    enum TopicColumns implements IColumn {
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
    }

    private static final String POST_COLUMNS = Sql.columns(PostColumns.values());
    private static final String TOPIC_COLUMNS = Sql.columns(TopicColumns.values());
    private static final String QUERY_POST_SAVE = Sql.getCreateStatement("lw_forum_post", PostColumns.values());
    private static final String QUERY_TOPIC_SAVE = Sql.getCreateStatement("lw_forum_topic", TopicColumns.values());

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
     * @return number of posts per users of defined group
     */
    public Map<Integer, Integer> getPostCountPerUserByGroup(int groupId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT p.user_id, COUNT(*) as post_count "
            + "FROM lw_forum_post p JOIN lw_forum_topic t USING (topic_id) WHERE group_id = ? GROUP BY p.user_id")) {
            select.setInt(1, groupId);

            try (ResultSet rs = select.executeQuery()) {
                Map<Integer, Integer> postCounts = new HashMap<>();
                while (rs.next()) {
                    int userId = rs.getInt("user_id");
                    int postCount = rs.getInt("post_count");
                    postCounts.put(userId, postCount);
                }
                return postCounts;
            }
        }
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
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement(QUERY_TOPIC_SAVE, Statement.RETURN_GENERATED_KEYS)) {
            if (topic.getId() < 0) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, topic.getId());
            }
            ps.setInt(2, topic.getGroupId());
            ps.setBoolean(3, topic.isDeleted());
            ps.setString(4, topic.getTitle());
            ps.setInt(5, topic.getUserId());
            ps.setTimestamp(6, new java.sql.Timestamp(topic.getDate().getTime()));
            ps.setInt(7, topic.getViews());
            ps.setInt(8, topic.getReplies());
            ps.setInt(9, topic.getLastPostId());
            ps.setTimestamp(10, new java.sql.Timestamp(topic.getLastPostDate().getTime()));
            ps.setInt(11, topic.getLastPostUserId());
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
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement(QUERY_POST_SAVE, Statement.RETURN_GENERATED_KEYS)) {
            if (post.getId() < 0) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, post.getId());
            }
            ps.setBoolean(2, post.isDeleted());
            ps.setInt(3, post.getTopicId());
            ps.setInt(4, post.getUserId());
            ps.setString(5, post.getText());
            ps.setTimestamp(6, new java.sql.Timestamp(post.getDate().getTime()));
            ps.setTimestamp(7, new java.sql.Timestamp(post.getLastEditDate().getTime()));
            ps.setInt(8, post.getEditCount());
            ps.setInt(9, post.getEditUserId());
            ps.setString(10, post.getCategory());
            ps.executeUpdate();

            if (post.getId() < 0) { // get the assigned id
                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                post.setId(rs.getInt(1));

                // updated view count and statistic of parent topic
                String sqlQuery = "UPDATE lw_forum_topic SET topic_replies = topic_replies + 1, topic_last_post_id = ?, topic_last_post_time = ?, topic_last_post_user_id = ? WHERE topic_id = ? AND topic_views > 0";
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
        topic.setId(rs.getInt(TopicColumns.TOPIC_ID.name()));
        topic.setUserId(rs.getInt(TopicColumns.USER_ID.name()));
        topic.setGroupId(rs.getInt(TopicColumns.GROUP_ID.name()));
        topic.setTitle(rs.getString(TopicColumns.TOPIC_TITLE.name()));
        topic.setDate(new Date(rs.getTimestamp(TopicColumns.TOPIC_TIME.name()).getTime()));
        topic.setViews(rs.getInt(TopicColumns.TOPIC_VIEWS.name()));
        topic.setReplies(rs.getInt(TopicColumns.TOPIC_REPLIES.name()));
        topic.setLastPostId(rs.getInt(TopicColumns.TOPIC_LAST_POST_ID.name()));
        topic.setLastPostDate(new Date(rs.getTimestamp(TopicColumns.TOPIC_LAST_POST_TIME.name()).getTime()));
        topic.setLastPostUserId(rs.getInt(TopicColumns.TOPIC_LAST_POST_USER_ID.name()));

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
        if (notificationFrequency.equals(NotificationFrequency.NEVER)) {
            throw new IllegalArgumentException();
        }

        List<ForumTopic> topics = new LinkedList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT ft.*, gu.notification_frequency, ftu.last_visit "
                + "FROM lw_forum_topic ft LEFT JOIN lw_group_user gu USING(group_id) LEFT JOIN lw_forum_topic_user ftu ON gu.user_id = ftu.user_id AND ft.topic_id = ftu.topic_id "
                + "WHERE ft.topic_last_post_time BETWEEN DATE_SUB(NOW(), INTERVAL ? DAY) AND NOW() AND g.user_id = ? AND g.notification_frequency = ? "
                + "AND (ftu.last_visit IS NULL OR ftu.last_visit < ft.topic_last_post_time) GROUP BY f.topic_id")) {
            select.setInt(1, notificationFrequency.getDays());
            select.setInt(2, userId);
            select.setString(3, notificationFrequency.toString());
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                topics.add(createTopic(rs));
            }
        }
        return topics;
    }

    /**
     * Updates last_visit time when user open topic.
     */
    public void updatePostVisitTime(int topicId, int userId) throws SQLException {
        try (PreparedStatement update = learnweb.getConnection().prepareStatement("INSERT INTO `lw_forum_topic_user`(`topic_id`, `user_id`) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_visit = NOW();")) {
            update.setInt(1, topicId);
            update.setInt(2, userId);
            update.executeUpdate();
        }
    }

}

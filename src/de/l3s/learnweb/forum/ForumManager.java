package de.l3s.learnweb.forum;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.l3s.learnweb.Learnweb;

public class ForumManager
{
    // private final static Logger log = Logger.getLogger(ForumManager.class);
    private final static String POST_COLUMNS = "post_id, topic_id, user_id, text, post_time, post_edit_time, post_edit_count, post_edit_user_id, category";
    private final static String TOPIC_COLUMNS = "topic_id, group_id, topic_title, user_id, topic_time, topic_views, topic_replies, topic_last_post_id, topic_last_post_time, topic_last_post_user_id";

    private final Learnweb learnweb;

    public ForumManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
    }

    /**
     * returns all topic of the define group. Sorted by ORDER
     *
     * @param groupId
     * @return
     */
    public List<ForumTopic> getTopicsByGroup(int groupId) throws SQLException
    {
        LinkedList<ForumTopic> topics = new LinkedList<>();
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + TOPIC_COLUMNS + " FROM `lw_forum_topic` WHERE group_id = ? ORDER BY topic_last_post_time DESC");)
        {
            select.setInt(1, groupId);
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                topics.add(createTopic(rs));
            }
        }
        return topics;
    }

    /**
     *
     * @param topicId
     * @return null if not found
     * @throws SQLException
     */
    public ForumTopic getTopicById(int topicId) throws SQLException
    {
        ForumTopic topic = null;
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + TOPIC_COLUMNS + " FROM `lw_forum_topic` WHERE topic_id = ?");)
        {
            select.setInt(1, topicId);
            ResultSet rs = select.executeQuery();
            if(rs.next())
            {
                topic = createTopic(rs);
            }
        }
        return topic;
    }

    /**
     * Sorted by date DESC
     *
     * @param topicId
     * @return
     */
    public List<ForumPost> getPostsBy(int topicId) throws SQLException
    {
        LinkedList<ForumPost> posts = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `lw_forum_post` WHERE topic_id = ? ORDER BY post_time");
        select.setInt(1, topicId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            posts.add(createPost(rs));
        }
        select.close();

        return posts;
    }

    public ForumPost getPostById(int postId) throws SQLException
    {
        ForumPost post = null;

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `lw_forum_post` WHERE post_id = ?");)
        {
            select.setInt(1, postId);
            ResultSet rs = select.executeQuery();
            if(rs.next())
            {
                post = createPost(rs);
            }
        }
        return post;
    }

    /**
     * Returns all posts that were created in the users group after the Date "lowerBound"
     *
     * @param userId
     * @return
     * @throws SQLException
     *             /
     *             public List<ForumPost> getNewPostsByUser(User user, Date lowerBound) throws SQLException
     *             {
     *             StringBuilder sb = new StringBuilder();
     *             for(Group group : user.getGroups())
     *             {
     *             sb.append(',');
     *             sb.append(Integer.toString(group.getId()));
     *             }
     *             String ids = sb.substring(1);
     *
     *             LinkedList<ForumPost> posts = new LinkedList<ForumPost>();
     *
     *
     *
     *             PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS +
     *             " FROM `lw_forum_post` WHERE topic_id = ? ORDER BY post_time");
     *             select.setInt(1, topicId);
     *             ResultSet rs = select.executeQuery();
     *             while(rs.next())
     *             {
     *             posts.add(createPost(rs));
     *             }
     *             select.close();
     *
     *             return posts;
     *             }
     */

    public List<ForumPost> getPostsByUser(int userId) throws SQLException
    {
        LinkedList<ForumPost> posts = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `lw_forum_post` WHERE user_id = ? ORDER BY post_time DESC");
        select.setInt(1, userId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            posts.add(createPost(rs));
        }
        select.close();

        return posts;
    }

    public int getPostCountByUser(int userId) throws SQLException
    {
        int posts = 0;

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT COUNT(*) FROM `lw_forum_post` WHERE user_id = ?");
        select.setInt(1, userId);
        ResultSet rs = select.executeQuery();

        if(rs.next())
            posts = rs.getInt(1);

        select.close();

        return posts;
    }

    public ForumTopic save(ForumTopic topic) throws SQLException
    {
        String sqlQuery = "REPLACE INTO `lw_forum_topic` (" + TOPIC_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
        if(topic.getId() < 0)
            ps.setNull(1, java.sql.Types.INTEGER);
        else
            ps.setInt(1, topic.getId());
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

        if(topic.getId() < 0) // get the assigned id
        {
            ResultSet rs = ps.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            topic.setId(rs.getInt(1));
        }

        return topic;
    }

    public ForumPost save(ForumPost post) throws SQLException
    {
        String sqlQuery = "REPLACE INTO `lw_forum_post` (" + POST_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);

        if(post.getId() < 0)
            ps.setNull(1, java.sql.Types.INTEGER);
        else
            ps.setInt(1, post.getId());
        ps.setInt(2, post.getTopicId());
        ps.setInt(3, post.getUserId());
        ps.setString(4, post.getText());
        ps.setTimestamp(5, new java.sql.Timestamp(post.getDate().getTime()));
        ps.setTimestamp(6, new java.sql.Timestamp(post.getLastEditDate().getTime()));
        ps.setInt(7, post.getEditCount());
        ps.setInt(8, post.getEditUserId());
        ps.setString(9, post.getCategory());
        ps.executeUpdate();

        if(post.getId() < 0) // get the assigned id
        {
            ResultSet rs = ps.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            post.setId(rs.getInt(1));

            sqlQuery = "UPDATE lw_forum_topic SET topic_replies = topic_replies + 1, topic_last_post_id = ?, topic_last_post_time = ?, topic_last_post_user_id = ? WHERE topic_id = ? AND topic_views > 0";
            PreparedStatement update = learnweb.getConnection().prepareStatement(sqlQuery);
            update.setInt(1, post.getId());
            update.setTimestamp(2, new Timestamp(post.getDate().getTime()));
            update.setInt(3, post.getUserId());
            update.setInt(4, post.getTopicId());
            update.executeUpdate();

            post.getUser().incForumPostCount();
        }

        return post;
    }

    /**
     * increment topic view counter
     *
     * @param topicId
     * @throws SQLException
     *
     */

    public void incViews(int topicId) throws SQLException
    {
        String sqlQuery = "UPDATE lw_forum_topic SET topic_views = topic_views +1 WHERE topic_id = ?";
        PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
        ps.setInt(1, topicId);
        ps.executeUpdate();
    }

    public ForumPost createPost(ResultSet rs) throws SQLException
    {
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

    public ForumTopic createTopic(ResultSet rs) throws SQLException
    {
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

    public void deletePost(ForumPost post) throws SQLException
    {

        String sqlQuery = "DELETE FROM lw_forum_post WHERE post_id=? ";
        PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
        ps.setInt(1, post.getId());

        ps.executeUpdate();
    }

}

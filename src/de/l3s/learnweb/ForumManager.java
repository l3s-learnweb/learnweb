package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class ForumManager
{
    enum ORDER // possible order values; Not all values are applicable for every method
    {
	DATE,
	TITLE,
	REPLIES,
	VIEWS,
	TOPIC
    }

    private final static Logger log = Logger.getLogger(ForumManager.class);
    private final static String POST_COLUMNS = "post_id, topic_id,group_id, user_id, text, post_time, post_edit_time, post_edit_user_id";
    private final static String TOPIC_COLUMNS = "topic_id, group_id, topic_title, user_id, topic_time, topic_views, topic_replies, topic_last_post_id, topic_last_post_time";

    private final Learnweb learnweb;

    protected ForumManager(Learnweb learnweb) throws SQLException
    {
	this.learnweb = learnweb;
    }

    /**
     * returns all topic of the define group. Sorted by ORDER
     * 
     * @param groupId
     * @param order
     * @return
     */
    public List<ForumTopic> getTopicsByGroup(int groupId) throws SQLException
    {
	LinkedList<ForumTopic> topics = new LinkedList<ForumTopic>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + TOPIC_COLUMNS + " FROM `forum_topic` WHERE group_id = ? ORDER BY topic_time DESC");
	select.setInt(1, groupId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    topics.add(createTopic(rs));
	}
	select.close();
	return topics;
    }

    /**
     * Sorted by date DESC
     * 
     * @param topicId
     * @return
     */
    public List<ForumPost> getPostsBy(int topicId) throws SQLException
    {
	LinkedList<ForumPost> posts = new LinkedList<ForumPost>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `forum_post` WHERE topic_id = ? ORDER BY topic_time DESC");
	select.setInt(1, topicId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    posts.add(createPost(rs));
	}
	select.close();

	return posts;
    }

    public List<ForumPost> getPostsByUser(int userId, ORDER order) throws SQLException
    {
	LinkedList<ForumPost> posts = new LinkedList<ForumPost>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + POST_COLUMNS + " FROM `forum_post` WHERE user_id = ? ORDER BY topic_time DESC");
	select.setInt(1, userId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    posts.add(createPost(rs));
	}
	select.close();

	return posts;
    }

    public ForumTopic save(ForumTopic forum) throws SQLException
    {
	String sqlQuery = "REPLACE INTO `forum_topic` (" + TOPIC_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?)";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	if(forum.getId() < 0)
	    ps.setNull(1, java.sql.Types.INTEGER);
	else
	    ps.setInt(1, forum.getId());
	ps.setInt(2, forum.getGroupId());
	ps.setString(3, forum.getTitle());
	ps.setInt(4, forum.getUserId());
	ps.setTimestamp(5, new java.sql.Timestamp(forum.getDate().getTime()));
	ps.setInt(6, forum.getViews());
	ps.setInt(7, forum.getReplies());
	ps.setInt(8, forum.getLastPostId());
	ps.setTimestamp(9, new java.sql.Timestamp(forum.getLastPostDate().getTime()));
	ps.executeUpdate();

	if(forum.getId() < 0) // get the assigned id
	{
	    ResultSet rs = ps.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    forum.setId(rs.getInt(1));
	}

	return forum;
    }

    public ForumPost save(ForumPost forumPost) throws SQLException
    {
	String sqlQuery = "REPLACE INTO `forum_post` (" + POST_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?)";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);

	if(forumPost.getId() < 0)
	    ps.setNull(1, java.sql.Types.INTEGER);
	else
	    ps.setInt(1, forumPost.getId());
	ps.setInt(2, forumPost.getId());
	ps.setInt(3, forumPost.getGroupId());
	ps.setInt(4, forumPost.getUserId());
	ps.setString(5, forumPost.getText());
	ps.setTimestamp(6, new java.sql.Timestamp(forumPost.getDate().getTime()));
	ps.setTimestamp(7, new java.sql.Timestamp(forumPost.getLastEditDate().getTime()));
	ps.setInt(8, forumPost.getEditUserId());
	ps.executeUpdate();

	if(forumPost.getId() < 0) // get the assigned id
	{
	    ResultSet rs = ps.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    forumPost.setId(rs.getInt(1));

	    // TODO update topic count, last post id and last post time

	}

	return forumPost;
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
	String sqlQuery = "UPDATE forum_topic SET topic_views = topic_views +1 WHERE topic_id = ?";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setInt(1, topicId);
	ps.executeUpdate();
    }

    public ForumPost createPost(ResultSet rs) throws SQLException
    {
	ForumPost post = new ForumPost();
	post.setId(rs.getInt("post_id"));
	post.setId(rs.getInt("topic_id"));
	post.setUserId(rs.getInt("user_id"));
	post.setGroupId(rs.getInt("group_id"));
	post.setText(rs.getString("text"));
	post.setDate(new Date(rs.getTimestamp("post_time").getTime()));
	post.setLastEditDate(new Date(rs.getTimestamp("post_edit_time").getTime()));
	post.setEditCount(rs.getInt("post_edit_count"));
	post.setEditUserId(rs.getInt("post_edit_user_id"));
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
	return topic;
    }
}

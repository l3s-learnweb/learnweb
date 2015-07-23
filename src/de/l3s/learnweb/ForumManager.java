package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private final static String FORUMPOSTCOLUMNS = "post_id, topic_id,group_id, user_id, text, post_time, post_edit_time, post_edit_user_id";
    private final static String FORUMTOPICCOLUMNS = "topic_id, group_id, topic_title, user_id, topic_time, topic_views, topic_replies, topic_last_post_id";

    private Learnweb learnweb;

    protected ForumManager(Learnweb learnweb) throws SQLException
    {
	super();
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
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FORUMTOPICCOLUMNS + " FROM `forum_topic` WHERE group_id = ? ORDER BY topic_time DESC");
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
    public List<ForumPost> getPostsByTopic(int topicId) throws SQLException
    {
	LinkedList<ForumPost> posts = new LinkedList<ForumPost>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FORUMPOSTCOLUMNS + " FROM `forum_post` WHERE topic_id = ? ORDER BY topic_time DESC");
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

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FORUMPOSTCOLUMNS + " FROM `forum_post` WHERE user_id = ? ORDER BY topic_time DESC");
	select.setInt(1, userId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    posts.add(createPost(rs));
	}
	select.close();

	return posts;
    }

    // todo user object ForumPost

    public ForumTopic saveTopic(ForumTopic forumTopic) throws SQLException
    {
	// TODO use static columns var (see user manager)
	String sqlQuery = "REPLACE INTO `forum_topics` (" + FORUMTOPICCOLUMNS + ") VALUES (?,?,?,?,?,?,?,?)";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	if(forumTopic.getTopicId() < 0)
	    ps.setNull(1, java.sql.Types.INTEGER);
	else
	    ps.setInt(1, forumTopic.getTopicId());
	ps.setInt(2, forumTopic.getGroupId());
	ps.setString(3, forumTopic.getTopic());
	ps.setInt(4, forumTopic.getUserId());
	ps.setDate(4, forumTopic.getDate() == null ? null : new java.sql.Date(forumTopic.getDate().getTime()));
	ps.setInt(5, forumTopic.getTopicView());
	ps.setInt(6, forumTopic.getTopicReplies());
	ps.setInt(7, forumTopic.getTopicLastPostId());
	ps.executeUpdate();
	// TODO set assigned id ; see user manager

	if(forumTopic.getTopicId() < 0) // get the assigned id
	{
	    ResultSet rs = ps.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    forumTopic.setTopicId(rs.getInt(1));
	}

	return forumTopic;
    }

    public ForumPost saveForumPost(ForumPost forumPost) throws SQLException
    {
	String sqlQuery = "REPLACE INTO `forum_post` (" + FORUMPOSTCOLUMNS + ") VALUES (?,?,?,?,?,?,?,?)";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);

	if(forumPost.getPostId() < 0)
	    ps.setNull(1, java.sql.Types.INTEGER);
	else
	    ps.setInt(1, forumPost.getPostId());
	ps.setInt(2, forumPost.getTopicId());
	ps.setInt(3, forumPost.getGroupId());
	ps.setInt(4, forumPost.getUserId());
	ps.setString(5, forumPost.getText());
	ps.setDate(6, forumPost.getDate() == null ? null : new java.sql.Date(forumPost.getDate().getTime()));
	ps.setDate(7, forumPost.getLastEditDate() == null ? null : new java.sql.Date(forumPost.getLastEditDate().getTime()));
	ps.setInt(8, forumPost.getEditUserId());
	ps.executeUpdate();

	if(forumPost.getPostId() < 0) // get the assigned id
	{
	    ResultSet rs = ps.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    forumPost.setTopicId(rs.getInt(1));
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

    public void incTopicViews(int topicId) throws SQLException
    {
	String sqlQuery = "UPDATE forum_topic SET topic_views = topic_views +1 WHERE topic_id = ?";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setInt(1, topicId);
	ps.executeUpdate();
    }

    // don't return a resultset return a ForumPost object; write a createPost and createTopic method (you will need it multiple times).
    /*
        public ForumPost getForumPost(int postId) throws SQLException
        {
    	ForumPost editPost = new ForumPost();
    	String sqlQuery = "Select text, post_edit_count Where post_id = postId";
    	@SuppressWarnings("unused")
    	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
    	//	editPost = ps.executeQuery();
    	return editPost;
        }
    */

    public ForumPost createPost(ResultSet rs) throws SQLException
    {
	int postId = rs.getInt("post_id");
	ForumPost forumPost = new ForumPost();
	forumPost.setPostId(postId);
	forumPost.setTopicId(rs.getInt("topic_id"));
	forumPost.setUserId(rs.getInt("user_id"));
	forumPost.setGroupId(rs.getInt("group_id"));
	forumPost.setText(rs.getString("text"));
	forumPost.setDate(rs.getDate("post_time") == null ? null : new java.sql.Date(rs.getDate("post_time").getTime()));
	forumPost.setLastEditDate(rs.getDate("post_edit_time") == null ? null : new java.sql.Date(rs.getDate("post_edit_time").getTime()));
	forumPost.setEditCount(rs.getInt("post_edit_count"));
	forumPost.setEditUserId(rs.getInt("post_edit_user_id"));
	return forumPost;
    }

    public ForumTopic createTopic(ResultSet rs) throws SQLException
    {
	int topicId = rs.getInt("topic_id");
	ForumTopic forumTopic = new ForumTopic();
	forumTopic.setTopicId(topicId);
	forumTopic.setUserId(rs.getInt("user_id"));
	forumTopic.setGroupId(rs.getInt("group_id"));
	forumTopic.setTopic(rs.getString("topic_title"));
	forumTopic.setDate(rs.getDate("topic_time") == null ? null : new java.sql.Date(rs.getDate("topic_time").getTime())); // TODO sql.date contains only the date not the time you have to use rs.getTimeStamp (see usermanager for example) This must is also a problem while saving a date
	forumTopic.setTopicView(rs.getInt("topic_views"));
	forumTopic.setTopicReplies(rs.getInt("topic_replies"));
	forumTopic.setTopicLastPostId(rs.getInt("topic_last_post_id"));
	return forumTopic;
    }
}

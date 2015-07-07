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
    public List<ForumTopic> getTopicsByGroup(int groupId, ORDER order) throws SQLException
    {
	LinkedList<ForumTopic> topics = new LinkedList<ForumTopic>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FORUMTOPICCOLUMNS + " FROM `forum_topic` WHERE group_id = ? ORDER BY ORDER");
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

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FORUMPOSTCOLUMNS + " FROM `forum_post` WHERE topic_id = ? ORDER BY ORDER");
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

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FORUMPOSTCOLUMNS + " FROM `forum_post` WHERE user_id = ? ORDER BY ORDER");
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
	String sqlQuery = "Insert into forum_topic(topic_title,group_id) values (?,?) ";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setString(1, forumTopic.getTopic());
	ps.setInt(2, forumTopic.getGroupId());
	ps.executeUpdate();

	// TODO set assigned id ; see user manager

	return null;
    }

    public ForumPost saveForumPost(ForumPost forumPost) throws SQLException
    {
	String sqlQuery = "Insert into forum_post(text, topic_id, group_id, user_id) values (?,?,?,?) ";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setString(1, forumPost.getText());
	ps.setInt(2, forumPost.getTopicId());
	ps.setInt(2, forumPost.getGroupId());
	ps.setInt(2, forumPost.getUserId());
	ps.executeUpdate();
	return null;
    }

    /**
     * increment topic view counter
     * 
     * @param topicId
     * 
     * 
     *            public void incTopicViews(int topicId)
     *            {
     *            int readCount = getReadStatus() + 1;
     *            String sqlQuery = "UPDATE forum_topic WHERE "
     * 
     * 
     *            }
     * 
     *            private int getReadStatus()
     *            {
     *            // TODO Auto-generated method stub
     *            return 0;
     *            }
     */

    /**
     * 
     * @param postId
     * @param text new text
     * @param user the user who changed the text
     * @return
     * @throws SQLException
     */

    public ForumPost editForumPost(int postId, String text, User user) throws SQLException
    {
	// ResultSet editPost = getForumPost(postId);

	String sqlQuery = "Update forum_post set text = text Where postId = postId AND user_id = userId"; //increase change counter set user who changed the post
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	int status = ps.executeUpdate();

	return null;
    }

    // don't return a resultset return a ForumPost object; write a createPost and createTopic method (you will need it multiple times).

    public ForumPost getForumPost(int postId) throws SQLException
    {
	ForumPost editPost = new ForumPost();
	String sqlQuery = "Select text, post_edit_count Where post_id = postId";
	@SuppressWarnings("unused")
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	//	editPost = ps.executeQuery();
	return editPost;
    }

    public ForumPost createPost(ResultSet rs) throws SQLException
    {
	int postId = rs.getInt("post_id");
	ForumPost forumPost = new ForumPost();
	forumPost.setPostId(postId);
	forumPost.setTopicId(rs.getInt("topic_id"));
	forumPost.setUserId(rs.getInt("user_id"));
	forumPost.setGroupId(rs.getInt("group_id"));
	forumPost.setText(rs.getString("text"));
	forumPost.setDate(rs.getDate("post_time"));
	forumPost.setLastEditDate(rs.getDate("post_edit_time"));
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
	forumTopic.setDate(rs.getDate("topic_time"));
	forumTopic.setTopicView(rs.getInt("topic_views"));
	forumTopic.setTopicReplies(rs.getInt("topic_replies"));
	forumTopic.setTopicLastPostId(rs.getInt("topic_last_post_id"));
	return forumTopic;
    }
}

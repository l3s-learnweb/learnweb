package de.l3s.learnweb;

import java.sql.PreparedStatement;
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

    private Learnweb learnweb;

    protected ForumManager(Learnweb learnweb) throws SQLException
    {
	super();
	this.learnweb = learnweb;
    }

    // String topic, int group_id
    //ForumTopic topic 
    // user 

    /**
     * returns all topic of the define group. Sorted by ORDER
     * 
     * @param groupId
     * @param order
     * @return
     */
    public List<ForumTopic> getTopicsByGroup(int groupId, ORDER order)
    {
	LinkedList<ForumTopic> topics = new LinkedList<ForumTopic>();
	// TODO
	return topics;
    }

    /**
     * Sorted by date DESC
     * 
     * @param topicId
     * @return
     */
    public List<ForumPost> getPostsByTopic(int topicId)
    {
	LinkedList<ForumPost> posts = new LinkedList<ForumPost>();
	// TODO
	return posts;
    }

    public List<ForumPost> getPostsByUser(int userId, ORDER order)
    {
	LinkedList<ForumPost> posts = new LinkedList<ForumPost>();
	// TODO
	return posts;
    }

    // todo user object ForumPost

    public ForumTopic saveTopic(ForumTopic forumTopic) throws SQLException
    {
	String sqlQuery = "Insert into forum_topic(topic_title,group_id) values (?,?) ";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setString(1, forumTopic.getTopic());
	ps.setInt(2, forumTopic.getGroupId());
	ps.executeUpdate();
	return null;
    }

    public ForumPost saveForumPost(String post, int topic_id, int group_id, int user_id) throws SQLException
    {
	String sqlQuery = "Insert into forum_post(text, topic_id, group_id, user_id) values (?,?,?,?) ";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setString(1, post);
	ps.setInt(2, topic_id);
	ps.setInt(2, group_id);
	ps.setInt(2, user_id);
	int status = ps.executeUpdate();
	return null;
    }

    /**
     * increment topic view counter
     * 
     * @param topicId
     */
    public void incTopicViews(int topicId)
    {

    }

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
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	//	editPost = ps.executeQuery();
	return editPost;
    }

    public void createPost()
    {

    }

    public void createTopic()
    {

    }
}

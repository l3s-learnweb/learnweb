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

    private Learnweb learnweb;

    protected ForumManager(Learnweb learnweb) throws SQLException
    {
	super();
	this.learnweb = learnweb;
    }

    // String topic, int group_id
    // user 
    public ForumTopic saveTopic(ForumTopic topic) throws SQLException
    {
	// this table name is wrong. have you ever tested this method?
	String sqlQuery = "Insert into forum_topics(topic_title,group_id) values (?,?) ";
	/*
		PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
		//ps.setInt(2, group_id);
		int status = ps.executeUpdate();
		
		
		 * don't return such messages.
		 * return 
		 * 
		if(status == 1) 
		    return "Forum topic inserted successfully";
		else
		    return "Forum topic insert failed";
		    */
	return null;
    }

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
    public String saveForumPost(String post, int topic_id, int group_id, int user_id) throws SQLException
    {
	String sqlQuery = "Insert into forum_post(text, topic_id, group_id, user_id) values (?,?,?,?) ";

	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setString(1, post);
	ps.setInt(2, topic_id);
	ps.setInt(2, group_id);
	ps.setInt(2, user_id);
	int status = ps.executeUpdate();

	if(status == 1)
	    return "Forum post inserted successfully"; // don't return  messages
	else
	    return "Forum post insert failed";
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
    public String editForumPost(int postId, String text, User user) throws SQLException
    {
	ResultSet editPost = getForumPost(postId);

	String sqlQuery = "Update forum_post set text = text Where postId = postId AND user_id = userId"; //increase change counter set user who changed the post
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	int status = ps.executeUpdate();

	if(status == 1)
	    return "Forum post Updated successfully";
	else
	    return "Forum post Update failed";
    }

    // don't return a resultset return a ForumPost object; write a createPost and createTopic method (you will need it multiple times).
    public ResultSet getForumPost(int postId) throws SQLException
    {
	String sqlQuery = "Select text, post_edit_count Where post_id = postId";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ResultSet editPost = ps.executeQuery();
	return editPost;
    }
}

package de.l3s.learnweb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class ForumManager
{
    private final static Logger log = Logger.getLogger(ForumManager.class);

    private Learnweb learnweb;
    private Connection dbCon;

    protected ForumManager(Learnweb learnweb) throws SQLException
    {
	super();
	this.learnweb = learnweb;
    }

    public String saveForumTopic(String topic, int group_id) throws SQLException
    {

	String sqlQuery = "Insert into forum_topics(topic_title,group_id) values (?,?) ";

	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setString(1, topic);
	ps.setInt(2, group_id);
	int status = ps.executeUpdate();

	if(status == 1)
	    return "Forum topic inserted successfully";
	else
	    return "Forum topic insert failed";
    }

    public List<ForumTopic> getTopicsByGroup(int groupId)
    {
	LinkedList<ForumTopic> topics = new LinkedList<ForumTopic>();
	return topics;
    }

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
	    return "Forum post inserted successfully";
	else
	    return "Forum post insert failed";
    }

}

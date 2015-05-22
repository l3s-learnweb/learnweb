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

    public String saveForumPost(String name, String email, String query) throws SQLException
    {

	String sqlQuery = "Insert into forum_post_test(name,email,query) values (?,?,?) ";

	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);
	ps.setString(1, name);
	ps.setString(2, email);
	ps.setString(3, query);
	int status = ps.executeUpdate();

	if(status == 1)
	    return "success";
	else
	    return "fail";
    }

    public List<ForumTopic> getTopicsByGroup(int groupId)
    {
	LinkedList<ForumTopic> topics = new LinkedList<ForumTopic>();

	// TODO

	return topics;
    }

}

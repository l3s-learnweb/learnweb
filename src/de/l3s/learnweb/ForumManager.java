package de.l3s.learnweb;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class ForumManager
{
    private final static Logger log = Logger.getLogger(ForumManager.class);

    private Learnweb learnweb;

    protected ForumManager(Learnweb learnweb) throws SQLException
    {
	super();
	this.learnweb = learnweb;

	learnweb.getConnection();
    }

    public List<ForumTopic> getTopicsByGroup(int groupId)
    {
	LinkedList<ForumTopic> topics = new LinkedList<ForumTopic>();

	// TODO

	return topics;
    }

}

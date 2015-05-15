package de.l3s.learnweb;

import java.sql.SQLException;

import org.apache.log4j.Logger;

public class ForumManager
{
    private final static Logger log = Logger.getLogger(ForumManager.class);

    private Learnweb learnweb;

    public ForumManager(Learnweb learnweb) throws SQLException
    {
	super();
	this.learnweb = learnweb;

	learnweb.getConnection();
    }

}

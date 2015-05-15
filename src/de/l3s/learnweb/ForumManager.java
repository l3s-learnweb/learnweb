package de.l3s.learnweb;

import org.apache.log4j.Logger;

public class ForumManager
{
    private final static Logger log = Logger.getLogger(ForumManager.class);

    private Learnweb learnweb;

    public ForumManager(Learnweb learnweb)
    {
	super();
	this.learnweb = learnweb;
    }

}

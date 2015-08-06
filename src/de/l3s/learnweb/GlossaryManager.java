package de.l3s.learnweb;

import java.sql.SQLException;

public class GlossaryManager
{
    enum ORDER // possible order values; Not all values are applicable for every method
    {
	DATE,
	TITLE,
	REPLIES,
	VIEWS,
	TOPIC
    }

    private final static String GLOSSARY = "resource_id, user_id, item, description, topic, italian, german, spanisch, spanish, lastModified";

    private final Learnweb learnweb;

    protected GlossaryManager(Learnweb learnweb) throws SQLException
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

}

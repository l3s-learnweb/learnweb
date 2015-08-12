package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class GlossaryManager
{
    enum ORDER// possible order values; Not all values are applicable for every method
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

    public List<Glossary> getGlosseryByResourceId(int resourceId) throws SQLException
    {
	LinkedList<Glossary> glosserys = new LinkedList<Glossary>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + GLOSSARY + " FROM `glossery` WHERE resource_id = ? ORDER BY lastModified DESC");
	select.setInt(1, resourceId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    glosserys.add(createGlossery(rs));
	}

	select.close();
	return glosserys;
    }

    private Glossary createGlossery(ResultSet rs) throws SQLException
    {
	Glossary glossary = new Glossary();
	glossary.setId(rs.getInt("glossery_id"));
	glossary.setResourceId(rs.getInt("resource_id"));
	glossary.setUserId(rs.getInt("user_id"));
	glossary.setTopic(rs.getString("topic"));
	glossary.setDescription(rs.getString("description"));
	glossary.setGerman(rs.getString("german"));
	glossary.setItalian(rs.getString("italian"));
	glossary.setSpanish(rs.getString("spanish"));
	glossary.setLastModified(new Date(rs.getTimestamp("last_modified").getTime()));
	return glossary;
    }

    public Glossary save(Glossary glossary) throws SQLException
    {
	String sqlQuery = "REPLACE INTO `glossery` (" + GLOSSARY + ") VALUES (?,?,?,?,?,?,?,?,?)";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);

	if(glossary.getId() < 0)
	    ps.setNull(1, java.sql.Types.INTEGER);
	else
	    ps.setInt(1, glossary.getId());
	ps.setInt(2, glossary.getResourceId());
	ps.setInt(3, glossary.getUserId());
	ps.setString(4, glossary.getTopic());
	ps.setString(5, glossary.getDescription());
	ps.setString(6, glossary.getGerman());
	ps.setString(7, glossary.getItalian());
	ps.setString(8, glossary.getSpanish());
	ps.setTimestamp(9, new java.sql.Timestamp(glossary.getLastModified().getTime()));
	ps.executeUpdate();

	if(glossary.getId() < 0)
	{
	    ResultSet rs = ps.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    glossary.setId(rs.getInt(1));
	}

	return glossary;
    }

    public int deleteByGlossaryId(int glossaryId) throws SQLException
    {
	PreparedStatement select = learnweb.getConnection().prepareStatement("DELETE FROM `glossery` WHERE glossary_id = ?");
	select.setInt(1, glossaryId);
	select.executeQuery();
	return glossaryId;
    }

}

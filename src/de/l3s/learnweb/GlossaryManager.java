package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class GlossaryManager
{

    private final static String GLOSSARY = "glossary_id, resource_id, user_id, item, description, topic, italian, german, spanish, reference_url, last_modified";

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

    public List<Glossary> getGlossaryByResourceId(int resourceId) throws SQLException
    {
	LinkedList<Glossary> glossarys = new LinkedList<Glossary>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + GLOSSARY + " FROM `lw_glossary` WHERE resource_id = ? AND deleted = 0 ORDER BY last_modified DESC");
	select.setInt(1, resourceId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    glossarys.add(createGlossary(rs));
	}

	select.close();
	return glossarys;
    }

    public List<Glossary> getGlossaryByUserId(int userId) throws SQLException
    {
	LinkedList<Glossary> glossarys = new LinkedList<Glossary>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + GLOSSARY + " FROM `lw_glossary` WHERE user_id = ? AND deleted = 0 ORDER BY last_modified DESC");
	select.setInt(1, userId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    glossarys.add(createGlossary(rs));
	}

	select.close();
	return glossarys;
    }

    private Glossary createGlossary(ResultSet rs) throws SQLException
    {
	Glossary glossary = new Glossary();
	glossary.setId(rs.getInt("glossary_id"));
	glossary.setResourceId(rs.getInt("resource_id"));
	glossary.setUserId(rs.getInt("user_id"));
	glossary.setItem(rs.getString("item"));
	glossary.setTopic(rs.getString("topic"));
	glossary.setDescription(rs.getString("description"));
	glossary.setGerman(rs.getString("german"));
	glossary.setItalian(rs.getString("italian"));
	glossary.setSpanish(rs.getString("spanish"));
	glossary.setReference(rs.getString("reference_url"));
	glossary.setLastModified(new Date(rs.getTimestamp("last_modified").getTime()));
	return glossary;
    }

    public Glossary save(Glossary selectedEntry) throws SQLException
    {
	String sqlQuery = "REPLACE INTO `lw_glossary` (" + GLOSSARY + ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";
	PreparedStatement ps = learnweb.getConnection().prepareStatement(sqlQuery);

	if(selectedEntry.getId() < 0)
	    ps.setNull(1, java.sql.Types.INTEGER);
	else
	    ps.setInt(1, selectedEntry.getId());
	ps.setInt(2, selectedEntry.getResourceId());
	ps.setInt(3, selectedEntry.getUserId());
	ps.setString(4, selectedEntry.getItem());
	ps.setString(5, selectedEntry.getDescription());
	ps.setString(6, selectedEntry.getTopic());
	ps.setString(7, selectedEntry.getGerman());
	ps.setString(8, selectedEntry.getItalian());
	ps.setString(9, selectedEntry.getSpanish());
	ps.setString(10, selectedEntry.getReference());
	ps.setTimestamp(11, new java.sql.Timestamp(selectedEntry.getLastModified().getTime()));
	ps.executeUpdate();

	if(selectedEntry.getId() < 0)
	{
	    ResultSet rs = ps.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    selectedEntry.setId(rs.getInt(1));
	}

	return selectedEntry;
    }

    public int delete(int glossaryId) throws SQLException
    {
	PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary` SET deleted = 1 WHERE glossary_id = ?");
	update.setInt(1, glossaryId);
	int updateVal = update.executeUpdate();
	return updateVal;
    }

}

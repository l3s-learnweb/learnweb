package de.l3s.learnweb.group;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Link.LinkType;

/**
 * DAO for the Link class.
 *
 * @author Philipp
 *
 */
public class LinkManager
{
    private static String COLUMNS = "link_id, group_id, type, url, title";

    private Learnweb learnweb;

    public LinkManager(Learnweb learnweb) throws SQLException
    {
        super();
        this.learnweb = learnweb;
    }

    public List<Link> getLinksByGroupId(int groupId, LinkType type) throws SQLException
    {
        LinkedList<Link> links = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_link` WHERE group_id = ? AND type = ? AND deleted = 0 ORDER BY title");
        select.setInt(1, groupId);
        select.setString(2, type.name());
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            links.add(new Link(rs));
        }
        select.close();
        return links;
    }

    /**
     * Sets the deleted flag of a link (can be restored)
     *
     * @param linkId
     * @throws SQLException
     */
    public void deleteLink(int linkId) throws SQLException
    {
        PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_link` SET deleted = 1 WHERE link_id = ?");
        update.setInt(1, linkId);
        update.executeUpdate();
        update.close();
    }

    /**
     * Deletes the link (can't be restored)
     *
     * @param linkId
     * @throws SQLException
     */
    public void deleteLinkHard(int linkId) throws SQLException
    {
        PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_link` WHERE link_id = ?");
        delete.setInt(1, linkId);
        delete.executeUpdate();
        delete.close();
    }

    /**
     * Saves the link to the database.
     * If the link is not yet stored at the database, a new record will be created and the returned link contains the new id.
     *
     * @param link
     * @return
     * @throws SQLException
     */
    public synchronized Link save(Link link) throws SQLException
    {
        if(null == link)
            throw new IllegalArgumentException("parameter must not be null");

        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_link` (" + COLUMNS + ") VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

        if(link.getId() < 0) // the link is not yet stored at the database
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, link.getId());
        replace.setInt(2, link.getGroupId());
        replace.setString(3, link.getType().name());
        replace.setString(4, link.getUrl());
        replace.setString(5, link.getTitle());
        replace.executeUpdate();

        if(link.getId() < 0) // it's a new link -> get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            link.setId(rs.getInt(1));
        }
        replace.close();

        return link;
    }
}

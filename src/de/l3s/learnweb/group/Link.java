package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Link class used in the groups
 * 
 * @author Kemkes
 *
 */
public class Link implements Comparable<Link>, Serializable
{
    private static final long serialVersionUID = -2629178586222765571L;

    public enum LinkType
    {
        LINK,
        DOCUMENT
    }

    private int id;
    private int groupId;
    private LinkType type;
    private String title;
    private String url;

    protected Link(ResultSet rs) throws SQLException
    {
        super();
        this.id = rs.getInt("link_id");
        this.groupId = rs.getInt("group_id");
        this.title = rs.getString("title");
        this.url = rs.getString("url");
        this.type = LinkType.valueOf(LinkType.class, rs.getString("type"));
    }

    /**
     * Creates a temporary Link-object witch isn't stored in the database
     */
    public Link()
    {
        super();
        this.id = -1;
        this.type = LinkType.LINK;
    }

    /**
     * Creates a temporary Link-object witch isn't stored in the database
     * 
     * @param type
     * @param title
     * @param url
     */
    public Link(LinkType type, String title, String url)
    {
        super();
        this.id = -1;
        this.groupId = -1;
        this.type = type;
        this.title = title;
        this.url = url;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public int getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getUrl()
    {
        return url;
    }

    public LinkType getType()
    {
        return type;
    }

    public void setType(LinkType type)
    {
        this.type = type;
    }

    /**
     * This method should only be called by LinkManager
     * 
     * @param id
     */
    protected void setId(int id)
    {
        this.id = id;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    @Override
    public int compareTo(Link o)
    {
        return getTitle().compareTo(o.getTitle());
    }

    @Override
    public String toString()
    {
        return "Link [id=" + id + ", groupId=" + groupId + ", type=" + type + ", title=" + title + ", url=" + url + "]";
    }

}

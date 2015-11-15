package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class Folder implements Serializable
{
    private static final long serialVersionUID = 2147007718176177138L;

    private int folderId = -1;
    private int groupId = -1;
    private int parentFolderId;
    private String name;

    // cache
    private transient String path;
    private transient String prettyPath = null;

    public Folder()
    {
	super();
    }

    public Folder(int folderId, int groupId, int parentFolderId, String name)
    {
	super();
	this.folderId = folderId;
	this.groupId = groupId;
	this.parentFolderId = parentFolderId;
	this.name = name;
    }

    public int getFolderId()
    {
	return folderId;
    }

    public void setFolderId(int folderId)
    {
	this.folderId = folderId;
    }

    public int getGroupId()
    {
	return groupId;
    }

    public void setGroupId(int groupId)
    {
	this.groupId = groupId;
    }

    public int getParentFolderId()
    {
	return parentFolderId;
    }

    public void setParentFolderId(int parentFolderId)
    {
	this.parentFolderId = parentFolderId;
    }

    public String getName()
    {
	return name;
    }

    public void setName(String name)
    {
	this.name = name;
    }

    /**
     * returns a string representation of the resources path
     * 
     * @return
     * @throws SQLException
     */
    public String getPath() throws SQLException
    {
	if(null == path)
	{
	    StringBuilder sb = new StringBuilder();

	    Folder folder = getParentFolder();
	    while(folder != null)
	    {
		sb.insert(0, "/");
		sb.insert(0, folder.getFolderId());
		folder = folder.getParentFolder();
	    }

	    sb.insert(0, "/");

	    sb.append(this.getFolderId());
	    path = sb.toString();
	}
	return path;
    }

    /**
     * returns a string representation of the resources path for views
     * 
     * @return
     * @throws SQLException
     */
    public String getPrettyPath() throws SQLException
    {
	if(null == prettyPath)
	{
	    StringBuilder sb = new StringBuilder();

	    Folder folder = getParentFolder();
	    while(folder != null)
	    {
		sb.insert(0, "/" + folder.getName());
		folder = folder.getParentFolder();
	    }

	    sb.append("/" + this.getName());
	    path = sb.toString();
	}
	return path;
    }

    public List<Folder> getSubfolders() throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().getFolders(groupId, folderId);
    }

    public Folder getParentFolder() throws SQLException
    {
	if(parentFolderId == 0)
	    return null;

	return Learnweb.getInstance().getGroupManager().getFolder(parentFolderId);
    }
}

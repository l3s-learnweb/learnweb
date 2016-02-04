package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.util.HasId;

public class Folder implements Serializable, HasId
{
    private static final long serialVersionUID = 2147007718176177138L;
    private static Logger log = Logger.getLogger(Folder.class);

    private int folderId = -1;
    private int groupId = -1;
    private int parentFolderId;
    private String name;
    private int userId = -1;

    // cache
    private transient String path;
    private transient String prettyPath = null;
    private transient List<Folder> subfolders;

    public Folder()
    {
	super();
    }

    public Folder(int groupId, String name)
    {
	super();
	this.groupId = groupId;
	this.name = name;
    }

    public Folder(int folderId, int groupId, String name)
    {
	super();
	this.folderId = folderId;
	this.groupId = groupId;
	this.name = name;
    }

    @Override
    public int getId()
    {
	return getFolderId();
    }

    public int getFolderId()
    {
	return folderId;
    }

    public void setFolderId(int folderId)
    {
	this.folderId = folderId;
    }

    public Group getGroup() throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().getGroupById(groupId);
    }

    public List<Resource> getResources() throws SQLException
    {
	ResourceManager rm = Learnweb.getInstance().getResourceManager();
	return rm.getResourcesByFolderId(folderId);

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
		sb.insert(0, " > " + folder.getName());
		folder = folder.getParentFolder();
	    }

	    sb.append(" > " + this.getName());
	    prettyPath = getGroup().getTitle() + sb.toString();
	}
	return prettyPath;
    }

    public List<Folder> getSubfolders() throws SQLException
    {
	if(subfolders == null)
	{
	    subfolders = Learnweb.getInstance().getGroupManager().getFolders(groupId, folderId);
	}

	return subfolders;
    }

    public Folder getParentFolder() throws SQLException
    {
	if(parentFolderId == 0)
	    return null;

	return Learnweb.getInstance().getGroupManager().getFolder(parentFolderId);
    }

    public User getUser() throws SQLException
    {
	if(userId < 0)
	    return null;
	return Learnweb.getInstance().getUserManager().getUser(userId);
    }

    public void setUser(User user)
    {
	this.userId = user.getId();
    }

    public int getUserId()
    {
	return userId;
    }

    public void setUserId(int userId)
    {
	this.userId = userId;
    }

    public Folder save() throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().saveFolder(this);
    }

    public Folder moveTo(int newGroupId, int newParentFolderId) throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().moveFolder(this, newParentFolderId, newGroupId);
    }

    public void delete() throws SQLException
    {
	Learnweb.getInstance().getGroupManager().deleteFolder(this);
    }

    public int getCountResources() throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().getCountResources(groupId, folderId);
    }

    public int getCountSubfolders() throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().getCountFolders(groupId, folderId);
    }

    protected void clearCaches()
    {
	path = null;
	prettyPath = null;
	subfolders = null;

	if(subfolders != null)
	{
	    for(Folder folder : subfolders)
	    {
		folder.clearCaches();
	    }

	    subfolders = null;
	}

	try
	{
	    getParentFolder().clearCaches();
	}
	catch(SQLException e)
	{
	    log.fatal("Couldn't clear folder cache", e);
	}
    }

    @Override
    public String toString()
    {
	return this.name;
    }
}

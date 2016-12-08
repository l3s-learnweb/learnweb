package de.l3s.learnweb;

import de.l3s.util.HasId;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class Folder implements Serializable, HasId, GroupItem
{
    private static final long serialVersionUID = 2147007718176177138L;
    private static final Logger log = Logger.getLogger(Folder.class);

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
        return folderId;
    }

    @Override
    public void setId(int id)
    {
        this.folderId = id;
    }

    @Deprecated
    public int getFolderId()
    {
        return this.getId();
    }

    @Deprecated
    public void setFolderId(int folderId)
    {
        this.setId(folderId);
    }

    @Override
    public int getGroupId()
    {
        return groupId;
    }

    @Override
    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    @Override
    public Group getGroup() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getGroupById(groupId);
    }

    public int getParentFolderId()
    {
        return parentFolderId;
    }

    public void setParentFolderId(int parentFolderId)
    {
        this.parentFolderId = parentFolderId;
    }

    public Folder getParentFolder() throws SQLException
    {
        if (parentFolderId == 0)
            return null;

        return Learnweb.getInstance().getGroupManager().getFolder(parentFolderId);
    }

    @Override
    public String getTitle()
    {
        return name;
    }

    @Override
    public void setTitle(String title)
    {
        this.name = title;
    }

    @Deprecated
    public String getName()
    {
        return this.getTitle();
    }

    @Deprecated
    public void setName(String name)
    {
        this.setTitle(name);
    }

    @Override
    public int getUserId()
    {
        return userId;
    }

    @Override
    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    @Override
    public User getUser() throws SQLException
    {
        if (userId < 0)
            return null;
        return Learnweb.getInstance().getUserManager().getUser(userId);
    }

    @Override
    public void setUser(User user)
    {
        this.userId = user.getId();
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByFolderId(folderId);
    }

    public List<Resource> getResourcesSubset() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getFolderResourcesByUserId(groupId, parentFolderId, userId, 4);
    }

    /**
     * @return a string representation of the resources path
     */
    @Override
    public String getPath() throws SQLException
    {
        if (null == path)
        {
            StringBuilder sb = new StringBuilder();

            Folder folder = getParentFolder();
            while (folder != null)
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
     * @return a string representation of the resources path for views
     */
    @Override
    public String getPrettyPath() throws SQLException
    {
        if (null == prettyPath)
        {
            StringBuilder sb = new StringBuilder();

            Folder folder = getParentFolder();
            while (folder != null)
            {
                sb.insert(0, " > " + folder.getName());
                folder = folder.getParentFolder();
            }

            sb.append(" > ").append(this.getName());
            prettyPath = getGroup().getTitle() + sb.toString();
        }
        return prettyPath;
    }

    public List<Folder> getSubfolders() throws SQLException
    {
        if (subfolders == null)
        {
            subfolders = Learnweb.getInstance().getGroupManager().getFolders(groupId, folderId);
        }

        return subfolders;
    }

    @Override
    public Folder save() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().saveFolder(this);
    }

    public Folder moveTo(int newGroupId, int newParentFolderId) throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().moveFolder(this, newParentFolderId, newGroupId);
    }

    @Override
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
        this.clearCaches(true, true);
    }

    protected void clearCaches(boolean isClearParent, boolean isClearSubRecurs)
    {
        path = null;
        prettyPath = null;

        try
        {
            if (getSubfolders() != null)
            {
                if (isClearSubRecurs)
                {
                    for (Folder folder : getSubfolders())
                    {
                        folder.clearCaches(false, true);
                    }
                }

                subfolders = null;
            }

            if (isClearParent && this.getParentFolderId() > 0)
            {
                getParentFolder().clearCaches(false, false);
            }

        }
        catch (SQLException e)
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

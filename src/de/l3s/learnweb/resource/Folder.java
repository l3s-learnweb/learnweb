package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

public class Folder extends AbstractResource implements Serializable, ResourceContainer
{
    private static final long serialVersionUID = 2147007718176177138L;
    private static final Logger log = Logger.getLogger(Folder.class);

    private int folderId = -1;
    private int groupId = -1;
    private int parentFolderId;
    private String name;
    private String description;
    private int userId = -1;
    private boolean deleted = false; // indicates whether this folder has been deleted

    // cache
    private transient String path;
    private transient String prettyPath = null;
    private transient List<Folder> subFolders;

    public Folder()
    {
        super();
    }

    public Folder(int groupId, String name)
    {
        this(-1, groupId, name, null);
    }

    public Folder(int groupId, String name, String description)
    {
        this(-1, groupId, name, description);
    }

    public Folder(int folderId, int groupId, String name)
    {
        this(folderId, groupId, name, null);
    }

    public Folder(int folderId, int groupId, String name, String description)
    {
        super();
        this.folderId = folderId;
        this.groupId = groupId;
        this.name = name;
        this.description = description;
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
        if(parentFolderId == 0)
            return null;

        if(parentFolderId == folderId)
        {
            log.warn("Folder " + folderId + " has itself as parent folder.");
            return null;
        }

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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
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

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(final boolean deleted)
    {
        this.deleted = deleted;
    }

    @Override
    public User getUser() throws SQLException
    {
        if(userId < 0)
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
        if(null == path)
        {
            StringBuilder sb = new StringBuilder();

            Folder folder = getParentFolder();
            while(folder != null)
            {
                sb.insert(0, "/");
                sb.insert(0, folder.getId());
                folder = folder.getParentFolder();
            }

            sb.insert(0, "/");

            sb.append(this.getId());
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
        if(null == prettyPath)
        {
            StringBuilder sb = new StringBuilder();

            Folder folder = getParentFolder();
            while(folder != null)
            {
                sb.insert(0, " > " + folder.getTitle());
                folder = folder.getParentFolder();
            }

            if(folderId != 0) sb.append(" > ").append(name);
            sb.insert(0, getGroup() != null ? getGroup().getTitle() : "Private resources");
            prettyPath = sb.toString();
        }
        return prettyPath;
    }

    public List<Folder> getSubFolders() throws SQLException
    {
        if(subFolders == null)
        {
            subFolders = Learnweb.getInstance().getGroupManager().getFolders(groupId, folderId);
        }

        return subFolders;
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
        for(Folder folder : this.getSubFolders())
        {
            folder.delete();
        }

        for(Resource resource : this.getResources())
        {
            resource.delete();
        }

        setDeleted(true);
        this.save();

        Folder parentFolder = this.getParentFolder();

        if(parentFolder != null)
        {
            parentFolder.clearCaches();
        }
    }

    public int getCountResources() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getCountResources(groupId, folderId);
    }

    public int getCountSubFolders() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getCountFolders(groupId, folderId);
    }

    public void clearCaches()
    {
        this.clearCaches(true, true);
    }

    protected void clearCaches(boolean isClearParent, boolean isClearSubRecurs)
    {
        path = null;
        prettyPath = null;

        try
        {
            if(getSubFolders() != null)
            {
                if(isClearSubRecurs)
                {
                    for(Folder folder : getSubFolders())
                    {
                        folder.clearCaches(false, true);
                    }
                }

                subFolders = null;
            }

            if(isClearParent && this.getParentFolderId() > 0)
            {
                getParentFolder().clearCaches(false, false);
            }

        }
        catch(SQLException e)
        {
            log.fatal("Couldn't clear folder cache", e);
        }
    }

    @Override
    public boolean canViewResource(User user) throws SQLException
    {
        Group group = getGroup();
        if(group != null)
            return group.canViewResources(user);

        return false;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}

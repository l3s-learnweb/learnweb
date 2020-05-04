package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

public class Folder extends AbstractResource implements Serializable, ResourceContainer
{
    private static final long serialVersionUID = 2147007718176177138L;
    private static final Logger log = LogManager.getLogger(Folder.class);

    private int id = -1;
    private int groupId = -1;
    private int parentFolderId;
    @NotBlank
    private String title;
    private String description;
    private int userId = -1;
    private boolean deleted = false; // indicates whether this folder has been deleted

    // cache
    private transient String path;
    private transient String prettyPath = null;
    private transient List<Folder> subFolders;

    public Folder()
    {
    }

    public Folder(int id, int groupId, String title)
    {
        this(id, groupId, title, null);
    }

    public Folder(int id, int groupId, String title, String description)
    {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.description = description;
    }

    /**
     * Copy constructor
     */
    public Folder(Folder another)
    {
        this.id = -1;
        this.groupId = another.getGroupId();
        this.parentFolderId = another.getParentFolderId();
        this.title = another.getTitle();
        this.description = another.getDescription();
        this.userId = another.getUserId();
        this.deleted = another.isDeleted();
    }

    @Override
    public int getId()
    {
        return id;
    }

    @Override
    public void setId(int id)
    {
        this.id = id;
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

        if(parentFolderId == id)
        {
            log.warn("Folder " + id + " has itself as parent folder.");
            return null;
        }

        return Learnweb.getInstance().getGroupManager().getFolder(parentFolderId);
    }

    public boolean isChildOf(int folderId) throws SQLException
    {
        if(folderId == 0)
            return true;

        Folder parentFolder = this.getParentFolder();
        while(parentFolder != null)
        {
            if(parentFolder.getId() == folderId)
                return true;
            parentFolder = parentFolder.getParentFolder();
        }

        return false;
    }

    public boolean isParentOf(int folderId) throws SQLException
    {
        if(folderId == 0)
            return false;

        Folder parentFolder = Learnweb.getInstance().getGroupManager().getFolder(folderId);
        return parentFolder.isChildOf(getId());
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public void setTitle(String title)
    {
        this.title = title;
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
        return Learnweb.getInstance().getResourceManager().getResourcesByFolderId(id);
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
                sb.insert(0, " > ");
                sb.insert(0, folder.getTitle());
                folder = folder.getParentFolder();
            }

            prettyPath = sb.append(title).toString();
        }
        return prettyPath;
    }

    @Override
    public List<Folder> getSubFolders() throws SQLException
    {
        if(subFolders == null)
        {
            subFolders = Learnweb.getInstance().getGroupManager().getFolders(groupId, id);
        }

        return subFolders;
    }

    @Override
    public Folder save() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().saveFolder(this);
    }

    @Override
    public void moveTo(int newGroupId, int newParentFolderId) throws SQLException
    {
        Learnweb.getInstance().getGroupManager().moveFolder(this, newParentFolderId, newGroupId);
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
        return this.title;
    }
}

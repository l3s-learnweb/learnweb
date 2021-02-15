package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

public class Folder extends AbstractResource implements Serializable, ResourceContainer {
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
    private transient String prettyPath;
    private transient List<Folder> subFolders;

    public Folder() {
    }

    public Folder(int id, int groupId, String title) {
        this(id, groupId, title, null);
    }

    public Folder(int id, int groupId, String title, String description) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.description = description;
    }

    /**
     * Copy constructor.
     */
    public Folder(Folder another) {
        this.id = -1;
        this.groupId = another.getGroupId();
        this.parentFolderId = another.getParentFolderId();
        this.title = another.getTitle();
        this.description = another.getDescription();
        this.userId = another.getUserId();
        this.deleted = another.isDeleted();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    @Override
    public Group getGroup() {
        return Learnweb.dao().getGroupDao().findById(groupId);
    }

    public int getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(int parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public Folder getParentFolder() {
        if (parentFolderId == 0) {
            return null;
        }

        if (parentFolderId == id) {
            log.warn("Folder {} has itself as parent folder.", id);
            return null;
        }

        return Learnweb.dao().getFolderDao().findById(parentFolderId);
    }

    public boolean isChildOf(int folderId) {
        if (folderId == 0) {
            return true;
        }

        Folder parentFolder = this.getParentFolder();
        while (parentFolder != null) {
            if (parentFolder.getId() == folderId) {
                return true;
            }
            parentFolder = parentFolder.getParentFolder();
        }

        return false;
    }

    public boolean isParentOf(int folderId) {
        if (folderId == 0) {
            return false;
        }

        Folder parentFolder = Learnweb.dao().getFolderDao().findById(folderId);
        return parentFolder.isChildOf(getId());
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public User getUser() {
        if (userId < 0) {
            return null;
        }
        return Learnweb.dao().getUserDao().findById(userId);
    }

    @Override
    public void setUser(User user) {
        this.userId = user.getId();
    }

    public List<Resource> getResources() {
        return Learnweb.dao().getResourceDao().findByFolderId(id);
    }

    public List<Resource> getResourcesSubset() {
        return Learnweb.dao().getResourceDao().findByGroupIdAndFolderIdAndOwnerId(groupId, parentFolderId, userId, 4);
    }

    /**
     * @return a string representation of the resources path
     */
    @Override
    public String getPath() {
        if (null == path) {
            StringBuilder sb = new StringBuilder();

            Folder folder = getParentFolder();
            while (folder != null) {
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
    public String getPrettyPath() {
        if (null == prettyPath) {
            StringBuilder sb = new StringBuilder();

            Folder folder = getParentFolder();
            while (folder != null) {
                sb.insert(0, " > ");
                sb.insert(0, folder.getTitle());
                folder = folder.getParentFolder();
            }

            prettyPath = sb.append(title).toString();
        }
        return prettyPath;
    }

    @Override
    public List<Folder> getSubFolders() {
        if (subFolders == null) {
            subFolders = Learnweb.dao().getFolderDao().findByGroupIdAndParentFolderId(groupId, id);
        }

        return subFolders;
    }

    @Override
    public Folder save() {
        Learnweb.dao().getFolderDao().save(this);
        return this;
    }

    @Override
    public void moveTo(int newGroupId, int newFolderId) {
        // TODO @astappiev: throw an error instead of silent ignore
        if (getId() == newFolderId) {
            return; // if move to itself
        }
        if (getGroupId() == newGroupId && isParentOf(newFolderId)) {
            return; // if move to own sub folder
        }

        int groupId = getGroupId();
        int parentFolderId = getParentFolderId();
        List<Folder> subFolders = getSubFolders();

        setGroupId(newGroupId);
        setParentFolderId(newFolderId);
        save();

        for (Folder subFolder : subFolders) {
            subFolder.moveTo(newGroupId, getId());
        }

        for (Resource resource : getResources()) {
            resource.setGroupId(newGroupId);
            resource.save();
        }

        if (newFolderId > 0) {
            Learnweb.dao().getFolderDao().findById(newFolderId).clearCaches();
        } else if (newGroupId > 0) {
            Learnweb.dao().getGroupDao().findById(newGroupId).clearCaches();
        }

        if (parentFolderId > 0) {
            Learnweb.dao().getFolderDao().findById(parentFolderId).clearCaches();
        } else if (groupId > 0) {
            Learnweb.dao().getGroupDao().findById(groupId).clearCaches();
        }
    }

    @Override
    public void delete() {
        for (Folder folder : this.getSubFolders()) {
            folder.delete();
        }

        for (Resource resource : this.getResources()) {
            resource.delete();
        }

        Learnweb.dao().getFolderDao().deleteSoft(id);

        Folder parentFolder = this.getParentFolder();

        if (parentFolder != null) {
            parentFolder.clearCaches();
        }
    }

    public void clearCaches() {
        this.clearCaches(true, true);
    }

    protected void clearCaches(boolean isClearParent, boolean isClearSubRecurs) {
        path = null;
        prettyPath = null;

        if (getSubFolders() != null) {
            if (isClearSubRecurs) {
                for (Folder folder : getSubFolders()) {
                    folder.clearCaches(false, true);
                }
            }

            subFolders = null;
        }

        if (isClearParent && this.getParentFolderId() > 0) {
            getParentFolder().clearCaches(false, false);
        }

    }

    @Override
    public boolean canViewResource(User user) {
        Group group = getGroup();
        if (group != null) {
            return group.canViewResources(user);
        }

        return false;
    }

    @Override
    public String toString() {
        return this.title;
    }
}

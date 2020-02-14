package de.l3s.learnweb.resource;

import java.sql.SQLException;
import java.util.Date;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

/**
 * The abstract class which is extended by Resource and Folder.
 * Groups may contains Resources and Folders (AbstractResource is common for them)
 */
public abstract class AbstractResource implements HasId
{
    private EditLocker editLocker;

    public abstract void setId(int id);

    public abstract String getTitle();

    public abstract void setTitle(String title);

    public abstract int getGroupId();

    public abstract void setGroupId(int groupId);

    public abstract Group getGroup() throws SQLException;

    public abstract int getUserId();

    public abstract void setUserId(int userId);

    public abstract User getUser() throws SQLException;

    public abstract void setUser(User user);

    public abstract AbstractResource save() throws SQLException;

    public abstract void delete() throws SQLException;

    public abstract String getPath() throws SQLException;

    public abstract String getPrettyPath() throws SQLException;

    public abstract boolean canViewResource(User user) throws SQLException;

    public abstract void moveTo(int newGroupId, int newFolderId) throws SQLException;

    public boolean canEditResource(User user) throws SQLException
    {
        if(user == null) return false; // not logged in

        if(getGroup() != null) return getGroup().canEditResource(user, this);
        return user.isAdmin() || getUserId() == user.getId();
    }

    public boolean canDeleteResource(User user) throws SQLException
    {
        if(user == null) return false; // not logged in

        // if the resource is part of a group the group policy has priority
        if(getGroup() != null) return getGroup().canDeleteResource(user, this);
        return user.isAdmin() || getUserId() == user.getId();
    }

    public boolean lockResource(User user)
    {
        if(isEditPossible())
        {
            editLocker = new EditLocker(user);
            return true;
        }

        return false;
    }

    public boolean unlockResource(User user)
    {
        if(editLocker != null && editLocker.getUser().equals(user))
        {
            editLocker = null;
            return true;
        }

        return false;
    }

    public boolean lockerUpdate(User user)
    {
        if(editLocker != null && editLocker.getUser().equals(user))
        {
            editLocker.setLastActivity(new Date());
            return true;
        }

        return false;
    }

    public boolean isEditLocked()
    {
        return editLocker != null;
    }

    public String getLockUsername()
    {
        if(editLocker != null)
        {
            return editLocker.getUser().getUsername();
        }

        return null;
    }

    public boolean isEditPossible()
    {
        if(!isEditLocked()) return true;

        if(editLocker.isSessionExpired())
        {
            editLocker = null;
            return true;
        }

        return false;
    }
}

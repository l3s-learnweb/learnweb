package de.l3s.learnweb;

import de.l3s.util.HasId;

import java.sql.SQLException;

/**
 * The abstract class which is extended by Resource and Folder.
 * Groups may contains Resources and Folders (AbstractResource is common for them)
 */
public abstract class AbstractResource implements HasId
{
    abstract public int getId();

    abstract public void setId(int id);

    abstract public String getTitle();

    abstract public void setTitle(String title);

    abstract public int getGroupId();

    abstract public void setGroupId(int groupId);

    abstract public Group getGroup() throws SQLException;

    abstract public int getUserId();

    abstract public void setUserId(int userId);

    abstract public User getUser() throws SQLException;

    abstract public void setUser(User user);

    abstract public AbstractResource save() throws SQLException;

    abstract public void delete() throws SQLException;

    abstract public String getPath() throws SQLException;

    abstract public String getPrettyPath() throws SQLException;

    abstract public boolean canViewResource(User user) throws SQLException;

    public boolean canEditResource(User user) throws SQLException
    {
        if(user == null) // not logged in
            return false;

        Group group = getGroup();

        if(group != null)
            return group.canEditResource(user, this);

        if(user.isAdmin() || getUserId() == user.getId())
            return true;

        return false;
    }

    public boolean canDeleteResource(User user) throws SQLException
    {
        if(user == null) // not logged in
            return false;

        Group group = getGroup();

        if(group != null) // if the resource is part of a group the group policy has priority
            return group.canDeleteResource(user, this);

        if(user.isAdmin() || getUserId() == user.getId())
            return true;

        return false;
    }
}

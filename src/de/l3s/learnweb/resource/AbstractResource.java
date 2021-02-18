package de.l3s.learnweb.resource;

import java.time.Instant;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;
import de.l3s.util.Deletable;
import de.l3s.util.HasId;

/**
 * The abstract class which is extended by Resource and Folder.
 * Groups may contains Resources and Folders (AbstractResource is common for them)
 */
public abstract class AbstractResource implements HasId, Deletable {
    private EditLocker editLocker;

    public abstract void setId(int id);

    public abstract String getTitle();

    public abstract void setTitle(String title);

    public abstract int getGroupId();

    public abstract void setGroupId(int groupId);

    public abstract Group getGroup();

    public abstract Integer getUserId();

    public abstract void setUserId(Integer userId);

    public abstract User getUser();

    public abstract void setUser(User user);

    public abstract AbstractResource save();

    public abstract void delete();

    public abstract String getPath();

    public abstract String getPrettyPath();

    public abstract boolean canViewResource(User user);

    public abstract void moveTo(int newGroupId, int newFolderId);

    public boolean canEditResource(User user) {
        if (user == null) {
            return false; // not logged in
        }

        if (getGroup() != null) {
            return getGroup().canEditResource(user, this);
        }
        return user.isAdmin() || getUserId() == user.getId();
    }

    public boolean canDeleteResource(User user) {
        if (user == null) {
            return false; // not logged in
        }

        // if the resource is part of a group the group policy has priority
        if (getGroup() != null) {
            return getGroup().canDeleteResource(user, this);
        }
        return user.isAdmin() || getUserId() == user.getId();
    }

    public boolean lockResource(User user) {
        if (user != null && isEditPossible()) {
            editLocker = new EditLocker(user);
            return true;
        }

        return false;
    }

    public boolean unlockResource(User user) {
        if (editLocker != null && editLocker.getUser().equals(user)) {
            editLocker = null;
            return true;
        }

        return false;
    }

    public boolean lockerUpdate(User user) {
        if (editLocker != null && editLocker.getUser().equals(user)) {
            editLocker.setLastActivity(Instant.now());
            return true;
        }

        return false;
    }

    public boolean isEditLocked() {
        return editLocker != null;
    }

    public String getLockUsername() {
        if (editLocker != null) {
            return editLocker.getUser().getUsername();
        }

        return null;
    }

    public boolean isEditPossible() {
        if (!isEditLocked()) {
            return true;
        }

        if (editLocker.isSessionExpired()) {
            editLocker = null;
            return true;
        }

        return false;
    }
}

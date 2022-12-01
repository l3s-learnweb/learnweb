package de.l3s.learnweb.resource;

import java.time.Instant;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;
import de.l3s.util.Deletable;
import de.l3s.util.HasId;

/**
 * The abstract class which is extended by Resource and Folder.
 * Groups may contain Resources and Folders (AbstractResource is common for them)
 */
public abstract class AbstractResource implements HasId, Deletable {
    private EditLocker editLocker;

    public abstract void setId(int id);

    public abstract String getTitle();

    public abstract void setTitle(String title);

    public abstract int getGroupId();

    public abstract void setGroupId(int groupId);

    public abstract Group getGroup();

    public abstract int getUserId();

    public abstract void setUserId(int userId);

    public abstract User getUser();

    public abstract void setUser(User user);

    public abstract AbstractResource save();

    public abstract void delete();

    public abstract String getPath();

    public abstract String getPrettyPath();

    public abstract void moveTo(int newGroupId, int newFolderId);

    public boolean canModerateResource(User user) {
        if (user == null || isDeleted()) {
            return false;
        }

        // resource owners and admins can always access the resource
        if (getUserId() == user.getId() || user.isAdmin()) {
            return true;
        }

        if (getGroupId() != 0 && user.isModerator() && getGroup().getCourse().isModerator(user)) {
            return true;
        }

        return false;
    }

    public boolean canViewResource(User user) {
        if (user == null) {
            return false;
        }

        if (canModerateResource(user)) {
            return true;
        }

        if (getGroup() != null) {
            return switch (getGroup().getPolicyView()) {
                case ALL_LEARNWEB_USERS -> true;
                case COURSE_MEMBERS -> getGroup().getCourse().isMember(user) || getGroup().isMember(user);
                case GROUP_MEMBERS -> getGroup().isMember(user);
                case GROUP_LEADER -> getGroup().isLeader(user);
            };
        }

        return false;
    }

    public boolean canEditResource(User user) {
        if (user == null) {
            return false; // not logged in
        }

        if (canModerateResource(user)) {
            return true;
        }

        if (getGroup() != null) {
            return switch (getGroup().getPolicyEdit()) {
                case GROUP_MEMBERS -> getGroup().isMember(user);
                case GROUP_LEADER -> getGroup().isLeader(user);
                case GROUP_LEADER_AND_FILE_OWNER -> getGroup().isLeader(user) || getUserId() == user.getId();
            };
        }

        return false;
    }

    public boolean canDeleteResource(User user) {
        return canEditResource(user); // currently, they share the same policy
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

package de.l3s.learnweb.group;

import java.io.Serial;
import java.util.List;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;

public final class PrivateGroup extends Group {
    @Serial
    private static final long serialVersionUID = -4541934616443225307L;

    public PrivateGroup(final String title, final User user) {
        super(0, title);
        setLeader(user);
    }

    /**
     * Only root folders which belongs to certain user.
     */
    @Override
    public List<Folder> getSubFolders() {
        if (folders == null) {
            folders = Learnweb.dao().getFolderDao().findByPrivateGroupAndRootFolder(getLeaderUserId());
        }

        return folders;
    }

    @Override
    public Course getCourse() {
        return null;
    }

    @Override
    public boolean canAddResources(final User user) {
        return isLeader(user);
    }

    @Override
    public boolean canDeleteGroup(final User user) {
        return false;
    }

    @Override
    public boolean canJoinGroup(final User user) {
        return false;
    }

    @Override
    public boolean canViewGroup(final User user) {
        return isLeader(user);
    }
}

package de.l3s.learnweb.group;

import java.util.List;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;

public class PrivateGroup extends Group {
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
            folders = Learnweb.dao().getFolderDao().findByGroupIdAndParentFolderIdAndUserId(getId(), 0, getLeaderUserId());
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
    public boolean canEditResource(final User user, final AbstractResource resource) {
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
    public boolean canViewResources(final User user) {
        return isLeader(user);
    }

    @Override
    public boolean canAnnotateResources(final User user) {
        return isLeader(user);
    }
}

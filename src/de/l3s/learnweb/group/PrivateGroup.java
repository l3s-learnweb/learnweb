package de.l3s.learnweb.group;

import java.util.List;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Folder;
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
}

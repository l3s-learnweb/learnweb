package de.l3s.maintenance.users;

import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.maintenance.MaintenanceTask;

public class UsersAddMissingProfilePictures extends MaintenanceTask {

    private UserManager userManager;

    @Override
    protected void init() throws Exception {
        userManager = getLearnweb().getUserManager();
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        deleteMissingAvatars();
        //setDefaultAvatars();
    }

    @SuppressWarnings("unused")
    private void setDefaultAvatars() throws SQLException {
        List<User> users = userManager.getUsers();
        for (User user : users) {
            File file = user.getImageFile();
            if (null == file || !file.exists()) {
                log.debug("Update user: {}", user);

                user.setDefaultProfilePicture();
                user.save();
            }
        }
    }

    /**
     * deletes avatars that were created on elsewhere and aren't present on the server.
     */
    private void deleteMissingAvatars() throws SQLException {
        List<User> users = userManager.getUsers();
        for (User user : users) {
            File file = user.getImageFile();
            if (null != file && !file.exists()) {
                log.debug("Update user: {}", user);

                user.setImageFileId(0);
                user.save();
            }
        }
    }

    public static void main(String[] args) {
        new UsersAddMissingProfilePictures().start(args);
    }
}

package de.l3s.maintenance.users;

import java.util.List;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.maintenance.MaintenanceTask;

public class UsersAddMissingProfilePictures extends MaintenanceTask {

    private UserDao userDao;

    @Override
    protected void init() {
        userDao = getLearnweb().getDaoProvider().getUserDao();
    }

    @Override
    protected void run(final boolean dryRun) {
        deleteMissingAvatars();
        //setDefaultAvatars();
    }

    @SuppressWarnings("unused")
    private void setDefaultAvatars() {
        List<User> users = userDao.findAll();
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
    private void deleteMissingAvatars() {
        List<User> users = userDao.findAll();
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

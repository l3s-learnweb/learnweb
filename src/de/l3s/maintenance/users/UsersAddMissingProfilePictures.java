package de.l3s.maintenance.users;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutableTriple;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.maintenance.MaintenanceTask;
import de.l3s.util.HashHelper;
import de.l3s.util.ProfileImageHelper;

public class UsersAddMissingProfilePictures extends MaintenanceTask {

    private UserDao userDao;
    private FileDao fileDao;

    @Override
    protected void init() {
        userDao = getLearnweb().getDaoProvider().getUserDao();
        fileDao = getLearnweb().getDaoProvider().getFileDao();
    }

    @Override
    protected void run(final boolean dryRun) {
        deleteMissingAvatars();
        //setDefaultAvatars();
    }

    @SuppressWarnings("unused")
    private void setDefaultAvatars() throws IOException {
        List<User> users = userDao.findAll();
        for (User user : users) {
            if (user.getImageFileId() == 0 && user.getEmail() != null) {
                log.debug("Update user: {}", user);

                ImmutableTriple<String, String, InputStream> gravatar = ProfileImageHelper.getGravatarAvatar(HashHelper.md5(user.getEmail()));

                if (gravatar != null) {
                    File file = new File(File.TYPE.PROFILE_PICTURE, gravatar.getLeft(), gravatar.getMiddle());
                    Learnweb.dao().getFileDao().save(file, gravatar.getRight());

                    user.setImageFileId(file.getId());
                    user.save();
                }
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
                fileDao.deleteHard(file);

                user.setImageFileId(0);
                user.save();
            }
        }
    }

    public static void main(String[] args) {
        new UsersAddMissingProfilePictures().start(args);
    }
}

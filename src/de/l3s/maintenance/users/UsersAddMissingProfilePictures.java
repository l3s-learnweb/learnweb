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
                    File file = new File(File.FileType.PROFILE_PICTURE, gravatar.getLeft(), gravatar.getMiddle());
                    Learnweb.dao().getFileDao().save(file, gravatar.getRight());

                    user.setImageFileId(file.getId());
                    userDao.save(user);
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
            File file = fileDao.findById(user.getImageFileId(), true).get();
            if (null != file && !file.isExists()) {
                log.debug("Image file {} of user {} doesn't exist", file, user);
                /*
                fileDao.deleteHard(file);
                user.setImageFileId(0);
                userDao.save(user);
                */
            }
        }
    }

    public static void main(String[] args) {
        new UsersAddMissingProfilePictures().start(args);
    }
}

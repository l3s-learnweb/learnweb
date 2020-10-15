package de.l3s.maintenance.users;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

public class UsersAddMissingProfilePictures {
    private static final Logger log = LogManager.getLogger(UsersAddMissingProfilePictures.class);

    private final Learnweb learnweb;
    private final UserManager um;

    public UsersAddMissingProfilePictures() throws Exception {

        learnweb = Learnweb.createInstance();
        um = learnweb.getUserManager();

        deleteMissingAvatars();
        //setDefaultAvatars();

        learnweb.onDestroy();
    }

    @SuppressWarnings("unused")
    private void setDefaultAvatars() throws SQLException, IOException {
        List<User> users = um.getUsers();
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
    private void deleteMissingAvatars() throws SQLException, IOException {
        List<User> users = um.getUsers();
        for (User user : users) {
            File file = user.getImageFile();
            if (null != file && !file.exists()) {
                log.debug("Update user: " + user);

                user.setImageFileId(0);
                user.save();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //   System.exit(-1);

        new UsersAddMissingProfilePictures();
    }

}

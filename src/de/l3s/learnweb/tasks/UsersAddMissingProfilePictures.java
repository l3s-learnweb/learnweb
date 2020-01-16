package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

public class UsersAddMissingProfilePictures
{

    public static void main(String[] args) throws Exception
    {
        //   System.exit(-1);

        new UsersAddMissingProfilePictures();
    }

    private final static Logger log = Logger.getLogger(IndexFakeNews.class);

    private Learnweb learnweb;

    private UserManager um;

    public UsersAddMissingProfilePictures() throws Exception
    {

        learnweb = Learnweb.createInstance();
        um = learnweb.getUserManager();

        deleteMissingAvatars();
        //setDefaultAvatars();

        learnweb.onDestroy();
    }

    @SuppressWarnings("unused")
    private void setDefaultAvatars() throws SQLException, IOException
    {
        List<User> users = um.getUsers();
        for(User user : users)
        {
            File file = user.getImageFile();
            if(null == file || !file.exists())
            {
                log.debug("Update user: " + user);

                InputStream is = user.getDefaultImageIS();
                if(is != null)
                {
                    user.setImage(is);
                    user.save();
                }

            }
        }
    }

    /**
     * deletes avatars that were created on elsewhere and aren't present on the server
     */
    private void deleteMissingAvatars() throws SQLException, IOException
    {
        List<User> users = um.getUsers();
        for(User user : users)
        {
            File file = user.getImageFile();
            if(null != file && !file.exists())
            {
                log.debug("Update user: " + user);

                user.setImageFileId(0);
                user.save();
            }
        }
    }

}

package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

/**
 * Find webpage resources that have no thumbnail and create it
 *
 * @author Kemkes
 *
 */
public class ForceEmailValidation
{
    private static Logger log = Logger.getLogger(ForceEmailValidation.class);

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws SQLException, MalformedURLException, IOException, ClassNotFoundException
    {
        Learnweb learnweb = Learnweb.createInstance(null);
        UserManager um = learnweb.getUserManager();

        /*
        String query = "SELECT distinct s.user_id  FROM `lw_user_course` s " +
                "left join lw_user_course t on s.user_id = t.user_id and t.`course_id` = 1250 " +
                "WHERE s.`course_id` IN (1338,1348,1349) and t.user_id is null";
        */

        String query = "SELECT user_id FROM `lw_user` WHERE `email` LIKE '%uni.au.dk' AND `organisation_id` !=478 AND `phone` LIKE '2%'";

        log.debug("start");

        ResultSet rs = learnweb.getConnection().createStatement().executeQuery(query);

        while(rs.next())
        {
            User user = um.getUser(rs.getInt(1));

            /*
            // force creation of mail validation token
            String mailBackup = user.getEmail();
            user.setEmail("");
            user.setEmail(mailBackup);
            */

            user.setEmail(user.getStudentId() + "@post.au.dk");
            log.debug(user.getEmail());
            user.save();
            user.sendEmailConfirmation();
        }

        log.debug("done");

        learnweb.onDestroy();
    }

}

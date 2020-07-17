package de.l3s.maintenance.users;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

/**
 * Resent the email authentication link for selected users.
 *
 * @author Philipp Kemkes
 */
public class ForceEmailValidation {
    private static final Logger log = LogManager.getLogger(ForceEmailValidation.class);

    /**
     *
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        Learnweb learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");
        UserManager um = learnweb.getUserManager();
        if (!Learnweb.getInstance().getServerUrl().equals("https://learnweb.l3s.uni-hannover.de")) {
            throw new RuntimeException("make sure the server url is correct since it is used in the validation mail");
        }

        String query = "SELECT * FROM `lw_user` WHERE `is_email_confirmed` = 0 and organisation_id != 478 and email != '' ORDER BY `lw_user`.`registration_date` DESC ";

        log.debug("start");

        ResultSet rs = learnweb.getConnection().createStatement().executeQuery(query);

        while (rs.next()) {
            User user = um.getUser(rs.getInt(1));

            /*
            // force creation of mail validation token
            String mailBackup = user.getEmail();
            user.setEmail("");
            user.setEmail(mailBackup);
            */

            log.debug(user.getEmail());
            user.sendEmailConfirmation();
        }

        log.debug("done");

        learnweb.onDestroy();
    }
}

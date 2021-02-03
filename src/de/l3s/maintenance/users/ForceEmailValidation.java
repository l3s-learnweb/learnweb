package de.l3s.maintenance.users;

import java.sql.ResultSet;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Resent the email authentication link for selected users.
 *
 * @author Philipp Kemkes
 */
public class ForceEmailValidation extends MaintenanceTask {

    @Override
    protected void run(final boolean dryRun) throws Exception {
        UserManager um = getLearnweb().getUserManager();

        String query = "SELECT * FROM `lw_user` WHERE `is_email_confirmed` = 0 and organisation_id != 478 and email != '' ORDER BY `lw_user`.`registration_date` DESC ";

        ResultSet rs = getLearnweb().getConnection().createStatement().executeQuery(query);
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
    }

    public static void main(String[] args) {
        new ForceEmailValidation().start(args);
    }
}

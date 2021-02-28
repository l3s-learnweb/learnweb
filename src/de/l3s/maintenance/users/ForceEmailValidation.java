package de.l3s.maintenance.users;

import java.util.List;

import org.jdbi.v3.core.Handle;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Resent the email authentication link for selected users.
 *
 * @author Philipp Kemkes
 */
public class ForceEmailValidation extends MaintenanceTask {

    @Override
    protected void run(final boolean dryRun) {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            List<User> users = handle.select("SELECT * FROM lw_user WHERE is_email_confirmed = 0 and organisation_id != 478 and email != '' "
                + "ORDER BY registration_date DESC").map(new UserDao.UserMapper()).list();

            for (User user : users) {
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
    }

    public static void main(String[] args) {
        new ForceEmailValidation().start(args);
    }
}

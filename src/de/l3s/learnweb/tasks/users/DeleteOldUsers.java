package de.l3s.learnweb.tasks.users;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

/**
 * Deletes users that haven't logged in for a long time
 *
 * @author Kemkes
 *
 */
public class DeleteOldUsers
{
    private static Logger log = Logger.getLogger(DeleteOldUsers.class);

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException
    {
        log.debug("start");

        Learnweb learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");

        try
        {
            // just to make sure that SOLR is connected reindex a random resource
            ResourceManager rm = learnweb.getResourceManager();
            rm.setReindexMode(true);
            learnweb.getSolrClient().reIndexResource(rm.getResource(2054));

            int configYears = 5; // delete users that didn't login for more than defined number of years
            deleteUsersThatHaventLoggedInForYears(learnweb, configYears, 478);

        }
        catch(Throwable e)
        {
            log.fatal("fatel error", e);
        }
        finally
        {
            learnweb.onDestroy();
        }
    }

    /**
     * Deletes users that didn't login for more than defined number of years.
     * Use this method only if you know what you are doing.
     * It will also delete resources a user has created in public groups. Thus this call might affect other users.
     *
     * @param learnweb
     * @param configYears number of years a user has to be inactive to be deleted
     * @param organisationId the organization from which inactive users are deleted
     * @throws Throwable
     */
    private static void deleteUsersThatHaventLoggedInForYears(Learnweb learnweb, int configYears, int organisationId) throws Throwable
    {
        log.info("deleteUsersThatHaventLoggedInForYears() - Start");
        UserManager um = learnweb.getUserManager();

        Instant now = Instant.now();
        Instant deadline = now.minus(configYears * 365, ChronoUnit.DAYS);

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_user` WHERE organisation_id = ? AND is_moderator = 0 AND is_admin = 0 AND `registration_date` < ?");
        select.setInt(1, organisationId);
        select.setTimestamp(2, Timestamp.from(deadline));
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            int userId = rs.getInt(1);
            Optional<Instant> lastLogin = um.getLastLoginDate(userId);
            log.debug(userId + " - " + lastLogin);

            if(lastLogin.isPresent() && deadline.isBefore(lastLogin.get()))
            {
                log.debug("ignore");
                continue;
            }
            User user = um.getUser(userId);

            if(user.getUsername().startsWith("Anonym"))
            {
                log.debug("ignore2");
                continue;
            }

            log.debug("Delete: " + user.getUsername() + "; " + user.getRegistrationDate() + "; " + user.getEmail() + "; " + user.isModerator());

            um.deleteUserHard(user);
        }

        log.info("deleteUsersThatHaventLoggedInForYears() - End");
    }

    private static void deleteAbandonedResources(Learnweb learnweb)
    {

        //SELECT * FROM `lw_resource` r LEFT JOIN lw_group g USING(`group_id`) WHERE r.group_id != 0 AND r.deleted = 0 AND g.group_id IS NULL
    }

    private static void deleteAbandonedGroups(Learnweb learnweb)
    {

    }

    // find problems
    // users who belong to a different organization then their courses
    //SELECT * FROM `lw_course` c JOIN lw_user_course USING(course_id) JOIN lw_user u USING(user_id) WHERE c.`organisation_id` != u.`organisation_id` ORDER BY `course_id` DESC

}

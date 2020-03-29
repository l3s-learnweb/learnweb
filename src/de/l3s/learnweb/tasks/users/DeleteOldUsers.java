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
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

/**
 * Deletes users that haven't logged in for a long time
 *
 * @author Kemkes
 *
 */
@SuppressWarnings("unused")
public class DeleteOldUsers
{
    private static Logger log = Logger.getLogger(DeleteOldUsers.class);
    private static Learnweb learnweb;

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

        learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");

        try
        {
            // just to make sure that SOLR is connected reindex a random resource
            ResourceManager rm = learnweb.getResourceManager();
            rm.setReindexMode(true);
            learnweb.getSolrClient().reIndexResource(rm.getResource(200233));

            //deleteUsersWhoHaventLoggedInForYears(4, 478); // delete users that didn't login for more than 4 years from the public organization
            /*
            deleteUsersWhoHaveBeenSoftDeleted(1);
            deleteAbandonedGroups();
            deleteAbandonedResources();
            */
            deleteAlmostAbandonedGroups();

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
     * Hard deletes users who have already been soft deleted. The method deletes only users who haven't logged in within the defined number of years.
     *
     * @param configYears
     * @throws Throwable
     */
    private static void deleteUsersWhoHaveBeenSoftDeleted(int configYears) throws Throwable
    {
        log.info("deleteUsersWhoHaveBeenSoftDeleted() - Start");
        UserManager um = learnweb.getUserManager();

        Instant now = Instant.now();
        Instant deadline = now.minus(configYears * 365, ChronoUnit.DAYS);

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_user` WHERE deleted = 1");
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            int userId = rs.getInt(1);
            Optional<Instant> lastLogin = um.getLastLoginDate(userId);

            User user = um.getUser(userId);

            if(lastLogin.isPresent() && deadline.isBefore(lastLogin.get()))
            {
                log.debug("Ignore active user: " + user + "; login=" + lastLogin);
                continue;
            }
            log.debug("Delete: " + user.getRealUsername() + "; userId=" + user.getId() + "; registration=" + user.getRegistrationDate() + "; login=" + lastLogin + "; mail=" + user.getEmail() + "; " + user.isModerator());

            um.deleteUserHard(user);
        }

        log.info("deleteUsersWhoHaveBeenSoftDeleted() - End");
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
    private static void deleteUsersWhoHaventLoggedInForYears(int configYears, int organisationId) throws Throwable
    {
        log.info("deleteUsersThatHaventLoggedInForYears() - Start");
        UserManager um = learnweb.getUserManager();

        Instant now = Instant.now();
        Instant deadline = now.minus(configYears * 365, ChronoUnit.DAYS);

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_user` WHERE (organisation_id = ? AND is_moderator = 0 AND is_admin = 0 AND `registration_date` < ?) OR deleted = 1");
        select.setInt(1, organisationId);
        select.setTimestamp(2, Timestamp.from(deadline));
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            int userId = rs.getInt(1);
            Optional<Instant> lastLogin = um.getLastLoginDate(userId);

            User user = um.getUser(userId);

            if(lastLogin.isPresent() && deadline.isBefore(lastLogin.get()))
            {
                log.debug("Ignore active user: " + user + "; login=" + lastLogin);
                continue;
            }

            if(user.getUsername().startsWith("Anonym"))
            {
                log.debug("Ignore anonymous user: " + userId);
                continue;
            }

            log.debug("Delete: " + user.getUsername() + "; registration=" + user.getRegistrationDate() + "; login=" + lastLogin + "; mail=" + user.getEmail() + "; " + user.isModerator());

            um.deleteUserHard(user);
        }

        log.info("deleteUsersThatHaventLoggedInForYears() - End");
    }

    private static void deleteAbandonedResources() throws SQLException
    {

        ResourceManager rm = learnweb.getResourceManager();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT resource_id FROM `lw_resource` r LEFT JOIN lw_group g USING(`group_id`) WHERE (r.group_id != 0 AND g.group_id IS NULL) OR r.deleted = 1"))
        {

            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                log.debug("Delete abonded resource: " + rs.getInt(1));
                rm.deleteResourceHard(rs.getInt(1));
            }
        }

        // remove references to deleted resources
        String[] tables = { "lw_resource_history", "lw_resource_langlevel", "lw_resource_purpose", "lw_resource_rating", "lw_resource_tag", "lw_thumb", "lw_transcript_actions", "lw_glossary_resource",
                "lw_transcript_selections", "lw_transcript_summary", "ted_transcripts_paragraphs" };

        for(String table : tables)
        {
            try(PreparedStatement delete = learnweb.getConnection().prepareStatement("delete d FROM `" + table + "` d LEFT JOIN lw_resource r USING(resource_id) WHERE r.resource_id is null"))
            {
                int numRowsAffected = delete.executeUpdate();
                log.debug("Deleted " + numRowsAffected + " rows from " + table);
            }
        }
    }

    private static void deleteAlmostAbandonedGroups() throws Exception
    {
        GroupManager gm = learnweb.getGroupManager();
        /*
        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT g.group_id FROM `lw_group` g LEFT JOIN lw_group_user u USING(group_id) LEFT JOIN lw_resource r USING(group_id) where course_id in (485,891) and YEAR(creation_time) < year(now())-1 GROUP BY g.group_id HAVING count(u.user_id) <= 2 AND count(r.resource_id) <=2 ORDER BY `g`.`creation_time` DESC "))
          */
        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT * FROM `lw_group` WHERE `course_id` = 891"))

        {

            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                Group group = gm.getGroupById(rs.getInt(1));

                log.debug("Delete: " + group + "; resources: " + group.getResourcesCount());
                if(group.getResourcesCount() > 2)
                {
                    log.debug("confirm");

                }
                group.deleteHard();
            }
        }
    }

    private static void deleteAbandonedGroups() throws Exception
    {
        GroupManager gm = learnweb.getGroupManager();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT group_id FROM `lw_group` LEFT JOIN lw_group_user u USING(group_id) WHERE (YEAR(creation_time) < year(now())-1 AND u.group_id IS NULL and course_id != 891) OR deleted = 1"))
        {

            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                Group group = gm.getGroupById(rs.getInt(1));

                log.debug("Delete: " + group + "; resources: " + group.getResourcesCount());
                if(group.getResourcesCount() > 1)
                {
                    log.debug("confirm");

                }
                group.deleteHard();
            }
        }

        // remove references to deleted resources
        String[] tables = { "lw_link", "lw_forum_topic", "lw_group_folder", "lw_group_user", "lw_user_log" };

        for(String table : tables)
        {
            try(PreparedStatement delete = learnweb.getConnection().prepareStatement("delete d FROM `" + table + "` d LEFT JOIN lw_group g USING(group_id) WHERE d.group_id != 0 AND g.group_id is null"))
            {
                int numRowsAffected = delete.executeUpdate();
                log.debug("Deleted " + numRowsAffected + " rows from " + table);
            }
        }
    }

    // find problems
    // users who belong to a different organization then their courses
    //SELECT * FROM `lw_course` c JOIN lw_user_course USING(course_id) JOIN lw_user u USING(user_id) WHERE c.`organisation_id` != u.`organisation_id` ORDER BY `course_id` DESC

}

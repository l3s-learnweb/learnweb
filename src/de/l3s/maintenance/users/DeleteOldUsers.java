package de.l3s.maintenance.users;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Deletes users that haven't logged in for a long time.
 *
 * @author Philipp Kemkes
 */
public class DeleteOldUsers extends MaintenanceTask {

    private UserManager userManager;
    private GroupManager groupManager;
    private ResourceManager resourceManager;

    @Override
    protected void init() throws Exception {
        userManager = getLearnweb().getUserManager();
        groupManager = getLearnweb().getGroupManager();
        resourceManager = getLearnweb().getResourceManager();
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        // just to make sure that SOLR is connected reindex a random resource
        ResourceManager rm = getLearnweb().getResourceManager();
        rm.setReindexMode(true);
        getLearnweb().getSolrClient().reIndexResource(rm.getResource(200233));

        //deleteUsersWhoHaventLoggedInForYears(4, 478); // delete users that didn't login for more than 4 years from the public organization
        /*
        deleteUsersWhoHaveBeenSoftDeleted(1);
        deleteAbandonedGroups();
        deleteAbandonedResources();
        */
    }

    /**
     * Hard deletes users who have already been soft deleted. The method deletes only users who haven't logged in within the defined number of years.
     */
    private void deleteUsersWhoHaveBeenSoftDeleted(int configYears) throws Throwable {
        log.info("deleteUsersWhoHaveBeenSoftDeleted() - Start");

        Instant now = Instant.now();
        Instant deadline = now.minus(configYears * 365L, ChronoUnit.DAYS);

        PreparedStatement select = getLearnweb().getConnection().prepareStatement("SELECT * FROM `lw_user` WHERE deleted = 1");
        ResultSet rs = select.executeQuery();

        while (rs.next()) {
            int userId = rs.getInt(1);
            Optional<Instant> lastLogin = userManager.getLastLoginDate(userId);

            User user = userManager.getUser(userId);
            if (user.isModerator() || user.isAdmin()) {
                log.debug("Ignore moderator user: {}", user);
                continue;
            }

            if (lastLogin.isPresent() && deadline.isBefore(lastLogin.get())) {
                log.debug("Ignore active user: {}; login={}", user, lastLogin);
                continue;
            }
            log.debug("Delete: {}; userId={}; registration={}; login={}; mail={}; {}", user.getRealUsername(), user.getId(), user.getRegistrationDate(), lastLogin, user.getEmail(), user.isModerator());

            userManager.deleteUserHard(user);
        }

        log.info("deleteUsersWhoHaveBeenSoftDeleted() - End");
    }

    /**
     * Deletes users that didn't login for more than defined number of years.
     * Use this method only if you know what you are doing.
     * It will also delete resources a user has created in public groups. Thus this call might affect other users.
     *
     * @param configYears number of years a user has to be inactive to be deleted
     * @param organisationId the organization from which inactive users are deleted
     */
    private void deleteUsersWhoHaventLoggedInForYears(int configYears, int organisationId) throws Throwable {
        log.info("deleteUsersThatHaventLoggedInForYears() - Start");
        Instant now = Instant.now();
        Instant deadline = now.minus(configYears * 365L, ChronoUnit.DAYS);

        PreparedStatement select = getLearnweb().getConnection().prepareStatement("SELECT * FROM `lw_user` WHERE (organisation_id = ? AND is_moderator = 0 AND is_admin = 0 AND `registration_date` < ?) OR deleted = 1");
        select.setInt(1, organisationId);
        select.setTimestamp(2, Timestamp.from(deadline));

        ResultSet rs = select.executeQuery();

        while (rs.next()) {
            int userId = rs.getInt(1);
            Optional<Instant> lastLogin = userManager.getLastLoginDate(userId);

            User user = userManager.getUser(userId);
            if (user.isModerator() || user.isAdmin()) {
                log.debug("Ignore moderator user: {}", user);
                continue;
            }

            if (lastLogin.isPresent() && deadline.isBefore(lastLogin.get())) {
                log.debug("Ignore active user: {}; login={}", user, lastLogin);
                continue;
            }

            log.debug("Delete: {}; registration={}; login={}; mail={}; {}", user.getUsername(), user.getRegistrationDate(), lastLogin, user.getEmail(), user.isModerator());

            userManager.deleteUserHard(user);
        }
        log.info("deleteUsersThatHaventLoggedInForYears() - End");
    }

    private void deleteAbandonedResources() throws SQLException {
        try (PreparedStatement select = getLearnweb().getConnection().prepareStatement(
            "SELECT resource_id FROM `lw_resource` r LEFT JOIN lw_group g USING(`group_id`) "
                + "WHERE (r.group_id != 0 AND g.group_id IS NULL) OR r.deleted = 1")) {

            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                log.debug("Delete abonded resource: {}", rs.getInt(1));
                resourceManager.deleteResourceHard(rs.getInt(1));
            }
        }

        // remove references to deleted resources
        String[] tables = {"lw_resource_history", "lw_resource_rating", "lw_resource_tag", "lw_thumb",
            "lw_transcript_actions", "lw_glossary_resource", "lw_transcript_selections", "lw_transcript_summary", "learnweb_large.ted_transcripts_paragraphs"};

        for (String table : tables) {
            try (PreparedStatement delete = getLearnweb().getConnection().prepareStatement("delete d FROM `" + table + "` d LEFT JOIN lw_resource r USING(resource_id) WHERE r.resource_id is null")) {
                int numRowsAffected = delete.executeUpdate();
                log.debug("Deleted {} rows from {}", numRowsAffected, table);
            }
        }
    }

    private void deleteAlmostAbandonedGroups() throws Exception {
        try (PreparedStatement select = getLearnweb().getConnection().prepareStatement(
            "SELECT g.group_id FROM `lw_group` g LEFT JOIN lw_group_user u USING(group_id) LEFT JOIN lw_resource r USING(group_id) "
                + "WHERE course_id in (485) and YEAR(creation_time) < year(now())-1 GROUP BY g.group_id, g.creation_time HAVING count(u.user_id) <= 2 "
                + "AND count(r.resource_id) <=2 ORDER BY g.creation_time DESC ")) {
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                Group group = groupManager.getGroupById(rs.getInt(1));

                log.debug("Delete: {}; resources: {}", group, group.getResourcesCount());
                if (group.getResourcesCount() > 2) {
                    log.debug("confirm");

                }
                group.deleteHard();
            }
        }
    }

    private void deleteAbandonedGroups() throws Exception {
        try (PreparedStatement select = getLearnweb().getConnection().prepareStatement(
            "SELECT group_id FROM `lw_group` LEFT JOIN lw_group_user u USING(group_id) "
                + "WHERE (YEAR(creation_time) < year(now())-1 AND u.group_id IS NULL) OR deleted = 1")) {

            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                Group group = groupManager.getGroupById(rs.getInt(1));

                log.debug("Delete: {}; resources: {}", group, group.getResourcesCount());
                if (group.getResourcesCount() > 1) {
                    log.debug("confirm");

                }
                group.deleteHard();
            }
        }

        // remove references to deleted groups
        String[] tables = {"lw_forum_topic", "lw_group_folder", "lw_group_user", "lw_user_log"};

        for (String table : tables) {
            try (PreparedStatement delete = getLearnweb().getConnection().prepareStatement(
                "delete d FROM `" + table + "` d LEFT JOIN lw_group g USING(group_id) WHERE d.group_id != 0 AND g.group_id is null")) {
                int numRowsAffected = delete.executeUpdate();
                log.debug("Deleted {} rows from {}", numRowsAffected, table);
            }
        }
    }

    // find problems
    // users who belong to a different organization then their courses
    //SELECT * FROM `lw_course` c JOIN lw_user_course USING(course_id) JOIN lw_user u USING(user_id) WHERE c.`organisation_id` != u.`organisation_id` ORDER BY `course_id` DESC

    public static void main(String[] args) {
        new DeleteOldUsers().start(args);
    }
}

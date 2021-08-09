package de.l3s.maintenance.users;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Deletes users that haven't logged in for a long time.
 *
 * @author Philipp Kemkes
 */
public class DeleteOldUsers extends MaintenanceTask {

    private UserDao userDao;
    private GroupDao groupDao;
    private ResourceDao resourceDao;

    @Override
    protected void init() {
        userDao = getLearnweb().getDaoProvider().getUserDao();
        groupDao = getLearnweb().getDaoProvider().getGroupDao();
        resourceDao = getLearnweb().getDaoProvider().getResourceDao();
    }

    @Override
    protected void run(final boolean dryRun) {
        // just to make sure that SOLR is connected reindex a random resource
        getLearnweb().getSolrClient().reIndexResource(resourceDao.findByIdOrElseThrow(200233));

        //deleteUsersWhoHaventLoggedInForYears(4, 478); // delete users that didn't login for more than 4 years from the public organisation
        /*
        deleteUsersWhoHaveBeenSoftDeleted(1);
        deleteAbandonedGroups();
        deleteAbandonedResources();
        */
    }

    /**
     * Hard deletes users who have already been soft deleted. The method deletes only users who haven't logged in within the defined number of years.
     */
    public void deleteUsersWhoHaveBeenSoftDeleted(int configYears) {
        log.info("deleteUsersWhoHaveBeenSoftDeleted() - Start");

        LocalDateTime deadline = LocalDateTime.now().minus(configYears * 365L, ChronoUnit.DAYS);

        try (Handle handle = getLearnweb().openJdbiHandle()) {
            List<User> users = handle.select("SELECT * FROM lw_user WHERE deleted = 1").map(new UserDao.UserMapper()).list();

            for (User user : users) {
                Optional<LocalDateTime> lastLogin = userDao.findLastLoginDate(user.getId());

                if (user.isModerator() || user.isAdmin()) {
                    log.debug("Ignore moderator user: {}", user);
                    continue;
                }

                if (lastLogin.isPresent() && deadline.isBefore(lastLogin.get())) {
                    log.debug("Ignore active user: {}; login={}", user, lastLogin);
                    continue;
                }
                log.debug("Delete: {}; userId={}; registration={}; login={}; mail={}; {}",
                    user.getRealUsername(), user.getId(), user.getCreatedAt(), lastLogin, user.getEmail(), user.isModerator());

                userDao.deleteHard(user);
            }
        }

        log.info("deleteUsersWhoHaveBeenSoftDeleted() - End");
    }

    /**
     * Deletes users that didn't login for more than defined number of years.
     * Use this method only if you know what you are doing.
     * It will also delete resources a user has created in public groups. Thus this call might affect other users.
     *
     * @param configYears number of years a user has to be inactive to be deleted
     * @param organisationId the organisation from which inactive users are deleted
     */
    public void deleteUsersWhoHaveNotLoggedInForYears(int configYears, int organisationId) {
        log.info("deleteUsersWhoHaveNotLoggedInForYears() - Start");
        LocalDateTime deadline = LocalDateTime.now().minus(configYears * 365L, ChronoUnit.DAYS);

        try (Handle handle = getLearnweb().openJdbiHandle()) {
            List<User> users = handle.select("SELECT * FROM lw_user WHERE (organisation_id = ? AND is_moderator = 0 AND is_admin = 0 AND "
                + "registration_date < ?) OR deleted = 1", organisationId, deadline).map(new UserDao.UserMapper()).list();

            for (User user : users) {
                Optional<LocalDateTime> lastLogin = userDao.findLastLoginDate(user.getId());

                if (user.isModerator() || user.isAdmin()) {
                    log.debug("Ignore moderator user: {}", user);
                    continue;
                }

                if (lastLogin.isPresent() && deadline.isBefore(lastLogin.get())) {
                    log.debug("Ignore active user: {}; login={}", user, lastLogin);
                    continue;
                }

                log.debug("Delete: {}; registration={}; login={}; mail={}; {}",
                    user.getUsername(), user.getCreatedAt(), lastLogin, user.getEmail(), user.isModerator());

                userDao.deleteHard(user);
            }
        }
        log.info("deleteUsersWhoHaveNotLoggedInForYears() - End");
    }

    public void deleteAbandonedResources() {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            List<Integer> resourceIds = handle.select("SELECT resource_id FROM lw_resource r LEFT JOIN lw_group g USING(group_id) "
                + "WHERE (r.group_id != 0 AND g.group_id IS NULL) OR r.deleted = 1").mapTo(Integer.class).list();

            for (Integer resourceId : resourceIds) {
                log.debug("Delete abandoned resource: {}", resourceId);
                resourceDao.findById(resourceId).ifPresent(resource -> resourceDao.deleteHard(resource));
            }
        }
    }

    public void deleteAlmostAbandonedGroups() {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            List<Group> groups = handle.select("SELECT g.* FROM lw_group g LEFT JOIN lw_group_user u USING(group_id) LEFT JOIN lw_resource r USING(group_id) "
                + "WHERE course_id in (485) and YEAR(g.created_at) < year(now())-1 GROUP BY g.group_id, g.created_at HAVING count(u.user_id) <= 2 "
                + "AND count(r.resource_id) <=2 ORDER BY g.created_at DESC ").map(new GroupDao.GroupMapper()).list();

            for (Group group : groups) {
                log.debug("Delete: {}; resources: {}", group, group.getResourcesCount());
                if (group.getResourcesCount() > 2) {
                    log.debug("confirm");

                }
                groupDao.deleteHard(group);
            }
        }
    }

    public void deleteAbandonedGroups() {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            List<Group> groups = handle.select("SELECT * FROM lw_group LEFT JOIN lw_group_user u USING(group_id) "
                + "WHERE (YEAR(created_at) < year(now())-1 AND u.group_id IS NULL) OR deleted = 1").map(new GroupDao.GroupMapper()).list();

            for (Group group : groups) {
                log.debug("Delete: {}; resources: {}", group, group.getResourcesCount());
                if (group.getResourcesCount() > 1) {
                    log.debug("confirm");
                }
                groupDao.deleteHard(group);
            }
        }
    }

    // find problems
    // users who belong to a different organisation then their courses
    // SELECT * FROM lw_course c JOIN lw_course_user USING(course_id) JOIN lw_user u USING(user_id) WHERE c.organisation_id != u.organisation_id

    public static void main(String[] args) {
        new DeleteOldUsers().start(args);
    }
}

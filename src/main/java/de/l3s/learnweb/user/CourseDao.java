package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(CourseDao.CourseMapper.class)
public interface CourseDao extends SqlObject, Serializable {
    ICache<Course> cache = new Cache<>(10000);

    default Optional<Course> findById(int courseId) {
        return Optional.ofNullable(cache.get(courseId))
            .or(() -> getHandle().select("SELECT * FROM lw_course g WHERE course_id = ?", courseId).mapTo(Course.class).findOne());
    }

    default Course findByIdOrElseThrow(int courseId) {
        return findById(courseId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_group_description"));
    }

    /**
     * Returns the course with the specified wizard parameter.
     */
    @SqlQuery("SELECT * FROM lw_course WHERE reg_type != 'closed' AND reg_wizard = ?")
    Optional<Course> findByWizard(String wizard);

    @SqlQuery("SELECT * FROM lw_course ORDER BY title")
    List<Course> findAll();

    @SqlQuery("SELECT * FROM lw_course WHERE reg_type = ?")
    List<Course> findByRegistrationType(Course.RegistrationType registrationType);

    @SqlQuery("SELECT * FROM lw_course WHERE organisation_id = ? ORDER BY title")
    List<Course> findByOrganisationId(int organisationId);

    @SqlQuery("SELECT c.* FROM lw_course c JOIN lw_course_user uc USING (course_id) WHERE uc.user_id = ? ORDER BY c.title")
    List<Course> findByUserId(int userId);

    /**
     * Add a user to a course.
     */
    @SqlUpdate("INSERT INTO lw_course_user (course_id, user_id) VALUES (?, ?)")
    void insertUser(Course course, User user);

    @SqlUpdate("DELETE FROM lw_course_user WHERE course_id = ? AND user_id = ?")
    void deleteUser(Course course, User user);

    default void save(Course course) {
        course.setUpdatedAt(SqlHelper.now());
        if (course.getCreatedAt() == null) {
            course.setCreatedAt(course.getUpdatedAt());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("course_id", SqlHelper.toNullable(course.getId()));
        params.put("title", course.getTitle());
        params.put("organisation_id", course.getOrganisationId());
        params.put("default_group_id", SqlHelper.toNullable(course.getDefaultGroupId()));
        params.put("reg_type", course.getRegistrationType().name());
        params.put("reg_wizard", SqlHelper.toNullable(course.getRegistrationWizard()));
        params.put("reg_description", SqlHelper.toNullable(course.getRegistrationDescription()));
        params.put("reg_icon_file_id", SqlHelper.toNullable(course.getRegistrationIconFileId()));
        params.put("next_x_users_become_moderator", course.getNextXUsersBecomeModerator());
        params.put("welcome_message", SqlHelper.toNullable(course.getWelcomeMessage()));
        SqlHelper.setBitSet(params, Course.Option.values().length, "options_field", course.getOptions());
        params.put("updated_at", course.getUpdatedAt());
        params.put("created_at", course.getCreatedAt());

        Optional<Integer> courseId = SqlHelper.handleSave(getHandle(), "lw_course", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (courseId.isPresent() && courseId.get() != 0) {
            course.setId(courseId.get());
            cache.put(course);
        }
    }

    default List<User> deleteHard(Course course, boolean force) {
        GroupDao groupDao = getHandle().attach(GroupDao.class);
        for (Group group : groupDao.findAllByCourseId(course.getId())) {
            groupDao.deleteHard(group);
        }

        UserDao userDao = getHandle().attach(UserDao.class);
        if (!force && userDao.countByCourseId(course.getId()) > 0) {
            throw new IllegalArgumentException("The course can't be deleted, remove all members first");
        }

        List<User> undeletedUsers = new LinkedList<>(); // users that can't be deleted because they are member of other courses
        for (User user : userDao.findAllByCourseId(course.getId())) {
            if (userDao.countCoursesByUserId(user.getId()) > 1 || user.isAdmin()) {
                undeletedUsers.add(user);
            } else {
                userDao.deleteHard(user);
            }
        }

        getHandle().execute("DELETE FROM lw_course WHERE course_id = ?", course);

        cache.remove(course.getId());
        return undeletedUsers;
    }

    /**
     * The personal information of all course members will be removed, and they won't be able to login anymore. Users who are also member of other
     * courses are not affected.
     *
     * @return The users who where not anonymize because they are member of other courses.
     */
    default List<User> anonymize(Course course) {
        // disable registration wizard
        course.setRegistrationType(Course.RegistrationType.CLOSED);
        course.setRegistrationWizard(null);
        save(course);

        // anonymize users
        UserDao userDao = getHandle().attach(UserDao.class);
        List<User> undeletedUsers = new LinkedList<>(); // users that can't be deleted because they are member of other courses
        for (User user : course.getMembers()) {
            if (user.getCourses().size() > 1) {
                undeletedUsers.add(user);
            } else {
                userDao.anonymize(user);
            }
        }
        return undeletedUsers;
    }

    class CourseMapper implements RowMapper<Course> {
        @Override
        public Course map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Course course = cache.get(rs.getInt("course_id"));

            if (course == null) {
                course = new Course();
                course.setId(rs.getInt("course_id"));
                course.setOrganisationId(rs.getInt("organisation_id"));
                course.setTitle(rs.getString("title"));
                course.setDefaultGroupId(rs.getInt("default_group_id"));
                course.setRegistrationType(Course.RegistrationType.valueOf(rs.getString("reg_type")));
                course.setRegistrationWizard(rs.getString("reg_wizard"));
                course.setRegistrationDescription(rs.getString("reg_description"));
                course.setRegistrationIconFileId(rs.getInt("reg_icon_file_id"));
                course.setNextXUsersBecomeModerator(rs.getInt("next_x_users_become_moderator"));
                course.setWelcomeMessage(rs.getString("welcome_message"));
                course.setOptions(SqlHelper.getBitSet(rs, Course.Option.values().length, "options_field"));
                course.setUpdatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("updated_at")));
                course.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
                cache.put(course);
            }
            return course;
        }
    }
}

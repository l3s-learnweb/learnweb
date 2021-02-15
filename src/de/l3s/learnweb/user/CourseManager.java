package de.l3s.learnweb.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.util.SqlHelper;

/**
 * DAO for the Course class.
 * Because there are only a few courses we keep them all in memory
 *
 * @author Philipp Kemkes
 */
public class CourseManager {
    private static final Logger log = LogManager.getLogger(CourseManager.class);

    protected static final int FIELDS = 1; // number of options_fieldX fields, increase if Course.Options has more than 64 values
    private final Learnweb learnweb;
    private final Map<Integer, Course> cache;

    public CourseManager(Learnweb learnweb) throws SQLException {
        this.learnweb = learnweb;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(80));
        this.resetCache();
    }

    public synchronized void resetCache() throws SQLException {
        cache.clear();

        // load all courses into cache
        try (Handle handle = learnweb.openHandle()) {
            handle.select("SELECT * FROM lw_course ORDER BY title").map(new CourseMapper()).forEach(course -> {
                cache.put(course.getId(), course);
            });
        }
    }

    /**
     * @return number of cached objects
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Get an Course by his id.
     *
     * @return null if not found
     */
    public Course getCourseById(int id) {
        return cache.get(id);
    }

    /**
     * Returns the course with the specified wizard parameter.
     *
     * @return null if no course was found
     */
    public Course getCourseByWizard(String wizardParam) {
        for (Course course : cache.values()) { // it's ok to iterate over the courses because we have only a few
            if (null != course.getWizardParam() && course.getWizardParam().equalsIgnoreCase(wizardParam)) {
                return course;
            }
        }
        return null;
    }

    /**
     * Returns a list of all Courses.
     *
     * @return The collection is unmodifiable
     */
    public Collection<Course> getCoursesAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    /**
     * @return The list is empty (but not null) if no courses were found
     */
    public List<Course> getCoursesByOrganisationId(int organisationId) {
        // it's ok to iterate over the courses because we have only a few
        return cache.values().stream().filter(c -> c.getOrganisationId() == organisationId).collect(Collectors.toList());
    }

    /**
     * @return The list is empty (but not null) if no courses were found
     * TODO: inefficient method, can be used joint query for better performance
     */
    public List<Course> getCoursesByUserId(int userId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT course_id FROM lw_user_course WHERE user_id = ?", userId)
                .map((rs, ctx) -> getCourseById(rs.getInt(1)))
                .list();
        }
    }

    /**
     * Saves the course to the database.
     * If the course is not yet stored at the database, a new record will be created and the returned course contains the new id.
     */
    protected synchronized Course save(Course course) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("course_id", course.getId() < 0 ? null : course.getId());
            params.put("title", course.getTitle());
            params.put("organisation_id", course.getOrganisationId());
            params.put("default_group_id", course.getDefaultGroupId());
            params.put("wizard_param", course.getWizardParam());
            params.put("next_x_users_become_moderator", course.getNextXUsersBecomeModerator());
            params.put("welcome_message", course.getWelcomeMessage());
            params.put("timestamp_creation", course.getCreationTimestamp());
            params.put("options_field1", course.getOptions()[0]);

            Optional<Integer> courseId = SqlHelper.generateInsertQuery(handle, "lw_course", params)
                .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

            if (courseId.isPresent()) {
                course.setId(courseId.get());
            } else if (course.getId() < 0) {
                throw new SQLException("database error: no id generated");
            }

            cache.put(course.getId(), course);
            return course;
        }
    }

    /**
     * The personal information of all course members will be removed and they won't be able to login anymore. Users who are also member of other
     * courses are not affected.
     *
     * @return The users who where not anonymize because they are member of other courses.
     */
    public List<User> anonymize(Course course) throws SQLException {
        // disable registration wizard
        course.setOption(Option.Users_Disable_wizard, true);
        save(course);

        // anonymize users
        List<User> undeletedUsers = new LinkedList<>(); // users that can't be deleted because they are member of other courses
        for (User user : course.getMembers()) {
            if (user.getCourses().size() > 1) {
                log.debug("Can't delete user: " + user);
                undeletedUsers.add(user);
            } else {
                learnweb.getUserManager().anonymize(user);
            }
        }
        return undeletedUsers;
    }

    public void delete(Course course) throws SQLException {
        deleteHard(course, false);
    }

    /**
     * All users and their resources will be deleted. Except for users who are also member of other courses.
     *
     * @param force If true the course is deleted even if it still contains users
     * @return The users who where not deleted because they are member of other courses.
     */
    public List<User> deleteHard(Course course, boolean force) throws SQLException {
        if (!force && course.getMemberCount() > 0) {
            throw new IllegalArgumentException("course can't be deleted, remove all members first");
        }

        UserManager userManager = learnweb.getUserManager();

        List<User> undeletedUsers = new LinkedList<>(); // users that can't be deleted because they are member of other courses
        for (User user : course.getMembers()) {
            if (user.getCourses().size() > 1 || user.isAdmin()) {
                log.debug("Can't delete user: " + user);
                undeletedUsers.add(user);
            } else {
                userManager.deleteUserHard(user);
            }
        }
        for (Group group : course.getGroups()) {
            if (group.getCourseId() != course.getId()) { // skip public groups
                continue;
            }
            group.deleteHard();
        }

        try (Handle handle = learnweb.openHandle()) {
            handle.execute("DELETE FROM `lw_user_course` WHERE course_id = ?", course.getId());
            handle.execute("DELETE FROM `lw_course` WHERE course_id = ?", course.getId());
        }

        cache.remove(course.getId());

        return undeletedUsers;
    }

    /**
     * Add a user to a course.
     */
    public void addUser(Course course, User user) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("INSERT INTO `lw_user_course` (`user_id` ,`course_id`) VALUES (?, ?)", user.getId(), course.getId());
        }
    }

    public void removeUser(Course course, User user) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("DELETE FROM `lw_user_course` WHERE `user_id` = ? AND `course_id` = ?", user.getId(), course.getId());
        }
    }

    /**
     * Overrides toString method to simplify output in templates.
     */
    private static class CoursesList extends LinkedList<Course> implements Comparable<CoursesList> {
        private static final long serialVersionUID = 8924683355506959050L;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Course course : this) {
                sb.append(course.getTitle());
                sb.append(", ");
            }
            if (sb.length() > 0) { // remove last comma
                sb.setLength(sb.length() - 2);
            }

            return sb.toString();
        }

        @Override
        public int compareTo(CoursesList other) {
            return toString().compareTo(other.toString());
        }
    }

    private static class CourseMapper implements RowMapper<Course> {
        @Override
        public Course map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Course course = new Course();
            course.setId(rs.getInt("course_id"));
            course.setOrganisationId(rs.getInt("organisation_id"));
            course.setTitle(rs.getString("title"));
            course.setDefaultGroupId(rs.getInt("default_group_id"));
            course.setWizardParam(rs.getString("wizard_param"));
            course.setNextXUsersBecomeModerator(rs.getInt("next_x_users_become_moderator"));
            course.setWelcomeMessage(rs.getString("welcome_message"));
            course.setCreationTimestamp(rs.getObject("timestamp_creation", LocalDateTime.class));

            long[] options = new long[FIELDS];
            for (int i = 0; i < FIELDS; i++) {
                options[i] = rs.getLong("options_field" + (i + 1));
            }
            course.setOptions(options);

            return course;
        }
    }
}

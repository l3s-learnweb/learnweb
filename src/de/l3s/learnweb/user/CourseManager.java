package de.l3s.learnweb.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.util.Sql;

/**
 * DAO for the Course class.
 * Because there are only a few courses we keep them all in memory
 *
 * @author Philipp
 *
 */
public class CourseManager
{
    protected final static int FIELDS = 1; // number of options_fieldX fields, increase if Course.Options has more than 64 values
    private static final String[] COLUMNS = { "course_id", "title", "organisation_id", "default_group_id", "wizard_param", "next_x_users_become_moderator", "welcome_message", "timestamp_creation", "options_field1" };
    private static final String SELECT = String.join(", ", COLUMNS);
    private static final String SAVE = Sql.getCreateStatement("lw_course", COLUMNS);
    private static final Logger log = Logger.getLogger(CourseManager.class);

    private Learnweb learnweb;
    private Map<Integer, Course> cache;

    public CourseManager(Learnweb learnweb) throws SQLException
    {
        super();
        this.learnweb = learnweb;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(80));
        this.resetCache();
    }

    public synchronized void resetCache() throws SQLException
    {
        cache.clear();

        // load all courses into cache
        try(ResultSet rs = learnweb.getConnection().createStatement().executeQuery("SELECT " + SELECT + " FROM lw_course ORDER BY title"))
        {
            while(rs.next())
            {
                Course course = createCourse(rs);
                cache.put(course.getId(), course);
            }
        }
    }

    private Course createCourse(ResultSet rs) throws SQLException
    {
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
        for(int i = 0; i < FIELDS;)
            options[i] = rs.getLong("options_field" + ++i);
        course.setOptions(options);

        return course;
    }

    /**
     *
     * @return number of cached objects
     */
    public int getCacheSize()
    {
        return cache.size();
    }

    /**
     * Get an Course by his id
     *
     * @param id
     * @return null if not found
     */
    public Course getCourseById(int id)
    {
        return cache.get(id);
    }

    /**
     * Returns the course with the specified wizard parameter
     *
     * @param wizardParam
     * @return null if no course was found
     */
    public Course getCourseByWizard(String wizardParam)
    {
        for(Course course : cache.values()) // it's ok to iterate over the courses because we have only a few
        {
            if(null != course.getWizardParam() && course.getWizardParam().equalsIgnoreCase(wizardParam))
                return course;
        }
        return null;
    }

    /**
     * Returns a list of all Courses
     *
     * @return The collection is unmodifiable
     */
    public Collection<Course> getCoursesAll()
    {
        return Collections.unmodifiableCollection(cache.values());
    }

    /**
     *
     * @param organisationId
     * @return The list is empty (but not null) if no courses were found
     */
    public List<Course> getCoursesByOrganisationId(int organisationId)
    {
        return cache.values().stream().filter(c -> c.getOrganisationId() == organisationId).collect(Collectors.toList());

        /*
        List<Course> courses = new CoursesList();


        for(Course course : cache.values()) // it's ok to iterate over the courses because we have only a few
        {
            if(course.getOrganisationId() == organisationId)
                courses.add(course);
        }

        return courses;
        */
    }

    /**
     *
     * @param userId
     * @return The list is empty (but not null) if no courses were found
     * @throws SQLException
     */
    public List<Course> getCoursesByUserId(int userId) throws SQLException
    {
        List<Course> courses = new CoursesList();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT course_id FROM lw_user_course WHERE user_id = ?"))
        {
            select.setInt(1, userId);
            ResultSet rs = select.executeQuery();
            while(rs.next())
                courses.add(getCourseById(rs.getInt(1)));

            return courses;
        }
    }

    /**
     * Saves the course to the database.
     * If the course is not yet stored at the database, a new record will be created and the returned course contains the new id.
     *
     * @param course
     * @return
     * @throws SQLException
     */
    protected synchronized Course save(Course course) throws SQLException
    {
        if(course.getId() < 0) // the course is not yet stored at the database
        {
            // TODO this is not necessary any more. Remove it and use auto increment of lw_course.course_id
            // we have to get a new id from the group manager
            Group group = new Group();
            group.setTitle(course.getTitle());
            group.setDescription("Course");

            learnweb.getGroupManager().save(group);
            course.setId(group.getId());
            group.delete();

            cache.put(course.getId(), course);
        }

        try(PreparedStatement save = learnweb.getConnection().prepareStatement(SAVE))
        {
            save.setInt(1, course.getId());
            save.setString(2, course.getTitle());
            save.setInt(3, course.getOrganisationId());
            save.setInt(4, course.getDefaultGroupId());
            save.setString(5, course.getWizardParam());
            save.setInt(6, course.getNextXUsersBecomeModerator());
            save.setString(7, course.getWelcomeMessage());
            save.setObject(8, course.getCreationTimestamp());
            save.setLong(9, course.getOptions()[0]);
            save.executeUpdate();
        }

        return course;
    }

    /**
     * The personal information of all course members will be removed and they won't be able to login anymore. Users who are also member of other
     * courses are not affected.
     *
     * @param course
     * @throws SQLException
     */
    public void anonymize(Course course) throws SQLException
    {
        // disable registration wizard
        course.setOption(Option.Users_Disable_wizard, true);
        save(course);

        // anonymize users
        List<User> undeletedUsers = new LinkedList<>(); // users that can't be deleted because they are member of other courses
        for(User user : course.getMembers())
        {
            if(user.getCourses().size() > 1)
            {
                log.debug("Can't delete user: " + user);
                undeletedUsers.add(user);
            }
            else
            {
                learnweb.getUserManager().anonymize(user);
            }
        }
    }

    public void delete(Course course) throws SQLException
    {
        deleteHard(course, false);
    }

    /**
     * All users and their resources will be deleted. Except for users who are also member of other courses.
     *
     * @param course
     * @param force If true the course is deleted even if it still contains users
     * @return The users who where not deleted because they are member of other courses.
     * @throws SQLException
     */
    public List<User> deleteHard(Course course, boolean force) throws SQLException
    {
        if(!force && course.getMemberCount() > 0)
            throw new IllegalArgumentException("course can't be deleted, remove all members first");

        UserManager userManager = learnweb.getUserManager();

        List<User> undeletedUsers = new LinkedList<>(); // users that can't be deleted because they are member of other courses
        for(User user : course.getMembers())
        {
            if(user.getCourses().size() > 1)
            {
                log.debug("Can't delete user: " + user);
                undeletedUsers.add(user);
            }
            else
                userManager.deleteUserHard(user);
        }
        for(Group group : course.getGroups())
        {
            group.deleteHard();
        }

        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_user_course` WHERE course_id = ?"))
        {
            delete.setInt(1, course.getId());
            delete.executeUpdate();
        }
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_course` WHERE course_id = ?"))
        {
            delete.setInt(1, course.getId());
            delete.executeUpdate();
        }
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_category` WHERE category_course_id = ?"))
        {
            delete.setInt(1, course.getId());
            delete.executeUpdate();
        }

        cache.remove(course.getId());

        return undeletedUsers;
    }

    /**
     * Add a user to a course
     *
     * @param course
     * @param user
     * @throws SQLException
     */
    public void addUser(Course course, User user) throws SQLException
    {
        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_user_course` (`user_id` ,`course_id`) VALUES (?, ?)"))
        {
            insert.setInt(1, user.getId());
            insert.setInt(2, course.getId());
            insert.executeUpdate();
        }
    }

    public void removeUser(Course course, User user) throws SQLException
    {
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_user_course` WHERE `user_id` = ? AND `course_id` = ?"))
        {
            delete.setInt(1, user.getId());
            delete.setInt(2, course.getId());
            delete.executeUpdate();
        }
    }

    /**
     * overrides toString method to simplify output in templates
     *
     *
     */
    private class CoursesList extends LinkedList<Course> implements Comparable<CoursesList>
    {
        private static final long serialVersionUID = 8924683355506959050L;

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            for(Course course : this)
            {
                sb.append(course.getTitle());
                sb.append(", ");
            }
            if(sb.length() > 0) // remove last comma
                sb.setLength(sb.length() - 2);

            return sb.toString();
        }

        @Override
        public int compareTo(CoursesList other)
        {
            return toString().compareTo(other.toString());
        }
    }

}

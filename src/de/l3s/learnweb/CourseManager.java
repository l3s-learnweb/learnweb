package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.Course.Option;

/**
 * DAO for the Course class.
 * Because there are only a few courses we keep them all in memory
 * 
 * @author Philipp
 * 
 */
public class CourseManager
{
    /*
     * The options are stored bitwise in long variables.
     * How many long vars are necessary to store the options?
     */
    protected final static int FIELDS = (int) Math.ceil(Option.values().length / 64.0);
    private final static String COLUMNS;

    static
    {
        StringBuilder qry = new StringBuilder(
                "course_id, title, forum_id, forum_category_id, organisation_id, default_group_id, wizard_param, wizard_enabled, next_x_users_become_moderator, default_interweb_username, default_interweb_password, welcome_message, banner_color, banner_image_file_id, options_field1");
        for(int i = 2; i <= FIELDS; i++)
            qry.append(", options_field").append(i);
        COLUMNS = qry.toString();
    }

    private Learnweb learnweb;
    private Map<Integer, Course> cache;

    protected CourseManager(Learnweb learnweb) throws SQLException
    {
        super();
        this.learnweb = learnweb;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<Integer, Course>(70));
        this.resetCache();
    }

    public synchronized void resetCache() throws SQLException
    {
        cache.clear();

        // load all courses into cache
        Statement select = learnweb.getConnection().createStatement();
        ResultSet rs = select.executeQuery("SELECT " + COLUMNS + " FROM lw_course LEFT JOIN lw_user_course USING(course_id) ORDER BY title");
        while(rs.next())
            cache.put(rs.getInt("course_id"), new Course(rs));
        select.close();
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
        List<Course> courses = new LinkedList<Course>();

        for(Course course : cache.values()) // it's ok to iterate over the courses because we have only a few
        {
            if(course.getOrganisationId() == organisationId)
                courses.add(course);
        }

        return courses;
    }

    /**
     * 
     * @param userId
     * @return The list is empty (but not null) if no courses were found
     * @throws SQLException
     */
    public List<Course> getCoursesByUserId(int userId) throws SQLException
    {
        List<Course> courses = new LinkedList<Course>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT course_id FROM lw_user_course WHERE user_id = ?");
        select.setInt(1, userId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
            courses.add(getCourseById(rs.getInt(1)));
        select.close();

        return courses;
    }

    /**
     * Saves the course to the database.
     * If the course is not yet stored at the database, a new record will be created and the returned course contains the new id.
     * 
     * @param course
     * @return
     * @throws SQLException
     */
    public synchronized Course save(Course course) throws SQLException
    {
        if(course.getId() < 0) // the course is not yet stored at the database 
        { // we have to get a new id from the group manager
            Group group = new Group();
            group.setTitle(course.getTitle());
            group.setDescription("Course");

            learnweb.getGroupManager().save(group);
            learnweb.getGroupManager().deleteGroup(group);
            course.setId(group.getId());

            cache.put(course.getId(), course);
        }

        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_course` (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

        if(course.getId() < 0) // the course is not yet stored at the database 			
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, course.getId());
        replace.setString(2, course.getTitle());
        replace.setInt(3, 0); // not used any more
        replace.setInt(4, 0); // not used any more
        replace.setInt(5, course.getOrganisationId());
        replace.setInt(6, course.getDefaultGroupId());
        replace.setString(7, course.getWizardParam());
        replace.setInt(8, course.isWizardEnabled() ? 1 : 0);
        replace.setInt(9, course.getNextXUsersBecomeModerator());
        replace.setString(10, course.getDefaultInterwebUsername());
        replace.setString(11, course.getDefaultInterwebPassword());
        replace.setString(12, course.getWelcomeMessage());
        replace.setString(13, course.getBannerColor());
        replace.setInt(14, course.getBannerImageFileId());
        replace.setLong(15, course.getOptions()[0]);
        replace.executeUpdate();

        if(course.getId() < 0) // it's a new course -> get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            course.setId(rs.getInt(1));

            cache.put(course.getId(), course); // add the new organisation to the cache
        }
        replace.close();

        return course;
    }

    public void delete(int courseId) throws SQLException
    {
        delete(getCourseById(courseId));
    }

    public void delete(Course course) throws SQLException
    {
        if(course.getMemberCount() > 0)
            throw new IllegalArgumentException("course can't be deleted, remove all members first");

        PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_course` WHERE course_id = ?");
        delete.setInt(1, course.getId());
        delete.executeUpdate();
        delete.close();

        delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_user_course` WHERE course_id = ?");
        delete.setInt(1, course.getId());
        delete.executeUpdate();
        delete.close();

        cache.remove(course.getId());
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
        PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_user_course` (`user_id` ,`course_id`) VALUES (?, ?)");
        insert.setInt(1, user.getId());
        insert.setInt(2, course.getId());
        insert.executeUpdate();
        insert.close();
    }

    /*
    public static void main(String[] args) throws SQLException // tests	
    {
    Learnweb lw = Learnweb.getInstance();
    CourseManager cm = lw.getCourseManager();
    
    for(Course o : cm.getCoursesAll())
    {
     
    }
    }
    */

}

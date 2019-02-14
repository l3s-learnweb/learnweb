package de.l3s.learnweb.yourinformation;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

/**
 * CoursesBean is responsible for displaying user courses.
 */
@Named
@ViewScoped
public class YourCoursesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 2345329598608998027L;
    private static final Logger log = Logger.getLogger(YourCoursesBean.class);

    private List<Course> courses;

    public YourCoursesBean()
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;
        try
        {
            courses = user.getCourses();
        }
        catch(SQLException sqlException)
        {
            log.error("Could not properly retrieve user courses.", sqlException);
        }
    }

    public List<Course> getUserCourses()
    {
        return courses;
    }
}

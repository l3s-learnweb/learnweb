package de.l3s.learnweb.yourinformation;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;

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

    public YourCoursesBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        courses = user.getCourses();
    }

    public List<Course> getUserCourses()
    {
        return courses;
    }
}

package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.user.Course;

import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
* CoursesBean is responsible for displaying user courses.
* */
@ManagedBean(name = "yourCoursesBean", eager = true)
@SessionScoped
public class YourCoursesBean extends YourGeneralInfoBean
{
    private List<Course> userCourses;

    public YourCoursesBean(){
        try{
            this.userCourses = this.getUser().getCourses();
        } catch(SQLException sqlException){
            this.userCourses = new ArrayList<>();
            logger.error("Could not properly retrieve user courses." + sqlException);
        }
    }

    public List<Course> getUserCourses()
    {
        return userCourses;
    }

    public void setUserCourses(final List<Course> userCourses)
    {
        this.userCourses = userCourses;
    }
}

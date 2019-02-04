package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.user.Course;

import javax.inject.Named;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
* CoursesBean is responsible for displaying user courses.
* */
@Named
public class CoursesBean extends GeneralinfoBean {
    private List<Course> userCourses;

    public CoursesBean(){
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

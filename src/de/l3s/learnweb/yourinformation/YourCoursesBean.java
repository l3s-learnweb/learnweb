package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Course;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
* CoursesBean is responsible for displaying user courses.
* */
@Named
@ViewScoped
public class YourCoursesBean extends ApplicationBean implements Serializable {
    private static final Logger logger = Logger.getLogger(YourCoursesBean.class);

    public YourCoursesBean() { }

    public List<Course> getUserCourses() {
        try {
            return this.getUser().getCourses();
        }
        catch(SQLException sqlException) {
            logger.error("Could not properly retrieve user courses." + sqlException);
            return new ArrayList<>();
        }
    }
}

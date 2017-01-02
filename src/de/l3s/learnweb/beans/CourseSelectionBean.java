package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Course;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class CourseSelectionBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -7497852434186287337L;
    private static final Logger log = Logger.getLogger(CourseSelectionBean.class);

    private List<Course> inactiveCourses;
    private Course activeCourse;

    public CourseSelectionBean() throws SQLException
    {
        if(getUser() == null)
            return;

        activeCourse = getUser().getActiveCourse();
        System.out.println("active course" + activeCourse);
        inactiveCourses = getUser().getCourses();

        // remove active course from course list
        Iterator<Course> iterator = inactiveCourses.iterator();
        System.out.println(inactiveCourses.size());
        while(iterator.hasNext())
        {
            Course course = iterator.next();

            if(course.equals(activeCourse))
            {
                iterator.remove();
                return;
            }
        }
        log.warn("Active course not found among all courses");
    }

    public Course getActiveCourse()
    {
        return activeCourse;
    }

    public List<Course> getInactiveCourses()
    {
        System.out.println(inactiveCourses.size());
        return inactiveCourses;
    }

    public String onChangeActiveCourse(int courseId) throws SQLException
    {
        if(courseId != activeCourse.getId())
        {
            getUser().setActiveCourseId(courseId);
            getUser().save();
            log.debug(getUser().toString() + " has set active course to:" + courseId);
        }
        return ApplicationBean.getTemplateDir() + "/" + getUser().getOrganisation().getWelcomePage() + "?faces-redirect=true";
    }

}

package de.l3s.learnweb.beans;

import java.sql.SQLException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Course;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class CourseSelectionBean extends ApplicationBean
{

    private List<Course> courses;
    private int activeCourseId;

    public CourseSelectionBean() throws SQLException
    {
	courses = getUser().getCourses();
	activeCourseId = getUser().getActiveCourseId();
    }

    public List<Course> getCourses()
    {
	return courses;
    }

    public int getActiveCourseId()
    {
	return activeCourseId;
    }

    public void onChangeActiveCourse(Course courses)
    {
	// TODO
    }

}

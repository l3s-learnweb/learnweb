package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import de.l3s.learnweb.Course;

@ManagedBean
@SessionScoped
public class CoursesBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = -7002093731953644113L;
	
	private List<Course> courses = new LinkedList<Course>();
	
	public CoursesBean() throws SQLException 
	{
		
	}

	public List<Course> getCourses() {
		return courses;
	}

	public void setCourses(List<Course> courses) {
		this.courses = courses;
	}

}

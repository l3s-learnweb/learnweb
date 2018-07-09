package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationManager;
import de.l3s.learnweb.user.User;

@ManagedBean
@RequestScoped
public class AdminCoursesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5469152668344315959L;
    private List<Course> courses;
    @NotNull
    @Size(min = 2, max = 50)
    private String newCourseTitle;

    @NotNull
    @Size(min = 2, max = 50)
    private String newOrganisationTitle;

    public AdminCoursesBean() throws SQLException
    {
        load();
    }

    private void load() throws SQLException
    {
        if(getUser().isAdmin())
            courses = new ArrayList<Course>(getLearnweb().getCourseManager().getCoursesAll());
        else if(getUser().isModerator())
            courses = new ArrayList<Course>(getUser().getOrganisation().getCourses());
        else
            return;

        Collections.sort(courses);
    }

    public void onCreateCourse()
    {
        try
        {
            User user = getUser();

            Course course = new Course();
            course.setTitle(newCourseTitle);
            course.setOrganisationId(user.getOrganisationId());
            getLearnweb().getCourseManager().save(course);

            course.addUser(user);
            addMessage(FacesMessage.SEVERITY_INFO, "A new course has been created. You should edit it now.");
            load(); // update course list
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void onDeleteCourse(Course course)
    {
        try
        {
            getLearnweb().getCourseManager().delete(course);
            addMessage(FacesMessage.SEVERITY_INFO, "The course '" + course.getTitle() + "' has been deleted.");
            load(); // update course list
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void onCreateOrganisation()
    {
        try
        {
            OrganisationManager om = getLearnweb().getOrganisationManager();

            if(om.getOrganisationByTitle(newOrganisationTitle) != null)
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "The title is already already take by an other organisation.");
                return;
            }
            Organisation org = new Organisation(-1);
            org.setTitle(newOrganisationTitle);
            om.save(org);
            addMessage(FacesMessage.SEVERITY_INFO, "A new organisation has been created. Now you can assign courses to it.");
            load(); // update course list
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public List<Course> getCourses()
    {
        return courses;
    }

    public String getNewCourseTitle()
    {
        return newCourseTitle;
    }

    public void setNewCourseTitle(String newCourseTitle)
    {
        this.newCourseTitle = newCourseTitle;
    }

    public String getNewOrganisationTitle()
    {
        return newOrganisationTitle;
    }

    public void setNewOrganisationTitle(String newOrganisationTitle)
    {
        this.newOrganisationTitle = newOrganisationTitle;
    }
}

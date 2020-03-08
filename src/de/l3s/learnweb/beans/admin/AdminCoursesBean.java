package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationManager;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class AdminCoursesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5469152668344315959L;
    private static final Logger log = Logger.getLogger(AdminCoursesBean.class);

    private List<Course> courses;
    @NotBlank
    @Length(min = 2, max = 50)
    private String newCourseTitle;

    @NotBlank
    @Length(min = 2, max = 50)
    private String newOrganisationTitle;

    public AdminCoursesBean() throws SQLException
    {
        load();
    }

    private void load() throws SQLException
    {
        if(getUser().isAdmin())
            courses = new ArrayList<>(getLearnweb().getCourseManager().getCoursesAll());
        else if(getUser().isModerator())
            courses = new ArrayList<>(getUser().getOrganisation().getCourses());
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
            course.save();

            course.addUser(user);
            addMessage(FacesMessage.SEVERITY_INFO, "A new course has been created. You should edit it now.");
            load(); // update course list
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void onDeleteCourse(Course course)
    {
        try
        {
            List<User> undeletedUsers = getLearnweb().getCourseManager().deleteHard(course, getUser().isAdmin());

            log.info("Deleted course " + course);
            log(Action.course_delete, 0, course.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "The course '" + course.getTitle() + "' has been deleted. " + (undeletedUsers.size() > 0 ? "But " + undeletedUsers.size() + "were not deleted because they are member of other courses." : ""));

            load(); // update course list
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void onAnonymiseCourse(Course course)
    {
        try
        {
            getLearnweb().getCourseManager().anonymize(course);

            log.info("Anonymized course " + course);
            log(Action.course_anonymize, 0, course.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "The course '" + course.getTitle() + "' has been anonymized.");

            load(); // update course list
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void onCreateOrganisation() throws SQLException
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

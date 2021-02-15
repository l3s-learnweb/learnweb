package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class AdminCoursesBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -5469152668344315959L;
    private static final Logger log = LogManager.getLogger(AdminCoursesBean.class);

    private List<Course> courses;
    @NotBlank
    @Length(min = 2, max = 50)
    private String newCourseTitle;

    @NotBlank
    @Length(min = 2, max = 50)
    private String newOrganisationTitle;

    @Inject
    private OrganisationDao organisationDao;

    @Inject
    private CourseDao courseDao;

    @PostConstruct
    public void init() {
        if (getUser().isAdmin()) {
            courses = courseDao.findAll();
        } else if (getUser().isModerator()) {
            courses = new ArrayList<>(getUser().getOrganisation().getCourses());
        } else {
            BeanAssert.authorized(false);
        }

        Collections.sort(courses);
    }

    public void onCreateCourse() {
        User user = getUser();

        Course course = new Course();
        course.setTitle(newCourseTitle);
        course.setOrganisationId(user.getOrganisationId());
        course.save();

        course.addUser(user);
        addMessage(FacesMessage.SEVERITY_INFO, "A new course has been created. You should edit it now.");
        newCourseTitle = ""; // reset input value
        init(); // update course list
    }

    public void onDeleteCourse(Course course) {
        List<User> undeletedUsers = courseDao.deleteHard(course, getUser().isAdmin());

        log.info("Deleted course {}", course);
        log(Action.course_delete, 0, course.getId());
        addMessage(FacesMessage.SEVERITY_INFO, "The course '" + course.getTitle() + "' has been deleted. " + (undeletedUsers.isEmpty() ? "" : "But " + undeletedUsers.size() + " were not deleted because they are member of other courses."));

        init(); // update course list
    }

    public void onAnonymiseCourse(Course course) {
        courseDao.anonymize(course);

        log.info("Anonymized course {}", course);
        log(Action.course_anonymize, 0, course.getId());
        addMessage(FacesMessage.SEVERITY_INFO, "The course '" + course.getTitle() + "' has been anonymized.");

        init(); // update course list
    }

    public void onCreateOrganisation() {
        if (organisationDao.findByTitle(newOrganisationTitle).isPresent()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "The title is already already take by an other organisation.");
            return;
        }

        Organisation org = new Organisation(newOrganisationTitle);
        organisationDao.save(org);
        addMessage(FacesMessage.SEVERITY_INFO, "A new organisation has been created. Now you can assign courses to it.");
        init(); // update course list
    }

    public List<Course> getCourses() {
        return courses;
    }

    public String getNewCourseTitle() {
        return newCourseTitle;
    }

    public void setNewCourseTitle(String newCourseTitle) {
        this.newCourseTitle = newCourseTitle;
    }

    public String getNewOrganisationTitle() {
        return newOrganisationTitle;
    }

    public void setNewOrganisationTitle(String newOrganisationTitle) {
        this.newOrganisationTitle = newOrganisationTitle;
    }
}

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class AdminCoursesBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -5469152668344315959L;
    private static final Logger log = LogManager.getLogger(AdminCoursesBean.class);

    private Course newCourse = new Course();
    private List<Course> courses;

    @Inject
    private CourseDao courseDao;

    @Inject
    private GroupDao groupDao;

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
        createCourse();
    }

    public void onCreateCourseAndGroup() {
        User user = getUser();
        Course course = createCourse();

        Group group = new Group();
        group.setLeader(user);
        group.setTitle(course.getTitle());
        group.setCourseId(course.getId());

        groupDao.save(group);
        user.joinGroup(group);

        // log and show notification
        log(Action.group_creating, group.getId(), group.getId());
        addMessage(FacesMessage.SEVERITY_INFO, "A new group with the name of the course was created.");
    }

    private Course createCourse() {
        User user = getUser();

        if (StringUtils.isNotBlank(newCourse.getWizardParam()) && courseDao.findByWizard(newCourse.getWizardParam()).isPresent()) {
            throw new IllegalArgumentException("This wizard param is already used!");
        }

        Course course = newCourse;
        course.setOrganisationId(user.getOrganisationId());
        courseDao.save(course);

        course.addUser(user);
        addMessage(FacesMessage.SEVERITY_INFO, "A new course has been created. You should edit it now.");

        newCourse = new Course(); // reset input values
        courses.add(course);
        Collections.sort(courses);
        return course;
    }

    public void onDeleteCourse(Course course) {
        List<User> undeletedUsers = courseDao.deleteHard(course, getUser().isAdmin());

        log.info("Deleted course {}", course);
        log(Action.course_delete, 0, course.getId());
        addMessage(FacesMessage.SEVERITY_INFO, "The course '" + course.getTitle() + "' has been deleted. " +
            (undeletedUsers.isEmpty() ? "" : "But " + undeletedUsers.size() + " were not deleted because they are member of other courses."));

        courses.remove(course);
    }

    public void onAnonymiseCourse(Course course) {
        courseDao.anonymize(course);

        log.info("Anonymized course {}", course);
        log(Action.course_anonymize, 0, course.getId());
        addMessage(FacesMessage.SEVERITY_INFO, "The course '" + course.getTitle() + "' has been anonymized.");
    }

    public Course getNewCourse() {
        return newCourse;
    }

    public List<Course> getCourses() {
        return courses;
    }
}

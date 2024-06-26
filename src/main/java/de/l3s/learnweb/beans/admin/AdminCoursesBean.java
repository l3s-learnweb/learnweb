package de.l3s.learnweb.beans.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

@Named
@ViewScoped
public class AdminCoursesBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -5469152668344315959L;
    private static final Logger log = LogManager.getLogger(AdminCoursesBean.class);

    private Course newCourse = new Course();

    private transient List<Course> courses;
    private transient Course selectedCourse;

    @Inject
    private CourseDao courseDao;

    @Inject
    private GroupDao groupDao;

    @Inject
    private UserDao userDao;

    @PostConstruct
    public void init() {
        if (getUser().isAdmin()) {
            courses = courseDao.findAll();
        } else if (getUser().isModerator()) {
            courses = new ArrayList<>(getUser().getOrganisation().getCourses());
        } else {
            throw new ForbiddenHttpException();
        }

        Collections.sort(courses);
    }

    public String onCreateCourse() {
        Course course = createCourse();
        return "admin/course.jsf?course_id=" + course.getId();
    }

    public String onCreateCourseAndGroup() {
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
        return "admin/course.jsf?course_id=" + course.getId();
    }

    private Course createCourse() {
        User user = getUser();

        if (StringUtils.isNotBlank(newCourse.getRegistrationWizard()) && courseDao.findByWizard(newCourse.getRegistrationWizard()).isPresent()) {
            throw new IllegalArgumentException("This wizard param is already used!");
        }

        Course course = newCourse;
        course.setOrganisationId(user.getOrganisationId());
        courseDao.save(course);

        course.addUser(user);
        addMessage(FacesMessage.SEVERITY_INFO, "A new course has been created. You should edit it now.");
        if (StringUtils.isNotEmpty(course.getRegistrationWizard())) {
            addMessage(FacesMessage.SEVERITY_INFO, "The wizard is: " + Faces.getRequestBaseURL() + course.getRegistrationWizard());
        }

        newCourse = new Course(); // reset input values
        courses.add(course);
        Collections.sort(courses);
        return course;
    }

    public String getDeleteSummary(Course course) {
        StringBuilder summary = new StringBuilder();

        if (course != null) {
            List<Group> groups = groupDao.findAllByCourseId(course.getId());
            if (!groups.isEmpty()) {
                summary.append("Groups (").append(groups.size()).append(") and resources in them:").append("<br/>");
                for (Group group : groups) {
                    summary.append(" - <b>").append(group.getTitle()).append("</b> including ").append(group.getResourcesCount()).append(" resource;<br/>");
                }
            }

            summary.append("Users:").append("<br/>");
            for (User user : userDao.findByCourseId(course.getId())) {
                if (userDao.countCoursesByUserId(user.getId()) > 1) {
                    summary.append(" - <s>").append(user.getDisplayName()).append("</s> (have other groups)<br/>");
                } else if (user.isAdmin()) {
                    summary.append(" - <s>").append(user.getDisplayName()).append("</s> (is admin)<br/>");
                } else {
                    summary.append(" - <b>").append(user.getDisplayName()).append("</b><br/>");
                }
            }
        }

        return summary.toString();
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

    public Course getSelectedCourse() {
        return selectedCourse;
    }

    public void setSelectedCourse(final Course selectedCourse) {
        this.selectedCourse = selectedCourse;
    }
}

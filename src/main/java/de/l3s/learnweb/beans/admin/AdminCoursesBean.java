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
import de.l3s.util.StringHelper;

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
        addMessage(FacesMessage.SEVERITY_INFO, "admin.course_group_created");
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
        addMessage(FacesMessage.SEVERITY_INFO, "admin.course_created");
        if (StringUtils.isNotEmpty(course.getRegistrationWizard())) {
            addMessage(FacesMessage.SEVERITY_INFO, "admin.course_wizard_link", Faces.getRequestBaseURL() + "wizard=" + course.getRegistrationWizard());
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
                summary.append(getLocaleMessage("groupsTitle")).append(" (").append(groups.size()).append("):<br/>");
                for (Group group : groups) {
                    summary.append(" - <b>").append(StringHelper.escapeHtml(group.getTitle())).append("</b> (").append(getLocaleMessage("resources")).append(": ")
                        .append(group.getResourcesCount()).append(")<br/>");
                }
            }

            summary.append(getLocaleMessage("users")).append(":<br/>");
            for (User user : userDao.findByCourseId(course.getId())) {
                // users who won't be deleted are struck through
                String keptReason = userDao.countCoursesByUserId(user.getId()) > 1 ? "admin.member_of_other_courses" : user.isAdmin() ? "admin.user_is_admin" : null;
                String name = StringHelper.escapeHtml(user.getDisplayName());
                if (keptReason != null) {
                    summary.append(" - <s>").append(name).append("</s> (").append(getLocaleMessage(keptReason)).append(")<br/>");
                } else {
                    summary.append(" - <b>").append(name).append("</b><br/>");
                }
            }
        }

        return summary.toString();
    }

    public void onDeleteCourse(Course course) {
        List<User> undeletedUsers = courseDao.deleteHard(course, getUser().isAdmin());

        log.info("Deleted course {}", course);
        log(Action.course_delete, 0, course.getId());
        addMessage(FacesMessage.SEVERITY_INFO, "admin.course_deleted", course.getTitle());
        if (!undeletedUsers.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_INFO, "admin.course_users_not_deleted", String.join(", ", undeletedUsers.stream().map(User::getUsername).toList()));
        }

        courses.remove(course);
    }

    public void onAnonymiseCourse(Course course) {
        courseDao.anonymize(course);

        log.info("Anonymized course {}", course);
        log(Action.course_anonymize, 0, course.getId());
        addMessage(FacesMessage.SEVERITY_INFO, "admin.course_anonymised", course.getTitle());
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

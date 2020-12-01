package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.LoginBean;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserView;

@Named
@ViewScoped
public class AdminUsersBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 155899638864937408L;

    private List<UserView> userViews;
    private int courseId;

    public void onLoad() throws SQLException {
        User user = getUser();
        BeanAssert.authorized(user);
        BeanAssert.hasPermission(user.isModerator());

        List<User> users;
        if (courseId != 0) {
            Course course = getLearnweb().getCourseManager().getCourseById(courseId);
            BeanAssert.isFound(course);
            // make sure that moderators can access only their own courses
            BeanAssert.hasPermission(user.isAdmin() || (user.isModerator() && user.getCourses().contains(course)));

            users = course.getMembers();
        } else if (user.isAdmin()) {
            users = getLearnweb().getUserManager().getUsers();
        } else if (user.isModerator()) {
            users = getLearnweb().getUserManager().getUsersByOrganisationId(user.getOrganisationId());
        } else {
            throw new IllegalStateException();
        }

        this.userViews = UserView.of(users, UserView::getCoursesTitles, UserView::getGroupsTitles);
    }

    public String rootLogin(User targetUser) throws SQLException {
        return LoginBean.rootLogin(this, targetUser);
    }

    public List<UserView> getUserViews() {
        return userViews;
    }

    public void updateUser(User targetUser) throws SQLException {
        //Updating moderator rights for particular user
        targetUser.save();
        addGrowl(FacesMessage.SEVERITY_INFO, "Updated moderator settings for '" + targetUser.getUsername() + "'");
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
}

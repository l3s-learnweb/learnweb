package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.LoginBean;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class AdminUsersBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 155899638864937408L;

    private transient List<User> users;
    private int courseId;

    public void onLoad() throws SQLException
    {
        User user = getUser();

        if(null == user) // not logged in
            return;

        if(courseId != 0)
        {
            Course course = getLearnweb().getCourseManager().getCourseById(courseId);
            if(null == course)
            {
                addInvalidParameterMessage("course_id");
                return;
            }
            if(!user.isAdmin() && !user.getCourses().contains(course)) // make sure that moderators can access only their own courses
            {
                addAccessDeniedMessage();
                return;
            }
            users = course.getMembers();
        }
        else if(user.isAdmin())
            users = getLearnweb().getUserManager().getUsers();
        else if(user.isModerator())
            users = getLearnweb().getUserManager().getUsersByOrganisationId(user.getOrganisationId());
    }

    public String login(User user) throws SQLException
    {
        if(!canLoginToAccount(user))
        {
            addErrorMessage(new IllegalAccessError(getUser() + " tried to hijack account"));
            return "";
        }
        getUserBean().setModeratorUser(getUser()); // store moderator account while logged in as user

        return LoginBean.loginUser(this, user, getUser().getId());
    }

    public List<User> getUsers()
    {
        return users;
    }

    /**
     * Make sure that only admins login to moderator accounts
     *
     * @param targetUser
     * @return
     */
    public boolean canLoginToAccount(User targetUser)
    {
        User user = getUser();
        if(user.isAdmin())
            return true;

        if(targetUser.isModerator())
            return false;

        if(user.isModerator())
            return true;

        return false;
    }

    public void updateUser(User targetUser) throws SQLException
    {
        //Updating moderator rights for particular user
        targetUser.save();
        addGrowl(FacesMessage.SEVERITY_INFO, "Updated moderator settings for '" + targetUser.getUsername() + "'");
    }

    public int getCourseId()
    {
        return courseId;
    }

    public void setCourseId(int courseId)
    {
        this.courseId = courseId;
    }

    public static String concatGroups(List<Group> groups)
    {
        return groups.stream().sorted().map(Group::getTitle).collect(Collectors.joining(", "));
    }
}

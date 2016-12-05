package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.learnwebBeans.LoginBean;

@ManagedBean
@RequestScoped
public class AdminUserBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 155899638864937408L;

    private transient List<User> users;

    public AdminUserBean() throws SQLException
    {
	User user = getUser();

	if(null == user) // not logged in
	    return;

	Integer courseId = getParameterInt("course_id");

	if(courseId != null)
	    users = getLearnweb().getCourseManager().getCourseById(courseId).getMembers();
	else if(user.isAdmin())
	    users = getLearnweb().getUserManager().getUsers();
	else if(user.isModerator())
	    users = getLearnweb().getUserManager().getUsersByOrganisationId(user.getOrganisationId());
    }

    public String login(User user) throws SQLException
    {
	UtilBean.getUserBean().setModeratorUser(getUser()); // store moderator account while logged in as user 

	return LoginBean.loginUser(this, user, true);
    }

    public List<User> getUsers()
    {
	return users;
    }
}

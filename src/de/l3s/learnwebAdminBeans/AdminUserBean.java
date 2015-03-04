package de.l3s.learnwebAdminBeans;

import java.sql.SQLException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class AdminUserBean extends ApplicationBean {

	private List<User> users;

	public AdminUserBean() throws SQLException
	{		
		User user = getUser();
		
		if(null == user) // not logged in
			return;		
		
		if(user.isAdmin())
			users = getLearnweb().getUserManager().getUsers();
		else if(user.isModerator())
			users = getLearnweb().getUserManager().getUsersByOrganisationId(user.getOrganisationId());
	}
	
	public String login(User user)
	{
		UtilBean.getUserBean().setUser(user);
		
		return getTemplateDir()+ "/myhome/profile.xhtml";
	}

	public List<User> getUsers() {
		return users;
	}	
}

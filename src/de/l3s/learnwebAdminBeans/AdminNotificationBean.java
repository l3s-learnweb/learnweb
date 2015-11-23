package de.l3s.learnwebAdminBeans;

import java.sql.SQLException;
import java.util.Date;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.Message;
import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class AdminNotificationBean extends ApplicationBean
{

    @NotEmpty
    private String text;
    @NotEmpty
    private String title;
    //Alana
    private String[] listStudents;

    public void send() throws SQLException
    {
	// get selected users, necessary because jsf sucks
	HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
	String[] tempSelectedUsers = request.getParameterValues("selected_users");

	if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "Please select the users you want to send a message.");
	    return;
	}

	// tree is used to make sure that every user gets the message only once
	TreeSet<Integer> selectedUsers = new TreeSet<Integer>();
	for(String userId : tempSelectedUsers)
	{
	    selectedUsers.add(Integer.parseInt(userId));
	}

	User fromUser = getUser();

	Message message = new Message();
	message.setFromUser(fromUser);
	message.setTitle(this.title);
	message.setText(this.text);
	message.setTime(new Date());

	UserManager um = getLearnweb().getUserManager();
	int counter = 0;

	for(int userId : selectedUsers)
	{
	    message.setToUser(um.getUser(userId));
	    message.save();
	    counter++;
	}
	addMessage(FacesMessage.SEVERITY_INFO, counter + " Notifications send");
    }

    //Alana
    public void send2() throws SQLException
    {
	System.out.println("Send2");
	System.out.println("lista no admin:" + this.listStudents[0]);
	if(null == this.listStudents || this.listStudents.length == 0)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "Please select the users you want to send a message.");
	    return;
	}

	TreeSet<Integer> selectedUsers = new TreeSet<Integer>();
	for(String userId : this.listStudents)
	{
	    selectedUsers.add(Integer.parseInt(userId));
	}

	User fromUser = getUser();

	Message message = new Message();
	message.setFromUser(fromUser);
	message.setTitle(this.title);
	message.setText(this.text);
	message.setTime(new Date());

	UserManager um = getLearnweb().getUserManager();
	int counter = 0;

	for(int userId : selectedUsers)
	{
	    message.setToUser(um.getUser(userId));
	    message.save();
	    counter++;
	}
	addMessage(FacesMessage.SEVERITY_INFO, counter + " Notifications send");
    }

    public String getText()
    {
	return text;
    }

    public void setText(String text)
    {
	this.text = text;
    }

    public String getTitle()
    {
	return title;
    }

    public void setTitle(String title)
    {
	this.title = title;
    }

    //Alana
    public String[] getListStudents()
    {
	return listStudents;
    }

    public void setListStudents(String[] listStudents)
    {
	this.listStudents = listStudents;
    }
}

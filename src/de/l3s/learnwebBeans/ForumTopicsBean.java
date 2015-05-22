package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;

@ManagedBean
@RequestScoped
public class ForumTopicsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(ForumTopicsBean.class);

    private int groupId;

    private String name, email, query, message;

    private Group group;

    public ForumTopicsBean()
    {

    }

    public void preRenderView(ComponentSystemEvent e)
    {
	try
	{
	    loadGroup();
	}
	catch(SQLException e1)
	{
	    log.error("Cant load group", e1);
	}

	//log.debug(group.getTitle());
    }

    private void loadGroup() throws SQLException
    {
	if(0 == groupId)
	{
	    String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");
	    if(temp != null && temp.length() != 0)
	    {
		groupId = Integer.parseInt(temp);
		group = getLearnweb().getGroupManager().getGroupById(groupId);
	    }

	    if(0 == groupId)
		return;
	}

    }

    public String getName()
    {
	return name;
    }

    public void setName(String name)
    {
	this.name = name;
	System.out.println(name);

    }

    public String getEmail()
    {

	return email;
    }

    public void setEmail(String email)
    {

	this.email = email;
	System.out.println(email);

    }

    public String getQuery()
    {
	return query;
    }

    public void setQuery(String query)
    {
	this.query = query;
	System.out.println(query);

    }

    public void saveForumPost()
    {
	try
	{
	    message = Learnweb.getInstance().getForumManager().saveForumPost(name, email, query);
	    this.name = "";
	    this.email = "";
	    this.query = "";
	    addGrowl(FacesMessage.SEVERITY_INFO, message);
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    log.error("Error while inserting in db", e);
	}
    }

    public String getMessage()
    {
	return message;
    }

    public void setMessage(String message)
    {
	this.message = message;
    }

}

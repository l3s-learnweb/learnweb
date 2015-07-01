package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;

@ManagedBean
@RequestScoped
public class ForumPostBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(ForumPostBean.class);

    private int groupId;

    private String topic, message, post;

    private Group group;

    public ForumPostBean()
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

    public String getPost()
    {
	return post;
    }

    public void setPost(String post)
    {
	this.post = post;
	System.out.println(post);

    }

    public void saveForumPost()
    {
	try
	{
	    int group_id = 883;
	    int user_id = 1;
	    int topic_id = 1;
	    Learnweb.getInstance().getForumManager().saveForumPost(post, topic_id, group_id, user_id);
	    this.topic = "";
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

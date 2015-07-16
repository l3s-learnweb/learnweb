package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.ForumTopic;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;

@ManagedBean
@RequestScoped
public class ForumTopicsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(ForumTopicsBean.class);

    private int groupId;

    private String topic;

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
    }

    private void loadGroup() throws SQLException
    {
	if(0 == groupId)
	{
	    String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");
	    if(temp != null && temp.length() != 0)
	    {
		groupId = Integer.parseInt(temp);
		setGroup(getLearnweb().getGroupManager().getGroupById(groupId));
	    }

	    if(0 == groupId)
		return;
	}

    }

    //   public String getTopic()
    // {
    //	return topic;
    //   }

    //   public void setTopic(String topic)
    //  {
    //	this.topic = topic;
    //	System.out.println(topic);

    //}

    public void saveForumTopic() throws SQLException
    {
	try
	{
	    ForumTopic forumTopic = new ForumTopic();
	    forumTopic.setTopic(topic);
	    forumTopic.setGroupId(groupId);
	    Learnweb.getInstance().getForumManager().saveTopic(forumTopic);
	    this.topic = "";
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    log.error("Error while inserting in db", e);
	}
    }
    /**
     * public String getMessage()
     * {
     * return message;
     * }
     * 
     * public void setMessage(String message)
     * {
     * this.message = message;
     * }
     **/

    public Group getGroup()
    {
	return group;
    }

    public void setGroup(Group group)
    {
	this.group = group;
    }
}

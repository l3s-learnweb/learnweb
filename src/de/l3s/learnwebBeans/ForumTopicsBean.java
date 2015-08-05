package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.application.FacesMessage;
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
	    Integer id = getParameterInt("group_id");
	    if(id != null)
		groupId = id.intValue();
	}

	group = getLearnweb().getGroupManager().getGroupById(groupId);

	if(null == group)
	    addMessage(FacesMessage.SEVERITY_ERROR, "invalid or no group id");

    }

    public void saveForumTopic() throws SQLException
    {
	try
	{
	    ForumTopic forumTopic = new ForumTopic();
	    forumTopic.setTitle(topic);
	    forumTopic.setGroupId(groupId);
	    Learnweb.getInstance().getForumManager().save(forumTopic);
	    this.topic = "";
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}
    }

    public List<ForumTopic> getTopics() throws SQLException
    {
	List<ForumTopic> topics = getLearnweb().getForumManager().getTopicsByGroup(groupId);

	return topics;
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
}

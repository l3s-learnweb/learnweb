package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.ForumPost;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;

@ManagedBean
@RequestScoped
public class ForumPostBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(ForumPostBean.class);

    private int groupId, topicId, userId;

    private String text, post;

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

    public void saveForumPost()
    {
	try
	{
	    ForumPost forumPost = new ForumPost();
	    forumPost.setText(text);
	    forumPost.setTopicId(topicId);
	    forumPost.setGroupId(groupId);
	    forumPost.setUserId(userId);

	    Learnweb.getInstance().getForumManager().save(forumPost);
	    this.text = "";
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}
    }

}

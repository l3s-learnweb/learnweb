package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.ForumPost;
import de.l3s.learnweb.ForumTopic;

@ManagedBean
@ViewScoped
public class ForumPostBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6077135964610986190L;

    private final static Logger log = Logger.getLogger(ForumPostBean.class);

    private int topicId;

    private List<ForumPost> posts;

    private ForumTopic topic;

    public ForumPostBean()
    {

    }

    public void preRenderView(ComponentSystemEvent e) throws SQLException
    {
	if(topicId == 0)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "No topic_id provided");
	    return;
	}
	posts = getLearnweb().getForumManager().getPostsBy(topicId);
	//topic = getLearnweb().getForumManager().gett
    }

    public int getTopicId()
    {
	return topicId;
    }

    public void setTopicId(int topicId)
    {
	this.topicId = topicId;
    }

    public List<ForumPost> getPosts()
    {
	return posts;
    }

    public ForumTopic getTopic()
    {
	return topic;
    }

}

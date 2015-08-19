package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.ForumManager;
import de.l3s.learnweb.ForumPost;
import de.l3s.learnweb.ForumTopic;
import de.l3s.learnweb.Group;

@ManagedBean
@ViewScoped
public class ForumPostBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6077135964610986190L;

    private final static Logger log = Logger.getLogger(ForumPostBean.class);

    private int topicId;
    private List<ForumPost> posts;
    private ForumTopic topic;
    private Group group;
    private ForumPost newPost;

    public ForumPostBean()
    {
	newPost = new ForumPost();
    }

    public void preRenderView(ComponentSystemEvent e) throws SQLException
    {
	if(topicId == 0)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "No topic_id provided");
	    return;
	}
	ForumManager fm = getLearnweb().getForumManager();
	posts = fm.getPostsBy(topicId);
	topic = fm.getTopicById(topicId);
	group = getLearnweb().getGroupManager().getGroupById(topic.getGroupId());
    }

    public String onSavePost() throws SQLException
    {
	Date now = new Date();

	ForumManager fm = getLearnweb().getForumManager();
	topic = fm.save(topic);

	newPost.setUserId(getUser().getId());
	newPost.setDate(now);
	newPost.setTopicId(topicId);

	fm.save(newPost);

	newPost = new ForumPost();

	return null;//"forum_post.jsf?faces-redirect=true&topic_id=" + topicId + "#lastPost";
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

    public Group getGroup()
    {
	return group;
    }

    public ForumPost getNewPost()
    {
	return newPost;
    }

}

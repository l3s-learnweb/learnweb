package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.l3s.learnweb.ForumManager;
import de.l3s.learnweb.ForumPost;
import de.l3s.learnweb.ForumTopic;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class ForumEditBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(ForumEditBean.class);
    private static final long serialVersionUID = 6561750124856501158L;
    int postId;
    int topicId;
    private ForumPost post;
    private ForumTopic topic;
    private Group group;

    public void preRenderView(ComponentSystemEvent e) throws SQLException
    {
	if(postId == 0)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "No post_id provided");
	    return;
	}

	ForumManager fm = getLearnweb().getForumManager();
	post = fm.getPostById(postId);
	topic = fm.getTopicById(post.getTopicId());
	group = getLearnweb().getGroupManager().getGroupById(topic.getGroupId());

    }

    public String onSavePost() throws SQLException
    {
	Date date = new Date();
	User user = getUser();
	post.setLastEditDate(date);
	post.setEditCount(post.getEditCount() + 1);
	post.setEditUserId(user.getId());

	if(user.getId() == post.getUserId())
	{
	    ForumManager fm = getLearnweb().getForumManager();
	    fm.save(post);
	}

	addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
	//nameTag(post.getText());

	return "/lw/group/forum_post.xhtml?topic_id=" + post.getTopicId() + "&faces-redirect=true";
    }

    public void nameTag(String text)
    {
	Scanner s = new Scanner(text);
	String user = null;
	//if(scan.hasNext())
	//scan.next();

	while(s.hasNext())
	{
	    user = s.next();
	    if(user.contains("@"))
	    {

		log.debug("@ gefunden");
		//user = user.replaceAll("[@]", "");

		user = user.substring(user.lastIndexOf('@') + 1);

		ForumManager fm = getLearnweb().getForumManager();
		//User useruser = user;
		//sendMessage(useruser, "test");
		//if(s.hasNext())
		//{
		//    user = s.next();
		//}
		log.debug(user);
	    }
	}

	//search @User

	//write message to User
	s.close();
    }

    public void sendMessage(User toUser, String text) throws SQLException
    {
	Date time = new Date();
	DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("INSERT INTO message (from_user, to_user, m_title, m_text, m_seen, m_read, m_time) " + "VALUES (?,?,?,?,?,?,?)");
	stmt.setInt(1, toUser.getId());
	stmt.setInt(2, toUser.getId());
	stmt.setString(3, "you were mentioned in a Forumpost");

	//String convertedText = convertText(text);
	stmt.setString(4, text);
	stmt.setBoolean(5, false);
	stmt.setBoolean(6, false);
	stmt.setString(7, format.format(time.getTime()));
	stmt.executeUpdate();
	stmt.close();
    }

    public ForumTopic getTopic()
    {
	return topic;
    }

    public int getTopicId()
    {

	return topicId;
    }

    public List<SelectItem> getCategories()
    {
	return ForumPostBean.getCategoriesByCourse(group.getCourseId());
    }

    public ForumPost getPost()
    {
	return post;
    }

    public Group getGroup()
    {
	return group;
    }

    public int getPostId()
    {
	return postId;
    }

    public void setPostId(int postId)
    {
	this.postId = postId;
    }

}

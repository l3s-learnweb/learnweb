package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import de.l3s.learnweb.ForumManager;
import de.l3s.learnweb.ForumPost;
import de.l3s.learnweb.ForumTopic;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
@ViewScoped
public class ForumPostBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6077135964610986190L;
    // private static final Logger log = Logger.getLogger(ForumPostBean.class);

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

	fm.incViews(topicId);
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

	return null;
    }

    public boolean isMember() throws SQLException
    {
	User user = getUser();

	if(null == user)
	    return false;

	if(null == group)
	    return false;

	return group.isMember(user);
    }

    public List<SelectItem> getCategories()
    {
	return getCategoriesByCourse(group.getCourseId());
    }

    protected static List<SelectItem> getCategoriesByCourse(int courseId)
    {
	SelectItemGroup g1 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.1"));
	g1.setSelectItems(new SelectItem[] { new SelectItem("Forum.cell.category.1a", UtilBean.getLocaleMessage("Forum.cell.category.1a")), new SelectItem("Forum.cell.category.1b", UtilBean.getLocaleMessage("Forum.cell.category.1b")),
		new SelectItem("Forum.cell.category.1c", UtilBean.getLocaleMessage("Forum.cell.category.1c")), new SelectItem("Forum.cell.category.1d", UtilBean.getLocaleMessage("Forum.cell.category.1d")) });

	SelectItemGroup g2 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.2"));
	g2.setSelectItems(new SelectItem[] { new SelectItem("Forum.cell.category.2a", UtilBean.getLocaleMessage("Forum.cell.category.2a")), new SelectItem("Forum.cell.category.2b", UtilBean.getLocaleMessage("Forum.cell.category.2b")),
		new SelectItem("Forum.cell.category.2c", UtilBean.getLocaleMessage("Forum.cell.category.2c")), new SelectItem("Forum.cell.category.2d", UtilBean.getLocaleMessage("Forum.cell.category.2d")),
		new SelectItem("Forum.cell.category.2e", UtilBean.getLocaleMessage("Forum.cell.category.2e")), new SelectItem("Forum.cell.category.2f", UtilBean.getLocaleMessage("Forum.cell.category.2f")),
		new SelectItem("Forum.cell.category.2g", UtilBean.getLocaleMessage("Forum.cell.category.2g")), new SelectItem("Forum.cell.category.2h", UtilBean.getLocaleMessage("Forum.cell.category.2h")) });

	SelectItemGroup g3 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.3"));
	g3.setSelectItems(new SelectItem[] { new SelectItem("Forum.cell.category.3a", UtilBean.getLocaleMessage("Forum.cell.category.3a")), new SelectItem("Forum.cell.category.3b", UtilBean.getLocaleMessage("Forum.cell.category.3b")),
		new SelectItem("Forum.cell.category.3c", UtilBean.getLocaleMessage("Forum.cell.category.3c")) });

	ArrayList<SelectItem> categories = new ArrayList<SelectItem>();
	categories.add(g1);
	categories.add(g2);
	categories.add(g3);

	return categories;
    }

    public boolean canDeletePost(ForumPost obj)
    {
	User user = getUser();

	if(user.isModerator())
	    return true;

	if(user.getId() == obj.getUserId())
	    return true;

	return false;
    }

    public boolean canEditPost(ForumPost obj)
    {
	User user = getUser();

	if(user.getId() == obj.getUserId())
	    return true;

	return false;
    }

    public void deletePost(ForumPost post) throws SQLException
    {

	User user = getUser();
	if(user.isModerator() || user.getId() == post.getUserId())
	{

	    ForumManager fm = getLearnweb().getForumManager();
	    fm.deletePost(post);
	}
    }

    public void quotePost(ForumPost post) throws SQLException
    {

	//newPost.setText(post.getText());	//normal
	//newPost.setText(post.getUser().getUsername() + ": " + "<br>" + post.getText() + "<br>" + " "); //with name
	//newPost.setText("<div style='margin-left: 40px;'>" + post.getText() + "<br></div>" + " " + "<br>"); //right
	//newPost.setText(post.getText() + "<br><hr><br>"); //line
	//newPost.setText("<span style='background-color: rgb(255, 255, 153);'><span style='font-style: italic;'>" + post.getText() + "<br></span></span>" + " " + "<br>"); //colored
	//newPost.setText("<span class='fakeInput'> <input type='text' value='" + post.getText() + "'/></span>"); //in textbox
	//newPost.setText("<input  readonly='true' value=  " + post.getText() + "><br><hr><br>");
	newPost.setText("<div class='post_quote_name' style='margin: 0rem 2rem; padding: 0px 0.5rem;border-left: 3px solid rgb(168, 168, 168); background-color: rgb(168, 168, 168);'>" + post.getUser().getUsername()
		+ "</div><div class='post_quote'  style='margin: 0rem 2rem; padding: 0px 0.5rem; border-left: 2px solid rgb(168, 168, 168); border-right: 2px solid rgb(168, 168, 168); background-color: rgb(207, 207, 207);'> " + post.getText() + "</span></div><br>");
	newPost.setCategory(post.getCategory());

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

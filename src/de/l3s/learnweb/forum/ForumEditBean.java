package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.model.SelectItem;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class ForumEditBean extends ApplicationBean implements Serializable
{
    //private static final Logger log = Logger.getLogger(ForumEditBean.class);
    private static final long serialVersionUID = 6561750124856501158L;
    int postId;
    int topicId;
    private ForumPost post;
    private ForumTopic topic;
    private Group group;

    public void preRenderView() throws SQLException
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

        if(!canEditPost())
            addMessage(FacesMessage.SEVERITY_ERROR, "no_view_right");
    }

    public String onSavePost() throws SQLException
    {
        if(!canEditPost())
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "no_view_right");
            return null;
        }

        User user = getUser();
        post.setLastEditDate(new Date());
        post.setEditCount(post.getEditCount() + 1);
        post.setEditUserId(user.getId());

        if(user.getId() == post.getUserId())
        {
            ForumManager fm = getLearnweb().getForumManager();
            fm.save(post);
        }

        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");

        return "/lw/group/forum_post.xhtml?topic_id=" + post.getTopicId() + "&faces-redirect=true";
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

    public boolean canEditPost()
    {
        User user = getUser();

        if(null == user)
            return false;

        if(user.isAdmin() || user.getId() == post.getUserId())
            return true;

        return false;
    }

}

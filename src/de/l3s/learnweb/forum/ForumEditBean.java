package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class ForumEditBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 6561750124856501158L;
    //private static final Logger log = LogManager.getLogger(ForumEditBean.class);

    private int postId;
    private ForumPost post;
    private ForumTopic topic;
    private Group group;
    private List<ForumTopic> topics;

    public void onLoad() throws SQLException {
        BeanAsserts.authorized(isLoggedIn());

        ForumManager fm = getLearnweb().getForumManager();
        post = fm.getPostById(postId);
        BeanAsserts.validateNotNull(post);

        topic = fm.getTopicById(post.getTopicId());
        group = getLearnweb().getGroupManager().getGroupById(topic.getGroupId());
        topics = getLearnweb().getForumManager().getTopicsByGroup(group.getId());

        BeanAsserts.hasPermission(canEditPost());
    }

    public String onSavePost() throws SQLException {
        // TODO: it is possible to call the method avoiding onLoad method?
        BeanAsserts.hasPermission(canEditPost());

        User user = getUser();
        post.setLastEditDate(new Date());
        post.setEditCount(post.getEditCount() + 1);
        post.setEditUserId(user.getId());

        if (user.getId() == post.getUserId()) {
            ForumManager fm = getLearnweb().getForumManager();
            fm.save(post);
        }

        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");

        return "/lw/group/forum_post.xhtml?topic_id=" + post.getTopicId() + "&faces-redirect=true";
    }

    public ForumTopic getTopic() {
        return topic;
    }

    public List<SelectItem> getCategories() {
        return ForumPostBean.getCategoriesByCourse(group.getCourseId());
    }

    public ForumPost getPost() {
        return post;
    }

    public Group getGroup() {
        return group;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public boolean canEditPost() {
        User user = getUser();

        if (null == user) {
            return false;
        }

        if (user.isAdmin() || user.getId() == post.getUserId()) {
            return true;
        }

        return false;
    }

    public List<ForumTopic> getTopics() {
        return topics;
    }

}

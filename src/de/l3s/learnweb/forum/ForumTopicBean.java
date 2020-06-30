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
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class ForumTopicBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 6077135964610986190L;
    // private static final Logger log = LogManager.getLogger(ForumTopicBean.class);

    // params
    private int topicId;

    private Group group;
    private ForumTopic topic;
    private List<ForumPost> posts;
    private ForumPost dialogPost = new ForumPost();

    // used only by breadcrumbs
    private List<ForumTopic> topics;

    public void onLoad() throws SQLException {
        BeanAssert.authorized(isLoggedIn());

        ForumManager fm = getLearnweb().getForumManager();
        topic = fm.getTopicById(topicId);
        BeanAssert.validateNotNull(topic);

        group = getLearnweb().getGroupManager().getGroupById(topic.getGroupId());
        BeanAssert.groupNotNull(group);
        BeanAssert.hasPermission(group.canViewResources(getUser()));

        posts = fm.getPostsBy(topicId);
        fm.incViews(topicId);
        fm.updatePostVisitTime(topicId, getUser().getId());

        topics = getLearnweb().getForumManager().getTopicsByGroup(group.getId());
    }

    public boolean isReplyDialog() {
        return dialogPost.getId() == -1;
    }

    public void saveDialogPost() throws SQLException {
        ForumManager fm = getLearnweb().getForumManager();
        boolean replyDialog = isReplyDialog();

        if (replyDialog) {
            dialogPost.setUserId(getUser().getId());
            dialogPost.setDate(new Date());
            dialogPost.setTopicId(topicId);
        } else {
            dialogPost.setLastEditDate(new Date());
            dialogPost.setEditCount(dialogPost.getEditCount() + 1);
            dialogPost.setEditUserId(getUser().getId());
        }

        fm.save(dialogPost);

        if (replyDialog) {
            posts.add(dialogPost);
            log(Action.forum_post_added, group.getId(), topicId, topic.getTitle());
        } else {
            addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }

        dialogPost = new ForumPost();
    }

    public List<SelectItem> getCategories() {
        return ForumBean.getCategoriesByCourse(group.getCourseId(), getUserBean().getLocale());
    }

    public void deletePost(ForumPost post) throws SQLException {
        User user = getUser();
        if (user.isModerator() || user.getId() == post.getUserId()) {
            ForumManager fm = getLearnweb().getForumManager();
            fm.deletePost(post);
        }

        posts.remove(post);

        log(Action.forum_post_deleted, group.getId(), post.getId(), topic.getTitle());
    }

    public void replyPost() {
        dialogPost = new ForumPost();
    }

    public void editPost(ForumPost post) {
        dialogPost = post;
    }

    public void quotePost(ForumPost post) throws SQLException {
        dialogPost = new ForumPost();
        String username = post.getUser() != null ? post.getUser().getUsername() : "Anonymous"; // can happen for old imported posts
        String newStr = post.getText().replaceAll("<blockquote>", "<blockquote>&#160;&#160;&#160;&#160;");
        dialogPost.setText("<blockquote><strong>" + username + ":</strong>" + newStr + "</blockquote></br>");
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public List<ForumPost> getPosts() {
        return posts;
    }

    public ForumTopic getTopic() {
        return topic;
    }

    public Group getGroup() {
        return group;
    }

    public ForumPost getDialogPost() {
        return dialogPost;
    }

    public void setDialogPost(final ForumPost dialogPost) {
        this.dialogPost = dialogPost;
    }

    public List<ForumTopic> getTopics() {
        return topics;
    }
}

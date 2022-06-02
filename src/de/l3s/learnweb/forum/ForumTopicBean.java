package de.l3s.learnweb.forum;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class ForumTopicBean extends ApplicationBean implements Serializable {
    @Serial
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

    @Inject
    private GroupDao groupDao;

    @Inject
    private ForumPostDao forumPostDao;

    @Inject
    private ForumTopicDao forumTopicDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        topic = forumTopicDao.findById(topicId).orElseThrow(BeanAssert.NOT_FOUND);
        BeanAssert.notDeleted(topic);

        group = groupDao.findByIdOrElseThrow(topic.getGroupId());
        BeanAssert.hasPermission(group.canViewResources(getUser()));

        posts = forumPostDao.findByTopicId(topicId);
        forumTopicDao.updateIncreaseViews(topicId);
        forumTopicDao.insertUserVisit(topicId, getUser().getId());

        topics = forumTopicDao.findByGroupId(group.getId());
    }

    public boolean isReplyDialog() {
        return dialogPost.getId() == 0;
    }

    public void saveDialogPost() {
        boolean replyDialog = isReplyDialog();

        if (replyDialog) {
            dialogPost.setUserId(getUser().getId());
            dialogPost.setTopicId(topicId);
        } else {
            dialogPost.setEditCount(dialogPost.getEditCount() + 1);
            dialogPost.setEditUserId(getUser().getId());
        }

        forumPostDao.save(dialogPost);

        if (replyDialog) {
            posts.add(dialogPost);
            forumTopicDao.updateIncreaseReplies(dialogPost.getTopicId(), dialogPost.getId(), dialogPost.getUserId(), dialogPost.getCreatedAt());
            dialogPost.getUser().incForumPostCount();
            log(Action.forum_post_added, group.getId(), topicId, topic.getTitle());
        } else {
            addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }

        dialogPost = new ForumPost();
    }

    public List<SelectItem> getCategories() {
        return ForumBean.getCategoriesByCourse(group.getCourseId(), getUserBean().getBundle());
    }

    public void deletePost(ForumPost post) {
        User user = getUser();
        if (user.isModerator() || user.getId() == post.getUserId()) {
            forumPostDao.delete(post.getId());
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

    public void quotePost(ForumPost post) {
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

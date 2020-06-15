package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class ForumPostBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 6077135964610986190L;
    // private static final Logger log = LogManager.getLogger(ForumPostBean.class);

    private int topicId;
    private List<ForumPost> posts;
    private ForumTopic topic;
    private Group group;
    private ForumPost newPost;
    private List<ForumTopic> topics;

    public ForumPostBean() {
        newPost = new ForumPost();
    }

    public void onLoad() throws SQLException {
        BeanAsserts.authorized(isLoggedIn());

        ForumManager fm = getLearnweb().getForumManager();
        topic = fm.getTopicById(topicId);
        BeanAsserts.validateNotNull(topic);

        posts = fm.getPostsBy(topicId);
        group = getLearnweb().getGroupManager().getGroupById(topic.getGroupId());
        topics = getLearnweb().getForumManager().getTopicsByGroup(group.getId());

        BeanAsserts.hasPermission(group.canViewResources(getUser()));

        fm.incViews(topicId);
        fm.updatePostVisitTime(topicId, getUser().getId());
    }

    public String onSavePost() throws SQLException {
        Date now = new Date();

        ForumManager fm = getLearnweb().getForumManager();
        topic = fm.save(topic);

        newPost.setUserId(getUser().getId());
        newPost.setDate(now);
        newPost.setTopicId(topicId);

        fm.save(newPost);
        posts.add(newPost);

        newPost = new ForumPost();
        log(Action.forum_post_added, group.getId(), topicId, topic.getTitle());
        return null;
    }

    public boolean isMember() throws SQLException {
        User user = getUser();

        if (null == user) {
            return false;
        }

        if (null == group) {
            return false;
        }

        return group.isMember(user);
    }

    public List<SelectItem> getCategories() {
        return getCategoriesByCourse(group.getCourseId());
    }

    public boolean canDeletePost(ForumPost obj) {
        User user = getUser();

        if (user.isModerator()) {
            return true;
        }

        if (user.getId() == obj.getUserId()) {
            return true;
        }

        return false;
    }

    public boolean canEditPost(ForumPost obj) {
        User user = getUser();
        return user.isAdmin() || user.getId() == obj.getUserId();
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

    public void quotePost(ForumPost post) throws SQLException {
        String username = post.getUser() != null ? post.getUser().getUsername() : "Anonymous"; // can happen for old imported posts
        String newStr = post.getText().replaceAll("<blockquote>", "<blockquote>&#160;&#160;&#160;&#160;");
        newPost.setText("<blockquote><strong>" + username + ":</strong>" + newStr + "</blockquote></br>");
    }

    public void newPost() throws SQLException {
        newPost.setText("");
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

    public ForumPost getNewPost() {
        return newPost;
    }

    public List<ForumTopic> getTopics() {
        return topics;
    }

    protected static List<SelectItem> getCategoriesByCourse(int courseId) {
        SelectItemGroup g1 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.1"));
        g1.setSelectItems(new SelectItem[] {new SelectItem("Forum.cell.category.1a", UtilBean.getLocaleMessage("Forum.cell.category.1a")), new SelectItem("Forum.cell.category.1b", UtilBean.getLocaleMessage("Forum.cell.category.1b")),
            new SelectItem("Forum.cell.category.1c", UtilBean.getLocaleMessage("Forum.cell.category.1c")), new SelectItem("Forum.cell.category.1d", UtilBean.getLocaleMessage("Forum.cell.category.1d"))});

        SelectItemGroup g2 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.2"));
        g2.setSelectItems(new SelectItem[] {new SelectItem("Forum.cell.category.2a", UtilBean.getLocaleMessage("Forum.cell.category.2a")), new SelectItem("Forum.cell.category.2b", UtilBean.getLocaleMessage("Forum.cell.category.2b")),
            new SelectItem("Forum.cell.category.2c", UtilBean.getLocaleMessage("Forum.cell.category.2c")), new SelectItem("Forum.cell.category.2d", UtilBean.getLocaleMessage("Forum.cell.category.2d")),
            new SelectItem("Forum.cell.category.2e", UtilBean.getLocaleMessage("Forum.cell.category.2e")), new SelectItem("Forum.cell.category.2f", UtilBean.getLocaleMessage("Forum.cell.category.2f")),
            new SelectItem("Forum.cell.category.2g", UtilBean.getLocaleMessage("Forum.cell.category.2g")), new SelectItem("Forum.cell.category.2h", UtilBean.getLocaleMessage("Forum.cell.category.2h"))});

        SelectItemGroup g3 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.3"));
        g3.setSelectItems(new SelectItem[] {new SelectItem("Forum.cell.category.3a", UtilBean.getLocaleMessage("Forum.cell.category.3a")), new SelectItem("Forum.cell.category.3b", UtilBean.getLocaleMessage("Forum.cell.category.3b")),
            new SelectItem("Forum.cell.category.3c", UtilBean.getLocaleMessage("Forum.cell.category.3c"))});

        ArrayList<SelectItem> categories = new ArrayList<>();
        categories.add(g1);
        categories.add(g2);
        categories.add(g3);

        return categories;
    }
}

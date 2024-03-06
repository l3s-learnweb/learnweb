package de.l3s.learnweb.forum;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class ForumBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 8303246537720508084L;

    private int groupId;
    private Group group;
    private transient List<ForumTopic> topics;

    @NotBlank
    @Size(max = 100)
    private String newTopicTitle;
    @NotBlank
    private String newTopicText;
    private String newTopicCategory;

    @Inject
    private GroupDao groupDao;

    @Inject
    private ForumPostDao forumPostDao;

    @Inject
    private ForumTopicDao forumTopicDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        group = groupDao.findByIdOrElseThrow(groupId);
        BeanAssert.hasPermission(group.canViewGroup(getUser()));

        topics = forumTopicDao.findByGroupId(groupId);
    }

    public String onSavePost() {
        ForumTopic topic = new ForumTopic();
        topic.setGroupId(groupId);
        topic.setTitle(newTopicTitle);
        topic.setUserId(getUser().getId());
        topic.setLastPostUserId(getUser().getId());
        forumTopicDao.save(topic);

        ForumPost post = new ForumPost();
        post.setUserId(getUser().getId());
        post.setText(newTopicText);
        post.setCategory(newTopicCategory);
        post.setTopicId(topic.getId());
        forumPostDao.save(post);

        forumTopicDao.updateIncreaseReplies(post.getTopicId(), post.getId(), post.getUserId(), post.getCreatedAt());
        post.getUser().incForumPostCount();

        log(Action.forum_topic_added, groupId, topic.getId(), newTopicTitle);
        return "/lw/group/forum_topic.jsf?faces-redirect=true&topic_id=" + topic.getId();
    }

    public void onDeleteTopic(ForumTopic topic) {
        forumTopicDao.delete(topic.getId());
        topics = forumTopicDao.findByGroupId(groupId);
        addMessage(FacesMessage.SEVERITY_INFO, "The topic '" + topic.getTitle() + "' has been deleted.");
    }

    public List<SelectItem> getCategories() {
        return getCategoriesByCourse(group.getCourseId(), getLocale());
    }

    public List<ForumTopic> getTopics() {
        return topics;
    }

    public Group getGroup() {
        return group;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getNewTopicTitle() {
        return newTopicTitle;
    }

    public void setNewTopicTitle(String newTopicTitle) {
        this.newTopicTitle = newTopicTitle;
    }

    public String getNewTopicText() {
        return newTopicText;
    }

    public void setNewTopicText(final String newTopicText) {
        this.newTopicText = newTopicText;
    }

    public String getNewTopicCategory() {
        return newTopicCategory;
    }

    public void setNewTopicCategory(final String newTopicCategory) {
        this.newTopicCategory = newTopicCategory;
    }

    protected static List<SelectItem> getCategoriesByCourse(int courseId, Locale locale) {
        ResourceBundle msg = MessagesBundle.of(locale);
        SelectItemGroup g1 = new SelectItemGroup(msg.getString("forum.field_category_1"));
        g1.setSelectItems(
            new SelectItem("forum.field_category_1a", msg.getString("forum.field_category_1a")),
            new SelectItem("forum.field_category_1b", msg.getString("forum.field_category_1b")),
            new SelectItem("forum.field_category_1c", msg.getString("forum.field_category_1c")),
            new SelectItem("forum.field_category_1d", msg.getString("forum.field_category_1d"))
        );

        SelectItemGroup g2 = new SelectItemGroup(msg.getString("forum.field_category_2"));
        g2.setSelectItems(
            new SelectItem("forum.field_category_2a", msg.getString("forum.field_category_2a")),
            new SelectItem("forum.field_category_2b", msg.getString("forum.field_category_2b")),
            new SelectItem("forum.field_category_2c", msg.getString("forum.field_category_2c")),
            new SelectItem("forum.field_category_2d", msg.getString("forum.field_category_2d")),
            new SelectItem("forum.field_category_2e", msg.getString("forum.field_category_2e")),
            new SelectItem("forum.field_category_2f", msg.getString("forum.field_category_2f")),
            new SelectItem("forum.field_category_2g", msg.getString("forum.field_category_2g")),
            new SelectItem("forum.field_category_2h", msg.getString("forum.field_category_2h"))
        );

        SelectItemGroup g3 = new SelectItemGroup(msg.getString("forum.field_category_3"));
        g3.setSelectItems(
            new SelectItem("forum.field_category_3a", msg.getString("forum.field_category_3a")),
            new SelectItem("forum.field_category_3b", msg.getString("forum.field_category_3b")),
            new SelectItem("forum.field_category_3c", msg.getString("forum.field_category_3c"))
        );

        return Arrays.asList(g1, g2, g3);
    }
}

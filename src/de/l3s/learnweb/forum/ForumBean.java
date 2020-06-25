package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class ForumBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 8303246537720508084L;
    //private static final Logger log = LogManager.getLogger(ForumBean.class);

    private int groupId;
    private Group group;
    private List<ForumTopic> topics;

    @NotBlank
    @Length(max = 100)
    private String newTopicTitle;
    @NotBlank
    private String newTopicText;
    private String newTopicCategory;

    public void onLoad() throws SQLException {
        BeanAsserts.authorized(isLoggedIn());

        group = getLearnweb().getGroupManager().getGroupById(groupId);
        BeanAsserts.groupNotNull(group);
        BeanAsserts.hasPermission(group.canViewResources(getUser()));

        topics = getLearnweb().getForumManager().getTopicsByGroup(groupId);
    }

    public String onSavePost() throws SQLException {
        Date now = new Date();
        ForumManager fm = getLearnweb().getForumManager();

        ForumTopic topic = new ForumTopic();
        topic.setGroupId(groupId);
        topic.setTitle(newTopicTitle);
        topic.setUserId(getUser().getId());
        topic.setDate(now);
        topic.setLastPostUserId(getUser().getId());
        topic.setLastPostDate(now);
        topic = fm.save(topic);

        ForumPost post = new ForumPost();
        post.setUserId(getUser().getId());
        post.setDate(now);
        post.setText(newTopicText);
        post.setCategory(newTopicCategory);
        post.setTopicId(topic.getId());
        fm.save(post);

        log(Action.forum_topic_added, groupId, topic.getId(), newTopicTitle);
        return "forum_post.jsf?faces-redirect=true&topic_id=" + topic.getId();
    }

    public void onDeleteTopic(ForumTopic topic) {
        try {
            getLearnweb().getForumManager().deleteTopic(topic);
            topics = getLearnweb().getForumManager().getTopicsByGroup(groupId);
            addMessage(FacesMessage.SEVERITY_INFO, "The topic '" + topic.getTitle() + "' has been deleted.");
        } catch (Exception e) {
            addErrorMessage(e);
        }
    }

    public List<SelectItem> getCategories() {
        return getCategoriesByCourse(group.getCourseId(), getUserBean().getLocale());
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
        SelectItemGroup g1 = new SelectItemGroup(LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.1"));
        g1.setSelectItems(new SelectItem[] {
            new SelectItem("Forum.cell.category.1a", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.1a")),
            new SelectItem("Forum.cell.category.1b", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.1b")),
            new SelectItem("Forum.cell.category.1c", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.1c")),
            new SelectItem("Forum.cell.category.1d", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.1d"))
        });

        SelectItemGroup g2 = new SelectItemGroup(LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2"));
        g2.setSelectItems(new SelectItem[] {
            new SelectItem("Forum.cell.category.2a", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2a")),
            new SelectItem("Forum.cell.category.2b", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2b")),
            new SelectItem("Forum.cell.category.2c", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2c")),
            new SelectItem("Forum.cell.category.2d", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2d")),
            new SelectItem("Forum.cell.category.2e", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2e")),
            new SelectItem("Forum.cell.category.2f", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2f")),
            new SelectItem("Forum.cell.category.2g", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2g")),
            new SelectItem("Forum.cell.category.2h", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.2h"))
        });

        SelectItemGroup g3 = new SelectItemGroup(LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.3"));
        g3.setSelectItems(new SelectItem[] {
            new SelectItem("Forum.cell.category.3a", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.3a")),
            new SelectItem("Forum.cell.category.3b", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.3b")),
            new SelectItem("Forum.cell.category.3c", LanguageBundle.getLocaleMessage(locale, "Forum.cell.category.3c"))
        });

        return Arrays.asList(g1, g2, g3);
    }
}

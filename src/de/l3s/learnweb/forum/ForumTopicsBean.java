package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class ForumTopicsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8303246537720508084L;
    private static final Logger log = LogManager.getLogger(ForumTopicsBean.class);

    private int groupId;
    private Group group;
    private List<ForumTopic> topics;

    @NotBlank
    @Length(max = 100)
    private String newTopicTitle;
    private ForumPost newPost;

    public ForumTopicsBean()
    {
        newPost = new ForumPost();
    }

    public void onLoad() throws SQLException
    {
        if(!isLoggedIn())
            return;

        group = getLearnweb().getGroupManager().getGroupById(groupId);

        if(null == group)
        {
            addInvalidParameterMessage("group_id");
            return;
        }
        if(!group.canViewResources(getUser()))
        {
            group = null; // set main object of this page to null to block access
            addAccessDeniedMessage();
            return;
        }

        topics = getLearnweb().getForumManager().getTopicsByGroup(groupId);
    }

    public String onSavePost() throws SQLException
    {
        Date now = new Date();

        ForumTopic topic = new ForumTopic();
        topic.setGroupId(groupId);
        topic.setTitle(newTopicTitle);
        topic.setUserId(getUser().getId());
        topic.setDate(now);
        topic.setLastPostUserId(getUser().getId());
        topic.setLastPostDate(now);

        ForumManager fm = getLearnweb().getForumManager();
        topic = fm.save(topic);

        newPost.setUserId(getUser().getId());
        newPost.setDate(now);
        newPost.setTopicId(topic.getId());

        fm.save(newPost);

        log(Action.forum_topic_added, groupId, topic.getId(), newTopicTitle);
        return "forum_post.jsf?faces-redirect=true&topic_id=" + topic.getId();
    }

    public void onDeleteTopic(ForumTopic topic)
    {
        try
        {
            getLearnweb().getForumManager().deleteTopic(topic);
            topics = getLearnweb().getForumManager().getTopicsByGroup(groupId);
            addMessage(FacesMessage.SEVERITY_INFO, "The topic '" + topic.getTitle() + "' has been deleted.");
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public List<SelectItem> getCategories()
    {
        return ForumPostBean.getCategoriesByCourse(group.getCourseId());
    }

    public List<ForumTopic> getTopics() throws SQLException
    {
        return topics;
    }

    public Group getGroup()
    {
        return group;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public ForumPost getNewPost()
    {
        return newPost;
    }

    public String getNewTopicTitle()
    {
        return newTopicTitle;
    }

    public void setNewTopicTitle(String newTopicTitle)
    {
        this.newTopicTitle = newTopicTitle;
    }
}

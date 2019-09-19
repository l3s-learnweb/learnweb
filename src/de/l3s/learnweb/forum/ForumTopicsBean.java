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

import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class ForumTopicsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8303246537720508084L;

    private final static Logger log = Logger.getLogger(ForumTopicsBean.class);

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

    public void preRenderView()
    {
        try
        {
            if(0 == groupId)
            {
                Integer id = getParameterInt("group_id");
                if(id != null)
                    groupId = id;
            }

            group = getLearnweb().getGroupManager().getGroupById(groupId);
            topics = getLearnweb().getForumManager().getTopicsByGroup(groupId);
        }
        catch(SQLException e1)
        {
            log.error("Cant load group", e1);
        }

        if(null == group)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid or no group id");
            return;
        }
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

package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;

@Named
@RequestScoped
public class WelcomeBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4337683111157393180L;

    // Filter for topics
    private static final Action[] TOPICS_FILTER = {
            Action.forum_topic_added
    };

    // Filter for posts
    private static final Action[] POSTS_FILTER = {
            Action.forum_post_added
    };

    // Filter for resources
    private static final Action[] RESOURCES_FILTER = {
            Action.adding_resource,
            Action.edit_resource,
    };

    private String welcomeMessage;
    private List<LogEntry> newsTopics;
    private List<LogEntry> newsPosts;
    private List<LogEntry> newsResources;

    public WelcomeBean()
    {
        User user = getUser();
        if(null == user)
            return;

        welcomeMessage = user.getOrganisation().getWelcomeMessage();
    }

    private List<LogEntry> getLogs(Action[] filter) throws SQLException
    {
        return getLearnweb().getLogManager().getActivityLogOfUserGroups(getUser().getId(), filter, 5);
    }

    public String getWelcomeMessage()
    {
        return welcomeMessage;
    }

    public List<LogEntry> getNewsTopics() throws SQLException
    {
        if(null == newsTopics)
        {
            newsTopics = getLogs(TOPICS_FILTER);
        }
        return newsTopics;
    }

    public List<LogEntry> getNewsResources() throws SQLException
    {
        if(null == newsResources)
        {
            newsResources = getLogs(RESOURCES_FILTER);
        }
        return newsResources;
    }

    public List<LogEntry> getNewsPosts() throws SQLException
    {
        if(null == newsPosts)
        {
            newsPosts = getLogs(POSTS_FILTER);
        }
        return newsPosts;
    }
}

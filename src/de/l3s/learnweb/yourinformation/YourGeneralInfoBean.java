package de.l3s.learnweb.yourinformation;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;

/**
 * GeneralInfoBean is responsible for displaying user statistics on index page, e.g. amount of groups, in which user is a
 * member.
 */
@Named
@ViewScoped
public class YourGeneralInfoBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 3786761818878931646L;
    private static final Logger log = Logger.getLogger(YourGeneralInfoBean.class);

    private String username;
    private int userCoursesCount;
    private int userGroupsCount;
    private int userPostsCount;
    private int userResourcesCount;
    private int receivedMessagesCount;
    private int sentMessagesCount;
    private int submissionsCount;

    public YourGeneralInfoBean() throws SQLException
    {
        User user = getUser();
        if(null == user) // when not logged in
            return;

        this.username = this.getUser().getUsername();

        this.userCoursesCount = CollectionUtils.size(this.getUser().getCourses());

        try
        {
            this.userGroupsCount = this.getUser().getGroupCount();
        }
        catch(SQLException sqlException)
        {
            log.error("Can't retrieve amount of user groups properly ", sqlException);
        }

        try
        {
            this.userPostsCount = this.getUser().getForumPostCount();
        }
        catch(SQLException sqlException)
        {
            log.error("Can't retrieve amount of user posts properly ", sqlException);
        }

        try
        {
            this.userResourcesCount = this.getUser().getResourceCount();
        }
        catch(SQLException sqlException)
        {
            log.error("Can't retrieve amount of user resources properly ", sqlException);
        }

        try
        {
            this.receivedMessagesCount = CollectionUtils.size(Message.getAllMessagesToUser(this.getUser()));
        }
        catch(SQLException sqlException)
        {
            log.error("Problem with fetching messages of user", sqlException);
        }

        try
        {
            this.sentMessagesCount = CollectionUtils.size(Message.getAllMessagesFromUser(this.getUser()));
        }
        catch(SQLException sqlException)
        {
            log.error("Problem with fetching messages of user", sqlException);
        }

        try
        {
            this.submissionsCount = CollectionUtils.size(this.getLearnweb().getSubmissionManager().getSubmissionsByUser(this.getUser()));
        }
        catch(SQLException sqlException)
        {
            log.error("Problem with fetching submissions of user", sqlException);
        }

    }

    public String getUsername()
    {
        return this.username;
    }

    public String getUserCoursesCount()
    {
        if(0 == this.userCoursesCount)
        {
            return "no";
        }
        else
        {
            return String.valueOf(this.userCoursesCount);
        }
    }

    public String getUserGroupsCount()
    {
        if(0 == this.userGroupsCount)
        {
            return "no";
        }
        else
        {
            return String.valueOf(this.userGroupsCount);
        }
    }

    public String getUserPostsCount()
    {
        if(0 == this.userPostsCount)
        {
            return "no";
        }
        else
        {
            return String.valueOf(this.userPostsCount);
        }
    }

    public String getUserResourcesCount()
    {
        if(0 == this.userResourcesCount)
        {
            return "no";
        }
        else
        {
            return String.valueOf(this.userResourcesCount);
        }
    }

    public String getReceivedMessagesCount()
    {
        if(0 == this.receivedMessagesCount)
        {
            return "no";
        }
        else
        {
            return String.valueOf(this.receivedMessagesCount);
        }
    }

    public String getSentMessagesCount()
    {
        if(0 == this.sentMessagesCount)
        {
            return "no";
        }
        else
        {
            return String.valueOf(this.sentMessagesCount);
        }
    }

    public String getSubmissionsCount()
    {
        if(0 == this.submissionsCount)
        {
            return "no";
        }
        else
        {
            return String.valueOf(this.submissionsCount);
        }
    }
}

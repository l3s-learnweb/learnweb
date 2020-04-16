package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;

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
    //private static final Logger log = LogManager.getLogger(YourGeneralInfoBean.class);

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
        // when not logged in
        if(null == user)
            return;

        this.username = user.getUsername();
        this.userCoursesCount = CollectionUtils.size(user.getCourses());
        this.userGroupsCount = user.getGroupCount();
        this.userPostsCount = user.getForumPostCount();
        this.userResourcesCount = user.getResourceCount();
        this.receivedMessagesCount = CollectionUtils.size(Message.getAllMessagesToUser(user));
        this.sentMessagesCount = CollectionUtils.size(Message.getAllMessagesFromUser(user));
        this.submissionsCount = CollectionUtils.size(this.getLearnweb().getSubmissionManager().getSubmissionsByUser(user));
    }

    public String getUsername()
    {
        return this.username;
    }

    public int getUserCoursesCount()
    {
        return this.userCoursesCount;
    }

    public int getUserGroupsCount()
    {
        return this.userGroupsCount;
    }

    public int getUserPostsCount()
    {
        return this.userPostsCount;
    }

    public int getUserResourcesCount()
    {
        return this.userResourcesCount;
    }

    public int getReceivedMessagesCount()
    {
        return this.receivedMessagesCount;
    }

    public int getSentMessagesCount()
    {
        return this.sentMessagesCount;
    }

    public int getSubmissionsCount()
    {
        return this.submissionsCount;
    }
}

package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

/*
* GeneralinfoBean is responsible for displaying user statistics on index page, e.g. amount of groups, in which user is a
* member.
* */
@ManagedBean(name = "yourGeneralInfoBean", eager = true)
@SessionScoped
public class YourGeneralInfoBean extends ApplicationBean implements Serializable {
    protected static final Logger logger = Logger.getLogger(YourGeneralInfoBean.class);

    private String username;
    private int userCoursesNum;
    private int userGroupsNum;
    private int userPostsNum;
    private int userResourcesNum;
    private int receivedMessagesNum;
    private int sentMessagesNum;

    protected User user;

    public YourGeneralInfoBean(){
        this.user = this.getUser();

        this.username = user.getUsername();
        try {
            this.userCoursesNum = user.getCourses().size();
        } catch(SQLException sqlException) {
            this.userCoursesNum = 0;
            logger.error("Can't retrieve amount of user courses properly " + sqlException);
        }
        try {
            this.userGroupsNum = user.getGroupCount();
        } catch(SQLException sqlException) {
            this.userGroupsNum = 0;
            logger.error("Can't retrieve amount of user groups properly " + sqlException);
        }
        try {
            this.userPostsNum = user.getForumPostCount();
        } catch(SQLException sqlException) {
            this.userPostsNum = 0;
            logger.error("Can't retrieve amount of user posts properly " + sqlException);
        }
        try {
            this.userResourcesNum = user.getResourceCount();
        } catch(SQLException sqlException) {
            this.userResourcesNum = 0;
            logger.error("Can't retrieve amount of user resources properly " + sqlException);
        }
        try {
            receivedMessagesNum = Message.getAllMessagesToUser(this.user).size();
        } catch(SQLException sqlException){
            receivedMessagesNum = 0;
            logger.error("Problem with fetching messages of user" + sqlException);
        }
        try {
            sentMessagesNum = Message.getAllMessagesFromUser(this.user).size();
        } catch(SQLException sqlException){
            sentMessagesNum = 0;
            logger.error("Problem with fetching messages of user" + sqlException);
        }
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String userFullName)
    {
        this.username = userFullName;
    }

    public int getUserCoursesNum()
    {
        return userCoursesNum;
    }

    public void setUserCoursesNum(final int userCoursesNum)
    {
        this.userCoursesNum = userCoursesNum;
    }

    public int getUserGroupsNum()
    {
        return userGroupsNum;
    }

    public void setUserGroupsNum(final int userGroupsNum)
    {
        this.userGroupsNum = userGroupsNum;
    }

    public int getUserPostsNum()
    {
        return userPostsNum;
    }

    public void setUserPostsNum(final int userPostsNum)
    {
        this.userPostsNum = userPostsNum;
    }

    public int getUserResourcesNum()
    {
        return userResourcesNum;
    }

    public void setUserResourcesNum(final int userResourcesNum)
    {
        this.userResourcesNum = userResourcesNum;
    }

    public int getReceivedMessagesNum()
    {
        return receivedMessagesNum;
    }

    public void setReceivedMessagesNum(final int receivedMessagesNum)
    {
        this.receivedMessagesNum = receivedMessagesNum;
    }

    public int getSentMessagesNum()
    {
        return sentMessagesNum;
    }

    public void setSentMessagesNum(final int sentMessagesNum)
    {
        this.sentMessagesNum = sentMessagesNum;
    }
}

package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * GeneralinfoBean is responsible for displaying user statistics on index page, e.g. amount of groups, in which user is a
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


    public YourGeneralInfoBean()
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;


        this.username = this.getUser().getUsername();

        try
        {
            try
            {
                this.userCoursesCount = this.getUser().getCourses().size();
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocaleMessage("No courses list found ", null), null));
            }
            catch(NullPointerException npe)
            {
                // when for some reason user exists out of course
                this.addErrorMessage("No courses list found ", npe);
            }
        }
        catch(SQLException sqlException)
        {
            log.error("Can't retrieve amount of user courses properly ", sqlException);
        }

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
            try
            {
                this.receivedMessagesCount = Message.getAllMessagesToUser(this.getUser()).size();
            }
            catch(NullPointerException npe)
            {
                // when user haven't received any messages
                this.addErrorMessage("No messages list found ", npe);
            }
        }
        catch(SQLException sqlException)
        {
            log.error("Problem with fetching messages of user", sqlException);
        }

        try
        {
            try
            {
                this.sentMessagesCount = Message.getAllMessagesFromUser(this.getUser()).size();
            }
            catch(NullPointerException npe)
            {
                // when user haven't sent any messages
                this.addErrorMessage("No messages list found ", npe);
            }
        }
        catch(SQLException sqlException)
        {
            log.error("Problem with fetching messages of user", sqlException);
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
}

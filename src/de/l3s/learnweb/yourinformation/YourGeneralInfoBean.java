package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Message;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;

/*
* GeneralinfoBean is responsible for displaying user statistics on index page, e.g. amount of groups, in which user is a
* member.
* */
@Named
@ViewScoped
public class YourGeneralInfoBean extends ApplicationBean implements Serializable {
    private static final Logger logger = Logger.getLogger(YourGeneralInfoBean.class);

    public YourGeneralInfoBean() { }

    public String getUsername() {
        return this.getUser().getUsername();
    }

    public int getUserCoursesNum() {
        try {
            return this.getUser().getCourses().size();
        }
        catch(SQLException sqlException) {
            logger.error("Can't retrieve amount of user courses properly " + sqlException);
            return 0;
        }
    }

    public int getUserGroupsNum() {
        try {
            return this.getUser().getGroupCount();
        }
        catch(SQLException sqlException) {
            logger.error("Can't retrieve amount of user groups properly " + sqlException);
            return 0;
        }
    }

    public int getUserPostsNum() {
        try {
            return this.getUser().getForumPostCount();
        }
        catch(SQLException sqlException) {
            logger.error("Can't retrieve amount of user posts properly " + sqlException);
            return 0;
        }
    }

    public int getUserResourcesNum() {
        try {
            return this.getUser().getResourceCount();
        }
        catch(SQLException sqlException) {
            logger.error("Can't retrieve amount of user resources properly " + sqlException);
            return 0;
        }
    }

    public int getReceivedMessagesNum() {
        try {
            return Message.getAllMessagesToUser(this.getUser()).size();
        }
        catch(SQLException sqlException) {
            logger.error("Problem with fetching messages of user" + sqlException);
            return 0;
        }
    }

    public int getSentMessagesNum() {
        try {
            return Message.getAllMessagesFromUser(this.getUser()).size();
        }
        catch(SQLException sqlException) {
            logger.error("Problem with fetching messages of user" + sqlException);
            return 0;
        }
    }
}

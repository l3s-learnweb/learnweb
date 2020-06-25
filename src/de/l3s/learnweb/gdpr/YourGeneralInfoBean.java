package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;

/**
 * GeneralInfoBean is responsible for displaying user statistics on index page, e.g. amount of groups, in which user is a
 * member.
 */
@Named
@ViewScoped
public class YourGeneralInfoBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 3786761818878931646L;
    //private static final Logger log = LogManager.getLogger(YourGeneralInfoBean.class);

    private int receivedMessagesCount;
    private int sentMessagesCount;
    private int submissionsCount;

    public YourGeneralInfoBean() throws SQLException {
        User user = getUser();
        BeanAsserts.authorized(user);

        this.receivedMessagesCount = CollectionUtils.size(Message.getAllMessagesToUser(user));
        this.sentMessagesCount = CollectionUtils.size(Message.getAllMessagesFromUser(user));
        this.submissionsCount = CollectionUtils.size(this.getLearnweb().getSubmissionManager().getSubmissionsByUser(user));
    }

    public int getReceivedMessagesCount() {
        return this.receivedMessagesCount;
    }

    public int getSentMessagesCount() {
        return this.sentMessagesCount;
    }

    public int getSubmissionsCount() {
        return this.submissionsCount;
    }
}

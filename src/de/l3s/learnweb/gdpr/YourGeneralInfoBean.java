package de.l3s.learnweb.gdpr;

import java.io.Serial;
import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.user.MessageDao;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

/**
 * GeneralInfoBean is responsible for displaying user statistics on index page, e.g. amount of groups, in which user is a
 * member.
 */
@Named
@ViewScoped
public class YourGeneralInfoBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 3786761818878931646L;
    //private static final Logger log = LogManager.getLogger(YourGeneralInfoBean.class);

    private int receivedMessagesCount;
    private int sentMessagesCount;
    private int submissionsCount;

    @Inject
    private MessageDao messageDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        this.receivedMessagesCount = messageDao.findIncoming(user).size();
        this.sentMessagesCount = messageDao.findOutgoing(user).size();
        this.submissionsCount = dao().getSubmissionDao().countByCourseIds(HasId.collectIds(user.getCourses()));
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

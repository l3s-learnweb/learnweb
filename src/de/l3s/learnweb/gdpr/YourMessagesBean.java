package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.MessageDao;
import de.l3s.learnweb.user.User;

/**
 * MessagesBean is responsible for displaying user messages.
 */
@Named
@ViewScoped
public class YourMessagesBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 9183874194970002045L;
    //private static final Logger log = LogManager.getLogger(YourMessagesBean.class);

    private List<Message> receivedMessages;
    private List<Message> sentMessages;

    @Inject
    private MessageDao messageDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        this.receivedMessages = messageDao.findIncoming(user);
        this.sentMessages = messageDao.findOutgoing(user);
    }

    public List<Message> getMessagesToUser() {
        return this.receivedMessages;
    }

    public List<Message> getMessagesFromUser() {
        return this.sentMessages;
    }
}

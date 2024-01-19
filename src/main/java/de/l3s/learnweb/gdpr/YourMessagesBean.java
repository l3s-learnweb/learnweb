package de.l3s.learnweb.gdpr;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
    @Serial
    private static final long serialVersionUID = 9183874194970002045L;

    private transient List<Message> receivedMessages;
    private transient List<Message> sentMessages;

    @Inject
    private MessageDao messageDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        this.receivedMessages = messageDao.findIncoming(user);
        this.sentMessages = messageDao.findOutgoing(user);
    }

    public List<Message> getReceivedMessages() {
        return this.receivedMessages;
    }

    public List<Message> getSentMessages() {
        return this.sentMessages;
    }
}

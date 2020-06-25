package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.user.Message;
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

    public YourMessagesBean() throws SQLException {
        User user = getUser();
        BeanAsserts.authorized(user);

        this.receivedMessages = Message.getAllMessagesToUser(user);
        this.sentMessages = Message.getAllMessagesFromUser(user);
    }

    public List<Message> getMessagesToUser() {
        return this.receivedMessages;
    }

    public List<Message> getMessagesFromUser() {
        return this.sentMessages;
    }
}

package de.l3s.learnweb.gdpr.beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * MessagesBean is responsible for displaying user messages.
 */
@Named
@ViewScoped
public class YourMessagesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 9183874194970002045L;
    private static final Logger log = Logger.getLogger(YourMessagesBean.class);

    private List<Message> receivedMessages;
    private List<Message> sentMessages;

    public YourMessagesBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        this.receivedMessages = Message.getAllMessagesToUser(user);
        this.sentMessages = Message.getAllMessagesFromUser(user);
    }

    public List<Message> getMessagesToUser()
    {
        return this.receivedMessages;
    }

    public List<Message> getMessagesFromUser()
    {
        return this.sentMessages;
    }
}

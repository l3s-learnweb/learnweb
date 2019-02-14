package de.l3s.learnweb.yourinformation;

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

    public YourMessagesBean()
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        try
        {
            this.receivedMessages = Message.getAllMessagesToUser(this.getUser());
        }
        catch(SQLException sqlException)
        {
            log.error("Problem with fetching messages of user", sqlException);
        }

        try
        {
            this.sentMessages = Message.getAllMessagesFromUser(this.getUser());
        }
        catch(SQLException sqlException)
        {
            log.error("Problem with fetching messages of user", sqlException);
        }
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

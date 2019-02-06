package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.user.Message;

import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * MessagesBean is responsible for displaying user messages.
 * */
@ManagedBean(name = "yourMessagesBean", eager = true)
@SessionScoped
public class YourMessagesBean extends YourGeneralInfoBean implements Serializable {
    private List<Message> messagesToUser;
    private List<Message> messagesFromUser;

    public YourMessagesBean(){
        try {
            messagesToUser = Message.getAllMessagesToUser(this.user);
        } catch(SQLException sqlException){
            messagesToUser = new ArrayList<>();
            logger.error("Problem with fetching messages of user" + sqlException);
        }
        try {
            messagesFromUser = Message.getAllMessagesFromUser(this.user);
        } catch(SQLException sqlException){
            messagesFromUser = new ArrayList<>();
            logger.error("Problem with fetching messages of user" + sqlException);
        }
    }

    public List<Message> getMessagesToUser()
    {
        return messagesToUser;
    }

    public void setMessagesToUser(final List<Message> messagesToUser)
    {
        this.messagesToUser = messagesToUser;
    }

    public List<Message> getMessagesFromUser()
    {
        return messagesFromUser;
    }

    public void setMessagesFromUser(final List<Message> messagesFromUser)
    {
        this.messagesFromUser = messagesFromUser;
    }
}

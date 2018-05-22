package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;

@ManagedBean
@RequestScoped
public class NotificationReaderBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6231162839099220868L;
    private static final Logger log = Logger.getLogger(NotificationReaderBean.class);

    private ArrayList<Message> receivedMessages;
    private String howManyNewMessages;

    public NotificationReaderBean()
    {
        User user = UtilBean.getUserBean().getUser();
        Message message = new Message();
        try
        {
            receivedMessages = message.getAllMessagesToUser(user);
        }
        catch(Exception e)
        {
            log.error("unhandled error", e);
        }

    }

    public ArrayList<Message> getReceivedMessages() throws SQLException
    {

        Message.setAllMessagesSeen(getUser().getId());

        return receivedMessages;
    }

    public String getHowManyNewMessages() throws SQLException
    {
        int i = Message.howManyNotSeenMessages(getUser());

        if(i == 0)
            howManyNewMessages = "0";
        else
            howManyNewMessages = "" + i;

        return howManyNewMessages;
    }

}

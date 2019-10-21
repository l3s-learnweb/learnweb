package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.inject.Named;
import javax.faces.view.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class MessageBean extends ApplicationBean implements Serializable // TODO refactor complete message system and the update this class
{
    private static final long serialVersionUID = 6231162839099220868L;
    private static final Logger log = Logger.getLogger(MessageBean.class);

    private ArrayList<Message> receivedMessages;
    private Integer howManyNewMessages;

    public MessageBean()
    {

    }

    public  void setAllMessagesSeen() throws SQLException
    {
        Message.setAllMessagesSeen(getUser().getId());
    }

    public ArrayList<Message> getReceivedMessages() throws SQLException
    {
        if(receivedMessages == null)
        {
            try
            {
                receivedMessages = Message.getAllMessagesToUser(getUser());
            }
            catch(Exception e)
            {
                log.error("unhandled error", e);
            }
        }
        return receivedMessages;
    }

    public Integer getHowManyNewMessages() throws SQLException
    {
        if(howManyNewMessages == null)
        {
            howManyNewMessages = Message.howManyNotSeenMessages(getUser());
        }
        return howManyNewMessages;
    }

}

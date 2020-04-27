package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class MessageBean extends ApplicationBean implements Serializable // TODO refactor complete message system and the update this class
{
    private static final long serialVersionUID = 6231162839099220868L;
    private static final Logger log = LogManager.getLogger(MessageBean.class);

    private ArrayList<Message> receivedMessages;
    private Integer howManyNewMessages;

    public MessageBean()
    {

    }

    public void onLoad() throws SQLException
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

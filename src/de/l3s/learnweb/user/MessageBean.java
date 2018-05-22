package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@ManagedBean
@ViewScoped
public class MessageBean extends ApplicationBean implements Serializable // TODO refactor complete message system and the update this class
{
    private static final long serialVersionUID = 6231162839099220868L;
    private static final Logger log = Logger.getLogger(MessageBean.class);

    private ArrayList<Message> receivedMessages;
    private String howManyNewMessages;

    public MessageBean()
    {

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

    public String getHowManyNewMessages() throws SQLException
    {
        if(howManyNewMessages == null)
        {
            int i = Message.howManyNotSeenMessages(getUser());

            if(i == 0)
                howManyNewMessages = "0";
            else
                howManyNewMessages = "" + i;
        }
        return howManyNewMessages;
    }

}

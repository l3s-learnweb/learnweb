package de.l3s.learnwebBeans;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Message;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
@RequestScoped
public class NotificationReaderBean extends ApplicationBean
{

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
            e.printStackTrace();
        }

    }
    /*	
    public boolean getAreThereNewMessages(){
    	int count = 0;
    
    	User user = getUser();
    	if(user==null){
    		return false;
    	}
    	try{
    		count = Message.howManyNotSeenMessages(user);
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	
    	return (count == 0) ? false : true;
    }
    
    
    
    public void allSeen(){
    	try{
    	for(Message message : receivedMessages){
    		message.seen();
    	}
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }
    */

    public ArrayList<Message> getReceivedMessages() throws SQLException
    {

        Message.setAllMessagesSeen(getUser().getId());

        return receivedMessages;
    }

    /*
    public void setReceivedMessages(ArrayList<Message> receivedMessages) {
    	this.receivedMessages = receivedMessages;
    }
    
    
    public Message getSelectedMessage() {
    	return selectedMessage;
    }
    
    
    public void setSelectedMessage(Message selectedMessage) {
    	this.selectedMessage = selectedMessage;
    }
    */
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

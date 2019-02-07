package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Message;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * MessagesBean is responsible for displaying user messages.
 * */
@Named
@ViewScoped
public class YourMessagesBean extends ApplicationBean implements Serializable {
    private static final Logger logger = Logger.getLogger(YourMessagesBean.class);

    public YourMessagesBean() { }

    public List<Message> getMessagesToUser() {
        try {
            return Message.getAllMessagesToUser(this.getUser());
        }
        catch(SQLException sqlException) {
            logger.error("Problem with fetching messages of user" + sqlException);
            return new ArrayList<>();
        }
    }

    public List<Message> getMessagesFromUser() {
        try {
            return Message.getAllMessagesFromUser(this.getUser());
        }
        catch(SQLException sqlException) {
            logger.error("Problem with fetching messages of user" + sqlException);
            return new ArrayList<>();
        }
    }
}

package de.l3s.learnweb.user;

import java.io.Serializable;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class MessageBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 6231162839099220868L;
    private static final Logger log = LogManager.getLogger(MessageBean.class);

    private List<Message> receivedMessages;
    private Integer howManyNewMessages;

    @Inject
    private MessageDao messageDao;

    public void onLoad() {
        messageDao.updateMarkReadAll(getUser());
    }

    public List<Message> getReceivedMessages() {
        if (receivedMessages == null) {
            receivedMessages = messageDao.findIncoming(getUser());
        }
        return receivedMessages;
    }

    public Integer getHowManyNewMessages() {
        if (howManyNewMessages == null) {
            howManyNewMessages = messageDao.countNotSeen(getUser());
        }
        return howManyNewMessages;
    }

}

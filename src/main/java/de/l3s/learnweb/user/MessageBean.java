package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class MessageBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 6231162839099220868L;
    //private static final Logger log = LogManager.getLogger(MessageBean.class);

    private List<Message> receivedMessages;
    private Integer howManyNewMessages;

    @Inject
    private MessageDao messageDao;

    public void onLoad() {
        messageDao.updateMarkSeenAll(getUser());
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

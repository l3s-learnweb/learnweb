package de.l3s.learnweb.user;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.forum.ForumNotificator;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;

@Named
@ViewScoped
public class UnsubscribeBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -6855126832857385223L;

    private String givenHash;
    private User user;

    @Inject
    private GroupDao groupDao;

    @Inject
    private UserDao userDao;

    public void onLoad() {
        try {
            int userId = Integer.parseInt(givenHash.substring(0, givenHash.indexOf(':')));
            user = userDao.findById(userId);
        } catch (RuntimeException ignored) {
            // ignore all parsing problems
        }

        BeanAssert.validate(user, "Invalid value of 'hash' parameter.");

        String correctHash = ForumNotificator.getHash(user);
        BeanAssert.validate(correctHash.equals(givenHash), "Invalid value of 'hash' parameter.");
    }

    public String getHash() {
        return givenHash;
    }

    public void setHash(final String hash) {
        this.givenHash = hash;
    }

    public void onUnsubscribe() {
        user.setPreferredNotificationFrequency(User.NotificationFrequency.NEVER);
        user.save();

        for (Group group : user.getGroups()) {
            groupDao.updateNotificationFrequency(User.NotificationFrequency.NEVER, group.getId(), user.getId());
        }
        addMessage(FacesMessage.SEVERITY_INFO, "notification_settings.unsubscribed");
    }
}

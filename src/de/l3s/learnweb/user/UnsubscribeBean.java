package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.forum.ForumNotificator;
import de.l3s.learnweb.group.Group;

@Named
@ViewScoped
public class UnsubscribeBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -6855126832857385223L;

    private String givenHash;
    private User user;

    public void onLoad() throws SQLException {
        try {
            int userId = Integer.parseInt(givenHash.substring(0, givenHash.indexOf(':')));
            user = getLearnweb().getUserManager().getUser(userId);
        } catch (RuntimeException | SQLException ignored) {
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

    public void onUnsubscribe() throws SQLException {
        user.setPreferredNotificationFrequency(User.NotificationFrequency.NEVER);
        user.save();

        for (Group group : user.getGroups()) {
            getLearnweb().getGroupManager().updateNotificationFrequency(group.getId(), user.getId(), User.NotificationFrequency.NEVER);
        }
        addMessage(FacesMessage.SEVERITY_INFO, "notification_settings.unsubscribed");
    }
}

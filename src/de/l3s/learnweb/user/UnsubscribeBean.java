package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.forum.ForumNotificator;
import de.l3s.learnweb.group.Group;

@Named
@ViewScoped
public class UnsubscribeBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6855126832857385223L;

    private String givenHash;
    private User user;
    private boolean hashValid = false; // indicates if the given hash value is correct

    public void onLoad() throws SQLException
    {
        try
        {
            int userId = Integer.parseInt(givenHash.substring(0, givenHash.indexOf(":")));
            user = getLearnweb().getUserManager().getUser(userId);
        }
        catch(Exception e)
        {
            // ignore all parsing problems
        }

        if(user == null)
        {
            addInvalidParameterMessage("hash");
            return;
        }

        String correctHash = ForumNotificator.getHash(user);

        if(correctHash.equals(givenHash))
        {
            hashValid = true;
        }
        else
        {
            addInvalidParameterMessage("hash");
        }
    }

    public String getHash()
    {
        return givenHash;
    }

    public void setHash(final String hash)
    {
        this.givenHash = hash;
    }

    public void onUnsubscribe() throws SQLException
    {
        if(!hashValid)
            return;

        user.setPreferredNotificationFrequency(User.NotificationFrequency.NEVER);
        user.save();

        for(Group group : user.getGroups())
        {
            getLearnweb().getGroupManager().updateNotificationFrequency(group.getId(), user.getId(), User.NotificationFrequency.NEVER);
        }
        addMessage(FacesMessage.SEVERITY_INFO, "notification_settings.unsubscribed");
    }

    public boolean isHashValid()
    {
        return hashValid;
    }
}

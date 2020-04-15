package de.l3s.learnweb.group;

import java.io.Serializable;

import de.l3s.learnweb.user.User;

/**
 * Contains metadata of the group <-> user relation, equivalent to the lw_group_user table.
 * Right now only the notificationFrequency is need. Other values might be added on demand.
 *
 *
 */
public class GroupUser implements Serializable
{
    private static final long serialVersionUID = 7474061527247978561L;

    private final Group group;
    private User.NotificationFrequency notificationFrequency;

    GroupUser(Group group, User.NotificationFrequency notificationFrequency)
    {
        this.group = group;
        this.notificationFrequency = notificationFrequency;
    }

    public Group getGroup()
    {
        return group;
    }

    public User.NotificationFrequency getNotificationFrequency()
    {
        return notificationFrequency;
    }

    public void setNotificationFrequency(final User.NotificationFrequency notificationFrequency)
    {
        this.notificationFrequency = notificationFrequency;
    }
}

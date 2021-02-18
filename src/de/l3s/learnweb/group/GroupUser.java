package de.l3s.learnweb.group;

import java.io.Serializable;
import java.time.LocalDateTime;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;

/**
 * Contains metadata of the group <-> user relation, equivalent to the lw_group_user table.
 * Right now only the notificationFrequency is need. Other values might be added on demand.
 */
public class GroupUser implements Serializable {
    private static final long serialVersionUID = 7474061527247978561L;

    private int groupId;
    private int userId;
    private LocalDateTime joinTime;
    private LocalDateTime lastVisit;
    private User.NotificationFrequency notificationFrequency;

    private transient Group group;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(final int groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() {
        if (group == null) {
            group = Learnweb.dao().getGroupDao().findById(groupId);
        }
        return group;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(final LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }

    public LocalDateTime getLastVisit() {
        return lastVisit;
    }

    public void setLastVisit(final LocalDateTime lastVisit) {
        this.lastVisit = lastVisit;
    }

    public User.NotificationFrequency getNotificationFrequency() {
        return notificationFrequency;
    }

    public void setNotificationFrequency(final User.NotificationFrequency notificationFrequency) {
        this.notificationFrequency = notificationFrequency;
    }
}

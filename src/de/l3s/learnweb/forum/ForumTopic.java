package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.time.LocalDateTime;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;
import de.l3s.util.Deletable;

public class ForumTopic implements Serializable, Deletable {
    private static final long serialVersionUID = 5370327788969640983L;

    private int id;
    private int userId;
    private int groupId;
    private String title;
    private LocalDateTime date;
    private int views;
    private int replies;
    private int lastPostId;
    private LocalDateTime lastPostDate;
    private int lastPostUserId;
    private boolean deleted;

    // cached values
    private transient Group group;
    private transient User user;
    private transient User lastPostUser;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public User getUser() {
        if (user == null) {
            user = Learnweb.dao().getUserDao().findById(userId);
        }
        return user;
    }

    public Group getGroup() {
        if (group == null) {
            group = Learnweb.dao().getGroupDao().findById(groupId);
        }
        return group;
    }

    public User getLastPostUser() {
        if (lastPostUser == null && lastPostUserId != 0) {
            lastPostUser = Learnweb.dao().getUserDao().findById(lastPostUserId);
        }
        return lastPostUser;
    }

    public int getLastPostUserId() {
        return lastPostUserId;
    }

    public void setLastPostUserId(int lastPostUserId) {
        this.lastPostUserId = lastPostUserId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getReplies() {
        return replies;
    }

    public void setReplies(int replies) {
        this.replies = replies;
    }

    public int getLastPostId() {
        return lastPostId;
    }

    public void setLastPostId(int lastPostId) {
        this.lastPostId = lastPostId;
    }

    public LocalDateTime getLastPostDate() {
        return lastPostDate;
    }

    public void setLastPostDate(LocalDateTime lastPostDate) {
        this.lastPostDate = lastPostDate;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "ForumTopic [id=" + id + ", userId=" + userId + ", groupId=" + groupId + ", title=" + title + ", date=" + date + ", views=" + views
            + ", replies=" + replies + ", lastPostId=" + lastPostId + ", lastPostDate=" + lastPostDate + "]";
    }
}

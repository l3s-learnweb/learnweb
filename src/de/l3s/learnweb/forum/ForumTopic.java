package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

public class ForumTopic implements Serializable {
    private static final long serialVersionUID = 5370327788969640983L;

    private int id = -1;
    private int userId;
    private int groupId;
    private String title;
    private Date date;
    private int views;
    private int replies;
    private int lastPostId;
    private Date lastPostDate;
    private int lastPostUserId;
    private boolean deleted;
    // cached values
    private transient User user;
    private transient User lastPostUser;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public User getUser() throws SQLException {
        if (user == null) {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public User getLastPostUser() throws SQLException {
        if (lastPostUser == null) {
            lastPostUser = Learnweb.getInstance().getUserManager().getUser(lastPostUserId);
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
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

    public Date getLastPostDate() {
        return lastPostDate;
    }

    public void setLastPostDate(Date lastPostDate) {
        this.lastPostDate = lastPostDate;
    }

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

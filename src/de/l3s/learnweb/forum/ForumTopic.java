package de.l3s.learnweb.forum;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;
import de.l3s.util.Deletable;

public class ForumTopic implements Serializable, Deletable {
    @Serial
    private static final long serialVersionUID = 5370327788969640983L;

    private int id;
    private int userId;
    private int groupId;
    private String title;
    private int views;
    private int replies;
    private int lastPostId;
    private int lastPostUserId;
    private boolean deleted;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

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
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public Group getGroup() {
        if (group == null) {
            group = Learnweb.dao().getGroupDao().findByIdOrElseThrow(groupId);
        }
        return group;
    }

    public User getLastPostUser() {
        if (lastPostUser == null && lastPostUserId != 0) {
            lastPostUser = Learnweb.dao().getUserDao().findByIdOrElseThrow(lastPostUserId);
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

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ForumTopic [id=" + id + ", userId=" + userId + ", groupId=" + groupId + ", title=" + title + ", date=" + createdAt + ", views=" + views
            + ", replies=" + replies + ", lastPostId=" + lastPostId + ", lastPostDate=" + updatedAt + "]";
    }
}

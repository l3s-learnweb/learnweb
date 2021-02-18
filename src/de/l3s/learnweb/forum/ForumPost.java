package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.Deletable;

public class ForumPost implements Serializable, Deletable {
    private static final long serialVersionUID = 4093915855537221830L;

    private int id = -1;
    private int userId;
    private int topicId;
    @NotBlank
    private String text;
    private LocalDateTime date;
    private int editCount;
    private LocalDateTime lastEditDate;
    private Integer editUserId;
    private boolean deleted;
    private String category;

    // cached values
    private transient User user;
    private transient User editUser;

    public boolean canEditPost(User user) {
        if (user == null) {
            return false;
        }

        if (user.isAdmin() || user.getId() == userId) {
            return true;
        }

        return false;
    }

    public boolean canDeletePost(User user) {
        if (user == null) {
            return false;
        }

        if (user.isModerator() || user.getId() == userId) {
            return true;
        }

        return false;
    }

    public int getId() {
        return id;
    }

    public void setId(int postId) {
        this.id = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
        if (this.lastEditDate == null) {
            this.lastEditDate = date;
        }
    }

    public int getEditCount() {
        return editCount;
    }

    public void setEditCount(int editCount) {
        this.editCount = editCount;
    }

    public LocalDateTime getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(LocalDateTime lastEditDate) {
        this.lastEditDate = lastEditDate;
    }

    public Integer getEditUserId() {
        return editUserId;
    }

    public void setEditUserId(Integer editUserId) {
        this.editUserId = editUserId;
    }

    public User getUser() {
        if (user == null) {
            user = Learnweb.dao().getUserDao().findById(userId);
        }
        return user;
    }

    public User getEditUser() {
        if (editUser == null) {
            editUser = Learnweb.dao().getUserDao().findById(editUserId);
        }
        return editUser;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
        return "ForumPost [id=" + id + ", userId=" + userId + ", topicId=" + topicId + ", text=" + text + ", date=" + date + ", editCount=" + editCount
            + ", lastEditDate=" + lastEditDate + ", editUserId=" + editUserId + "]";
    }
}

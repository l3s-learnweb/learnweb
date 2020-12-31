package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.Deletable;

public class ForumPost implements Serializable, Deletable {
    private static final long serialVersionUID = 4093915855537221830L;

    private int id;
    private int userId;
    private int topicId;
    @NotBlank
    private String text;
    private int editCount;
    private int editUserId;
    private boolean deleted;
    private String category;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

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

    public int getEditCount() {
        return editCount;
    }

    public void setEditCount(int editCount) {
        this.editCount = editCount;
    }

    public int getEditUserId() {
        return editUserId;
    }

    public void setEditUserId(int editUserId) {
        this.editUserId = editUserId;
    }

    public User getUser() {
        if (user == null) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public User getEditUser() {
        if (editUser == null && editUserId != 0) {
            editUser = Learnweb.dao().getUserDao().findByIdOrElseThrow(editUserId);
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ForumPost [id=" + id + ", userId=" + userId + ", topicId=" + topicId + ", text=" + text + ", date=" + createdAt + ", editCount=" + editCount
            + ", lastEditDate=" + updatedAt + ", editUserId=" + editUserId + "]";
    }
}

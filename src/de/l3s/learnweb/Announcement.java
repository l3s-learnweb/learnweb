package de.l3s.learnweb;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;

public final class Announcement implements Serializable {
    private static final long serialVersionUID = 4219676681480459859L;

    private int id;
    private int userId;
    private boolean hidden;
    @NotBlank
    private String title;
    @NotBlank
    private String text;
    private LocalDateTime createdAt;

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Announcement [id=" + id +
            ", title=" + title +
            ", message=" + text +
            ", created_at=" + createdAt +
            ", userId=" + userId +
            ", hidden=" + hidden + "]";
    }
}

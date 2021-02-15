package de.l3s.learnweb;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;

public final class Announcement implements Serializable {
    private static final long serialVersionUID = 4219676681480459859L;

    private int id;
    @NotBlank
    private String title;
    @NotBlank
    private String text;
    private LocalDateTime date;
    private boolean hidden;
    private int userId;

    public int getUserId() {
        return userId;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(final LocalDateTime date) {
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
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

    public void setText(final String text) {
        this.text = text;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public String toString() {
        return "Announcement [id=" + id +
            ", title=" + title +
            ", message=" + text +
            ", created_at=" + date +
            ", userId=" + userId +
            ", hidden=" + hidden + "]";
    }
}

package de.l3s.learnweb.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.HasId;

public class Message implements Comparable<Message>, Serializable, HasId {
    private static final long serialVersionUID = -5510804242529450186L;

    private int id;
    private int senderUserId;
    private int recipientUserId;
    private String title;
    private String text;
    private boolean seen = false;
    private LocalDateTime createdAt;

    private transient User senderUser;
    private transient User recipientUser;

    @Override
    public int compareTo(Message g) {
        return this.toString().compareTo(g.toString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Message message = (Message) o;
        return id == message.id && senderUserId == message.senderUserId && recipientUserId == message.recipientUserId && seen == message.seen
            && title.equals(message.title) && text.equals(message.text) && createdAt.equals(message.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, senderUserId, recipientUserId, title, text, seen, createdAt);
    }

    @Override
    public String toString() {
        return String.format("%s -> %s %s: %s", getSenderUser().getUsername(), getRecipientUser().getUsername(), createdAt, title);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(final int senderUserId) {
        this.senderUserId = senderUserId;
    }

    public User getSenderUser() {
        if (senderUser == null && senderUserId != 0) {
            senderUser = Learnweb.dao().getUserDao().findByIdOrElseThrow(senderUserId);
        }
        return senderUser;
    }

    public void setSenderUser(User senderUser) {
        this.senderUserId = senderUser.getId();
        this.senderUser = senderUser;
    }

    public int getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(final int recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public User getRecipientUser() {
        if (recipientUser == null && recipientUserId != 0) {
            recipientUser = Learnweb.dao().getUserDao().findByIdOrElseThrow(recipientUserId);
        }
        return recipientUser;
    }

    public void setRecipientUser(User recipientUser) {
        this.recipientUserId = recipientUser.getId();
        this.recipientUser = recipientUser;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package de.l3s.learnweb.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.HasId;

public class Message implements Comparable<Message>, Serializable, HasId {
    private static final long serialVersionUID = -5510804242529450186L;

    private int id;
    private int fromUserId;
    private int toUserId;
    private String title;
    private String text;
    private boolean seen = false;
    private boolean read = false;
    private LocalDateTime time;

    private transient User fromUser;
    private transient User toUser;

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
        return id == message.id && fromUserId == message.fromUserId && toUserId == message.toUserId && seen == message.seen && read == message.read
            && title.equals(message.title) && text.equals(message.text) && time.equals(message.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromUserId, toUserId, title, text, seen, read, time);
    }

    @Override
    public String toString() {
        return String.format("%s -> %s%n%s: %n%s", fromUser.getUsername(), toUser.getUsername(), time, title);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(final int fromUserId) {
        this.fromUserId = fromUserId;
    }

    public User getFromUser() {
        if (fromUser == null) {
            fromUser = Learnweb.dao().getUserDao().findByIdOrElseThrow(fromUserId);
        }
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUserId = fromUser.getId();
        this.fromUser = fromUser;
    }

    public int getToUserId() {
        return toUserId;
    }

    public void setToUserId(final int toUserId) {
        this.toUserId = toUserId;
    }

    public User getToUser() {
        if (toUser == null) {
            toUser = Learnweb.dao().getUserDao().findByIdOrElseThrow(toUserId);
        }
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUserId = toUser.getId();
        this.toUser = toUser;
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

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}

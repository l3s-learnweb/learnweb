package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

public class Comment implements Serializable, Comparable<Comment>, HasId {
    private static final long serialVersionUID = -5854582234222584285L;
    private int id;
    private String text;
    private LocalDateTime date = LocalDateTime.now();
    private int userId;
    private int resourceId;

    private transient Resource resource;
    private transient User user;

    public Comment() {
    }

    public Comment(String text, LocalDateTime date, Resource resource, User user) {
        this.text = text;
        this.date = date;

        setResource(resource);
        setUser(user);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
    }

    public Resource getResource() {
        if (null == resource && resourceId != 0) {
            resource = Learnweb.dao().getResourceDao().findByIdOrElseThrow(resourceId);
        }
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;

        if (resource != null) {
            this.resourceId = resource.getId();
        }
    }

    public User getUser() {
        if (null == user && userId != 0) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public void setUser(User user) {
        this.user = user;

        if (user != null) {
            this.userId = user.getId();
        }
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
        this.user = null; //force reload
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
        setResource(null); //force reload
    }

    @Override
    public int compareTo(Comment comment) {
        return comment.getDate().compareTo(this.getDate());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Comment comment = (Comment) o;
        return id == comment.id && userId == comment.userId && resourceId == comment.resourceId
            && Objects.equals(text, comment.text) && Objects.equals(date, comment.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, date, userId, resourceId);
    }
}

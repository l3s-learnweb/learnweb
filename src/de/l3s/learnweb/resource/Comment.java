package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

public class Comment implements Serializable, Comparable<Comment>
{
    private static final long serialVersionUID = -5854582234222584285L;
    private int id = -1;
    private String text;
    private Date date = new Date();
    private int userId = -1;
    private int resourceId = -1;

    private transient Resource resource;
    private transient User user;

    public Comment()
    {
    }

    public Comment(String text, Date date, Resource resource, User user)
    {
        this.text = text;
        this.date = date;

        setResource(resource);
        setUser(user);
    }

    public int getId()
    {
        return id;
    }

    public String getText()
    {
        return text;
    }

    public Date getDate()
    {
        return date;
    }

    public Resource getResource() throws SQLException
    {
        if(null == resource && resourceId != -1)
        {
            resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);
        }
        return resource;
    }

    public User getUser() throws SQLException
    {
        if(null == user && userId != -1)
        {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
        this.user = null; //force reload
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
        setResource(null); //force reload
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public void setResource(Resource resource)
    {
        this.resource = resource;

        if(resource != null)
            this.resourceId = resource.getId();
    }

    public void setUser(User user)
    {
        this.user = user;

        if(user != null)
            this.userId = user.getId();
    }

    @Override
    public int compareTo(Comment comment)
    {
        return comment.getDate().compareTo(this.getDate());
    }

    @Override
    public boolean equals(final Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        final Comment comment = (Comment) o;
        return id == comment.id &&
                userId == comment.userId &&
                resourceId == comment.resourceId &&
                Objects.equals(text, comment.text) &&
                Objects.equals(date, comment.date);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, text, date, userId, resourceId);
    }
}

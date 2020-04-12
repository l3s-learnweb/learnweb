package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

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
        this.resource = resource;
        if(null != resource)
            this.resourceId = resource.getId();
        this.user = user;
        if(null != user)
            this.userId = user.getId();
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
        this.resource = null; //force reload
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
        this.resourceId = resource.getId();
    }

    public void setUser(User user)
    {
        this.user = user;
        this.userId = user.getId();
    }

    @Override
    public int compareTo(Comment c)
    {
        return c.getDate().compareTo(this.getDate());
    }
}

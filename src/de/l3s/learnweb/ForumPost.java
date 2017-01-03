package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import org.hibernate.validator.constraints.NotEmpty;

public class ForumPost implements Serializable
{
    private static final long serialVersionUID = 4093915855537221830L;

    private int id = -1;
    private int userId;
    private int topicId;
    @NotEmpty
    private String text;
    private Date date;
    private int editCount;
    private Date lastEditDate;
    private int editUserId;

    private String category;

    // cached value
    private transient User user;
    private transient User editUser;

    public int getId()
    {
        return id;
    }

    public void setId(int postId)
    {
        this.id = postId;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public int getTopicId()
    {
        return topicId;
    }

    public void setTopicId(int topicId)
    {
        this.topicId = topicId;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
        if(lastEditDate == null)
            this.lastEditDate = date;
    }

    public int getEditCount()
    {
        return editCount;
    }

    public void setEditCount(int editCount)
    {
        this.editCount = editCount;
    }

    public Date getLastEditDate()
    {
        return lastEditDate;
    }

    public void setLastEditDate(Date lastEditDate)
    {
        this.lastEditDate = lastEditDate;
    }

    public int getEditUserId()
    {
        return editUserId;
    }

    public void setEditUserId(int editUserId)
    {
        this.editUserId = editUserId;
    }

    public User getUser() throws SQLException
    {
        if(user == null)
        {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public User getEditUser() throws SQLException
    {
        if(editUser == null)
        {
            editUser = Learnweb.getInstance().getUserManager().getUser(editUserId);
        }
        return editUser;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    @Override
    public String toString()
    {
        return "ForumPost [id=" + id + ", userId=" + userId + ", topicId=" + topicId + ", text=" + text + ", date=" + date + ", editCount=" + editCount + ", lastEditDate=" + lastEditDate + ", editUserId=" + editUserId + "]";
    }
}

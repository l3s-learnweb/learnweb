package de.l3s.learnweb;

import java.util.Date;

public class ForumPost
{
    private int id = -1;
    private int userId;
    private int topicId;
    private int groupId;
    private String text;
    private Date date;
    private int editCount;
    private Date lastEditDate;
    private int editUserId;

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

    public int getGroupId()
    {
	return groupId;
    }

    public void setGroupId(int groupId)
    {
	this.groupId = groupId;
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

}

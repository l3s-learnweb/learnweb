package de.l3s.learnweb;

import java.util.Date;

public class ForumTopic
{
    private int userId;
    private int topicId = -1;
    private int groupId;
    private String topic;
    private Date date;
    private int topicView;
    private int topicReplies;
    private int topicLastPostId;

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

    public String getTopic()
    {
	return topic;
    }

    public void setTopic(String topic)
    {
	this.topic = topic;
    }

    public Date getDate()
    {
	return date;
    }

    public void setDate(Date date)
    {
	this.date = date;
    }

    public int getTopicView()
    {
	return topicView;
    }

    public void setTopicView(int topicView)
    {
	this.topicView = topicView;
    }

    public int getTopicReplies()
    {
	return topicReplies;
    }

    public void setTopicReplies(int topicReplies)
    {
	this.topicReplies = topicReplies;
    }

    public int getTopicLastPostId()
    {
	return topicLastPostId;
    }

    public void setTopicLastPostId(int topicLastPostId)
    {
	this.topicLastPostId = topicLastPostId;
    }

}

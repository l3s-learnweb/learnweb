package de.l3s.learnweb;

import java.io.Serializable;
import java.util.Date;

//import org.ocpsoft.pretty.time.PrettyTime;

import de.l3s.learnweb.LogEntry.Action;

public class NewsEntry implements Comparable<NewsEntry>, Serializable
{
    private static final long serialVersionUID = -8039903365395519945L;
    private User user;
    private Resource resource;
    private Integer comments;
    private Integer tags;
    private String text;
    private Boolean resourceAction;
    private Date date;
    private Action newsAction;

    //variable for checking if the item is a resource oriented action or user oriented

    public NewsEntry(LogEntry news, User user, Resource resource, Integer comments, Integer tags, String text, Boolean resourceAction, Date date)
    {
	super();
	this.user = user;
	this.resource = resource;
	this.comments = comments;
	this.tags = tags;
	this.text = text;
	this.resourceAction = resourceAction;
	this.date = date;
	this.newsAction = news.getAction();
    }

    public User getUser()
    {
	return user;
    }

    public Resource getResource()
    {
	return resource;
    }

    public Integer getComments()
    {
	return comments;
    }

    public Integer getTags()
    {
	return tags;
    }

    public String getText()
    {
	return text;
    }

    public Boolean getResourceAction()
    {
	return resourceAction;
    }

    public boolean isQueryNeeded()
    {
	if(newsAction == Action.adding_resource && resource != null && !resource.getQuery().equalsIgnoreCase("none"))
	    return true;
	return false;
    }

    public Date getDate()
    {
	return date;
    }

    @Override
    public int compareTo(NewsEntry o)
    {
	return -getDate().compareTo(o.getDate());
    }
}

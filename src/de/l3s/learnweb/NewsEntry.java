package de.l3s.learnweb;

import java.io.Serializable;
import java.util.Date;

//import org.ocpsoft.pretty.time.PrettyTime;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.UtilBean;

public class NewsEntry implements Comparable<NewsEntry>, Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -8039903365395519945L;
    private LogEntry news;
    private User user;
    private Resource resource;
    private Integer comments;
    private Integer tags;
    private String text;
    private Boolean resourceAction;
    private Date date;

    //	private String datedifference;
    //	private boolean isQueryNeeded;
    public NewsEntry()
    {
	// TODO Auto-generated constructor stub
    }

    public NewsEntry(LogEntry news)
    {
	super();

	this.news = news;

    }

    //variable for checking if the item is a resource oriented action or user oriented

    public NewsEntry(LogEntry news, User user, Resource resource, Integer comments, Integer tags, String text, Boolean resourceAction, Date date)
    {
	super();
	this.news = news;
	this.user = user;
	this.resource = resource;
	this.comments = comments;
	this.tags = tags;
	this.text = text;
	this.resourceAction = resourceAction;
	this.date = date;
	//		this.datedifference=null;

    }

    public LogEntry getNews()
    {
	return news;
    }

    public void setNews(LogEntry news)
    {
	this.news = news;
    }

    public User getUser()
    {
	return user;
    }

    public void setUser(User user)
    {
	this.user = user;
    }

    public Resource getResource()
    {
	return resource;
    }

    public void setResource(Resource resource)
    {
	this.resource = resource;
    }

    public Integer getComments()
    {
	return comments;
    }

    public void setComments(Integer comments)
    {
	this.comments = comments;
    }

    public Integer getTags()
    {
	return tags;
    }

    public void setTags(Integer tags)
    {
	this.tags = tags;
    }

    public String getText()
    {
	return text;
    }

    public void setText(String text)
    {
	this.text = text;
    }

    public Boolean getResourceAction()
    {
	return resourceAction;
    }

    public void setResourceAction(Boolean resourceAction)
    {
	this.resourceAction = resourceAction;
    }

    public boolean isQueryNeeded()
    {
	if(news.getAction() == Action.adding_resource && resource != null && !resource.getQuery().equalsIgnoreCase("none"))
	    return true;
	return false;
    }

    public Date getDate()
    {
	return date;
    }

    public void setDate(Date date)
    {
	this.date = date;
    }

    public String getDatedifference()
    {
	return UtilBean.getUserBean().getPrettyDate(date);
	// T ODO PrettyTime p= new PrettyTime();

	//	return date.toLocaleString();// p.format(date);
    }

    @Override
    public int compareTo(NewsEntry o)
    {
	return -getDate().compareTo(o.getDate());
    }

}

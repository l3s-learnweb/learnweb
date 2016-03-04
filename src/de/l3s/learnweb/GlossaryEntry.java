package de.l3s.learnweb;

import java.sql.SQLException;
import java.util.Date;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

public class GlossaryEntry
{
    private int id; // auto incremented id
    private int resourceId;
    private int userId;

    @NotEmpty
    private String item;
    private String description;
    private String topic;
    private String italian;
    private String german;
    private String spanish;
    private Date lastModified;

    // cached values
    private transient User user;

    @URL
    private String reference;

    public Date getLastModified()
    {
	return lastModified;
    }

    public void setLastModified(Date lastModified)
    {
	this.lastModified = lastModified;
    }

    public String getReference()
    {
	return reference;
    }

    public void setReference(String reference)
    {
	this.reference = reference;
    }

    public String getTopic()
    {
	return topic;
    }

    public void setTopic(String topic)
    {
	this.topic = topic;
    }

    public String getItalian()
    {
	return italian;
    }

    public void setItalian(String italian)
    {
	this.italian = italian;
    }

    public String getGerman()
    {
	return german;
    }

    public void setGerman(String german)
    {
	this.german = german;
    }

    public String getSpanish()
    {
	return spanish;
    }

    public void setSpanish(String spanish)
    {
	this.spanish = spanish;
    }

    public String getItem()
    {
	return item;
    }

    public void setItem(String item)
    {
	this.item = item;
    }

    public String getDescription()
    {
	return description;
    }

    public void setDescription(String description)
    {
	this.description = description;
    }

    public User getUser() throws SQLException
    {
	if(user == null)
	{
	    user = Learnweb.getInstance().getUserManager().getUser(userId);
	}
	return user;
    }

    public void setUser(User user)
    {
	this.user = user;
	this.userId = user.getId();
    }

    public int getId()
    {
	return id;
    }

    public void setId(int id)
    {
	this.id = id;
    }

    public int getResourceId()
    {
	return resourceId;
    }

    public void setResourceId(int resourceId)
    {
	this.resourceId = resourceId;
    }

    public int getUserId()
    {
	return userId;
    }

    public void setUserId(int userId)
    {
	this.userId = userId;
	this.user = null;
    }

}

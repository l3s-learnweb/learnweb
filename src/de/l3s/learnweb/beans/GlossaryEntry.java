package de.l3s.learnweb.beans;

import java.util.Date;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

import de.l3s.learnweb.User;

public class GlossaryEntry
{
    @NotEmpty
    private String item;
    private String description;
    private String topic;
    private String italian;
    private String german;
    private String spanish;
    private User user;
    private Date lastModified;

    @URL
    private String reference;
    private String addby;

    public Date getLastModified()
    {
	return lastModified;
    }

    public void setLastModified(Date lastModified)
    {
	this.lastModified = lastModified;
    }

    public String getAddby()
    {
	return addby;
    }

    public void setAddby(String addby)
    {
	this.addby = addby;
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

    public User getUser()
    {
	return user;
    }

    public void setUser(User user)
    {
	this.user = user;
    }

}

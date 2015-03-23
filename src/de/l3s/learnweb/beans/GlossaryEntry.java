package de.l3s.learnweb.beans;

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

public class GlossaryEntry
{
    private String item;
    private String description;
    private String topic;
    private String italian;
    private String german;
    private String spanish;
    private String reference;
    private String addby;

    public void linkvalidator(FacesContext ctx, UIComponent component, Object value) throws ValidatorException
    {
	if(value instanceof String)
	{
	    String urlValue = (String) value;

	    if(!urlValue.startsWith("http://") || !urlValue.startsWith("www."))
	    {
		throw new ValidatorException(new FacesMessage("The reference isn't a URL. Please change your input.", null));
	    }
	}
    }

    public Date getLastmodified()
    {
	Date lastmodified = new Date();
	return lastmodified;
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

}

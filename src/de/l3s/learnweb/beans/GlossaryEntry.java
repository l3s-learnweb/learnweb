package de.l3s.learnweb.beans;

public class GlossaryEntry
{
    private String item;
    private String description;
    private String topic;
    private String italian;
    private String german;
    private String spanish;
    private String reference;

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

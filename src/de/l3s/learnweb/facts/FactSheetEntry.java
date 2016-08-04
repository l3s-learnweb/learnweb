package de.l3s.learnweb.facts;

import java.util.List;

public class FactSheetEntry
{
    private String label;
    private List<Object> data;
    private String template;

    public String getLabel()
    {
	return label;
    }

    public void setLabel(String label)
    {
	this.label = label;
    }

    public List<Object> getData()
    {
	return data;
    }

    public void setData(List<Object> data)
    {
	this.data = data;
    }

    public String getTemplate()
    {
	return template;
    }

    public void setTemplate(String template)
    {
	this.template = template;
    }

}

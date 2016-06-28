package de.l3s.learnweb;

import java.util.Arrays;
import java.util.List;

public class Fact
{

    String label;
    String template;
    List<Object> values;

    public Fact(String label, String template, List<Object> values)
    {
	super();
	this.label = label;
	this.template = template;
	this.values = values;
    }

    public Fact(String label, String template, Object... values)
    {
	super();
	this.label = label;
	this.template = template;
	this.values = Arrays.asList(values);
    }

    public String getLabel()
    {
	return label;
    }

    public String getTemplate()
    {
	return template;
    }

    public List<Object> getValues()
    {
	return values;
    }

}

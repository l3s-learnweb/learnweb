package de.l3s.archivedemo;

public class Entity
{

    private String label;
    private String uri;

    public String getLabel()
    {
	return label;
    }

    public void setLabel(String label)
    {
	this.label = label;
    }

    public String getUri()
    {
	return uri;
    }

    public void setUri(String uri)
    {
	uri = uri.substring(uri.lastIndexOf("/") + 1);
	this.uri = uri;
    }

}

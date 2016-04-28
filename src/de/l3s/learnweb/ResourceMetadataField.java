package de.l3s.learnweb;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class ResourceMetadataField implements Serializable
{
    private static final long serialVersionUID = -7698089608547415349L;

    public enum MetadataType
    { // represents primeface input types
	INPUT_TEXT,
	ONE_MENU
    }

    private String name; // the name of this field, will be used as SOLR column name and as label on the website 
    private MetadataType type;
    private List<String> options = new LinkedList<String>(); // default options for some input types like OneMenu
    private boolean moderatorOnly = false; // only admins and moderators have write access

    public ResourceMetadataField(String name, MetadataType type)
    {
	super();
	this.name = name;
	this.type = type;
    }

    public ResourceMetadataField(String name, MetadataType type, boolean moderatorOnly)
    {
	super();
	this.name = name;
	this.type = type;
	this.moderatorOnly = moderatorOnly;
    }

    public String getName()
    {
	return name;
    }

    public void setName(String name)
    {
	this.name = name;
    }

    public MetadataType getType()
    {
	return type;
    }

    public void setType(MetadataType type)
    {
	this.type = type;
    }

    public List<String> getOptions()
    {
	return options;
    }

    public void setOptions(List<String> options)
    {
	this.options = options;
    }

    public boolean isModeratorOnly()
    {
	return moderatorOnly;
    }

    public void setModeratorOnly(boolean moderatorOnly)
    {
	this.moderatorOnly = moderatorOnly;
    }

}

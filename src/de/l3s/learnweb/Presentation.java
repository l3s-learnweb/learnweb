package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Presentation implements Serializable
{
    private static final long serialVersionUID = 8403450212340352749L;
    private int groupId;
    private int ownerId;
    private int presentationId;
    private String presentationName;
    private String code;
    private Date date;
    private String presentationTitle;
    private List<Resource> resources;

    public Presentation()
    {
	presentationId = -1;
	resources = new ArrayList<Resource>();
    }

    public void parseCode()
    {
	if(!code.isEmpty())
	{
	    Document doc = Jsoup.parse(code);
	    Elements slides = doc.getElementsByClass("slide");
	    Resource r = new Resource();
	    String lastId = "";
	    ResourceManager resourceManager = Learnweb.getInstance().getResourceManager();

	    for(Element slide : slides)
	    {
		if(slide.id().equals("resource_slide_1"))
		{
		    presentationTitle = slide.getElementById("presentation_title").text();
		}
		else
		{
		    try
		    {
			if(!lastId.contains(slide.id()))
			{
			    int id = Integer.parseInt(slide.id().replace("resource_slide_", ""));
			    r = resourceManager.getResource(id);
			    if(r == null)
				continue;
			    r = r.clone();
			    r.setId(id);
			    r.setTitle(slide.children().first().html());
			    r.setDescription(slide.children().last().getElementsByTag("td").last().html());
			    r.setSource("Previous Presentation");
			    resources.add(r);
			}
			else
			{
			    r.setTitle(slide.children().first().html());
			    r.setDescription(slide.children().last().html());
			    r.setSource("Previous Presentation");
			    resources.add(r);
			}
		    }
		    catch(SQLException e)
		    {
			e.printStackTrace();
		    }
		    catch(NumberFormatException ne)
		    {
			try
			{
			    lastId = slide.id();
			    int id = Integer.parseInt(slide.id().replace("resource_slide_", "").replace("_iframe", ""));
			    r = Learnweb.getInstance().getResourceManager().getResource(id).clone();
			    r.setId(id);
			}
			catch(SQLException se)
			{
			    se.printStackTrace();
			}
		    }
		}
	    }
	}
    }

    public User getOwner()
    {
	User owner = new User();
	try
	{
	    owner = Learnweb.getInstance().getUserManager().getUser(ownerId);
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}
	return owner;
    }

    public int getGroupId()
    {
	return groupId;
    }

    public void setGroupId(int groupId)
    {
	this.groupId = groupId;
    }

    public int getOwnerId()
    {
	return ownerId;
    }

    public void setOwnerId(int ownerId)
    {
	this.ownerId = ownerId;
    }

    public int getPresentationId()
    {
	return presentationId;
    }

    public void setPresentationId(int presentationId)
    {
	this.presentationId = presentationId;
    }

    public String getCode()
    {
	return code;
    }

    public void setCode(String code)
    {
	this.code = code;
    }

    public String getPresentationName()
    {
	return presentationName;
    }

    public void setPresentationName(String presentationName)
    {
	this.presentationName = presentationName;
    }

    public String getPresentationTitle()
    {
	return presentationTitle;
    }

    public void setPresentationTitle(String presentationTitle)
    {
	this.presentationTitle = presentationTitle;
    }

    public List<Resource> getResources()
    {
	return resources;
    }

    public void setResources(List<Resource> resources)
    {
	this.resources = resources;
    }

    public Date getDate()
    {
	return date;
    }

    public void setDate(Date date)
    {
	this.date = date;
    }

}

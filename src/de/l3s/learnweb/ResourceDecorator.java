package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.util.StringHelper;

/**
 * 
 * @author Kemkes
 * 
 *         This class wraps a resource. It's necessary because a resource can appear in various search results with different search terms. Which lead
 *         to different text snippets.
 * 
 */
public class ResourceDecorator implements Serializable
{
    private static final long serialVersionUID = -6611930555147350248L;
    private Resource resource;
    private int tempId;
    private String snippet;
    private boolean newResource; //Used to highlight new resources when we compare the current result set with a result set from a similar query posted earlier
    private int rankAtService; // the rank which the resource had at its original service (youtube, flickr...)
    private String title;

    public ResourceDecorator(Resource resource)
    {
	this.resource = resource;
	newResource = false;
    }

    public int getTempId()
    {
	return tempId;
    }

    public void setTempId(int tempId)
    {
	this.tempId = tempId;
    }

    public String getSnippet()
    {
	return snippet;
    }

    public String getShortSnippet()
    {
	return Jsoup.clean(StringHelper.shortnString(getSnippet(), 80), Whitelist.none());
	// return resource.getShorterDescription();
    }

    public void setSnippet(String snippet)
    {
	this.snippet = snippet;
    }

    public Resource getResource()
    {
	return resource;
    }

    // Convenience methods which call the underlying resource

    public String getLearnwebUrl()
    {
	return resource.getLearnwebUrl();
    }

    /**
     * The title with highlighted search terms
     * 
     * @return
     */
    public String getTitle()
    {
	if(title != null)
	    return title;

	return resource.getTitle();
    }

    public void setTitle(String title)
    {
	this.title = title;
    }

    public Thumbnail getThumbnail0()
    {
	return resource.getThumbnail0();
    }

    public Thumbnail getThumbnail1()
    {
	return resource.getThumbnail1();
    }

    public Thumbnail getThumbnail2()
    {
	return resource.getThumbnail2();
    }

    public Thumbnail getThumbnail2b()
    {
	return resource.getThumbnail2b();
    }

    public Thumbnail getThumbnail2c()
    {
	return resource.getThumbnail2c();
    }

    public Thumbnail getThumbnail3()
    {
	return resource.getThumbnail3();
    }

    public Thumbnail getThumbnail4()
    {
	return resource.getThumbnail4();
    }

    public String getLocation()
    {
	return resource.getLocation();
    }

    public String getSource()
    {
	return resource.getSource();
    }

    public String getDescription()
    {
	return resource.getDescription();
    }

    public String getDescriptionHTML()
    {
	return resource.getDescriptionHTML();
    }

    public String getEmbedded()
    {
	return resource.getEmbedded();
    }

    public String getUrl()
    {
	return resource.getUrl();
    }

    public String getDurationInMinutes()
    {
	return resource.getDurationInMinutes();
    }

    public List<Group> getGroups() throws SQLException
    {
	return resource.getGroups();
    }

    public boolean isNewResource()
    {
	return newResource;
    }

    public void setNewResource(boolean newResource)
    {
	this.newResource = newResource;
    }

    /**
     * The rank this resource had at its source service
     * 
     * @return
     */
    public int getRankAtService()
    {
	return rankAtService;
    }

    public void setRankAtService(int rankAtService)
    {
	this.rankAtService = rankAtService;
    }

}

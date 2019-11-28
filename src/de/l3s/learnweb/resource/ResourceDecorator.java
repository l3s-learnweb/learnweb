package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.yellMetadata.ExtendedMetadata;
import de.l3s.learnweb.user.User;
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
    private int rank;
    private String snippet;
    // the rank which the resource has in the current search result
    private String title;

    public ResourceDecorator(Resource resource)
    {
        this.resource = resource;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int rank)
    {
        this.rank = rank;
    }

    /**
     * the rank which the resource has in the current search result
     *
     * @return
     */

    public String getSnippet()
    {
        return snippet;
    }

    public String getShortSnippet()
    {
        return Jsoup.clean(StringHelper.shortnString(getSnippet(), 80), Whitelist.none());
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

    public String getServiceIcon()
    {
        return resource.getServiceIcon();
    }

    public String getLearnwebUrl() throws SQLException
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

    public String getMetadataValue(String key)
    {
        return resource.getMetadataValue(key);
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

    public ResourceService getSource()
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

    public String getUrlProxied()
    {
        return resource.getUrlProxied();
    }

    public String getDurationInMinutes()
    {
        return resource.getDurationInMinutes();
    }

    public Group getGroup() throws SQLException
    {
        return resource.getGroup();
    }

    public int getId()
    {
        return resource.getId();
    }

    public String getType()
    {
        return resource.getType().toString();
    }

    public boolean isArchived()
    {
        return resource.isArchived();
    }

    public ArchiveUrl getFirstArchivedObject()
    {
        return resource.getFirstArchivedObject();
    }

    public ArchiveUrl getLastArchivedObject()
    {
        return resource.getLastArchivedObject();
    }

    public User getUser() throws SQLException
    {
        return resource.getUser();
    }

    public String getFileName()
    {
        return resource.getFileName();
    }

    public double getStarRating()
    {
        return resource.getStarRating();
    }

    public int getRateNumber()
    {
        return resource.getRateNumber();
    }

    public int getRatingSum()
    {
        return resource.getRatingSum();
    }

    //getters for new variables for extended metadata (Chloe)

    public String getLanguage()
    {
        return resource.getLanguage();
    }

    public Boolean isEditLocked()
    {
        return resource.isEditLocked();
    }

    public ExtendedMetadata getExtendedMetadata() throws SQLException
    {
        return resource.getExtendedMetadata();
    }

    @Override
    public String toString()
    {
        return "ResourceDecorator [resource=" + resource + ", rank=" + rank + ", snippet=" + snippet + ", title=" + title + "]";
    }

}

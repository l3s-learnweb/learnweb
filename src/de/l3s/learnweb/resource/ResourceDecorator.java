package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.group.Group;
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
    private int tempId;
    private String snippet;
    private boolean newResource; //Used to highlight new resources when we compare the current result set with a result set from a similar query posted earlier
    private int rankAtService; // the rank which the resource had at its original service (youtube, flickr...)
    private String title;

    private User addedToGroupBy; // only set if the resource is loaded in a group context
    private Date addedToGroupOn; // only set if the resource is loaded in a group context

    //new variables for extended metadata
    private String mtype;
    private String msource;
    private String language;
    private ExtendedMetadata eMetadata;

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

    public SERVICE getSource()
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

    public User getOwnerUser() throws SQLException
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

    public String getMtype()
    {
        return resource.getMtype();
    }

    public String getMsource()
    {
        return resource.getMsource();
    }

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
        return "ResourceDecorator [resource=" + resource + ", tempId=" + tempId + ", snippet=" + snippet + ", rankAtService=" + rankAtService + ", title=" + title + ", language=" + language + ", addedToGroupBy=" + addedToGroupBy + ", addedToGroupOn=" + addedToGroupOn + ", mtype="
                + mtype + ", msource=" + msource + ", eMetadata=" + eMetadata + "]";
    }

}

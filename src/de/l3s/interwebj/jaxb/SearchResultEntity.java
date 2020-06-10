package de.l3s.interwebj.jaxb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "result")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResultEntity
{
    // base
    @XmlElement(name = "service")
    private String service;
    @XmlElement(name = "rank_at_service")
    private int rank;
    @XmlElement(name = "id_at_service")
    private String id;

    // general
    @XmlElement(name = "type")
    private String type;
    @XmlElement(name = "title")
    private String title;
    @XmlElement(name = "description")
    private String description;
    @XmlElement(name = "url")
    private String url;
    @XmlElement(name = "date")
    private String date;
    @XmlElement(name = "snippet")
    private String snippet;
    @XmlElement(name = "duration")
    private Long duration;
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private Set<String> tags;

    // statistic
    @XmlElement(name = "number_of_views")
    private Long viewCount;
    @XmlElement(name = "number_of_comments")
    private Long commentCount;

    // media
    @XmlElement(name = "embedded_code")
    private String embeddedCode;
    /**
     * Usually an image with HEIGHT between 100 and 180 px.
     */
    @XmlElement(name = "thumbnail_small")
    private ThumbnailEntity thumbnailSmall;
    /**
     * Usually an image with HEIGHT between 200 and 440 px.
     */
    @XmlElement(name = "thumbnail_medium")
    private ThumbnailEntity thumbnailMedium;
    /**
     * Usually an image with HEIGHT between 600 and 920 px.
     */
    @XmlElement(name = "thumbnail_large")
    private ThumbnailEntity thumbnailLarge;
    /**
     * Image in max available quality, if bigger than large.
     */
    @XmlElement(name = "thumbnail_original")
    private ThumbnailEntity thumbnailOriginal;

    public SearchResultEntity()
    {
        tags = new HashSet<>();
    }


    public String getService()
    {
        return service;
    }

    public void setService(final String service)
    {
        this.service = service;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(final int rank)
    {
        this.rank = rank;
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public String getType()
    {
        if(type.equalsIgnoreCase("text"))
            this.type = "website";

        return type.toLowerCase();
    }

    public void setType(final String type)
    {
        this.type = type;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(final String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(final String date)
    {
        this.date = date;
    }

    public String getSnippet()
    {
        return snippet;
    }

    public void setSnippet(final String snippet)
    {
        this.snippet = snippet;
    }

    public Long getDuration()
    {
        return duration;
    }

    public void setDuration(final Long duration)
    {
        this.duration = duration;
    }

    public Set<String> getTags()
    {
        return tags;
    }

    public void setTags(final Set<String> tags)
    {
        this.tags = tags;
    }

    public Long getViewCount()
    {
        return viewCount;
    }

    public void setViewCount(final Long viewCount)
    {
        this.viewCount = viewCount;
    }

    public Long getCommentCount()
    {
        return commentCount;
    }

    public void setCommentCount(final Long commentCount)
    {
        this.commentCount = commentCount;
    }

    public String getEmbeddedCode()
    {
        return embeddedCode;
    }

    public void setEmbeddedCode(final String embeddedCode)
    {
        this.embeddedCode = embeddedCode;
    }

    public ThumbnailEntity getThumbnailSmall()
    {
        return thumbnailSmall;
    }

    public void setThumbnailSmall(final ThumbnailEntity thumbnailSmall)
    {
        this.thumbnailSmall = thumbnailSmall;
    }

    public ThumbnailEntity getThumbnailMedium()
    {
        return thumbnailMedium;
    }

    public void setThumbnailMedium(final ThumbnailEntity thumbnailMedium)
    {
        this.thumbnailMedium = thumbnailMedium;
    }

    public ThumbnailEntity getThumbnailLarge()
    {
        return thumbnailLarge;
    }

    public void setThumbnailLarge(final ThumbnailEntity thumbnailLarge)
    {
        this.thumbnailLarge = thumbnailLarge;
    }

    public ThumbnailEntity getThumbnailOriginal()
    {
        return thumbnailOriginal;
    }

    public void setThumbnailOriginal(final ThumbnailEntity thumbnailOriginal)
    {
        this.thumbnailOriginal = thumbnailOriginal;
    }

    @Override
    public String toString()
    {
        return "SearchResultEntity [" +
                "service='" + service + '\'' +
                ", rank=" + rank +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", date='" + date + '\'' +
                ", snippet='" + snippet + '\'' +
                ", duration=" + duration +
                ", tags=" + tags +
                ", viewCount=" + viewCount +
                ", commentCount=" + commentCount +
                ", embeddedCode='" + embeddedCode + '\'' +
                ", thumbnailSmall=" + thumbnailSmall +
                ", thumbnailMedium=" + thumbnailMedium +
                ", thumbnailLarge=" + thumbnailLarge +
                ", thumbnailOriginal=" + thumbnailOriginal +
                ']';
    }
}

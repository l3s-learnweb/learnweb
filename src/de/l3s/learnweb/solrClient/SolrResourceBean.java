package de.l3s.learnweb.solrClient;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.beans.Field;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.Tag;

public class SolrResourceBean
{

    @Field
    private String id;

    @Field("title")
    private String title;

    @Field("source")
    private String source;

    @Field("description")
    private String description;

    @Field("location")
    private String location;

    @Field("type")
    private String type;

    @Field("format")
    private String format;

    @Field("language")
    private String language;

    @Field("author")
    private String author;

    @Field("tags")
    private List<String> tags;

    @Field("groups")
    private List<Integer> groups;

    @Field("comments")
    private List<String> comments;

    @Field("machineDescription")
    private String machineDescription;

    @Field("oldRank")
    private int oldRank;

    @Field("embeddedCode")
    private String embeddedCode;

    @Field("thumbnailUrl2")
    private String thumbnailUrl2;

    @Field("thumbnailHeight2")
    private int thumbnailHeight2;

    @Field("thumbnailWidth2")
    private int thumnailWidth2;

    @Field("thumbnailUrl3")
    private String thumbnailUrl3;

    @Field("thumbnailHeight3")
    private int thumbnailHeight3;

    @Field("thumbnailWidth3")
    private int thumnailWidth3;

    @Field("thumbnailUrl4")
    private String thumbnailUrl4;

    @Field("thumbnailHeight4")
    private int thumbnailHeight4;

    @Field("thumbnailWidth4")
    private int thumnailWidth4;

    @Field("ownerUserId")
    private int ownerUserId;

    @Field("url")
    private String url;

    // dynamic fields
    @Field("collector_s")
    private String collector;

    @Field("coverage_s")
    private String coverage;

    @Field("publisher_s")
    private String publisher;

    public SolrResourceBean() // empty constructor necessary for SolrSearch
    {

    }

    public SolrResourceBean(ResourceDecorator decoratedResource) throws SQLException
    {
	this(decoratedResource.getResource());

	setOldRank(decoratedResource.getRankAtService());
	//setEmbeddedCode(decoratedResource.getResource().getEmbeddedRaw());
	setId(decoratedResource.getResource().getUrl());

	if(null != decoratedResource.getResource().getThumbnail4())
	{
	    setThumbnailUrl2(decoratedResource.getResource().getThumbnail2().getUrl());
	    setThumbnailHeight2(decoratedResource.getResource().getThumbnail2().getHeight());
	    setThumnailWidth2(decoratedResource.getResource().getThumbnail2().getWidth());
	    setThumbnailUrl3(decoratedResource.getResource().getThumbnail3().getUrl());
	    setThumbnailHeight3(decoratedResource.getResource().getThumbnail3().getHeight());
	    setThumnailWidth3(decoratedResource.getResource().getThumbnail3().getWidth());
	    setThumbnailUrl4(decoratedResource.getResource().getThumbnail4().getUrl());
	    setThumbnailHeight4(decoratedResource.getResource().getThumbnail4().getHeight());
	    setThumnailWidth4(decoratedResource.getResource().getThumbnail4().getWidth());
	}
    }

    public SolrResourceBean(Resource resource) throws SQLException
    {
	this.id = "r_" + resource.getId();
	this.title = resource.getTitle();
	this.description = resource.getDescription();
	this.source = resource.getSource().equals("Archive-It") ? "ArchiveIt" : resource.getSource();
	this.location = resource.getLocation().equals("Archive-It") ? "ArchiveIt" : resource.getLocation();
	this.type = resource.getType();
	this.format = resource.getFormat();
	this.language = resource.getLanguage();
	this.author = resource.getAuthor();
	this.ownerUserId = resource.getOwnerUserId();
	this.url = resource.getUrl();

	if(null != resource.getTags())
	{
	    this.tags = new LinkedList<String>();
	    for(Tag tag : resource.getTags())
		tags.add(tag.getName());
	}
	if(null != resource.getComments())
	{
	    this.comments = new LinkedList<String>();
	    for(Comment comment : resource.getComments())
		comments.add(comment.getText());
	}
	if(null != resource.getGroups())
	{
	    this.groups = new LinkedList<Integer>();
	    for(Group group : resource.getGroups())
		groups.add(group.getId());
	}

	if(null != resource.getThumbnail4())
	{
	    setThumbnailHeight4(resource.getThumbnail4().getHeight());
	    setThumnailWidth4(resource.getThumbnail4().getWidth());
	}

	this.collector = resource.getMetadataValue("collector");
	this.coverage = resource.getMetadataValue("coverage");
	this.publisher = resource.getMetadataValue("publisher");
    }

    public String getAuthor()
    {
	return author;
    }

    public void setAuthor(String author)
    {
	this.author = author;
    }

    public Integer getOldRank()
    {
	return oldRank;
    }

    public void setOldRank(Integer oldRank)
    {
	this.oldRank = oldRank;
    }

    public String getMachineDescription()
    {
	return machineDescription;
    }

    public void setMachineDescription(String machineDescription)
    {
	this.machineDescription = machineDescription;
    }

    public String getId()
    {
	return id;
    }

    public void setId(String id)
    {
	this.id = id;
    }

    public String getTitle()
    {
	return title;
    }

    public void setTitle(String title)
    {
	this.title = title;
    }

    public String getSource()
    {
	return source;
    }

    public void setSource(String source)
    {
	this.source = source;
    }

    public String getDescription()
    {
	return description;
    }

    public void setDescription(String description)
    {
	this.description = description;
    }

    public String getLocation()
    {
	return location;
    }

    public void setLocation(String location)
    {
	this.location = location;
    }

    public String getType()
    {
	return type;
    }

    public void setType(String type)
    {
	this.type = type;
    }

    public String getFormat()
    {
	return format;
    }

    public void setFormat(String format)
    {
	this.format = format;
    }

    public String getLanguage()
    {
	return language;
    }

    public void setLanguage(String language)
    {
	this.language = language;
    }

    public List<String> getTags()
    {
	return tags;
    }

    public void setTags(List<String> tags)
    {
	this.tags = tags;
    }

    public void addTag(String tag)
    {
	this.tags.add(tag);
    }

    public List<Integer> getGroups()
    {
	return groups;
    }

    public void setGroups(List<Integer> groups)
    {
	this.groups = groups;
    }

    public List<String> getComments()
    {
	return comments;
    }

    public void setComments(List<String> comments)
    {
	this.comments = comments;
    }

    public String getEmbeddedCode()
    {
	return embeddedCode;
    }

    public void setEmbeddedCode(String embeddedCode)
    {
	this.embeddedCode = embeddedCode;
    }

    public String getThumbnailUrl2()
    {
	return thumbnailUrl2;
    }

    public void setThumbnailUrl2(String thumbnailUrl2)
    {
	this.thumbnailUrl2 = thumbnailUrl2;
    }

    public int getThumbnailHeight2()
    {
	return thumbnailHeight2;
    }

    public void setThumbnailHeight2(int thumbnailHeight2)
    {
	this.thumbnailHeight2 = thumbnailHeight2;
    }

    public int getThumnailWidth2()
    {
	return thumnailWidth2;
    }

    public void setThumnailWidth2(int thumnailWidth2)
    {
	this.thumnailWidth2 = thumnailWidth2;
    }

    public String getThumbnailUrl3()
    {
	return thumbnailUrl3;
    }

    public void setThumbnailUrl3(String thumbnailUrl3)
    {
	this.thumbnailUrl3 = thumbnailUrl3;
    }

    public int getThumbnailHeight3()
    {
	return thumbnailHeight3;
    }

    public void setThumbnailHeight3(int thumbnailHeight3)
    {
	this.thumbnailHeight3 = thumbnailHeight3;
    }

    public int getThumnailWidth3()
    {
	return thumnailWidth3;
    }

    public void setThumnailWidth3(int thumnailWidth3)
    {
	this.thumnailWidth3 = thumnailWidth3;
    }

    public String getThumbnailUrl4()
    {
	return thumbnailUrl4;
    }

    public void setThumbnailUrl4(String thumbnailUrl4)
    {
	this.thumbnailUrl4 = thumbnailUrl4;
    }

    public int getThumbnailHeight4()
    {
	return thumbnailHeight4;
    }

    public void setThumbnailHeight4(int thumbnailHeight4)
    {
	this.thumbnailHeight4 = thumbnailHeight4;
    }

    public int getThumnailWidth4()
    {
	return thumnailWidth4;
    }

    public void setThumnailWidth4(int thumnailWidth4)
    {
	this.thumnailWidth4 = thumnailWidth4;
    }

    public void setOldRank(int oldRank)
    {
	this.oldRank = oldRank;
    }

    public int getOwnerUserId()
    {
	return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId)
    {
	this.ownerUserId = ownerUserId;
    }

    public String getUrl()
    {
	return url;
    }

    public void setUrl(String url)
    {
	this.url = url;
    }

    public String getCollector()
    {
	return collector;
    }

    public void setCollector(String collector)
    {
	this.collector = collector;
    }

    public String getCoverage()
    {
	return coverage;
    }

    public void setCoverage(String coverage)
    {
	this.coverage = coverage;
    }

    public String getPublisher()
    {
	return publisher;
    }

    public void setPublisher(String publisher)
    {
	this.publisher = publisher;
    }

}

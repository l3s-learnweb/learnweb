package de.l3s.learnweb.resource.search.solrClient;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.beans.Field;

import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Tag;

public class ResourceDocument
{
    private static final Logger log = Logger.getLogger(ResourceDocument.class);

    @Field
    private String id;

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("machineDescription")
    private String machineDescription;

    @Field("url")
    private String url;

    @Field("language")
    private String language;

    @Field("source")
    private String source;

    @Field("type")
    private String type;

    @Field("format")
    private String format;

    /*
    @Field("duration")
    private int duration;

    @Field("thumbnailWidth")
    private int thumbnailWidth;

    @Field("thumbnailHeight")
    private int thumbnailHeight;
    */

    @Field("author")
    private String author;

    @Field("tags")
    private List<String> tags;

    @Field("comments")
    private List<String> comments;

    @Field("location")
    private String location;

    @Field("groupId")
    private Integer groupId;

    @Field("path")
    private String path;

    @Field("ownerUserId")
    private int ownerUserId;

    @Field("timestamp")
    private Date timestamp;

    // dynamic fields
    @Field("*_ss")
    public Map<String, String[]> dynamicFieldsStrings;

    public ResourceDocument() // empty constructor necessary for SolrSearch
    {

    }

    public ResourceDocument(Resource resource) throws SQLException
    {
        this.id = "r_" + resource.getId();
        this.title = resource.getTitle();
        this.description = resource.getDescription();
        this.machineDescription = resource.getMachineDescription();
        this.url = resource.getUrl();
        this.language = resource.getLanguage().toLowerCase();
        this.source = resource.getSource().toString().toLowerCase();
        this.type = resource.getType().name();
        this.format = resource.getFormat().toLowerCase();
        // this.duration = resource.getDuration();
        this.author = resource.getAuthor();
        this.location = resource.getLocation().toLowerCase().replace("-", "");
        this.groupId = resource.getGroupId();
        this.path = resource.getPath();
        this.ownerUserId = resource.getUserId();
        this.timestamp = resource.getCreationDate() != null ? resource.getCreationDate() : resource.getResourceTimestamp();

        if(null != resource.getTags())
        {
            this.tags = new LinkedList<>();
            for(Tag tag : resource.getTags())
                tags.add(tag.getName());
        }

        if(null != resource.getComments())
        {
            this.comments = new LinkedList<>();
            for(Comment comment : resource.getComments())
                comments.add(comment.getText());
        }

        /*
        if(null != resource.getThumbnail4())
        {
            this.thumbnailHeight = resource.getThumbnail4().getHeight();
            this.thumbnailWidth = resource.getThumbnail4().getWidth();
        }*/

        // copy metadata to dynamic fields
        dynamicFieldsStrings = new HashMap<>(resource.getMetadata().size());
        for(Entry<String, String> entry : resource.getMetadata().entrySet())
        {
            if(entry.getValue() == null)
            {
                log.warn("entry has no value: " + entry);
                continue;
            }
            String[] value;

            if(entry.getValue().indexOf(Resource.METADATA_SEPARATOR) != -1) // Warning: the presence of the separator char isn't a good indicator for a multi valued field. in the past singe value fields were allowed to contain this separator
                value = StringUtils.split(entry.getValue(), Resource.METADATA_SEPARATOR);
            else
                value = new String[] { entry.getValue() };

            dynamicFieldsStrings.put(entry.getKey() + "_ss", value);
        }
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getMachineDescription()
    {
        return machineDescription;
    }

    public void setMachineDescription(String machineDescription)
    {
        this.machineDescription = machineDescription;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
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

    /*
    public int getDuration()
    {
        return duration;
    }

    public void setDuration(final int duration)
    {
        this.duration = duration;
    }

    public int getThumbnailWidth()
    {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(int thumbnailWidth)
    {
        this.thumbnailWidth = thumbnailWidth;
    }

    public int getThumbnailHeight()
    {
        return thumbnailHeight;
    }

    public void setThumbnailHeight(int thumbnailHeight)
    {
        this.thumbnailHeight = thumbnailHeight;
    }
    */
    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }

    public List<String> getComments()
    {
        return comments;
    }

    public void setComments(List<String> comments)
    {
        this.comments = comments;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public Integer getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Integer groupId)
    {
        this.groupId = groupId;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public int getOwnerUserId()
    {
        return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId)
    {
        this.ownerUserId = ownerUserId;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public Map<String, String[]> getDynamicFieldsStrings()
    {
        return dynamicFieldsStrings;
    }

    public void setDynamicFieldsStrings(final Map<String, String[]> dynamicFieldsStrings)
    {
        this.dynamicFieldsStrings = dynamicFieldsStrings;
    }
}

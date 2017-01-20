package de.l3s.learnweb.solrClient;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.beans.Field;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Resource;
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
    private List<String> tags; // the facets of this field are splitted on every white space; the field type has to be change from text_general to string or better use a copy method

    @Field("tags_ss")
    private List<String> tagsTemp;

    @Field("groups")
    private List<Integer> groups;

    @Field("comments")
    private List<String> comments;

    @Field("machineDescription")
    private String machineDescription;

    @Field("ownerUserId")
    private int ownerUserId;

    @Field("url")
    private String url;

    // dynamic fields
    @Field("path_s")
    private String path;

    @Field("*_s")
    public Map<String, String> dynamicFieldsStrings;

    public SolrResourceBean() // empty constructor necessary for SolrSearch
    {

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
            this.tagsTemp = new LinkedList<String>();
            for(Tag tag : resource.getTags())
            {
                tags.add(tag.getName());
                tagsTemp.add(tag.getName());
            }
        }
        if(null != resource.getComments())
        {
            this.comments = new LinkedList<String>();
            for(Comment comment : resource.getComments())
                comments.add(comment.getText());
        }
        if(null != resource.getGroup())
        {
            this.groups = new LinkedList<Integer>(Arrays.asList(resource.getGroupId()));
        }

        // copy metadata to dynamic fields
        dynamicFieldsStrings = new HashMap<String, String>(resource.getMetadata().size());

        for(Entry<String, String> entry : resource.getMetadata().entrySet())
        {
            dynamicFieldsStrings.put(entry.getKey() + "_s", entry.getValue());
        }
        /*
        this.collector = resource.getMetadataValue("collector");
        this.coverage = resource.getMetadataValue("coverage");
        this.publisher = resource.getMetadataValue("publisher");
        */
        this.path = resource.getPath();
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
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

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public List<String> getTagsTemp()
    {
        return tagsTemp;
    }

    public void setTagsTemp(List<String> tagsTemp)
    {
        this.tagsTemp = tagsTemp;
    }
}

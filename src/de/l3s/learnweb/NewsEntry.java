package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

//import org.ocpsoft.pretty.time.PrettyTime;

import de.l3s.learnweb.LogEntry.Action;

/**
 * This whole stupid class should be removed
 * 
 * @author Philipp
 *
 */
@Deprecated
public class NewsEntry implements Comparable<NewsEntry>, Serializable
{
    private static final long serialVersionUID = -8039903365395519945L;
    private User user;
    private Resource resource;
    private Integer comments;
    private Integer tags;
    private String text;
    private Boolean resourceAction;
    private Date date;
    private Action newsAction;
    private LogEntry logEntry;

    public NewsEntry(LogEntry l) throws SQLException
    {
        Resource r = l.getResource();

        int commentcount = 0;
        int tagcount = 0;
        String text = l.getDescription();

        if(r != null)
        {
            if(r.getComments() != null)
                commentcount = r.getComments().size();

            if(r.getTags() != null)
                tagcount = r.getTags().size();

        }

        this.resource = r;
        this.comments = commentcount;
        this.tags = tagcount;
        this.text = text;
        this.resourceAction = r != null;
        this.date = l.getDate();
        this.newsAction = l.getAction();

        this.logEntry = l;
    }

    public User getUser()
    {
        return user;
    }

    public Resource getResource()
    {
        return resource;
    }

    public Integer getComments()
    {
        return comments;
    }

    public Integer getTags()
    {
        return tags;
    }

    public String getText()
    {
        return text;
    }

    public Boolean getResourceAction()
    {
        return resourceAction;
    }

    public boolean isQueryNeeded()
    {
        if(newsAction == Action.adding_resource && resource != null && !resource.getQuery().equalsIgnoreCase("none"))
            return true;
        return false;
    }

    public Date getDate()
    {
        return date;
    }

    @Override
    public int compareTo(NewsEntry o)
    {
        return -getDate().compareTo(o.getDate());
    }

    public LogEntry getLogEntry()
    {
        return logEntry;
    }

}

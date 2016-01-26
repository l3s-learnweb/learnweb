package de.l3s.learnwebBeans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;

@RequestScoped
@ManagedBean
public class NewsFeedBean extends ApplicationBean
{

    public final static int LIST = 0;
    public final static int BIG = 2;
    public final static int SMALL = 1;
    ArrayList<NewsEntry> newslist;
    private int entityId;
    private Boolean userlog = false;
    private int type;

    public NewsFeedBean()
    {
	/*
	String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");

	type = BIG;
	if(temp != null && temp.length() != 0)
	    entityId = Integer.parseInt(temp);
	else
	{
	    User u = getUser();
	    entityId = u.getId();
	    userlog = true;
	}
	*/
	Integer groupId = getParameterInt("group_id");
	Integer userId = getParameterInt("user_id");

	type = BIG;
	if(groupId != null)
	    entityId = groupId;
	else if(userId != null)
	{
	    entityId = userId;
	    userlog = true;
	}
	else
	{
	    entityId = getUser().getId();
	    userlog = true;
	}

    }

    private void convert() throws SQLException
    {
	Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
		Action.group_changing_restriction, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource };
	List<LogEntry> feed = null;

	if(userlog)
	    feed = getLearnweb().getLogsByUser(entityId, filter, 50);
	else
	    feed = getLearnweb().getLogsByGroup(entityId, filter, 50);

	UserManager userManager = getLearnweb().getUserManager();
	ResourceManager resourceManager = getLearnweb().getResourceManager();

	if(feed != null)
	{
	    newslist = new ArrayList<NewsEntry>();
	    for(LogEntry l : feed)
	    {
		User u = null;
		Resource r = null;

		u = userManager.getUser(l.getUserId());
		r = resourceManager.getResource(l.getResourceId());

		//log.debug(l.getAction().toString());

		int commentcount = 0;
		int tagcount = 0;
		String text = l.getDescription();
		if(l.getAction() == filter[3] || r == null)
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, false, l.getDate()));
		    continue;
		}

		if(r.getComments() != null)
		    commentcount += r.getComments().size();

		if(r.getTags() != null)
		    tagcount += r.getTags().size();

		if(l.getAction() == filter[0]) //add_resource
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true, l.getDate()));
		}
		else if(l.getAction() == filter[1] && commentcount > 0)
		{
		    Comment commenttobeadded = new Comment();
		    commenttobeadded.setText("comment removed!");

		    for(Comment c : getLearnweb().getResourceManager().getCommentsByResourceId(r.getId()))
		    {
			if(c.getId() == Integer.parseInt(l.getParams()))
			{
			    commenttobeadded = c;
			}
		    }

		    text = text + " " + getLocaleMessage("with") + " " + "<b>" + commenttobeadded.getText() + "</b>";
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true, l.getDate()));
		}
		else if(l.getAction() == filter[15])
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true, l.getDate()));
		}
		else if(l.getAction() == filter[14])
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true, l.getDate()));
		}
		else
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, false, l.getDate()));
	    }
	}
    }

    public ArrayList<NewsEntry> getNewslist()
    {
	if(null == newslist)
	{
	    try
	    {
		convert();
	    }
	    catch(SQLException e)
	    {
		addFatalMessage(e);
	    }
	}
	return newslist;
    }

    public void setNewslist(ArrayList<NewsEntry> newslist)
    {
	this.newslist = newslist;
    }

    public int getEntityId()
    {
	return entityId;
    }

    public void setEntityId(int groupId)
    {
	this.entityId = groupId;
    }

    public Boolean getUserlog()
    {
	return userlog;
    }

    public void setUserlog(Boolean userlog)
    {
	this.userlog = userlog;
    }

    public int getType()
    {
	return type;
    }

    public void setType(int type)
    {
	this.type = type;

	try
	{
	    convert();
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}

    }
}

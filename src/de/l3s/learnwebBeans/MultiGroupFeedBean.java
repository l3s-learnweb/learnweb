package de.l3s.learnwebBeans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@RequestScoped
@ManagedBean
public class MultiGroupFeedBean extends ApplicationBean
{

    ArrayList<NewsEntry> newslist;

    private User user;

    public MultiGroupFeedBean()
    {

	user = UtilBean.getUserBean().getUser();

    }

    private void convert()
    {
	HashSet<Integer> deletedResources = new HashSet<Integer>();
	Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
		Action.group_changing_restriction, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource };
	List<LogEntry> feed = null;

	try
	{
	    feed = getLearnweb().getActivityLogOfUserGroups(getUser().getId(), filter, 50);
	}
	catch(SQLException e1)
	{
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}

	if(feed != null)
	{
	    newslist = new ArrayList<NewsEntry>();
	    for(LogEntry l : feed)
	    {
		User u = null;
		Resource r = null;
		boolean resourceaction = true;
		try
		{
		    u = getLearnweb().getUserManager().getUser(l.getUserId());
		    r = getLearnweb().getResourceManager().getResource(l.getResourceId());
		}
		catch(Exception e)
		{
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		if(r != null && deletedResources.contains(r.getId()))
		    resourceaction = false;

		//log.debug(l.getAction().toString());

		int commentcount = 0;
		int tagcount = 0;
		String text = l.getDescription();
		if(l.getAction() == filter[3] || r == null || l.getAction() == filter[17])
		{
		    if(r != null)
			deletedResources.add(r.getId());
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, !resourceaction, l.getDate()));
		    continue;
		}
		try
		{
		    if(r.getComments() != null)
			commentcount += r.getComments().size();
		}
		catch(Exception e)
		{
		    // TODO Auto-generated catch block

		}

		try
		{
		    if(r.getTags() != null)
			tagcount += r.getTags().size();
		}
		catch(Exception e)
		{
		    // TODO Auto-generated catch block

		}

		if(l.getAction() == filter[0]) //add_resource
		{

		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}
		if(l.getAction() == filter[1] && commentcount > 0)
		{
		    Comment commenttobeadded = new Comment();
		    commenttobeadded.setText("comment removed!");
		    try
		    {

			for(Comment c : getLearnweb().getResourceManager().getCommentsByResourceId(r.getId()))
			{
			    if(c.getId() == Integer.parseInt(l.getParams()))
			    {
				commenttobeadded = c;
			    }
			}

		    }
		    catch(SQLException e)
		    {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }//
		    text = text + " " + getLocaleMessage("with") + " " + "<b>" + commenttobeadded.getText() + "</b>";
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}
		if(l.getAction() == filter[15])
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}
		if(l.getAction() == filter[14])
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}

		newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));

	    }

	}

    }

    public ArrayList<NewsEntry> getNewslist()
    {
	convert();
	return newslist;
    }

    public void setNewslist(ArrayList<NewsEntry> newslist)
    {
	this.newslist = newslist;
    }

    @Override
    public User getUser()
    {
	return user;
    }

    public void setUser(User user)
    {
	this.user = user;
    }

}

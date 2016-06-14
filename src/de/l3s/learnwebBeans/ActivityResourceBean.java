package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.GroupDetailBean.RPAction;

@ManagedBean
@ViewScoped
public class ActivityResourceBean extends ApplicationBean implements Serializable
{
    /*
    private static Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
        Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource };
    */
    private static final long serialVersionUID = -7630987853810267209L;
    private ArrayList<NewsEntry> newslist;
    private Resource clickedResource;
    private Folder clickedFolder;

    private RPAction rightPanelAction = null;

    private boolean reloadLogs = false;

    public boolean isReloadLogs()
    {
	return reloadLogs;
    }

    public void setReloadLogs(boolean reloadLogs)
    {
	this.reloadLogs = reloadLogs;
    }

    public ActivityResourceBean()
    {
	clickedResource = new Resource();
    }

    public void loadResources()
    {
	// do nothing. this method is needed for the right pane template
    }

    public boolean canDeleteTag(Object tagO) throws SQLException
    {
	if(!(tagO instanceof Tag))
	    return false;

	User user = getUser();
	if(null == user) // || true)
	    return false;
	if(user.isAdmin() || user.isModerator())
	    return true;

	Tag tag = (Tag) tagO;
	User owner = clickedResource.getTags().getElementOwner(tag);
	if(user.equals(owner))
	    return true;
	return false;
    }

    private void generateNewsList() throws SQLException
    {
	//HashSet<Integer> deletedResources = new HashSet<Integer>();

	List<LogEntry> feed = getfeed();

	if(feed != null)
	{
	    ResourceManager resourceManager = getLearnweb().getResourceManager();

	    newslist = new ArrayList<NewsEntry>();
	    for(LogEntry l : feed)
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

		    if(l.getAction() == Action.commenting_resource && commentcount > 0)
		    {
			Comment comment = resourceManager.getComment(Integer.parseInt(l.getParams()));

			if(comment != null)
			    text = text + " " + getLocaleMessage("with") + " " + "<b>" + comment.getText() + "</b>";
		    }

		}

		newslist.add(new NewsEntry(l, null, r, commentcount, tagcount, text, r != null, l.getDate()));
		/*
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
		
		int commentcount = 0;
		int tagcount = 0;
		String text = l.getDescription();
		if(l.getAction() == Action.deleting_resource || r == null || l.getAction() == Action.group_removing_resource)
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
		    }
		    text = text + " " + UtilBean.getLocaleMessage("with") + " " + "<i>" + commenttobeadded.getText() + "</i>";
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
		
		newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, false, l.getDate()));
		*/
	    }

	}

    }

    public ArrayList<NewsEntry> getNewslist() throws SQLException
    {
	if(null == newslist || reloadLogs)
	{
	    generateNewsList();
	}
	return newslist;
    }

    public void setNewslist(ArrayList<NewsEntry> newslist)
    {
	this.newslist = newslist;
    }

    public RPAction getRightPanelAction()
    {
	return rightPanelAction;
    }

    public void setRightPanelAction(RPAction rightPanelAction)
    {
	this.rightPanelAction = rightPanelAction;
    }

    public Resource getClickedResource()
    {
	return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
	if(this.rightPanelAction != RPAction.editResource || this.clickedResource != clickedResource)
	{
	    this.clickedResource = clickedResource;
	    this.rightPanelAction = RPAction.viewResource;
	}
    }

    public Folder getClickedFolder()
    {
	return clickedFolder;
    }

    public void setClickedFolder(Folder clickedFolder)
    {
	this.clickedFolder = clickedFolder;
    }

    private List<LogEntry> getfeed()
    {

	List<LogEntry> logs = null;
	try
	{
	    logs = getLearnweb().getActivityLogOfUserGroups(getUser().getId(), null, 20);
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return logs;
    }

    /*public Comment getClickedComment()
    {
    return clickedComment;
    }
    
    public void setClickedComment(Comment clickedComment)
    {
    this.clickedComment = clickedComment;
    }
    
    public void onEditComment()
    {
    try
    {
        getLearnweb().getResourceManager().saveComment(clickedComment);
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }
    catch(Exception e)
    {
        e.printStackTrace();
        addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
    }
    }
    
    public void onDeleteComment()
    {
    try
    {
        clickedResource.deleteComment(clickedComment);
        addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
        log(Action.deleting_comment, clickedComment.getResourceId(), clickedComment.getId() + "");
    }
    catch(Exception e)
    {
        e.printStackTrace();
        addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
    }
    }
    
    public boolean canEditComment(Object commentO) throws Exception
    {
    if(!(commentO instanceof Comment))
        return false;
    
    User user = getUser();
    if(null == user)// || true)
        return false;
    if(user.isAdmin() || user.isModerator())
        return true;
    
    Comment comment = (Comment) commentO;
    User owner = comment.getUser();
    if(user.equals(owner))
        return true;
    return false;
    }*/
}

package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.GroupDetailBean.RPAction;

@ManagedBean
@ViewScoped
public class ActivityResourceBean extends ApplicationBean implements Serializable
{
    private static Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
	    Action.group_changing_restriction, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource };

    private static final long serialVersionUID = -7630987853810267209L;
    private ArrayList<NewsEntry> newslist;
    private Resource clickedResource;
    private Folder clickedFolder;

    private RPAction rightPanelAction = null;
    //private String newComment;
    //private Comment clickedComment;
    //private String tagName;
    private boolean reloadLogs = false;

    public boolean isReloadLogs()
    {
	return reloadLogs;
    }

    public void setReloadLogs(boolean reloadLogs)
    {
	this.reloadLogs = reloadLogs;
    }

    /*public String getTagName()
    {
    return tagName;
    }
    
    public void setTagName(String tagName)
    {
    this.tagName = tagName;
    }
    
    private Tag selectedTag;
    
    public Tag getSelectedTag()
    {
    return selectedTag;
    }
    
    public void setSelectedTag(Tag selectedTag)
    {
    this.selectedTag = selectedTag;
    }
    
    public String getNewComment()
    {
    return newComment;
    }
    
    public void setNewComment(String newComment)
    {
    this.newComment = newComment;
    }*/

    public ActivityResourceBean()
    {
	clickedResource = new Resource();
    }

    /*public void addComment() throws Exception
    {
    //getLearnweb().getResourceManager().commentResource(newComment, getUser(), clickedResource);
    Comment comment = clickedResource.addComment(newComment, getUser());
    log(Action.commenting_resource, clickedResource.getId(), comment.getId() + "");
    addGrowl(FacesMessage.SEVERITY_INFO, "comment_added");
    newComment = "";
    }*/

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

    /*public void onDeleteTag()
    {
    try
    {
        clickedResource.deleteTag(selectedTag);
        addMessage(FacesMessage.SEVERITY_INFO, "tag_deleted");
    }
    catch(Exception e)
    {
        e.printStackTrace();
        addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
    }
    }
    
    public String addTag()
    {
    if(null == getUser())
    {
        addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
        return null;
    }
    
    if(tagName == null || tagName.length() == 0)
        return null;
    
    try
    {
        clickedResource.addTag(tagName, getUser());
        addGrowl(FacesMessage.SEVERITY_INFO, "tag_added");
        log(Action.tagging_resource, clickedResource.getId(), tagName);
        tagName = ""; // clear tag input field 
    }
    catch(Exception e)
    {
        e.printStackTrace();
        addGrowl(FacesMessage.SEVERITY_ERROR, "fatal_error");
    }
    return null;
    }*/

    private void generateNewsList()
    {
	HashSet<Integer> deletedResources = new HashSet<Integer>();

	List<LogEntry> feed = getfeed();

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

		newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));

	    }

	}

    }

    public ArrayList<NewsEntry> getNewslist()
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

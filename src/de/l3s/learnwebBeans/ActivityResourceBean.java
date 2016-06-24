package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Folder;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.GroupDetailBean.RPAction;

@ManagedBean
@ViewScoped
public class ActivityResourceBean extends ApplicationBean implements Serializable
{
    private final static Action[] FILTER = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
	    Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource };

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
	if(tagO == null || !(tagO instanceof Tag))
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

	List<LogEntry> feed = getLearnweb().getActivityLogOfUserGroups(getUser().getId(), FILTER, 25);

	if(feed != null)
	{

	    //ResourceManager resourceManager = getLearnweb().getResourceManager();

	    newslist = new ArrayList<NewsEntry>();
	    for(LogEntry l : feed)
	    {
		newslist.add(new NewsEntry(l));

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

}

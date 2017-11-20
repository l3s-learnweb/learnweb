package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Folder;
import de.l3s.learnweb.GroupItem;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Tag;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.GroupDetailBean.RPAction;

@ManagedBean
@ViewScoped
public class ActivityResourceBean extends ApplicationBean implements Serializable
{
    private final static Action[] FILTER = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
            Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.adding_resource_metadata, Action.adding_yourown_metadata, Action.group_metadata_search,
            Action.group_category_search };

    private static final long serialVersionUID = -7630987853810267209L;
    private ArrayList<NewsEntry> newslist;
    private GroupItem clickedGroupItem;

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
        clickedGroupItem = new Resource();
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
        User owner = ((Resource) clickedGroupItem).getTags().getElementOwner(tag);
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

    public RPAction getRightPanelAction()
    {
        return rightPanelAction;
    }

    public void setRightPanelAction(RPAction rightPanelAction)
    {
        this.rightPanelAction = rightPanelAction;
    }

    public GroupItem getClickedGroupItem()
    {
        return clickedGroupItem;
    }

    public void setClickedGroupItem(GroupItem clickedGroupItem)
    {
        this.clickedGroupItem = clickedGroupItem;
        this.rightPanelAction = RPAction.viewResource;
    }

    @Deprecated
    public Resource getClickedResource()
    {
        if(clickedGroupItem instanceof Resource)
        {
            return (Resource) getClickedGroupItem();
        }

        return null;
    }

    @Deprecated
    public void setClickedResource(Resource clickedResource)
    {
        setClickedGroupItem(clickedResource);
    }

    @Deprecated
    public Folder getClickedFolder()
    {
        if(clickedGroupItem instanceof Folder)
        {
            return (Folder) getClickedGroupItem();
        }

        return null;
    }

    @Deprecated
    public void setClickedFolder(Folder clickedFolder)
    {
        setClickedGroupItem(clickedFolder);
    }

}

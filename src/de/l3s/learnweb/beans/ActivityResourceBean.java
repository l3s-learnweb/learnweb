package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class ActivityResourceBean extends ApplicationBean implements Serializable
{
    // the filter defines which log entries are show on this page
    private final static Action[] FILTER = new Action[] {
            Action.adding_resource,
            Action.commenting_resource,
            Action.edit_resource,
            Action.group_adding_document,
            Action.group_adding_link,
            Action.group_changing_description,
            Action.group_changing_leader,
            Action.group_changing_title,
            Action.group_creating,
            Action.group_deleting,
            Action.rating_resource,
            Action.tagging_resource,
            Action.thumb_rating_resource
    };

    private static final long serialVersionUID = -7630987853810267209L;
    private ArrayList<NewsEntry> newslist;

    private boolean reloadLogs = false;

    public ActivityResourceBean()
    {
        // TODO: can be removed later (for demo purpose only)
        User user = getUser();
        if (!user.getIsEmailConfirmed()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Email confirmation required!", "Your email is not confirmed, please, check your mailbox.");
        }
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

    public boolean isReloadLogs()
    {
        return reloadLogs;
    }

    public void setReloadLogs(boolean reloadLogs)
    {
        this.reloadLogs = reloadLogs;
    }
}

package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;

@Named
@ViewScoped
public class ActivityResourceBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -7630987853810267209L;

    // the filter defines which log entries are show on this page
    private static final Action[] FILTER = {
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
            Action.thumb_rating_resource,
            Action.forum_topic_added,
            Action.changing_office_resource,
            Action.forum_post_added
    };

    private List<LogEntry> newsList;

    private boolean reloadLogs = false;

    private void generateNewsList() throws SQLException
    {
        newsList = getLearnweb().getLogManager().getActivityLogOfUserGroups(getUser().getId(), FILTER, 25);
    }

    public List<LogEntry> getNewsList() throws SQLException
    {
        if(null == newsList || reloadLogs)
        {
            generateNewsList();
        }
        return newsList;
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

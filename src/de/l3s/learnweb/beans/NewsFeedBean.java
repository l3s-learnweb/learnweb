package de.l3s.learnweb.beans;

import java.sql.SQLException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;

@RequestScoped
@ManagedBean
public class NewsFeedBean extends ApplicationBean
{

    private int entityId;
    private List<LogEntry> newsList;

    private final static Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
            Action.group_changing_restriction, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource,
            Action.changing_resource,
            Action.group_removing_resource };

    public NewsFeedBean()
    {
        Integer groupId = getParameterInt("group_id");
        Integer userId = getParameterInt("user_id");

        if(groupId != null)
            entityId = groupId;
        else if(userId != null)
        {
            entityId = userId;
        }
        else
        {
            entityId = getUser().getId();
        }
    }

    public List<LogEntry> getNewsList()
    {
        if(null == newsList)
        {
            try
            {
                newsList = getLearnweb().getLogsByUser(entityId, filter, 50);
            }
            catch(SQLException e)
            {
                addFatalMessage(e);
            }
        }
        return newsList;
    }

    public int getEntityId()
    {
        return entityId;
    }

    public void setEntityId(int groupId)
    {
        this.entityId = groupId;
    }

}

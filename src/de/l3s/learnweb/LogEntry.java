package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable
{

    /**
	 * 
	 */
    private static final long serialVersionUID = -4239479043091966928L;

    public enum Action
    { // add new values add the BOTTOM !!!
	tagging_resource, // param = the tag; target_id = id of the resource
	rating_resource, // param = rate; target_id = id of the resource
	commenting_resource, // param = comment id; target_id = id of the resource		
	opening_resource, // target_id = id of the tagged resource
	opening_url, //param = the url
	searching, // param = search query
	group_joining, // target_id = group_id
	group_creating, // target_id = group_id
	group_leaving, // target_id = group_id
	login,
	logout,
	unused3,
	register,
	changing_profile,
	deleting_resource, // param = resource title, target_id = resource id
	adding_resource, // target_id = resource id
	open_link,
	deleting_comment, // target_id = resource id, param = comment_id 
	unused2,
	edit_resource, // target_id = resource id
	unused4,
	thumb_rating_resource, // target_id = resource id
	group_deleting, // target_id = group_id
	group_changing_description, // target_id = group_id
	group_changing_title, // target_id = group_id, param = old title
	group_changing_leader, // target_id = group_id
	group_changing_restriction, // target_id = group_id
	group_adding_link, // target_id = group_id, param = title
	group_adding_document, // target_id = group_id, param = title
	error,
	group_removing_resource, // target_id = resource id
	unused, // target_id = resource id
	downloading, // target_id = file_id
	group_deleting_link // param = title
    }

    private int userId;

    private Action action;
    private int groupId;
    private Date date;
    private String params;
    private String username;
    private String description;
    private String usernameLink;
    private boolean highlighted = false;

    private int resourceId;

    public LogEntry(ResultSet rs) throws SQLException
    {
	int userId = rs.getInt(1);
	username = rs.getString(2);
	action = Action.values()[rs.getInt(3)];
	int targetId = rs.getInt(4);
	params = rs.getString(5);
	date = rs.getTimestamp(6);
	//date = rs.getDate(6);
	setResourceId(targetId);
	setUserId(userId);
	String url = UtilBean.getLearnwebBean().getContextUrl() + "/lw/";

	int groupId = rs.getInt(7);
	//user_id, username, action, target_id, params, timestamp, group_id, r.title AS resource_title, g.title AS group_title

	usernameLink = "<a href=\"" + url + "user/detail.jsf?user_id=" + userId + "\" style=\" color:#3399FF;text-decoration: none;\">" + username + "</a>";

	String resourceTitle = rs.getString("resource_title");
	if(null == resourceTitle)
	    resourceTitle = "a resource";
	else
	    resourceTitle = "<i>" + StringHelper.shortnString(resourceTitle, 80) + "</i>";
	String resource = "<b>" + resourceTitle + "</b>";

	String groupTitle = rs.getString("group_title");
	if(null == groupTitle)
	    groupTitle = "a group";
	else
	    groupTitle = "<i>" + StringHelper.shortnString(groupTitle, 80) + "</i>";
	String group = "<a href=\"" + url + "group/overview.jsf?group_id=" + groupId + "\" style=\" color:#3399FF;text-decoration: none;\">" + groupTitle + "</a>";

	switch(action)
	{
	case adding_resource:
	    description = usernameLink + " has added " + resource + " to " + group;
	    break;
	case edit_resource:
	    description = usernameLink + " has edited " + resource;
	    break;
	case deleting_resource:
	    description = usernameLink + " has deleted " + resourceTitle;
	    break;
	case tagging_resource:
	    description = usernameLink + " has tagged " + resource + " with " + params;
	    break;
	case commenting_resource:
	    description = usernameLink + " has commented on " + resource;
	    break;
	case rating_resource:
	case thumb_rating_resource:
	    description = usernameLink + " has rated " + resource;
	    break;
	case opening_resource:
	    description = usernameLink + " has opened " + resource;
	    break;
	case opening_url:
	    description = usernameLink + " has opened the following url: " + params;
	    break;
	case searching:
	    description = usernameLink + " searched for \"" + params + "\"";
	    break;
	case group_joining:
	    //if(targetId==groupId)
	    description = usernameLink + " has joined the group " + group;
	    break;
	case group_leaving:
	    description = usernameLink + " has left the group " + group;
	    break;
	case group_creating:
	    description = usernameLink + " has created the group " + group;
	    break;
	case group_deleting:
	    description = usernameLink + " has deleted the group " + groupTitle;
	    break;

	case group_changing_title:
	    description = usernameLink + " has changed the title of group " + group;
	    break;
	case group_changing_description:
	    description = usernameLink + " has changed the description of group " + group;
	    break;
	case group_changing_leader:
	    description = usernameLink + " has changed the leader of group " + group;
	    break;
	case group_adding_document:
	    description = usernameLink + " has added a document to " + group;
	    break;
	case group_adding_link:
	    description = usernameLink + " has added a link to " + group;
	    break;
	case group_removing_resource:
	    description = usernameLink + " has deleted " + resourceTitle;
	    break;
	case downloading:
	    description = usernameLink + " has downloaded " + resource;

	    /*			
	    			Action.group_changing_restriction,			
	    */

	default:
	    description = "no message for action " + action.name();
	}
    }

    public boolean isHighlighted()
    {
	return highlighted;
    }

    public void setHighlighted(boolean highlighted)
    {
	this.highlighted = highlighted;
    }

    /**
     * Error entry
     */
    public LogEntry(String description)
    {
	super();
	this.userId = -1;
	this.action = Action.error;
	this.groupId = -1;
	this.date = new Date();
	this.params = "";
	this.description = description;
    }

    public LogEntry(int userId, Action action, int groupId, Date date, String params)
    {
	super();
	this.userId = userId;
	this.action = action;
	this.groupId = groupId;
	this.date = date;
	this.params = params;
    }

    public LogEntry(int userId, String username, Action action, int groupId, Date date, String params)
    {
	this.userId = userId;
	this.action = action;
	this.groupId = groupId;
	this.date = date;
	this.params = params;
	this.username = username;
    }

    public int getUserId()
    {
	return userId;
    }

    public Action getAction()
    {
	return action;
    }

    public int getGroupId()
    {
	return groupId;
    }

    public Date getDate()
    {
	return date;
    }

    public String getParams()
    {
	return params;
    }

    public String getUsername()
    {
	return username;
    }

    public String getDescription()
    {
	return description;
    }

    public String getUsernameLink()
    {
	return usernameLink;
    }

    @Override
    public String toString()
    {
	//MessageFormat format = new MessageFormat("{1} added \"{2}\" to group");
	return MessageFormat.format("{1} added \"{2}\" to group", "userx", "image");
    }

    public void setUserId(int userId)
    {
	this.userId = userId;
    }

    public int getResourceId()
    {
	return resourceId;
    }

    public void setResourceId(int resourceId)
    {
	this.resourceId = resourceId;
    }

    public static void main(String[] args)
    {
	System.out.println(LogEntry.Action.downloading.ordinal());
    }

}

package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable
{
    private static final long serialVersionUID = -4239479043091966928L;

    public enum Action
    { // add new values add the BOTTOM !!!
	tagging_resource, // param = the tag; target_id = id of the resource
	rating_resource, // param = rate; target_id = id of the resource
	commenting_resource, // param = comment id; target_id = id of the resource		
	opening_resource, // target_id = id of the tagged resource
	unused, // param = the url
	searching, // param = search query
	group_joining, // target_id = group_id
	group_creating, // target_id = group_id
	group_leaving, // target_id = group_id
	login,
	logout,
	unused3,
	register,
	changing_profile, // target_id = user_id of the user whose profile was changed
	deleting_resource, // param = resource title; target_id = resource id
	adding_resource, // target_id = resource id
	open_link,
	deleting_comment, // target_id = resource id; param = comment_id 
	unused2,
	edit_resource, // target_id = resource id
	unused4,
	thumb_rating_resource, // target_id = resource id
	group_deleting, // target_id = group_id
	group_changing_description, // target_id = group_id
	group_changing_title, // target_id = group_id; param = old title
	group_changing_leader, // target_id = group_id
	group_changing_restriction, // target_id = group_id
	group_adding_link, // target_id = group_id; param = title
	group_adding_document, // target_id = group_id; param = title
	unused5,
	group_removing_resource, // target_id = resource id
	deleting_folder, // param = folder name; target_id = folder_id
	downloading, // target_id = file_id
	group_deleting_link, // param = title of deleted link; out dated
	group_resource_search, // param = query
	add_folder, // param = folder name; target_id = folder_id
	edit_folder, // param = folder name; target_id = folder_id
    }

    private int userId;

    private Action action;
    private int groupId;
    private Date date;
    private String params;
    private String username;
    private String description;
    private int resourceId;

    public LogEntry(ResultSet rs) throws SQLException
    {
	int userId = rs.getInt(1);
	username = rs.getString(2);
	action = Action.values()[rs.getInt(3)];
	int targetId = rs.getInt(4);
	params = rs.getString(5);
	date = new Date(rs.getTimestamp(6).getTime());
	setResourceId(targetId);
	setUserId(userId);
	String url = UtilBean.getLearnwebBean().getContextUrl() + "/lw/";

	int groupId = rs.getInt(7);
	//user_id, username, action, target_id, params, timestamp, group_id, r.title AS resource_title, g.title AS group_title

	String usernameLink = "<a href=\"" + url + "user/detail.jsf?user_id=" + userId + "\" style=\" color:#3399FF;text-decoration:none;\">" + username + "</a> ";

	String resourceTitle = rs.getString("resource_title");
	if(null == resourceTitle)
	    resourceTitle = "a resource";
	else
	    resourceTitle = "<b>" + StringHelper.shortnString(resourceTitle, 80) + "</b>";
	String resource = resourceTitle;

	String groupTitle = rs.getString("group_title");
	if(null == groupTitle)
	    groupTitle = "a group";
	else
	    groupTitle = StringHelper.shortnString(groupTitle, 80);
	String group = "<a href=\"" + url + "group/overview.jsf?group_id=" + groupId + "\" style=\" color:#53b398;text-decoration:none;font-weight:bold\">" + groupTitle + "</a>";

	//

	switch(action)
	{
	case adding_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_adding_resource", resource, group);
	    //description = usernameLink + " has added " + resource + " to " + group;
	    break;
	case edit_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_edit_resource", resource);
	    //description = usernameLink + " has edited " + resource;
	    break;
	case deleting_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_deleting_resource", params); // resourceTitle
	    //description = usernameLink + " has deleted " + resourceTitle;
	    break;
	case tagging_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_tagging_resource", resource, params);
	    //description = usernameLink + " has tagged " + resource + " with " + params;
	    break;
	case commenting_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_commenting_resource", resource);
	    //description = usernameLink + " has commented on " + resource;
	    break;
	case rating_resource:
	case thumb_rating_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_thumb_rating_resource", resource);
	    //description = usernameLink + " has rated " + resource;
	    break;
	case opening_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_opening_resource", resource);
	    //description = usernameLink + " has opened " + resource;
	    break;
	/*
	case opening_url:
	description = usernameLink + UtilBean.getLocaleMessage("log_opening_url_resource", params);
	//description = usernameLink + " has opened the following url: " + params;
	break;
	*/
	case searching:
	    description = usernameLink + UtilBean.getLocaleMessage("log_searching_resource", params);
	    //description = usernameLink + " searched for \"" + params + "\"";
	    break;
	case group_joining:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_joining", group);
	    //description = usernameLink + " has joined the group " + group;
	    break;
	case group_leaving:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_leaving", group);
	    //description = usernameLink + " has left the group " + group;
	    break;
	case group_creating:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_creating", group);
	    //description = usernameLink + " has created the group " + group;
	    break;
	case group_deleting:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_deleting", groupTitle);
	    //description = usernameLink + " has deleted the group " + groupTitle;
	    break;
	case group_changing_title:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_changing_title", group);
	    //description = usernameLink + " has changed the title of group " + group;
	    break;
	case group_changing_description:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_changing_description", group);
	    //description = usernameLink + " has changed the description of group " + group;
	    break;
	case group_changing_leader:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_changing_leader", group);
	    //description = usernameLink + " has changed the leader of group " + group;
	    break;
	case group_adding_document:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_adding_document", group);
	    //description = usernameLink + " has added a document to " + group;
	    break;
	case group_adding_link:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_adding_link", group);
	    //description = usernameLink + " has added a link to " + group;
	    break;
	case group_deleting_link:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_deleting_link", group);
	    //description = usernameLink + " has deleted a link from " + group;
	    break;
	case group_removing_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_removing_resource", params, group);
	    //description = usernameLink + " has deleted " + resourceTitle + " from " + group;
	    break;
	case downloading:
	    description = usernameLink + UtilBean.getLocaleMessage("log_downloading", resource);
	    //description = usernameLink + " has downloaded " + resource;
	    break;
	default:
	    description = "no message for action " + action.name(); // should never happen; muss nicht übersetzt werden
	}
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
}

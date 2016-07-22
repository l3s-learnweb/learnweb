package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

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
	forum_post_added, // target_id = 
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
	downloading, // target_id = resource_id, param = file_id
	group_deleting_link, // param = title of deleted link; out dated
	group_resource_search, // param = query
	add_folder, // param = folder name; target_id = folder_id
	edit_folder, // param = folder name; target_id = folder_id
    }

    private final static HashSet<Action> resourceActions = Sets.newHashSet(Action.tagging_resource, Action.rating_resource, Action.commenting_resource, Action.opening_resource, Action.adding_resource, Action.deleting_comment, Action.edit_resource, Action.thumb_rating_resource);
    //private final static HashSet<Action> folderActions = Sets.newHashSet(Action.deleting_folder, Action.add_folder, Action.edit_folder);

    private int userId;
    private Action action;
    private int groupId;
    private Date date;
    private String params;
    private String username;
    private String description;
    private int resourceId;
    private String userImage;

    // cache
    private transient Resource resource;

    public LogEntry(ResultSet rs) throws SQLException
    {
	userId = rs.getInt(1);
	username = rs.getString(2);
	action = Action.values()[rs.getInt(3)];
	params = rs.getString(5);
	date = new Date(rs.getTimestamp(6).getTime());
	groupId = rs.getInt(7);
	userImage = User.getImage(rs.getInt("image_file_id"));

	int targetId = rs.getInt(4);

	if(resourceActions.contains(action))
	    resourceId = targetId;
	/* currently not used
	else if(folderActions.contains(action))
	    fileId = targetId;   
	*/
	String url = UtilBean.getLearnwebBean().getContextUrl() + "/lw/";

	String usernameLink = "<a href=\"" + url + "user/detail.jsf?user_id=" + userId + "\" style=\" color:#3399FF;text-decoration:none;\">" + username + "</a> ";

	String resourceTitle = rs.getString("resource_title");
	resourceTitle = (null == resourceTitle) ? "a resource" : "<b>" + StringHelper.shortnString(resourceTitle, 80) + "</b>";

	String groupTitle = rs.getString("group_title");
	String groupLink;

	if(null == groupTitle)
	    groupLink = groupTitle = "private group";
	else
	{
	    groupTitle = StringHelper.shortnString(groupTitle, 80);
	    groupLink = "<a href=\"" + url + "group/overview.jsf?group_id=" + groupId + "\" style=\" color:#53b398;text-decoration:none;font-weight:bold\">" + groupTitle + "</a>";
	}

	switch(action)
	{
	case adding_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_adding_resource", resourceTitle, groupLink);
	    //description = usernameLink + " has added " + resource + " to " + group;
	    break;
	case edit_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_edit_resource", resourceTitle);
	    //description = usernameLink + " has edited " + resource;
	    break;
	case deleting_resource:
	    if(!StringUtils.isEmpty(params))
		resourceTitle = "<b>" + params + "</b>";
	    description = usernameLink + UtilBean.getLocaleMessage("log_deleting_resource", resourceTitle); // resourceTitle
	    //description = usernameLink + " has deleted " + resourceTitle;
	    break;
	case tagging_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_tagging_resource", resourceTitle, params);
	    //description = usernameLink + " has tagged " + resource + " with " + params;
	    break;
	case commenting_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_commenting_resource", resourceTitle);

	    Comment comment = Learnweb.getInstance().getResourceManager().getComment(StringHelper.parseInt(params));

	    if(comment != null)
		description += " " + UtilBean.getLocaleMessage("with") + " " + "<b>" + comment.getText() + "</b>";

	    //description = usernameLink + " has commented on " + resource;
	    break;
	case rating_resource:
	case thumb_rating_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_thumb_rating_resource", resourceTitle);
	    //description = usernameLink + " has rated " + resource;
	    break;
	case opening_resource:
	    description = usernameLink + UtilBean.getLocaleMessage("log_opening_resource", resourceTitle);
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
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_joining", groupLink);
	    //description = usernameLink + " has joined the group " + group;
	    break;
	case group_leaving:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_leaving", groupLink);
	    //description = usernameLink + " has left the group " + group;
	    break;
	case group_creating:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_creating", groupLink);
	    //description = usernameLink + " has created the group " + group;
	    break;
	case group_deleting:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_deleting", groupTitle);
	    //description = usernameLink + " has deleted the group " + groupTitle;
	    break;
	case group_changing_title:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_changing_title", groupLink);
	    //description = usernameLink + " has changed the title of group " + group;
	    break;
	case group_changing_description:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_changing_description", groupLink);
	    //description = usernameLink + " has changed the description of group " + group;
	    break;
	case group_changing_leader:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_changing_leader", groupLink);
	    //description = usernameLink + " has changed the leader of group " + group;
	    break;
	case group_adding_document:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_adding_document", groupLink);
	    //description = usernameLink + " has added a document to " + group;
	    break;
	case group_adding_link:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_adding_link", groupLink);
	    //description = usernameLink + " has added a link to " + group;
	    break;
	case group_deleting_link:
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_deleting_link", groupLink);
	    //description = usernameLink + " has deleted a link from " + group;
	    break;
	case group_removing_resource:
	    if(!StringUtils.isEmpty(params))
		resourceTitle = "<b>" + params + "</b>";
	    description = usernameLink + UtilBean.getLocaleMessage("log_group_removing_resource", resourceTitle, groupLink);
	    //description = usernameLink + " has deleted " + resourceTitle + " from " + group;
	    break;
	case downloading:
	    description = usernameLink + UtilBean.getLocaleMessage("log_downloading", resourceTitle);
	    //description = usernameLink + " has downloaded " + resource;
	    break;
	default:
	    description = "no message for action " + action.name(); // should never happen; 
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

    public int getResourceId()
    {
	return resourceId;
    }

    public String getUserImage()
    {
	return userImage;
    }

    public Resource getResource() throws SQLException
    {
	if(resource == null && resourceId > 0)
	{
	    resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);

	    if(resource == null || resource.isDeleted())
		resourceId = 0;
	}
	return resource;
    }

}

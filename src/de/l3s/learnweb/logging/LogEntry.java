package de.l3s.learnweb.logging;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable
{
    private static final long serialVersionUID = -4239479043091966928L;

    private int userId;
    private Action action;
    private Date date;
    private String params;
    private String username;
    private String description;
    private int resourceId;
    private int groupId;
    private String userImage = "";

    // cache
    private transient Resource resource;

    public LogEntry(ResultSet rs) throws SQLException
    {
        userId = rs.getInt(1);
        User user = Learnweb.getInstance().getUserManager().getUser(userId);
        if(user != null)
        {
            username = user.getUsername();
            userImage = user.getImage();
        }
        else
            username = rs.getString(2);

        action = Action.values()[rs.getInt(3)];
        params = rs.getString(5);
        date = new Date(rs.getTimestamp(6).getTime());
        groupId = rs.getInt(7);

        int targetId = rs.getInt(4);

        switch(action.getTargetId())
        {
        case RESOURCE_ID:
            resourceId = targetId;
            break;
        default:
            break; // right now other id types are not handled
        }

        String url = "";//Learnweb.getInstance().getServerUrl() + "/lw/";

        String usernameLink = "<a href=\"" + url + "user/detail.jsf?user_id=" + userId + "\">" + username + "</a> ";

        String resourceTitle = rs.getString("resource_title");
        resourceTitle = (null == resourceTitle) ? "a resource" : "<b>" + StringHelper.shortnString(resourceTitle, 80) + "</b>";

        String groupTitle = rs.getString("group_title");
        String groupLink;

        if(null == groupTitle)
            groupLink = groupTitle = "private group";
        else
        {
            groupTitle = StringHelper.shortnString(groupTitle, 80);
            groupLink = "<a href=\"" + url + "group/overview.jsf?group_id=" + groupId + "\" >" + groupTitle + "</a>";
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
        case changing_office_resource:
            description = usernameLink + UtilBean.getLocaleMessage("log_document_changing", resourceTitle);
            break;

        case forum_topic_added:
            String topicLink = "<a href=\"" + url + "group/forum_post.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + params + "</a>";
            description = usernameLink + "has added " + "<b>" + topicLink + "</b>" + " post";
            break;

        case forum_post_added:
            String topic = "<a href=\"" + url + "group/forum_post.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + params + "</a>";
            description = usernameLink + "has replied to " + "<b>" + topic + "</b>" + " topic";
            break;

        case deleting_folder:
            description = usernameLink + UtilBean.getLocaleMessage("log_deleting_folder", params);
            break;

        case add_folder:
            description = usernameLink + UtilBean.getLocaleMessage("log_add_folder", params);
            break;

        case adding_resource_metadata:
            description = usernameLink + UtilBean.getLocaleMessage("log_add_resource_metadata", params) + resourceTitle;
            break;

        case login:
            description = usernameLink + UtilBean.getLocaleMessage("log_login", params);
            break;

        case changing_profile:
            description = usernameLink + UtilBean.getLocaleMessage("log_change_profile", params);
            break;

        case submission_view_resource:
            description = usernameLink + UtilBean.getLocaleMessage("log_submission_view_resource", params);
            break;

        case submission_submitted:
            description = usernameLink + UtilBean.getLocaleMessage("log_submission_submit", params);
            break;

        case opening_folder:
            description = usernameLink + UtilBean.getLocaleMessage("log_open_folder", params);
            break;

        case register:
            description = usernameLink + UtilBean.getLocaleMessage("log_register", params);
            break;

        case logout:
            description = usernameLink + UtilBean.getLocaleMessage("log_logout", params);
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
            {
                resourceId = 0;
                resource = null;
            }
        }
        return resource;
    }

    public boolean isQueryNeeded()
    {
        return action == Action.adding_resource && resource != null && !resource.getQuery().equalsIgnoreCase("none");

    }

}

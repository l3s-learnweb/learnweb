package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LogEntry implements Serializable
{
    private static final long serialVersionUID = -4239479043091966928L;

    public enum Action
    { // add new values add the BOTTOM !!!
        tagging_resource, // target_id = resource id, param = the tag
        rating_resource, // target_id = resource id, param = rate
        commenting_resource, // target_id = resource id, param = comment id
        opening_resource, // target_id = resource id
        submission_view_resource, // target_id = submission_id; param = user_id of the submission
        searching, // param = search query
        group_joining, // target_id = group_id
        group_creating, // target_id = group_id
        group_leaving, // target_id = group_id
        login, // param = page the user logged in on
        logout,
        forum_post_added, // target_id = topic_id, param = topic title
        register,
        changing_profile, // target_id = user_id of the user whose profile was changed
        deleting_resource, // target_id = resource id, param = resource title;
        adding_resource, // target_id = resource id
        open_link,
        deleting_comment, // target_id = resource id; param = comment_id
        survey_save, // target_id = survey resource id
        edit_resource, // target_id = resource id
        survey_submit, // target_id = survey resource id
        thumb_rating_resource, // target_id = resource id
        group_deleting, // target_id = group_id
        group_changing_description, // target_id = group_id
        group_changing_title, // target_id = group_id; param = old title
        group_changing_leader, // target_id = group_id
        group_changing_restriction, // target_id = group_id
        group_adding_link, // target_id = group_id; param = title
        group_adding_document, // target_id = group_id; param = title
        opening_folder, // target_id = id of the tagged resource
        group_removing_resource, // target_id = resource id
        deleting_folder, // target_id = folder_id, param = folder name;
        downloading, // target_id = resource_id, param = file_id
        group_deleting_link, // param = title of deleted link; out dated
        group_resource_search, // param = query
        add_folder, // param = folder name; target_id = folder_id
        edit_folder, // param = folder name; target_id = folder_id
        glossary_open, // target_id = resource id, param = glossary id
        glossary_create, // target_id = resource id, param = glossary id
        glossary_entry_edit, // target_id = resource id, param = glossary id
        glossary_entry_add, // target_id = resource id, param = glossary id
        glossary_entry_delete, // target_id = resource id, param = glossary id
        glossary_term_edit, // target_id = resource id, param = glossary id
        glossary_term_add, // target_id = resource id, param = glossary id
        glossary_term_delete, // target_id = resource id, param = glossary_term_id
        resource_thumbnail_update, // target_id = resource_id
        submission_submitted, //target_id = resource_id
        extended_metadata_open_dialog, //target_id = resource_id

        //log entries for extended metadata (yell group only)
        adding_yourown_metadata, //target_id = clicked resource id, param = type of metadata added
        adding_resource_metadata, //target_id = resource id, param = type of metadata added (options: author, language, media source, media type)
        edit_resource_metadata, //target_id = resource id, param = type of metadata edited (options: author, language, media source, media type)
        group_metadata_search, // param = filter:value only if it is not null
        group_category_search, // param = clicked category
        //editing link to hypothesis, experimental
        group_changing_discussion_link,

        changing_resource,
        forum_reply_message,
        moderator_login, // target_id = user_id of the moderator logs into a user account
        course_delete, // target_id = course_id
        course_anonymize // target_id = course_id
        ;

        // This actions are showed in the activity stream
        private final static HashSet<Action> RESOURCE_INTERESTED_ACTIONS = Sets.newHashSet(tagging_resource,
                rating_resource, commenting_resource, opening_resource, adding_resource, deleting_comment,
                changing_resource, edit_resource, thumb_rating_resource);

        private final static HashSet<Action> FOLDER_INTERESTED_ACTIONS = Sets.newHashSet(deleting_folder, add_folder,
                edit_folder);

        public static final HashSet<Action> RESOURCE_RELATED_ACTIONS = Sets.newHashSet(tagging_resource, rating_resource,
                commenting_resource, opening_resource, deleting_resource, adding_resource, edit_resource,
                thumb_rating_resource, group_removing_resource, resource_thumbnail_update, adding_resource_metadata,
                edit_resource_metadata, downloading, resource_thumbnail_update);

        public static final HashSet<Action> SEARCH_RELATED_ACTIONS = Sets.newHashSet(searching, group_resource_search,
                group_category_search, group_metadata_search);

        public static final HashSet<Action> GLOSSARY_RELATED_ACTIONS = Sets.newHashSet(glossary_open, glossary_create,
                glossary_entry_edit, glossary_entry_add, glossary_entry_delete, glossary_term_edit, glossary_term_add,
                glossary_term_delete);

        public static final HashSet<Action> FOLDER_ACTIONS = Sets.newHashSet(deleting_folder, add_folder,
                edit_folder, opening_folder);

        public static final HashSet<Action> GROUP_ACTIONS = Sets.newHashSet(group_adding_document, group_adding_link,
                group_joining, group_creating, group_leaving, group_deleting, group_changing_description, group_changing_discussion_link, group_changing_leader,
                group_changing_restriction, group_changing_title, group_removing_resource, group_deleting_link);
        public static final HashSet<Action> LOG_ACTIONS = Sets.newHashSet(login, logout, moderator_login,
                register);
        public static final HashSet<Action> OTHER_ACTIONS = Sets.newHashSet(submission_view_resource, forum_post_added, forum_reply_message,
                changing_profile, open_link, deleting_comment, survey_save, survey_submit, submission_submitted, extended_metadata_open_dialog, adding_yourown_metadata, course_delete, course_anonymize);
    }

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
        User user = Learnweb.getInstance().getUserManager().getUser(userId);
        if(user != null)
            username = user.getUsername();
        else
            username = rs.getString(2);

        action = Action.values()[rs.getInt(3)];
        params = rs.getString(5);
        date = new Date(rs.getTimestamp(6).getTime());
        groupId = rs.getInt(7);
        userImage = User.getImage(rs.getInt("image_file_id"));

        int targetId = rs.getInt(4);

        if(Action.RESOURCE_INTERESTED_ACTIONS.contains(action))
            resourceId = targetId;
        /* currently not used
        else if(Action.FOLDER_INTERESTED_ACTIONS.contains(action))
            fileId = targetId;
        */
        String url = "";//Learnweb.getInstance().getServerUrl() + "/lw/";

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
        case group_changing_discussion_link:
            description = usernameLink + UtilBean.getLocaleMessage("log_changing_discussion_link", resourceTitle);
            break;
        case changing_resource:
            description = usernameLink + UtilBean.getLocaleMessage("log_document_changing", resourceTitle, groupLink);
            break;

        case forum_post_added:
            String topicLink = "<a href=\"" + url + "group/forum_post.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + params + "</a>";
            description = usernameLink + "has added " + "<b>" + topicLink + "</b>" + " post";
            break;

        case forum_reply_message:
            String topic = "<a href=\"" + url + "group/forum_post.jsf?topic_id=" + targetId + "\" style=\" color:black;font-weight:bold\">" + params + "</a>";
            description = usernameLink + "has replied to " + "<b>" + topic + "</b>" + " topic";
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

    public static void main(String[] arg)
    {
        Action[] actions = { Action.glossary_term_delete };

        for(Action action : actions)
        {
            System.out.print(action.ordinal() + ",");
        }

        System.out.print("\nCASE action");

        for(Action action : actions)
        {
            System.out.print(" WHEN " + action.ordinal() + " THEN '" + action.name() + "'");
        }
        System.out.println(" END CASE");
    }
}

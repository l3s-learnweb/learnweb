package de.l3s.learnwebAdminBeans;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.GroupManager;
import de.l3s.learnweb.JForumManager;
import de.l3s.learnweb.JForumManager.ForumStatistic;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.OwnerList;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.util.StringHelper;

@ManagedBean
@RequestScoped
public class AdminStatisticsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 5584983377737726111L;
    private List<Map<String, String>> groupStatistics;
    private boolean showDetails = true;
    private String detailedDescription = "";

    public TreeSet<Integer> getSelectedUsers() throws SQLException
    {

	HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
	String[] tempSelectedUsers = request.getParameterValues("selected_users");

	if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
	{
	    addMessage(FacesMessage.SEVERITY_WARN, "select_user");
	    return null;
	}

	TreeSet<Integer> selectedUsersSet = new TreeSet<Integer>();
	for(String userId : tempSelectedUsers)
	{
	    selectedUsersSet.add(Integer.parseInt(userId));

	}

	return selectedUsersSet;
    }

    public TreeSet<Integer> getSelectedGroups() throws SQLException
    {

	HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
	String[] tempSelectedGroups = request.getParameterValues("selected_groups");

	if(null == tempSelectedGroups || tempSelectedGroups.length == 0)
	{
	    addMessage(FacesMessage.SEVERITY_WARN, "select_group");
	    return null;
	}

	TreeSet<Integer> selectedGroupsSet = new TreeSet<Integer>();
	for(String groupId : tempSelectedGroups)
	{
	    selectedGroupsSet.add(Integer.parseInt(groupId));

	}

	return selectedGroupsSet;
    }

    public void onGroupStatistics() throws Exception
    {
	groupStatistics = new LinkedList<Map<String, String>>();
	GroupManager gm = getLearnweb().getGroupManager();

	try
	{
	    TreeSet<Integer> selectedGroups = getSelectedGroups();

	    if(null == selectedGroups)
		return;

	    String query = "SELECT g.title, g.forum_id, COUNT(r.resource_id) AS resources, IFNULL(SUM(rate_number), 0) AS ratings, "
		    + "(SELECT count(*) FROM lw_group_resource gr JOIN lw_resource ir ON gr.resource_id = ir.resource_id AND ir.deleted=0 JOIN lw_thumb c ON c.resource_id=ir.resource_id WHERE gr.group_id = g.group_id) as thumb_ratings, "
		    + "(SELECT count(*) FROM lw_group_resource gr JOIN lw_resource ir ON gr.resource_id = ir.resource_id AND ir.deleted=0 JOIN lw_comment c ON c.resource_id=ir.resource_id WHERE gr.group_id = g.group_id) as comments, "
		    + "(SELECT count(*) FROM lw_group_resource gr JOIN lw_resource ir ON gr.resource_id = ir.resource_id AND ir.deleted=0 JOIN lw_resource_tag t ON t.resource_id=ir.resource_id WHERE gr.group_id = g.group_id) as tags, "
		    + "(SELECT count(*) FROM lw_group_resource gr JOIN lw_resource ir ON gr.resource_id = ir.resource_id AND ir.deleted=0 JOIN lw_resource_archiveurl t ON t.resource_id=ir.resource_id WHERE gr.group_id = g.group_id) as no_of_archived_versions, "
		    + "(SELECT count(distinct(t.resource_id)) FROM lw_group_resource gr JOIN lw_resource ir ON gr.resource_id = ir.resource_id AND ir.deleted=0 JOIN lw_resource_archiveurl t ON t.resource_id=ir.resource_id WHERE gr.group_id = g.group_id) as no_of_archived_resources "
		    + "FROM `lw_group` g " + "LEFT JOIN lw_group_resource ogr USING(group_id) " + "LEFT JOIN lw_resource r ON r.resource_id=ogr.resource_id AND r.deleted=0 " + "WHERE group_id IN(" + StringHelper.implodeInt(selectedGroups, ",") + ") " + "GROUP BY group_id";
	    System.out.println(StringHelper.implodeInt(selectedGroups, ","));

	    ResultSet rs = Learnweb.getInstance().getConnection().createStatement().executeQuery(query);

	    JForumManager forumManager = Learnweb.getInstance().getJForumManager();

	    while(rs.next())
	    {
		Map<String, String> result = new HashMap<String, String>();

		result.put("title", rs.getString("title"));
		result.put("resources", rs.getString("resources"));
		result.put("star_ratings", rs.getString("ratings"));
		result.put("thumb_ratings", rs.getString("thumb_ratings"));
		result.put("comments", rs.getString("comments"));
		result.put("tags", rs.getString("tags"));
		result.put("no_of_archived_versions", rs.getString("no_of_archived_versions"));
		result.put("no_of_archived_resources", rs.getString("no_of_archived_resources"));

		ForumStatistic forumStatistics = forumManager.getForumStatistics(rs.getInt("forum_id"));

		if(forumStatistics != null)
		{
		    result.put("forum_topics", Integer.toString(forumStatistics.getTopics()));
		    result.put("forum_posts", Integer.toString(forumStatistics.getPosts()));
		}
		else
		{
		    result.put("forum_topics", "-");
		    result.put("forum_posts", "-");
		}

		groupStatistics.add(result);

	    }
	    /*
	     * - Group:  English Language Lab 3
	        - resources uploaded in this group from the desktop  (in alphabetical order)
	            - resource A - Title, source, description, type, comments, tags
	            - resource B - Title, source, description, type, comments, tags
	            - resource C - -----
	        - resources uploaded from the Web (in alphabetical order)
	     */

	    if(showDetails)
	    {
		String[] type = { "", "uploaded resource", "web resource" };
		String[] color = { "", "green", "blue" };

		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");

		for(Integer groupId : selectedGroups)
		{
		    Group group = gm.getGroupById(groupId);

		    sb.append("<li><div style=\"color:red\">Group: ");
		    sb.append(group.getTitle());
		    sb.append("</div>\n<ul>");

		    OwnerList<Resource, User> resources = group.getResources();
		    Collections.sort(resources, new ResourceComparater());

		    for(Resource resource : resources)
		    {
			sb.append("\n\t<li><div style=\"color:" + color[resource.getStorageType()] + "\">");
			sb.append(resource.getTitle());
			sb.append("; ");
			sb.append(type[resource.getStorageType()]);
			sb.append("; Source: ");
			sb.append(resource.getSource());
			sb.append("; Tags: ");
			sb.append(resource.getTagsAsString());
			sb.append("</div>");
			/*
						if(resource.getComments().size() == 0)
						{
						    sb.append("; no comments");
						}

						if(resource.getDescription().length() == 0)
						{
						    sb.append("; no description");
						}
					*/
			if(resource.getDescription().length() != 0)
			{
			    sb.append("<div class=\"description\" style=\"display:none\"><h4>Description</h4>");
			    sb.append(resource.getDescription());
			    sb.append("</div>");
			}

			if(resource.getComments().size() != 0)
			{
			    sb.append("<h4 class=\"admin_comments\" style=\"display:none\">Comments</h4><table class=\"admin_comments\" style=\"display:none\" border='1' cellspacing='0'>");
			    for(Comment comment : resource.getComments())
			    {
				sb.append("<tr><td>");
				sb.append(comment.getUser().getUsername());
				sb.append("</td><td>");
				sb.append(comment.getDate());
				sb.append("</td><td>");
				sb.append(comment.getText());
				sb.append("</td></tr>");
			    }
			    sb.append("</table>");
			}
			sb.append("</li>");
		    }
		    sb.append("</ul></li>");
		}

		sb.append("</ul>");
		detailedDescription = sb.toString();

	    }
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}

    }

    private class ResourceComparater implements Comparator<Resource>
    {
	@Override
	public int compare(Resource o1, Resource o2)
	{
	    if(o1.getStorageType() == o2.getStorageType())
		return o1.getTitle().compareTo(o2.getTitle());

	    return o1.getStorageType() - o2.getStorageType();
	}

    }

    public List<Map<String, String>> getGroupStatistics()
    {
	return groupStatistics;
    }

    public boolean isShowDetails()
    {
	return showDetails;
    }

    public void setShowDetails(boolean showDetails)
    {
	this.showDetails = showDetails;
    }

    public String getDetailedDescription()
    {
	return detailedDescription;
    }
}

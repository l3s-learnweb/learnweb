package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jdbi.v3.core.Handle;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.Resource;
import de.l3s.util.bean.BeanHelper;

@Named
@RequestScoped
public class AdminStatisticsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 5584983377737726111L;

    private TreeNode[] selectedNodes;
    private boolean showDetails = true;
    private String detailedDescription = "";

    private TreeNode treeRoot;
    private List<Map<String, String>> groupStatistics;

    @Inject
    private GroupDao groupDao;

    @PostConstruct
    public void init() {
        treeRoot = BeanHelper.createGroupsUsersTree(getUser(), getLocale(), false);
    }

    public void fetchStatistics() {
        groupStatistics = new LinkedList<>();

        Collection<Integer> selectedGroups = BeanHelper.getSelectedGroups(selectedNodes);
        if (selectedGroups.isEmpty()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "You have to select at least one group.");
            return;
        }

        String query = "SELECT g.group_id, g.title, COUNT(r.resource_id) AS resources, IFNULL(SUM(rate_number), 0) AS ratings, "
            + "(SELECT count(*) FROM lw_resource ir JOIN lw_resource_thumb c ON c.resource_id=ir.resource_id WHERE ir.deleted=0 AND ir.group_id = g.group_id) as thumb_ratings, "
            + "(SELECT count(*) FROM lw_resource ir JOIN lw_comment c ON c.resource_id=ir.resource_id WHERE ir.deleted=0 AND ir.group_id = g.group_id) as comments, "
            + "(SELECT count(*) FROM lw_resource ir JOIN lw_resource_tag t ON t.resource_id=ir.resource_id WHERE ir.deleted=0 AND ir.group_id = g.group_id) as tags, "
            + "(SELECT count(*) FROM lw_resource ir JOIN lw_resource_archiveurl t ON t.resource_id=ir.resource_id WHERE ir.deleted=0 AND ir.group_id = g.group_id) as no_of_archived_versions, "
            + "(SELECT count(distinct(t.resource_id)) FROM lw_resource ir JOIN lw_resource_archiveurl t ON t.resource_id=ir.resource_id WHERE ir.deleted=0 AND ir.group_id = g.group_id) as no_of_archived_resources "
            + "FROM lw_group g LEFT JOIN lw_resource r USING(group_id) WHERE r.deleted=0 AND group_id IN(<selectedGroups>) GROUP BY group_id";

        try (Handle handle = getLearnweb().openJdbiHandle()) {
            groupStatistics = handle.createQuery(query).bindList("selectedGroups", selectedGroups).map((rs, ctx) -> {
                Map<String, String> result = new HashMap<>();

                int groupId = rs.getInt("group_id");
                result.put("title", rs.getString("title"));
                result.put("resources", rs.getString("resources"));
                result.put("star_ratings", rs.getString("ratings"));
                result.put("thumb_ratings", rs.getString("thumb_ratings"));
                result.put("comments", rs.getString("comments"));
                result.put("tags", rs.getString("tags"));
                result.put("no_of_archived_versions", rs.getString("no_of_archived_versions"));
                result.put("no_of_archived_resources", rs.getString("no_of_archived_resources"));

                String forumQuery = "SELECT COUNT(DISTINCT t.topic_id) AS topics, COUNT(*) AS posts "
                    + "FROM lw_forum_post p INNER JOIN lw_forum_topic t ON t.topic_id = p.topic_id "
                    + "WHERE t.group_id = ?";

                handle.select(forumQuery, groupId).map((rs1, ctx1) -> {
                    result.put("forum_topics", rs1.getString("topics"));
                    result.put("forum_posts", rs1.getString("posts"));
                    return null;
                }).one();

                return result;
            }).list();
        }

        if (showDetails) {
            String[] type = {"uploaded resource", "web resource"};
            String[] color = {"green", "blue"};

            StringBuilder sb = new StringBuilder();
            sb.append("<ul>");

            for (Integer groupId : selectedGroups) {
                Group group = groupDao.findByIdOrElseThrow(groupId);

                sb.append("<li><div style=\"color:red\">Group: ");
                sb.append(group.getTitle());
                sb.append("</div>\n<ul>");

                List<Resource> resources = group.getResources();
                resources.sort(Resource.createTitleComparator());

                for (Resource resource : resources) {
                    sb.append("\n\t<li><div style=\"color:").append(color[resource.getStorageType().ordinal()]).append("\">");
                    sb.append(resource.getTitle());
                    sb.append("; ");
                    sb.append(type[resource.getStorageType().ordinal()]);
                    sb.append("; Source: ");
                    sb.append(resource.getService());
                    sb.append("; Tags: ");
                    sb.append(resource.getTagsAsString());
                    sb.append("</div>");
                    /*
                    if (resource.getComments().size() == 0) {
                        sb.append("; no comments");
                    } if (resource.getDescription().length() == 0) {
                        sb.append("; no description");
                    }
                    */
                    if (!resource.getDescription().isEmpty()) {
                        sb.append("<div class=\"description\" style=\"display:none\"><h4>Description</h4>");
                        sb.append(resource.getDescription());
                        sb.append("</div>");
                    }

                    if (!resource.getComments().isEmpty()) {
                        sb.append("<h4 class=\"admin_comments\" style=\"display:none\">Comments</h4>");
                        sb.append("<table class=\"admin_comments\" style=\"display:none\" border='1' cellspacing='0'>");
                        for (Comment comment : resource.getComments()) {
                            sb.append("<tr><td>");
                            sb.append(comment.getUser().getUsername());
                            sb.append("</td><td>");
                            sb.append(comment.getCreatedAt());
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

    public TreeNode getTreeRoot() {
        return treeRoot;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(final TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public boolean isShowDetails() {
        return showDetails;
    }

    public void setShowDetails(boolean showDetails) {
        this.showDetails = showDetails;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public List<Map<String, String>> getGroupStatistics() {
        return groupStatistics;
    }
}

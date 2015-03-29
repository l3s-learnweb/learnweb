package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import de.l3s.learnweb.Link.LinkType;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.util.HasId;

public class Group implements Comparable<Group>, HasId, Serializable
{
    private static final long serialVersionUID = -6209978709028007958L;

    private int id;
    private int leaderUserId;
    private User leader;
    private int courseId;
    private int forumId;

    @NotBlank
    @Size(min = 3, max = 60)
    private String title;
    private String description;
    private String university;
    private String metaInfo1;
    private String location;
    @Size(max = 50)
    private String language;
    private Course course;
    private int parentGroupId;
    private transient Group parentGroup;
    private String subgroupsLabel;

    private boolean restrictionOnlyLeaderCanAddResources;

    // caches
    private transient List<Link> documentLinks;
    private transient OwnerList<Resource, User> resources;
    private transient List<User> members;
    private transient List<Link> links;
    private transient List<Group> subgroups;

    private HashMap<Integer, Integer> lastVisitCache = new HashMap<Integer, Integer>();

    public void clearCaches()
    {
	documentLinks = null;
	resources = null;
	members = null;
	links = null;
	subgroups = null;
    }

    public Group()
    {
	this.id = -1;
    }

    public Group(ResultSet rs) throws SQLException
    {
	this.id = rs.getInt("group_id");
	this.title = rs.getString("title");
	this.description = rs.getString("description");
	this.leaderUserId = rs.getInt("leader_id");
	this.university = rs.getString("university");
	this.metaInfo1 = rs.getString("course");
	this.location = rs.getString("location");
	this.language = rs.getString("language");
	this.courseId = rs.getInt("course_id");
	this.forumId = rs.getInt("forum_id");
	this.restrictionOnlyLeaderCanAddResources = rs.getInt("restriction_only_leader_can_add_resources") == 1;
	this.parentGroupId = rs.getInt("parent_group_id");
	this.subgroupsLabel = rs.getString("subgroup_label");
    }

    @Override
    public int getId()
    {
	return id;
    }

    public List<User> getMembers() throws SQLException
    {
	if(null == members)
	{
	    members = Learnweb.getInstance().getUserManager().getUsersByGroupId(id);
	}
	return members;
    }

    /**
     * 
     * @param user Returns TRUE if the user is member of this group
     * @throws SQLException
     */
    public boolean isMember(User user) throws SQLException
    {
	getMembers(); // make sure members are loaded

	return members.contains(user);
    }

    public boolean isLeader(User user) throws SQLException
    {
	return user.getId() == leaderUserId;
    }

    public User getLeader() throws SQLException
    {
	if(null == leader)
	    leader = Learnweb.getInstance().getUserManager().getUser(leaderUserId);
	return leader;
    }

    @Override
    public int compareTo(Group g)
    {
	return this.getTitle().compareTo(g.getTitle());
    }

    @Override
    public boolean equals(Object obj)
    {
	if(obj.getClass() == this.getClass())
	{
	    Group g2 = (Group) obj;
	    return g2.getId() == this.getId();
	}

	return false;
    }

    public OwnerList<Resource, User> getResources() throws SQLException
    {
	// caching disabled until a good strategie exist how to deal with deleting resources on the my resource page
	//if(null == resources) 
	//{
	ResourceManager rm = Learnweb.getInstance().getResourceManager();
	resources = rm.getResourcesByGroupId(id);
	//}
	return resources;
    }

    public boolean addResource(Resource resource, User user) throws SQLException
    {
	return Learnweb.getInstance().getResourceManager().addResourceToGroup(resource, this, user);
    }

    public void removeResource(Resource resource, User user) throws SQLException
    {
	Learnweb.getInstance().getResourceManager().removeResourceFromGroup(resource, this, user);
    }

    //metadata

    public String getTitle()
    {
	return title;
    }

    public void setTitle(String title) throws SQLException
    {
	this.title = title;
    }

    public String getDescription()
    {
	return description;
    }

    public void setDescription(String description) throws SQLException
    {
	this.description = description;
    }

    public String getUniversity()
    {
	return university;
    }

    public void setUniversity(String university)
    {
	this.university = university;
    }

    public String getLocation()
    {
	return location;
    }

    public void setLocation(String location)
    {
	this.location = location;
    }

    public String getLanguage()
    {
	return language;
    }

    public void setLanguage(String language)
    {
	this.language = language;
    }

    public String getMetaInfo1()
    {
	return metaInfo1;
    }

    public void setMetaInfo1(String course)
    {
	this.metaInfo1 = course;
    }

    public void setId(int id)
    {
	this.id = id;
    }

    /**
     * The course of the user who created this group and which course this group will belong to
     * 
     * @return
     */
    public int getCourseId()
    {
	return courseId;
    }

    public Course getCourse() throws SQLException
    {
	if(null == this.course)
	    this.course = Learnweb.getInstance().getCourseManager().getCourseById(courseId);

	return this.course;
    }

    /**
     * The course which this group will belong to
     */
    public void setCourseId(int courseId)
    {
	this.courseId = courseId;
    }

    public int getForumId()
    {
	return forumId;
    }

    public int getLeaderUserId()
    {
	return leaderUserId;
    }

    public void setLeader(User user) throws SQLException
    {
	this.leaderUserId = user.getId();
	this.leader = user;
    }

    public void setLeaderUserId(int userId) throws SQLException
    {
	this.leaderUserId = userId;
	this.leader = null; // force reload
    }

    public boolean isRestrictionOnlyLeaderCanAddResources()
    {
	return restrictionOnlyLeaderCanAddResources;
    }

    public void setRestrictionOnlyLeaderCanAddResources(boolean restrictionOnlyLeaderCanAddResources) throws SQLException
    {
	this.restrictionOnlyLeaderCanAddResources = restrictionOnlyLeaderCanAddResources;
    }

    @Override
    public String toString()
    {
	return "Group [id=" + id + ", courseId=" + courseId + ", forumId=" + forumId + ", title=" + title + "]";
    }

    public void setForumId(int forumId) throws SQLException
    {
	this.forumId = forumId;
    }

    public void clearLinksCache()
    {
	documentLinks = null;
    }

    public List<Link> getDocumentLinks() throws SQLException
    {
	if(null == documentLinks)
	    documentLinks = Learnweb.getInstance().getLinkManager().getLinksByGroupId(id, LinkType.DOCUMENT);

	return documentLinks;
    }

    public List<Link> getLinks() throws SQLException
    {
	if(null == links)
	    links = Learnweb.getInstance().getLinkManager().getLinksByGroupId(id, LinkType.LINK);

	return Collections.unmodifiableList(links);
    }

    public void addLink(String title, String url, LinkType type) throws SQLException
    {
	Link link = new Link();
	link.setGroupId(id);
	link.setType(type);
	link.setTitle(title);
	link.setUrl(url);
	Learnweb.getInstance().getLinkManager().save(link);

	links = null; // force reload
	documentLinks = null;
    }

    public void deleteLink(int linkId) throws SQLException
    {
	Learnweb.getInstance().getLinkManager().deleteLink(linkId);

	links = null; // force reload
	documentLinks = null;
    }

    public String getForumUrl(User user) throws SQLException
    {
	return Learnweb.getInstance().getForumManger().getForumUrl(user, forumId);
    }

    public int getParentGroupId()
    {
	return parentGroupId;
    }

    public void setParentGroupId(int parentGroupId)
    {
	this.parentGroupId = parentGroupId;
	this.parentGroup = null; // force reload
    }

    public Group getParentGroup() throws IllegalArgumentException, SQLException
    {
	if(parentGroupId != 0 && null == parentGroup)
	    parentGroup = Learnweb.getInstance().getGroupManager().getGroupById(parentGroupId);
	return parentGroup;
    }

    public void setParentGroup(Group parentGroup)
    {
	this.parentGroupId = parentGroup.getId();
	this.parentGroup = parentGroup;
    }

    public String getSubgroupsLabel()
    {
	return subgroupsLabel;
    }

    public void setSubgroupLabel(String subgroupLabel)
    {
	this.subgroupsLabel = subgroupLabel;
    }

    public List<Group> getSubgroups() throws SQLException
    {
	if(null == subgroups)
	{
	    subgroups = Learnweb.getInstance().getGroupManager().getSubgroups(this);
	}
	return subgroups;
    }

    public void addSubgroup(Group subgroup) throws SQLException
    {
	subgroup.setParentGroup(this);
	Learnweb.getInstance().getGroupManager().addSubgroup(this, subgroup);

	// update cache
	if(subgroups == null)
	    subgroups = new ArrayList<Group>();
	subgroups.add(subgroup);
    }

    public void setLastVisit(User user) throws Exception
    {
	int time = UtilBean.time();
	Learnweb.getInstance().getGroupManager().setLastVisit(user, this, time);
	lastVisitCache.put(user.getId(), time);
    }

    public int getLastVisit(User user) throws Exception
    {
	Integer time = lastVisitCache.get(user.getId());
	if(null != time)
	    return time.intValue();

	time = Learnweb.getInstance().getGroupManager().getLastVisit(user, this);
	lastVisitCache.put(user.getId(), time);
	return time;
    }

    /**
     * 
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entrys are returned
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getLogs(Action[] actions, int limit) throws SQLException
    {
	return Learnweb.getInstance().getLogsByGroup(id, actions, limit);
    }

    /**
     * returns the 5 newest log entrys
     * 
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getLogs() throws SQLException
    {
	return getLogs(null, 5);
    }

}

package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;

/**
 * DAO for the Group class.
 * Because there are only a few Groups we keep them all in memory
 * 
 * @author Philipp
 * 
 */
public class GroupManager
{

    // if you change this, you have to change the constructor of Group too
    private final static String COLUMNS = "g.group_id, g.title, g.description, g.leader_id, g.course_id, g.university, g.course, g.location, g.language, g.forum_id, g.restriction_only_leader_can_add_resources, g.parent_group_id, g.subgroup_label, lw_group_category.group_category_id, lw_group_category.category_title, lw_group_category.category_abbreviation";

    private Learnweb learnweb;
    private ICache<Group> cache;

    public GroupManager(Learnweb learnweb) throws SQLException
    {
	super();
	Properties properties = learnweb.getProperties();
	int groupCacheSize = Integer.parseInt(properties.getProperty("GROUP_CACHE"));

	this.learnweb = learnweb;
	this.cache = groupCacheSize == 0 ? new DummyCache<Group>() : new Cache<Group>(groupCacheSize);
    }

    public void resetCache() throws SQLException
    {
	cache.clear();
    }

    private List<Group> getGroups(String query, int... params) throws SQLException
    {
	List<Group> groups = new LinkedList<Group>();
	PreparedStatement select = learnweb.getConnection().prepareStatement(query);

	int i = 1;
	for(int param : params)
	    select.setInt(i++, param);

	ResultSet rs = select.executeQuery();

	while(rs.next())
	{
	    groups.add(createGroup(rs));
	}
	select.close();

	return groups;
    }

    /**
     * Returns a list of all Groups a user belongs to
     * 
     * @throws SQLException
     */
    public List<Group> getGroupsByUserId(int userId) throws SQLException
    {
	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_group_user u USING(group_id) WHERE u.user_id = ? AND g.deleted = 0 ORDER BY title";
	return getGroups(query, userId);
    }

    /**
     * Returns a list of all Groups which belong to the defined course
     * 
     * @throws SQLException
     */
    public List<Group> getGroupsByCourseId(int courseId) throws SQLException
    {
	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE g.course_id = ? AND g.deleted = 0 ORDER BY title";
	return getGroups(query, courseId);
    }

    /**
     * Returns a list of Groups which belong to the defined course and were created after the specified date
     */
    public List<Group> getGroupsByCourseId(int courseId, Date newerThan) throws SQLException
    {
	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE g.course_id = ? AND g.deleted = 0 AND `creation_time` > FROM_UNIXTIME(?) ORDER BY title";
	return getGroups(query, courseId, (int) (newerThan.getTime() / 1000));
    }

    public List<Group> getGroupsByResourceId(int resourceId) throws SQLException
    {
	if(resourceId <= 0)
	    return new LinkedList<Group>();

	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_group_resource USING(group_id) WHERE resource_id = ? AND g.deleted = 0 ORDER BY g.title";
	return getGroups(query, resourceId);
    }

    /**
     * Returns a list of all Groups a user belongs to and which groups are also part of the defined course
     * 
     * @throws SQLException
     */
    public List<Group> getGroupsByUserIdFilteredByCourseId(int userId, int courseId) throws SQLException
    {
	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_group_user USING(group_id) WHERE user_id = ? AND g.course_id = ? AND g.deleted = 0 ORDER BY title";
	return getGroups(query, userId, courseId);
    }

    public Group getGroupByTitleFilteredByOrganisation(String title, int organisationId) throws SQLException
    {
	PreparedStatement pstmtGetGroup = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_course gc USING(course_id) WHERE g.title LIKE ? AND organisation_id = ? AND g.deleted = 0");
	pstmtGetGroup.setString(1, title);
	pstmtGetGroup.setInt(2, organisationId);
	ResultSet rs = pstmtGetGroup.executeQuery();

	if(!rs.next())
	    return null;

	Group group = createGroup(rs);

	pstmtGetGroup.close();

	return group;
    }

    private Group createGroup(ResultSet rs) throws SQLException
    {
	Group group = cache.get(rs.getInt("group_id"));
	if(null == group)
	{
	    group = new Group(rs);
	    group = cache.put(group);
	}
	return group;
    }

    /**
     * Returns all groups a user can join
     * This are all groups of his courses except for groups he has already joined
     * 
     * @param user
     * @return
     * @throws SQLException
     */
    public List<Group> getJoinAbleGroups(User user) throws SQLException
    {
	StringBuilder sb = new StringBuilder(",-1");
	for(Course course : user.getCourses())
	    sb.append("," + course.getId());
	String coursesIn = sb.substring(1);

	sb = new StringBuilder(",-1"); // make sure that the string is not empty
	for(Group group : user.getGroups())
	    sb.append("," + group.getId());
	String groupsIn = sb.substring(1);

	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE g.group_id NOT IN(" + groupsIn + ") AND g.course_id IN(" + coursesIn + ") AND g.deleted = 0 ORDER BY title";
	return getGroups(query);
    }

    /**
     * Get a Group by her id
     * 
     * @param id
     * @return
     * @throws SQLException
     */
    public Group getGroupById(int id) throws SQLException
    {
	Group group = cache.get(id);

	if(null != group)
	    return group;

	PreparedStatement pstmtGetGroup = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE group_id = ?");
	pstmtGetGroup.setInt(1, id);
	ResultSet rs = pstmtGetGroup.executeQuery();

	if(!rs.next())
	    return null;

	group = new Group(rs);
	pstmtGetGroup.close();

	group = cache.put(group);
	return group;
    }

    /**
     * Saves the Group to the database.
     * If the Group is not yet stored at the database, a new record will be created and the returned Group contains the new id.
     * 
     * @param Group
     * @return
     * @throws SQLException
     */
    public synchronized Group save(Group group) throws SQLException
    {
	PreparedStatement replace = learnweb.getConnection().prepareStatement(
		"REPLACE INTO `lw_group` (group_id, `title` ,`description` ,`leader_id` , university, course, location, language, course_id, parent_group_id, subgroup_label, forum_id, group_category_id) VALUES (?, ?,?, ?, ?, ?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

	if(group.getId() < 0) // the Group is not yet stored at the database 
	{
	    replace.setNull(1, java.sql.Types.INTEGER);

	    if(group.getCategoryId() != 0)
	    {
		group.setCategoryAbbreviation("TDLab"); // TODO get values from db
		group.setCategoryTitle("TDLab");
	    }
	}
	else
	    replace.setInt(1, group.getId());
	replace.setString(2, group.getTitle());
	replace.setString(3, group.getDescription());
	replace.setInt(4, group.getLeaderUserId()); // leader id		
	replace.setString(5, group.getUniversity());
	replace.setString(6, group.getMetaInfo1());
	replace.setString(7, group.getLocation());
	replace.setString(8, group.getLanguage());
	replace.setInt(9, group.getCourseId());
	replace.setInt(10, group.getParentGroupId());
	replace.setString(11, group.getSubgroupsLabel());
	replace.setInt(12, group.getForumId());
	replace.setInt(13, group.getCategoryId());
	replace.executeUpdate();

	if(group.getId() < 0) // get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    group.setId(rs.getInt(1));
	    group = cache.put(group.getId(), group); // add the new Group to the cache
	}
	else if(cache.get(group.getId()) != null)//remove old group and add the new one
	{
	    cache.remove(group.getId());
	    cache.put(group);
	}
	replace.close();

	int groupId = group.getId();

	if(groupId != group.getId())
	    throw new RuntimeException("fedora error");

	return group;
    }

    /**
     * 
     * @param group
     * @return The subgroups of a group (only next level not subgroups of subgroups)
     * @throws SQLException
     */
    public List<Group> getSubgroups(Group group) throws SQLException
    {
	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE g.parent_group_id = ? AND g.deleted = 0 ORDER BY title";
	return getGroups(query, group.getId());
    }

    /**
     * Adds a subgroup to a group
     * 
     * @param group Must have been stored before
     * @param subgroup May NOT have been stored before.
     * @throws SQLException
     */
    public void addSubgroup(Group group, Group subgroup) throws SQLException
    {
	save(subgroup);
    }

    public void addUserToGroup(User user, Group group) throws SQLException
    {
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
	select.setInt(1, group.getId());
	select.setInt(2, user.getId());
	ResultSet rs = select.executeQuery();
	boolean userIsAlreadyMember = rs.next();
	select.close();

	if(userIsAlreadyMember)
	    return;

	PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_group_user` (`group_id`,`user_id`) VALUES (?,?)");
	insert.setInt(1, group.getId());
	insert.setInt(2, user.getId());
	insert.execute();
	insert.close();
    }

    public void removeUserFromGroup(User user, Group group) throws SQLException
    {
	PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
	delete.setInt(1, group.getId());
	delete.setInt(2, user.getId());
	delete.execute();
	delete.close();
    }

    public void deleteGroup(int groupId) throws SQLException
    {
	PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_user` WHERE `group_id` = ?");
	delete.setInt(1, groupId);
	delete.execute();
	delete.close();

	delete = learnweb.getConnection().prepareStatement("UPDATE `lw_group` SET deleted = 1 WHERE `group_id` = ?");
	delete.setInt(1, groupId);
	delete.execute();
	delete.close();

	cache.remove(groupId);
    }

    /**
     * Define at which timestamp the user has visited the group the last time
     * 
     * @param user
     * @param group
     * @param time
     * @throws SQLException
     */
    public void setLastVisit(User user, Group group, int time) throws SQLException
    {
	PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_group_user` SET `last_visit` = ? WHERE `group_id` = ? AND `user_id` = ?");
	update.setInt(1, time);
	update.setInt(2, group.getId());
	update.setInt(3, user.getId());
	update.executeUpdate();
	update.close();
    }

    /**
     * 
     * @param user
     * @param group
     * @return timestamp when the user has visited the group the last time
     * @throws SQLException
     */
    public int getLastVisit(User user, Group group) throws SQLException
    {
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT `last_visit` FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
	select.setInt(1, group.getId());
	select.setInt(2, user.getId());
	ResultSet rs = select.executeQuery();
	if(!rs.next())
	    return -1;

	int time = rs.getInt(1);
	select.close();
	return time;
    }

    /*
    public static void main(String[] args) throws SQLException // tests	
    {
    	Learnweb lw = Learnweb.getInstance();
    	GroupManager om = lw.getGroupManager();
    	
    	System.out.println("All Groups:");
    	for(Group o : om.getGroupsAll())
    		System.out.println(o);
    	
    	System.out.println("Create new Group");
    	Group o2 = new Group();
    	o2.setTitle("test title");
    	o2.setLogo("logo");
    	System.out.println("pre save: "+o2);
    	om.save(o2);
    	System.out.println("post save: "+o2);
    	o2.setTitle("changed title");
    	om.save(o2);
    	
    	System.out.println("All Groups:");
    	for(Group o : om.getGroupsAll())
    		System.out.println(o);
    }*/
}

package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.Sql;

/**
 * DAO for the Group class.
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

    protected GroupManager(Learnweb learnweb) throws SQLException
    {
	int groupCacheSize = learnweb.getProperties().getPropertyIntValue("GROUP_CACHE");

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

    public static String bbcode(String text)
    {
	String html = text;

	Map<String, String> bbMap = new HashMap<String, String>();

	bbMap.put("(\r\n|\r|\n|\n\r)", "<br/>");
	bbMap.put("\\[b\\](.+?)\\[/b\\]", "<strong>$1</strong>");
	bbMap.put("\\[i\\](.+?)\\[/i\\]", "<span style='font-style:italic;'>$1</span>");
	bbMap.put("\\[u\\](.+?)\\[/u\\]", "<span style='text-decoration:underline;'>$1</span>");
	bbMap.put("\\[h1\\](.+?)\\[/h1\\]", "<h1>$1</h1>");
	bbMap.put("\\[h2\\](.+?)\\[/h2\\]", "<h2>$1</h2>");
	bbMap.put("\\[h3\\](.+?)\\[/h3\\]", "<h3>$1</h3>");
	bbMap.put("\\[h4\\](.+?)\\[/h4\\]", "<h4>$1</h4>");
	bbMap.put("\\[h5\\](.+?)\\[/h5\\]", "<h5>$1</h5>");
	bbMap.put("\\[h6\\](.+?)\\[/h6\\]", "<h6>$1</h6>");
	bbMap.put("\\[quote\\](.+?)\\[/quote\\]", "<blockquote>$1</blockquote>");
	bbMap.put("\\[p\\](.+?)\\[/p\\]", "<p>$1</p>");
	bbMap.put("\\[p=(.+?),(.+?)\\](.+?)\\[/p\\]", "<p style='text-indent:$1px;line-height:$2%;'>$3</p>");
	bbMap.put("\\[center\\](.+?)\\[/center\\]", "<div align='center'>$1");
	bbMap.put("\\[align=(.+?)\\](.+?)\\[/align\\]", "<div align='$1'>$2");
	bbMap.put("\\[color=(.+?)\\](.+?)\\[/color\\]", "<span style='color:$1;'>$2</span>");
	bbMap.put("\\[size=(.+?)\\](.+?)\\[/size\\]", "<span style='font-size:$1;'>$2</span>");
	bbMap.put("\\[img\\](.+?)\\[/img\\]", "<img src='$1' />");
	bbMap.put("\\[img=(.+?),(.+?)\\](.+?)\\[/img\\]", "<img width='$1' height='$2' src='$3' />");
	bbMap.put("\\[email\\](.+?)\\[/email\\]", "<a href='mailto:$1'>$1</a>");
	bbMap.put("\\[email=(.+?)\\](.+?)\\[/email\\]", "<a href='mailto:$1'>$2</a>");
	bbMap.put("\\[url\\](.+?)\\[/url\\]", "<a href='$1'>$1</a>");
	bbMap.put("\\[url=(.+?)\\](.+?)\\[/url\\]", "<a href='$1'>$2</a>");
	bbMap.put("\\[youtube\\](.+?)\\[/youtube\\]", "<object width='640' height='380'><param name='movie' value='http://www.youtube.com/v/$1'></param><embed src='http://www.youtube.com/v/$1' type='application/x-shockwave-flash' width='640' height='380'></embed></object>");
	bbMap.put("\\[video\\](.+?)\\[/video\\]", "<video src='$1' />");

	for(Map.Entry entry : bbMap.entrySet())
	{
	    html = html.replaceAll(entry.getKey().toString(), entry.getValue().toString());
	}

	return html;
    }

    public static void main(String[] args) throws SQLException
    {

	Learnweb lw = Learnweb.getInstance();
	JForumManager jfm = lw.getJForumManager();
	ForumManager fm = lw.getForumManager();
	GroupManager gm = lw.getGroupManager();
	UserManager um = lw.getUserManager();

	Statement stmt = lw.getConnection().createStatement();
	//	stmt.execute("TRUNCATE TABLE `forum_topic` ");
	//	stmt.execute("TRUNCATE TABLE `forum_post` ");

	/*
		PreparedStatement jforumGetTopics = jfm.getConnection().prepareStatement("select * from jforum_topics t join jforum_users using(user_id) join jforum_posts on topic_last_post_id = post_id WHERE t.`forum_id` = ?");
	
	List<Group> groups = gm.getGroups("SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE forum_id != ?", 0);
	for(Group group : groups)
	{
	    jforumGetTopics.setInt(1, group.getForumId());
	    ResultSet rs = jforumGetTopics.executeQuery();
	    while(rs.next())
	    {
		ForumTopic topic = new ForumTopic();
		topic.setId(rs.getInt("topic_id"));
		topic.setGroupId(group.getId());
		topic.setTitle(rs.getString("topic_title"));
		int user = um.getUserIdByUsername(rs.getString("username"));
		if(user < 0)
		    user = 0;
		topic.setUserId(user);
		topic.setDate(new Date(rs.getTimestamp("topic_time").getTime()));
		topic.setViews(rs.getInt("topic_views"));
		//topic.setReplies(rs.getInt("topic_replies"));
		//topic.setLastPostId(rs.getInt("topic_last_post_id"));
		//topic.setLastPostDate(new Date(rs.getTimestamp("post_time").getTime()));

		System.out.println(topic);
		fm.save(topic);
	    }

	    System.out.print(group.getTitle() + " - " + group.getForumId() + " - ");
	}	
	*/
	PreparedStatement jforumGetPosts = jfm.getConnection().prepareStatement("select * from jforum_posts join jforum_users using(user_id) join jforum_posts_text using(post_id) where topic_id = 329");

	ResultSet rs = jforumGetPosts.executeQuery();
	while(rs.next())
	{
	    ForumPost post = new ForumPost();
	    post.setId(rs.getInt("post_id"));
	    post.setDate(new Date(rs.getTimestamp("post_time").getTime()));

	    int user = um.getUserIdByUsername(rs.getString("username"));
	    if(user < 0)
		user = 0;
	    post.setUserId(user);

	    String text = bbcode(rs.getString("post_text"));

	    if(text.contains("http:"))
	    {
		post.setText(text);

		text = text.toLowerCase();
		if(text.contains("arschfick") || text.contains("fuck") || text.contains("buy") || text.contains("nudists") || text.contains("so hot") || text.contains("nude") || text.contains("lolita") || text.contains("models") || text.contains("naked") || text.contains("porn")
			|| text.contains("sex") || text.contains("sponsors") || text.contains("coupon") || text.contains("/user/view") || text.contains("cialis") || text.contains("prescription") || text.contains("pills") || text.contains("i'm busy at the moment")
			|| text.contains("wolkrmfbsoxfc") || text.contains("iopskmfbsoxfc") || text.contains("qyqosmfbsoxfc") || text.contains("hnuqjmfbsoxfc") || text.contains("hoes") || text.contains("funny pictures") || text.contains("paid")
			|| text.contains("i like watching")

		)
		{
		    System.out.println("skipped");
		    continue;
		}
	    }
	    post.setText(text);

	    System.out.println(post);
	    //fm.save(post);
	}

    }

    /**
     * Returns a list of all Groups a user belongs to
     * 
     * @throws SQLException
     */
    public List<Group> getGroupsByUserId(int userId) throws SQLException
    {
	String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_group_user u USING(group_id) WHERE u.user_id = ? ORDER BY title";
	return getGroups(query, userId);
    }

    public int getGroupCountByUserId(int userId) throws SQLException
    {
	String query = "SELECT COUNT(*) FROM `lw_group` g JOIN lw_group_user u USING(group_id) WHERE u.user_id = " + userId;
	return ((Long) Sql.getSingleResult(query)).intValue();
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
		GroupCategory category = getGroupCategoryById(group.getCategoryId());
		group.setCategoryAbbreviation(category.getAbbreviation());
		group.setCategoryTitle(category.getTitle());
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

    public List<GroupCategory> getGroupCategoriesByCourse(int courseId) throws SQLException
    {
	LinkedList<GroupCategory> categories = new LinkedList<GroupCategory>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT group_category_id, category_title, category_abbreviation FROM `lw_group_category` WHERE category_course_id = ? ORDER BY category_title");
	select.setInt(1, courseId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    categories.add(new GroupCategory(rs.getInt(1), courseId, rs.getString(2), rs.getString(3)));
	}

	select.close();
	return categories;
    }

    public GroupCategory getGroupCategoryById(int categoryId) throws SQLException
    {
	GroupCategory cat = null;

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT group_category_id, category_course_id, category_title, category_abbreviation FROM `lw_group_category` WHERE group_category_id = ?");
	select.setInt(1, categoryId);
	ResultSet rs = select.executeQuery();
	if(rs.next())
	    cat = new GroupCategory(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4));

	select.close();
	return cat;
    }

    protected GroupCategory save(GroupCategory category) throws SQLException
    {
	PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_group_category` (group_category_id, course_id, category_title, category_abbreviation) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

	if(category.getId() < 0) // the Group is not yet stored at the database 
	    replace.setNull(1, java.sql.Types.INTEGER);
	else
	    replace.setInt(1, category.getId());
	replace.setInt(1, category.getCourseId());
	replace.setString(2, category.getTitle());
	replace.setString(3, category.getAbbreviation());
	replace.executeUpdate();

	if(category.getId() < 0) // get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    category.setId(rs.getInt(1));

	    resetCache();
	}

	replace.close();

	return category;
    }
}

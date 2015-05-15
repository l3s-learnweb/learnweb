package de.l3s.learnweb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.util.MD5;
import de.l3s.util.StringHelper;

public class JForumManager
{
    private final String forumUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final String databaseUrl;

    protected JForumManager(Learnweb learnweb)
    {
	Properties properties = learnweb.getProperties();

	this.forumUrl = properties.getProperty("FORUM_URL");
	this.databaseUser = properties.getProperty("FORUM_MYSQL_USER");
	this.databasePassword = properties.getProperty("FORUM_MYSQL_PASSWORD");
	this.databaseUrl = properties.getProperty("FORUM_MYSQL_URL");
    }

    private Connection getConnection() throws SQLException
    {
	Connection dbConnection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);

	return dbConnection;
    }

    public ForumStatistic getForumStatistics(int forumId) throws SQLException
    {
	ForumStatistic result = null;

	if(forumId == 0)
	    return result;

	Connection connection = getConnection();
	PreparedStatement select = connection.prepareStatement("SELECT count(*) as posts, count(DISTINCT topic_id) AS topics   FROM `jforum_posts` WHERE `forum_id` = ?");
	select.setInt(1, forumId);

	ResultSet rs = select.executeQuery();

	if(rs.next())
	{
	    result = new ForumStatistic();
	    result.setPosts(rs.getInt("posts"));
	    result.setTopics(rs.getInt("topics"));
	}

	connection.close();

	return result;
    }

    /**
     * Creates a forum in the specified category and returns the id of the new forum
     * 
     * @param title
     * @param categoryId
     * @return
     * @throws KRSMException
     */
    public int createForum(String title, int categoryId)
    {
	if(null == title || title.length() == 0)
	    throw new IllegalArgumentException("invalid title, should not be empty");
	if(categoryId < 1)
	    throw new IllegalArgumentException("invalid categoryId: " + categoryId);

	Element res = sendGetRequest("createForum?name=" + StringHelper.urlEncode(title) + "&categoryId=" + categoryId);

	int forumId = Integer.parseInt(res.elementText("forumId"));

	if(!isForumExisting(forumId))
	    throw new RuntimeException("The forum couldn't be created");

	return forumId;
    }

    public boolean isForumExisting(int forumId)
    {
	try
	{
	    URL oracle = new URL(forumUrl + "forums/show/" + forumId + ".page");
	    URLConnection yc = oracle.openConnection();
	    BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
	    String inputLine;

	    while((inputLine = in.readLine()) != null)
	    {
		if(inputLine.contains("<td align=\"center\"><div class=\"gen\">Oooops.. You don't have sufficient privileges to access this forum</div></td>"))
		{
		    in.close();
		    return false;
		}
	    }
	    in.close();
	    return true;
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
	return false;
    }

    /**
     * Creates a category and returns his id
     * 
     * @param title
     * @return
     * @throws KRSMException
     */
    public int createCategory(String title)
    {
	if(null == title || title.length() == 0)
	    throw new IllegalArgumentException("invalid title");

	Element res = sendGetRequest("createCategory?name=" + StringHelper.urlEncode(title));

	return Integer.parseInt(res.elementText("categoryId"));
    }

    private Element sendGetRequest(String request)
    {
	try
	{
	    return new SAXReader().read(new URL(forumUrl + "services/" + request).openStream()).getRootElement();
	}
	catch(FileNotFoundException e)
	{ // nothing found
	    return null;
	}
	catch(Exception e)
	{
	    throw new RuntimeException(e);
	}
    }

    public String getForumUrl(User user, int forumId)
    {
	if(null == user || forumId < 1)
	    return null;

	return forumUrl + "api/go/1.page?forumId=" + forumId + "&username=" + StringHelper.urlEncode(user.getUsername()) + "&email=user" + user.getId() + "@learnweb.xyz" + "&password=" + MD5.hash(user.getId() + "supersicher") + "&language="
		+ UtilBean.getUserBean().getLocale().toString();
	//TODO besser mit user.getLocale().getLanguage()
    }

    public static void main(String[] args) throws SQLException
    {
	/*
	Learnweb lw = Learnweb.getInstance();
	ForumManager fm = lw.getForumManger();

	List<Group> groups = lw.getGroups(null);
	for(Group group : groups)
	{
		if(group.getForumId() == 0)
			continue;
		
		System.out.print(group.getTitle()+" - "+group.getForumId()+" - ");
		
		if(!fm.isForumExisting(group.getForumId()))
		{
			//group.setForumId(0);
			
			System.out.println("nein");
		}
		else
			System.out.println("ja");		
	}
	*/
	/*
	 *
	List<Course> courses = lw.getCourses();
	for(Course course : courses)
	{
		if(course.getId() == 1 || course.getId() == 2)
			continue;
		
		System.out.println(course);
		
		int categoryId = fm.createCategory("test-"+course.getTitle());
		int forumId = fm.createForum("test-"+course.getTitle(), categoryId);
		
		course.setForumCategoryId(categoryId);
		course.setForumId(forumId);
		//course.save();

		
	}*/
	//System.out.println(fm.createCategory("test"));

    }

    public class ForumStatistic
    {
	private int topics;
	private int posts;

	public int getTopics()
	{
	    return topics;
	}

	public void setTopics(int topics)
	{
	    this.topics = topics;
	}

	public int getPosts()
	{
	    return posts;
	}

	public void setPosts(int posts)
	{
	    this.posts = posts;
	}

    }

    /*
     * 
     * /JForum/services/createCategory?name=klassexy	return kategorieId
    /JForum/services/createForum?name=gruppen_bzw_klassenNAME&categoryId=111

    http://out.l3s.uni-hannover.de:9111/JForum/api/go/1.page?forumname=testASDF&username=usn&email=a@b.c&password=***REMOVED***

    :Parameters: >forumname, >username, >email, >password (MD5 Hash) This method redirects to the forum with given name, logged in as specified user. If either of those does not exist, it creates it. This can be used as the standard-Link from a LW-Group to its Forum.



    http://out.l3s.uni-hannover.de:9111/JForum/api/createForum/1.page?forumname=testForum123    ---->    forumId
    http://out.l3s.uni-hannover.de:9111/JForum/api/loginUserForForum/1.page?email=a@b.c&password=***REMOVED***&forumId=4     ---->  logged in as user and showing the specified forum


    http://out.l3s.uni-hannover.de:9111/JForum/api/createUser/1.page?username=usn&email=a@b.c&password=***REMOVED***
    http://out.l3s.uni-hannover.de:9111/JForum/api/changeUserUsername/1.page?email=a@b.c&username=usn
    http://out.l3s.uni-hannover.de:9111/JForum/api/changeUserPassword/1.page?email=a@b.c&password=***REMOVED***
    http://out.l3s.uni-hannover.de:9111/JForum/api/changeUserEmail/1.page?old=a@b.c&new=org@example.com


    Admin pwd: Gec9ADrAMucHetrususp
     */
}

package de.l3s.learnweb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.SolrClient;

public class LoroManager
{
    public static final String DB_CONNECTION = "jdbc:mysql://prometheus.kbs.uni-hannover.de:3306/learnweb_crawler?characterEncoding=utf8";
    public static final String DB_USER = "learnweb_crawler";
    public static final String DB_PASSWORD = "***REMOVED***";
    private long lastCheck = 0L;

    Connection DBConnection = null;
    private final static Logger log = Logger.getLogger(LoroManager.class);
    private final Learnweb learnweb;

    public void getConnection()
    {
	try
	{
	    Class.forName("com.mysql.jdbc.Driver");
	    java.util.Properties connProperties = new java.util.Properties();
	    connProperties.setProperty("user", DB_USER);
	    connProperties.setProperty("password", DB_PASSWORD);
	    DBConnection = DriverManager.getConnection(DB_CONNECTION, connProperties);

	    System.out.println("Connected to the database....");

	}
	catch(SQLException e)
	{
	    log.error("SQL Exception in getConnection ", e);
	}
	catch(ClassNotFoundException e)
	{
	    log.error("getConnection ", e);
	}
    }

    protected void checkConnection(Connection DBConnection) throws SQLException
    {
	// exit if last check was two or less seconds ago
	if(lastCheck > System.currentTimeMillis() - 2000)
	    return;

	if(!DBConnection.isValid(1))
	{
	    System.err.println("Database connection invalid try to reconnect");

	    try
	    {
		DBConnection.close();
	    }
	    catch(SQLException e)
	    {
		log.error("Error in closing connection", e);
	    }

	    getConnection();
	}

	lastCheck = System.currentTimeMillis();
    }

    public LoroManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;
    }

    //For saving Loro resources to LW table
    public void saveLoroResource() throws SQLException, IOException, SolrServerException
    {
	getConnection();
	ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
	SolrClient solr = learnweb.getSolrClient();
	Group loroGroup = learnweb.getGroupManager().getGroupById(883);
	User admin = learnweb.getUserManager().getUser(9139);
	PreparedStatement update = DBConnection.prepareStatement("UPDATE LORO_resource SET resource_id = ? WHERE loro_resource_id = ?");

	PreparedStatement getLoroResource = DBConnection
		.prepareStatement("SELECT t1.loro_resource_id , t1.resource_id , t1.description , t1.tags , t1.title , t1.creator_name , t1.course_code , t1.language_level , t1.languages , t1.preview_img_url,  t2.filename , t2.doc_format , t2.doc_url FROM LORO_resource t1 JOIN LORO_resource_docs t2 ON t1.loro_resource_id = t2.loro_resource_id WHERE t2.doc_format LIKE '%video%' ORDER BY t1.loro_resource_id LIMIT 1");
	getLoroResource.executeQuery();
	ResultSet rs = getLoroResource.getResultSet();
	ResultSetMetaData rsmd = rs.getMetaData();
	int columnsNumber = rsmd.getColumnCount();
	/*while(rs.next())
	{
	    for(int i = 1; i <= columnsNumber; i++)
	    {
		if(i > 1)
		    System.out.print(",  ");
		String columnValue = rs.getString(i);
		System.out.print("\n" + columnValue + rsmd.getColumnName(i));
	    }
	    System.out.println("");
	}*/
	while(rs.next())
	{
	    int learnwebResourceId = rs.getInt("resource_id");

	    Resource loroResource = createResource(rs, learnwebResourceId);
	    int loroId = Integer.parseInt(loroResource.getIdAtService());

	    loroResource.setOwner(admin);

	    if(learnwebResourceId == 0) // not yet stored in Learnweb

	    {
		rpm.processImage(loroResource, FileInspector.openStream(loroResource.getMaxImageUrl()));
		loroResource.save();
		update.setInt(1, loroResource.getId());
		update.setInt(2, loroId);
		update.executeUpdate();

		admin.addResource(loroResource);
		loroGroup.addResource(loroResource, admin);

		//solr.indexResource(loroResource);

	    }
	    else
		loroResource.save();

	    log.debug("Processed; lw: " + learnwebResourceId + " loro: " + loroId + " title:" + loroResource.getTitle());
	}

    }

    //Yet to be defined properly
    private Resource createResource(ResultSet rs, int learnwebResourceId) throws SQLException
    {

	Resource resource = new Resource();

	if(learnwebResourceId != 0) // the video is already stored and will be updated
	    resource = learnweb.getResourceManager().getResource(learnwebResourceId);

	resource.setTitle(rs.getString("title"));
	String description = "";
	if(rs.getString("description") != null)
	    description = rs.getString("description");
	if(rs.getString("language_level") != null)
	    description += "\nLanguage Level: " + rs.getString("language_level");
	if(rs.getString("languages") != null)
	    description += "\nLanguage: " + rs.getString("languages");
	if(rs.getString("course_code") != null)
	    description += "\nCourse Code: " + rs.getString("course_code");

	description += "\nThis file is a part of resource available on: http://loro.open.ac.uk/" + String.valueOf(rs.getInt("loro_resource_id")) + "/";
	resource.setDescription(description);
	resource.setUrl("http://loro.open.ac.uk/" + String.valueOf(rs.getInt("loro_resource_id")) + "/");
	resource.setSource("LORO");
	resource.setLocation("LORO");
	resource.setMaxImageUrl(rs.getString("preview_img_url"));
	if(rs.getString("doc_format").contains("image"))
	    resource.setType("image");
	else if(rs.getString("doc_format").contains("video") && !rs.getString("doc_format").contains("quicktime"))
	{
	    resource.setType("Video");
	    resource.setEmbeddedRaw("<h:outputStylesheet library=\"resources\" name=\"css/video-js.css\" /><h:outputScript library=\"resources\" name=\"js/video.js\" /><script>videojs.options.flash.swf = \"/resources/js/video-js.swf\";</script><video controls=\"controls\"><source src='"
		    + rs.getString("doc_url") + "' ' type='video/mp4'/></video>");
	}
	else
	    resource.setType("text");
	//resource.setDuration(rs.getInt("duration"));

	resource.setIdAtService(Integer.toString(rs.getInt("loro_resource_id")));

	return resource;

    }

    public static void main(String[] args) throws Exception
    {

	LoroManager lm = Learnweb.getInstance().getLoroManager();
	lm.saveLoroResource();
    }

}

package de.l3s.learnweb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import de.l3s.learnweb.resource.SERVICE;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class LoroManager
{
    // TODO: this settings should be moved to configuration
    public static final String DB_CONNECTION = "jdbc:mysql://prometheus.kbs.uni-hannover.de:3306/learnweb_crawler?characterEncoding=utf8";
    public static final String DB_USER = "learnweb_crawler";
    public static final String DB_PASSWORD = "***REMOVED***";
    public static PrintWriter writer = null;
    private long lastCheck = 0L;

    static Connection DBConnection = null;
    private final static Logger log = Logger.getLogger(LoroManager.class);
    private final Learnweb learnweb;

    public void getConnection()
    {
        try
        {
            //Class.forName("com.mysql.jdbc.Driver");
            Class.forName("org.mariadb.jdbc.Driver");

            java.util.Properties connProperties = new java.util.Properties();
            connProperties.setProperty("user", DB_USER);
            connProperties.setProperty("password", DB_PASSWORD);
            DBConnection = DriverManager.getConnection(DB_CONNECTION, connProperties);
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
            log.error("Database connection invalid try to reconnect");

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

    protected LoroManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    private void metaData(ResultSet rs, Resource resource) throws SQLException
    {

        String description = "";
        if(rs.getString("description") != null)
            description = rs.getString("description");
        if(rs.getString("language_level") != null)
            description += "\nLanguage Level: " + rs.getString("language_level");
        if(rs.getString("languages") != null)
        {
            description += "\nLanguage: " + rs.getString("languages");
            String language = rs.getString("languages");
            Locale[] allLocale = Locale.getAvailableLocales();
            for(Locale l : allLocale)
            {
                if(l.getDisplayName().toLowerCase().trim().contains(language.toLowerCase().trim()))
                {
                    if(l.getLanguage() != null)
                    {
                        resource.setLanguage(l.getLanguage().trim());
                    }
                    break;
                }
            }

        }
        if(rs.getString("tags") != null)
            description += "\nKeyWords: " + rs.getString("tags");

        description += "\nThis file is a part of resource collection available on LORO - http://loro.open.ac.uk/" + rs.getInt("loro_resource_id") + "/";

        resource.setDescription(description);
        resource.setUrl("http://loro.open.ac.uk/" + rs.getInt("loro_resource_id") + "/");
        resource.setSource(SERVICE.loro);
        resource.setLocation("LORO");

        resource.setCreationDate(rs.getTimestamp("added_on"));
        //set maxImageUrl for different types
        //Restricted
        if(rs.getString("preview_img_url").contains("RestrictedAccess"))
            resource.setRestricted(true);
        //For resources with preview images available
        else if(!rs.getString("preview_img_url").contains("No-Preview"))
        {
            if(rs.getString("doc_format").contains("image"))
                resource.setMaxImageUrl(rs.getString("doc_url"));
            else
                resource.setMaxImageUrl(rs.getString("preview_img_url"));
        }
        else if(rs.getString("doc_format").contains("image")) //For resources where no preview images are available, we can still set preview images for type image
            resource.setMaxImageUrl(rs.getString("doc_url"));

    }

    private boolean isLoroResourceDeleted(Resource loroResource, String doc_url)
    {
        try
        {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(doc_url).openConnection();
            con.setRequestMethod("HEAD");

            // make 5 attempts
            for(int i = 1; i < 6; i++)
            {
                if(con.getResponseCode() == 404)
                    return true;

                try
                {
                    Thread.sleep(10000 * (int) Math.pow(2, i));
                }
                catch(InterruptedException e)
                {
                    log.error("Failed due to some interrupt exception on the thread that fetches from the LORO", e);
                }
            }
        }
        catch(Exception e)
        {
            log.error("Failed because there was a problem in establishing connection.");
            log.error("unhandled error", e);
        }
        return false;
    }

    //For saving Loro resources to LW table
    public void saveLoroResource() throws SQLException, IOException, SolrServerException
    {

        ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
        Group loroGroup = learnweb.getGroupManager().getGroupById(883);
        ResourceManager resourceManager = learnweb.getResourceManager();

        try
        {
            writer = new PrintWriter("LoroErrorUrl.txt", "UTF-8");
        }
        catch(FileNotFoundException | UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        getConnection();

        User admin = learnweb.getUserManager().getUser(7727);

        PreparedStatement getLoroResource = DBConnection.prepareStatement(
                "SELECT t1.loro_resource_id , t2.resource_id , t1.description , t1.tags , t1.title , t1.creator_name , t1.course_code , t1.language_level , t1.languages , t1.flag , t1.preview_img_url, t1.added_on , t2.filename , t2.doc_format , t2.doc_url FROM LORO_resource t1 JOIN LORO_resource_docs t2 ON t1.loro_resource_id = t2.loro_resource_id WHERE t1.`loro_resource_id` = ?");
        PreparedStatement update = DBConnection.prepareStatement("UPDATE LORO_resource_docs SET resource_id = ? WHERE loro_resource_id = ? AND doc_url= ?");
        PreparedStatement getCount = DBConnection.prepareStatement("SELECT loro_resource_id, COUNT( * ) AS rowcount FROM  `LORO_resource_docs` group by `loro_resource_id`");
        getCount.executeQuery();
        ResultSet rs1 = getCount.getResultSet();

        while(rs1.next())
        {

            getLoroResource.setInt(1, rs1.getInt("loro_resource_id"));
            getLoroResource.executeQuery();
            ResultSet rs = getLoroResource.getResultSet();

            int resourceId = 0;
            //Variable to keep track for resourceId of a particular file belonging to type "text" and under same loro_resource_id group
            boolean textTest = true;

            while(rs.next())
            {

                int learnwebResourceId = rs.getInt("resource_id");

                String docFormat = rs.getString("doc_format");
                if(!docFormat.contains("video") && !docFormat.contains("image"))
                {
                    if(resourceId != 0)
                    {
                        learnwebResourceId = resourceId;
                        textTest = false;
                    }
                }

                Resource loroResource;
                if(docFormat.contains("video") || docFormat.contains("image") || textTest) //Creating resource
                    loroResource = createResource(rs, learnwebResourceId);
                else
                {
                    update.setInt(1, resourceId);
                    update.setInt(2, rs.getInt("loro_resource_id"));
                    update.setString(3, rs.getString("doc_url"));
                    update.executeUpdate();
                    continue; //Updates the resource_id in LORO table and continues
                }

                int loroId = Integer.parseInt(loroResource.getIdAtService());

                loroResource.setUser(admin);

                if(learnwebResourceId == 0) // not yet stored in Learnweb
                {
                    loroResource.setGroup(loroGroup);
                    loroResource = admin.addResource(loroResource);
                    if(!docFormat.contains("video") && !docFormat.contains("image"))
                    {
                        if(resourceId == 0)
                            resourceId = loroResource.getId();
                        update.setInt(1, resourceId);
                    }
                    else
                        update.setInt(1, loroResource.getId());
                    update.setInt(2, loroId);
                    update.setString(3, rs.getString("doc_url"));
                    update.executeUpdate();

                    //processVideo can not be used to fetch preview image URL of a video if the video has restricted access
                    if(rs.getString("doc_format").contains("video"))
                    {
                        if(!rs.getString("preview_img_url").contains("RestrictedAccess"))
                        {
                            try
                            {
                                rpm.processVideo(loroResource);
                            }
                            catch(Exception e)
                            {
                                writer.println(rs.getString("doc_url"));
                                resourceManager.deleteResource(loroResource.getId());
                            }
                        }
                    } //Preview images for video can be generated even when there is no preview image available
                      // else if((!rs.getString("preview_img_url").contains("No-Preview") && !rs.getString("preview_img_url").contains("RestrictedAccess")) || (rs.getString("doc_format").contains("image") && !rs.getString("preview_img_url").contains("RestrictedAccess")))

                    else if(!rs.getString("preview_img_url").contains("RestrictedAccess"))
                    {
                        checkConnection(DBConnection);
                        if(!rs.getString("preview_img_url").contains("No-Preview") || rs.getString("doc_format").contains("image"))
                        {
                            if((!rs.getString("preview_img_url").endsWith(".jpg") || !rs.getString("preview_img_url").endsWith(".png")) && !rs.getString("doc_format").contains("image"))
                            {
                                // TODO Dupe: same code found in the class
                                try
                                {
                                    rpm.processWebsite(loroResource); //For websites where preview image is not available
                                }
                                catch(Exception e)
                                {
                                    String loroUrl = "http://loro.open.ac.uk/" + rs.getInt("loro_resource_id") + "/";
                                    if(isLoroResourceDeleted(loroResource, loroUrl))
                                    {
                                        resourceManager.deleteResourcePermanent(loroResource.getId());
                                        PreparedStatement delete = DBConnection.prepareStatement("DELETE FROM `LORO_resource_docs` WHERE `resource_id` = ? and filename = ?");
                                        delete.setInt(1, loroResource.getId());
                                        delete.setString(2, rs.getString("filename"));
                                        delete.executeUpdate();
                                        delete.close();
                                    }
                                    else
                                    {
                                        writer.println(loroUrl);
                                        resourceManager.deleteResource(loroResource.getId());
                                    }
                                }
                            }
                            else
                            {
                                try
                                {
                                    rpm.processImage(loroResource, FileInspector.openStream(loroResource.getMaxImageUrl())); // For all other resources of type != video
                                }
                                catch(Exception e)
                                {
                                    if(isLoroResourceDeleted(loroResource, rs.getString("doc_url")))
                                    {
                                        // TODO Dupe: same code found in the class
                                        resourceManager.deleteResourcePermanent(loroResource.getId());
                                        PreparedStatement delete = DBConnection.prepareStatement("DELETE FROM `LORO_resource_docs` WHERE `resource_id` = ? and filename = ?");
                                        delete.setInt(1, loroResource.getId());
                                        delete.setString(2, rs.getString("filename"));
                                        delete.executeUpdate();
                                        delete.close();
                                    }
                                    else
                                    {
                                        writer.println(rs.getString("doc_url"));
                                        resourceManager.deleteResource(loroResource.getId());
                                    }
                                }
                            }
                        }
                        else
                        {
                            // TODO Dupe: same code found in the class
                            try
                            {
                                rpm.processWebsite(loroResource); //For websites where preview image is not available
                            }
                            catch(Exception e)
                            {
                                String loroUrl = "http://loro.open.ac.uk/" + rs.getInt("loro_resource_id") + "/";
                                if(isLoroResourceDeleted(loroResource, loroUrl))
                                {
                                    resourceManager.deleteResourcePermanent(loroResource.getId());
                                    PreparedStatement delete = DBConnection.prepareStatement("DELETE FROM `LORO_resource_docs` WHERE `resource_id` = ? and filename = ?");
                                    delete.setInt(1, loroResource.getId());
                                    delete.setString(2, rs.getString("filename"));
                                    delete.executeUpdate();
                                    delete.close();
                                }
                                else
                                {
                                    writer.println(loroUrl);
                                    resourceManager.deleteResource(loroResource.getId());
                                }
                            }
                        }
                    }
                    loroResource.save();

                    // textTest = true;
                }
                else
                    loroResource.save();

                log.debug("Processed; lw: " + learnwebResourceId + " loro: " + loroId + " title:" + loroResource.getTitle());
            }

        }

    }

    private Resource createResource(ResultSet rs, int learnwebResourceId) throws SQLException
    {

        Resource resource = new Resource();

        if(learnwebResourceId != 0) // the video is already stored and updated during LORO crawl
        {
            resource = learnweb.getResourceManager().getResource(learnwebResourceId);
            if(rs.getBoolean("flag"))
            {
                checkConnection(DBConnection);
                PreparedStatement setFlag = DBConnection.prepareStatement("UPDATE LORO_resource SET flag=0 WHERE loro_resource_id=" + rs.getInt("loro_resource_id"));
                setFlag.executeUpdate();
                resource.setDescription(null);
            }
        }
        metaData(rs, resource);
        if(rs.getString("doc_format").contains("image"))
        {
            resource.setType(Resource.ResourceType.image);
            String titleFilename = StringHelper.urlDecode(rs.getString("filename"));
            resource.setFileName(titleFilename);
            resource.setTitle(rs.getString("title") + " - " + titleFilename);
            resource.setIdAtService(Integer.toString(rs.getInt("loro_resource_id")));
            try
            {
                resource.setFileUrl(rs.getString("doc_url"));
            }
            catch(Exception e)
            {
                writer.println("There was a problem in setting FileUrl " + rs.getString("doc_url") + "Resource ID " + rs.getInt("loro_resource_id"));
            }
            return resource;
        }
        else if(rs.getString("doc_format").contains("video"))
        {
            // TODO: don't use scripts from CDN, they may collect some data and therefore we need to include them to EULA
            resource.setType(Resource.ResourceType.video);
            resource.setEmbeddedRaw(
                    "<link href=\"//vjs.zencdn.net/6.2.4/video-js.css\" rel=\"stylesheet\"/><script src=\"//vjs.zencdn.net/6.2.4/video.js\"></script><video id=\"MY_VIDEO_1\" class=\"video-js vjs-fill vjs-default-skin vjs-big-play-centered\" data-setup='{\"controls\": true, \"autoplay\": false, \"preload\": \"auto\"}' width=\"100%\" height=\"100%\"><source src=\""
                            + rs.getString("doc_url") + "\"> </video>");
            String titleFilename = StringHelper.urlDecode(rs.getString("filename"));
            resource.setFileName(titleFilename);
            resource.setTitle(rs.getString("title") + " - " + titleFilename);
            resource.setIdAtService(Integer.toString(rs.getInt("loro_resource_id")));
            try
            {
                resource.setFileUrl(rs.getString("doc_url"));
            }
            catch(Exception e)
            {
                writer.println("There was a problem in setting FileUrl " + rs.getString("doc_url") + "Resource ID " + rs.getInt("loro_resource_id"));
            }

            return resource;
        }
        //For text resources, we need same resource id for all docs

        else
        {

            resource.setType(Resource.ResourceType.text);
            resource.setTitle(rs.getString("title"));
            resource.setIdAtService(Integer.toString(rs.getInt("loro_resource_id")));

            return resource;
        }
    }

    public static void main(String[] args) throws Exception
    {

        LoroManager lm = Learnweb.getInstance().getLoroManager();
        lm.saveLoroResource();
        writer.close();

        DBConnection.close();

        System.exit(0);
    }
}
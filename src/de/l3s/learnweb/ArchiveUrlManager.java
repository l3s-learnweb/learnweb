package de.l3s.learnweb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.l3s.learnweb.resource.*;
import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.l3s.archiveSearch.CDXClient;
import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource.OnlineStatus;
import de.l3s.learnweb.user.User;

public class ArchiveUrlManager
{
    private final static Logger log = Logger.getLogger(ArchiveUrlManager.class);
    private static ArchiveUrlManager instance;
    private final Learnweb learnweb;

    private String archiveSaveURL;
    //private URL serviceUrlObj;
    //private static int collectionId;
    //private Queue<Resource> resources = new ConcurrentLinkedQueue<Resource>();
    //private Map<Integer, Date> trackResources = new ConcurrentHashMap<Integer, Date>();
    private ExecutorService executorService;
    private ExecutorService cdxExecutorService;

    private SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    // there should exist only one instance of this class because of the executor services
    protected static ArchiveUrlManager getInstance(Learnweb learnweb)
    {
        if(instance == null)
        {
            instance = new ArchiveUrlManager(learnweb);
        }
        return instance;
    }

    private ArchiveUrlManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        archiveSaveURL = learnweb.getProperties().getProperty("INTERNET_ARCHIVE_SAVE_URL");
        //collectionId = Integer.parseInt(learnweb.getProperties().getProperty("COLLECTION_ID"));
        /*try
        {
            serviceUrlObj = new URL(archiveSaveURL);
        }
        catch(MalformedURLException e)
        {
            log.error("The archive today service URL is malformed:", e);
        }*/

        executorService = Executors.newCachedThreadPool();//new ThreadPoolExecutor(maxThreads, maxThreads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(maxThreads * 1000, true), new ThreadPoolExecutor.CallerRunsPolicy());
        cdxExecutorService = Executors.newSingleThreadExecutor();//In order to sequentially poll the CDX server and not overload it
    }

    public void updateArchiveUrl(int fileId, int resourceId, String archiveUrl) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("UPDATE `lw_resource_archiveurl` SET `file_id` = ?  WHERE `resource_id`=? and `archive_url`=?");
        replace.setInt(1, fileId);
        replace.setInt(2, resourceId);
        replace.setString(3, archiveUrl);
        replace.executeUpdate();
        replace.close();

        log.info("Processed archiveUrl: " + archiveUrl);
    }

    class ArchiveNowWorker implements Callable<String>
    {
        Resource resource;

        public ArchiveNowWorker(Resource resource)
        {
            this.resource = resource;
        }

        @Override
        public String call() throws Exception
        {
            DateFormat responseDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

            if(resource == null)
                return "resource was NULL";

            //resource = learnweb.getResourceManager().getResource(resource.getId());

            if(resource.getArchiveUrls() != null)
            {
                int versions = resource.getArchiveUrls().size();
                if(versions > 0)
                {
                    long timeDifference = (new Date().getTime() - resource.getArchiveUrls().getLast().getTimestamp().getTime()) / 1000;
                    if(timeDifference < 300)
                        return "resource was last archived less than 5 minutes ago";
                }
            }

            String archiveURL = null, mementoDateString = null;
            try
            {
                Client client = Client.create();
                WebResource webResource = client.resource(archiveSaveURL + resource.getUrl());
                ClientResponse response = webResource.get(ClientResponse.class);

                if(response.getStatus() == HttpURLConnection.HTTP_OK)
                {
                    if(response.getHeaders().containsKey("Content-Location"))
                        archiveURL = "http://web.archive.org" + response.getHeaders().getFirst("Content-Location");
                    else
                        log.debug("Content Location not found");

                    if(response.getHeaders().containsKey("X-Archive-Orig-Date"))
                        mementoDateString = response.getHeaders().getFirst("X-Archive-Orig-Date");
                    else
                        log.debug("X-Archive-Orig-Date not found");

                    Date archiveUrlDate = null;
                    if(mementoDateString != null)
                        archiveUrlDate = responseDate.parse(mementoDateString);

                    log.debug("Archived URL:" + archiveURL + " Memento DateTime:" + mementoDateString);
                    PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("INSERT into lw_resource_archiveurl(`resource_id`,`archive_url`,`timestamp`) VALUES (?,?,?)");
                    prepStmt.setInt(1, resource.getId());
                    prepStmt.setString(2, archiveURL);
                    prepStmt.setTimestamp(3, new java.sql.Timestamp(archiveUrlDate.getTime()));
                    prepStmt.executeUpdate();
                    prepStmt.close();

                    resource.addArchiveUrl(null); // TODO
                }
                else if(response.getStatus() == HttpURLConnection.HTTP_FORBIDDEN)
                {
                    if(response.getHeaders().containsKey("X-Archive-Wayback-Liveweb-Error"))
                    {
                        if(response.getHeaders().getFirst("X-Archive-Wayback-Liveweb-Error").equalsIgnoreCase("RobotAccessControlException: Blocked By Robots"))
                            return "ROBOTS_ERROR";
                    }

                    log.error("Cannot archive URL because of an error other than robots.txt for resource: " + resource.getId() + "; Response: " + InterWeb.responseToString(response));
                    return "GENERIC_ERROR";
                }
            }
            catch(SQLException e)
            {
                log.error("Error while trying to save the archived URL for resource: " + resource.getId(), e);
                return "SQL_SAVE_ERROR";
            }
            catch(ParseException e)
            {
                log.error("Error while trying to parse the response date from archive URL service for resource: " + resource.getId() + "; Date trying to parse: " + mementoDateString, e);
                return "PARSE_DATE_ERROR";
            }

            return "ARCHIVE_SUCCESS";
        }

    }

    class CDXWorker implements Callable<String>
    {
        ResourceDecorator resource;

        public CDXWorker(ResourceDecorator resource)
        {
            this.resource = resource;
        }

        @Override
        public String call() throws NumberFormatException, SQLException
        {
            CDXClient cdxClient = new CDXClient();
            cdxClient.isArchived(resource);
            return null;
        }

    }

    public void checkWaybackCaptures(ResourceDecorator resource)
    {
        try
        {
            if(resource.getResource().getMetadataValue("first_timestamp") == null)
            {
                PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT first_capture, last_capture FROM wb_url WHERE url = ?");
                pStmt.setString(1, resource.getUrl());
                ResultSet rs = pStmt.executeQuery();
                if(rs.next())
                {
                    Timestamp first = rs.getTimestamp(1);
                    Timestamp last = rs.getTimestamp(2);
                    if(first != null && last != null) // url was checked and has captures
                    {
                        resource.getResource().setMetadataValue("first_timestamp", waybackDateFormat.format(new Date(first.getTime())));
                        resource.getResource().setMetadataValue("last_timestamp", waybackDateFormat.format(new Date(last.getTime())));
                    }

                }
                else
                    cdxExecutorService.submit(new CDXWorker(resource));
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching wayback url capture info: ", e);
        }
        catch(RejectedExecutionException e)
        {
            log.error("Checking if executor was shutdown: " + cdxExecutorService.isShutdown());
            log.error("Executor exception while submitting new wayback capture request", e);
        }
    }

    public String getFileUrl(int resourceId, String archiveUrl) throws SQLException
    {
        PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT `file_id`,`httpstatuscode` FROM `lw_resource_archiveurl` where `resource_id`=? and `archive_url`=?");
        ps.setInt(1, resourceId);
        ps.setString(2, archiveUrl);
        ResultSet rs = ps.executeQuery();
        if(rs.next())
        {
            int fileId = rs.getInt("file_id");
            if(rs.getInt("httpstatuscode") != 200 || fileId == 0)
                return null;
            else
                return learnweb.getFileManager().createUrl(fileId, "wayback_thumbnail.png");
        }
        return null;
    }

    public String addResourceToArchive(Resource resource)
    {
        String response = "";
        if(!(resource.getStorageType() == Resource.LEARNWEB_RESOURCE))
        {
            Future<String> executorResponse = executorService.submit(new ArchiveNowWorker(resource));

            try
            {
                response = executorResponse.get();
                //log.debug(response);
            }
            catch(InterruptedException e)
            {
                log.error("Execution of the thread was interrupted on a task for resource: " + resource.getId(), e);
            }
            catch(ExecutionException e)
            {
                log.error("Error while retrieving response from a task that was interrupted by an exception for resource: " + resource.getId(), e);
            }
            //resources.add(resource);
        }
        return response;
    }

    class ProcessWebsiteWorker implements Callable<String>
    {
        Resource archiveResource;
        ResourcePreviewMaker rpm;
        PrintWriter writer;

        public ProcessWebsiteWorker(Resource archiveResource, ResourcePreviewMaker rpm, PrintWriter writer)
        {
            this.archiveResource = archiveResource;
            this.rpm = rpm;
            this.writer = writer;
        }

        @Override
        public String call() throws Exception
        {
            try
            {
                rpm.processWebsite(archiveResource);
                archiveResource.setOnlineStatus(OnlineStatus.ONLINE);
            }
            catch(Exception e)
            {
                log.error(e);
                writer.println("prometheus url: " + archiveResource.getUrl());
                archiveResource.setOnlineStatus(OnlineStatus.OFFLINE); // offline
            }
            return "website thumbnails created";
        }
    }

    //For saving crawled Archive-It Urls to lw_resource_table as resource
    public void saveArchiveItResources() throws SQLException, IOException
    {
        ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
        User admin = learnweb.getUserManager().getUser(7727);

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("Process-Website.txt", true)));

        //int tagCount = 0;
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM archiveit_collection WHERE collection_id = ?");
        PreparedStatement pStmtCollections = learnweb.getConnection().prepareStatement("SELECT group_id,collection_id FROM `archiveit_subject` JOIN lw_group USING(group_id) WHERE collector LIKE 'Stanford University.%' AND deleted != 0");
        ResultSet rsCollections = pStmtCollections.executeQuery();
        while(rsCollections.next())
        {
            Group archiveGroup = learnweb.getGroupManager().getGroupById(rsCollections.getInt("group_id"));
            int collectionId = rsCollections.getInt("collection_id");
            pStmt.setInt(1, collectionId);
            ResultSet rs = pStmt.executeQuery();
            while(rs.next())
            {
                //String subject = rs.getString("subject");
                int resourceId = rs.getInt("resource_id");

                boolean toProcessUrl = false, redirect = false;
                int learnwebResourceId = rs.getInt("lw_resource_id");
                String archiveitUrl = rs.getString("url"); //To fetch Archive-It versions of a resource
                Resource archiveResource = createResource(learnwebResourceId, rs);

                if(learnwebResourceId == 0)
                {
                    try
                    {
                        URL url = new URL(archiveResource.getUrl());
                        URLConnection con = url.openConnection();
                        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
                        con.setRequestProperty("Accept-Language", "en-US");
                        con.setConnectTimeout(60000);
                        con.connect();
                        HttpURLConnection httpCon = (HttpURLConnection) con;
                        int status = httpCon.getResponseCode();

                        if(status != HttpURLConnection.HTTP_OK)
                        {
                            // Handling, 3xx which is redirect
                            if(status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)
                            {
                                redirect = true;
                                toProcessUrl = true;
                            }
                            else
                                log.info("Failed : HTTP error code : " + httpCon.getResponseCode() + " URL:" + con.getURL());

                        }
                        else
                            toProcessUrl = true;

                        if(redirect)
                        {
                            if(con.getHeaderField("Location") != null)
                                archiveResource.setUrl(con.getHeaderField("Location"));
                            else
                                toProcessUrl = false;
                        }
                        if(toProcessUrl)
                        {
                            log.info("Url Still Alive " + httpCon.getResponseCode() + " URL:" + archiveResource.getUrl());
                            Future<String> processResponse = executorService.submit(new ProcessWebsiteWorker(archiveResource, rpm, writer));
                            try
                            {
                                log.info(processResponse.get(2, TimeUnit.MINUTES));
                            }
                            catch(InterruptedException e)
                            {
                                log.error("Execution of the thread was interrupted", e);
                                writer.println("interruptexp url: " + archiveResource.getUrl());
                                archiveResource.setOnlineStatus(OnlineStatus.OFFLINE);
                            }
                            catch(ExecutionException e)
                            {
                                log.error("Error while retrieving response from a task that was interrupted by an exception", e);
                                writer.println("executionexp url: " + archiveResource.getUrl());
                                archiveResource.setOnlineStatus(OnlineStatus.OFFLINE);
                            }
                            catch(TimeoutException e)
                            {
                                log.info("Taking too long to create thumbnail for " + archiveResource.getUrl());
                                writer.println("timeout url: " + archiveResource.getUrl());
                                processResponse.cancel(true);
                                archiveResource.setOnlineStatus(OnlineStatus.OFFLINE);
                            }
                        }
                        else
                        {
                            archiveResource.setOnlineStatus(OnlineStatus.OFFLINE);
                        }
                    }
                    catch(IOException | IllegalArgumentException e)
                    {
                        log.error("Error while trying to connect to the URL", e);
                        archiveResource.setOnlineStatus(OnlineStatus.OFFLINE);
                    }

                    archiveResource.setGroup(archiveGroup);
                    archiveResource = admin.addResource(archiveResource);

                    PreparedStatement update = Learnweb.getInstance().getConnection().prepareStatement("UPDATE archiveit_collection SET lw_resource_id = ? WHERE collection_id = ? AND resource_id = ?");
                    update.setInt(1, archiveResource.getId());
                    update.setInt(2, collectionId);
                    update.setInt(3, resourceId);
                    update.executeUpdate();

                    /*try
                    {
                    solr.indexResource(archiveResource);
                    }
                    catch(IOException | SolrServerException e)
                    {
                    log.error("Error in indexing the Archive-It resource with lw_resource ID: " + archiveResource.getId(), e);
                    }*/

                    MementoClient mClient = learnweb.getMementoClient();
                    List<ArchiveUrl> archiveVersions = mClient.getArchiveItVersions(collectionId, archiveitUrl);
                    saveArchiveItVersions(archiveResource.getId(), archiveVersions);
                    log.debug(archiveResource.getUrl() + " " + archiveResource.getTitle() + " " + archiveResource.getOnlineStatus() + " " + archiveResource.getLanguage());

                    /*String[] tags = subject.split(",");
                    HashSet<String> lwTags = new HashSet<String>();
                    tagCount = 0;
                    for(String tag : tags)
                    {
                    tagCount++;
                    tag = tag.trim();
                    tag = tag.replace("and ", "");
                    lwTags.add(tag);
                    if(tagCount == 9)
                        break;
                    }
                    lwTags.add(humanRightsGroup.getTitle());
                    for(String tagName : lwTags)
                    {*/
                    String tagName = archiveGroup.getTitle();
                    try
                    {
                        addTagToResource(archiveResource, tagName, admin);
                    }
                    catch(Exception e)
                    {
                        log.error("Error in adding tags " + tagName, e);
                    }
                    //}
                }
                else
                    archiveResource.save();
                log.info("Processed; lw: " + archiveResource.getId() + " collection ID: " + collectionId + " resource ID: " + resourceId + " title:" + archiveResource.getTitle());
            }
            log.info("Processed; lw_group: " + archiveGroup.getId() + " collection ID: " + collectionId + " group title:" + archiveGroup.getTitle());
        }

        writer.close();
    }

    private Resource createResource(int learnwebResourceId, ResultSet rs) throws SQLException
    {
        Resource resource = new Resource();

        if(learnwebResourceId != 0) // the resource is already stored and will be updated
            resource = learnweb.getResourceManager().getResource(learnwebResourceId);

        resource.setTitle(rs.getString("title").trim());
        resource.setDescription(rs.getString("description").trim());
        resource.setUrl(rs.getString("url").trim());
        resource.setAuthor(rs.getString("creator").trim());

        String language = rs.getString("language");
        String[] languages;
        String lwLang = "";
        if(!language.isEmpty())
        {
            languages = language.split("[,;]+");
            lwLang = languages[0];
            if(lwLang.contains("-") || lwLang.contains("_"))
            {
                String[] lwLangSplit = lwLang.split("[-_]+");
                Locale l2 = new Locale(lwLangSplit[0], lwLangSplit[1]);
                lwLang = l2.getLanguage();
            }
            else if(lwLang.length() != 2)
            {
                Locale[] allLocale = Locale.getAvailableLocales();
                for(Locale l : allLocale)
                {
                    if(l.getDisplayName().toLowerCase().trim().contains(lwLang.toLowerCase().trim()))
                    {
                        lwLang = l.getLanguage().trim();
                        break;
                    }
                }
            }

            if(lwLang.length() != 2)
                lwLang = "";
        }

        resource.setLanguage(lwLang);
        resource.setSource(SERVICE.archiveit);
        resource.setType(Resource.ResourceType.text);
        resource.setMetadataValue("collector", rs.getString("collector").trim());
        resource.setMetadataValue("coverage", rs.getString("coverage").trim());
        resource.setMetadataValue("publisher", rs.getString("publisher").trim());
        return resource;
    }

    /**
     * copied method to avoid reindexing at SOLR
     *
     * @param resource
     * @param tagName
     * @param user
     * @throws Exception
     */
    private void addTagToResource(Resource resource, String tagName, User user) throws Exception
    {
        ResourceManager rsm = Learnweb.getInstance().getResourceManager();
        Tag tag = rsm.getTag(tagName);

        if(tag == null)
            tag = rsm.addTag(tagName);

        rsm.tagResource(resource, tag, user);
    }

    public void saveArchiveItVersions(int resourceId, List<ArchiveUrl> archiveVersions)
    {
        for(ArchiveUrl version : archiveVersions)
        {
            try
            {
                // TODO Dupe: same code exists in ResourceManager.saveArchiveUrlsByResourceId method
                PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("INSERT into lw_resource_archiveurl(`resource_id`,`archive_url`,`timestamp`) VALUES (?,?,?)");
                prepStmt.setInt(1, resourceId);
                prepStmt.setString(2, version.getArchiveUrl());
                prepStmt.setTimestamp(3, new java.sql.Timestamp(version.getTimestamp().getTime()));
                prepStmt.executeUpdate();
                prepStmt.close();
            }
            catch(SQLException e)
            {
                log.error("Error while trying to save the resource with the archived URL", e);
            }
            //log.debug(version.getArchiveUrl() + " " + gmtDate.format(version.getTimestamp()));
        }
    }

    public List<ArchiveUrl> getArchiveItVersions(int resourceId)
    {
        MementoClient mClient = learnweb.getMementoClient();
        List<ArchiveUrl> archiveVersions = null;
        try
        {
            PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("SELECT collection_id, url FROM archiveit_collection WHERE lw_resource_id = ?");
            prepStmt.setInt(1, resourceId);
            ResultSet rs = prepStmt.executeQuery();
            if(rs.next())
            {
                int collectionId = rs.getInt("collection_id");
                String archiveItURL = rs.getString("url");
                archiveVersions = mClient.getArchiveItVersions(collectionId, archiveItURL);
            }
            prepStmt.close();
        }
        catch(SQLException e)
        {
            log.error("Error while trying to save the resource with the archived URL", e);
        }
        return archiveVersions;
    }

    public void updateArchiveItVersions() throws SQLException
    {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT * FROM archiveit_collection WHERE collection_id = ?");
        PreparedStatement pStmtCollections = learnweb.getConnection().prepareStatement("SELECT t1.*, COUNT(*) FROM `archiveit_subject` t1 JOIN lw_group t2 USING(group_id) JOIN archiveit_collection t3 USING(collection_id) WHERE t2.deleted = 0 GROUP BY collection_id LIMIT 3,1");
        ResultSet rsCollections = pStmtCollections.executeQuery();
        while(rsCollections.next())
        {
            int collectionId = rsCollections.getInt("collection_id");
            pStmt.setInt(1, collectionId);
            ResultSet rs = pStmt.executeQuery();
            while(rs.next())
            {
                int learnwebResourceId = rs.getInt("lw_resource_id");
                List<ArchiveUrl> savedArchiveUrls = learnweb.getResourceManager().getArchiveUrlsByResourceId(learnwebResourceId);
                List<ArchiveUrl> archiveUrlsFromArchiveIt = getArchiveItVersions(learnwebResourceId);
                if(archiveUrlsFromArchiveIt != null)
                {
                    archiveUrlsFromArchiveIt.removeAll(savedArchiveUrls);
                    if(archiveUrlsFromArchiveIt.size() > 0)
                    {
                        log.debug("learnweb resource id:" + learnwebResourceId + " before:" + savedArchiveUrls.size());
                        saveArchiveItVersions(learnwebResourceId, archiveUrlsFromArchiveIt);
                    }
                }
            }
        }
    }

    public void createArchiveItGroups()
    {
        try
        {
            int courseId = 891;
            User admin = learnweb.getUserManager().getUser(7727);
            PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("SELECT * FROM  `archiveit_subject` WHERE  `collector` LIKE  'Stanford University.%' AND group_id = 0");
            PreparedStatement updateGroupId = learnweb.getConnection().prepareStatement("UPDATE archiveit_subject SET group_id = ? WHERE collection_id = ?");
            ResultSet rs = prepStmt.executeQuery();
            while(rs.next())
            {
                Group newGroup = new Group();
                newGroup.setTitle(rs.getString("collection_name"));
                String description = rs.getString("description").trim();
                if(!description.equalsIgnoreCase("no description."))
                    description += "<br/>URL: <a href=\"" + rs.getString("url").trim() + "\" target=\"_blank\">" + rs.getString("url") + "</a>";
                else
                    description = "URL: <a href=\"" + rs.getString("url").trim() + "\" target=\"_blank\">" + rs.getString("url") + "</a>";
                newGroup.setDescription(description);
                newGroup.setCourseId(courseId);
                newGroup.setLeader(admin);
                Group group = learnweb.getGroupManager().save(newGroup);
                updateGroupId.setInt(1, group.getId());
                updateGroupId.setInt(2, rs.getInt("collection_id"));
                updateGroupId.executeUpdate();
            }
            updateGroupId.close();
            prepStmt.close();
        }
        catch(SQLException e)
        {
            log.error("Error while trying to save the resource with the archived URL", e);
        }

    }

    /*public static String getMementoDatetime(String archiveURL)
    {
    Client client = Client.create();
    WebResource webResource = client.resource(archiveURL);
    ClientResponse response;
    for(int i = 1; i < 6;)
    {
        response = webResource.head();
        if(response.getHeaders().containsKey("Memento-Datetime"))
    	return response.getHeaders().getFirst("Memento-Datetime");
        else
    	log.debug("Memento Datetime not found");
        try
        {
    	Thread.sleep(1000 * (int) Math.pow(2, i));
        }
        catch(InterruptedException e)
        {
    	log.error("Failed due to some interrupt exception on the thread that fetches from archive.is", e);
        }
    }
    return null;
    }*/

    public static void main(String[] args) throws SQLException, ParseException, IOException
    {
        ArchiveUrlManager archiveUrlManager = Learnweb.getInstance().getArchiveUrlManager();
        archiveUrlManager.saveArchiveItResources();
        //archiveUrlManager.updateArchiveItVersions();
        //archiveUrlManager.createArchiveItGroups();
        /*MementoClient mClient = Learnweb.getInstance().getMementoClient();
        PreparedStatement pStmtCollections = Learnweb.getInstance().getConnection().prepareStatement("SELECT collection_id, group_id FROM archiveit_subject WHERE group_id in (1095,1096,1097,1098)");
        ResultSet rsCollections = pStmtCollections.executeQuery();
        while(rsCollections.next())
        {
            int collectionId = rsCollections.getInt("collection_id");
            Group archiveGroup = Learnweb.getInstance().getGroupManager().getGroupById(rsCollections.getInt("group_id"));
            for(Resource archiveResource : archiveGroup.getResources())
            {
        	List<ArchiveUrl> archiveVersions = mClient.getArchiveItVersions(collectionId, archiveResource.getUrl());
        	archiveUrlManager.saveArchiveItVersions(archiveResource.getId(), archiveVersions);
            }
        }*/

        /*ResourceManager rm = Learnweb.getInstance().getResourceManager();
        SolrClient solr = Learnweb.getInstance().getSolrClient();
        ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
        Group group = Learnweb.getInstance().getGroupManager().getGroupById(1125);
        for(Resource resource : group.getResources())
        {
            if(resource.getOnlineStatus() == OnlineStatus.OFFLINE)
            {
        	FileInfo info = null;
        	try
        	{
        	    info = new FileInspector().inspect(FileInspector.openStream(resource.getUrl()), "unknown");

        	    if(info != null)
        	    {
        		log.debug(info.getFileName() + " " + info.getMimeType());
        		if(info.getMimeType().equalsIgnoreCase("application/pdf"))
        		{
        		    resource.setMachineDescription(info.getTextContent());
        		    rpm.processFile(resource, FileInspector.openStream(resource.getUrl()), info);
        		    resource.save();
        		    solr.reIndexResource(resource);
        		}
        	    }
        	}
        	catch(IOException e)
        	{
        	    log.error("unhandled error", e);
        	}

            }
        }
        PreparedStatement select = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM lw_resource WHERE url LIKE '%facebook%' AND online_status = 'OFFLINE' AND deleted = 0 AND source= 'Archive-It'");
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            if(rs.getInt("resource_id") <= 0)
            {
        	log.info("Not in lw_resource collection ID:" + rs.getInt("collection_id") + " Resource ID:" + rs.getInt("resource_id"));
        	continue;
            }

            Resource resource = rm.getResource(rs.getInt("resource_id"));

            try
            {
        	HttpURLConnection con;
        	con = (HttpURLConnection) new URL(resource.getUrl()).openConnection();
        	con.setInstanceFollowRedirects(true);
        	con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:32.0) Gecko/20100101 Firefox/32.0");
        	log.debug(resource.getUrl());
        	for(int i = 0; i < 10; i++)
        	{
        	    if(con.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP || con.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER)
        	    {
        		log.debug(con.getResponseCode());
        		con = (HttpURLConnection) new URL(con.getHeaderField("Location")).openConnection();
        		con.setInstanceFollowRedirects(true);
        		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:32.0) Gecko/20100101 Firefox/32.0");

        		if(con.getResponseCode() == HttpURLConnection.HTTP_OK)
        		    break;
        	    }
        	}
        	String redirectUrl = con.getURL().toString();
        	int status = con.getResponseCode();
        	log.debug(resource.getId() + " " + status + " " + redirectUrl);
        	if(!redirectUrl.contains("login.php") && status == HttpURLConnection.HTTP_OK)
        	{
        	    String originalUrl = resource.getUrl();
        	    resource.setUrl(con.getURL().toString());
        	    rpm.processWebsite(resource);
        	    resource.setOnlineStatus(OnlineStatus.ONLINE);
        	    resource.setUrl(originalUrl);
        	    resource.save();
        	    solr.reIndexResource(resource);
        	    log.debug("processed:" + resource.getId());
        	}
            }
            catch(IOException e)
            {
        	log.error("Error in indexing the Archive-It resource with lw_resource ID: " + resource.getId(), e);
            }
        }*/

        /*PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM lw_resource WHERE title = '' AND online_status = 'ONLINE' ORDER BY resource_id DESC");
        PreparedStatement pStmt2 = Learnweb.getInstance().getConnection().prepareStatement("UPDATE lw_resource SET title=? WHERE resource_id=?");
        ResultSet rs = pStmt.executeQuery();
        while(rs.next())
        {
            try
            {
        	String url = rs.getString("url");

        	Document doc = Jsoup.connect(url).timeout(60000).userAgent("Mozilla").get();
        	log.debug(rs.getInt("resource_id") + " " + doc.title());
        	pStmt2.setString(1, doc.title());
        	pStmt2.setInt(2, rs.getInt("resource_id"));
        	pStmt2.executeUpdate();
            }
            catch(IOException e)
            {
        	log.error(e);
            }
        }*/
        /*Client client = Client.create();
        WebResource webResource = client.resource("http://suggestqueries.google.com/complete/search?output=firefox&hl=de&q=kr");
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("url", "http://docs.oracle.com/javase/7/docs/api/java/net/HttpURLConnection.html#setFollowRedirects(boolean)");
        ClientResponse response = webResource.get(ClientResponse.class);

        String refreshHeader = null;

        if(response.getHeaders().containsKey("Refresh"))
            refreshHeader = response.getHeaders().get("Refresh").get(0);
        Pattern p = Pattern.compile("https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher filenameParts = p.matcher(refreshHeader);
        String archiveURL = null;
        if(filenameParts.find())
            archiveURL = refreshHeader.substring(filenameParts.start(), filenameParts.end());
        log.debug(archiveURL);
        String mementoDatetime = getMementoDatetime(archiveURL);
        if(mementoDatetime != null)
            log.debug(mementoDatetime);*/
    }

    public void onDestroy()
    {
        executorService.shutdown();
        cdxExecutorService.shutdown();
        try
        {
            //Wait for a while for currently executing tasks to terminate
            if(!executorService.awaitTermination(1, TimeUnit.MINUTES))
                executorService.shutdownNow(); //cancelling currently executing tasks
        }
        catch(InterruptedException e)
        {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        try
        {
            //Wait for a while for currently executing tasks to terminate
            if(!cdxExecutorService.awaitTermination(1, TimeUnit.SECONDS))
                cdxExecutorService.shutdownNow(); //cancelling currently executing tasks
        }
        catch(InterruptedException e)
        {
            // (Re-)Cancel if current thread also interrupted
            cdxExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

    }

}

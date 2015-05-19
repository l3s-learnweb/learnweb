package de.l3s.learnweb;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.l3s.learnweb.Resource.OnlineStatus;

public class ArchiveUrlManager
{
    private final static Logger log = Logger.getLogger(ArchiveUrlManager.class);
    private final Learnweb learnweb;

    private String archiveTodayURL;
    private URL serviceUrlObj;
    private static int collectionId;
    private Queue<Resource> resources = new ConcurrentLinkedQueue<Resource>();
    private List<Integer> errorResponseCodes = new LinkedList<Integer>(Arrays.asList(403, 404, 503));
    private Map<Integer, Date> trackResources = new ConcurrentHashMap<Integer, Date>();
    private ThreadPoolExecutor executerService;

    protected ArchiveUrlManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;
	archiveTodayURL = learnweb.getProperties().getProperty("ARCHIVE_TODAY_URL");
	collectionId = Integer.parseInt(learnweb.getProperties().getProperty("COLLECTION_ID"));
	try
	{
	    serviceUrlObj = new URL(archiveTodayURL);
	}
	catch(MalformedURLException e)
	{
	    log.error("The archive today service URL is malformed:", e);
	}

	int maxThreads = 1;
	executerService = new ThreadPoolExecutor(maxThreads, maxThreads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(maxThreads * 1000, true), new ThreadPoolExecutor.CallerRunsPolicy());

    }

    class Worker implements Callable<Object>
    {

	@Override
	public Object call() throws Exception
	{
	    // TODO Auto-generated method stub
	    return null;
	}

    }

    public String addResourceToArchive(Resource resource)
    {
	// executerService.submit(new Worker(query, matchPattern, round));

	String response = "";
	if(!(resource.getStorageType() == Resource.FILE_RESOURCE))
	{
	    //if(trackResources.containsKey(resource.getId()))
	    //{
	    //long timeDifference = (new Date().getTime() - trackResources.get(resource.getId()).getTime()) / 1000;
	    //if(timeDifference > 300)
	    //{
	    resources.add(resource);
	    //  trackResources.put(resource.getId(), new Date());
	    //  response = "addedToArchiveQueue";
	    //}
	    //else
	    //  response = "archiveWaitMessage";
	    //}
	    /*else
	    {
	        resources.add(resource);
	    trackResources.put(resource.getId(), new Date());
	    response = "addedToArchiveQueue";
	    }*/
	}
	return response;
    }

    public void addArchiveUrlToResource() throws SQLException
    {
	String urlParameters;

	while(!resources.isEmpty())
	{
	    Resource resource = resources.poll();

	    DateFormat responseDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

	    if(resource == null)
		continue;

	    //    resource.setTitle("testsswssss");

	    resource = learnweb.getResourceManager().getResource(resource.getId());

	    System.out.println(resource.getTitle());

	    try
	    {
		if(resource.getArchiveUrls() != null)
		{
		    int versions = resource.getArchiveUrls().size();
		    if(versions > 0)
		    {
			long timeDifference = (new Date().getTime() - resource.getArchiveUrls().getLast().getTimestamp().getTime()) / 1000;
			if(timeDifference < 300)
			    continue;
		    }
		}
	    }
	    catch(SQLException e1)
	    {
		log.error("Error while retrieving archive urls for resource", e1);
	    }

	    try
	    {
		HttpsURLConnection con = (HttpsURLConnection) serviceUrlObj.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);

		urlParameters = "url=" + resource.getUrl();

		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		log.debug("Sending archive request for URL : " + resource.getUrl());
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while((inputLine = in.readLine()) != null)
		{
		    response.append(inputLine);
		}
		in.close();

		String resp = response.toString();

		Pattern p = Pattern.compile("https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		Matcher filenameParts = p.matcher(resp);
		String archiveURL = null;
		if(filenameParts.find())
		    archiveURL = resp.substring(filenameParts.start(), filenameParts.end());

		log.debug("Archived URL: " + archiveURL);
		String responseDateGMTString = con.getHeaderField("Date");
		Date archiveUrlDate = null;

		if(responseDateGMTString != null)
		    archiveUrlDate = responseDate.parse(responseDateGMTString);

		PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("INSERT into lw_resource_archiveurl(`resource_id`,`archive_url`,`timestamp`) VALUES (?,?,?)");
		prepStmt.setInt(1, resource.getId());
		prepStmt.setString(2, archiveURL);
		prepStmt.setTimestamp(3, new java.sql.Timestamp(archiveUrlDate.getTime()));
		prepStmt.executeUpdate();
		prepStmt.close();

		learnweb.getResourceManager().getResource(resource.getId()).addArchiveUrl(null);
		//resource.addArchiveUrl(null); // TODO 
	    }
	    catch(IOException e)
	    {
		log.error("HTTPs URL connection to the archive service causing error:", e);
	    }
	    catch(SQLException e)
	    {
		log.error("Error while trying to save the resource with the archived URL", e);
	    }
	    catch(ParseException e)
	    {
		log.error("Error while trying to parse the response date for archive URL service", e);
	    }
	}

	/*Iterator<Map.Entry<Integer, Date>> trackResourceIterator = trackResources.entrySet().iterator();
	while(trackResourceIterator.hasNext())
	{
	    Map.Entry<Integer, Date> entry = trackResourceIterator.next();
	    long timeDifference = (new Date().getTime() - entry.getValue().getTime()) / 1000;
	    if(timeDifference > 300)
	    {
		trackResourceIterator.remove();
	    }
	}*/
    }

    public boolean handleResponse(HttpURLConnection con) throws IOException
    {
	if(errorResponseCodes.contains(con.getResponseCode()))
	{
	    log.info("Failed : HTTP error code : " + con.getResponseCode() + " URL:" + con.getURL());
	    return false;
	}
	else
	{
	    log.info("Url Still Alive " + con.getResponseCode() + " URL:" + con.getURL());
	    return true;
	}
    }

    //For saving crawled Archive-It Urls to lw_resource_table as resource
    public void saveArchiveItResources() throws SQLException
    {
	ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();

	Group humanRightsGroup = learnweb.getGroupManager().getGroupById(917);
	User archiveDemo = learnweb.getUserManager().getUser(9182);

	try
	{
	    File collectionFile = new File("/home/fernando/1475.txt");
	    BufferedReader br = new BufferedReader(new FileReader(collectionFile));
	    //Client archiveItClient = Client.create();
	    //WebResource web;

	    String line;

	    while((line = br.readLine()) != null)
	    {
		String[] pageDetails = line.split("\t");
		String urlString = pageDetails[0];
		String title = null, description = null;
		boolean toProcessUrl = false;
		if(pageDetails.length == 3)
		{
		    title = pageDetails[1];
		    description = pageDetails[2];
		}
		else if(pageDetails.length == 2)
		    title = pageDetails[1];

		try
		{
		    URL url = new URL(urlString);
		    URLConnection con = url.openConnection();
		    con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
		    con.setConnectTimeout(1000);
		    con.connect();

		    if(con instanceof HttpsURLConnection)
		    {
			HttpsURLConnection httpsCon = (HttpsURLConnection) con;
			toProcessUrl = handleResponse(httpsCon);
		    }
		    else if(con instanceof HttpURLConnection)
		    {
			HttpURLConnection httpCon = (HttpURLConnection) con;
			toProcessUrl = handleResponse(httpCon);
		    }

		    if(toProcessUrl)
		    {
			if(title == null)
			{
			    BufferedReader brPage = new BufferedReader(new InputStreamReader(con.getInputStream()));
			    String inputLine, pageHtml = "";
			    while((inputLine = brPage.readLine()) != null)
			    {
				pageHtml += inputLine;
			    }
			    Document pageDoc = Jsoup.parse(pageHtml);
			    title = pageDoc.title();
			}
			Resource archiveResource = createResource(urlString, title, description);
			archiveResource.setOwner(archiveDemo);
			try
			{
			    rpm.processWebsite(archiveResource);
			}
			catch(Exception e)
			{
			    log.error(e);
			    archiveResource.setOnlineStatus(OnlineStatus.OFFLINE); // offline
			}
			archiveResource = archiveDemo.addResource(archiveResource);
			humanRightsGroup.addResource(archiveResource, archiveDemo);
			MementoClient mClient = learnweb.getMementoClient();
			List<ArchiveUrl> archiveVersions = mClient.getArchiveItVersions(collectionId, archiveResource.getUrl());
			saveArchiveItVersions(archiveResource.getId(), archiveVersions);

		    }
		}
		catch(UnknownHostException e)
		{
		    log.info("URL:" + line);
		    log.error("Error while trying to connect to the url", e);
		}
		catch(ConnectException e)
		{
		    log.info("URL:" + line);
		    log.error("Error while trying to connect to the url", e);
		}
		catch(SocketTimeoutException e)
		{
		    log.info("URL:" + line);
		    log.error("Error while trying to connect to the url", e);
		}
	    }
	}
	catch(IOException e)
	{
	    log.error("Error while trying to read the file", e);
	}

    }

    private Resource createResource(String url, String title, String description)
    {
	Resource resource = new Resource();

	if(title != null)
	    resource.setTitle(title);
	if(description != null)
	    resource.setDescription(description);
	resource.setUrl(url);
	resource.setSource("Internet");
	resource.setType("text");

	return resource;
    }

    public void saveArchiveItVersions(int resourceId, List<ArchiveUrl> archiveVersions)
    {
	DateFormat gmtDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	gmtDate.setTimeZone(TimeZone.getTimeZone("GMT"));

	for(ArchiveUrl version : archiveVersions)
	{
	    try
	    {
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
	    System.out.println(version.getArchiveUrl() + " " + gmtDate.format(version.getTimestamp()));
	}
    }

    public static void main(String[] args) throws SQLException, ParseException
    {
	ArchiveUrlManager archiveUrlManager = Learnweb.getInstance().getArchiveUrlManager();
	/*archiveUrlManager.saveArchiveItResources();*/

	//ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
	//List<Integer> resourceIds = new LinkedList<Integer>(Arrays.asList(110766, 110823, 110846, 110857, 110858, 110873));
	for(int id = 110855; id < 110861; id++)
	{
	    Resource res = Learnweb.getInstance().getResourceManager().getResource(id);
	    MementoClient mClient = Learnweb.getInstance().getMementoClient();
	    List<ArchiveUrl> archiveVersions = mClient.getArchiveItVersions(collectionId, res.getUrl());
	    archiveUrlManager.saveArchiveItVersions(res.getId(), archiveVersions);
	    /*try
	    {
	    Resource res = Learnweb.getInstance().getResourceManager().getResource(resourceId);
	    URL obj = new URL(res.getUrl());
	    HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
	    conn.setReadTimeout(5000);
	    conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
	    conn.addRequestProperty("User-Agent", "Mozilla");
	    conn.addRequestProperty("Referer", "google.com");

	    boolean redirect = false;

	    // normally, 3xx is redirect
	    int status = conn.getResponseCode();
	    if(status != HttpURLConnection.HTTP_OK)
	    {
	        if(status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)
	    	redirect = true;
	    }

	    System.out.println("Response Code ... " + status);

	    if(redirect)
	    {

	        // get redirect url from "location" header field
	        String newUrl = conn.getHeaderField("Location");
	        res.setUrl(newUrl);
	        rpm.processWebsite(res);
	        res.save();
	        System.out.println("Redirect to URL : " + newUrl);

	    }
	    }
	    catch(Exception e)
	    {
	    e.printStackTrace();
	    }*/
	}

    }

    public void onDestroy()
    {
	executerService.shutdown();
	try
	{
	    executerService.awaitTermination(1, TimeUnit.MINUTES);
	}
	catch(InterruptedException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace(); // TODO
	}

    }

}

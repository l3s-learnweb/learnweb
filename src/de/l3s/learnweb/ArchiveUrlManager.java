package de.l3s.learnweb;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

public class ArchiveUrlManager
{
    private final static Logger log = Logger.getLogger(ArchiveUrlManager.class);

    private static String archiveTodayURL;
    private URL serviceUrlObj;
    private Queue<Resource> resources = new ConcurrentLinkedQueue<Resource>();

    public ArchiveUrlManager(Learnweb learnweb)
    {
	archiveTodayURL = learnweb.getProperties().getProperty("ARCHIVE_TODAY_URL");
	try
	{
	    serviceUrlObj = new URL(archiveTodayURL);
	}
	catch(MalformedURLException e)
	{
	    log.error("The archive today service URL is malformed:", e);
	}
    }

    public void addResourceToArchive(Resource resource)
    {
	if(resource.getArchiveUrl() == null || resource.getArchiveUrl().length() == 0)
	    resources.add(resource);
    }

    public void addArchiveUrlToResource()
    {
	String urlParameters;

	while(!resources.isEmpty())
	{
	    Resource resource = resources.poll();

	    if(resource == null)
		continue;

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

		log.info("Sending archive request for URL : " + resource.getUrl());
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

		log.info("Archived URL: " + archiveURL);
		resource.setArchiveUrl(archiveURL);
		resource.save();
	    }
	    catch(IOException e)
	    {
		log.error("HTTPs URL connection to the archive service causing error:", e);
	    }
	    catch(SQLException e)
	    {
		log.error("Error while trying to save the resource with the archived URL", e);
	    }
	}
    }

}

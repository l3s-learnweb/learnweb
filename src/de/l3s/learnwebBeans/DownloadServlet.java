package de.l3s.learnwebBeans;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.l3s.learnweb.File;
import de.l3s.learnweb.FileManager;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.User;

/**
 * Servlet Class
 * 
 * @web.servlet name="downloadServlet" display-name="Simple DownloadServlet"
 *              description="Simple Servlet for Streaming Files to the Clients
 *              Browser"
 * @web.servlet-mapping url-pattern="/download"
 */
public class DownloadServlet extends HttpServlet
{
    private final static long serialVersionUID = 7083477094183456614L;
    private final static Logger log = Logger.getLogger(DownloadServlet.class);

    private final static int CACHE_DURATION_IN_SECOND = 60 * 60 * 24 * 365; // 1 year
    private final static long CACHE_DURATION_IN_MS = CACHE_DURATION_IN_SECOND * 1000L;

    private static final int BUFFER_SIZE = 16384;
    private final Learnweb learnweb;

    private final String urlPattern; // as defined in web.xml
    private final FileManager fileManager;

    public DownloadServlet()
    {
	this.learnweb = Learnweb.getInstance();
	this.urlPattern = learnweb.getProperties().getProperty("FILE_MANAGER_URL_PATTERN");
	this.fileManager = learnweb.getFileManager();
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
	super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
	// extract the file id from the request string
	String requestString = request.getRequestURI();
	int index = requestString.indexOf(urlPattern);
	if(index == -1)
	{
	    log.warn("Invalid download URL: " + requestString);
	    response.setStatus(404);
	    return;
	}
	requestString = requestString.substring(index + urlPattern.length());
	requestString = requestString.substring(0, requestString.indexOf("/"));
	int fileId = Integer.parseInt(requestString);

	try
	{
	    File file = fileManager.getFileById(fileId);
	    if(null == file)
	    {
		log.warn("Requested file " + fileId + " does not exist or was deleted");
		response.setStatus(404);
		return;
	    }

	    if(!file.exists()) // show error image
	    {
		response.setStatus(404);
	    }

	    long ifModifiedSince = request.getDateHeader("If-Modified-Since");

	    if(ifModifiedSince != -1)
	    {
		response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		response.setDateHeader("Expires", System.currentTimeMillis() + CACHE_DURATION_IN_MS);
		return;
	    }
	    /*
	        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
	            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
	            response.setHeader("ETag", eTag); // Required in 304.
	            response.setDateHeader("Expires", expires); // Postpone cache with 1 week.
	            return;
	        }
	    */
	    prepareResponseFor(response, file);
	    streamFileTo(response, file);

	    if(file.isDownloadLogActivated())
	    {
		HttpSession session = request.getSession(true);
		User user = null;
		Integer userId = (Integer) session.getAttribute("learnweb_user_id");

		if(userId != null)
		    user = learnweb.getUserManager().getUser(userId);

		if(null != user)
		    Learnweb.getInstance().log(user, Action.downloading, file.getResourceId(), null, session.getId(), 0);
	    }
	}
	catch(Exception e)
	{
	    log.error("Error while downloading file: " + fileId, e);
	    response.setStatus(500);
	}
    }

    private void streamFileTo(HttpServletResponse response, File file) throws IOException
    {
	OutputStream os = response.getOutputStream();
	InputStream fis = file.getInputStream();
	byte[] buffer = new byte[BUFFER_SIZE];
	int bytesRead = 0;
	while((bytesRead = fis.read(buffer)) > 0)
	{
	    os.write(buffer, 0, bytesRead);
	}
	os.flush();
	fis.close();
    }

    private void prepareResponseFor(HttpServletResponse response, File file)
    {
	response.setContentLength((int) file.getLength());
	response.setContentType(file.getMimeType());
	//response.setDateHeader("Expires", System.currentTimeMillis() + 15552000000L); // expires in 6 month
	//response.setHeader("Cache-Control", "max-age=2419200");

	long now = System.currentTimeMillis();
	response.addHeader("Cache-Control", "max-age=" + CACHE_DURATION_IN_SECOND);
	//response.addHeader("Cache-Control", "must-revalidate");//optional
	response.setDateHeader("Last-Modified", file.getLastModified().getTime());
	response.setDateHeader("Expires", now + CACHE_DURATION_IN_MS);

	if(file.getMimeType().contains("octet-stream"))
	    response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
    }
}

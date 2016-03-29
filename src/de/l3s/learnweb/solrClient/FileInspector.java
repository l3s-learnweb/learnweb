package de.l3s.learnweb.solrClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.util.MimeTypes;
import de.l3s.util.StringHelper;

public class FileInspector
{
    private final static Logger log = Logger.getLogger(FileInspector.class);
    private SolrClient solrClient;

    public class FileInfo
    {
	private String mimeType;
	private String title;
	private String textContent;
	private String fileName;
	private String author;

	public String getFileName()
	{
	    return fileName;
	}

	public String getMimeType()
	{
	    return mimeType;
	}

	public String getTitle()
	{
	    return title == null ? fileName : title;
	}

	public String getTextContent()
	{
	    return textContent;
	}

	public String getAuthor()
	{
	    return author;
	}

	@Override
	public String toString()
	{
	    return "FileInfo [mimeType=" + mimeType + ", title=" + title + ", fileName=" + fileName + ", author=" + author + ", textContent:\n" + StringHelper.shortnString(textContent, 80) + "]";
	}

    }

    public FileInspector()
    {
	Learnweb learnweb = Learnweb.getInstance();
	solrClient = learnweb.getSolrClient();
    }

    @SuppressWarnings("unchecked")
    public FileInfo inspect(InputStream inputStream, String fileName)
    {
	FileInfo info = new FileInfo();
	info.fileName = fileName;

	int i = fileName.lastIndexOf('.');
	if(i > 0)
	{
	    String extension = fileName.substring(i + 1);
	    info.mimeType = MimeTypes.getMimeType(extension);
	    info.title = fileName.substring(0, i);
	}
	else
	{
	    info.mimeType = "application/octet-stream";
	    info.title = fileName;
	}

	if(info.mimeType.startsWith("video/")) // solr/tika doesn't work good with videos
	    return info;

	SolrServer server = solrClient.getSolrServer();

	ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
	up.addContentStream(new MyContentStream(info.fileName, info.mimeType, inputStream));
	up.setParam(ExtractingParams.EXTRACT_ONLY, "true");
	up.setParam(ExtractingParams.EXTRACT_FORMAT, "text"); // default : xml(with xml tags)

	NamedList<Object> result;
	try
	{
	    result = server.request(up);
	}
	catch(Exception e)
	{
	    log.error("FileInspector: Can't extract Text from File", e);

	    return info;
	}

	NamedList<Object> metadata = (NamedList<Object>) result.get("null_metadata");

	if(metadata.indexOf("title", 0) > -1)
	{
	    List<String> titles = (List<String>) metadata.get("title");
	    if(titles.size() > 0 && titles.get(0).length() > 0)
		info.title = titles.get(0);
	}
	if(metadata.indexOf("Author", 0) > -1)
	{
	    List<String> authors = (List<String>) metadata.get("Author", 0);
	    info.author = authors.get(0);
	}
	if(metadata.indexOf("Content-Type", 0) > -1)
	{
	    List<String> types = (List<String>) metadata.get("Content-Type");
	    if(types.size() > 0 && types.get(0).length() > 0)
		info.mimeType = types.get(0).split(";")[0];
	}
	info.textContent = result.getVal(1).toString().trim().replaceAll("(?m)^[ \t]*\r?\n", "");

	return info;
    }

    /**
     * This function checks if a given String is a valid url.
     * When the url leads to a redirect the function will return the target of the redirect.
     * Returns null if the url is invalid or not reachable.
     * 
     * @param url
     * @return
     */
    public static String checkUrl(String url)
    {

	if(url == null)
	    return null;
	if(!url.startsWith("http"))
	    url = "http://" + url;
	HttpURLConnection con;
	try
	{
	    con = (HttpURLConnection) new URL(url).openConnection();
	    con.setInstanceFollowRedirects(false);
	    con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:32.0) Gecko/20100101 Firefox/32.0");

	    int responseCode = con.getResponseCode();
	    if(responseCode / 100 == 2)
		return url;
	    else if(responseCode / 100 == 3)
	    {
		String location = con.getHeaderField("Location");
		if(location.startsWith("/"))
		{
		    String domain = url.substring(0, url.indexOf("/", url.indexOf("//") + 2));
		    return domain + location;
		}
		else
		    return location;
	    }
	    else
		return null;
	}
	catch(UnknownHostException e)
	{
	    log.info(e.getMessage());
	    return null;
	}
	catch(Throwable t)
	{
	    log.error("invalid url", t);
	    return null;
	}
    }

    public static InputStream openStream(String url) throws IOException
    {
	HttpURLConnection con;

	con = (HttpURLConnection) new URL(url).openConnection();
	con.setInstanceFollowRedirects(true);
	con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:32.0) Gecko/20100101 Firefox/32.0");

	return con.getInputStream();
    }

    private static void getBestImage(Resource resource) throws SQLException
    {
	String url = resource.getUrl();
	String imageUrl = null;
	Pattern pattern = Pattern.compile("<.+src=\"([^\"<>]+)\".*/>");
	if(null != resource.getEmbeddedSize1())
	{
	    Matcher matcher = pattern.matcher(resource.getEmbeddedSize1());
	    if(matcher.matches())
		imageUrl = matcher.group(1);
	}

	if(resource.getSource().equalsIgnoreCase("YouTube"))
	{
	    url = checkUrl(url);
	    if(url != null)
	    {
		Pattern pattern1 = Pattern.compile("v[/=]([^&]+)");
		Matcher matcher1 = pattern1.matcher(url);
		if(matcher1.find())
		{
		    String videoId = matcher1.group(1);
		    imageUrl = "http://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
		}
	    }
	}
	else if(resource.getSource().equalsIgnoreCase("Google") && resource.getType().equalsIgnoreCase("Video"))
	{
	    if(url != null)
	    {
		Pattern pattern1 = Pattern.compile("youtube.com/watch%3Fv%3D([^&]+)");
		Matcher matcher1 = pattern1.matcher(url);
		if(matcher1.find())
		{
		    String videoId = matcher1.group(1);
		    imageUrl = "http://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
		}
	    }
	}
	else if(resource.getSource().equalsIgnoreCase("Vimeo"))
	{
	    if(imageUrl != null)
	    {
		Pattern pattern1 = Pattern.compile("([^_]+)_([0-9]+)\\.[a-zA-Z]+");
		Matcher matcher1 = pattern1.matcher(imageUrl);
		if(matcher1.matches())
		    imageUrl = getBetterSize(imageUrl, imageUrl.lastIndexOf('_') + 1, imageUrl.lastIndexOf('.'));
	    }
	}
	else if(resource.getSource().equalsIgnoreCase("Ipernity"))
	{
	    if(imageUrl != null)
	    {
		Pattern pattern1 = Pattern.compile(".+\\.[0-9]+\\.[a-zA-Z]+");
		Matcher matcher1 = pattern1.matcher(imageUrl);
		if(matcher1.matches())
		    imageUrl = getBetterSize(imageUrl, imageUrl.lastIndexOf('.', imageUrl.lastIndexOf('.') - 1) + 1, imageUrl.lastIndexOf('.'));
	    }
	}
	else if(resource.getSource().equalsIgnoreCase("Flickr"))
	{
	    imageUrl = resource.getMaxImageUrl();
	    if(imageUrl != null)
	    {
		Pattern pattern1 = Pattern.compile("http://.+\\.[a-zA-Z]+");
		Matcher matcher1 = pattern1.matcher(imageUrl);
		if(matcher1.matches())
		{
		    char size[] = { 'o', 'b', 'c', 'z', 'n', 'm', 't', 'q', 's' };
		    int end = imageUrl.lastIndexOf('.');
		    char original = 'm';
		    if(imageUrl.charAt(end - 2) == '_')
			original = imageUrl.charAt(end - 1);
		    int i = 0;
		    String newUrl = null;
		    while(i < size.length && size[i] != original)
		    {
			if(size[i] == 'm')
			{
			    newUrl = imageUrl.substring(0, end - 2) + imageUrl.substring(end, imageUrl.length());
			    newUrl = checkUrl(newUrl);
			    if(newUrl != null && !newUrl.contains("unavailable"))
			    {
				imageUrl = newUrl;
				break;
			    }
			}
			else
			{
			    if(original == 'm')
				newUrl = imageUrl.substring(0, end) + "_" + size[i] + imageUrl.substring(end, imageUrl.length());
			    else
				newUrl = imageUrl.substring(0, end - 2) + "_" + size[i] + imageUrl.substring(end, imageUrl.length());

			    newUrl = checkUrl(newUrl);
			    if(newUrl != null && !newUrl.contains("unavailable"))
			    {
				imageUrl = newUrl;
				break;
			    }
			}
			i++;
		    }

		    if(size[i] == 'z')
			imageUrl += "?zz=1";
		}
	    }
	}
	else if(resource.getSource().equalsIgnoreCase("Desktop"))
	{
	    if(null != resource.getEmbeddedSize4())
	    {
		Matcher matcher = pattern.matcher(resource.getEmbeddedSize4());
		if(matcher.matches())
		    imageUrl = matcher.group(1);
	    }
	    else if(null != resource.getEmbeddedSize3())
	    {
		Matcher matcher = pattern.matcher(resource.getEmbeddedSize3());
		if(matcher.matches())
		    imageUrl = matcher.group(1);
	    }
	}
	else
	{
	    // that's ok. This seem to be mostly web sites
	    log.debug("unhandled resource " + resource);
	    return;

	}

	if(imageUrl == null) // why can't we get an images for this resource...
	{
	    log.error("can't get image for " + resource);
	}

	if(url != null && url.startsWith("http://immediatenet.com"))
	    return;

	log.debug(imageUrl);

	if(imageUrl == null || checkUrl(imageUrl) == null)
	{
	    resource.setMaxImageUrl("-1");
	}
	else
	    resource.setMaxImageUrl(imageUrl);

	resource.save();

    }

    private static String getBetterSize(String imageUrl, int start, int end)
    {
	int size[] = { 1024, 800, 640, 500, 320, 240, 150, 100, 75 };
	int originalSize = Integer.parseInt(imageUrl.substring(start, end));
	int i = 0;
	String newUrl = null;
	while(i < size.length && size[i] > originalSize)
	{
	    newUrl = imageUrl.substring(0, start) + size[i] + imageUrl.substring(end, imageUrl.length());
	    if(checkUrl(newUrl) != null)
		return newUrl;
	    i++;
	}
	return imageUrl;
    }

    // tests
    public static void main(String[] args) throws Exception
    {
	log.debug(checkUrl("www.storyofstuff.org"));
	/*
	Learnweb learnweb = Learnweb.getInstance();
	List<Resource> resources = learnweb.getResourceManager().getResources("SELECT * FROM `lw_resource` WHERE `source` LIKE 'flickr' AND `max_image_url` LIKE '%z.jpg' ORDER BY `resource_id` ASC ", null); // loads all resources (very slow)
	
	for(Resource resource : resources)
	{
	    getBestImage(resource);
	
	}
	*/
    }

    private class MyContentStream implements ContentStream
    {
	private String fileName;
	private String mimeType;
	private InputStream inputStream;

	public MyContentStream(String fileName, String mimeType, InputStream inputStream)
	{
	    super();
	    this.fileName = fileName;
	    this.mimeType = mimeType;
	    this.inputStream = inputStream;
	}

	@Override
	public String getContentType()
	{
	    return mimeType;
	}

	@Override
	public String getName()
	{
	    return fileName;
	}

	@Override
	public Reader getReader() throws IOException
	{
	    return new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
	}

	@Override
	public Long getSize()
	{
	    return null;
	}

	@Override
	public String getSourceInfo()
	{
	    // I don't know what they expect it seems to be unnecessary
	    return null;
	}

	@Override
	public InputStream getStream() throws IOException
	{
	    return inputStream;
	}

    }
}

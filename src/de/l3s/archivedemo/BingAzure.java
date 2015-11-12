package de.l3s.archivedemo;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;

public class BingAzure
{
    final static Logger log = Logger.getLogger(BingAzure.class);
    private boolean BING_DisableQueryAlterations;
    //  // kemkes@kbs.uni Ego #
    private static AuthCredentials authCredentials_web = new AuthCredentials("***REMOVED***", "***REMOVED***"); // kemkes@kbs.uni
    //private static AuthCredentials authCredentials_web = new AuthCredentials("***REMOVED***", "***REMOVED***"); // philipp@kemkes.net
    private static AuthCredentials authCredentials = authCredentials_web; //
    //private static AuthCredentials authCredentials = new AuthCredentials("ccaaeac1-04d8-4761-8a2c-5d2dfc5f2133", "KE+X3nVEVxvvxM/fZE+1FR4rpl27/nmzQB6VVqGB/2I="); // interweb
    private static int MAX_PER_REQUEST = 50;

    private String type;

    private int loadedResourceDecorators;
    private int page;
    private List<ResourceDecorator> results;

    private int estimatedResourceDecoratorCount;
    private int webTotal = -1;
    private int imageTotal = -1;
    private int top;

    private Query query;
    private ArchiveSearchManager archiveSearchManager;

    public BingAzure()
    {
	super();
	archiveSearchManager = Learnweb.getInstance().getArchiveSearchManager();
    }

    public static void main(String[] args) throws DocumentException, IOException, SQLException
    {
	Query q = new Query();
	q.setQueryString("Barabbas der zweite");
	q.setRequestedResultCount(50);
	q.setMarket("de-DE");

	BingAzure bing = new BingAzure();
	bing.search(q, "web");
    }

    public List<ResourceDecorator> search(Query query, String type) throws DocumentException, IOException, SQLException
    {
	if(null == query.getQueryString())
	    throw new IllegalArgumentException("queryString is null");

	//archiveSearchManager.saveQuery(query);
	this.query = query;
	this.type = type;
	this.page = 0;
	this.loadedResourceDecorators = 0;
	this.results = new LinkedList<ResourceDecorator>();

	//	log.info("Search for: " + queryString);

	boolean hasMoreResourceDecorators = doNormalSearch();

	while(hasMoreResourceDecorators && query.getRequestedResultCount() > this.loadedResourceDecorators)
	{
	    hasMoreResourceDecorators = doNormalSearch();
	}

	estimatedResourceDecoratorCount = -1;

	if(type.equalsIgnoreCase("web"))
	    estimatedResourceDecoratorCount = webTotal;
	else if(type.equalsIgnoreCase("image"))
	    estimatedResourceDecoratorCount = imageTotal;
	else
	    throw new IllegalArgumentException("illegal type: " + type);

	query.setEstimatedResultCount(estimatedResourceDecoratorCount);
	query.setLoadedResultCount(loadedResourceDecorators);
	query.setResults(results);

	//archiveSearchManager.saveQuery(query);

	return results;
    }

    public int getEstimatedResourceDecoratorCount()
    {
	return estimatedResourceDecoratorCount;
    }

    private WebResource createParameters(WebResource ResourceDecorator)
    {
	if(type.equals("web"))
	{
	    //ResourceDecorator = ResourceDecorator.queryParam("Options", "'EnableHighlighting+DisableLocationDetection'");
	    if(BING_DisableQueryAlterations)
		ResourceDecorator = ResourceDecorator.queryParam("WebSearchOptions", "'DisableQueryAlterations'");
	}
	else
	    throw new RuntimeException("image search not implemented");

	top = (query.getRequestedResultCount() - loadedResourceDecorators > MAX_PER_REQUEST) ? MAX_PER_REQUEST : query.getRequestedResultCount() - loadedResourceDecorators;
	//requestedResourceDecorators -= top;

	ResourceDecorator = ResourceDecorator.queryParam("Query", "'" + query.getQueryString() + "'");
	ResourceDecorator = ResourceDecorator.queryParam("$top", Integer.toString(top));
	ResourceDecorator = ResourceDecorator.queryParam("$skip", Integer.toString(page * MAX_PER_REQUEST));
	ResourceDecorator = ResourceDecorator.queryParam("Adult", "'Off'");
	ResourceDecorator = ResourceDecorator.queryParam("Options", "'EnableHighlighting'");

	if(query.getMarket() != null)
	    ResourceDecorator = ResourceDecorator.queryParam("Market", "'" + query.getMarket() + "'");

	//log.debug("page " + page + " - " + Integer.toString(top) + " - " + Integer.toString(page * MAX_PER_REQUEST));

	page++;

	return ResourceDecorator;
    }

    private static String convertHighlighting(String string)
    {
	if(string == null)
	    return "";

	string = string.replaceAll("[^\\u0000-\\uFFFF]", "\uFFFD"); // remove invalid 4byte utf8 chars

	return string.replace("<", "&lt;").replace(">", "&gt;").replace("" + (char) 57344, "<b>").replace("" + (char) 57345, "</b>");
    }

    @SuppressWarnings("unused")
    private boolean doCompositeSearch() throws SQLException
    {
	WebResource ResourceDecorator = createWebResource("https://api.datamarket.azure.com/Data.ashx/Bing/Search/v1/Composite", authCredentials);
	ResourceDecorator = ResourceDecorator.queryParam("Sources", "'" + type + "'");
	ResourceDecorator = createParameters(ResourceDecorator);

	ClientResponse response = null;

	for(int retry = 0; retry < 20; retry++)
	{
	    try
	    {
		response = ResourceDecorator.get(ClientResponse.class);
		break;
	    }
	    catch(Exception e)
	    {
		log.fatal("Fatal Bing request; try number: " + retry, e);
		try
		{
		    Thread.sleep(2000);
		}
		catch(InterruptedException e1)
		{
		}
	    }

	    if(response != null && response.getStatus() == 503)
	    {
		log.fatal("no transactions left; try number: " + retry);
		try
		{
		    Thread.sleep(600000);
		}
		catch(InterruptedException e1)
		{
		}
	    }
	}

	if(response == null)
	    throw new RuntimeException("Fatal Bing error");

	if(response.getStatus() == 503)
	    throw new RuntimeException("No transactions left for Bing Web Search");

	SAXReader reader = new SAXReader();
	Document document;

	try
	{
	    document = reader.read(response.getEntityInputStream());

	}
	catch(DocumentException e1)
	{
	    log.fatal("Can't get ResourceDecorator from Bing Composite Search");

	    try
	    {
		log.fatal(IOUtils.toString(response.getEntityInputStream()));
	    }
	    catch(IOException e)
	    {
	    }

	    throw new RuntimeException(e1);
	}
	Element root = document.getRootElement().element("entry");

	if(null == root)
	{
	    log.error("Invalid ResourceDecorator: \n" + document.asXML());
	}

	Element content = root.element("content");

	if(null == content)
	{
	    log.error("Invalid ResourceDecorator: \n" + document.asXML());
	}
	Element properties = content.element("properties");

	try
	{
	    webTotal = Integer.parseInt(properties.elementText("WebTotal"));
	    imageTotal = Integer.parseInt(properties.elementText("ImageTotal"));
	}
	catch(NumberFormatException e)
	{
	}

	List<Element> links = root.elements("link");

	for(Element link : links)
	{
	    if(link.attributeValue("title").equalsIgnoreCase(type))
	    {
		return parse(link.element("inline").element("feed"));
	    }
	}

	throw new RuntimeException("haven't found ResourceDecorators");

    }

    private static WebResource createWebResource(String apiUrl, AuthCredentials consumerAuthCredentials)
    {
	Client client = Client.create();
	client.addFilter(new HTTPBasicAuthFilter(consumerAuthCredentials.getKey(), consumerAuthCredentials.getSecret()));
	client.setConnectTimeout(220000);
	client.setReadTimeout(220000);
	WebResource resource = client.resource(apiUrl);
	return resource;
    }

    private boolean doNormalSearch() throws IOException, DocumentException, SQLException
    {
	String url;

	if(type.equals("web"))
	    url = "https://api.datamarket.azure.com/Bing/SearchWeb/v1/Web";
	else if(type.equals("image"))
	    url = "https://api.datamarket.azure.com/Bing/Search/v1/Image";
	else
	    throw new IllegalArgumentException();

	WebResource webResource = createWebResource(url, authCredentials_web);
	webResource = createParameters(webResource);

	ClientResponse response = null;
	String content = null;

	for(int retry = 0; retry < 2; retry++)
	{
	    try
	    {
		response = webResource.get(ClientResponse.class);

	    }
	    catch(Exception e)
	    {
		log.fatal("Fatal Bing request; wait 1s; try number: " + retry);
		try
		{
		    Thread.sleep(1000);
		}
		catch(InterruptedException e1)
		{
		}
		continue;
	    }

	    StringWriter writer = new StringWriter();
	    IOUtils.copy(response.getEntityInputStream(), writer);
	    content = writer.toString();

	    if(response != null && response.getStatus() == 503)
	    {
		log.fatal("bing error wait for 11 minutes; retry: " + retry + " description: " + content);

		try
		{
		    Thread.sleep(660000);
		}
		catch(InterruptedException e1)
		{
		}
	    }
	    else
	    {
		if(response.getStatus() == 503)
		    throw new RuntimeException("No transactions left for Bing Web Search");

		String xml = content.trim().replaceFirst("^([\\W]+)<", "<");

		SAXReader reader = new SAXReader();

		Document document;
		try
		{
		    document = reader.read(IOUtils.toInputStream(xml, "UTF-8"));
		    //document = reader.read(response.getEntityInputStream());
		    return parse(document.getRootElement());
		}
		catch(DocumentException e)
		{
		    if(content.contains("Please try again later"))
		    {
			log.fatal("bing bussy; wait for a while; retry: " + retry + " description: " + content);

			try
			{
			    Thread.sleep(120000 + retry * 10000);
			}
			catch(InterruptedException e1)
			{
			}
		    }
		    else
		    {
			log.fatal("Can't parse content: " + content);
			throw e;
		    }
		}
	    }
	}
	throw new RuntimeException("couldn finish after 50 retrys");

    }

    private boolean parse(Element startElement) throws SQLException
    {

	List<Element> entrys = startElement.elements("entry");

	int counter = 0;

	for(Element entry : entrys)
	{

	    Element prop = entry.element("content").element("properties");

	    Resource resource = new Resource();
	    ResourceDecorator decoratedResource = new ResourceDecorator(resource);
	    String ti = convertHighlighting(prop.elementText("Title"));
	    resource.setTitle(ti);
	    resource.setDescription(convertHighlighting(prop.elementText("Description")));

	    System.out.println(prop.elementText("Title") + " --- " + ti + " --- " + resource.getTitle());

	    String url3 = prop.elementText("Url");
	    String url4 = url3.replaceAll("[^\\u0000-\\uFFFF]", "\uFFFD");

	    if(!url3.equals(url4))
	    {
		log.warn("replaced 4 byte char in: " + url4);
	    }
	    resource.setUrl(url4);
	    //ResourceDecorator.setResourceDecoratorId(prop.elementText("ID"));
	    decoratedResource.setRankAtService(++loadedResourceDecorators);

	    //ResourceDecorator.setThumbnails(thumbnails);

	    resource.setMetadataValue("query_id", Integer.toString(query.getId()));
	    resource.setMetadataValue("url_captures", null);
	    resource.setMetadataValue("first_timestamp", "");
	    resource.setMetadataValue("last_timestamp", "");
	    resource.setMetadataValue("crawl_time", "0");

	    results.add(decoratedResource);

	    //archiveSearchManager.insertResult(query.getId(), decoratedResource.getRankAtService(), resource.getTitle(), resource.getDescription(), resource.getUrl());

	    counter++;
	}

	return counter == top; // return false if we got less ResourceDecorators than requested
    }

    /*
        private static String createMarket(String language)
        {
    	if(language.equalsIgnoreCase("ar"))
    	    return "ar-XA";
    	if(language.equalsIgnoreCase("bg"))
    	    return "bg-BG";
    	if(language.equalsIgnoreCase("cs"))
    	    return "cs-CZ";
    	if(language.equalsIgnoreCase("da"))
    	    return "da-DK";
    	if(language.equalsIgnoreCase("de"))
    	    return "de-DE";
    	if(language.equalsIgnoreCase("el"))
    	    return "el-GR";
    	if(language.equalsIgnoreCase("es"))
    	    return "es-ES";
    	if(language.equalsIgnoreCase("et"))
    	    return "et-EE";
    	if(language.equalsIgnoreCase("fi"))
    	    return "fi-FI";
    	if(language.equalsIgnoreCase("fr"))
    	    return "fr-FR";
    	if(language.equalsIgnoreCase("he"))
    	    return "he-IL";
    	if(language.equalsIgnoreCase("hr"))
    	    return "hr-HR";
    	if(language.equalsIgnoreCase("hu"))
    	    return "hu-HU";
    	if(language.equalsIgnoreCase("it"))
    	    return "it-IT";
    	if(language.equalsIgnoreCase("ja"))
    	    return "ja-JP";
    	if(language.equalsIgnoreCase("ko"))
    	    return "ko-KR";
    	if(language.equalsIgnoreCase("lt"))
    	    return "lt-LT";
    	if(language.equalsIgnoreCase("lv"))
    	    return "lv-LV";
    	if(language.equalsIgnoreCase("nb"))
    	    return "nb-NO";
    	if(language.equalsIgnoreCase("nl"))
    	    return "nl-NL";
    	if(language.equalsIgnoreCase("pl"))
    	    return "pl-PL";
    	if(language.equalsIgnoreCase("pt"))
    	    return "pt-PT";
    	if(language.equalsIgnoreCase("ro"))
    	    return "ro-RO";
    	if(language.equalsIgnoreCase("ru"))
    	    return "ru-RU";
    	if(language.equalsIgnoreCase("sk"))
    	    return "sk-SK";
    	if(language.equalsIgnoreCase("sl"))
    	    return "sl-SL";
    	if(language.equalsIgnoreCase("sv"))
    	    return "sv-SE";
    	if(language.equalsIgnoreCase("th"))
    	    return "th-TH";
    	if(language.equalsIgnoreCase("tr"))
    	    return "tr-TR";
    	if(language.equalsIgnoreCase("uk"))
    	    return "uk-UA";
    	if(language.equalsIgnoreCase("zh"))
    	    return "zh-CN";
    	return "en-US";
        }
    */

    public static class AuthCredentials implements Serializable
    {
	private static final long serialVersionUID = 1411969017572131214L;

	private final String key;
	private final String secret;

	public AuthCredentials(String key)
	{
	    this(key, null);
	}

	public AuthCredentials(String key, String secret)
	{

	    this.key = key;
	    this.secret = secret;
	}

	public String getKey()
	{
	    return key;
	}

	public String getSecret()
	{
	    return secret;
	}

	@Override
	public String toString()
	{
	    StringBuilder builder = new StringBuilder();
	    builder.append("AuthCredentials [");
	    if(key != null)
	    {
		builder.append("key=");
		builder.append(key);
		builder.append(", ");
	    }
	    if(secret != null)
	    {
		builder.append("secret=");
		builder.append(secret);
	    }
	    builder.append("]");
	    return builder.toString();
	}

    }

}

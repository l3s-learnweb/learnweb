package de.l3s.archiveSearch;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.util.StringHelper;

public class OpenSearchClient
{
    private static final Logger log = Logger.getLogger(OpenSearchClient.class);

    String openSearchUrl = "https://archive-it.org/seam/resource/opensearch?";
    String query;
    int numHits = 50; //Default is 10
    int position = 0; //Default is 0
    int maxHitsPerSite = 1; //Default is 1, 0 is for all hits
    int index; //Index to search, for Archive-It its collection Id
    String[] types = null; //types: text/html, application/pdf, etc.
    String[] sites = null; //Multiple sites separated by semi-colon, Default is all
    int totalResultsCount = 0; //To retrieve the total search results count for a search

    private SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM dd, yyyy");

    public OpenSearchClient()
    {
    }

    public OpenSearchClient(String query, int index)
    {
        this.query = query;
        this.index = index;
    }

    public OpenSearchClient(String query, int index, String types, String sites)
    {
        this.query = query;
        this.index = index;
        if(types != null && !types.isEmpty())
            this.types = types.split(";");
        if(sites != null && !sites.isEmpty())
            this.sites = sites.split(";");
    }

    public List<ResourceDecorator> getResults()
    {
        List<ResourceDecorator> resources = new ArrayList<ResourceDecorator>();
        openSearchUrl += "i=" + index + "&n=" + numHits + "&q=" + StringHelper.urlEncode(query);
        if(types != null)
        {
            for(String type : types)
                openSearchUrl += "&t=" + type;
        }
        if(sites != null)
        {
            for(String site : sites)
                openSearchUrl += "&s=" + site;
        }
        String response;
        try
        {
            response = IOUtils.toString(new URL(openSearchUrl));
            resources = parseXmlResponse(response);
        }
        catch(IOException | DOMException | ParseException e)
        {
            e.printStackTrace();
        }
        return resources;
    }

    public List<ResourceDecorator> parseXmlResponse(String response) throws DOMException, ParseException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document doc = null;
        try
        {
            db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(response)));
        }
        catch(ParserConfigurationException e)
        {
            log.error(e);
        }
        catch(SAXException | IOException e)
        {
            log.error(e);
        }

        totalResultsCount = Integer.parseInt(doc.getElementsByTagName("totalResults").item(0).getTextContent());

        NodeList items = doc.getElementsByTagName("item");
        List<ResourceDecorator> resources = new ArrayList<ResourceDecorator>();

        for(int i = 0; i < items.getLength(); i++)
        {
            Element item = (Element) items.item(i);
            String waybackDate = item.getElementsByTagName("date").item(0).getTextContent();
            Date rDate = waybackDateFormat.parse(waybackDate);

            Resource r = new Resource();
            r.setTitle(item.getElementsByTagName("title").item(0).getTextContent());
            r.setUrl("https://wayback.archive-it.org/" + index + "/" + waybackDate + "/" + item.getElementsByTagName("link").item(0).getTextContent());
            r.setDescription("&lt;B&gt;" + shortDateFormat.format(rDate) + "&lt;/B&gt;" + "&lt;br/&gt;" + item.getElementsByTagName("description").item(0).getTextContent());
            r.setType(item.getElementsByTagName("type").item(0).getTextContent());
            r.setCreationDate(waybackDateFormat.parse(item.getElementsByTagName("date").item(0).getTextContent()));
            ResourceDecorator decoratedResource = new ResourceDecorator(r);
            decoratedResource.setTempId(i + 1);
            decoratedResource.setSnippet(StringHelper.shortnString(r.getDescription(), 240));
            //decoratedResource.setSnippet(decoratedResource.getShortSnippet());
            resources.add(decoratedResource);
        }
        return resources;
    }

    public int getTotalResultsCount()
    {
        return totalResultsCount;
    }

    public static void main(String[] args)
    {
        OpenSearchClient c = new OpenSearchClient("canadian groups", 227);
        //System.out.println(c.getResults());
        for(ResourceDecorator r : c.getResults())
            log.info(r.getResource());
        System.exit(0);
    }
}

package de.l3s.archivedemo;

import java.util.ArrayList;

import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.l3s.util.StringHelper;

public class DBPediaClient
{

    private static final Logger log = Logger.getLogger(DBPediaClient.class);

    public ArrayList<Entity> lookup(String query)
    {
	ArrayList<Entity> e = new ArrayList<Entity>();
	Client client = Client.create();
	WebResource resource = client.resource("http://prometheus.kbs.uni-hannover.de:1112/api/search/KeywordSearch?QueryString=" + StringHelper.urlEncode(query));

	ClientResponse response;
	try
	{
	    response = resource.accept("application/xml").get(ClientResponse.class);
	}
	catch(ClientHandlerException ex)
	{
	    log.error("No connection to DBpedia: " + ex.getMessage());
	    return e;
	}

	if(response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL)
	{
	    e = parseXML(response.getEntity(String.class));
	}
	return e;
    }

    public ArrayList<Entity> parseXML(String response)
    {
	ArrayList<Entity> e = new ArrayList<Entity>();
	Document doc = Jsoup.parse(response, "", Parser.xmlParser());
	for(Element el : doc.select("Result"))
	{
	    Entity newEntity = new Entity();
	    newEntity.setLabel(el.getElementsByTag("Label").get(0).text());
	    newEntity.setUri(el.getElementsByTag("URI").get(0).text());
	    e.add(newEntity);
	}
	return e;
    }

    public static void main(String[] args)
    {
	DBPediaClient dbPediaClient = new DBPediaClient();
	ArrayList<Entity> relatedEntities = dbPediaClient.lookup("german chancellor");
	for(Entity e : relatedEntities)
	    System.out.println(e.getLabel() + "-" + e.getUri());
    }
}

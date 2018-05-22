package de.l3s.learnweb.facts2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

public class FactSheet implements Serializable
{
    private static final long serialVersionUID = 8407292019239615913L;
    private static final Logger log = Logger.getLogger(FactSheet.class);

    private List<Entity> entities;
    private String query;
    private Boolean supported = false;

    public FactSheet()
    {
        query = "";
        setEntities(null);
    }

    public FactSheet(String query)
    {
        this.query = query;
        setEntities(lookup(query));
    }

    private ArrayList<Entity> lookup(String query)
    {
        ArrayList<Entity> e = new ArrayList<Entity>();
        Client client = Client.create();
        WebResource resource = client.resource("http://prometheus.kbs.uni-hannover.de:1112/api/search/KeywordSearch?QueryString=" + StringHelper.urlEncode(query));

        //WebResource resource = client.resource("http://lookup.dbpedia.org/api/search.asmx/KeywordSearch?QueryString="+query);
        ClientResponse response;
        try
        {
            response = resource.accept("application/xml").get(ClientResponse.class);
        }
        catch(ClientHandlerException ex)
        {
            log.fatal("No connection to DBpedia: " + ex.getMessage());
            return e;
        }

        if(response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL)
        {
            e = parseXML(response.getEntity(String.class));
        }
        for(Entity entity : e)
        {
            if(entity.getSupported())
            {
                e.remove(entity);
                e.add(0, entity);
                break;
            }
        }
        if(e.size() > 0 && e.get(0).getSupported())
        {
            setSupported(true);
            e.get(0).extractInfo();
        }
        else
        {
            for(Entity entity : e)
            {
                entity.extractInfo();

            }

            e.clear();
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
            for(Element label : el.getElementsByTag("Label"))
            {
                String type = label.text();
                if(type.equals("person") || type.equals("place") || type.equals("organization"))
                {
                    newEntity.setType(type);
                    newEntity.setSupported(true);
                }
            }
            newEntity.setLabel(el.getElementsByTag("Label").get(0).text());
            newEntity.setUrl(el.getElementsByTag("URI").get(0).text());
            newEntity.setDescription(el.getElementsByTag("Description").get(0).text());
            e.add(newEntity);
        }
        return e;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public List<Entity> getEntities()
    {
        return entities;
    }

    public void setEntities(List<Entity> entities)
    {
        this.entities = entities;
    }

    public Boolean getSupported()
    {
        return supported;
    }

    public void setSupported(Boolean supported)
    {
        this.supported = supported;
    }

    /* test
    public static void main(String[] args)
    {
        new FactSheet("gerhard schröder");
        
    }
    */
}

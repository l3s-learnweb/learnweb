package de.l3s.sparqlclient;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.l3s.learnweb.Learnweb;

public class SparqlClient
{
    private static String baseSparQLURL;
    private static String webserviceURL = "submitquery";
    private static String languagesURL = "transcriptlanguages";
    private Client client;

    public SparqlClient(Learnweb learnweb)
    {
	client = Client.create();
	baseSparQLURL = learnweb.getProperties().getProperty("SPARQL_SERVICE_URL");
    }

    public String search(String title, String transcriptLanguage)
    {

	WebResource web = client.resource(baseSparQLURL + webserviceURL);
	String query = "Select ?value FROM <http://data-observatory.org/ted_talks> WHERE  {?talk <http://www.w3.org/ns/ma-ont#title> \"" + title
		+ "\"^^<http://www.w3.org/2001/XMLSchema#string> . ?transcript <http://purl.org/ontology/bibo/transcriptOf> ?talk . ?transcript <http://purl.org/dc/terms/language>\"" + transcriptLanguage
		+ "\"^^<http://www.w3.org/2001/XMLSchema#string>. ?transcript <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?value} LIMIT 1";
	String SparqlURL = "http://meco.l3s.uni-hannover.de:8890/sparql";
	MultivaluedMap<String, String> params = new MultivaluedMapImpl();
	params.add("query", query);
	params.add("url", SparqlURL);
	ClientResponse resp = web.queryParams(params).post(ClientResponse.class);

	if(resp.getStatus() != 200)
	{
	    throw new RuntimeException("Failed : HTTP error code : " + resp.getStatus());
	}
	return resp.getEntity(String.class);
    }

    public HashMap<String, String> getTranscriptLanguages(String title)
    {
	WebResource web = client.resource(baseSparQLURL + languagesURL);
	String query = "Select ?value FROM <http://data-observatory.org/ted_talks> WHERE  {?talk <http://www.w3.org/ns/ma-ont#title> \"" + title
		+ "\"^^<http://www.w3.org/2001/XMLSchema#string> . ?transcript <http://purl.org/ontology/bibo/transcriptOf> ?talk . ?transcript <http://purl.org/dc/terms/language> ?value}";
	String SparqlURL = "http://meco.l3s.uni-hannover.de:8890/sparql";
	MultivaluedMap<String, String> params = new MultivaluedMapImpl();
	params.add("query", query);
	params.add("url", SparqlURL);
	ResourceLogHashMap resp = web.queryParams(params).accept(MediaType.APPLICATION_XML).get(ResourceLogHashMap.class);

	return resp.getResourceLogHashMap();
    }

    /* public static void main(String[] args)
     {

    Client client = Client.create();
    WebResource web = client.resource(webserviceURL);
    String query = "Select ?value FROM <http://data-observatory.org/ted_talks> WHERE  {?talk <http://data.linkededucation.org/resource/ted/id> \"aaron_o_connell_making_sense_of_a_visible_quantum_object\"^^<http://www.w3.org/2001/XMLSchema#string>. ?transcript <http://purl.org/ontology/bibo/transcriptOf> ?talk . ?transcript <http://purl.org/dc/terms/language> \"en\"^^<http://www.w3.org/2001/XMLSchema#string>. ?transcript <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?value} LIMIT 1";
    String SparqlURL = "http://meco.l3s.uni-hannover.de:8890/sparql";
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("query", query);
    params.add("url", SparqlURL);
    ClientResponse resp = web.queryParams(params).post(ClientResponse.class);

    if(resp.getStatus() != 200)
    {
        throw new RuntimeException("Failed : HTTP error code : " + resp.getStatus());
    }
    System.out.println(resp.getEntity(String.class));

     }*/
}

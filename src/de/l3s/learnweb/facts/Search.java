package de.l3s.learnweb.facts;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;

public class Search
{
    private static String[] propertyList = { "P569", "P19", "P570", "P20", "P21", "P26", "P40", "P102", "P112", "P17", "P159", "P1128", "452", "P36", "P473", "P1082", "P2046", "P272", "P161", "P57", "P136", "P50", "P1712", "P674", "P170", "P136", "P1104", "P577", "P178", "P404",
	    "P136", "P400", "P287", "P725", "P225", "P141", "P279", "P2067", "P2048", "P18" };
    private static String serviceUrl = "http://godzilla.kbs.uni-hannover.de/bigdata/namespace/wdq/sparql";

    /*
     * get entity wikidata Id from name on sparql
     */
    public static List<String> searchWikiIdSparql(String name)
    {
	List<String> idList = new ArrayList<>();
	String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "SELECT ?item  WHERE {\n" + "  ?item rdfs:label \"" + name + "\"@en .\n" + "}\n";
	QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceUrl, queryString);
	try
	{
	    ResultSet results = qexec.execSelect();
	    for(; results.hasNext();)
	    {
		QuerySolution soln = results.nextSolution();
		String idString = soln.get("item").toString();
		idList.add(idString.substring(idString.indexOf("Q")));
	    }
	}
	catch(Exception ex)
	{
	    System.out.println(ex.getMessage());
	}
	finally
	{
	    qexec.close();
	}
	return idList;
    }

    /*
     * get wikidata Id from name through web API
     */
    public static List<String> searchWikiIdPhp(String name)
    {
	List<String> idList = new ArrayList<>();
	String formatName = name.replaceAll(" ", "%20");
	String url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=" + formatName + "&language=en&format=json";
	CloseableHttpClient httpClient = null;
	CloseableHttpResponse response = null;
	HttpEntity entity = null;
	String responseContent = null;
	try
	{
	    httpClient = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost(url);
	    response = httpClient.execute(httpPost);
	    entity = response.getEntity();
	    responseContent = EntityUtils.toString(entity, "UTF-8");
	    JSONObject jsonObject = new JSONObject(responseContent);
	    JSONArray jsonArray = (JSONArray) jsonObject.get("search");
	    for(int i = 0; i < jsonArray.length(); i++)
	    {
		JSONObject wikiEntity = jsonArray.getJSONObject(i);
		idList.add(wikiEntity.getString("id"));
	    }
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
	finally
	{
	    try
	    {
		if(response != null)
		{
		    response.close();
		}
		if(httpClient != null)
		{
		    httpClient.close();
		}
	    }
	    catch(IOException e)
	    {
		e.printStackTrace();
	    }
	}
	return idList;
    }

    public static Entity searchRdfWikidata(String id, String language) throws ParseException
    {
	Entity entity = new Entity();
	entity.setWikiId(id);
	String labelString = "PREFIX schema: <http://schema.org/>\n" + "PREFIX entity: <http://www.wikidata.org/entity/>\n" + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "SELECT ?label1 ?title1 WHERE \n" + "{\n" + "  entity:" + id + " schema:description ?title .\n"
		+ "  entity:" + id + " rdfs:label ?label .\n" + "  FILTER(LANG(?label) = 'en') .\n" + "  FILTER(LANG(?title) = 'en') .\n" + "   BIND(str(?label) AS ?label1) .\n" + "   BIND (str(?title) AS ?title1) .\n" + "}";
	QueryExecution labelExec = QueryExecutionFactory.sparqlService(serviceUrl, labelString);
	try
	{
	    ResultSet res = labelExec.execSelect();
	    for(; res.hasNext();)
	    {
		QuerySolution soln = res.nextSolution();
		String label = soln.get("label1").toString();
		String title = soln.get("title1").toString();
		entity.setLabel(label);
		entity.setTitle(title);
	    }
	}
	catch(Exception ex)
	{
	    System.out.println(ex.getMessage());
	}
	finally
	{
	    labelExec.close();
	}
	if(language.equals("en"))
	{
	    getProp(entity, language);
	}
	else
	{
	    Entity tmpEntity = new Entity();
	    tmpEntity.setWikiId(entity.getWikiId());
	    getProp(tmpEntity, "en");
	    getProp(entity, language);
	    Map<String, List<String>> wikiProp = new HashMap<>();
	    for(String propId : tmpEntity.getPropList().keySet())
	    {
		wikiProp = entity.getWikiStats();
		if(!entity.getPropList().containsKey(propId))
		{
		    wikiProp.put(propId, tmpEntity.getWikiStats().get(propId));
		}
	    }
	    entity.setWikiStats(wikiProp);
	}
	formatProp(entity);
	return entity;
    }

    /*
     * get properties of entity for given language
     */
    public static Entity getProp(Entity entity, String language)
    {
	String queryString = "PREFIX entity: <http://www.wikidata.org/entity/>\n" + "PREFIX wikibase: <http://wikiba.se/ontology#>\n" + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" + "PREFIX wd: <http://www.wikidata.org/Entity/>\n"
		+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "SELECT ?propUrl ?propLabel1 ?valUrl1 ?valLabel1\n" + "WHERE\n" + "{\n" + "   entity:" + entity.getWikiId() + " ?propUrl ?valUrl .\n" + "   ?property ?ref ?propUrl .\n"
		+ "   ?property a wikibase:Property .\n" + "   ?property rdfs:label ?propLabel\n";
	if(propertyList.length != 0)
	{
	    queryString += "   values ?propUrl {";
	    for(String prop : propertyList)
	    {
		queryString += " wdt:" + prop;
	    }
	    queryString += "}\n";
	}
	queryString += "   OPTIONAL {\n" + "       ?valUrl rdfs:label ?valLabel .\n" + "       FILTER( LANG(?valLabel) = '" + language + "' ) . \n" + "       }\n" + "       FILTER( LANG(?propLabel) = '" + language + "' ) . \n" + "   BIND (str(?propLabel) AS ?propLabel1) .\n"
		+ "   BIND (str(?valUrl) AS ?valUrl1) .\n" + "   BIND (str(?valLabel) AS ?valLabel1) .\n" + "   FILTER regex(str(?propUrl),'direct') .\n" + "}\n";
	QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceUrl, queryString);
	Map<String, List<String>> wikiProp = new HashMap<>();
	Map<String, String> propList = new HashMap<>();
	List<String> image = new ArrayList<>();
	try
	{
	    ResultSet results = qexec.execSelect();
	    for(; results.hasNext();)
	    {
		QuerySolution soln = results.nextSolution();
		String propUrl = soln.get("propUrl").toString();
		String propUrl1 = propUrl.substring(propUrl.indexOf("P"));
		String propLabel = soln.get("propLabel1").toString();
		String place = "";
		String val = "";
		if(soln.get("valLabel1") != null)
		{
		    val = soln.get("valLabel1").toString();
		}
		else
		{
		    val = soln.get("valUrl1").toString();
		}
		if(propUrl1.equals("P18")) //get imageUrl
		{
		    image.add(val);
		}
		if(propUrl1.equals("P19") || propUrl1.equals("P20")) //get place from city to country
		{
		    String placeId = soln.get("valUrl1").toString();
		    place = getPlaces(placeId, language);
		    List<String> places = new ArrayList(Arrays.asList(place.split(", ")));
		    if(!propList.containsKey(propUrl1))
		    {
			propList.put(propUrl1, propLabel);
		    }
		    if(!wikiProp.containsKey(propUrl1))
		    {
			wikiProp.put(propUrl1, places);
		    }
		}
		else
		{
		    if(!val.contains("http"))
		    {
			if(!propList.containsKey(propUrl1))
			{
			    propList.put(propUrl1, propLabel);
			}
			if(wikiProp.containsKey(propUrl1))
			{
			    wikiProp.get(propUrl1).add(val);
			}
			else
			{
			    List<String> valList = new ArrayList<>();
			    valList.add(val);
			    wikiProp.put(propUrl1, valList);
			}
		    }
		}
	    }
	    entity.setWikiStats(wikiProp);
	    entity.setPropList(propList);
	    entity.setImageUrl(image);
	}
	catch(Exception ex)
	{
	    System.out.println(ex.getMessage());
	}
	finally
	{
	    qexec.close();
	}
	return entity;
    }

    /*
     * for place of birth or death, the result must be a list from city to country
     */
    public static String getPlaces(String placeId, String language)
    {
	String place = "";
	String places = "";
	String newPlaceId = "";
	String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" + "SELECT ?propUrl1 ?valUrl ?title ?otherTitle WHERE {\n" + "   <" + placeId + "> ?propUrl ?valUrl .\n"
		+ "   values ?propUrl {wdt:P31 wdt:P131} .\n" + "   <" + placeId + "> rdfs:label ?title1 .\n" + "   FILTER(LANG(?title1)='en') .\n" + "   OPTIONAL {" + "       <" + placeId + "> rdfs:label ?otherTitle1 .\n" + "       FILTER(LANG(?otherTitle1)='" + language
		+ "'). \n" + "       BIND (str(?otherTitle1) AS ?otherTitle) .\n" + "   }\n" + "   BIND (str(?title1) AS ?title) .\n" + "   BIND (str(?propUrl) AS ?propUrl1) .\n" + "}";
	QueryExecution qexec = QueryExecutionFactory.sparqlService("http://query.wikidata.org/sparql", query);
	Map<String, List<String>> wikiProp = new HashMap<>();
	try
	{
	    ResultSet results = qexec.execSelect();
	    for(; results.hasNext();)
	    {
		QuerySolution soln = results.nextSolution();
		String propUrl = soln.get("propUrl1").toString();
		String propUrl1 = propUrl.substring(propUrl.indexOf("P"));
		String title = soln.get("title").toString();
		if(place.isEmpty())
		{
		    if(soln.get("otherTitle") != null)
		    {
			place = soln.get("otherTitle").toString();
		    }
		    else
		    {
			place = title;
		    }
		}

		if(propUrl1.equals("P131"))
		{
		    newPlaceId = soln.get("valUrl").toString();

		}
		if(wikiProp.containsKey(propUrl1))
		{
		    wikiProp.get(propUrl1).add(soln.get("valUrl").toString());
		}
		else
		{
		    List<String> valList = new ArrayList<>();
		    valList.add(soln.get("valUrl").toString());
		    wikiProp.put(propUrl1, valList);
		}
	    }
	    if(wikiProp.containsKey("P131"))
	    {
		places = place + ", " + getPlaces(newPlaceId, language);
	    }
	    else
	    {
		places = place;
	    }
	}
	catch(Exception ex)
	{
	    System.out.println(ex.getMessage());
	}
	finally
	{
	    qexec.close();
	}
	return places;
    }

    public static Entity formatProp(Entity entity) throws ParseException
    {
	List<FactSheetEntry> factSheetEntries = new ArrayList<>();

	if(entity.getPropList().containsKey("P569")) //birth template
	{
	    FactSheetEntry factSheetEntry = new FactSheetEntry();
	    List<Object> propValueList = new ArrayList<Object>();
	    factSheetEntry.setLabel("birth");
	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    Date date = df.parse(entity.getWikiStats().get("P569").get(0));
	    propValueList.add(date);
	    for(String place : entity.getWikiStats().get("P19"))
	    {
		propValueList.add(place);
	    }
	    entity.getPropList().remove("P19");
	    factSheetEntry.setData(propValueList);
	    factSheetEntry.setTemplate("birth_place");
	    entity.getPropList().remove("P569");
	    factSheetEntries.add(factSheetEntry);
	}
	if(entity.getPropList().containsKey("P570")) //death template
	{
	    FactSheetEntry factSheetEntry = new FactSheetEntry();
	    List<Object> propValueList = new ArrayList<Object>();
	    factSheetEntry.setLabel("death");
	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    Date date = df.parse(entity.getWikiStats().get("P570").get(0));
	    propValueList.add(date);
	    for(String place : entity.getWikiStats().get("P20"))
	    {
		propValueList.add(place);
	    }
	    entity.getPropList().remove("P20");
	    factSheetEntry.setData(propValueList);
	    factSheetEntry.setTemplate("death_place");
	    entity.getPropList().remove("P570");
	    factSheetEntries.add(factSheetEntry);
	}
	for(String prop : entity.getPropList().keySet())
	{
	    FactSheetEntry factSheetEntry = new FactSheetEntry();
	    List<Object> propValueList = new ArrayList<Object>();
	    if(prop.equals("P2067"))
		factSheetEntry.setLabelKey("weight");
	    factSheetEntry.setLabel(entity.getPropList().get(prop));
	    for(String propValue : entity.getWikiStats().get(prop))
	    {
		if(!propValue.contains("http"))
		{
		    propValueList.add(propValue);
		}
	    }
	    factSheetEntry.setData(propValueList);
	    if(!factSheetEntry.getData().isEmpty())
		factSheetEntries.add(factSheetEntry);
	}
	entity.setFacts(factSheetEntries);
	return entity;
    }

}

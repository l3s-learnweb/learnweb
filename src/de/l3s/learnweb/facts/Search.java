package de.l3s.learnweb.facts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class Search
{
    public static String[] propertyList = { "P569", "P19", "P570", "P20", "P21", "P26", "P40", "P102", "P112", "P17", "P159", "P1128", "452", "P36", "P473", "P1082", "P2046", "P272", "P161", "P57", "P136", "P50", "P1712", "P674", "P170", "P136", "P1104", "P577", "P178", "P404",
	    "P136", "P400", "P287", "P725", "P225", "P141", "P279", "P2067", "P2048" };

    public static Entity searchRdfWikidata(String id, String language) throws ParseException
    {
	Entity entity = new Entity();
	entity.setWikiId(id);
	String queryString = "PREFIX entity: <http://www.wikidata.org/entity/>\n" + "PREFIX wikibase: <http://wikiba.se/ontology#>\n" + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" + "PREFIX wd: <http://www.wikidata.org/Entity/>\n"
		+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "SELECT ?propUrl ?propLabel1 ?valUrl1 ?valLabel1\n" + "WHERE\n" + "{\n" + "   entity:" + id + " ?propUrl ?valUrl .\n" + "   ?property ?ref ?propUrl .\n" + "   ?property a wikibase:Property .\n"
		+ "   ?property rdfs:label ?propLabel\n";
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
	Map<String, List<String>> wikiProp = new HashMap<>();
	Map<String, String> propList = new HashMap<>();
	QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
	try
	{
	    ResultSet results = qexec.execSelect();
	    for(; results.hasNext();)
	    {
		QuerySolution soln = results.nextSolution();
		String propUrl = soln.get("propUrl").toString();
		String propUrl1 = propUrl.substring(propUrl.indexOf("P"));
		String propLabel = soln.get("propLabel1").toString();
		String val = "";
		if(!propList.containsKey(propUrl1))
		{
		    propList.put(propUrl1, propLabel);
		}
		if(soln.get("valLabel1") != null)
		{
		    val = soln.get("valLabel1").toString();
		}
		else
		{
		    val = soln.get("valUrl1").toString();
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
	    entity.setWikiStats(wikiProp);
	    entity.setPropList(propList);
	}
	catch(Exception ex)
	{
	    System.out.println(ex.getMessage());
	}
	finally
	{
	    qexec.close();
	}

	return formatProp(entity);
	//	return entity;
    }

    public static Entity formatProp(Entity entity) throws ParseException
    {
	List<FactSheetEntry> factSheetEntries = new ArrayList<>();
	if(entity.getPropList().containsKey("P569"))
	{
	    FactSheetEntry factSheetEntry = new FactSheetEntry();
	    List<Object> propValueList = new ArrayList<Object>();
	    factSheetEntry.setLabel("birth");
	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    Date date = df.parse(entity.getWikiStats().get("P569").get(0));
	    propValueList.add(date);
	    if(!entity.getWikiStats().get("P19").toString().contains("http"))
	    {
		propValueList.add(entity.getWikiStats().get("P19").get(0));
		entity.getPropList().remove("P19");
	    }
	    factSheetEntry.setData(propValueList);
	    factSheetEntry.setTemplate("birth_place");
	    entity.getPropList().remove("P569");
	    factSheetEntries.add(factSheetEntry);
	}
	for(String prop : entity.getPropList().keySet())
	{
	    FactSheetEntry factSheetEntry = new FactSheetEntry();
	    List<Object> propValueList = new ArrayList<Object>();
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

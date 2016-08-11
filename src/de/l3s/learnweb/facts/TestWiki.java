package de.l3s.learnweb.facts;

import java.io.IOException;
import java.text.ParseException;

import org.json.JSONObject;

public class TestWiki
{
    public static void main(String[] args) throws IOException, ParseException
    {
	System.out.println("hello");
	Entity entity = Search.searchRdfWikidata("Q91", "en");
	JSONObject json = new JSONObject(entity);
	System.out.println(json.toString());
	System.out.println("bye");
	//	List<String> id = Search.searchWikiIdSparql("Barack Obama");
	//	//	List<String> id = Search.searchWikiIdPhp("Barack Obama");
	//	for(String idName : id)
	//	{
	//	    System.out.println(idName);
	//	}

    }

}

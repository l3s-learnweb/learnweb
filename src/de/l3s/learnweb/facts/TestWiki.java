package de.l3s.learnweb.facts;

import java.io.IOException;
import java.text.ParseException;

import com.google.gson.Gson;;

public class TestWiki
{
    public static void main(String[] args) throws IOException, ParseException
    {
	System.out.println("hello");
	Entity entity = Search.searchRdfWikidata("Q76", "en");
	Gson gson = new Gson();
	String json = gson.toJson(entity);
	System.out.println(json);
	System.out.println("bye");
    }

}

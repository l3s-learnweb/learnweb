package de.l3s.archiveSearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class QueryCompletor
{
    private final static Logger log = Logger.getLogger(QueryCompletor.class);
    private final static int MAX_SUGGESTIONS = 20; // number of suggestions that is stored in the tree; 

    public static void main(String[] args)
    {

	final QueryCompletor queryCompletor = new QueryCompletor("de/l3s/archiveSearch/main_pages_de.csv");

	//String[] queries = { "obam", "francois de", "schrö", "o", "familia", "gerhard schrod" };

	String[] queries = { "4", "47" };

	for(final String query : queries)
	{
	    System.out.println("\n" + query + "\n-------------");
	    System.out.println(queryCompletor.getSuggestions(query, 20));
	}
    }

    private PatriciaTrie<Collection<WikipediaEntity>> trie;

    /**
     * 
     * @param resource expects a entity csv file like "de/l3s/archiveSearch/main_pages_en.csv"
     */
    public QueryCompletor(String resource)
    {
	trie = new PatriciaTrie<>();

	loadFromResource(resource);
    }

    public Collection<WikipediaEntity> getSuggestionsAsEntity(String query, int count)
    {
	// when count is > MAX_SUGGESTIONS the suggestions are not correct   

	query = query.replace('-', ' ').replace(',', ' ').replace('(', ' ').replace(')', ' ').toLowerCase();

	Collection<Collection<WikipediaEntity>> suggestionsCollections = trie.prefixMap(query).values();

	Collection<WikipediaEntity> suggestions = new FixedSizeTreeSet<>(count);

	// merge suggestions and sort by view count
	for(Collection<WikipediaEntity> collection : suggestionsCollections)
	{
	    for(WikipediaEntity entity : collection)
	    {
		if(!suggestions.add(entity))
		    break; // the view count of all other entities in this collection is to small 
	    }
	}

	return suggestions;
    }

    public List<String> getSuggestions(String query, int count)
    {
	Collection<WikipediaEntity> entities = getSuggestionsAsEntity(query, count);
	List<String> suggestions = new LinkedList<>();

	for(WikipediaEntity entity : entities)
	    suggestions.add(entity.getEntity());

	return suggestions;
    }

    private void addEntityToTrie(String key, WikipediaEntity entity)
    {
	Collection<WikipediaEntity> entities = trie.get(key);

	if(entities == null)
	{
	    entities = new FixedSizeTreeSet<>(MAX_SUGGESTIONS);
	    trie.put(key, entities);
	}

	entities.add(entity);
    }

    private void loadFromResource(String resource)
    {
	log.debug("Load entites from resource: " + resource);

	BufferedReader buffer = null;
	String line = "";
	final String spliter = "\t"; //"\",\"";
	try
	{
	    buffer = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(resource)));

	    while((line = buffer.readLine()) != null)
	    {
		//System.out.println(line);
		String[] fields = line.split(spliter); // substring(1, line.length() - 1). remove the leading and trailing " before splitting

		String title = fields[0];
		String viewCountStr = fields[1];

		WikipediaEntity wikipediaEntity = new WikipediaEntity(title, Integer.parseInt(viewCountStr));
		String formatted = title.replace('-', ' ').replace(',', ' ').replace('(', ' ').replace(')', ' ').toLowerCase();
		//System.out.println("\ntitle:" + title);

		String[] terms = formatted.split("\\s+");

		String subterms = "";
		StringBuilder sb = new StringBuilder();

		for(int j = 0; j < terms.length; j++)
		{
		    for(int i = j; i < terms.length; i++)
		    {
			sb.append(' ');
			sb.append(terms[i]);
		    }

		    subterms = sb.substring(1); // remove leading whitespace
		    addEntityToTrie(subterms, wikipediaEntity);
		    //System.out.println(subterms);

		    String clean = StringUtils.stripAccents(subterms);

		    if(!clean.equals(subterms))
		    {
			//System.out.println(clean);
			addEntityToTrie(clean, wikipediaEntity);
		    }

		    sb.setLength(0);
		}
	    }
	    buffer.close();

	    log.debug("Entities loaded");
	}
	catch(IOException e)
	{
	    log.error("Can't load entities", e);
	}
    }

    public class WikipediaEntity implements Comparable<WikipediaEntity>
    {
	private int viewCount;
	private String entity;

	public WikipediaEntity(String entity, int viewCount)
	{
	    super();
	    this.viewCount = viewCount;
	    this.entity = entity;
	}

	public int getViewCount()
	{
	    return viewCount;
	}

	public String getEntity()
	{
	    return entity;
	}

	@Override
	public int compareTo(WikipediaEntity other)
	{
	    if(other.viewCount == viewCount)
		return entity.compareTo(other.entity);

	    return Integer.compare(other.viewCount, viewCount);
	}

	@Override
	public String toString()
	{
	    return "Entity [" + entity + ", views=" + viewCount + "]";
	}

    }

}

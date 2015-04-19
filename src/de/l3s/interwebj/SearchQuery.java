package de.l3s.interwebj;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.xml.sax.SAXException;

import de.l3s.interwebj.jaxb.SearchResponse;
import de.l3s.interwebj.jaxb.SearchResultEntity;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.ResourceManager;

public class SearchQuery implements Serializable
{
    private final static Logger log = Logger.getLogger(SearchQuery.class);

    private static final long serialVersionUID = 681547180910687848L;
    protected List<ResourceDecorator> results;
    protected String elapsedTime;
    protected long totalResults;

    public SearchQuery(InputStream inputStream) throws IllegalResponseException
    {
	parse(inputStream);
    }

    /**
     * replaces all fields of this object with the values from the xml @param inputStream
     * 
     * @param inputStream stream of an interweb search query
     * @param date
     * @throws IOException
     * @throws SAXException
     * @throws IllegalResponseException
     */
    private void parse(InputStream inputStream) throws IllegalResponseException
    {
	try
	{
	    int counter = 0;
	    results = new LinkedList<ResourceDecorator>();
	    totalResults = 0;
	    Set<String> serviceSet = new HashSet<String>();

	    JAXBContext jaxbContext = JAXBContext.newInstance(SearchResponse.class);
	    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
	    SearchResponse response = (SearchResponse) jaxbUnmarshaller.unmarshal(inputStream);
	    List<SearchResultEntity> searchResults = response.getQuery().getResults();

	    for(SearchResultEntity searchResult : searchResults)
	    {
		Resource currentResource = ResourceManager.getResourceFromInterwebResult(searchResult);

		if(!currentResource.getType().equalsIgnoreCase("text") && null == currentResource.getThumbnail4()) // no thumbnail set
		{
		    log.error("Found no thumbnail:" + searchResult);

		    counter++;

		    if(counter > 5)
		    {
			log.error("To many missing thumbnails", new Exception());
			return;
		    }
		    continue;
		}

		ResourceDecorator decoratedResource = new ResourceDecorator(currentResource);
		decoratedResource.setSnippet(searchResult.getSnippet());
		decoratedResource.setRankAtService(searchResult.getRankAtService());

		// bing description contains snippet with term highlighting
		if(currentResource.getSource().equalsIgnoreCase("bing") && decoratedResource.getSnippet() == null)
		{
		    // add snippet
		    decoratedResource.setSnippet(currentResource.getDescription());
		    // remove search term highlighting from description
		    currentResource.setDescription(Jsoup.clean(currentResource.getDescription(), Whitelist.none()));
		}

		if(decoratedResource.getSnippet() == null)
		{
		    decoratedResource.setSnippet(currentResource.getShortDescription());
		}
		results.add(decoratedResource);

		if(!serviceSet.contains(searchResult.getService()))
		{
		    totalResults += searchResult.getTotalResultsAtService();
		    serviceSet.add(searchResult.getService());
		}
	    }

	}
	catch(JAXBException e)
	{
	    throw new IllegalResponseException(e);
	}
    }

    public List<ResourceDecorator> getResults()
    {
	return results;
    }

    public long getTotalResultCount()
    {
	return totalResults;
    }
}

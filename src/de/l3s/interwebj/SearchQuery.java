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
import de.l3s.interwebj.jaxb.ThumbnailEntity;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.Thumbnail;
import de.l3s.util.StringHelper;

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

		Resource currentResult = new Resource();

		currentResult.setType(searchResult.getType());
		currentResult.setTitle(searchResult.getTitle());

		if(!currentResult.getTitle().equals(searchResult.getDescription()))
		    currentResult.setDescription(searchResult.getDescription());
		currentResult.setLocation(searchResult.getService());
		currentResult.setSource(searchResult.getService());

		currentResult.setViews(searchResult.getNumberOfViews());
		currentResult.setUrl(StringHelper.urlDecode(searchResult.getUrl()));
		currentResult.setEmbeddedSize1Raw(searchResult.getEmbeddedSize1());
		currentResult.setEmbeddedSize3Raw(searchResult.getEmbeddedSize3());
		currentResult.setEmbeddedSize4Raw(searchResult.getEmbeddedSize4());
		//currentResult.setMaxImageUrl(searchResult.getImageUrl());
		currentResult.setDuration(searchResult.getDuration());

		if(!currentResult.getType().equalsIgnoreCase("image"))
		{
		    currentResult.setEmbeddedRaw(searchResult.getEmbeddedSize4());
		    if(null == currentResult.getEmbeddedRaw())
			currentResult.setEmbeddedRaw(searchResult.getEmbeddedSize3());
		}

		List<ThumbnailEntity> thumbnails = searchResult.getThumbnailEntities();

		for(ThumbnailEntity thumbnailElement : thumbnails)
		{
		    String url = thumbnailElement.getUrl();

		    int height = thumbnailElement.getHeight();
		    int width = thumbnailElement.getWidth();

		    // ipernity api doesn't return largest available thumbnail, so we have to guess it
		    if(searchResult.getService().equals("Ipernity") && url.contains(".560."))
		    {
			if(width == 560 || height == 560)
			{
			    double ratio = 640.0 / 560.;
			    width *= ratio;
			    height *= ratio;

			    url = url.replace(".560.", ".640.");
			}
		    }

		    Thumbnail thumbnail = new Thumbnail(url, width, height);

		    if(thumbnail.getHeight() <= 100 && thumbnail.getWidth() <= 100)
			currentResult.setThumbnail0(thumbnail);
		    else if(thumbnail.getHeight() < 170 && thumbnail.getWidth() < 170)
		    {
			thumbnail = thumbnail.resize(120, 100);
			currentResult.setThumbnail1(thumbnail);
		    }
		    else if(thumbnail.getHeight() < 500 && thumbnail.getWidth() < 500)
		    {
			currentResult.setThumbnail2(thumbnail.resize(300, 220));
		    }
		    else
		    //if(thumbnail.getHeight() < 600 && thumbnail.getWidth() < 600)
		    {
			currentResult.setThumbnail4(thumbnail);
		    }
		}
		if(!currentResult.getType().equalsIgnoreCase("text") && null == currentResult.getThumbnail4()) // no thumbnail set
		{
		    log.error("Found no thumbnail:" + searchResult);

		    counter++;

		    if(counter > 5)
		    {
			new Exception().printStackTrace();
			return;
		    }
		    continue;
		}

		ResourceDecorator decoratedResource = new ResourceDecorator(currentResult);
		decoratedResource.setSnippet(searchResult.getSnippet());
		decoratedResource.setRankAtService(searchResult.getRankAtService());

		// bing description contains snippet with term highlighting
		if(currentResult.getSource().equalsIgnoreCase("bing") && decoratedResource.getSnippet() == null)
		{
		    // add snippet
		    decoratedResource.setSnippet(currentResult.getDescription());
		    // remove search term highlighting from description
		    currentResult.setDescription(Jsoup.clean(currentResult.getDescription(), Whitelist.none()));
		}

		if(decoratedResource.getSnippet() == null)
		{
		    decoratedResource.setSnippet(currentResult.getShortDescription());
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

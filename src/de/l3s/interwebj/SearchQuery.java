package de.l3s.interwebj;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.interwebj.jaxb.SearchResponse;
import de.l3s.interwebj.jaxb.SearchResultEntity;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;

public class SearchQuery implements Serializable
{
    private static final Logger log = Logger.getLogger(SearchQuery.class);

    private static final long serialVersionUID = 681547180910687848L;
    private List<ResourceDecorator> results;

    private long totalResults;
    private List<Count> serviceCount = new ArrayList<>(); //service name, number of results at this service
    private List<String> serviceCountSaver = new ArrayList<>();

    SearchQuery(InputStream inputStream) throws IllegalResponseException
    {
        parse(inputStream);
    }

    /**
     * replaces all fields of this object with the values from the xml @param inputStream
     *
     * @param inputStream stream of an interweb search query
     * @throws IllegalResponseException
     */
    private void parse(InputStream inputStream) throws IllegalResponseException
    {
        try
        {
            FacetField ff = new FacetField("location");
            int counter = 0;
            results = new LinkedList<>();
            totalResults = 0;

            JAXBContext jaxbContext = JAXBContext.newInstance(SearchResponse.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            SearchResponse response = (SearchResponse) jaxbUnmarshaller.unmarshal(inputStream);

            if(response == null)
            {
                log.fatal("response is null");
                return;
            }
            if(response.getQuery() == null)
            {
                log.fatal("response query is null");
                return;
            }

            List<SearchResultEntity> searchResults = response.getQuery().getResults();

            for(SearchResultEntity searchResult : searchResults)
            {
                Resource currentResource = ResourceManager.getResourceFromInterwebResult(searchResult);

                if(currentResource.getType() != ResourceType.website && null == currentResource.getThumbnail2()) // no thumbnail set
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
                decoratedResource.setTitle(searchResult.getTitle());

                // bing description contains snippet with term highlighting
                if(currentResource.getSource() == ResourceService.bing && decoratedResource.getSnippet() == null)
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

                if(!serviceCountSaver.contains(searchResult.getService()))
                {
                    serviceCountSaver.add(searchResult.getService());
                    totalResults += searchResult.getTotalResultsAtService();
                    serviceCount.add(new Count(ff, searchResult.getService(), searchResult.getTotalResultsAtService()));
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

    public List<Count> getResultCountPerService()
    {
        return serviceCount;
    }
}

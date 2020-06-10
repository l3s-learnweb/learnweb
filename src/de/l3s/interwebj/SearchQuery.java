package de.l3s.interwebj;

import de.l3s.interwebj.jaxb.SearchResponse;
import de.l3s.interwebj.jaxb.SearchResultEntity;
import de.l3s.interwebj.jaxb.ThumbnailEntity;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.SERVICE;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.util.StringHelper;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SearchQuery implements Serializable
{
    private static final long serialVersionUID = 681547180910687848L;
    private final static Logger log = Logger.getLogger(SearchQuery.class);

    private long totalResults;
    private List<Count> serviceCount = new ArrayList<>(); // service name, number of results at this service
    private List<ResourceDecorator> results;

    public SearchQuery(InputStream inputStream) throws IllegalResponseException
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

            List<SearchResultEntity> searchResults = response.getResultItems();

            totalResults = response.getTotalResults();
            for(Map.Entry<String, Long> entry : response.getResultsPerService().entrySet())
            {
                serviceCount.add(new Count(ff, entry.getKey(), entry.getValue()));
            }

            for(SearchResultEntity searchResult : searchResults)
            {
                Resource resource = createResource(searchResult);

                if(!resource.getType().equals(Resource.ResourceType.website) && null == resource.getThumbnail2()) // no thumbnail set
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

                ResourceDecorator decoratedResource = createDecoratedResource(searchResult, resource);
                results.add(decoratedResource);
            }

        }
        catch(JAXBException e)
        {
            throw new IllegalResponseException(e);
        }
    }

    private static Resource createResource(SearchResultEntity searchResult)
    {
        Resource resource = new Resource();
        resource.setType(Resource.ResourceType.valueOf(searchResult.getType()));
        resource.setTitle(searchResult.getTitle());
        resource.setLocation(searchResult.getService());
        resource.setSource(searchResult.getService());
        //resource.setViews(searchResult.getNumberOfViews());
        resource.setIdAtService(searchResult.getId());
        resource.setDescription(searchResult.getDescription());

        if(resource.getTitle().equals(resource.getDescription()))
        { // delete description when equal to title
            resource.setDescription("");
        }

        if(searchResult.getUrl() != null)
        {
            resource.setUrl(StringHelper.urlDecode(searchResult.getUrl()));
        }

        if(searchResult.getDuration() != null)
        {
            resource.setDuration(searchResult.getDuration().intValue());
        }

        if(searchResult.getEmbeddedCode() != null)
        {
            resource.setEmbeddedRaw(searchResult.getEmbeddedCode());
        }

        setThumbnails(resource, searchResult);
        return resource;
    }

    private static void setThumbnails(Resource resource, SearchResultEntity searchResult)
    {
        if(searchResult.getThumbnailSmall() != null)
        {
            resource.setThumbnail0(createThumbnail(searchResult.getThumbnailSmall()));
        }
        else if(searchResult.getThumbnailLarge() != null)
        {
            resource.setThumbnail0(createThumbnail(searchResult.getThumbnailLarge()));
        }

        if(searchResult.getThumbnailMedium() != null)
        {
            resource.setThumbnail2(createThumbnail(searchResult.getThumbnailMedium()));
        }
        else if(searchResult.getThumbnailLarge() != null)
        {
            resource.setThumbnail2(createThumbnail(searchResult.getThumbnailLarge()));
        }

        if(searchResult.getThumbnailLarge() != null)
        {
            resource.setThumbnail4(createThumbnail(searchResult.getThumbnailLarge()));
        }
        else if(searchResult.getThumbnailOriginal() != null)
        {
            resource.setThumbnail4(createThumbnail(searchResult.getThumbnailOriginal()));
        }

        if(searchResult.getThumbnailOriginal() != null)
        {
            resource.setMaxImageUrl(searchResult.getThumbnailOriginal().getUrl());
        }
        else
        {
            Thumbnail biggestThumbnail = resource.getLargestThumbnail();
            if(biggestThumbnail != null)
            {
                resource.setMaxImageUrl(biggestThumbnail.getUrl());
            }
        }
    }

    private static Thumbnail createThumbnail(ThumbnailEntity searchThumbnail)
    {
        return new Thumbnail(searchThumbnail.getUrl(), searchThumbnail.getWidth(), searchThumbnail.getHeight());
    }

    private static ResourceDecorator createDecoratedResource(SearchResultEntity searchResult, Resource resource)
    {
        ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        decoratedResource.setRankAtService(searchResult.getRank());
        decoratedResource.setTitle(searchResult.getTitle());
        decoratedResource.setSnippet(searchResult.getSnippet());

        // bing description contains snippet with term highlighting
        if(resource.getSource() == SERVICE.bing && decoratedResource.getSnippet() == null)
        {
            // add snippet
            decoratedResource.setSnippet(resource.getDescription());
            // remove search term highlighting from description
            resource.setDescription(Jsoup.clean(resource.getDescription(), Whitelist.none()));
        }

        if(decoratedResource.getSnippet() == null && resource.getShortDescription() != null)
        {
            decoratedResource.setSnippet(resource.getShortDescription());
        }

        return decoratedResource;
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

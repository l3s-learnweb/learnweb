package de.l3s.learnweb.resource.search;

import de.l3s.interwebj.client.model.SearchResponse;
import de.l3s.interwebj.client.model.SearchResult;
import de.l3s.interwebj.client.model.SearchThumbnail;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.SERVICE;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.util.StringHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InterwebResultsWrapper implements Serializable
{
    @Serial
    private static final long serialVersionUID = 681547180910687848L;
    private static final Logger log = LogManager.getLogger(InterwebResultsWrapper.class);

    private long totalResults;
    private final List<ResourceDecorator> resources = new LinkedList<>();
    private final List<FacetField.Count> resultCountPerService = new ArrayList<>();

    public InterwebResultsWrapper(SearchResponse response) {
        if (response == null || response.getQuery() == null) {
            log.fatal("response is null");
            return;
        }

        totalResults = response.getTotalResults();

        if (response.getResultsPerService() != null && !response.getResultsPerService().isEmpty()) {
            FacetField facetField = new FacetField("source");
            for (Map.Entry<String, Long> serviceResults : response.getResultsPerService().entrySet()) {
                resultCountPerService.add(new FacetField.Count(facetField, serviceResults.getKey(), serviceResults.getValue()));
            }
        }

        List<SearchResult> searchResults = response.getResults();

        int counter = 0;
        for (SearchResult searchResult : searchResults) {
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
            resources.add(decoratedResource);
        }
    }

    private static Resource createResource(SearchResult searchResult) {
        Resource resource = new Resource();
        resource.setType(Resource.ResourceType.valueOf(searchResult.getType()));
        resource.setTitle(searchResult.getTitle());
        resource.setLocation(searchResult.getService());
        resource.setSource(searchResult.getService());
        //resource.setViews(searchResult.getNumberOfViews());
        resource.setIdAtService(searchResult.getIdAtService());
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

        if(searchResult.getEmbeddedUrl() != null)
        {
            resource.setEmbeddedRaw("<iframe src=\"" + searchResult.getEmbeddedUrl() + "\"></iframe>");
        }

        setThumbnails(resource, searchResult);
        return resource;
    }

    private static void setThumbnails(Resource resource, SearchResult searchResult) {
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

    private static Thumbnail createThumbnail(SearchThumbnail searchThumbnail)
    {
        return new Thumbnail(searchThumbnail.getUrl(), searchThumbnail.getWidth(), searchThumbnail.getHeight());
    }

    private static ResourceDecorator createDecoratedResource(SearchResult searchResult, Resource resource) {
        ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        decoratedResource.setRankAtService(searchResult.getRankAtService());
        decoratedResource.setTitle(searchResult.getTitle());
        decoratedResource.setSnippet(searchResult.getSnippet());

        // bing description contains snippet with term highlighting
        if(resource.getSource() == SERVICE.bing && decoratedResource.getSnippet() == null)
        {
            // add snippet
            decoratedResource.setSnippet(resource.getDescription());
            // remove search term highlighting from description
            resource.setDescription(Jsoup.clean(resource.getDescription(), Safelist.none()));
        }

        if(decoratedResource.getSnippet() == null && resource.getShortDescription() != null)
        {
            decoratedResource.setSnippet(resource.getShortDescription());
        }

        return decoratedResource;
    }

    public long getTotalResults() {

        return totalResults;
    }

    public List<ResourceDecorator> getResources() {
        return resources;
    }

    public List<FacetField.Count> getResultCountPerService() {
        return resultCountPerService;
    }
}

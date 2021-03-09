package de.l3s.learnweb.resource.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.interwebj.client.model.SearchResponse;
import de.l3s.interwebj.client.model.SearchResult;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.Thumbnail;
import de.l3s.util.StringHelper;

public class InterwebResultsWrapper implements Serializable {
    private static final long serialVersionUID = 681547180910687848L;
    private static final Logger log = LogManager.getLogger(InterwebResultsWrapper.class);

    private long totalResults;
    private final List<ResourceDecorator> resources = new LinkedList<>();
    private final List<Count> resultCountPerService = new ArrayList<>();

    public InterwebResultsWrapper(SearchResponse response) {
        if (response == null || response.getQuery() == null) {
            log.fatal("response is null");
            return;
        }

        totalResults = response.getTotalResults();

        if (response.getResultsPerService() != null && !response.getResultsPerService().isEmpty()) {
            FacetField facetField = new FacetField("location");
            for (Map.Entry<String, Long> serviceResults : response.getResultsPerService().entrySet()) {
                resultCountPerService.add(new Count(facetField, serviceResults.getKey(), serviceResults.getValue()));
            }
        }

        List<SearchResult> searchResults = response.getResults();

        int counter = 0;
        for (SearchResult searchResult : searchResults) {
            Resource resource = createResource(searchResult);

            if (resource.getType() != ResourceType.website && null == resource.getThumbnailMedium()) { // no thumbnail set
                log.warn("Found no thumbnail: {}", searchResult);
                counter++;

                if (counter > 5) {
                    log.error("To many missing thumbnails", new Exception());
                    return;
                }

                totalResults -= 1;
                continue;
            }

            ResourceDecorator decoratedResource = createDecoratedResource(searchResult, resource);
            resources.add(decoratedResource);
        }
    }

    private static Resource createResource(SearchResult searchResult) {
        ResourceType resourceType = "text".equals(searchResult.getType()) ? ResourceType.website : ResourceType.valueOf(searchResult.getType());
        ResourceService resourceService = ResourceService.valueOf(searchResult.getService().toLowerCase().replace("-", ""));

        Resource resource = new Resource(Resource.StorageType.WEB, resourceType, resourceService);
        resource.setTitle(searchResult.getTitle());
        // resource.setViews(searchResult.getNumberOfViews());
        resource.setIdAtService(searchResult.getIdAtService());
        resource.setAuthor(searchResult.getAuthor());

        if (!resource.getTitle().equals(resource.getDescription())) {
            resource.setDescription(searchResult.getDescription());
        }

        if (searchResult.getWidth() != null) {
            resource.setWidth(searchResult.getWidth());
        }

        if (searchResult.getHeight() != null) {
            resource.setHeight(searchResult.getHeight());
        }

        if (searchResult.getUrl() != null) {
            resource.setUrl(StringHelper.urlDecode(searchResult.getUrl()));
        }

        if (searchResult.getDuration() != null) {
            resource.setDuration(searchResult.getDuration().intValue());
        }

        if (searchResult.getEmbeddedUrl() != null) {
            resource.setEmbeddedUrl(searchResult.getEmbeddedUrl());
        }

        setThumbnails(resource, searchResult);
        return resource;
    }

    private static void setThumbnails(Resource resource, SearchResult searchResult) {
        if (searchResult.getThumbnailSmall() != null) {
            resource.setThumbnailSmall(new Thumbnail(searchResult.getThumbnailSmall().getUrl()));
        } else if (searchResult.getThumbnailLarge() != null) {
            resource.setThumbnailSmall(new Thumbnail(searchResult.getThumbnailLarge().getUrl()));
        }

        if (searchResult.getThumbnailMedium() != null) {
            resource.setThumbnailMedium(new Thumbnail(searchResult.getThumbnailMedium().getUrl()));
        } else if (searchResult.getThumbnailLarge() != null) {
            resource.setThumbnailMedium(new Thumbnail(searchResult.getThumbnailLarge().getUrl()));
        }

        if (searchResult.getThumbnailLarge() != null) {
            resource.setThumbnailLarge(new Thumbnail(searchResult.getThumbnailLarge().getUrl()));
        } else if (searchResult.getThumbnailOriginal() != null) {
            resource.setThumbnailLarge(new Thumbnail(searchResult.getThumbnailOriginal().getUrl()));
        }

        if (searchResult.getThumbnailOriginal() != null) {
            resource.setMaxImageUrl(searchResult.getThumbnailOriginal().getUrl());
        } else {
            Thumbnail biggestThumbnail = resource.getThumbnailLargest();
            if (biggestThumbnail != null) {
                resource.setMaxImageUrl(biggestThumbnail.getUrl());
            }
        }
    }

    private static ResourceDecorator createDecoratedResource(SearchResult searchResult, Resource resource) {
        ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        decoratedResource.setRank(searchResult.getRankAtService());
        decoratedResource.setTitle(searchResult.getTitle());
        decoratedResource.setSnippet(searchResult.getSnippet());
        decoratedResource.setAuthorUrl(searchResult.getAuthorUrl());

        // bing description contains snippet with term highlighting
        if (resource.getService() == ResourceService.bing && decoratedResource.getSnippet() == null) {
            // add snippet
            decoratedResource.setSnippet(resource.getDescription());
            // remove search term highlighting from description
            resource.setDescription(Jsoup.clean(resource.getDescription(), Whitelist.none()));
        }

        if (decoratedResource.getSnippet() == null && resource.getShortDescription() != null) {
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

    public List<Count> getResultCountPerService() {
        return resultCountPerService;
    }
}

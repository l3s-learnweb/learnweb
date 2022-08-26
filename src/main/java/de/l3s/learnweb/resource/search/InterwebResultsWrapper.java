package de.l3s.learnweb.resource.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import de.l3s.interwebj.client.model.SearchResponse;
import de.l3s.interwebj.client.model.SearchResult;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.web.WebResource;
import de.l3s.util.StringHelper;

public class InterwebResultsWrapper implements Serializable {
    @Serial
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
            FacetField facetField = new FacetField("source");
            for (Map.Entry<String, Long> serviceResults : response.getResultsPerService().entrySet()) {
                resultCountPerService.add(new Count(facetField, serviceResults.getKey(), serviceResults.getValue()));
            }
        }

        List<SearchResult> searchResults = response.getResults();

        int counter = 0;
        for (SearchResult searchResult : searchResults) {
            WebResource resource = createResource(searchResult);

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

    private static WebResource createResource(SearchResult searchResult) {
        ResourceType resourceType = "text".equals(searchResult.getType()) ? ResourceType.website : ResourceType.valueOf(searchResult.getType());
        ResourceService resourceService = ResourceService.parse(searchResult.getService());

        WebResource resource = new WebResource(resourceType, resourceService);
        resource.setTitle(searchResult.getTitle());
        // resource.setViews(searchResult.getNumberOfViews());
        resource.setIdAtService(searchResult.getIdAtService());
        resource.setAuthor(searchResult.getAuthor());

        if (!StringUtils.equals(resource.getTitle(), resource.getDescription())) {
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
        if (searchResult.getThumbnailSmallCombined() != null) {
            resource.setThumbnailSmall(searchResult.getThumbnailSmallCombined().getUrl());
        }

        if (searchResult.getThumbnailMediumCombined() != null) {
            resource.setThumbnailMedium(searchResult.getThumbnailMediumCombined().getUrl());
        }

        if (searchResult.getThumbnailLargeCombined() != null) {
            resource.setThumbnailLarge(searchResult.getThumbnailLargeCombined().getUrl());
        }

        if (searchResult.getThumbnailOriginalCombined() != null) {
            resource.setMaxImageUrl(searchResult.getThumbnailOriginalCombined().getUrl());
        }
    }

    private static ResourceDecorator createDecoratedResource(SearchResult searchResult, WebResource resource) {
        ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        decoratedResource.setRank(searchResult.getRankAtService());
        decoratedResource.setTitle(searchResult.getTitle());
        decoratedResource.setSnippet(searchResult.getSnippet());
        decoratedResource.setAuthorUrl(searchResult.getAuthorUrl());

        // bing description contains snippet with term highlighting
        if (resource.getService() == ResourceService.bing && decoratedResource.getSnippet() == null && decoratedResource.getDescription() != null) {
            // add snippet
            decoratedResource.setSnippet(resource.getDescription());
            // remove search term highlighting from description
            resource.setDescription(Jsoup.clean(resource.getDescription(), Safelist.none()));
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
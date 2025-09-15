package de.l3s.learnweb.resource.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;

import de.l3s.interweb.core.search.SearchConnectorResults;
import de.l3s.interweb.core.search.SearchItem;
import de.l3s.interweb.core.search.SearchResults;
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

    private long totalResults = 0;
    private final LinkedList<ResourceDecorator> resources = new LinkedList<>();
    private final ArrayList<Count> resultCountPerService = new ArrayList<>();

    public InterwebResultsWrapper(SearchResults response) {
        if (response == null || response.getResults() == null) {
            log.fatal("response is null");
            return;
        }

        List<SearchConnectorResults> searchResults = response.getResults();

        int counter = 0;
        FacetField facetField = new FacetField("source");
        for (SearchConnectorResults connectorResults : searchResults) {
            ResourceService service = ResourceService.parse(connectorResults.getService());
            totalResults += connectorResults.getTotalResults();
            resultCountPerService.add(new Count(facetField, connectorResults.getService(), connectorResults.getTotalResults()));

            for (var searchResult : connectorResults.getItems()) {
                WebResource resource = createResource(service, searchResult);

                if (resource.getType() != ResourceType.website && null == resource.getThumbnailLargest()) { // no thumbnail set
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
    }

    private static WebResource createResource(final ResourceService service, SearchItem searchItem) {
        WebResource resource = new WebResource(ResourceType.fromContentType(searchItem.getType()), service);
        resource.setTitle(searchItem.getTitle());
        resource.setIdAtService(searchItem.getId());
        resource.setAuthor(searchItem.getAuthor());

        if (!StringUtils.equals(resource.getTitle(), resource.getDescription())) {
            resource.setDescription(searchItem.getDescription());
        }

        if (searchItem.getWidth() != null) {
            resource.setWidth(searchItem.getWidth());
        }

        if (searchItem.getHeight() != null) {
            resource.setHeight(searchItem.getHeight());
        }

        if (searchItem.getUrl() != null) {
            resource.setUrl(StringHelper.urlDecode(searchItem.getUrl()));
        }

        if (searchItem.getDuration() != null) {
            resource.setDuration(searchItem.getDuration().intValue());
        }

        if (searchItem.getEmbedUrl() != null) {
            resource.setEmbeddedUrl(searchItem.getEmbedUrl());
        }

        setThumbnails(resource, searchItem);
        return resource;
    }

    private static void setThumbnails(Resource resource, SearchItem searchItem) {
        if (searchItem.getThumbnailSmall() != null) {
            resource.setThumbnailSmall(searchItem.getThumbnailSmall().getUrl());
        }

        if (searchItem.getThumbnailMedium() != null) {
            resource.setThumbnailMedium(searchItem.getThumbnailMedium().getUrl());
        }

        if (searchItem.getThumbnailLarge() != null) {
            resource.setThumbnailLarge(searchItem.getThumbnailLarge().getUrl());
        }

        if (searchItem.getThumbnailOriginal() != null) {
            resource.setMaxImageUrl(searchItem.getThumbnailOriginal().getUrl());
        }
    }

    private static ResourceDecorator createDecoratedResource(SearchItem searchItem, WebResource resource) {
        ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        decoratedResource.setRank(searchItem.getRank());
        decoratedResource.setTitle(searchItem.getTitle());
        decoratedResource.setSnippet(searchItem.getDescription());
        decoratedResource.setAuthorUrl(searchItem.getAuthorUrl());

        // TODO: investigate possibility to highlight search terms in snippet

        if (decoratedResource.getSnippet() == null && resource.getShortDescription() != null) {
            decoratedResource.setSnippet(resource.getShortDescription());
        }

        return decoratedResource;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public LinkedList<ResourceDecorator> getResources() {
        return resources;
    }

    public ArrayList<Count> getResultCountPerService() {
        return resultCountPerService;
    }
}

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

import de.l3s.interwebj.model.SearchResponse;
import de.l3s.interwebj.model.SearchResult;
import de.l3s.interwebj.model.SearchThumbnail;
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

        totalResults = response.getQuery().getTotalResults();

        if (response.getQuery().getFacetSources() != null && !response.getQuery().getFacetSources().isEmpty()) {
            FacetField facetField = new FacetField("location");
            for (Map.Entry<String, Integer> serviceResults : response.getQuery().getFacetSources().entrySet()) {
                resultCountPerService.add(new Count(facetField, serviceResults.getKey(), serviceResults.getValue()));
            }
        }

        List<SearchResult> searchResults = response.getQuery().getResults();

        int counter = 0;
        for (SearchResult searchResult : searchResults) {
            Resource resource = createResource(searchResult);

            if (resource.getType() != ResourceType.website && null == resource.getThumbnail2()) { // no thumbnail set
                log.error("Found no thumbnail: {}", searchResult);
                counter++;

                if (counter > 5) {
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
        resource.setType("text".equals(searchResult.getType()) ? ResourceType.website : ResourceType.valueOf(searchResult.getType()));
        resource.setTitle(searchResult.getTitle());
        resource.setLocation(searchResult.getService());
        resource.setSource(searchResult.getService());
        // resource.setViews(searchResult.getNumberOfViews());
        resource.setIdAtService(searchResult.getIdAtService());
        resource.setDuration(searchResult.getDuration());
        resource.setDescription(searchResult.getDescription());
        resource.setUrl(StringHelper.urlDecode(searchResult.getUrl()));

        if (resource.getTitle().equals(resource.getDescription())) { // delete description when equal to title
            resource.setDescription("");
        }

        if (resource.getSource() == ResourceService.slideshare) {
            resource.setEmbeddedRaw(searchResult.getEmbeddedSize4());
            if (null == resource.getEmbeddedRaw()) {
                resource.setEmbeddedRaw(searchResult.getEmbeddedSize3());
            }
        }

        setThumbnails(searchResult, resource);
        return resource;
    }

    private static void setThumbnails(SearchResult searchResult, Resource resource) {
        SearchThumbnail biggestThumbnail = null;
        int biggestThumbnailHeight = 0;

        List<SearchThumbnail> thumbnails = searchResult.getThumbnails();

        for (SearchThumbnail thumbnailElement : thumbnails) {
            String url = thumbnailElement.getUrl();
            int height = thumbnailElement.getHeight();
            int width = thumbnailElement.getWidth();

            if (height > biggestThumbnailHeight) {
                biggestThumbnailHeight = height;
                biggestThumbnail = thumbnailElement;
            }

            // ipernity api doesn't return largest available thumbnail, so we have to guess it
            if (resource.getSource() == ResourceService.ipernity && url.contains(".560.")) {
                if (width == 560 || height == 560) {
                    double ratio = 640.0 / 560.0;
                    width *= ratio;
                    height *= ratio;

                    url = url.replace(".560.", ".640.");
                }
            }

            Thumbnail thumbnail = new Thumbnail(url, width, height);

            if (resource.getThumbnail0() == null && thumbnail.getHeight() <= 100 && thumbnail.getWidth() <= 100) {
                resource.setThumbnail0(thumbnail);
            } else if (resource.getThumbnail1() == null && thumbnail.getHeight() <= 200 && thumbnail.getWidth() <= 200) {
                resource.setThumbnail1(thumbnail);
            } else if (resource.getThumbnail2() == null && thumbnail.getHeight() < 500 && thumbnail.getWidth() < 500) {
                resource.setThumbnail2(thumbnail.resize(300, 220));
            } else { // if (thumbnail.getHeight() < 600 && thumbnail.getWidth() < 600)
                resource.setThumbnail4(thumbnail);
            }
        }

        // remove old bing images first
        if (biggestThumbnail != null) {
            resource.setMaxImageUrl(biggestThumbnail.getUrl());

            if (resource.getThumbnail2() == null) {
                resource.setThumbnail2(new Thumbnail(biggestThumbnail.getUrl(), biggestThumbnail.getWidth(), biggestThumbnail.getHeight()));
            }
        } else if (resource.getType() != ResourceType.website) {
            log.warn("no image url for: {}", searchResult);
        }
    }

    private static ResourceDecorator createDecoratedResource(SearchResult searchResult, Resource resource) {
        ResourceDecorator decoratedResource = new ResourceDecorator(resource);
        decoratedResource.setTitle(searchResult.getTitle());
        decoratedResource.setSnippet(searchResult.getSnippet());

        // bing description contains snippet with term highlighting
        if (resource.getSource() == ResourceService.bing && decoratedResource.getSnippet() == null) {
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

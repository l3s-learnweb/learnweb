package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;

import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.ResourceDecorator;

public class SolrPaginator extends AbstractPaginator {
    @Serial
    private static final long serialVersionUID = 3823389610985272265L;

    private final SolrSearch search;

    private transient List<FacetField> facetFieldsResults;
    private transient Map<String, Integer> facetQueriesResults;

    public SolrPaginator(SolrSearch search) {
        super(search.getResultsPerPage());

        this.search = search;
    }

    @Override
    public synchronized List<ResourceDecorator> getCurrentPage() throws IOException {
        if (getCurrentPageCache() != null) {
            return getCurrentPageCache();
        }

        List<ResourceDecorator> results = search.getResourcesByPage(getPageIndex() + 1);
        setTotalResults((int) search.getResultsFound());
        facetFieldsResults = search.getResultsFacetFields();
        facetQueriesResults = search.getResultsFacetQuery();

        setCurrentPageCache(results);
        return results;
    }

    public List<FacetField> getFacetFields() throws IOException {
        if (facetFieldsResults == null) {
            getCurrentPage();
        }

        return facetFieldsResults;
    }

    public Map<String, Integer> getFacetQueries() throws IOException {
        if (facetQueriesResults == null) {
            getCurrentPage();
        }

        return facetQueriesResults;
    }
}

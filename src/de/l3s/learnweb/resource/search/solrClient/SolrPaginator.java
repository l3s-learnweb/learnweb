package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.ResourceDecorator;

public class SolrPaginator extends AbstractPaginator {
    private static final long serialVersionUID = 3823389610985272265L;

    private final SolrSearch search;
    private int searchLogId;

    private List<FacetField> facetFieldsResults;
    private Map<String, Integer> facetQueriesResults;

    public SolrPaginator(SolrSearch search) {
        super(search.getResultsPerPage());

        this.search = search;

        if (search.getFilterGroupIds().size() == 1 && search.getQuery() != null && !"*".equals(search.getQuery())) {
            this.searchLogId = Learnweb.dao().getSearchHistoryDao().insertGroupQuery(
                search.getFilterGroupIds().get(0),
                search.getQuery(),
                null,
                search.getFilterLanguage(),
                search.getUserId()
            );
        }
    }

    @Override
    public synchronized List<ResourceDecorator> getCurrentPage() throws IOException, SolrServerException {
        if (getCurrentPageCache() != null) {
            return getCurrentPageCache();
        }

        List<ResourceDecorator> results = search.getResourcesByPage(getPageIndex() + 1);
        setTotalResults((int) search.getResultsFound());
        facetFieldsResults = search.getResultsFacetFields();
        facetQueriesResults = search.getResultsFacetQuery();

        setCurrentPageCache(results);
        if (searchLogId != 0) {
            Learnweb.dao().getSearchHistoryDao().insertResources(searchLogId, results);
        }
        return results;
    }

    public List<FacetField> getFacetFields() throws IOException, SolrServerException {
        if (facetFieldsResults == null) {
            getCurrentPage();
        }

        return facetFieldsResults;
    }

    public Map<String, Integer> getFacetQueries() throws IOException, SolrServerException {
        if (facetQueriesResults == null) {
            getCurrentPage();
        }

        return facetQueriesResults;
    }
}

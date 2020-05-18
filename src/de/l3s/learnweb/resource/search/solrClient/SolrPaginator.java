package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.ResourceDecorator;

public class SolrPaginator extends AbstractPaginator {
    private static final long serialVersionUID = 3823389610985272265L;

    private final SolrSearch search;
    private int searchLogId = -1;

    private List<FacetField> facetFieldsResults;
    private Map<String, Integer> facetQueriesResults;

    public SolrPaginator(SolrSearch search) {
        super(search.getResultsPerPage());

        this.search = search;

        if (search.getFilterGroupIds().size() == 1 && search.getQuery() != null && !"*".equals(search.getQuery())) {
            this.searchLogId = Learnweb.getInstance().getSearchLogManager().logGroupQuery(
                search.getFilterGroupIds().get(0),
                search.getQuery(),
                null,
                search.getFilterLanguage(),
                search.getUserId()
            );
        }
    }

    @Override
    public synchronized List<ResourceDecorator> getCurrentPage() throws SQLException, IOException, SolrServerException {
        if (getCurrentPageCache() != null) {
            return getCurrentPageCache();
        }

        List<ResourceDecorator> results = search.getResourcesByPage(getPageIndex() + 1);
        setTotalResults((int) search.getQueryResponse().getResults().getNumFound());
        facetFieldsResults = search.getQueryResponse().getFacetFields();
        facetQueriesResults = search.getQueryResponse().getFacetQuery();

        setCurrentPageCache(results);
        if (searchLogId > 0) {
            Learnweb.getInstance().getSearchLogManager().logResources(searchLogId, results, getPageIndex() + 1);
        }
        return results;
    }

    public List<FacetField> getFacetFields() throws SQLException, IOException, SolrServerException {
        if (facetFieldsResults == null) {
            getCurrentPage();
        }

        return facetFieldsResults;
    }

    public Map<String, Integer> getFacetQueries() throws SQLException, IOException, SolrServerException {
        if (facetQueriesResults == null) {
            getCurrentPage();
        }

        return facetQueriesResults;
    }
}

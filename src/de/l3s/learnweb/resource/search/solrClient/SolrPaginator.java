package de.l3s.learnweb.resource.search.solrClient;

import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.ResourceDecorator;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SolrPaginator extends AbstractPaginator
{
    private static final long serialVersionUID = 3823389610985272265L;

    private final SolrSearch search;

    private List<FacetField> facetFieldsResults;
    private Map<String, Integer> facetQueriesResults;

    public SolrPaginator(SolrSearch search)
    {
        super(search.getResultsPerPage());

        this.search = search;
    }

    @Override
    public synchronized List<ResourceDecorator> getCurrentPage() throws SQLException, IOException, SolrServerException
    {
        if(getCurrentPageCache() != null) return getCurrentPageCache();

        List<ResourceDecorator> results = search.getResourcesByPage(getPageIndex() + 1);
        setTotalResults((int) search.getTotalResultCount());
        facetFieldsResults = search.getFacetFields();
        facetQueriesResults = search.getFacetQueries();

        setCurrentPageCache(results);
        return results;
    }

    public List<FacetField> getFacetFields() throws SQLException, IOException, SolrServerException
    {
        if(facetFieldsResults == null)
        {
            getCurrentPage();
        }

        return facetFieldsResults;
    }

    public Map<String, Integer> getFacetQueries() throws SQLException, IOException, SolrServerException
    {
        if(facetQueriesResults == null)
        {
            getCurrentPage();
        }

        return facetQueriesResults;
    }
}

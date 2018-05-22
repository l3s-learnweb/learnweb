package de.l3s.learnweb.beans;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.archiveSearch.OpenSearchClient;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.search.solrClient.SolrSearch;

@ManagedBean
@ViewScoped
public class ArchiveitSearchBean
{
    private String query;
    private String index;
    private String types;
    private String sites;
    private List<ResourceDecorator> resources;
    private List<ResourceDecorator> metadataIndexResources;
    private int archiveItResultCount;
    private long metadataIndexResultCount;

    public ArchiveitSearchBean()
    {

    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getIndex()
    {
        return index;
    }

    public void setIndex(String index)
    {
        this.index = index;
    }

    public String getTypes()
    {
        return types;
    }

    public void setTypes(String types)
    {
        this.types = types;
    }

    public String getSites()
    {
        return sites;
    }

    public void setSites(String sites)
    {
        this.sites = sites;
    }

    public void onSearch() throws SQLException, IOException, SolrServerException
    {
        OpenSearchClient openSearchClient = new OpenSearchClient(query, Integer.parseInt(index), types, sites);
        resources = openSearchClient.getResults();
        archiveItResultCount = openSearchClient.getTotalResultsCount();
        getResourcesFromSolr();
    }

    public void getResourcesFromSolr() throws SQLException, IOException, SolrServerException
    {
        SolrSearch solrSearch = new SolrSearch(StringUtils.isEmpty(query) ? "*" : query, null);
        solrSearch.setFilterGroups(937);
        solrSearch.setResultsPerPage(50);
        solrSearch.setSort("timestamp DESC");
        metadataIndexResources = solrSearch.getResourcesByPage(1);
        metadataIndexResultCount = solrSearch.getTotalResultCount();
    }

    public List<ResourceDecorator> getResources()
    {
        return resources;
    }

    public int getArchiveItResultCount()
    {
        return archiveItResultCount;
    }

    public List<ResourceDecorator> getMetadataIndexResources()
    {
        return metadataIndexResources;
    }

    public long getMetadataIndexResultCount()
    {
        return metadataIndexResultCount;
    }
}

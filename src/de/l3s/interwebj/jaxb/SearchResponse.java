package de.l3s.interwebj.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "rsp")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResponse
{
    @XmlElement(name = "query")
    protected SearchQueryEntity query;

    @XmlElement(name = "total_results")
    private Long totalResults;
    @XmlElementWrapper(name = "results_per_service")
    @XmlElement(name = "count")
    private Map<String, Long> resultsPerService;

    @XmlElementWrapper(name = "results")
    @XmlElement(name = "result")
    private List<SearchResultEntity> resultItems;

    @XmlElement(name = "created_time")
    private long createdTime;
    @XmlElement(name = "elapsed_time")
    private long elapsedTime;

    public SearchResponse()
    {
    }

    public SearchQueryEntity getQuery()
    {
        return query;
    }

    public void setQuery(SearchQueryEntity query)
    {
        this.query = query;
    }

    public Long getTotalResults()
    {
        return totalResults;
    }

    public void setTotalResults(final Long totalResults)
    {
        this.totalResults = totalResults;
    }

    public Map<String, Long> getResultsPerService()
    {
        return resultsPerService;
    }

    public void setResultsPerService(final Map<String, Long> resultsPerService)
    {
        this.resultsPerService = resultsPerService;
    }

    public List<SearchResultEntity> getResultItems()
    {
        return resultItems;
    }

    public void setResultItems(final List<SearchResultEntity> resultItems)
    {
        this.resultItems = resultItems;
    }

    public long getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime(final long createdTime)
    {
        this.createdTime = createdTime;
    }

    public long getElapsedTime()
    {
        return elapsedTime;
    }

    public void setElapsedTime(final long elapsedTime)
    {
        this.elapsedTime = elapsedTime;
    }
}

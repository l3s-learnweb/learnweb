package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

public abstract class AbstractPaginator implements Serializable
{
    private static final long serialVersionUID = 2495539559727294482L;
    private static final int DEFAULT_PAGE_INDEX = 0;
    private static final int DEFAULT_N_PAGE_LIMIT = 5;
    public static final int PAGE_SIZE = Learnweb.getInstance().getProperties().getPropertyIntValue("RESOURCES_PAGE_SIZE");

    private int pageIndex = DEFAULT_PAGE_INDEX;
    private int totalPages;
    private int totalResults = Integer.MIN_VALUE;

    public abstract List<ResourceDecorator> getCurrentPage() throws SQLException, SolrServerException;

    public AbstractPaginator(int totalResults)
    {
	setTotalResults(totalResults);
    }

    /**
     * Class which use this constructor must call setTotalResults(int totalResults) as soon as possible
     */
    public AbstractPaginator()
    {
	setTotalResults(totalResults);
    }

    public int getTotalResults()
    {
	return totalResults;
    }

    protected void setTotalResults(int totalResults)
    {
	this.totalResults = totalResults;
	this.totalPages = (totalResults + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    /**
     * Starting from 0
     * 
     * @return
     */
    public int getPageIndex()
    {
	return pageIndex;
    }

    public void setPageIndex(int pageIndex)
    {
	this.pageIndex = pageIndex;
    }

    public int getTotalPages()
    {
	return totalPages;
    }

    public void setTotalPages(int totalPages)
    {
	this.totalPages = totalPages;
    }

    public List<Integer> listPages()
    {
	List<Integer> pages = new LinkedList<Integer>();
	int fromIndex = 0, endIndex = 0;
	if(pageIndex == 0)
	{
	    fromIndex = pageIndex + 1;
	    endIndex = fromIndex + 3;
	}
	else if(pageIndex >= 1 && pageIndex < 3)
	{
	    fromIndex = 1;
	    endIndex = pageIndex + 4;
	}
	else if(pageIndex >= 3 && pageIndex < 4)
	{
	    fromIndex = 1;
	    endIndex = pageIndex + 3;
	}
	else if(pageIndex >= 4 && pageIndex < totalPages - 3)
	{
	    fromIndex = pageIndex - 1;
	    endIndex = fromIndex + 4;
	}
	else if(pageIndex == totalPages - 3)
	{
	    fromIndex = pageIndex - 1;
	    endIndex = totalPages;
	}
	else if(pageIndex >= totalPages - 2 && pageIndex < totalPages)
	{
	    fromIndex = totalPages - 3;
	    endIndex = totalPages;
	}
	else if(pageIndex == totalPages)
	{
	    fromIndex = pageIndex - 3;
	    endIndex = totalPages;
	}

	if(totalPages <= 5)
	    for(int i = 0; i < totalPages; i++)
		pages.add(i + 1);
	else
	    for(int i = fromIndex; i < endIndex - 1; i++)
		pages.add(i + 1);

	return pages;
    }

    public boolean isLessThanNPages()
    {
	if(totalPages <= DEFAULT_N_PAGE_LIMIT)
	    return true;
	else
	    return false;
    }

    public boolean isLessThanNPagesFromLast()
    {
	if(pageIndex < totalPages - 3)
	    return true;
	else
	    return false;
    }

    public boolean isEmpty()
    {
	if(totalResults == Integer.MIN_VALUE)
	    throw new IllegalStateException("Call getPageIndex() first");
	if(totalResults == 0)
	    return true;
	return false;
    }
}

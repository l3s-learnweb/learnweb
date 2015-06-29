package de.l3s.learnweb;

import java.util.LinkedList;
import java.util.List;

public class Paginator
{
    private static final int DEFAULT_PAGE_INDEX = 0;
    private static final int DEFAULT_N_PAGE_LIMIT = 5;
    private int pageIndex;
    private int totalPages;

    public Paginator(int totalPages)
    {
	pageIndex = DEFAULT_PAGE_INDEX;
	this.totalPages = totalPages;
    }

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
	    endIndex = fromIndex + 2;
	}
	else if(pageIndex >= 1 && pageIndex < 3)
	{
	    fromIndex = 1;
	    endIndex = pageIndex + 3;
	}
	else if(pageIndex >= 3 && pageIndex < 4)
	{
	    fromIndex = 1;
	    endIndex = pageIndex + 2;
	}
	else if(pageIndex >= 4 && pageIndex < totalPages - 3)
	{
	    fromIndex = pageIndex - 1;
	    endIndex = fromIndex + 3;
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

	if(totalPages < 5)
	    for(int i = 0; i < totalPages + 1; i++)
		pages.add(i + 1);
	else
	    for(int i = fromIndex; i < endIndex; i++)
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

}

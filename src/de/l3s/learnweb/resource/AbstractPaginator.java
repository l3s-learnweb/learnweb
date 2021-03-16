package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

public abstract class AbstractPaginator implements Serializable {
    private static final long serialVersionUID = 2495539559727294482L;
    private static final int DEFAULT_PAGE_INDEX = 0;
    private static final int DEFAULT_N_PAGE_LIMIT = 5;

    private final int pageSize;
    private int pageIndex = DEFAULT_PAGE_INDEX;
    private int totalPages;
    private int totalResults = Integer.MIN_VALUE;

    private final transient LinkedHashMap<Integer, List<ResourceDecorator>> pageCache = new LinkedHashMap<>();

    /**
     * Classes which use this constructor must call setTotalResults(int totalResults) as soon as possible.
     */
    public AbstractPaginator(int pageSize) {
        this.pageSize = pageSize;
    }

    public abstract List<ResourceDecorator> getCurrentPage() throws IOException, SolrServerException;

    public int getTotalResults() {
        return totalResults;
    }

    protected void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
        this.totalPages = (totalResults + pageSize - 1) / pageSize;
    }

    /**
     * Starting from 0.
     */
    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    protected List<ResourceDecorator> getCurrentPageCache() {
        return pageCache.get(pageIndex);
    }

    protected void setCurrentPageCache(List<ResourceDecorator> currentPageCache) {
        this.pageCache.put(pageIndex, currentPageCache);
    }

    public List<Integer> getPageList() {
        List<Integer> pages = new LinkedList<>();
        int fromIndex = 0;
        int endIndex = 0;
        if (pageIndex == 0) {
            fromIndex = pageIndex + 1;
            endIndex = fromIndex + 3;
        } else if (pageIndex >= 1 && pageIndex < 3) {
            fromIndex = 1;
            endIndex = pageIndex + 4;
        } else if (pageIndex == 3) {
            fromIndex = 1;
            endIndex = pageIndex + 3;
        } else if (pageIndex >= 4 && pageIndex < totalPages - 3) {
            fromIndex = pageIndex - 1;
            endIndex = fromIndex + 4;
        } else if (pageIndex == totalPages - 3) {
            fromIndex = pageIndex - 1;
            endIndex = totalPages;
        } else if (pageIndex >= totalPages - 2 && pageIndex < totalPages) {
            fromIndex = totalPages - 3;
            endIndex = totalPages;
        } else if (pageIndex == totalPages) {
            fromIndex = pageIndex - 3;
            endIndex = totalPages;
        }

        if (totalPages <= 5) {
            for (int i = 0; i < totalPages; i++) {
                pages.add(i + 1);
            }
        } else {
            for (int i = fromIndex; i < endIndex - 1; i++) {
                pages.add(i + 1);
            }
        }

        return pages;
    }

    public boolean isLessThanNPages() {
        return totalPages <= DEFAULT_N_PAGE_LIMIT;
    }

    public boolean isLessThanNPagesFromLast() {
        return pageIndex < totalPages - 3;
    }

    public boolean isEmpty() {
        if (totalResults == Integer.MIN_VALUE) {
            throw new IllegalStateException("Call getPageIndex() first");
        }
        if (totalResults == 0) {
            return true;
        }
        return false;
    }
}

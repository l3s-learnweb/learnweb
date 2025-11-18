package de.l3s.learnweb.searchhistory;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActivityEvent;
import de.l3s.learnweb.resource.search.Search;

/**
 * Keeps only the search id, so the search with its results isn't held until the event is observed.
 */
public class SearchEvent extends ActivityEvent {
    private int rank;
    private int resourceId;

    public SearchEvent(Action action, Search search) {
        super(action, 0, search.getId()); // search actions don't declare a target type, but the search id has always been logged
    }

    public int getSearchId() {
        return getTargetId();
    }

    /**
     * The rank of the clicked or saved search result.
     */
    public int getRank() {
        return rank;
    }

    public SearchEvent setRank(int rank) {
        this.rank = rank;
        return this;
    }

    /**
     * The id of the resource a search result was saved as.
     */
    public int getResourceId() {
        return resourceId;
    }

    public SearchEvent setResourceId(int resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + "[rank=" + rank + ", resourceId=" + resourceId + "]";
    }
}

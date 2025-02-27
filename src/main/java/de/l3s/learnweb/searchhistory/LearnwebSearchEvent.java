package de.l3s.learnweb.searchhistory;

import java.io.Serial;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LearnwebEvent;
import de.l3s.learnweb.resource.search.Search;

public class LearnwebSearchEvent extends LearnwebEvent {
    @Serial
    private static final long serialVersionUID = 3514167448712540353L;

    private final Search search;

    public LearnwebSearchEvent(Action action, Search search) {
        super(action);
        this.search = search;
    }

    public Search getSearch() {
        return search;
    }
}

package de.l3s.learnweb.searchhistory;

import java.io.Serial;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.logging.LearnwebEvent;
import de.l3s.learnweb.logging.LearnwebEventListener;
import de.l3s.learnweb.user.User;

@ApplicationScoped
public class SearchHistoryListener implements LearnwebEventListener {
    @Serial
    private static final long serialVersionUID = -8073175169591967421L;
    private static final Logger log = LogManager.getLogger(SearchHistoryListener.class);

    @Inject
    private SearchHistoryDao searchHistoryDao;

    @Override
    public void onEvent(final LearnwebEvent event, final User user, final String sessionId) {

    }

    @Override
    public boolean isInterestedIn(final LearnwebEvent event) {
        return event instanceof LearnwebSearchEvent;
    }
}

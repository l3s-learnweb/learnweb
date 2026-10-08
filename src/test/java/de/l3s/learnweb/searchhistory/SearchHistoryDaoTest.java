package de.l3s.learnweb.searchhistory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.searchhistory.SearchHistoryDao.SearchAction;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.test.LearnwebExtension;

class SearchHistoryDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final SearchHistoryDao searchHistoryDao = learnwebExt.attach(SearchHistoryDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);
    private final ResourceDao resourceDao = learnwebExt.attach(ResourceDao.class);

    @Test
    void findSessionsByUserId() {
        List<SearchSession> sessions = searchHistoryDao.findSessionsByUserId(2, null, null);
        assertEquals(1, sessions.size());
        assertEquals("B7D0E4C1A2F3B4C5D6E7F8A9B0C1D2E3", sessions.getFirst().getSessionId());
        assertEquals(List.of(2), sessions.getFirst().getQueries().stream().map(SearchHistoryQuery::searchId).toList());
    }

    @Test
    void findSessionsByUserIdIgnoresQueriesWithoutSession() {
        assertTrue(searchHistoryDao.findSessionsByUserId(1, null, null).isEmpty()); // search 1 has no session
    }

    @Test
    void findSessionsByUserIdFiltersByQuery() {
        assertEquals(1, searchHistoryDao.findSessionsByUserId(2, "SPACE", null).size()); // case-insensitive, part of the query
        assertTrue(searchHistoryDao.findSessionsByUserId(2, "whales", null).isEmpty());
        assertTrue(searchHistoryDao.findSessionsByUserId(2, "space%", null).isEmpty()); // wildcards match literally
    }

    @Test
    void findSessionsByGroupIdFiltersByUsername() {
        String username = userDao.findByIdOrElseThrow(4).getUsername();
        assertEquals(1, searchHistoryDao.findSessionsByGroupId(1, null, username.substring(1).toUpperCase()).size());
        assertTrue(searchHistoryDao.findSessionsByGroupId(1, null, username + "_").isEmpty());
    }

    @Test
    void findSessionsByGroupId() {
        List<SearchSession> sessions = searchHistoryDao.findSessionsByGroupId(1, null, null);
        assertEquals(1, sessions.size());
        assertEquals(4, sessions.getFirst().getUserId());
        assertEquals("C8E1F5D2B3A4C5D6E7F8A9B0C1D2E3F4", sessions.getFirst().getSessionId());
    }

    @Test
    void insertQuery() {
        User user = userDao.findByIdOrElseThrow(1);
        int searchId = searchHistoryDao.insertQuery("whales", SearchMode.text, ResourceService.google, "en", null, user, "D9F2A6E3C4B5A6D7E8F9A0B1C2D3E4F5");
        assertTrue(searchId > 0);

        List<SearchSession> sessions = searchHistoryDao.findSessionsByUserId(1, null, null);
        assertEquals(1, sessions.size());
        assertEquals("D9F2A6E3C4B5A6D7E8F9A0B1C2D3E4F5", sessions.getFirst().getSessionId());
        assertEquals(List.of(searchId), sessions.getFirst().getQueries().stream().map(SearchHistoryQuery::searchId).toList());
        assertEquals("whales", sessions.getFirst().getQueries().getFirst().query());
    }

    @Test
    void findSessionsIgnoresQueriesOfOtherUsersInSession() {
        // the session id is kept on login and when a moderator logs in as another user
        searchHistoryDao.insertQuery("anonymous", SearchMode.text, ResourceService.google, "en", null, null, "E1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6");
        int searchId = searchHistoryDao.insertQuery("whales", SearchMode.text, ResourceService.google, "en", null,
            userDao.findByIdOrElseThrow(1), "E1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6");

        List<SearchSession> sessions = searchHistoryDao.findSessionsByUserId(1, null, null);
        assertEquals(List.of(searchId), sessions.getFirst().getQueries().stream().map(SearchHistoryQuery::searchId).toList());
    }

    @Test
    void insertAction() {
        searchHistoryDao.insertAction(3, 2, SearchAction.resource_saved);
        searchHistoryDao.insertAction(3, 2, SearchAction.resource_saved); // a repeated action is ignored

        int count = searchHistoryDao.withHandle(handle -> handle.select("SELECT COUNT(*) FROM lw_search_history_action WHERE search_id = 3 AND `rank` = 2")
            .mapTo(Integer.class).one());
        assertEquals(1, count);
    }

    @Test
    void findQueriesByUserIdIncludesQueriesWithoutSession() {
        assertEquals(List.of(1), searchHistoryDao.findQueriesByUserId(1, 30, 0).stream().map(SearchHistoryQuery::searchId).toList());
    }

    @Test
    void findQueriesByUserIdPagesTheQueries() {
        assertEquals(1, searchHistoryDao.countQueriesByUserId(1));
        assertTrue(searchHistoryDao.findQueriesByUserId(1, 30, 1).isEmpty());
        assertTrue(searchHistoryDao.findQueriesByUserId(1, 0, 0).isEmpty());
    }

    @Test
    void findSearchResultsByQueryCountsActionsSeparately() {
        SearchHistoryQuery query = searchHistoryDao.findQueriesByUserId(1, 30, 0).getFirst();
        List<ResourceDecorator> results = searchHistoryDao.findSearchResultsByQuery(query, 100);

        ResourceDecorator onlyClicked = results.stream().filter(r -> r.getRank() == 1).findFirst().orElseThrow();
        assertTrue(onlyClicked.getClicked());
        assertFalse(onlyClicked.getSaved());

        ResourceDecorator clickedAndSaved = results.stream().filter(r -> r.getRank() == 2).findFirst().orElseThrow();
        assertTrue(clickedAndSaved.getClicked());
        assertTrue(clickedAndSaved.getSaved());
    }

    @Test
    void findSearchResultsByQueryLoadsStoredResources() {
        Resource resource = resourceDao.findByIdOrElseThrow(1);
        ResourceDecorator decorator = new ResourceDecorator(resource);
        decorator.setRank(50);
        searchHistoryDao.insertResources(2, List.of(SearchHistoryResult.of(decorator)));

        SearchHistoryQuery query = searchHistoryDao.findQueriesByUserId(2, 30, 0).getFirst();
        ResourceDecorator result = searchHistoryDao.findSearchResultsByQuery(query, 100).stream().filter(r -> r.getRank() == 50).findFirst().orElseThrow();
        assertEquals(1, result.getResource().getId());
    }

    @Test
    void findSearchResultsByQuery() {
        SearchHistoryQuery query = searchHistoryDao.findQueriesByUserId(2, 30, 0).getFirst();
        List<ResourceDecorator> results = searchHistoryDao.findSearchResultsByQuery(query, 100);
        assertFalse(results.isEmpty());
        assertEquals(1, results.getFirst().getRank());
    }
}

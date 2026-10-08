package de.l3s.learnweb.searchhistory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.resource.web.WebResource;
import de.l3s.learnweb.searchhistory.SearchHistoryDao.SearchAction;

@ExtendWith(MockitoExtension.class)
class SearchHistoryRecorderTest {
    @Mock
    private SearchHistoryDao searchHistoryDao;
    @Mock
    private ConfigProvider config;
    @InjectMocks
    private SearchHistoryRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder.init();
    }

    @AfterEach
    void tearDown() {
        recorder.shutdown();
    }

    @Test
    void recordsQuery() {
        when(config.isCollectSearchHistory()).thenReturn(true);
        when(searchHistoryDao.insertQuery("whales", SearchMode.text, ResourceService.google, "en", null, null, "A1B2C3")).thenReturn(7);

        assertEquals(7, recorder.recordQuery("whales", SearchMode.text, ResourceService.google, "en", null, null, "A1B2C3"));
    }

    @Test
    void ignoresQueryIfHistoryDisabled() {
        when(config.isCollectSearchHistory()).thenReturn(false);

        assertEquals(0, recorder.recordQuery("whales", SearchMode.text, ResourceService.google, "en", null, null, "A1B2C3"));
        verifyNoInteractions(searchHistoryDao);
    }

    @Test
    void queryFailureDoesNotBreakSearch() {
        when(config.isCollectSearchHistory()).thenReturn(true);
        when(searchHistoryDao.insertQuery(any(), any(), any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("database down"));

        assertEquals(0, recorder.recordQuery("whales", SearchMode.text, ResourceService.google, "en", null, null, "A1B2C3"));
    }

    @Test
    void recordsResultsAsLoaded() {
        WebResource resource = new WebResource();
        resource.setUrl("https://example.org/whales");
        resource.setTitle("Whales");
        ResourceDecorator result = new ResourceDecorator(resource);
        result.setRank(1);

        recorder.recordResults(7, List.of(result));
        resource.setTitle("Saved whales"); // e.g. the user adds it to a group while it is stored
        recorder.shutdown();

        verify(searchHistoryDao).insertResources(7, List.of(new SearchHistoryResult(1, 0, "https://example.org/whales", "Whales",
            result.getDescription(), result.getThumbnailMedium(), result.getHeight(), result.getWidth())));
    }

    @Test
    void recordsActionsInOrder() {
        recorder.recordAction(7, 3, SearchAction.resource_clicked);
        recorder.recordAction(7, 3, SearchAction.resource_saved);
        recorder.shutdown();

        InOrder inOrder = inOrder(searchHistoryDao);
        inOrder.verify(searchHistoryDao).insertAction(7, 3, SearchAction.resource_clicked);
        inOrder.verify(searchHistoryDao).insertAction(7, 3, SearchAction.resource_saved);
    }

    @Test
    void ignoresUnstoredSearch() {
        recorder.recordResults(0, List.of(mock(ResourceDecorator.class)));
        recorder.recordAction(0, 3, SearchAction.resource_clicked);
        recorder.shutdown();

        verifyNoInteractions(searchHistoryDao);
    }
}

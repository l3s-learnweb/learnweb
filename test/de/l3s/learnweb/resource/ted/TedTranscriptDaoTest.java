package de.l3s.learnweb.resource.ted;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class TedTranscriptDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final TedTranscriptDao tedTranscriptDao = learnwebExt.attach(TedTranscriptDao.class);

    @Test
    void findResourceIdByTedId() {
        Optional<Integer> resourceId = tedTranscriptDao.findResourceIdByTedId(1);
        assertTrue(resourceId.isPresent());
        assertEquals(73155, resourceId.get());
    }

    @Test
    void findAllTedVideos() {
        List<TedVideo> tedVideos = tedTranscriptDao.findAllTedVideos();
        assertFalse(tedVideos.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, tedVideos.stream().map(TedVideo::getTedId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findResourceIdBySlug() {
        Optional<Integer> resourceId = tedTranscriptDao.findResourceIdBySlug("al_gore_averting_the_climate_crisis");
        assertTrue(resourceId.isPresent());
        assertEquals(73155, resourceId.get());
    }

    @Test
    void findResourceIdByTedXUrl() {
        Optional<Integer> resourceId = tedTranscriptDao.findResourceIdByTedXUrl("https://www.youtube.com/watch?v=U7nJBFjKqAY");
        assertTrue(resourceId.isEmpty());
    }

    @Disabled("Missing test data")
    @Test
    void findTranscriptLogsByUserIds() {
    }

    @Disabled("Missing test data")
    @Test
    void findSimpleTranscriptLogs() {
    }

    @Test
    void deleteTranscriptParagraphs() {
        List<Transcript.Paragraph> transcriptsParagraphs = tedTranscriptDao.findTranscriptsParagraphs(73155,  "de");
        assertFalse(transcriptsParagraphs.isEmpty());
        tedTranscriptDao.deleteTranscriptParagraphs(73155);

        transcriptsParagraphs = tedTranscriptDao.findTranscriptsParagraphs(73155, "de");
        assertTrue(transcriptsParagraphs.isEmpty());
    }

    @Disabled("Missing test data")
    @Test
    void findTranscriptSummariesByUserIds() {
    }

    @Test
    void findLanguagesByResourceId() {
        List<String> languages = tedTranscriptDao.findLanguagesByResourceId(73155);
        assertFalse(languages.isEmpty());
        assertArrayEquals(new String[] {"de"}, languages.toArray());
    }

    @Test
    void findLanguages() {
        Map<String, String> languages = tedTranscriptDao.findLanguages(73155);
        assertFalse(languages.isEmpty());
        assertArrayEquals(new String[] {"de"}, languages.values().toArray(new String[0]));
    }

    @Disabled("Missing test data")
    @Test
    void findTranscriptSummariesByResourceId() {
    }

    @Test
    void findTranscriptsParagraphs() {
        List<Transcript.Paragraph> paragraphs = tedTranscriptDao.findTranscriptsParagraphs(73155, "de");
        assertEquals(48, paragraphs.size());
    }

    @Test
    void findTranscriptsByResourceId() {
        List<Transcript> transcripts = tedTranscriptDao.findTranscriptsByResourceId(73155);
        assertFalse(transcripts.isEmpty());
    }

    @Test
    void updateResourceIdByTedId() {
        Optional<Integer> resourceBefore = tedTranscriptDao.findResourceIdByTedId(1);
        assertTrue(resourceBefore.isPresent());
        tedTranscriptDao.updateResourceIdByTedId(70000, 1);

        Optional<Integer> resourceAfter = tedTranscriptDao.findResourceIdByTedId(1);
        assertTrue(resourceAfter.isPresent());
        assertNotEquals(resourceAfter.get(), resourceBefore.get());
    }

    @Test
    void updateTedVideo() {
        Optional<TedVideo> tedVideoBefore = tedTranscriptDao.findTedVideoByResourceId(73164);
        assertTrue(tedVideoBefore.isPresent());
        tedTranscriptDao.updateTedVideo("Test", "Only test sentence", "test_1", 73164);

        Optional<TedVideo> tedVideoAfter = tedTranscriptDao.findTedVideoByResourceId(73164);
        assertTrue(tedVideoAfter.isPresent());
        assertNotEquals(tedVideoBefore.get().getTitle(), tedVideoAfter.get().getTitle());
    }

    @Disabled("Missing test data")
    @Test
    void saveTranscriptSummary() {
    }


    @Test
    void saveTranscriptParagraphs() {
        tedTranscriptDao.saveTranscriptParagraphs(74000, "en", 70000, "ABC Test.");

        List<Transcript.Paragraph> retrieved = tedTranscriptDao.findTranscriptsParagraphs(74000, "en");
        assertFalse(retrieved.isEmpty());
        assertArrayEquals(new Integer[] {70000},  retrieved.stream().map(Transcript.Paragraph::startTime).sorted().toArray(Integer[]::new));
    }

    @Test
    void saveTranscriptLangMapping() {
        tedTranscriptDao.saveTranscriptLangMapping("vi", "Vietnamese");
        tedTranscriptDao.saveTranscriptParagraphs(73164, "vi", 14820, "Test Learnweb");

        List<String> languages = tedTranscriptDao.findLanguagesByResourceId(73164);
        assertFalse(languages.isEmpty());
        assertArrayEquals(new String[] {"vi"}, languages.toArray());
    }

    @Disabled("Missing test data")
    @Test
    void saveTranscriptLog() {
    }

    @Disabled("Missing test data")
    @Test
    void saveTranscriptSelection() {
    }

    @Test
    void saveTedVideo() {
        TedVideo tedVideo = new TedVideo();
        tedVideo.setTedId(11);
        tedVideo.setResourceId(74000);
        tedVideo.setTitle("Test");
        tedVideo.setDescription("Learnweb test");
        tedVideo.setSlug("test_learnweb");
        tedVideo.setTags("education");
        tedTranscriptDao.saveTedVideo(tedVideo);

        Optional<TedVideo> retrieved = tedTranscriptDao.findTedVideoByResourceId(74000);
        assertTrue(retrieved.isPresent());
        assertEquals(retrieved.get().getTitle(), "Test");
    }
}

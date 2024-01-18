package de.l3s.learnweb.resource.glossary;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.dashboard.glossary.GlossaryUserActivity;
import de.l3s.learnweb.dashboard.glossary.GlossaryUserTermsSummary;
import de.l3s.test.LearnwebExtension;

class GlossaryTermDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final GlossaryTermDao glossaryTermDao = learnwebExt.attach(GlossaryTermDao.class);

    @Test
    void findById() {
        Optional<GlossaryTerm> retrieved = glossaryTermDao.findById("1");
        assertTrue(retrieved.isPresent());
        assertEquals("Giacimento minerale", retrieved.get().getTerm());
    }

    @Test
    void findByEntryId() {
        List<GlossaryTerm> retrieved = glossaryTermDao.findByEntryId(2);
        assertEquals(2, retrieved.size());
        assertEquals("encyclopaedia", retrieved.get(0).getSource());
        assertEquals("Mineral deposit", retrieved.get(1).getTerm());
    }

    @Test
    void countTotalTerms() {
        List<Integer> users = Arrays.asList(1, 4);
        int retrieved = glossaryTermDao.countTotalTerms(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertEquals(20, retrieved);
    }

    @Test
    void countTermsPerUser() {
        List<Integer> users = Arrays.asList(1, 4);
        Map<String, Integer> expected = Map.ofEntries(
            Map.entry("user2", 20)
        );
        Map<String, Integer> retrieved = glossaryTermDao.countTermsPerUser(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertNotNull(retrieved);
        assertEquals(expected, retrieved);
    }

    @Test
    void countTotalSources() {
        List<Integer> users = Arrays.asList(1, 4);
        int retrieved = glossaryTermDao.countTotalTerms(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertEquals(20, retrieved);
    }

    @Test
    void countUsagePerSource() {
        List<Integer> users = Arrays.asList(1, 4);
        Map<String, Integer> expected = Map.ofEntries(
            Map.entry("user2", 20)
        );
        Map<String, Integer> retrieved = glossaryTermDao.countTermsPerUser(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertNotNull(retrieved);
        assertEquals(expected, retrieved);
    }

    @Test
    void countGlossaryUserTermsSummary() {
        List<Integer> users = Arrays.asList(1, 4);
        List<GlossaryUserTermsSummary> retrieved = glossaryTermDao.countGlossaryUserTermsSummary(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertEquals(1, retrieved.size());
        assertEquals(4, retrieved.get(0).getUserId());
        assertEquals(20, retrieved.get(0).getTerms());
        assertEquals(9, retrieved.get(0).getEntries());
    }

    @Test
    void countGlossaryUserActivity() {
        List<Integer> users = Arrays.asList(1, 4);
        List<GlossaryUserActivity> retrieved = glossaryTermDao.countGlossaryUserActivity(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertEquals(1, retrieved.size());
        assertEquals(4, retrieved.get(0).getUserId());
        assertEquals(20, retrieved.get(0).getTotalTerms());
        assertEquals(9, retrieved.get(0).getTotalGlossaries());
        assertEquals(6, retrieved.get(0).getTotalReferences());
    }

    @Test
    void save() {
        GlossaryTerm glossaryTerm = new GlossaryTerm();
        glossaryTerm.setId(21);
        glossaryTerm.setEntryId(2);
        glossaryTerm.setTerm("term");
        glossaryTerm.setLanguage(Locale.of("fr"));
        glossaryTerm.setUses(Collections.singletonList("technical"));
        glossaryTerm.setPronounciation("");
        glossaryTerm.setAcronym("");
        glossaryTerm.setSource("Wikipedia");
        glossaryTerm.setPhraseology("");
        glossaryTerm.setTermPasted(true);
        glossaryTerm.setPronounciationPasted(false);
        glossaryTerm.setAcronymPasted(false);
        glossaryTerm.setPhraseologyPasted(false);
        glossaryTerm.setUpdatedAt(LocalDateTime.of(2021, 4, 5, 17, 0, 0));
        glossaryTerm.setCreatedAt(LocalDateTime.of(2021, 4, 5, 16, 0, 0));
        glossaryTermDao.save(glossaryTerm);

        Optional<GlossaryTerm> retrieved = glossaryTermDao.findById("21");
        assertTrue(retrieved.isPresent());
        assertEquals(glossaryTerm.getId(), retrieved.get().getId());
        assertEquals(glossaryTerm.getTerm(), retrieved.get().getTerm());
        assertEquals(glossaryTerm.getEntryId(), retrieved.get().getEntryId());
        assertEquals(glossaryTerm.getLanguage(), retrieved.get().getLanguage());
        assertEquals(glossaryTerm.getUses(), retrieved.get().getUses());
        assertEquals(glossaryTerm.getSource(), retrieved.get().getSource());
        assertEquals(glossaryTerm.isTermPasted(), retrieved.get().isTermPasted());
        assertEquals(glossaryTerm.getCreatedAt(), retrieved.get().getCreatedAt());
        assertEquals(glossaryTerm.getUpdatedAt(), retrieved.get().getUpdatedAt());
    }
}

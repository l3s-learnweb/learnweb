package de.l3s.learnweb.resource.glossary;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.dashboard.glossary.GlossaryDescriptionSummary;
import de.l3s.test.LearnwebExtension;

class GlossaryEntryDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final GlossaryEntryDao glossaryEntryDao = learnwebExt.attach(GlossaryEntryDao.class);

    @Test
    void findById() {
        Optional<GlossaryEntry> glossaryEntry = glossaryEntryDao.findById(1);
        assertTrue(glossaryEntry.isPresent());
        assertEquals("Globalization", glossaryEntry.get().getTopicOne());
    }

    @Test
    void findByResourceId() {
        List<GlossaryEntry> glossaryEntries = glossaryEntryDao.findByResourceId(9);
        assertEquals(9, glossaryEntries.size());
        assertNotEquals("Dummy Text", glossaryEntries.get(0).getDescription());
        assertEquals("a unit for measuring an area of land, equal to 10,000 square metres", glossaryEntries.get(8).getDescription());
    }

    @Test
    void deleteSoft() {
        List<GlossaryEntry> glossaryEntries = glossaryEntryDao.findByResourceId(9);
        assertEquals(9, glossaryEntries.size());
        assertFalse(glossaryEntries.get(0).isDeleted());
        assertEquals(4, glossaryEntries.get(0).getUserId());

        glossaryEntryDao.deleteSoft(glossaryEntries.get(0), 5);
        List<GlossaryEntry> updated = glossaryEntryDao.findByResourceId(9);
        assertEquals(8, updated.size());
        assertNotEquals("Dummy Text", glossaryEntries.get(0).getDescription());
    }

    @Test
    void countTotalEntries() {
        List<Integer> users = Arrays.asList(1, 4);
        int retrieved = glossaryEntryDao.countTotalEntries(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertEquals(10, retrieved);
    }

    @Test
    void countEntriesPerUser() {
        List<Integer> users = Arrays.asList(1, 4);
        Map<String, Integer> expected = Map.ofEntries(
            Map.entry("user2", 10)
        );
        Map<String, Integer> retrieved = glossaryEntryDao.countEntriesPerUser(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertNotNull(retrieved);
        assertEquals(expected, retrieved);
    }

    @Test
    void countGlossaryDescriptionSummary() {
        List<Integer> users = Arrays.asList(1, 4);
        List<GlossaryDescriptionSummary> retreieved = glossaryEntryDao.countGlossaryDescriptionSummary(users, LocalDate.of(2020, 3, 21), LocalDate.of(2021, 3, 21));
        assertEquals("Dummy Text", retreieved.get(0).getDescription());
        assertEquals("a unit for measuring an area of land, equal to 10,000 square metres", retreieved.get(9).getDescription());
        assertEquals(10, retreieved.size());
        retreieved.forEach(element -> assertEquals(9, element.getResourceId()));
        retreieved.forEach(element -> assertFalse(element.isDescriptionPasted()));
    }

    @Test
    void save() {
        GlossaryEntry glossary = new GlossaryEntry();
        glossary.setUserId(1);
        glossary.setResourceId(9);
        glossary.setTopicOne("Topic1");
        glossary.setTopicTwo("Topic2");
        glossary.setTopicThree("Topic3");
        glossary.setDescription("Description");
        glossary.setDescriptionPasted(false);
        glossary.setImported(false);
        glossaryEntryDao.save(glossary);

        Optional<GlossaryEntry> retrieved = glossaryEntryDao.findById(glossary.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(glossary.getResourceId(), retrieved.get().getResourceId());
        assertEquals(glossary.getTopicOne(), retrieved.get().getTopicOne());
        assertEquals(glossary.getTopicTwo(), retrieved.get().getTopicTwo());
        assertEquals(glossary.getTopicThree(), retrieved.get().getTopicThree());
        assertEquals(glossary.getDescription(), retrieved.get().getDescription());
        assertEquals(glossary.isDescriptionPasted(), retrieved.get().isDescriptionPasted());
        assertEquals(glossary.isImported(), retrieved.get().isImported());
        assertEquals(glossary.getUpdatedAt(), retrieved.get().getUpdatedAt());
        assertEquals(glossary.getCreatedAt(), retrieved.get().getCreatedAt());
    }
}

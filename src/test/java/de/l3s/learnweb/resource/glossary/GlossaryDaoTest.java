package de.l3s.learnweb.resource.glossary;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.test.LearnwebExtension;

class GlossaryDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final GlossaryDao glossaryDao = learnwebExt.attach(GlossaryDao.class);
    private final ResourceDao resourceDao = learnwebExt.attach(ResourceDao.class);
    private final GlossaryEntryDao glossaryEntryDao = learnwebExt.attach(GlossaryEntryDao.class);

    @Test
    void insertGlossaryResource() {
        glossaryDao.insertGlossaryResource(9, "ch");
        Optional<String> retrieved = glossaryDao.findGlossaryResourceAllowedLanguages(9);
        assertTrue(retrieved.isPresent());
        assertEquals("ch", retrieved.get());
    }

    @Test
    void findGlossaryResourceAllowedLanguages() {
        Optional<String> retrieved = glossaryDao.findGlossaryResourceAllowedLanguages(9);
        assertTrue(retrieved.isPresent());
        assertEquals("de,en,fr,it,es", retrieved.get());
    }

    @Test
    void findResourceById() {
        Optional<GlossaryResource> resource = glossaryDao.findResourceById(9);
        assertTrue(resource.isPresent());
        assertEquals("Glossary res", resource.get().getTitle());
    }

    @Test
    void convertToGlossaryResource() {
        Optional<Resource> resource = resourceDao.findById(9);
        assertTrue(resource.isPresent());

        Optional<GlossaryResource> retrieved = glossaryDao.convertToGlossaryResource(resource.get());
        assertTrue(retrieved.isPresent());
        assertEquals(resource.get().getDescription(), retrieved.get().getDescription());
        assertEquals(resource.get().getId(), retrieved.get().getId());
    }

    @Test
    void findByOwnerIds() {
        List<GlossaryResource> retrieved = glossaryDao.findByOwnerIds(Arrays.asList(1, 4));
        assertEquals(1, retrieved.size());
        assertEquals("Glossary res", retrieved.get(0).getTitle());
        assertEquals(9, retrieved.get(0).getId());
    }

    @Test
    void save() {
        GlossaryResource glossary = new GlossaryResource();
        glossary.setUserId(10);
        ArrayList<Locale> allowedLanguages = new ArrayList<>();
        allowedLanguages.add(Locale.of("fr"));
        glossary.setAllowedLanguages(allowedLanguages);
        glossary.setDeleted(false);
        LinkedList<GlossaryEntry> entries = new LinkedList<>();
        entries.add(glossaryEntryDao.findById(1).get());
        entries.add(glossaryEntryDao.findById(2).get());
        glossary.setEntries(entries);
        glossary.setDescription("desc");
        glossary.setTitle("title");
        glossary.setId(11);
        resourceDao.save(glossary);
        glossaryDao.save(glossary);

        Optional<GlossaryResource> retrieved = glossaryDao.findResourceById(11);
        assertTrue(retrieved.isPresent());
        assertEquals(glossary.getAllowedLanguages(), retrieved.get().getAllowedLanguages());
        assertEquals(glossary.getEntries(), retrieved.get().getEntries());
        assertEquals(glossary.getUserId(), retrieved.get().getUserId());
        assertEquals(glossary.isDeleted(), retrieved.get().isDeleted());
        assertEquals(glossary.getDescription(), retrieved.get().getDescription());
        assertEquals(glossary.getTitle(), retrieved.get().getTitle());
    }

    @Test
    void saveEntry() {
        GlossaryEntry glossary = new GlossaryEntry();
        glossary.setUserId(1);
        glossary.setResourceId(9);
        glossary.setTopicOne("Topic1");
        glossary.setTopicTwo("Topic2");
        glossary.setTopicThree("Topic3");
        glossary.setDescription("Description");
        glossary.setDescriptionPasted(false);
        glossary.setImported(false);
        glossaryDao.saveEntry(glossary);

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

    @Test
    void saveTerms() {
        GlossaryEntry glossary = new GlossaryEntry();
        glossary.setUserId(1);
        glossary.setResourceId(9);
        glossary.setTopicOne("Topic1");
        glossary.setTopicTwo("Topic2");
        glossary.setTopicThree("Topic3");
        glossary.setDescription("Description");
        glossary.setDescriptionPasted(false);
        glossary.setImported(false);
        glossaryDao.saveTerms(glossary);

        List<GlossaryTerm> retrieved = glossaryDao.getGlossaryTermDao().findByEntryId(glossary.getId());
        assertEquals(glossary.getTerms(), retrieved);
    }
}

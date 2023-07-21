package de.l3s.learnweb.user;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.test.LearnwebExtension;

class OrganisationDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final OrganisationDao organisationDao = learnwebExt.attach(OrganisationDao.class);

    @BeforeEach
    void setUp() {
        OrganisationDao.cache.clear();
    }

    @Test
    void findById() {
        Optional<Organisation> retrieved = organisationDao.findById(1);
        assertTrue(retrieved.isPresent());

        assertEquals(1, retrieved.get().getId());
        assertEquals("Public", retrieved.get().getTitle());
        assertEquals("Hello world", retrieved.get().getWelcomeMessage());
        assertEquals("myhome/welcome.jsf", retrieved.get().getWelcomePage());
        assertEquals("en", retrieved.get().getDefaultLanguage());
        assertEquals(3, retrieved.get().getGlossaryLanguages().size());
        assertEquals(ColorTheme.emerald, retrieved.get().getTheme());
        assertEquals(SearchMode.text, retrieved.get().getDefaultSearchMode());
        assertEquals(ResourceService.bing, retrieved.get().getDefaultSearchServiceText());
        assertEquals(ResourceService.flickr, retrieved.get().getDefaultSearchServiceImage());
        assertEquals(ResourceService.youtube, retrieved.get().getDefaultSearchServiceVideo());
    }

    @Test
    void findAll() {
        List<Organisation> allOrganisation = organisationDao.findAll();
        assertFalse(allOrganisation.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, allOrganisation.stream().map(Organisation::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByTitle() {
        Optional<Organisation> organisationOptional = organisationDao.findByTitle("Public");
        assertFalse(organisationOptional.isEmpty());
        assertEquals(1, organisationOptional.get().getId());
        assertEquals("Public", organisationOptional.get().getTitle());
    }

    @Test
    void findAuthors() {
        List<String> authors = organisationDao.findAuthors(1);
        assertFalse(authors.isEmpty());
        assertArrayEquals(new String[] {"", "Mark Rober"}, authors.toArray(String[]::new));
    }

    @Test
    void save() {
        Organisation testOrganisation = new Organisation(100);
        testOrganisation.setTitle("learnweb");
        testOrganisation.setWelcomeMessage("Hello Philipp & Oleh");
        testOrganisation.setTermsAndConditions("By registering in the organization you confirm you are not russian");
        testOrganisation.setWelcomePage("/lw/myhome/welcome.jsf");
        testOrganisation.setDefaultLanguage("en");
        testOrganisation.setDefaultSearchServiceImage(ResourceService.flickr);
        testOrganisation.setDefaultSearchServiceText(ResourceService.bing);
        testOrganisation.setDefaultSearchServiceVideo(ResourceService.youtube);

        organisationDao.save(testOrganisation);

        Optional<Organisation> retrieved = organisationDao.findById(100);
        assertTrue(retrieved.isPresent());

        assertEquals(100, retrieved.get().getId());
        assertEquals("learnweb", retrieved.get().getTitle());
        assertEquals("Hello Philipp & Oleh", retrieved.get().getWelcomeMessage());
        assertEquals("By registering in the organization you confirm you are not russian", retrieved.get().getTermsAndConditions());
        assertEquals("/lw/myhome/welcome.jsf", retrieved.get().getWelcomePage());
        assertEquals("en", retrieved.get().getDefaultLanguage());

        assertEquals(ColorTheme.emerald, retrieved.get().getTheme());
        assertEquals(SearchMode.text, retrieved.get().getDefaultSearchMode());
        assertEquals(ResourceService.bing, retrieved.get().getDefaultSearchServiceText());
        assertEquals(ResourceService.flickr, retrieved.get().getDefaultSearchServiceImage());
        assertEquals(ResourceService.youtube, retrieved.get().getDefaultSearchServiceVideo());
    }
}

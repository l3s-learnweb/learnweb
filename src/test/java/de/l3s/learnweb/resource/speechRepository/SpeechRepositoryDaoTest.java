package de.l3s.learnweb.resource.speechRepository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class SpeechRepositoryDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final SpeechRepositoryDao speechRepositoryDao = learnwebExt.attach(SpeechRepositoryDao.class);

    @Test
    void findById() {
        Optional<SpeechRepositoryEntity> speechRepository = speechRepositoryDao.findById(21662);
        assertTrue(speechRepository.isPresent());
    }

    @Test
    void isExists() {
        assertFalse(speechRepositoryDao.isExists(1));
        assertTrue(speechRepositoryDao.isExists(21662));
    }

    @Test
    void delete() {
        Optional<SpeechRepositoryEntity> speechRepository = speechRepositoryDao.findById(21662);
        assertTrue(speechRepository.isPresent());
        speechRepositoryDao.delete(21662);
        Optional<SpeechRepositoryEntity> deletedRepository = speechRepositoryDao.findById(21662);
        assertFalse(deletedRepository.isPresent());
    }

    @Test
    void save() {
        SpeechRepositoryEntity speechRepository = new SpeechRepositoryEntity();
        speechRepository.setTitle("title");
        speechRepository.setUrl("https://www.speechRepository.de");
        speechRepository.setRights("wrx");
        speechRepository.setDate("14.04.2021");
        speechRepository.setDescription("description");
        speechRepository.setNotes("notes");
        speechRepository.setImageLink("link/to/image");
        speechRepository.setVideoLink("link/to/video");
        speechRepository.setDuration(10);
        speechRepository.setLanguage("de");
        speechRepository.setLevel("3");
        speechRepository.setUse("test");
        speechRepository.setType("type");
        speechRepository.setDomains("domain");
        speechRepository.setTerminology("terminology");
        speechRepository.setLearnwebResourceId(1);
        speechRepository.setId(32);
        speechRepositoryDao.save(speechRepository);

        Optional<SpeechRepositoryEntity> retrieved = speechRepositoryDao.findById(32);
        assertTrue(retrieved.isPresent());
        assertEquals(speechRepository.getId(), retrieved.get().getId());
        assertEquals(speechRepository.getDate(), retrieved.get().getDate());
    }
}

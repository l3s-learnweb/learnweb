package de.l3s.learnweb.resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.user.User;
import de.l3s.test.LearnwebExtension;

class TagDaoTest {


    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final TagDao tagDao = learnwebExt.attach(TagDao.class);

    @Test
    void findById() {
        Optional<Tag> tagDaoById = tagDao.findById(1);
        assertTrue(tagDaoById.isPresent());
        assertEquals(1, tagDaoById.get().getId());
        assertEquals("Squirrel", tagDaoById.get().getName());
    }

    @Test
    void findByName() {
        Optional<Tag> tagDaoByName = tagDao.findByName("Squirrel");
        assertTrue(tagDaoByName.isPresent());
        assertEquals(1, tagDaoByName.get().getId());
    }

    @Test
    void findByUserId() {
        List<Tag> tagDaoList = tagDao.findByUserId(1);
        assertFalse(tagDaoList.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4}, tagDaoList.stream().map(Tag::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByResourceId() {
        OwnerList<Tag, User> tagDaoResource = tagDao.findByResourceId(1);
        assertFalse(tagDaoResource.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 5, 6}, tagDaoResource.stream().map(Tag::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void delete() {
        Optional<Tag> retrieved = tagDao.findById(1);
        assertTrue(retrieved.isPresent());
        Tag testTag = retrieved.get();
        tagDao.delete(testTag);
        assertTrue(tagDao.findById(1).isEmpty());
    }

    @Test
    void save() {
        Tag testTag = new Tag(0, "breaking");
        tagDao.save(testTag);

        Optional<Tag> tagOptional = tagDao.findByName("breaking");
        assertTrue(tagOptional.isPresent());
        assertTrue(tagOptional.get().getId() > 0);
        assertEquals("breaking", tagOptional.get().getName());
    }
}

package de.l3s.learnweb.resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.test.LearnwebExtension;

class ResourceDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final ResourceDao resourceDao = learnwebExt.attach(ResourceDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);
    private final TagDao tagDao = learnwebExt.attach(TagDao.class);

    @Test
    void findById() {
        Optional<Resource> resource = resourceDao.findById(1);
        assertTrue(resource.isPresent());
        assertEquals(1, resource.get().getId());
    }

    @Test
    void findAll() {
        Stream<Resource> resources = resourceDao.findAll();
        assertEquals(10, resources.count());
    }

    @Test
    void findByGroupId() {
        List<Resource> resources = resourceDao.findByGroupId(1);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {1, 4, 5, 6, 7, 8, 9}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByFolderId() {
        List<Resource> resources = resourceDao.findByFolderId(4);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {1}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByOwnerId() {
        List<Resource> resources = resourceDao.findByOwnerId(1);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 10}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByTagId() {
        List<Resource> resources = resourceDao.findByTagId(1);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {1, 6}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByUrl() {
        Optional<Resource> resources = resourceDao.findByUrl("https://www.youtube.com/watch?v=hFZFjoX2cGg");
        assertTrue(resources.isPresent());
        assertEquals(1, resources.get().getId());
    }

    @Test
    void findRatedByUsedId() {
        List<Resource> resources = resourceDao.findRatedByUsedId(2);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {1, 4, 6}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByGroupIdAndFolderIdAndOwnerId() {
        List<Resource> resources = resourceDao.findByGroupIdAndFolderIdAndOwnerId(1, 4, 1, 1);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {1}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Disabled("Missing test data")
    @Test
    void findBySubmissionIdAndUserId() {
        List<Resource> resources = resourceDao.findBySubmissionIdAndUserId(1, 1);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {1, 4, 5, 6, 7, 8, 9}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findSurveysByCourseId() {
        List<Resource> resources = resourceDao.findSurveysByCourseId(1);
        assertTrue(resources.isEmpty());
    }

    @Test
    void findByOwnerIdsAndType() {
        ResourceType type = ResourceType.image;
        List<Resource> resources = resourceDao.findByOwnerIdsAndType(Collections.singletonList(4), type);
        assertFalse(resources.isEmpty());
        assertArrayEquals(new Integer[] {4, 5, 6, 7, 8}, resources.stream().map(Resource::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void countUndeleted() {
        assertEquals(10, resourceDao.countUndeleted());
    }

    @Test
    void countByOwnerId() {
        assertEquals(4, resourceDao.countByOwnerId(1));
    }

    @Test
    void countByGroupId() {
        int count = resourceDao.countByGroupId(1);
        assertTrue(count > 0);
        assertEquals(7, count);
    }

    @Test
    void countPerUserByGroupId() {
        Map<Integer, Integer> count = resourceDao.countPerUserByGroupId(1);
        assertEquals(2, count.size());
        assertArrayEquals(new Integer[] {1, 4}, count.keySet().toArray());
    }

    @Test
    void findResourceRating() {
        Optional<Integer> rating = resourceDao.findResourceRating(4, 2);
        assertTrue(rating.isPresent());
        assertEquals(rating.get(), 3);
    }

    @Test
    void insertResourceRating() {
        Optional<Integer> rating = resourceDao.findResourceRating(4, 1);
        assertTrue(rating.isEmpty());

        resourceDao.insertResourceRating(4, 1, 5);

        Optional<Integer> newRating = resourceDao.findResourceRating(4, 1);
        assertTrue(newRating.isPresent());
        assertEquals(newRating.get(), 5);
    }

    @Test
    void insertThumbRate() {
        Optional<Resource> resource = resourceDao.findById(1);
        assertTrue(resource.isPresent());
        Optional<User> user = userDao.findById(1);
        assertTrue(user.isPresent());
        resourceDao.insertThumbRate(resource.get(), user.get(), 1);
    }

    @Test
    void findThumbRate() {
        Optional<Resource> resource = resourceDao.findById(1);
        assertTrue(resource.isPresent());
        Optional<User> user = userDao.findById(5);
        assertTrue(user.isPresent());
        Optional<Integer> value = resourceDao.findThumbRate(resource.get(), user.get());
        assertTrue(value.isPresent());
        assertEquals(1, value.get());
    }

    @Test
    void insertTag() {
        Optional<Resource> resource = resourceDao.findById(4);
        assertTrue(resource.isPresent());
        Optional<User> user = userDao.findById(1);
        assertTrue(user.isPresent());
        Optional<Tag> tag = tagDao.findById(1);
        assertTrue(tag.isPresent());
        assertFalse(resourceDao.findByTagId(tag.get().getId()).contains(resource.get()));
        resourceDao.insertTag(resource.get(), user.get(), tag.get());
        assertTrue(resourceDao.findByTagId(tag.get().getId()).contains(resource.get()));
    }

    @Test
    void deleteTag() {
        Optional<Resource> resource = resourceDao.findById(6);
        assertTrue(resource.isPresent());
        Optional<Tag> tag = tagDao.findById(10);
        assertTrue(tag.isPresent());
        assertTrue(resourceDao.findByTagId(tag.get().getId()).contains(resource.get()));
        resourceDao.deleteTag(resource.get(), tag.get());
        assertFalse(resourceDao.findByTagId(tag.get().getId()).contains(resource.get()));
    }

    @Test
    void save() {
        Resource resource = new Resource();
        resource.setId(10);
        resource.setUserId(2);
        resource.setService(ResourceService.bing);
        resource.setType(ResourceType.document);
        resource.setTitle("ABC");
        resource.setFormat("doc");
        resourceDao.save(resource);
        Optional<Resource> retrieved = resourceDao.findById(10);
        assertTrue(retrieved.isPresent());
        assertEquals(10, retrieved.get().getId());
    }

    @Test
    void deleteSoft() {
        Optional<Resource> resource = resourceDao.findById(7);
        assertTrue(resource.isPresent());

        resourceDao.deleteSoft(resource.get());

        Optional<Resource> retrieved = resourceDao.findById(7);
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().isDeleted());
    }

    @Test
    void deleteHard() {
        Optional<Resource> resource = resourceDao.findById(7);
        assertTrue(resource.isPresent());

        resourceDao.deleteHard(resource.get());

        Optional<Resource> retrieved = resourceDao.findById(7);
        assertTrue(retrieved.isEmpty());
    }
}

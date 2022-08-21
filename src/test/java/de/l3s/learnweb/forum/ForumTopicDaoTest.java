package de.l3s.learnweb.forum;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class ForumTopicDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final ForumPostDao forumPostDao = learnwebExt.attach(ForumPostDao.class);
    private final ForumTopicDao forumTopicDao = learnwebExt.attach(ForumTopicDao.class);

    @Test
    void findById() {
        Optional<ForumTopic> topic = forumTopicDao.findById(2);

        assertTrue(topic.isPresent());
        assertEquals(2, topic.get().getId());
        assertEquals(4, topic.get().getUserId());
        assertEquals(1, topic.get().getGroupId());
        assertEquals(LocalDateTime.of(2021, 2, 19, 18, 34, 2), topic.get().getCreatedAt());
        assertEquals(3, topic.get().getReplies());
        assertEquals(2, topic.get().getViews());
        assertFalse(topic.get().isDeleted());
        assertEquals("How I Live Now", topic.get().getTitle());
        assertEquals(5, topic.get().getLastPostId());
        assertEquals(LocalDateTime.of(2021, 2, 19, 18, 34, 43), topic.get().getUpdatedAt());
        assertEquals(1, topic.get().getLastPostUserId());
    }

    @Test
    void findByGroupId() {
        List<ForumTopic> topics = forumTopicDao.findByGroupId(1);
        assertFalse(topics.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4}, topics.stream().map(ForumTopic::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void updateIncreaseViews() {
        assertEquals(1, forumTopicDao.findById(4).orElseThrow().getViews());
        forumTopicDao.updateIncreaseViews(4);
        assertEquals(2, forumTopicDao.findById(4).orElseThrow().getViews());
    }

    @Test
    void updateIncreaseReplies() {
        Optional<ForumTopic> topic = forumTopicDao.findById(4);
        assertTrue(topic.isPresent());
        assertEquals(3, topic.get().getReplies());

        ForumPost newPost = new ForumPost();
        newPost.setTopicId(4);
        newPost.setUserId(2);
        newPost.setText("new post text");
        forumPostDao.save(newPost);

        forumTopicDao.updateIncreaseReplies(4, newPost.getId(), newPost.getUserId(), newPost.getCreatedAt());

        Optional<ForumTopic> updated = forumTopicDao.findById(4);
        assertTrue(updated.isPresent());
        assertEquals(4, updated.get().getReplies());
        assertEquals(newPost.getId(), updated.get().getLastPostId());
        assertEquals(newPost.getUpdatedAt(), updated.get().getUpdatedAt());
        assertEquals(newPost.getUserId(), updated.get().getLastPostUserId());
    }

    @Test
    void delete() {
        assertTrue(forumTopicDao.findById(5).isPresent());
        forumTopicDao.delete(5);
        assertTrue(forumTopicDao.findById(5).isEmpty());
    }

    @Test
    void findByNotificationFrequencies() {
        // TODO
        // Map<Integer, List<ForumTopic>> topics = forumTopicDao.findByNotificationFrequencies(Collections.singletonList(User.NotificationFrequency.DAILY));
    }

    @Test
    void insertUserVisit() {
        // TODO
    }

    @Test
    void save() {
        ForumTopic topic = new ForumTopic();
        topic.setUserId(4);
        topic.setGroupId(2);
        topic.setTitle("Phasellus in felis");
        forumTopicDao.save(topic);
        assertTrue(topic.getId() != 0);

        Optional<ForumTopic> retrieved = forumTopicDao.findById(topic.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(topic.getId(), retrieved.get().getId());
        assertEquals(topic.getUserId(), retrieved.get().getUserId());
        assertEquals(topic.getGroupId(), retrieved.get().getGroupId());
        assertEquals(topic.getCreatedAt(), retrieved.get().getCreatedAt());
        assertEquals(topic.getReplies(), retrieved.get().getReplies());
        assertEquals(topic.getViews(), retrieved.get().getViews());
        assertEquals(topic.isDeleted(), retrieved.get().isDeleted());
        assertEquals(topic.getTitle(), retrieved.get().getTitle());

        topic.setTitle("updated text");
        forumTopicDao.save(topic);
        assertNotEquals(retrieved.get().getTitle(), topic.getTitle());

        Optional<ForumTopic> updated = forumTopicDao.findById(topic.getId());
        assertTrue(updated.isPresent());
        assertEquals(topic.getTitle(), updated.get().getTitle());
    }
}

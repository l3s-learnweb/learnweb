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
    void getters() {
        ForumPost post = new ForumPost();
        post.setText("dog");
        post.setUserId(3);
        forumPostDao.save(post);

        ForumPost post2 = new ForumPost();
        post2.setText("zebra");
        post2.setUserId(3);
        forumPostDao.save(post2);

        ForumTopic topic = new ForumTopic();
        topic.setUserId(3);
        topic.setGroupId(12);
        topic.setTitle("monkey");
        topic.setLastPostDate(LocalDateTime.now());
        topic.setLastPostId(post.getId());
        topic.setLastPostUserId(post.getUserId());
        forumTopicDao.save(topic);

        ForumTopic topic2 = new ForumTopic();
        topic2.setUserId(2);
        topic2.setGroupId(12);
        topic2.setTitle("parrot");
        topic2.setLastPostDate(LocalDateTime.now());
        topic2.setLastPostId(post2.getId());
        topic2.setLastPostUserId(post2.getUserId());
        forumTopicDao.save(topic2);

        Optional<ForumTopic> retrieved = forumTopicDao.findById(topic.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(topic.getId(), retrieved.get().getId());
        assertEquals(topic.getUserId(), retrieved.get().getUserId());
        assertEquals(topic.getGroupId(), retrieved.get().getGroupId());
        assertEquals(topic.getTitle(), retrieved.get().getTitle());

        List<ForumTopic> topicsByGroupId = forumTopicDao.findByGroupId(topic2.getGroupId());
        assertEquals(2, topicsByGroupId.size());
        assertEquals(topic2.getTitle(), topicsByGroupId.get(0).getTitle());
    }

    @Test
    void save() {
        ForumPost post = new ForumPost();
        post.setText("parrot");
        post.setUserId(3);
        forumPostDao.save(post);
        assertTrue(post.getId() > 0);

        ForumTopic topic = new ForumTopic();
        topic.setUserId(3);
        topic.setGroupId(12);
        topic.setTitle("monkey");
        topic.setLastPostDate(LocalDateTime.now());
        topic.setLastPostId(post.getId());
        topic.setLastPostUserId(post.getUserId());
        forumTopicDao.save(topic);
        assertTrue(topic.getId() > 0);

        Optional<ForumTopic> retrieved = forumTopicDao.findById(topic.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(topic.getId(), retrieved.get().getId());
        assertEquals(topic.getUserId(), retrieved.get().getUserId());
        assertEquals(topic.getGroupId(), retrieved.get().getGroupId());
        assertEquals(topic.getTitle(), retrieved.get().getTitle());

        forumTopicDao.delete(topic.getId());
        Optional<ForumTopic> deleted = forumTopicDao.findById(topic.getId());
        assertFalse(deleted.isPresent());
    }
}

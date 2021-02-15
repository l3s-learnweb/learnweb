package de.l3s.learnweb.forum;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class ForumPostDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final ForumPostDao forumPostDao = learnwebExt.attach(ForumPostDao.class);

    @Test
    void getters() {
        ForumPost post = new ForumPost();
        post.setTopicId(1);
        post.setText("hello world");
        post.setUserId(113);
        forumPostDao.save(post);
        assertTrue(post.getId() > 0);

        ForumPost post2 = new ForumPost();
        post2.setTopicId(2);
        post2.setText("second world");
        post2.setUserId(113);
        forumPostDao.save(post2);
        assertTrue(post2.getId() > 0);

        Optional<ForumPost> retrieved = forumPostDao.findById(post.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(post.getId(), retrieved.get().getId());
        assertEquals(post.getTopicId(), retrieved.get().getTopicId());
        assertEquals(post.getText(), retrieved.get().getText());
        assertEquals(post.getUserId(), retrieved.get().getUserId());

        List<ForumPost> postsByTopicId = forumPostDao.findByTopicId(post.getTopicId());
        assertEquals(1, postsByTopicId.size());

        List<ForumPost> postsByUserId = forumPostDao.findByUserId(post.getUserId());
        assertEquals(2, postsByUserId.size());

        int postCountByUserId = forumPostDao.countByUserId(post.getUserId());
        assertEquals(2, postCountByUserId);
    }

    @Test
    void save() {
        ForumPost post = new ForumPost();
        post.setText("new world");
        post.setUserId(120);
        forumPostDao.save(post);
        assertTrue(post.getId() > 0);

        Optional<ForumPost> retrieved = forumPostDao.findById(post.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(post.getId(), retrieved.get().getId());
        assertEquals(post.getText(), retrieved.get().getText());
        assertEquals(post.getUserId(), retrieved.get().getUserId());

        post.setText("updated world");
        forumPostDao.save(post);
        assertNotEquals(retrieved.get().getText(), post.getText());

        Optional<ForumPost> updated = forumPostDao.findById(post.getId());
        assertTrue(updated.isPresent());
        assertEquals(post.getText(), updated.get().getText());

        forumPostDao.delete(post.getId());
        Optional<ForumPost> deleted = forumPostDao.findById(post.getId());
        assertFalse(deleted.isPresent());
    }
}

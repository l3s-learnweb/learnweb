package de.l3s.learnweb.forum;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.DatabaseExtension;

class ForumPostDaoTest {

    @RegisterExtension
    static final DatabaseExtension db = new DatabaseExtension();

    private static ForumPostDao forumPostDao;

    @BeforeAll
    static void beforeAll() {
        forumPostDao = db.attach(ForumPostDao.class);
    }

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

        Optional<ForumPost> retrieved = forumPostDao.getPostById(post.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(post.getId(), retrieved.get().getId());
        assertEquals(post.getTopicId(), retrieved.get().getTopicId());
        assertEquals(post.getText(), retrieved.get().getText());
        assertEquals(post.getUserId(), retrieved.get().getUserId());

        List<ForumPost> postsByTopicId = forumPostDao.getPostsByTopicId(post.getTopicId());
        assertEquals(1, postsByTopicId.size());

        List<ForumPost> postsByUserId = forumPostDao.getPostsByUserId(post.getUserId());
        assertEquals(2, postsByUserId.size());

        int postCountByUserId = forumPostDao.getPostCountByUserId(post.getUserId());
        assertEquals(2, postCountByUserId);
    }

    @Test
    void save() {
        ForumPost post = new ForumPost();
        post.setText("new world");
        post.setUserId(120);
        forumPostDao.save(post);
        assertTrue(post.getId() > 0);

        Optional<ForumPost> retrieved = forumPostDao.getPostById(post.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(post.getId(), retrieved.get().getId());
        assertEquals(post.getText(), retrieved.get().getText());
        assertEquals(post.getUserId(), retrieved.get().getUserId());

        post.setText("updated world");
        forumPostDao.save(post);
        assertNotEquals(retrieved.get().getText(), post.getText());

        Optional<ForumPost> updated = forumPostDao.getPostById(post.getId());
        assertTrue(updated.isPresent());
        assertEquals(post.getText(), updated.get().getText());

        forumPostDao.deletePostById(post.getId());
        Optional<ForumPost> deleted = forumPostDao.getPostById(post.getId());
        assertFalse(deleted.isPresent());
    }
}

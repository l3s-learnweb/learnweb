package de.l3s.learnweb.forum;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class ForumPostDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final ForumPostDao forumPostDao = learnwebExt.attach(ForumPostDao.class);

    @Test
    void findById() {
        Optional<ForumPost> post = forumPostDao.findById(5);

        assertTrue(post.isPresent());
        assertEquals(5, post.get().getId());
        assertEquals(2, post.get().getTopicId());
        assertFalse(post.get().isDeleted());
        assertEquals(1, post.get().getUserId());
        assertEquals("<p>Proin leo odio, porttitor id, consequat in, consequat ut, nulla.</p>", post.get().getText());
        assertEquals(0, post.get().getEditCount());
        assertEquals(LocalDateTime.of(2021, 2, 19, 18, 34, 43), post.get().getDate());
    }

    @Test
    void findByTopicId() {
        List<ForumPost> posts = forumPostDao.findByTopicId(2);
        assertFalse(posts.isEmpty());
        assertArrayEquals(new Integer[] {2, 3, 4, 5}, posts.stream().map(ForumPost::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByUserId() {
        List<ForumPost> posts = forumPostDao.findByUserId(4);
        assertFalse(posts.isEmpty());
        assertArrayEquals(new Integer[] {1, 2}, posts.stream().map(ForumPost::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void countByUserId() {
        int posts = forumPostDao.countByUserId(1);
        assertEquals(10, posts);
    }

    @Test
    void countPerUserByGroupId() {
        Map<Integer, Integer> posts = forumPostDao.countPerUserByGroupId(1);
        assertFalse(posts.isEmpty());
        assertEquals(Map.ofEntries(
            Map.entry(1, 10),
            Map.entry(4, 2)
        ), posts);
    }

    @Test
    void delete() {
        assertTrue(forumPostDao.findById(5).isPresent());
        forumPostDao.delete(5);
        assertTrue(forumPostDao.findById(5).isEmpty());
    }

    @Test
    void save() {
        ForumPost post = new ForumPost();
        post.setTopicId(3);
        post.setUserId(2);
        post.setText("original forum post text");
        forumPostDao.save(post);
        assertTrue(post.getId() > 0);

        Optional<ForumPost> retrieved = forumPostDao.findById(post.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(post.getId(), retrieved.get().getId());
        assertEquals(post.getText(), retrieved.get().getText());
        assertEquals(post.getUserId(), retrieved.get().getUserId());
        assertEquals(post.isDeleted(), retrieved.get().isDeleted());
        assertEquals(post.getDate(), retrieved.get().getDate());

        post.setText("updated text");
        forumPostDao.save(post);
        assertNotEquals(retrieved.get().getText(), post.getText());

        Optional<ForumPost> updated = forumPostDao.findById(post.getId());
        assertTrue(updated.isPresent());
        assertEquals(post.getText(), updated.get().getText());
    }
}

package de.l3s.learnweb.resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class CommentDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final CommentDao commentDao = learnwebExt.attach(CommentDao.class);

    @Test
    void findByUserId() {
        List<Comment> retrieved = commentDao.findByUserId(1);
        assertFalse(retrieved.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3}, retrieved.stream().map(Comment::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByUserIds() {
        List<Comment> retrieved = commentDao.findByUserIds(Arrays.asList(1, 6));
        assertFalse(retrieved.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 9, 10}, retrieved.stream().map(Comment::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByResourceId() {
        List<Comment> retrieved = commentDao.findByResourceId(1);
        assertFalse(retrieved.isEmpty());
        assertTrue(retrieved.get(1).getResourceId() > 0);
    }

    @Test
    void delete() {
        Comment comment = new Comment();
        comment.setText("hello world!");
        comment.setUserId(5);
        comment.setResourceId(1);
        commentDao.save(comment);
        commentDao.delete(comment);

        Optional<Comment> retrieved = commentDao.findById(comment.getId());
        assertTrue(retrieved.isEmpty());
        assertFalse(commentDao.findByResourceId(1).contains(comment));
    }

    @Test
    void save() {
        Comment comment = new Comment();
        comment.setText("hello world!");
        comment.setUserId(1);
        comment.setResourceId(1);
        commentDao.save(comment);
        assertTrue(comment.getId() > 0);

        Optional<Comment> retrieved = commentDao.findById(comment.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(comment.getId(), retrieved.get().getId());
        assertEquals(comment.getResourceId(), retrieved.get().getResourceId());
        assertEquals(comment.getText(), retrieved.get().getText());
        assertEquals(comment.getCreatedAt(), retrieved.get().getCreatedAt());

        comment.setText("updated text");
        commentDao.save(comment);
        assertNotEquals(retrieved.get().getText(), comment.getText());

        Optional<Comment> updated = commentDao.findById(comment.getId());
        assertTrue(updated.isPresent());
        assertEquals(comment.getText(), updated.get().getText());
    }
}

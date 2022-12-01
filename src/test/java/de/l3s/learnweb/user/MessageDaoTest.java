package de.l3s.learnweb.user;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class MessageDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final MessageDao messageDao = learnwebExt.attach(MessageDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);

    @Test
    void findById() {
        Optional<Message> retrieved = messageDao.findById(1);
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().isSeen());
        assertEquals("Sed eos vitae voluptatem qui et est. Aliquid nam officiis dolore ipsa. Repellat ab tenetur eveniet nostrum.", retrieved.get().getText());
    }

    @Test
    void findOutgoing() {
        List<Message> retrieved = messageDao.findOutgoing(userDao.findByIdOrElseThrow(6));
        assertFalse(retrieved.isEmpty());
        assertEquals(2, retrieved.size());
        assertEquals("Dolorum maxime.", retrieved.get(0).getTitle());
    }

    @Test
    void findIncoming() {
        List<Message> retrieved = messageDao.findIncoming(userDao.findByIdOrElseThrow(1));
        assertFalse(retrieved.isEmpty());
        assertEquals(2, retrieved.size());
        assertEquals("Omnis ducimus.", retrieved.get(1).getTitle());
        assertEquals(4, retrieved.get(0).getId());
    }

    @Test
    void updateMarkSeen() {
        Optional<Message> retrievedOld = messageDao.findById(2);
        assertTrue(retrievedOld.isPresent());
        assertFalse(retrievedOld.get().isSeen());

        messageDao.updateMarkSeen(retrievedOld.get());
        Optional<Message> retrieved = messageDao.findById(2);
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().isSeen());
    }

    @Test
    void updateMarkSeenAll() {
        List<Message> retrievedOld = messageDao.findIncoming(userDao.findByIdOrElseThrow(1));
        assertFalse(retrievedOld.get(0).isSeen());
        assertTrue(retrievedOld.get(1).isSeen());

        messageDao.updateMarkSeenAll(userDao.findByIdOrElseThrow(1));
        List<Message> retrieved = messageDao.findIncoming(userDao.findByIdOrElseThrow(1));
        assertTrue(retrieved.get(0).isSeen());
        assertTrue(retrieved.get(1).isSeen());
    }

    @Test
    void countNotSeen() {
        int retrieved = messageDao.countNotSeen(userDao.findByIdOrElseThrow(2));
        assertEquals(2, retrieved);
    }

    @Test
    void save() {
        Message message = new Message();
        message.setId(11);
        message.setTitle("first message");
        message.setText("Hello World!");
        message.setSeen(false);
        message.setCreatedAt(LocalDateTime.of(2021, Month.FEBRUARY, 28, 18, 0, 0));
        message.setSenderUserId(1);
        message.setRecipientUserId(2);
        messageDao.save(message);

        Optional<Message> retrieved = messageDao.findById(11);
        assertTrue(retrieved.isPresent());
        assertEquals(message.getId(), retrieved.get().getId());
        assertEquals(message.getCreatedAt(), retrieved.get().getCreatedAt());
        assertEquals(message.getTitle(), retrieved.get().getTitle());
        assertEquals(message.getSenderUserId(), retrieved.get().getSenderUserId());
        assertEquals(message.getRecipientUserId(), retrieved.get().getRecipientUserId());

        message.setTitle("new title");
        messageDao.save(message);
        assertNotEquals(retrieved.get().getTitle(), message.getTitle());

        Optional<Message> updated = messageDao.findById(11);
        assertTrue(updated.isPresent());
        assertEquals(message.getTitle(), updated.get().getTitle());
    }
}

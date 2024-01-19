package de.l3s.learnweb.group;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.user.UserDao;
import de.l3s.test.LearnwebExtension;

class FolderDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final FolderDao folderDao = learnwebExt.attach(FolderDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);

    @BeforeEach
    void setUp() {
        FolderDao.cache.clear();
    }

    @Test
    void findById() {
        Optional<Folder> retrieved = folderDao.findById(1);
        assertTrue(retrieved.isPresent());
        assertEquals(retrieved.get().getId(), 1);
        assertEquals(retrieved.get().getTitle(), "Sonair");
    }

    @Test
    void findByGroupAndRootFolder() {
        List<Folder> retrieved = folderDao.findByGroupAndRootFolder(1);
        assertEquals(3, retrieved.size());
        assertArrayEquals(new Integer[] {1, 2, 3}, retrieved.stream().map(Folder::getId).toArray(Integer[]::new));
    }

    @Test
    void findByGroupAndFolder() {
        List<Folder> retrieved = folderDao.findByGroupAndFolder(1, 1);
        assertEquals(3, retrieved.size());
        assertArrayEquals(new Integer[] {4, 5, 10}, retrieved.stream().map(Folder::getId).toArray(Integer[]::new));
    }

    @Test
    void findByPrivateGroupAndRootFolder() {
        List<Folder> retrieved = folderDao.findByPrivateGroupAndRootFolder(1);
        assertEquals(1, retrieved.size());
        assertEquals(6, retrieved.getFirst().getId());
        assertEquals(0, retrieved.getFirst().getGroupId());
        assertEquals(0, retrieved.getFirst().getParentFolderId());
    }

    @Test
    void findByPrivateGroupAndFolder() {
        List<Folder> retrieved = folderDao.findByPrivateGroupAndFolder(6, 1);
        assertEquals(2, retrieved.size());
        assertArrayEquals(new Integer[] {7, 8}, retrieved.stream().map(Folder::getId).toArray(Integer[]::new));
    }

    @Test
    void deleteSoft() {
        Optional<Folder> retrievedOld = folderDao.findById(1);
        assertTrue(retrievedOld.isPresent());
        assertFalse(retrievedOld.get().isDeleted());

        folderDao.deleteSoft(retrievedOld.get());

        Optional<Folder> retrieved = folderDao.findById(1);
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().isDeleted());
    }

    @Test
    void save() {
        Folder folder = new Folder();
        folder.setGroupId(0);
        folder.setParentFolderId(6);
        folder.setTitle("private stuff");
        folder.setDescription("myFolder");
        folder.setUser(userDao.findByIdOrElseThrow(1));
        folderDao.save(folder);
        FolderDao.cache.clear();

        Optional<Folder> retrieved = folderDao.findById(folder.getId());
        assertTrue(retrieved.isPresent());
        assertNotSame(folder, retrieved.get());
        assertEquals(folder.getId(), retrieved.get().getId());
        assertEquals(folder.getDescription(), retrieved.get().getDescription());
        assertEquals(folder.getParentFolderId(), retrieved.get().getParentFolderId());
        assertEquals(folder.getGroupId(), retrieved.get().getGroupId());
        assertEquals(folder.getCreatedAt(), retrieved.get().getCreatedAt());
        assertEquals(folder.getTitle(), retrieved.get().getTitle());

        folder.setTitle("new title");
        folderDao.save(folder);
        FolderDao.cache.clear();

        Optional<Folder> updated = folderDao.findById(folder.getId());
        assertTrue(updated.isPresent());
        assertNotSame(folder, retrieved.get());
        assertEquals(folder.getTitle(), updated.get().getTitle());
    }
}

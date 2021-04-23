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
        assertFalse(retrieved.get().isDeleted());
    }

    @Test
    void findByGroupAndRootFolder() {
        List<Folder> retrieved = folderDao.findByGroupAndRootFolder(1);
        assertNotNull(retrieved);
        assertEquals(3, retrieved.size());
        assertEquals(1, retrieved.get(0).getId());
        assertEquals(0.0, retrieved.get(1).getParentFolderId());
    }

    @Test
    void findByGroupAndFolder() { // difference
        List<Folder> retrieved = folderDao.findByGroupAndFolder(1, 1);
        assertNotNull(retrieved);
        assertEquals(3, retrieved.size());
        assertFalse(retrieved.get(0).isDeleted());
        assertEquals(1, retrieved.get(1).getParentFolderId());
    }

    @Test
    void findByPrivateGroupAndRootFolder() {
        List<Folder> retrieved = folderDao.findByPrivateGroupAndRootFolder(1);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        assertFalse(retrieved.get(0).isDeleted());
        assertEquals(0, retrieved.get(0).getGroupId());
        assertEquals(0, retrieved.get(0).getParentFolderId());
    }

    @Test
    void findByPrivateGroupAndFolder() {
        List<Folder> retrieved = folderDao.findByPrivateGroupAndFolder(6, 1);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        assertEquals(6, retrieved.get(0).getParentFolderId());
        assertEquals("A hidden folder", retrieved.get(1).getDescription());
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
        folder.setDeleted(false);
        folder.setParentFolderId(1);
        folder.setDescription("myFolder");
        folder.setId(11);
        folder.setGroupId(1);
        folder.setTitle("private stuff");
        folder.setUser(userDao.findByIdOrElseThrow(1));
        folderDao.save(folder);
        FolderDao.cache.clear();

        Optional<Folder> retrieved = folderDao.findById(11);
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

        Optional<Folder> updated = folderDao.findById(11);
        assertTrue(updated.isPresent());
        assertEquals(folder.getTitle(), updated.get().getTitle());
    }
}

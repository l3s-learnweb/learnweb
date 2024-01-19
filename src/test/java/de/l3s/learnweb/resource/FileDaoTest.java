package de.l3s.learnweb.resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.test.LearnwebExtension;

class FileDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final FileDao fileDao = learnwebExt.attach(FileDao.class);
    private final ResourceDao resourceDao = learnwebExt.attach(ResourceDao.class);

    @Test
    void findById() {
        Optional<File> retrieved = fileDao.findById(1, true);
        assertTrue(retrieved.isPresent());
        assertEquals("thumbnail4.png", retrieved.get().getName());
    }

    @Test
    void findByIdOrElseThrow() {
        assertThrows(NotFoundHttpException.class, () -> fileDao.findByIdOrElseThrow(1));
    }

    @Test
    void findAll() {
        Stream<File> retrieved = fileDao.findAll();
        Optional<File> file = fileDao.findById(1, true);
        assertTrue(file.isPresent());
        assertEquals(file.get(), retrieved.iterator().next());
    }

    @Test
    void findByResourceId() {
        List<File> retrieved = fileDao.findByResourceId(1);
        Optional<File> file1 = fileDao.findById(1, true);
        Optional<File> file2 = fileDao.findById(2, true);
        Optional<File> file3 = fileDao.findById(3, true);
        assertTrue(file1.isPresent());
        assertTrue(file2.isPresent());
        assertTrue(file3.isPresent());
        List<File> expected = Arrays.asList(file3.get(), file2.get(), file1.get());

        assertTrue(CollectionUtils.isEqualCollection(expected, retrieved));
    }

    @Test
    void insertResourceFiles() {
        Optional<File> file4 = fileDao.findById(4, true);
        Optional<File> file5 = fileDao.findById(5, true);
        assertTrue(file4.isPresent());
        assertTrue(file5.isPresent());
        List<File> toInsert = new ArrayList<>(Arrays.asList(file5.get(), file4.get()));
        Optional<Resource> resource = resourceDao.findById(1);
        assertTrue(resource.isPresent());
        fileDao.insertResourceFiles(resource.get(), toInsert);

        List<File> retrieved = fileDao.findByResourceId(1);

        assertTrue(CollectionUtils.containsAll(retrieved, toInsert));
    }

    @Test
    void deleteResourceFiles() {
        Optional<File> file1 = fileDao.findById(1, true);
        assertTrue(file1.isPresent());
        List<File> toDelete = Collections.singletonList(file1.get());
        Optional<Resource> resource = resourceDao.findById(1);
        assertTrue(resource.isPresent());
        fileDao.deleteResourceFiles(resource.get(), toDelete);

        List<File> retrieved = fileDao.findByResourceId(1);

        assertEquals(0, CollectionUtils.intersection(retrieved, toDelete).size());
    }

    @Test
    void deleteHard() {
        Optional<File> file2 = fileDao.findById(2, true);
        assertTrue(file2.isPresent());
        fileDao.deleteHard(file2.get());
        Optional<File> deletedFile2 = fileDao.findById(2, true);
        assertFalse(deletedFile2.isPresent());
    }

    @Test
    void save() {
        Optional<File> file1 = fileDao.findById(1, true);
        assertTrue(file1.isPresent());
        File file = new File(file1.get().getType(), "newFile", "image/png");
        fileDao.save(file);

        Optional<File> retrieved = fileDao.findById(file.getId(), true);
        assertTrue(retrieved.isPresent());
        assertEquals(file.getCreatedAt(), retrieved.get().getCreatedAt());
        assertEquals(file.getName(), retrieved.get().getName());
        assertEquals(file.getMimeType(), retrieved.get().getMimeType());
    }
}

package de.l3s.learnweb.user;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

class CourseDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final CourseDao courseDao = learnwebExt.attach(CourseDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);

    @BeforeEach
    void setUp() {
        CourseDao.cache.clear();
    }

    @Test
    void findById() {
        Optional<Course> course = courseDao.findById(1);
        assertTrue(course.isPresent());
        assertEquals("Public", course.get().getTitle());
        assertEquals(1, course.get().getOrganisationId());
        assertEquals(0, course.get().getDefaultGroupId());
        assertEquals("public", course.get().getRegistrationWizard());
        assertEquals(0, course.get().getNextXUsersBecomeModerator());
        assertEquals("Welcome to the public course", course.get().getWelcomeMessage());
        assertEquals(LocalDateTime.of(2019, 11, 27, 20, 38, 7), course.get().getCreatedAt());
        assertEquals(5, course.get().getMemberCount());
    }

    @Test
    void findByWizard() {
        Optional<Course> courseByWizard = courseDao.findByWizard("public");
        assertTrue(courseByWizard.isPresent());
        assertEquals(1, courseByWizard.get().getId());
        assertEquals("Public", courseByWizard.get().getTitle());
    }

    @Test
    void findAll() {
        List<Course> allCourse = courseDao.findAll();
        assertFalse(allCourse.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, allCourse.stream().map(Course::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByOrganisationId() {
        List<Course> coursesByOrganisationId = courseDao.findByOrganisationId(1);
        assertFalse(coursesByOrganisationId.isEmpty());
        assertArrayEquals(new Integer[] {1, 4, 5, 6, 9, 10}, coursesByOrganisationId.stream().map(Course::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByUserId() {
        List<Course> coursesByUserId = courseDao.findByUserId(1);
        assertFalse(coursesByUserId.isEmpty());
        assertArrayEquals(new Integer[] {1}, coursesByUserId.stream().map(Course::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void insertUser() {
        //We check if the list of courses by userID is different after insertion
        Optional<Course> course = courseDao.findById(1);
        User testUser = new User();
        testUser.setFullName("Rob Outlaw");
        testUser.setId(4);
        List<Course> coursesBefore = courseDao.findByUserId(testUser.getId());
        courseDao.insertUser(course.orElseThrow(), testUser);

        List<Course> coursesAfter = courseDao.findByUserId(testUser.getId());
        assertFalse(coursesAfter.isEmpty());
        assertNotEquals(coursesBefore.size(), coursesAfter.size());
    }

    @Test
    void deleteUser() {
        //We check if the list of courses by userID is different after deletion
        Optional<Course> course = courseDao.findById(1);
        User testUser = new User();
        testUser.setFullName("Rob Outlaw");
        testUser.setId(4);
        //Assuming insertUser is correct
        courseDao.insertUser(course.orElseThrow(), testUser);
        List<Course> coursesBefore = courseDao.findByUserId(testUser.getId());
        courseDao.deleteUser(course.orElseThrow(), testUser);

        List<Course> coursesAfter = courseDao.findByUserId(testUser.getId());
        assertFalse(coursesAfter.isEmpty());
        assertNotEquals(coursesBefore.size(), coursesAfter.size());
    }

    @Test
    void save() {
        Course course = new Course();
        course.setId(10);
        course.setTitle("Test");
        course.setOrganisationId(1);
        course.setDefaultGroupId(2);
        course.setWelcomeMessage("Welcome to the public course");
        course.setRegistrationWizard("test");
        course.setNextXUsersBecomeModerator(2);
        course.setOption(Course.Option.Users_Require_affiliation, true);
        courseDao.save(course);

        Optional<Course> retrievedCourse = courseDao.findById(10);
        assertTrue(retrievedCourse.isPresent());
        assertEquals("Test", retrievedCourse.get().getTitle());
        assertEquals(1, retrievedCourse.get().getOrganisationId());
        assertEquals("test", retrievedCourse.get().getRegistrationWizard());
        assertEquals(2, retrievedCourse.get().getNextXUsersBecomeModerator());
        assertEquals("Welcome to the public course", retrievedCourse.get().getWelcomeMessage());
        assertTrue(retrievedCourse.get().getOption(Course.Option.Users_Require_affiliation));
    }

    @Test
    void deleteHard() {
        Optional<Course> course = courseDao.findById(7);
        assertTrue(course.isPresent());

        courseDao.deleteHard(course.get(), false);

        Optional<Course> retrieved = courseDao.findById(7);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    void anonymize() {
        List<User> users = userDao.findByCourseId(1);
        assertFalse(users.isEmpty());
        assertFalse(users.stream().map(User::getUsername).anyMatch(username -> username.startsWith("Anonym")));

        courseDao.anonymize(courseDao.findByIdOrElseThrow(1));

        List<User> anonymUsers = userDao.findByCourseId(1);
        assertFalse(anonymUsers.isEmpty());
        assertTrue(anonymUsers.stream().map(User::getUsername).allMatch(username -> username.startsWith("Anonym")));
    }
}

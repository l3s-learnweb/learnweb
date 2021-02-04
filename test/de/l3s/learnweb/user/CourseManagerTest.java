package de.l3s.learnweb.user;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.l3s.learnweb.Learnweb;

class CourseManagerTest {
    private final Learnweb learnweb = Learnweb.createInstance();
    private final CourseManager courseManager = new CourseManager(learnweb);

    CourseManagerTest() throws SQLException, ClassNotFoundException {}

    @Test
    void resetCache() {
        int expected = 36;
        assertEquals(expected, courseManager.getCacheSize());
    }

    @Test
    void getCoursesByOrganisationId() {
        ArrayList<String> expected = new ArrayList<>(Arrays.asList("LogCanvasBrasilia", "LogCanvasCairo", "LogCanvasBerlin",
            "LogCanvasBeijing", "LogCanvasBrisbane", "LogCanvasTag 1"));
        List<Course> result = courseManager.getCoursesByOrganisationId(1518);
        assertTrue(result.stream().allMatch(org -> expected.contains(org.getTitle())));
    }

    @Test
    void getCoursesByUserId() throws SQLException {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(485, 1542));
        List<Course> result = courseManager.getCoursesByUserId(12502);
        assertTrue(result.stream().allMatch(course -> expected.contains(course.getId())));
    }

    @Test
    void saveUpdate() throws SQLException {
        learnweb.getConnection().setAutoCommit(false);
        try {
            Course expected = learnweb.getCourseManager().getCourseById(1542);
            expected.setTitle("test");
            expected.setOrganisationId(495);
            learnweb.getCourseManager().save(expected);
            Course result = learnweb.getCourseManager().getCourseById(1542);
            assertEquals(expected.getId(), result.getId());
            assertEquals(expected.getTitle(), result.getTitle());
            assertEquals(expected.getOrganisationId(), result.getOrganisationId());
        } finally {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void saveCreate() throws SQLException {
        learnweb.getConnection().setAutoCommit(false);
        try {
            Course expected = new Course();
            expected.setTitle("test");
            expected.setOrganisationId(495);
            expected.setWelcomeMessage("message");
            learnweb.getCourseManager().save(expected);


            PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_course` WHERE `title` = ? AND `organisation_id` = ?");
            select.setString(1, expected.getTitle());
            select.setInt(2, expected.getOrganisationId());
            ResultSet rs = select.executeQuery();
            rs.next();
            select.close();

            assertEquals(expected.getWelcomeMessage(), rs.getString("welcome_message"));
            assertEquals(expected.getTitle(), rs.getString("title"));
            assertEquals(expected.getOrganisationId(), rs.getInt("organisation_id"));
        } finally {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void addUser() throws SQLException {
        learnweb.getConnection().setAutoCommit(false);
        try {
            User user = new User();
            user.setId(12502);
            Course course = new Course();
            course.setId(1542);
            learnweb.getCourseManager().addUser(course, user);
            try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_user_course` WHERE user_id= ? and course_id = ?")) {
                select.setInt(1, user.getId());
                select.setInt(2, course.getId());
                ResultSet rs = select.executeQuery();
                assertTrue(rs.next());
            }

        } finally {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void removeUser() throws SQLException {
        learnweb.getConnection().setAutoCommit(false);
        try {
            User user = new User();
            user.setId(12502);
            Course course = new Course();
            course.setId(1542);
            learnweb.getCourseManager().removeUser(course, user);
            try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_user_course` WHERE user_id= ? and course_id = ?")) {
                select.setInt(1, user.getId());
                select.setInt(2, course.getId());
                ResultSet rs = select.executeQuery();
                assertFalse(rs.next());
            }

        } finally {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }
}

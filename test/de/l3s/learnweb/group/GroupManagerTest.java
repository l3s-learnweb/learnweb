package de.l3s.learnweb.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;

@Disabled
class GroupManagerTest {
    private final Learnweb learnweb = Learnweb.createInstance();

    GroupManagerTest() throws SQLException, ClassNotFoundException {
    }

    @Test
    void getGroupsByUserId() throws SQLException {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(185, 623, 896, 1307, 1309, 1434, 1463));
        List<Group> result = learnweb.getGroupManager().getGroupsByUserId(12502);
        assertTrue(result.stream().allMatch(group -> expected.contains(group.getId())));
    }

    @Test
    void getGroupsByCourseId() throws SQLException {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(894, 896, 904, 1158, 1160, 1346));
        List<Group> result = learnweb.getGroupManager().getGroupsByCourseId(892);
        assertTrue(result.stream().allMatch(group -> expected.contains(group.getId())));
    }

    @Test
    void testGetGroupsByCourseId() throws SQLException {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(1559, 1543, 1463, 1309, 1537));
        List<Course> courses = learnweb.getCourseManager().getCoursesByUserId(12502);
        Calendar date = Calendar.getInstance();
        date.set(2020, Calendar.FEBRUARY, 1);
        List<Group> result = learnweb.getGroupManager().getGroupsByCourseId(courses, date.toInstant());
        assertTrue(result.stream().allMatch(group -> expected.contains(group.getId())));
    }

    @Test
    void getGroupsByUserIdFilteredByCourseId() throws SQLException {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(623, 1463, 1309, 185, 1307));
        List<Group> result = learnweb.getGroupManager().getGroupsByUserIdFilteredByCourseId(12502, 485);
        assertTrue(result.stream().allMatch(group -> expected.contains(group.getId())));
    }

    @Test
    void getGroupByTitleFilteredByOrganisation() throws SQLException {
        String expected = "EU-MADE4LL project";
        Group result = learnweb.getGroupManager().getGroupByTitleFilteredByOrganisation(expected, 1249);
        assertEquals(expected, result.getTitle());
    }

    @Test
    void getJoinAbleGroups() throws SQLException {
        int expected = 27;
        List<Group> result = learnweb.getGroupManager().getJoinAbleGroups(learnweb.getUserManager().getUser(12502));
        assertEquals(expected, result.size());
    }

    @Test
    void getGroupById() throws SQLException {
        String expected = "Learning Apps";
        Group result = learnweb.getGroupManager().getGroupById(1283);
        assertEquals(expected, result.getTitle());
    }

    @Test
    void save() throws SQLException {
        learnweb.getConnection().setAutoCommit(false);
        try {
            Group expected = learnweb.getGroupManager().getGroupById(1463);
            expected.setTitle("AleksTest");
            expected.setDescription("desc");
            learnweb.getGroupManager().save(expected);
            Group result = learnweb.getGroupManager().getGroupById(1463);
            assertEquals(expected.getTitle(), result.getTitle());
            assertEquals(expected.getDescription(), result.getDescription());
        } finally {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void addUserToGroup() throws SQLException {
        learnweb.getConnection().setAutoCommit(false);
        try {
            User user = learnweb.getUserManager().getUser(5267);
            Group group = learnweb.getGroupManager().getGroupById(1463);
            learnweb.getGroupManager().addUserToGroup(user, group);

            PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
            select.setInt(1, group.getId());
            select.setInt(2, user.getId());
            ResultSet rs = select.executeQuery();
            rs.next();
            select.close();

            assertEquals(user.getId(), rs.getInt("user_id"));
        } finally {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void removeUserFromGroup() throws SQLException {
        learnweb.getConnection().setAutoCommit(false);
        try {
            User user = learnweb.getUserManager().getUser(12502);
            Group group = learnweb.getGroupManager().getGroupById(1463);
            learnweb.getGroupManager().removeUserFromGroup(user, group);

            PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
            select.setInt(1, group.getId());
            select.setInt(2, user.getId());
            ResultSet rs = select.executeQuery();
            select.close();
            assertFalse(rs.next());
        } finally {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void getMemberCount() throws SQLException {
        int expected = 3;
        int result = learnweb.getGroupManager().getMemberCount(1463);
        assertEquals(expected, result);
    }
}

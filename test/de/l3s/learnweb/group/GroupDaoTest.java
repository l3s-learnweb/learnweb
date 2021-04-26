package de.l3s.learnweb.group;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.test.LearnwebExtension;

class GroupDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final GroupDao groupDao = learnwebExt.attach(GroupDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);

    @Test
    void findById() {
        Optional<Group> group = groupDao.findById(1);
        assertTrue(group.isPresent());
        assertEquals(1, group.get().getId());
    }

    @Test
    void findAll() {
        List<Group> groups = groupDao.findAll();
        assertFalse(groups.isEmpty());
        assertEquals(10, groups.size());
    }

    @Test
    void findByUserId() {
        List<Group> groups = groupDao.findByUserId(1);
        assertFalse(groups.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 10}, groups.stream().map(Group::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByCourseId() {
        List<Group> groups = groupDao.findByCourseId(1);
        assertFalse(groups.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 6, 7, 8, 9, 10}, groups.stream().map(Group::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByUserIdAndCourseId() {
        List<Group> groups = groupDao.findByUserIdAndCourseId(1, 1);
        assertFalse(groups.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 10}, groups.stream().map(Group::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByCourseIds() {
        List<Group> groups = groupDao.findByCourseIds(Collections.singletonList(2), LocalDateTime.of(2019, 1, 1, 0, 0, 0));
        assertFalse(groups.isEmpty());
        assertArrayEquals(new Integer[] {5}, groups.stream().map(Group::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByTitleAndOrganisationId() {
        assertTrue(groupDao.findByTitleAndOrganisationId("Ut ut et", 1).isPresent());
        assertFalse(groupDao.findByTitleAndOrganisationId("Ut ut", 1).isPresent());
    }

    @Test
    void findGroupUserRelation() {
        Optional<GroupUser> groupUser = groupDao.findGroupUserRelation(groupDao.findByIdOrElseThrow(1), userDao.findByIdOrElseThrow(4));
        assertTrue(groupUser.isPresent());

        Optional<GroupUser> groupUser2 = groupDao.findGroupUserRelation(groupDao.findByIdOrElseThrow(1), userDao.findByIdOrElseThrow(2));
        assertFalse(groupUser2.isPresent());
    }

    @Test
    void findGroupUserRelations() {
        List<GroupUser> groupUsers = groupDao.findGroupUserRelations(1);
        assertArrayEquals(new Integer[] {1, 2, 10}, groupUsers.stream().map(GroupUser::getGroupId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findLastVisitTime() {
        Optional<Instant> visitTime = groupDao.findLastVisitTime(groupDao.findByIdOrElseThrow(1), userDao.findByIdOrElseThrow(4));
        assertTrue(visitTime.isPresent());
        assertEquals(Instant.ofEpochSecond(1613755967), visitTime.get());
    }

    @Test
    void countMembers() {
        assertEquals(3, groupDao.countMembers(1));
    }

    @Test
    void findJoinAble() {
        List<Group> joinAbles = groupDao.findJoinAble(userDao.findByIdOrElseThrow(4));
        assertFalse(joinAbles.isEmpty());
        assertArrayEquals(new Integer[] {4, 5}, joinAbles.stream().map(Group::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void updateNotificationFrequency() {
        Group group = groupDao.findByIdOrElseThrow(1);
        User user = userDao.findByIdOrElseThrow(4);

        Optional<GroupUser> groupUser = groupDao.findGroupUserRelation(group, user);
        assertTrue(groupUser.isPresent());
        assertEquals(groupUser.get().getNotificationFrequency(), User.NotificationFrequency.NEVER);

        groupDao.updateNotificationFrequency(User.NotificationFrequency.DAILY, 1, 4);
        Optional<GroupUser> lateGroupUser = groupDao.findGroupUserRelation(group, user);
        assertTrue(lateGroupUser.isPresent());
        assertEquals(lateGroupUser.get().getNotificationFrequency(), User.NotificationFrequency.DAILY);
    }

    @Test
    void insertLastVisitTime() {
        Group group = groupDao.findByIdOrElseThrow(1);
        User user = userDao.findByIdOrElseThrow(4);

        Optional<Instant> visitTime = groupDao.findLastVisitTime(group, user);
        assertTrue(visitTime.isPresent());

        groupDao.insertLastVisitTime(Instant.now(), group, user);

        Optional<Instant> lateVisitTime = groupDao.findLastVisitTime(group, user);
        assertTrue(lateVisitTime.isPresent());
        assertNotEquals(visitTime.get(), lateVisitTime.get());
    }

    @Test
    void insertUser() {
        Group group = groupDao.findByIdOrElseThrow(5);
        User user = userDao.findByIdOrElseThrow(5);

        assertFalse(groupDao.findGroupUserRelation(group, user).isPresent());
        groupDao.insertUser(group.getId(), user, User.NotificationFrequency.WEEKLY);
        assertTrue(groupDao.findGroupUserRelation(group, user).isPresent());
    }

    @Test
    void deleteUser() {
        Group group = groupDao.findByIdOrElseThrow(4);
        User user = userDao.findByIdOrElseThrow(6);

        assertTrue(groupDao.findGroupUserRelation(group, user).isPresent());
        groupDao.deleteUser(group.getId(), user);
        assertFalse(groupDao.findGroupUserRelation(group, user).isPresent());
    }

    @Test
    void deleteSoft() {
        groupDao.deleteHard(groupDao.findByIdOrElseThrow(3));
        assertTrue(groupDao.findById(3).isEmpty());
    }

    @Test
    void deleteHard() {
        groupDao.deleteHard(groupDao.findByIdOrElseThrow(3));
        assertTrue(groupDao.findById(3).isEmpty());
    }

    @Test
    void save() {
        Group group = new Group();
        group.setTitle("TestABC");
        group.setCourseId(3);
        group.setLeaderUserId(4);
        groupDao.save(group);

        Optional<Group> retrieved = groupDao.findByTitleAndOrganisationId("TestABC", 2);
        assertTrue(retrieved.isPresent());
    }
}

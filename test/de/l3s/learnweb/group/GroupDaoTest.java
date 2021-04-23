package de.l3s.learnweb.group;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.test.LearnwebExtension;

class GroupDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final GroupDao groupDao = learnwebExt.attach(GroupDao.class);
    private final UserDao userDao = learnwebExt.attach(UserDao.class);

    @Test
    void getResourceDao() {
        ResourceDao resourceDao = groupDao.getResourceDao();
        Optional<Resource> resource = resourceDao.findById(1);
        assertTrue(resource.isPresent());
        assertEquals(1, resource.get().getId());
    }

    @Test
    void findById() {
        Optional<Group> group = groupDao.findById(1);
        assertTrue(group.isPresent());
        assertEquals(1, group.get().getId());
    }

    //FIXME
    @Test
    void findByIdOrElseThrow() {

        //assertThrows(() -> groupDao.findByIdOrElseThrow(51));
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
        Optional<Group> group = groupDao.findById(1);
        assertTrue(group.isPresent());
        Optional<User> user = userDao.findById(4);
        assertTrue(user.isPresent());
        Optional<GroupUser> groupUser = groupDao.findGroupUserRelation(group.get(), user.get());
        assertTrue(groupUser.isPresent());
    }

    @Test
    void findGroupUserRelations() {
        List<GroupUser> groupUsers = groupDao.findGroupUserRelations(1);
        assertArrayEquals(new Integer[] {1, 2, 10}, groupUsers.stream().map(GroupUser::getGroupId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findLastVisitTime() {
        Optional<Group> group = groupDao.findById(1);
        assertTrue(group.isPresent());
        Optional<User> user = userDao.findById(4);
        assertTrue(user.isPresent());
        Optional<Instant> visitTime = groupDao.findLastVisitTime(group.get(), user.get());
        assertTrue(visitTime.isPresent());
    }

    @Test
    void countMembers() {
        int count = groupDao.countMembers(4);
        assertEquals(1, count);
    }

    @Test
    void findJoinAble() {
        Optional<User> user = userDao.findById(4);
        assertTrue(user.isPresent());
        List<Group> joinAbles = groupDao.findJoinAble(user.get());
        assertFalse(joinAbles.isEmpty());
        assertArrayEquals(new Integer[] {4, 5}, joinAbles.stream().map(Group::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void updateNotificationFrequency() {
        Optional<Group> group = groupDao.findById(1);
        assertTrue(group.isPresent());
        Optional<User> user = userDao.findById(4);
        assertTrue(user.isPresent());
        Optional<GroupUser> groupUser = groupDao.findGroupUserRelation(group.get(), user.get());
        assertTrue(groupUser.isPresent());
        User.NotificationFrequency frequency = groupUser.get().getNotificationFrequency();
        groupDao.updateNotificationFrequency(User.NotificationFrequency.DAILY, 1, 4);
        Optional<GroupUser> lateGroupUser = groupDao.findGroupUserRelation(group.get(), user.get());
        assertTrue(lateGroupUser.isPresent());
        User.NotificationFrequency lateFrequency = lateGroupUser.get().getNotificationFrequency();
        assertNotEquals(frequency, lateFrequency);
    }

    @Test
    void insertLastVisitTime() {
        Optional<Group> group = groupDao.findById(1);
        assertTrue(group.isPresent());
        Optional<User> user = userDao.findById(4);
        assertTrue(user.isPresent());
        Optional<Instant> visitTime = groupDao.findLastVisitTime(group.get(), user.get());
        assertTrue(visitTime.isPresent());
        groupDao.insertLastVisitTime(Instant.now(), group.get(), user.get());
        Optional<Instant> lateVisitTime = groupDao.findLastVisitTime(group.get(), user.get());
        assertNotEquals(visitTime, lateVisitTime);
    }

    @Test
    void insertUser() {
        Optional<Group> group = groupDao.findById(5);
        assertTrue(group.isPresent());
        Optional<User> user = userDao.findById(5);
        assertTrue(user.isPresent());
        Optional<GroupUser> groupUser = groupDao.findGroupUserRelation(group.get(), user.get());
        assertFalse(groupUser.isPresent());
        groupDao.insertUser(group.get().getId(), user.get(), User.NotificationFrequency.WEEKLY);
        groupUser = groupDao.findGroupUserRelation(group.get(), user.get());
        assertTrue(groupUser.isPresent());
    }

    @Test
    void deleteUser() {
        Optional<Group> group = groupDao.findById(4);
        assertTrue(group.isPresent());
        Optional<User> user = userDao.findById(6);
        assertTrue(user.isPresent());
        Optional<GroupUser> groupUser = groupDao.findGroupUserRelation(group.get(), user.get());
        assertTrue(groupUser.isPresent());
        groupDao.deleteUser(group.get().getId(), user.get());
        groupUser = groupDao.findGroupUserRelation(group.get(), user.get());
        assertFalse(groupUser.isPresent());
    }

    @Test
    void deleteSoft() {
        Optional<Group> group = groupDao.findById(3);
        assertTrue(group.isPresent());

        groupDao.deleteHard(group.get());

        Optional<Group> retrieved = groupDao.findById(3);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    void deleteHard() {
        Optional<Group> group = groupDao.findById(3);
        assertTrue(group.isPresent());

        groupDao.deleteHard(group.get());

        Optional<Group> retrieved = groupDao.findById(3);
        assertTrue(retrieved.isEmpty());
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

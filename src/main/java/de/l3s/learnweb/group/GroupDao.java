package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;
import de.l3s.util.Cache;
import de.l3s.util.HasId;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(GroupDao.GroupMapper.class)
@RegisterRowMapper(GroupDao.GroupUserMapper.class)
public interface GroupDao extends SqlObject, Serializable {
    ICache<Group> cache = new Cache<>(500);

    @CreateSqlObject
    ResourceDao getResourceDao();

    default Optional<Group> findById(int groupId) {
        return Optional.ofNullable(cache.get(groupId))
            .or(() -> getHandle().select("SELECT * FROM lw_group WHERE group_id = ?", groupId).mapTo(Group.class).findOne());
    }

    default Group findByIdOrElseThrow(int groupId) {
        return findById(groupId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_group_description"));
    }

    @SqlQuery("SELECT * FROM lw_group")
    List<Group> findAll();

    /**
     * Returns a list of all Groups a user belongs to.
     */
    @SqlQuery("SELECT g.* FROM lw_group g JOIN lw_group_user u USING(group_id) WHERE u.user_id = ? ORDER BY title")
    List<Group> findByUserId(int userid);

    /**
     * Returns a list of all Groups which belong to the defined course.
     */
    @SqlQuery("SELECT * FROM lw_group WHERE course_id = ? AND deleted = 0 ORDER BY title")
    List<Group> findByCourseId(int courseId);

    @SqlQuery("SELECT * FROM lw_group WHERE course_id = ?")
    List<Group> findAllByCourseId(int courseId);

    /**
     * Returns a list of all Groups a user belongs to and which groups are also part of the defined course.
     */
    @SqlQuery("SELECT g.* FROM lw_group g JOIN lw_group_user USING(group_id) WHERE user_id = ? AND g.course_id = ? AND g.deleted = 0 ORDER BY title")
    List<Group> findByUserIdAndCourseId(int userid, int courseId);

    /**
     * Returns a list of Groups which belong to the defined courses and were created after the specified date.
     */
    @SqlQuery("SELECT * FROM lw_group g WHERE g.course_id IN(<courseIds>) AND g.deleted = 0 AND created_at > :time ORDER BY title")
    List<Group> findByCourseIds(@BindList("courseIds") Collection<Integer> courseIds, @Bind("time") LocalDateTime newerThan);


    @SqlQuery("SELECT g.* FROM lw_group g JOIN lw_course gc USING(course_id) WHERE g.title LIKE ? AND organisation_id = ? AND g.deleted = 0")
    Optional<Group> findByTitleAndOrganisationId(String title, int organisationId);

    /**
     * Returns a group by user with notification frequency.
     */
    @SqlQuery("SELECT * FROM lw_group_user WHERE group_id = ? AND user_id = ?")
    Optional<GroupUser> findGroupUserRelation(Group group, User user);

    /**
     * Returns a list of all Groups a user belongs to with associated metadata like notification frequency.
     */
    @SqlQuery("SELECT * FROM lw_group_user WHERE user_id = ?")
    List<GroupUser> findGroupUserRelations(int userId);

    /**
     * @return unix timestamp when the user has visited the group the last time; returns -1 if they never viewed the group.
     */
    @SqlQuery("SELECT last_visit FROM lw_group_user WHERE group_id = ? AND user_id = ?")
    Optional<Instant> findLastVisitTime(Group group, User user);

    @SqlQuery("SELECT count(*) FROM lw_group_user WHERE group_id = ?")
    int countMembers(int groupId);

    /**
     * Returns all groups a user can join.
     * These are all groups of his courses except for groups he has already joined + groups that are open to everybody.
     */
    default List<Group> findJoinAble(User user) {
        ArrayList<Integer> coursesIn = HasId.collectIds(user.getCourses());
        ArrayList<Integer> groupsIn = HasId.collectIds(user.getGroups());
        groupsIn.add(0); // make sure that the list is not empty

        return getHandle().createQuery("SELECT g.* FROM lw_group g JOIN lw_course USING(course_id) WHERE g.deleted = 0 AND g.group_id NOT IN(<groupsIn>) "
            + "AND (g.policy_join = 'COURSE_MEMBERS' AND g.course_id IN(<coursesIn>) <showPublic> OR g.policy_join = 'ORGANISATION_MEMBERS' "
            + "AND organisation_id = :orgId) ORDER BY title")
            .define("showPublic", user.getOrganisation().getOption(Organisation.Option.Groups_Hide_public_groups) ? "" : "OR policy_join = 'ALL_LEARNWEB_USERS'")
            .defineList("groupsIn", groupsIn)
            .defineList("coursesIn", coursesIn)
            .bind("orgId", user.getOrganisationId())
            .map(new GroupMapper()).list();
    }

    /**
     * Saves the setting that defines how often a user will retrieve notifications for the given group.
     */
    @SqlUpdate("UPDATE lw_group_user SET notification_frequency= ? WHERE group_id= ? AND user_id= ?")
    void updateNotificationFrequency(User.NotificationFrequency notificationFrequency, int groupId, int userId);

    /**
     * Define at which timestamp the user has visited the group the last time.
     */
    @SqlUpdate("UPDATE lw_group_user SET last_visit = ? WHERE group_id = ? AND user_id = ?")
    void insertLastVisitTime(Instant visit, Group group, User user);

    @SqlUpdate("INSERT IGNORE INTO lw_group_user (group_id, user_id, notification_frequency) VALUES (?,?,?)")
    void insertUser(int groupId, User user, User.NotificationFrequency notificationFrequency);

    @SqlUpdate("DELETE FROM lw_group_user WHERE group_id = ? AND user_id = ?")
    void deleteUser(int groupId, User user);

    /**
     * Flags the group as deleted and removes all users from the group.
     */
    default void deleteSoft(Group group) {
        for (Resource resource : group.getResources()) {
            getResourceDao().deleteSoft(resource);
        }

        List<User> members = group.getMembers();
        getHandle().execute("DELETE FROM lw_group_user WHERE group_id = ?", group);
        getHandle().execute("UPDATE lw_group SET deleted = 1 WHERE group_id = ?", group);
        group.setDeleted(true);

        members.forEach(User::clearCaches);
        cache.remove(group.getId());
    }

    /**
     * Deletes the group and all its resources permanently. Don't use this if you don't know exactly what you are doing!
     */
    default void deleteHard(Group group) {
        for (Resource resource : getResourceDao().findAllByGroupId(group.getId())) {
            getResourceDao().deleteHard(resource);
        }

        List<User> members = group.getMembers();
        getHandle().execute("DELETE FROM lw_group WHERE group_id = ?", group);

        members.forEach(User::clearCaches);
        cache.remove(group.getId());
    }

    default void save(Group group) {
        if (group.getCreatedAt() == null) {
            group.setCreatedAt(SqlHelper.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("group_id", SqlHelper.toNullable(group.getId()));
        params.put("deleted", group.isDeleted());
        params.put("title", group.getTitle());
        params.put("description", SqlHelper.toNullable(group.getDescription()));
        params.put("image_file_id", SqlHelper.toNullable(group.getImageFileId()));
        params.put("leader_id", group.getLeaderUserId());
        params.put("course_id", group.getCourseId());
        params.put("restriction_forum_category_required", group.isRestrictionForumCategoryRequired());
        params.put("policy_add", group.getPolicyAdd().name());
        params.put("policy_annotate", group.getPolicyAnnotate().name());
        params.put("policy_edit", group.getPolicyEdit().name());
        params.put("policy_join", group.getPolicyJoin().name());
        params.put("policy_view", group.getPolicyView().name());
        params.put("created_at", group.getCreatedAt());

        Optional<Integer> groupId = SqlHelper.handleSave(getHandle(), "lw_group", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (groupId.isPresent() && groupId.get() != 0) {
            group.setId(groupId.get());
            cache.put(group);
        }
    }

    class GroupMapper implements RowMapper<Group> {
        @Override
        public Group map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Group group = cache.get(rs.getInt("group_id"));

            if (group == null) {
                group = new Group();
                group.setId(rs.getInt("group_id"));
                group.setDeleted(rs.getBoolean("deleted"));
                group.setTitle(rs.getString("title"));
                group.setDescription(rs.getString("description"));
                group.setImageFileId(rs.getInt("image_file_id"));
                group.setLeaderUserId(rs.getInt("leader_id"));
                group.setCourseId(rs.getInt("course_id"));
                group.setRestrictionForumCategoryRequired(rs.getBoolean("restriction_forum_category_required"));
                group.setMaxMemberCount(rs.getInt("max_member_count"));
                group.setPolicyAdd(Group.PolicyAdd.valueOf(rs.getString("policy_add")));
                group.setPolicyAnnotate(Group.PolicyAnnotate.valueOf(rs.getString("policy_annotate")));
                group.setPolicyEdit(Group.PolicyEdit.valueOf(rs.getString("policy_edit")));
                group.setPolicyJoin(Group.PolicyJoin.valueOf(rs.getString("policy_join")));
                group.setPolicyView(Group.PolicyView.valueOf(rs.getString("policy_view")));
                group.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));

                cache.put(group);
            }
            return group;
        }
    }

    class GroupUserMapper implements RowMapper<GroupUser> {
        @Override
        public GroupUser map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            GroupUser group = new GroupUser();
            group.setGroupId(rs.getInt("group_id"));
            group.setUserId(rs.getInt("user_id"));
            group.setJoinTime(SqlHelper.getLocalDateTime(rs.getTimestamp("join_time")));
            group.setLastVisit(SqlHelper.getLocalDateTime(rs.getTimestamp("last_visit")));
            group.setNotificationFrequency(User.NotificationFrequency.valueOf(rs.getString("notification_frequency")));
            return group;
        }
    }
}

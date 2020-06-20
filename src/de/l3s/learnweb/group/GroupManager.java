package de.l3s.learnweb.group;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group.PolicyAdd;
import de.l3s.learnweb.group.Group.PolicyAnnotate;
import de.l3s.learnweb.group.Group.PolicyEdit;
import de.l3s.learnweb.group.Group.PolicyJoin;
import de.l3s.learnweb.group.Group.PolicyView;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceContainer;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.User.NotificationFrequency;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.HasId;
import de.l3s.util.ICache;

/**
 * DAO for the Group class.
 *
 * @author Philipp
 */
public class GroupManager {
    // if you change this, you have to change the createGroup and save method too
    private static final String GROUP_COLUMNS = "g.group_id, g.title, g.description, g.leader_id, g.course_id, g.restriction_forum_category_required, g.policy_add, g.policy_annotate, g.policy_edit, g.policy_join, g.policy_view, g.max_member_count, g.hypothesis_link, g.hypothesis_token";
    private static final String FOLDER_COLUMNS = "f.folder_id, f.deleted, f.group_id, f.parent_folder_id, f.name, f.description, f.user_id";
    private static final Logger log = LogManager.getLogger(GroupManager.class);

    private final Learnweb learnweb;
    private final ICache<Group> groupCache;
    private final ICache<Folder> folderCache;

    public GroupManager(Learnweb learnweb) throws SQLException {
        int groupCacheSize = learnweb.getProperties().getPropertyIntValue("GROUP_CACHE");
        int folderCacheSize = learnweb.getProperties().getPropertyIntValue("FOLDER_CACHE");

        this.learnweb = learnweb;
        this.groupCache = groupCacheSize == 0 ? new DummyCache<>() : new Cache<>(groupCacheSize);
        this.folderCache = groupCacheSize == 0 ? new DummyCache<>() : new Cache<>(folderCacheSize);
    }

    public void resetCache() {
        for (Group group : groupCache.getValues()) {
            group.clearCaches();
        }

        groupCache.clear();
        folderCache.clear();
    }

    private List<Group> getGroups(String query, Object... params) throws SQLException {
        List<Group> groups = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement(query)) {
            int i = 1;
            for (Object param : params) {
                select.setObject(i++, param);
            }

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                groups.add(createGroup(rs));
            }

            return groups;
        }
    }

    /**
     * Returns a list of all Groups a user belongs to.
     */
    public List<Group> getGroupsByUserId(int userId) throws SQLException {
        String query = "SELECT " + GROUP_COLUMNS + " FROM `lw_group` g JOIN lw_group_user u USING(group_id) WHERE u.user_id = ? ORDER BY title";
        return getGroups(query, userId);
    }

    /**
     * Returns a group by user with notification frequency.
     */
    public GroupUser getGroupUserRelation(int userId, int groupId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT group_id, notification_frequency FROM lw_group_user WHERE user_id = ? AND group_id=?")) {
            select.setInt(1, userId);
            select.setInt(2, groupId);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                return new GroupUser(getGroupById(rs.getInt("group_id")), User.NotificationFrequency.valueOf(rs.getString("notification_frequency")));
            }
        }
        return null;
    }

    /**
     * Returns a list of all Groups a user belongs to with associated metadata like notification frequency.
     */
    public List<GroupUser> getGroupsRelations(int userId) throws SQLException {
        List<GroupUser> groups = new ArrayList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT group_id, notification_frequency FROM lw_group_user WHERE user_id = ?")) {
            select.setInt(1, userId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                Group group = getGroupById(rs.getInt("group_id"));
                NotificationFrequency frequency = User.NotificationFrequency.valueOf(rs.getString("notification_frequency"));
                groups.add(new GroupUser(group, frequency));
            }
        }
        return groups;
    }

    /**
     * Returns a list of all Groups which belong to the defined course.
     */
    public List<Group> getGroupsByCourseId(int courseId) throws SQLException {
        String query = "SELECT " + GROUP_COLUMNS + " FROM `lw_group` g  WHERE g.course_id = ? AND g.deleted = 0 ORDER BY title";
        return getGroups(query, courseId);
    }

    /**
     * Returns a list of Groups which belong to the defined courses and were created after the specified date.
     */
    public List<Group> getGroupsByCourseId(List<Course> courses, Instant newerThan) throws SQLException {
        if (courses.isEmpty()) {
            return Collections.emptyList();
        }
        String query = "SELECT " + GROUP_COLUMNS + " FROM `lw_group` g WHERE g.course_id IN(" + HasId.implodeIds(courses) + ") AND g.deleted = 0 AND `creation_time` > ? ORDER BY title";
        return getGroups(query, Timestamp.from(newerThan)); //(int) (newerThan.getTime() / 1000));
    }

    /**
     * Returns a list of all Groups a user belongs to and which groups are also part of the defined course.
     */
    public List<Group> getGroupsByUserIdFilteredByCourseId(int userId, int courseId) throws SQLException {
        String query = "SELECT " + GROUP_COLUMNS + " FROM `lw_group` g JOIN lw_group_user USING(group_id) WHERE user_id = ? AND g.course_id = ? AND g.deleted = 0 ORDER BY title";
        return getGroups(query, userId, courseId);
    }

    public Group getGroupByTitleFilteredByOrganisation(String title, int organisationId) throws SQLException {
        try (PreparedStatement pstmtGetGroup = learnweb.getConnection()
            .prepareStatement("SELECT " + GROUP_COLUMNS + " FROM `lw_group` g JOIN lw_course gc USING(course_id) WHERE g.title LIKE ? AND organisation_id = ? AND g.deleted = 0")) {
            pstmtGetGroup.setString(1, title);
            pstmtGetGroup.setInt(2, organisationId);
            ResultSet rs = pstmtGetGroup.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return createGroup(rs);
        }
    }

    private Group createGroup(ResultSet rs) throws SQLException {
        Group group = groupCache.get(rs.getInt("group_id"));
        if (null == group) {
            group = new Group();
            group.setId(rs.getInt("group_id"));
            group.setTitle(rs.getString("title"));
            group.setDescription(rs.getString("description"));
            group.setLeaderUserId(rs.getInt("leader_id"));
            group.setCourseId(rs.getInt("course_id"));
            group.setRestrictionForumCategoryRequired(rs.getInt("restriction_forum_category_required") == 1);
            group.setMaxMemberCount(rs.getInt("max_member_count"));
            group.setHypothesisLink(rs.getString("hypothesis_link"));
            group.setHypothesisToken(rs.getString("hypothesis_token"));

            group.setPolicyAdd(PolicyAdd.valueOf(rs.getString("policy_add")));
            group.setPolicyAnnotate(PolicyAnnotate.valueOf(rs.getString("policy_annotate")));
            group.setPolicyEdit(PolicyEdit.valueOf(rs.getString("policy_edit")));
            group.setPolicyJoin(PolicyJoin.valueOf(rs.getString("policy_join")));
            group.setPolicyView(PolicyView.valueOf(rs.getString("policy_view")));

            group = groupCache.put(group);
        }
        return group;
    }

    /**
     * Returns all groups a user can join.
     * This are all groups of his courses except for groups he has already joined + groups that are open to everybody.
     */
    public List<Group> getJoinAbleGroups(User user) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (Course course : user.getCourses()) {
            sb.append(",").append(course.getId());
        }

        String publicCourseClause = "";
        if (!user.getOrganisation().getOption(Option.Groups_Hide_public_groups)) {
            sb.append(",0");
            publicCourseClause = " OR policy_join = 'ALL_LEARNWEB_USERS'";
        }

        String coursesIn = sb.substring(1);

        sb = new StringBuilder(",-1"); // make sure that the string is not empty
        for (Group group : user.getGroups()) {
            sb.append(",").append(group.getId());
        }
        String groupsIn = sb.substring(1);

        String query = "SELECT " + GROUP_COLUMNS + " FROM `lw_group` g JOIN lw_course USING(course_id) WHERE g.deleted = 0 "
            + "AND g.group_id NOT IN(" + groupsIn + ") AND (g.policy_join = 'COURSE_MEMBERS' "
            + "AND g.course_id IN(" + coursesIn + ") " + publicCourseClause + " OR g.policy_join = 'ORGANISATION_MEMBERS' "
            + "AND organisation_id = " + user.getOrganisationId() + ") ORDER BY title";

        return getGroups(query);
    }

    /**
     * Get a Group by her id.
     */
    public Group getGroupById(int id) throws SQLException {
        return getGroupById(id, true);
    }

    public Group getGroupById(int id, boolean useCache) throws SQLException {
        Group group = useCache ? groupCache.get(id) : null;

        if (null != group) {
            return group;
        }

        try (PreparedStatement pstmtGetGroup = learnweb.getConnection().prepareStatement("SELECT " + GROUP_COLUMNS + " FROM `lw_group` g WHERE group_id = ? AND g.deleted = 0")) {
            pstmtGetGroup.setInt(1, id);
            ResultSet rs = pstmtGetGroup.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return createGroup(rs);
        }
    }

    /**
     * Saves the Group to the database.
     * If the Group is not yet stored at the database, a new record will be created and the returned Group contains the new id.
     */
    public synchronized Group save(Group group) throws SQLException {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement(
            "REPLACE INTO `lw_group` (group_id, `title`, `description`, `leader_id`, course_id, "
                + "restriction_forum_category_required, policy_add, policy_annotate, policy_edit, policy_join, "
                + "policy_view, max_member_count, hypothesis_link, hypothesis_token) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            if (group.getId() < 0) { // the Group is not yet stored at the database
                replace.setNull(1, java.sql.Types.INTEGER);
            } else {
                replace.setInt(1, group.getId());
            }

            replace.setString(2, group.getTitle());
            replace.setString(3, group.getDescription());
            replace.setInt(4, group.getLeaderUserId());
            replace.setInt(5, group.getCourseId());
            replace.setInt(6, group.isRestrictionForumCategoryRequired() ? 1 : 0);
            replace.setString(7, group.getPolicyAdd().name());
            replace.setString(8, group.getPolicyAnnotate().name());
            replace.setString(9, group.getPolicyEdit().name());
            replace.setString(10, group.getPolicyJoin().name());
            replace.setString(11, group.getPolicyView().name());
            replace.setInt(12, group.getMaxMemberCount());
            replace.setString(13, group.getHypothesisLink());
            replace.setString(14, group.getHypothesisToken());
            replace.executeUpdate();

            if (group.getId() < 0) { // get the assigned id
                ResultSet rs = replace.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                group.setId(rs.getInt(1));
                group = groupCache.put(group); // add the new Group to the cache
            } else if (groupCache.get(group.getId()) != null) { // remove old group and add the new one
                groupCache.remove(group.getId());
                group = groupCache.put(group);
            }

            return group;
        }

    }

    public void addUserToGroup(User user, Group group) throws SQLException {
        try (PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT IGNORE INTO `lw_group_user` (`group_id`,`user_id`,`notification_frequency`) VALUES (?,?,?)")) {
            insert.setInt(1, group.getId());
            insert.setInt(2, user.getId());
            insert.setString(3, user.getPreferredNotificationFrequency().toString());
            insert.execute();
        }
    }

    public void removeUserFromGroup(User user, Group group) throws SQLException {
        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?")) {
            delete.setInt(1, group.getId());
            delete.setInt(2, user.getId());
            delete.execute();
        }
    }

    void deleteGroupSoft(Group group) throws SQLException {
        for (Resource resource : group.getResources()) {
            resource.delete();
        }

        List<User> members = group.getMembers(); // load members before connection is deleted

        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_user` WHERE `group_id` = ?")) {
            delete.setInt(1, group.getId());
            delete.execute();
        }

        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("UPDATE `lw_group` SET deleted = 1 WHERE `group_id` = ?")) {
            delete.setInt(1, group.getId());
            delete.execute();
        }

        members.forEach(User::clearCaches);

        groupCache.remove(group.getId());
    }

    /**
     * Deletes the group and all its resources permanently. Don't use this if you don't know exactly what you are doing!
     */
    void deleteGroupHard(Group group) throws SQLException {
        log.debug("Delete group: " + group);
        for (Resource resource : group.getResources()) {
            resource.deleteHard();
        }

        List<User> members = group.getMembers();

        String[] tables = {"lw_forum_topic", "lw_group_folder", "lw_group_user", "lw_user_log", "lw_group"};

        for (String table : tables) {
            try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM " + table + " WHERE `group_id` = ?")) {
                delete.setInt(1, group.getId());
                //log.debug(delete);
                int numRowsAffected = delete.executeUpdate();
                log.debug("Deleted " + numRowsAffected + " rows from " + table);
            }
        }

        try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE lw_course SET default_group_id = 0 WHERE default_group_id = ?")) {
            update.setInt(1, group.getId());
            update.executeUpdate();
        }

        members.forEach(User::clearCaches);

        groupCache.remove(group.getId());
    }

    private Folder createFolder(ResultSet rs) throws SQLException {
        int folderId = rs.getInt("folder_id");

        Folder folder = folderCache.get(folderId);
        if (null == folder) {
            folder = new Folder();
            folder.setId(folderId);
            folder.setGroupId(rs.getInt("group_id"));
            folder.setParentFolderId(rs.getInt("parent_folder_id"));
            folder.setTitle(rs.getString("name"));
            folder.setDescription(rs.getString("description"));
            folder.setUserId(rs.getInt("user_id"));
            folder.setDeleted(rs.getInt("deleted") == 1);

            folder = folderCache.put(folder);
        }
        return folder;
    }

    public Folder getFolder(int folderId) throws SQLException {
        Folder folder = folderCache.get(folderId);

        if (folder != null) {
            return folder;
        }

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FOLDER_COLUMNS + " FROM `lw_group_folder` f WHERE `deleted` = 0 AND `folder_id` = ?")) {
            select.setInt(1, folderId);
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return createFolder(rs);
        }
    }

    /**
     *
     */
    public List<Folder> getFolders(int groupId, int parentFolderId) throws SQLException {
        List<Folder> folders = new ArrayList<>();
        if (parentFolderId < 0) {
            parentFolderId = 0;
        }

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FOLDER_COLUMNS + " FROM `lw_group_folder` f WHERE `deleted` = 0 AND `group_id` = ? AND `parent_folder_id` = ?")) {
            select.setInt(1, groupId);
            select.setInt(2, parentFolderId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                folders.add(createFolder(rs));
            }

            return folders;
        }
    }

    /**
     *
     */
    public List<Folder> getFolders(int groupId, int parentFolderId, int userId) throws SQLException {
        List<Folder> folders = new ArrayList<>();
        if (parentFolderId < 0) {
            parentFolderId = 0;
        }

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + FOLDER_COLUMNS + " FROM `lw_group_folder` f WHERE `deleted` = 0 AND `group_id` = ? AND `parent_folder_id` = ? AND `user_id` = ?")) {
            select.setInt(1, groupId);
            select.setInt(2, parentFolderId);
            select.setInt(3, userId);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                folders.add(createFolder(rs));
            }

            return folders;
        }
    }

    public void moveFolder(Folder folder, int newParentFolderId, int newGroupId) throws SQLException {
        // TODO: throw an error instead of silent ignore
        if (folder.getId() == newParentFolderId) {
            return; // if move to itself
        }
        if (folder.getGroupId() == newGroupId && folder.isParentOf(newParentFolderId)) {
            return; // if move to own sub folder
        }

        int groupId = folder.getGroupId();
        int parentFolderId = folder.getParentFolderId();
        List<Folder> subFolders = folder.getSubFolders();

        folder.setGroupId(newGroupId);
        folder.setParentFolderId(newParentFolderId);
        folder.save();

        for (Folder subFolder : subFolders) {
            moveFolder(subFolder, folder.getId(), newGroupId);
        }

        for (Resource resource : folder.getResources()) {
            resource.setGroupId(newGroupId);
            resource.save();
        }

        if (newParentFolderId > 0) {
            getFolder(newParentFolderId).clearCaches();
        } else if (newGroupId > 0) {
            getGroupById(newGroupId).clearCaches();
        }

        if (parentFolderId > 0) {
            getFolder(parentFolderId).clearCaches();
        } else if (groupId > 0) {
            getGroupById(groupId).clearCaches();
        }
    }

    public void moveResource(Resource resource, int newGroupId, int newFolderId) throws SQLException {
        // TODO: throw an error instead of silent ignore
        if (resource.getGroupId() == newGroupId && resource.getFolderId() == newFolderId) {
            return; // if move to the same folder
        }

        resource.setGroupId(newGroupId);
        resource.setFolderId(newFolderId);
        resource.save();
    }

    public Folder saveFolder(Folder folder) throws SQLException {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_group_folder` (folder_id, group_id, parent_folder_id, name, description, user_id, deleted) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            if (folder.getId() < 0) { // the folder is not yet stored at the database
                replace.setNull(1, java.sql.Types.INTEGER);
            } else {
                replace.setInt(1, folder.getId());
            }
            replace.setInt(2, folder.getGroupId());
            replace.setInt(3, folder.getParentFolderId());
            replace.setString(4, folder.getTitle());
            replace.setString(5, folder.getDescription());
            replace.setInt(6, folder.getUserId());
            replace.setInt(7, folder.isDeleted() ? 1 : 0);
            replace.executeUpdate();

            if (folder.getId() < 0) { // get the assigned id
                ResultSet rs = replace.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                folder.setId(rs.getInt(1));
            } else {
                folder.clearCaches();
                folderCache.remove(folder.getId());
            }
            folder = folderCache.put(folder);
            return folder;
        }
    }

    public AbstractResource getAbstractResource(String resourceType, int resourceId) throws SQLException {
        if (resourceId == -1) {
            return null;
        }

        if (resourceType == null || resourceType.isEmpty()) {
            throw new NullPointerException("resourceType is null or empty");
        }

        if (resourceType.equals("folder")) {
            return getFolder(resourceId);
        } else if (resourceType.equals("resource")) {
            return learnweb.getResourceManager().getResource(resourceId);
        }

        throw new NullPointerException("Unsupported resourceType: " + resourceType);
    }

    /**
     * @return number of cached objects
     */
    public int getGroupCacheSize() {
        return groupCache.size();
    }

    /**
     * @return number of cached objects
     */
    public int getFolderCacheSize() {
        return folderCache.size();
    }

    /**
     * Define at which timestamp the user has visited the group the last time.
     */
    public void setLastVisit(User user, Group group, int time) throws SQLException {
        try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_group_user` SET `last_visit` = ? WHERE `group_id` = ? AND `user_id` = ?")) {
            update.setInt(1, time);
            update.setInt(2, group.getId());
            update.setInt(3, user.getId());
            update.executeUpdate();
        }
    }

    /**
     * @return unix timestamp when the user has visited the group the last time; returns -1 if he never view the group.
     */
    public int getLastVisit(User user, Group group) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT `last_visit` FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?")) {
            select.setInt(1, group.getId());
            select.setInt(2, user.getId());
            ResultSet rs = select.executeQuery();
            if (!rs.next()) {
                return -1;
            }

            int time = rs.getInt(1);
            return time;
        }
    }

    public TreeNode getFoldersTree(Group group, int activeFolder) throws SQLException {
        if (group == null) {
            return null;
        }

        TreeNode treeNode = new DefaultTreeNode("GroupFolders");
        TreeNode rootNode = new DefaultTreeNode("folder", new Folder(0, group.getId(), group.getTitle()), treeNode);
        if (activeFolder == 0) {
            rootNode.setSelected(true);
            rootNode.setExpanded(true);
        }
        getChildNodesRecursively(rootNode, group, activeFolder);
        return treeNode;
    }

    public void getChildNodesRecursively(TreeNode parentNode, ResourceContainer container, int activeFolderId) throws SQLException {
        List<Folder> folders = container.getSubFolders();
        for (Folder folder : folders) {
            TreeNode folderNode = new DefaultTreeNode("folder", folder, parentNode);
            if (folder.getId() == activeFolderId) {
                folderNode.setSelected(true);
                folderNode.setExpanded(true);
                expandToNode(folderNode);
            }
            getChildNodesRecursively(folderNode, folder, activeFolderId);
        }
    }

    private void expandToNode(TreeNode treeNode) {
        if (treeNode.getParent() != null) {
            treeNode.getParent().setExpanded(true);
            expandToNode(treeNode.getParent());
        }
    }

    public int getMemberCount(int groupId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT COUNT(*) FROM lw_group_user WHERE group_id = ?")) {
            select.setInt(1, groupId);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        }
    }

    /**
     * Saves the setting that defines how often a user will retrieve notifications for the given group.
     */
    public void updateNotificationFrequency(int groupId, int userId, User.NotificationFrequency notificationFrequency) throws SQLException {
        try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_group_user` SET `notification_frequency`= ? WHERE `user_id`= ? and `group_id`= ?")) {
            update.setString(1, notificationFrequency.toString());
            update.setInt(2, userId);
            update.setInt(3, groupId);
            update.executeQuery();
        }
    }

}

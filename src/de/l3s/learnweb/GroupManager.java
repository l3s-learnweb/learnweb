package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.Organisation.Option;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.Sql;

/**
 * DAO for the Group class.
 *
 * @author Philipp
 */
public class GroupManager
{

    // if you change this, you have to change the constructor of Group too
    private final static String COLUMNS = "g.group_id, g.title, g.description, g.leader_id, g.course_id, g.university, g.course, g.location, g.language, g.restriction_only_leader_can_add_resources, g.read_only, lw_group_category.group_category_id, lw_group_category.category_title, lw_group_category.category_abbreviation, g.restriction_forum_category_required, g.policy_add, g.policy_annotate, g.policy_edit, g.policy_join, g.policy_view";
    private static Logger log = Logger.getLogger(GroupManager.class);

    private Learnweb learnweb;
    private ICache<Group> groupCache;
    private ICache<Folder> folderCache;

    protected GroupManager(Learnweb learnweb) throws SQLException
    {
        int groupCacheSize = learnweb.getProperties().getPropertyIntValue("GROUP_CACHE");
        int folderCacheSize = learnweb.getProperties().getPropertyIntValue("FOLDER_CACHE");

        this.learnweb = learnweb;
        this.groupCache = groupCacheSize == 0 ? new DummyCache<Group>() : new Cache<Group>(groupCacheSize);
        this.folderCache = groupCacheSize == 0 ? new DummyCache<Folder>() : new Cache<Folder>(folderCacheSize);
    }

    public void resetCache() throws SQLException
    {
        groupCache.clear();
        folderCache.clear();
    }

    private List<Group> getGroups(String query, int... params) throws SQLException
    {
        List<Group> groups = new LinkedList<Group>();
        PreparedStatement select = learnweb.getConnection().prepareStatement(query);

        int i = 1;
        for(int param : params)
            select.setInt(i++, param);

        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            groups.add(createGroup(rs));
        }
        select.close();

        return groups;
    }

    /**
     * Returns a list of all Groups a user belongs to
     *
     * @throws SQLException
     */
    public List<Group> getGroupsByUserId(int userId) throws SQLException
    {
        String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_group_user u USING(group_id) WHERE u.user_id = ? ORDER BY title";
        return getGroups(query, userId);
    }

    public int getGroupCountByUserId(int userId) throws SQLException
    {
        String query = "SELECT COUNT(*) FROM `lw_group` g JOIN lw_group_user u USING(group_id) WHERE u.user_id = " + userId;
        return ((Long) Sql.getSingleResult(query)).intValue();
    }

    /**
     * Returns a list of all Groups which belong to the defined course
     *
     * @throws SQLException
     */
    public List<Group> getGroupsByCourseId(int courseId) throws SQLException
    {
        String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE g.course_id = ? AND g.deleted = 0 ORDER BY title";
        return getGroups(query, courseId);
    }

    /**
     * Returns a list of Groups which belong to the defined course and were created after the specified date
     */
    public List<Group> getGroupsByCourseId(int courseId, Date newerThan) throws SQLException
    {
        String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE g.course_id = ? AND g.deleted = 0 AND `creation_time` > FROM_UNIXTIME(?) ORDER BY title";
        return getGroups(query, courseId, (int) (newerThan.getTime() / 1000));
    }

    /**
     * Returns a list of all Groups a user belongs to and which groups are also part of the defined course
     *
     * @throws SQLException
     */
    public List<Group> getGroupsByUserIdFilteredByCourseId(int userId, int courseId) throws SQLException
    {
        String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_group_user USING(group_id) WHERE user_id = ? AND g.course_id = ? AND g.deleted = 0 ORDER BY title";
        return getGroups(query, userId, courseId);
    }

    public Group getGroupByTitleFilteredByOrganisation(String title, int organisationId) throws SQLException
    {
        PreparedStatement pstmtGetGroup = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) JOIN lw_course gc USING(course_id) WHERE g.title LIKE ? AND organisation_id = ? AND g.deleted = 0");
        pstmtGetGroup.setString(1, title);
        pstmtGetGroup.setInt(2, organisationId);
        ResultSet rs = pstmtGetGroup.executeQuery();

        if(!rs.next())
            return null;

        Group group = createGroup(rs);

        pstmtGetGroup.close();

        return group;
    }

    private Group createGroup(ResultSet rs) throws SQLException
    {
        Group group = groupCache.get(rs.getInt("group_id"));
        if(null == group)
        {
            group = new Group(rs);
            group = groupCache.put(group);
        }
        return group;
    }

    /**
     * Returns all groups a user can join
     * This are all groups of his courses except for groups he has already joined
     *
     * @param user
     * @return
     * @throws SQLException
     */
    public List<Group> getJoinAbleGroups(User user) throws SQLException
    {
        StringBuilder sb = new StringBuilder();
        for(Course course : user.getCourses())
            sb.append("," + course.getId());

        String publicCourseClause = "";
        if(!user.getOrganisation().getOption(Option.Groups_Hide_public_groups))
        {
            sb.append(",0");
            publicCourseClause = " OR policy_join = 'ALL_LEARNWEB_USERS'";
        }

        String coursesIn = sb.substring(1);

        sb = new StringBuilder(",-1"); // make sure that the string is not empty
        for(Group group : user.getGroups())
            sb.append("," + group.getId());
        String groupsIn = sb.substring(1);

        String query = "SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE g.deleted = 0 AND g.group_id NOT IN(" + groupsIn + ") AND (g.policy_join = 'COURSE_MEMBERS' AND g.course_id IN(" + coursesIn + ") " + publicCourseClause
                + ") ORDER BY title";

        return getGroups(query);
    }

    /**
     * Get a Group by her id
     *
     * @param id
     * @return
     * @throws SQLException
     */
    public Group getGroupById(int id) throws SQLException
    {
        return getGroupById(id, true);
    }

    public Group getGroupById(int id, boolean useCache) throws SQLException
    {
        Group group = useCache ? groupCache.get(id) : null;

        if(null != group)
            return group;

        PreparedStatement pstmtGetGroup = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_group` g LEFT JOIN lw_group_category USING(group_category_id) WHERE group_id = ?");
        pstmtGetGroup.setInt(1, id);
        ResultSet rs = pstmtGetGroup.executeQuery();

        if(!rs.next())
            return null;

        group = new Group(rs);
        pstmtGetGroup.close();

        if(useCache)
            group = groupCache.put(group);

        return group;
    }

    /**
     * Saves the Group to the database.
     * If the Group is not yet stored at the database, a new record will be created and the returned Group contains the new id.
     *
     * @param Group
     * @return
     * @throws SQLException
     */
    public synchronized Group save(Group group) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement(
                "REPLACE INTO `lw_group` (group_id, `title`, `description`, `leader_id`, university, course, location, language, course_id, group_category_id, restriction_only_leader_can_add_resources, read_only, restriction_forum_category_required, policy_add, policy_annotate, policy_edit, policy_join, policy_view) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);

        if(group.getId() < 0) // the Group is not yet stored at the database
        {
            replace.setNull(1, java.sql.Types.INTEGER);

            if(group.getCategoryId() != 0)
            {
                GroupCategory category = getGroupCategoryById(group.getCategoryId());
                group.setCategoryAbbreviation(category.getAbbreviation());
                group.setCategoryTitle(category.getTitle());
            }
        }
        else
            replace.setInt(1, group.getId());
        replace.setString(2, group.getTitle());
        replace.setString(3, group.getDescription());
        replace.setInt(4, group.getLeaderUserId());
        replace.setString(5, group.getUniversity());
        replace.setString(6, group.getMetaInfo1());
        replace.setString(7, group.getLocation());
        replace.setString(8, group.getLanguage());
        replace.setInt(9, group.getCourseId());
        replace.setInt(10, group.getCategoryId());
        replace.setInt(11, group.isRestrictionOnlyLeaderCanAddResources() ? 1 : 0);
        replace.setInt(12, group.isReadOnly() ? 1 : 0);
        replace.setInt(13, group.isRestrictionForumCategoryRequired() ? 1 : 0);
        replace.setString(14, group.getPolicyAdd().name());
        replace.setString(15, group.getPolicyAnnotate().name());
        replace.setString(16, group.getPolicyEdit().name());
        replace.setString(17, group.getPolicyJoin().name());
        replace.setString(18, group.getPolicyView().name());
        replace.executeUpdate();

        if(group.getId() < 0) // get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            group.setId(rs.getInt(1));
            group = groupCache.put(group); // add the new Group to the cache
        }
        else if(groupCache.get(group.getId()) != null) //remove old group and add the new one
        {
            groupCache.remove(group.getId());
            group = groupCache.put(group);
        }
        replace.close();

        int groupId = group.getId();

        if(groupId != group.getId())
            throw new RuntimeException("fedora error");

        return group;
    }

    public void addUserToGroup(User user, Group group) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
        select.setInt(1, group.getId());
        select.setInt(2, user.getId());
        ResultSet rs = select.executeQuery();
        boolean userIsAlreadyMember = rs.next();
        select.close();

        if(userIsAlreadyMember)
            return;

        PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_group_user` (`group_id`,`user_id`) VALUES (?,?)");
        insert.setInt(1, group.getId());
        insert.setInt(2, user.getId());
        insert.execute();
        insert.close();
    }

    public void removeUserFromGroup(User user, Group group) throws SQLException
    {
        PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
        delete.setInt(1, group.getId());
        delete.setInt(2, user.getId());
        delete.execute();
        delete.close();
    }

    public void deleteGroup(Group group) throws SQLException
    {
        for(Resource resource : group.getResources())
        {
            resource.setGroupId(0);
            resource.save();
        }

        PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_user` WHERE `group_id` = ?");
        delete.setInt(1, group.getId());
        delete.execute();
        delete.close();

        delete = learnweb.getConnection().prepareStatement("UPDATE `lw_group` SET deleted = 1 WHERE `group_id` = ?");
        delete.setInt(1, group.getId());
        delete.execute();
        delete.close();

        groupCache.remove(group.getId());
    }

    private Folder createFolder(ResultSet rs) throws SQLException
    {
        int folderId = rs.getInt("folder_id");

        Folder folder = folderCache.get(folderId);
        if(null == folder)
        {
            folder = new Folder();
            folder.setId(folderId);
            folder.setGroupId(rs.getInt("group_id"));
            folder.setParentFolderId(rs.getInt("parent_folder_id"));
            folder.setTitle(rs.getString("name"));
            folder.setUserId(rs.getInt("user_id"));

            folder = folderCache.put(folder);
        }
        return folder;
    }

    public Folder getFolder(int folderId) throws SQLException
    {
        Folder folder = folderCache.get(folderId);

        if(folder == null)
        {
            PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT folder_id, group_id, parent_folder_id, name, user_id FROM `lw_group_folder` WHERE `folder_id` = ?");
            select.setInt(1, folderId);
            ResultSet rs = select.executeQuery();

            if(!rs.next())
                return null;

            folder = createFolder(rs);
            select.close();
        }
        return folder;
    }

    /**
     * @param groupId
     * @throws SQLException
     */
    public List<Folder> getFolders(int groupId) throws SQLException
    {
        List<Folder> folders = new ArrayList<Folder>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT folder_id, group_id, parent_folder_id, name, user_id FROM `lw_group_folder` WHERE `group_id` = ?");
        select.setInt(1, groupId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            folders.add(createFolder(rs));
        }
        select.close();

        return folders;
    }

    /**
     * @param groupId
     * @param parentFolderId
     * @throws SQLException
     */
    public List<Folder> getFolders(int groupId, int parentFolderId) throws SQLException
    {
        List<Folder> folders = new ArrayList<Folder>();
        if(parentFolderId < 0)
        {
            parentFolderId = 0;
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT folder_id, group_id, parent_folder_id, name, user_id FROM `lw_group_folder` WHERE `group_id` = ? AND `parent_folder_id` = ?");
        select.setInt(1, groupId);
        select.setInt(2, parentFolderId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            folders.add(createFolder(rs));
        }
        select.close();

        return folders;
    }

    /**
     * @param groupId
     * @param parentFolderId
     * @throws SQLException
     */
    public List<Folder> getFolders(int groupId, int parentFolderId, int userId) throws SQLException
    {
        List<Folder> folders = new ArrayList<Folder>();
        if(parentFolderId < 0)
        {
            parentFolderId = 0;
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT folder_id, group_id, parent_folder_id, name, user_id FROM `lw_group_folder` WHERE `group_id` = ? AND `parent_folder_id` = ? AND `user_id` = ?");
        select.setInt(1, groupId);
        select.setInt(2, parentFolderId);
        select.setInt(3, userId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            folders.add(createFolder(rs));
        }
        select.close();

        return folders;
    }

    /**
     * @param userId
     * @return
     * @throws SQLException
     */
    public List<Folder> getGroupsForMyResources(int userId) throws SQLException
    {
        LinkedList<Folder> folders = new LinkedList<Folder>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT DISTINCT(group_id), lw_group.title FROM `lw_resource` JOIN lw_group USING(group_id) WHERE `owner_user_id` = ? AND lw_resource.deleted = 0");
        select.setInt(1, userId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            Folder folder = new Folder();
            folder.setId(0);
            folder.setGroupId(rs.getInt("group_id"));
            folder.setParentFolderId(0);
            folder.setTitle(rs.getString("title"));
            folder.setUserId(userId);
            folders.add(folder);
        }
        select.close();
        return folders;
    }

    /**
     * @param groupId
     * @param parentFolderId
     * @throws SQLException
     */
    public int getCountFolders(int groupId, int parentFolderId) throws SQLException
    {
        if(parentFolderId < 0)
        {
            parentFolderId = 0;
        }

        int numberOfRows = 0;
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT COUNT(*) FROM `lw_group_folder` WHERE `group_id` = ? AND `parent_folder_id` = ?");
        select.setInt(1, groupId);
        select.setInt(2, parentFolderId);
        ResultSet rs = select.executeQuery();
        if(rs.next())
        {
            numberOfRows = rs.getInt(1);
        }
        select.close();

        return numberOfRows;
    }

    /**
     * @param groupId
     * @param parentFolderId
     * @throws SQLException
     */
    public int getCountResources(int groupId, int parentFolderId) throws SQLException
    {
        int numberOfRows = 0;
        SolrSearch solrSearch = new SolrSearch("*", UtilBean.getUserBean().getUser());
        solrSearch.setFilterGroups(groupId);
        solrSearch.setFilterFolder(parentFolderId, true);

        try
        {
            solrSearch.getResourcesByPage(1);
            numberOfRows = (int) solrSearch.getTotalResultCount();
        }
        catch(SolrServerException e)
        {
            log.fatal("Couldn't get resource counter in group", e);
        }

        return numberOfRows;
    }

    public Folder moveFolder(Folder original, int newParentFolderId, int newGroupId) throws SQLException
    {
        if(original.getId() == newParentFolderId)
        {
            return original;
        }

        int parentFolderId = original.getParentFolderId();
        List<Folder> subfolders = original.getSubfolders();

        original.setGroupId(newGroupId);
        original.setParentFolderId(newParentFolderId);
        original.save();

        for(Folder subfolder : subfolders)
        {
            this.moveFolder(subfolder, original.getId(), newGroupId);
        }

        for(Resource res : original.getResources())
        {
            res.setGroupId(newGroupId);
            res.save();
        }

        if(newParentFolderId > 0)
        {
            getFolder(newParentFolderId).clearCaches();
        }

        if(parentFolderId > 0)
        {
            getFolder(parentFolderId).clearCaches();
        }

        return original;
    }

    public Resource moveResource(Resource original, int newGroupId, int newFolderId) throws SQLException
    {
        if(original.getGroupId() == newGroupId && original.getFolderId() == newFolderId)
        {
            return original;
        }

        original.setGroupId(newGroupId);
        original.setFolderId(newFolderId);
        original.save();
        return original;
    }

    public Folder saveFolder(Folder folder) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_group_folder` (folder_id, group_id, parent_folder_id, name, user_id) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

        if(folder.getId() < 0) // the folder is not yet stored at the database
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, folder.getId());
        replace.setInt(2, folder.getGroupId());
        replace.setInt(3, folder.getParentFolderId());
        replace.setString(4, folder.getTitle());
        replace.setInt(5, folder.getUserId());
        replace.executeUpdate();

        if(folder.getId() < 0) // get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            folder.setId(rs.getInt(1));
            folder = folderCache.put(folder);
        }
        else
        {
            folder.clearCaches();
            folderCache.remove(folder.getId());
            folder = folderCache.put(folder);
        }

        replace.close();
        return folder;
    }

    /**
     * @return number of cached objects
     */
    public int getGroupCacheSize()
    {
        return groupCache.size();
    }

    /**
     * @return number of cached objects
     */
    public int getFolderCacheSize()
    {
        return folderCache.size();
    }

    public void deleteFolder(Folder folder) throws SQLException
    {
        List<Folder> subfolders = folder.getSubfolders();

        if(!subfolders.isEmpty())
        {
            for(Folder subFolder : subfolders)
            {
                this.deleteFolder(subFolder);
            }
        }

        for(Resource resource : folder.getResources())
        {
            resource.setGroupId(0);
            resource.setFolderId(0);
            resource.save();
        }

        Folder parentFolder = folder.getParentFolder();

        PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_group_folder` WHERE `folder_id` = ?");
        delete.setInt(1, folder.getId());
        delete.execute();
        delete.close();

        folderCache.remove(folder.getId());

        if(parentFolder != null)
        {
            parentFolder.clearCaches();
        }
    }

    /**
     * Define at which timestamp the user has visited the group the last time
     *
     * @param user
     * @param group
     * @param time
     * @throws SQLException
     */
    public void setLastVisit(User user, Group group, int time) throws SQLException
    {
        PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_group_user` SET `last_visit` = ? WHERE `group_id` = ? AND `user_id` = ?");
        update.setInt(1, time);
        update.setInt(2, group.getId());
        update.setInt(3, user.getId());
        update.executeUpdate();
        update.close();
    }

    /**
     * @param user
     * @param group
     * @return timestamp when the user has visited the group the last time
     * @throws SQLException
     */
    public int getLastVisit(User user, Group group) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT `last_visit` FROM `lw_group_user` WHERE `group_id` = ? AND `user_id` = ?");
        select.setInt(1, group.getId());
        select.setInt(2, user.getId());
        ResultSet rs = select.executeQuery();
        if(!rs.next())
            return -1;

        int time = rs.getInt(1);
        select.close();
        return time;
    }

    public List<GroupCategory> getGroupCategoriesByCourse(int courseId) throws SQLException
    {
        LinkedList<GroupCategory> categories = new LinkedList<GroupCategory>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT group_category_id, category_title, category_abbreviation FROM `lw_group_category` WHERE category_course_id = ? ORDER BY category_title");
        select.setInt(1, courseId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            categories.add(new GroupCategory(rs.getInt(1), courseId, rs.getString(2), rs.getString(3)));
        }

        select.close();
        return categories;
    }

    public GroupCategory getGroupCategoryById(int categoryId) throws SQLException
    {
        GroupCategory cat = null;

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT group_category_id, category_course_id, category_title, category_abbreviation FROM `lw_group_category` WHERE group_category_id = ?");
        select.setInt(1, categoryId);
        ResultSet rs = select.executeQuery();
        if(rs.next())
            cat = new GroupCategory(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4));

        select.close();
        return cat;
    }

    protected GroupCategory save(GroupCategory category) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_group_category` (group_category_id, course_id, category_title, category_abbreviation) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

        if(category.getId() < 0) // the Group is not yet stored at the database
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, category.getId());
        replace.setInt(1, category.getCourseId());
        replace.setString(2, category.getTitle());
        replace.setString(3, category.getAbbreviation());
        replace.executeUpdate();

        if(category.getId() < 0) // get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            category.setId(rs.getInt(1));

            resetCache();
        }

        replace.close();

        return category;
    }

    public TreeNode getFoldersTree(int groupId, int selectedFolderId) throws SQLException
    {
        if(groupId < 1)
        {
            return null;
        }

        Group group = getGroupById(groupId);
        TreeNode root = new DefaultTreeNode("GroupFolders");
        TreeNode rootFolder = new DefaultTreeNode("root", new Folder(0, group.getId(), group.getTitle()), root);
        if(selectedFolderId == 0)
        {
            rootFolder.setSelected(true);
            rootFolder.setExpanded(true);
        }
        getChildNodesRecursively(groupId, 0, rootFolder, selectedFolderId);
        return root;
    }

    public void getChildNodesRecursively(int groupId, int parentFolderId, TreeNode parent, int selectedFolderId) throws SQLException
    {
        for(Folder folder : this.getFolders(groupId, parentFolderId))
        {
            TreeNode folderNode = new DefaultTreeNode("folder", folder, parent);
            if(folder.getId() == selectedFolderId)
            {
                folderNode.setSelected(true);
                expand(folderNode);
            }
            getChildNodesRecursively(groupId, folder.getId(), folderNode, selectedFolderId);
        }
    }

    protected void expand(TreeNode treeNode)
    {
        if(treeNode.getParent() != null)
        {
            treeNode.getParent().setExpanded(true);
            expand(treeNode.getParent());
        }
    }

    public int getMemberCount(int groupId) throws SQLException
    {
        int count = 0;
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT COUNT(*) FROM lw_group_user WHERE group_id = ?");
        select.setInt(1, groupId);
        ResultSet rs = select.executeQuery();
        if(rs.next())
            count = rs.getInt(1);
        else
            throw new IllegalStateException("SQL query returned no result");

        select.close();

        return count;
    }
}

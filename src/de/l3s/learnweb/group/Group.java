package de.l3s.learnweb.group;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import jakarta.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceContainer;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

public class Group implements Comparable<Group>, HasId, Serializable, ResourceContainer {
    @Serial
    private static final long serialVersionUID = -6209978709028007958L;

    /**
     * Who can join this group?
     */
    public enum PolicyJoin { // be careful when adding options. The new option must be added to the lw_group table too
        ALL_LEARNWEB_USERS,
        ORGANISATION_MEMBERS,
        COURSE_MEMBERS,
        NOBODY
    }

    /**
     * Who can add resources and folders to this group?
     */
    public enum PolicyAdd { // be careful when adding options. The new option must be added to the lw_group table too
        GROUP_MEMBERS,
        GROUP_LEADER
    }

    /**
     * Who can delete or edit resources and folders of this group?
     */
    public enum PolicyEdit { // be careful when adding options. The new option must be added to the lw_group table too
        GROUP_MEMBERS,
        GROUP_LEADER_AND_FILE_OWNER,
        GROUP_LEADER
    }

    /**
     * Who can view resources of this group?
     */
    public enum PolicyView { // be careful when adding options. The new option must be added to the lw_group table too
        ALL_LEARNWEB_USERS,
        COURSE_MEMBERS,
        GROUP_MEMBERS,
        GROUP_LEADER
    }

    /**
     * Who can tag or comment resources of this group?
     */
    public enum PolicyAnnotate { // be careful when adding options. The new option must be added to the lw_group table too
        ALL_LEARNWEB_USERS,
        COURSE_MEMBERS,
        GROUP_MEMBERS,
        GROUP_LEADER
    }

    private int id;
    private int courseId;
    private int leaderUserId;
    private boolean deleted;
    @NotBlank
    @Length(min = 3, max = 60)
    private String title;
    @Length(max = 500)
    private String description;
    private int maxMemberCount = -1; // defines how many users can join this group
    private boolean restrictionForumCategoryRequired = false;
    private PolicyJoin policyJoin = PolicyJoin.COURSE_MEMBERS;
    private PolicyAdd policyAdd = PolicyAdd.GROUP_MEMBERS;
    private PolicyEdit policyEdit = PolicyEdit.GROUP_MEMBERS;
    private PolicyView policyView = PolicyView.COURSE_MEMBERS;
    private PolicyAnnotate policyAnnotate = PolicyAnnotate.COURSE_MEMBERS;
    private String hypothesisLink;
    private String hypothesisToken;
    private LocalDateTime createdAt;

    // caches
    private transient User leader;
    protected transient List<User> members;
    protected transient List<Folder> folders;
    private transient Course course;
    private long cacheTime = 0L;
    private int resourceCount = -1;
    private int memberCount = -1;
    private final HashMap<Integer, Instant> lastVisitCache = new HashMap<>();

    public Group() {
    }

    public Group(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public void clearCaches() {
        members = null;
        folders = null;
        resourceCount = -1;
        memberCount = -1;
    }

    @Override
    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public List<User> getMembers() {
        if (null == members) {
            members = Learnweb.dao().getUserDao().findByGroupId(id);
        }
        return members;
    }

    public int getMemberCount() {
        if (-1 == memberCount) {
            memberCount = Learnweb.dao().getGroupDao().countMembers(id);
        }
        return memberCount;
    }

    /**
     * @param user Returns TRUE if the user is member of this group
     */
    public boolean isMember(User user) {
        List<User> members = getMembers();

        return members.contains(user);
    }

    public boolean isLeader(User user) {
        return user.getId() == leaderUserId;
    }

    public User getLeader() {
        if (null == leader) {
            leader = Learnweb.dao().getUserDao().findByIdOrElseThrow(leaderUserId);
        }
        return leader;
    }

    public void setLeader(User user) {
        this.leaderUserId = user.getId();
        this.leader = user;
    }

    @Override
    public int compareTo(Group g) {
        return this.getTitle().compareTo(g.getTitle());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Group group = (Group) o;
        return id == group.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public List<Resource> getResources() {
        return Learnweb.dao().getResourceDao().findByGroupId(id);
    }

    public int getResourcesCount() {
        long now = System.currentTimeMillis();

        if (resourceCount == -1 || cacheTime < now - 3000L) {
            resourceCount = Learnweb.dao().getResourceDao().countByGroupId(id);
            cacheTime = now;
        }
        return resourceCount;
    }

    //metadata

    /**
     * Only root folders.
     */
    @Override
    public List<Folder> getSubFolders() {
        if (folders == null) {
            folders = Learnweb.dao().getFolderDao().findByGroupAndRootFolder(id);
        }

        return folders;
    }

    /**
     * Copy resource from this group to another group referred to by groupId, and by which user.
     */
    public void copyResources(int groupId, User user) {
        HashMap<Integer, Integer> foldersMap = new HashMap<>();
        foldersMap.put(0, 0);

        for (Folder folder : getSubFolders()) {
            copyFolderRecursive(folder, 0, groupId, user, foldersMap);
        }

        for (Resource resource : getResources()) {
            Resource newResource = resource.cloneResource();
            newResource.setGroupId(groupId);
            newResource.setFolderId(foldersMap.get(newResource.getFolderId()));
            newResource.setUser(user);

            for (File file : resource.getFiles().values()) {
                newResource.addFile(file);
            }

            newResource.save();
        }
    }

    private void copyFolderRecursive(final Folder folder, final int parentFolderId,
        final int groupId, final User user, final HashMap<Integer, Integer> foldersMap) {

        Folder newFolder = new Folder(folder);
        newFolder.setGroupId(groupId);
        newFolder.setParentFolderId(parentFolderId);
        newFolder.setUser(user);
        newFolder.save();

        foldersMap.put(folder.getId(), newFolder.getId());

        if (folder.getSubFolders() != null && !folder.getSubFolders().isEmpty()) {
            for (Folder subFolder : folder.getSubFolders()) {
                copyFolderRecursive(subFolder, newFolder.getId(), groupId, user, foldersMap);
            }
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The course of the user who created this group and which course this group will belong to.
     */
    public int getCourseId() {
        return courseId;
    }

    /**
     * The course which this group will belong to.
     */
    public void setCourseId(int courseId) {
        this.courseId = courseId;
        this.course = null;
    }

    public Course getCourse() {
        if (null == course) {
            course = Learnweb.dao().getCourseDao().findByIdOrElseThrow(courseId);
        }

        return course;
    }

    public int getLeaderUserId() {
        return leaderUserId;
    }

    public void setLeaderUserId(int userId) {
        this.leaderUserId = userId;
        this.leader = null; // force reload
    }

    public void setLastVisit(User user) {
        Instant now = Instant.now();
        Learnweb.dao().getGroupDao().insertLastVisitTime(now, this, user);
        lastVisitCache.put(user.getId(), now);
    }

    /**
     * @return time when the user has visited the group the last time; returns Instant. EPOCH if he never viewed the group
     */
    public Instant getLastVisit(User user) {
        Instant instant = lastVisitCache.get(user.getId());
        if (null != instant) {
            return instant;
        }

        instant = Learnweb.dao().getGroupDao().findLastVisitTime(this, user).orElse(Instant.EPOCH);
        lastVisitCache.put(user.getId(), instant);
        return instant;
    }

    /**
     * @param limit if limit is -1 all log entries are returned
     */
    public List<LogEntry> getLogs(int limit) {
        return Learnweb.dao().getLogDao().findByGroupId(id, Action.collectOrdinals(Action.LOGS_DEFAULT_FILTER), limit);
    }

    /**
     * Returns the 5 newest log entries.
     */
    public List<LogEntry> getLogs() {
        return getLogs(5);
    }

    public boolean isRestrictionForumCategoryEnabled() {
        return getCourse().getOption(Option.Groups_Forum_categories_enabled);
    }

    public boolean isRestrictionForumCategoryRequired() {
        return restrictionForumCategoryRequired;
    }

    public void setRestrictionForumCategoryRequired(boolean restrictionForumCategoryRequired) {
        this.restrictionForumCategoryRequired = restrictionForumCategoryRequired;
    }

    @Override
    public String toString() {
        return this.title;
    }

    public PolicyJoin getPolicyJoin() {
        return policyJoin;
    }

    public void setPolicyJoin(PolicyJoin policyJoin) {
        this.policyJoin = policyJoin;
    }

    public PolicyAdd getPolicyAdd() {
        return policyAdd;
    }

    public void setPolicyAdd(PolicyAdd policyAdd) {
        this.policyAdd = policyAdd;
    }

    public PolicyEdit getPolicyEdit() {
        return policyEdit;
    }

    public void setPolicyEdit(PolicyEdit policyEdit) {
        this.policyEdit = policyEdit;
    }

    public PolicyView getPolicyView() {
        return policyView;
    }

    public void setPolicyView(PolicyView policyView) {
        this.policyView = policyView;
    }

    public PolicyAnnotate getPolicyAnnotate() {
        return policyAnnotate;
    }

    public void setPolicyAnnotate(PolicyAnnotate policyAnnotate) {
        this.policyAnnotate = policyAnnotate;
    }

    public PolicyJoin[] getPolicyJoinOptions() {
        return PolicyJoin.values();
    }

    public PolicyAdd[] getPolicyAddOptions() {
        return PolicyAdd.values();
    }

    public PolicyEdit[] getPolicyEditOptions() {
        return PolicyEdit.values();
    }

    public PolicyView[] getPolicyViewOptions() {
        return PolicyView.values();
    }

    public PolicyAnnotate[] getPolicyAnnotateOptions() {
        return PolicyAnnotate.values();
    }

    public boolean canAddResources(User user) {
        if (user == null) { // not logged in
            return false;
        }

        if (user.isAdmin() || isLeader(user) || getCourse().isModerator(user)) {
            return true;
        }

        if (policyAdd == PolicyAdd.GROUP_MEMBERS && isMember(user)) {
            return true;
        }

        return false;
    }

    /**
     * Used for Drag&Drop functionality, using which it is possible to move resource between folders and groups.
     */
    public boolean canMoveResources(User user) {
        return canAddResources(user);
    }

    public boolean canDeleteResource(User user, AbstractResource resource) {
        return canEditResource(user, resource); // currently, they share the same policy
    }

    public boolean canEditResource(User user, AbstractResource resource) {
        if (user == null || resource == null) {
            return false;
        }

        if (getCourse().isModerator(user)) {
            return true;
        }

        return switch (policyEdit) {
            case GROUP_MEMBERS -> isMember(user);
            case GROUP_LEADER -> isLeader(user);
            case GROUP_LEADER_AND_FILE_OWNER -> isLeader(user) || resource.getUserId() == user.getId();
        };
    }

    public boolean canDeleteGroup(User user) {
        if (user == null) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user) || isLeader(user)) {
            return true;
        }

        return false;
    }

    public boolean canJoinGroup(User user) {
        if (user == null || isMember(user)) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user)) {
            return true;
        }

        return switch (policyJoin) {
            case ALL_LEARNWEB_USERS -> true;
            case ORGANISATION_MEMBERS -> getCourse().getOrganisationId() == user.getOrganisationId();
            case COURSE_MEMBERS -> getCourse().isMember(user);
            case NOBODY -> false;
        };
    }

    public boolean canViewResources(User user) {
        if (user == null) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user)) {
            return true;
        }

        return switch (policyView) {
            case ALL_LEARNWEB_USERS -> true;
            case COURSE_MEMBERS -> getCourse().isMember(user) || isMember(user);
            case GROUP_MEMBERS -> isMember(user);
            case GROUP_LEADER -> isLeader(user);
        };
    }

    public boolean canAnnotateResources(User user) {
        if (user == null) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user)) {
            return true;
        }

        return switch (policyAnnotate) {
            case ALL_LEARNWEB_USERS -> true;
            case COURSE_MEMBERS -> getCourse().isMember(user) || isMember(user);
            case GROUP_MEMBERS -> isMember(user);
            case GROUP_LEADER -> isLeader(user);
        };

    }

    public int getMaxMemberCount() {
        return maxMemberCount;
    }

    public void setMaxMemberCount(int maxMemberCount) {
        this.maxMemberCount = maxMemberCount;
    }

    public boolean isMemberCountLimited() {
        return maxMemberCount > -1;
    }

    public void setMemberCountLimited(boolean memberCountLimited) {
        if (!memberCountLimited) { // if no limit > set the member limit infinite
            maxMemberCount = -1;
        } else if (maxMemberCount < 1) { // if limit true but not defined yet > set default limit = 1
            maxMemberCount = 1;
        }
    }

    public String getHypothesisLink() {
        return hypothesisLink;
    }

    public void setHypothesisLink(String hypothesisLink) {
        this.hypothesisLink = hypothesisLink;
    }

    public String getHypothesisToken() {
        return hypothesisToken;
    }

    public void setHypothesisToken(String hypothesisToken) {
        this.hypothesisToken = hypothesisToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static TreeNode getFoldersTree(Group group, int activeFolder) {
        if (group == null) {
            return null;
        }

        TreeNode rootNode = new DefaultTreeNode("folder", new Folder(0, group.getId(), group.getTitle()), null);
        getChildNodesRecursively(rootNode, group, activeFolder);
        return rootNode;
    }

    public static void getChildNodesRecursively(TreeNode parentNode, ResourceContainer container, int activeFolderId) {
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

    private static void expandToNode(TreeNode treeNode) {
        if (treeNode.getParent() != null) {
            treeNode.getParent().setExpanded(true);
            expandToNode(treeNode.getParent());
        }
    }
}

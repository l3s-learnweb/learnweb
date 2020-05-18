package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceContainer;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

public class Group implements Comparable<Group>, HasId, Serializable, ResourceContainer {
    private static final long serialVersionUID = -6209978709028007958L;
    private static final Logger log = LogManager.getLogger(Group.class);

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
    private int leaderUserId;
    private User leader;
    private int courseId;

    @NotBlank
    @Length(min = 3, max = 60)
    private String title;
    @Length(max = 500)
    private String description;
    private String hypothesisLink;
    private String hypothesisToken;

    private PolicyJoin policyJoin = Group.PolicyJoin.COURSE_MEMBERS;
    private PolicyAdd policyAdd = Group.PolicyAdd.GROUP_MEMBERS;
    private PolicyEdit policyEdit = Group.PolicyEdit.GROUP_MEMBERS;
    private PolicyView policyView = Group.PolicyView.COURSE_MEMBERS;
    private PolicyAnnotate policyAnnotate = Group.PolicyAnnotate.COURSE_MEMBERS;

    private boolean restrictionForumCategoryRequired = false;
    private int maxMemberCount = -1; // defines how many users can join this group

    // caches
    protected transient List<User> members;
    protected transient List<Folder> folders;
    private transient Course course;
    private long cacheTime = 0L;
    private int resourceCount = -1;
    private int memberCount = -1;
    private final HashMap<Integer, Integer> lastVisitCache = new HashMap<>();

    public Group() {
        this.id = -1;
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

    public List<User> getMembers() throws SQLException {
        if (null == members) {
            members = Learnweb.getInstance().getUserManager().getUsersByGroupId(id);
        }
        return members;
    }

    public int getMemberCount() throws SQLException {
        if (-1 == memberCount) {
            memberCount = Learnweb.getInstance().getGroupManager().getMemberCount(id);
        }
        return memberCount;
    }

    /**
     * @param user Returns TRUE if the user is member of this group
     */
    public boolean isMember(User user) throws SQLException {
        List<User> members = getMembers();

        return members.contains(user);
    }

    public boolean isLeader(User user) throws SQLException {
        return user.getId() == leaderUserId;
    }

    public User getLeader() throws SQLException {
        if (null == leader) {
            leader = Learnweb.getInstance().getUserManager().getUser(leaderUserId);
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

    public List<Resource> getResources() throws SQLException {
        return Learnweb.getInstance().getResourceManager().getResourcesByGroupId(id);

    }

    public int getResourcesCount() throws SQLException {
        long now = System.currentTimeMillis();

        if (resourceCount == -1 || cacheTime < now - 3000L) {
            resourceCount = Learnweb.getInstance().getResourceManager().getResourceCountByGroupId(id);
            cacheTime = now;
        }
        return resourceCount;
    }

    //metadata

    /**
     * Only root folders.
     */
    @Override
    public List<Folder> getSubFolders() throws SQLException {
        if (folders == null) {
            folders = Learnweb.getInstance().getGroupManager().getFolders(id, 0);
        }

        return folders;
    }

    /**
     * Copy resource from this group to another group referred to by groupId, and by which user.
     */
    public void copyResourcesToGroupById(int groupId, User user) throws SQLException {
        for (Resource resource : getResources()) {
            Resource newResource = resource.clone();
            newResource.setGroupId(groupId);
            user.addResource(newResource);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) throws SQLException {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) throws SQLException {
        this.description = description == null ? null : description.trim();
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

    public Course getCourse() throws SQLException {
        if (null == this.course) {
            this.course = Learnweb.getInstance().getCourseManager().getCourseById(courseId);
        }

        return this.course;
    }

    public int getLeaderUserId() {
        return leaderUserId;
    }

    public void setLeaderUserId(int userId) {
        this.leaderUserId = userId;
        this.leader = null; // force reload
    }

    public void setLastVisit(User user) throws SQLException {
        int time = time();
        Learnweb.getInstance().getGroupManager().setLastVisit(user, this, time);
        lastVisitCache.put(user.getId(), time);
    }

    /**
     * @return unix timestamp when the user has visited the group the last time; returns -1 if he never viewed the group
     */
    public int getLastVisit(User user) throws Exception {
        Integer time = lastVisitCache.get(user.getId());
        if (null != time) {
            return time;
        }

        time = Learnweb.getInstance().getGroupManager().getLastVisit(user, this);
        lastVisitCache.put(user.getId(), time);
        return time;
    }

    /**
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entries are returned
     */
    public List<LogEntry> getLogs(Action[] actions, int limit) throws SQLException {
        return Learnweb.getInstance().getLogManager().getLogsByGroup(id, actions, limit);
    }

    /**
     * Returns the 5 newest log entries.
     */
    public List<LogEntry> getLogs() throws SQLException {
        return getLogs(null, 5);
    }

    public boolean isRestrictionForumCategoryEnabled() {
        try {
            return getCourse().getOption(Option.Groups_Forum_categories_enabled);
        } catch (SQLException e) {
            log.fatal("can't load setting", e);
            return false;
        }
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
        return Group.PolicyJoin.values();
    }

    public PolicyAdd[] getPolicyAddOptions() {
        return Group.PolicyAdd.values();
    }

    public PolicyEdit[] getPolicyEditOptions() {
        return Group.PolicyEdit.values();
    }

    public PolicyView[] getPolicyViewOptions() {
        return Group.PolicyView.values();
    }

    public PolicyAnnotate[] getPolicyAnnotateOptions() {
        return Group.PolicyAnnotate.values();
    }

    public boolean canAddResources(User user) throws SQLException {
        if (user == null) { // not logged in
            return false;
        }

        if (user.isAdmin() || isLeader(user) || getCourse().isModerator(user)) {
            return true;
        }

        if (policyAdd == Group.PolicyAdd.GROUP_MEMBERS && isMember(user)) {
            return true;
        }

        return false;
    }

    /**
     * Used for Drag&Drop functionality, using which it is possible to move resource between folders and groups.
     */
    public boolean canMoveResources(User user) throws SQLException {
        return canAddResources(user);
    }

    public boolean canDeleteResource(User user, AbstractResource resource) throws SQLException {
        return canEditResource(user, resource); // currently they share the same policy
    }

    public boolean canEditResource(User user, AbstractResource resource) throws SQLException {
        if (user == null || resource == null) {
            return false;
        }

        if (getCourse().isModerator(user)) {
            return true;
        }

        switch (policyEdit) {
            case GROUP_MEMBERS:
                return isMember(user);
            case GROUP_LEADER:
                return isLeader(user);
            case GROUP_LEADER_AND_FILE_OWNER:
                return isLeader(user) || resource.getUserId() == user.getId();
            default:
                log.error("Unknown edit policy: {}", policyEdit);
        }

        throw new NotImplementedException("this should never happen");
    }

    public boolean canDeleteGroup(User user) throws SQLException {
        if (user == null) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user) || isLeader(user)) {
            return true;
        }

        return false;
    }

    public boolean canJoinGroup(User user) throws SQLException {
        if (user == null || isMember(user)) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user)) {
            return true;
        }

        switch (policyJoin) {
            case ALL_LEARNWEB_USERS:
                return true;
            case ORGANISATION_MEMBERS:
                return getCourse().getOrganisationId() == user.getOrganisationId();
            case COURSE_MEMBERS:
                return getCourse().isMember(user);
            case NOBODY:
                return false;
            default:
                log.error("Unknown join policy: {}", policyJoin);
        }

        throw new NotImplementedException("this should never happen");
    }

    public boolean canViewResources(User user) throws SQLException {
        if (user == null) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user)) {
            return true;
        }

        switch (policyView) {
            case ALL_LEARNWEB_USERS:
                return true;
            case COURSE_MEMBERS:
                return getCourse().isMember(user) || isMember(user);
            case GROUP_MEMBERS:
                return isMember(user);
            case GROUP_LEADER:
                return isLeader(user);
            default:
                log.error("Unknown view policy: {}", policyView);
        }

        return false;
    }

    public boolean canAnnotateResources(User user) throws SQLException {
        if (user == null) {
            return false;
        }

        if (user.isAdmin() || getCourse().isModerator(user)) {
            return true;
        }

        switch (policyAnnotate) {
            case ALL_LEARNWEB_USERS:
                return true;
            case COURSE_MEMBERS:
                return getCourse().isMember(user) || isMember(user);
            case GROUP_MEMBERS:
                return isMember(user);
            case GROUP_LEADER:
                return isLeader(user);
            default:
                log.error("Unknown annotate policy: {}", policyAnnotate);
        }

        return false;
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
        } else if (maxMemberCount <= 0) { // if limit true but not defined yet > set default limit = 1
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

    public boolean isGoogleDocsSignInEnabled() throws SQLException {
        return getCourse().getOption(Course.Option.Groups_Google_Docs_sign_in_enabled);
    }

    /**
     * @see de.l3s.learnweb.group.GroupManager#deleteGroupHard
     */
    public void deleteHard() throws SQLException {
        Learnweb.getInstance().getGroupManager().deleteGroupHard(this);
    }

    /**
     * Flags the group and deleted and removes all users from the group.
     */
    public void delete() throws SQLException {
        Learnweb.getInstance().getGroupManager().deleteGroupSoft(this);
    }

    private static int time() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}

package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Link.LinkType;
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

public class Group implements Comparable<Group>, HasId, Serializable, ResourceContainer
{
    private static final long serialVersionUID = -6209978709028007958L;
    private static final Logger log = Logger.getLogger(Group.class);

    private int id;
    private int leaderUserId;
    private User leader;
    private int courseId;

    @NotBlank
    @Length(min = 3, max = 60)
    private String title;
    @Length(max = 500)
    private String description;
    private String metadata1;
    private String hypothesisLink;
    private String hypothesisToken;
    @Length(max = 50)
    private String language;
    private int categoryId;

    // restrictions / access policy

    /**
     * Who can join this group? *
     */
    public enum POLICY_JOIN // be careful when adding options. The new option must be added to the lw_group table too
    {
        ALL_LEARNWEB_USERS,
        COURSE_MEMBERS,
        NOBODY
    }

    /**
     * Who can add resources and folders to this group? *
     */
    public enum POLICY_ADD // be careful when adding options. The new option must be added to the lw_group table too
    {
        GROUP_MEMBERS,
        GROUP_LEADER
    }

    /**
     * Who can delete or edit resources and folders of this group?
     */
    public enum POLICY_EDIT // be careful when adding options. The new option must be added to the lw_group table too
    {
        GROUP_MEMBERS,
        GROUP_LEADER,
        GROUP_LEADER_AND_FILE_OWNER
    }

    /**
     * Who can view resources of this group?
     */
    public enum POLICY_VIEW // be careful when adding options. The new option must be added to the lw_group table too
    {
        ALL_LEARNWEB_USERS,
        COURSE_MEMBERS,
        GROUP_MEMBERS,
        GROUP_LEADER
    }

    /**
     * Who can tag or comment resources of this group?
     */
    public enum POLICY_ANNOTATE // be careful when adding options. The new option must be added to the lw_group table too
    {
        ALL_LEARNWEB_USERS,
        COURSE_MEMBERS,
        GROUP_MEMBERS,
        GROUP_LEADER
    }

    private POLICY_JOIN policyJoin = POLICY_JOIN.COURSE_MEMBERS;
    private POLICY_ADD policyAdd = POLICY_ADD.GROUP_MEMBERS;
    private POLICY_EDIT policyEdit = POLICY_EDIT.GROUP_MEMBERS;
    private POLICY_VIEW policyView = POLICY_VIEW.ALL_LEARNWEB_USERS;
    private POLICY_ANNOTATE policyAnnotate = POLICY_ANNOTATE.ALL_LEARNWEB_USERS;

    private boolean restrictionForumCategoryRequired = false;
    private boolean restrictionAnonymousResources = false; // the owner of resources is not shown
    private int maxMemberCount = -1; // defines how many users can join this group

    // caches
    private String categoryTitle;
    private String categoryAbbreviation;
    protected transient List<Link> documentLinks;
    protected transient List<User> members;
    protected transient List<Link> links;
    protected transient List<Folder> folders;
    private transient Course course;
    private long cacheTime = 0L;
    private int resourceCount = -1;
    private int memberCount = -1;
    private int linkCount = -1;
    private HashMap<Integer, Integer> lastVisitCache = new HashMap<>();

    public void clearCaches()
    {
        documentLinks = null;
        members = null;
        links = null;
        folders = null;
        resourceCount = -1;
        memberCount = -1;
    }

    public Group()
    {
        this.id = -1;
    }

    public Group(int id, String title)
    {
        this.id = id;
        this.title = title;
    }

    @Override
    public int getId()
    {
        return id;
    }

    public List<User> getMembers() throws SQLException
    {
        if(null == members)
        {
            members = Learnweb.getInstance().getUserManager().getUsersByGroupId(id);
        }
        return members;
    }

    public int getMemberCount() throws SQLException
    {
        if(-1 == memberCount)
        {
            memberCount = Learnweb.getInstance().getGroupManager().getMemberCount(id);
        }
        return memberCount;
    }

    /**
     *
     * @param user Returns TRUE if the user is member of this group
     * @throws SQLException
     */
    public boolean isMember(User user) throws SQLException
    {
        List<User> members = getMembers();

        return members.contains(user);
    }

    public boolean isLeader(User user) throws SQLException
    {
        return user.getId() == leaderUserId;
    }

    public User getLeader() throws SQLException
    {
        if(null == leader)
            leader = Learnweb.getInstance().getUserManager().getUser(leaderUserId);
        return leader;
    }

    @Override
    public int compareTo(Group g)
    {
        return this.getTitle().compareTo(g.getTitle());
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj.getClass() == this.getClass())
        {
            Group g2 = (Group) obj;
            return g2.getId() == this.getId();
        }

        return false;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByGroupId(id);

    }

    public int getResourcesCount() throws SQLException
    {
        long now = System.currentTimeMillis();

        if(resourceCount == -1 || cacheTime < now - 3000L)
        {
            resourceCount = Learnweb.getInstance().getResourceManager().getResourceCountByGroupId(id);
            cacheTime = now;
        }
        return resourceCount;
    }

    /**
     * Only root folders
     *
     * @return
     * @throws SQLException
     */
    @Override
    public List<Folder> getSubFolders() throws SQLException
    {
        if(folders == null)
        {
            folders = Learnweb.getInstance().getGroupManager().getFolders(id, 0);
        }

        return folders;
    }

    /**
     * Copy resource from this group to another group referred to by groupId, and by which user
     */
    public void copyResourcesToGroupById(int groupId, User user) throws SQLException
    {
        for(Resource resource : getResources())
        {
            Resource newResource = resource.clone();
            newResource.setGroupId(groupId);
            newResource = user.addResource(newResource);
        }
    }

    //metadata

    public String getTitle()
    {
        return title;
    }

    /**
     * Title + category abbreviation (if group is categorized)
     *
     * @return
     */
    public String getLongTitle()
    {
        if(StringUtils.isNotEmpty(categoryAbbreviation))
            return "[" + categoryAbbreviation + "] " + title;

        return title;
    }

    public void setTitle(String title) throws SQLException
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description) throws SQLException
    {
        this.description = description == null ? null : description.trim();
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getMetadata1()
    {
        return metadata1;
    }

    public void setMetadata1(String metadata)
    {
        this.metadata1 = metadata;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * The course of the user who created this group and which course this group will belong to
     *
     * @return
     */
    public int getCourseId()
    {
        return courseId;
    }

    public Course getCourse() throws SQLException
    {
        if(null == this.course)
            this.course = Learnweb.getInstance().getCourseManager().getCourseById(courseId);

        return this.course;
    }

    /**
     * The course which this group will belong to
     */
    public void setCourseId(int courseId)
    {
        this.courseId = courseId;
        this.course = null;
    }

    public int getLeaderUserId()
    {
        return leaderUserId;
    }

    public void setLeader(User user) throws SQLException
    {
        this.leaderUserId = user.getId();
        this.leader = user;
    }

    public void setLeaderUserId(int userId) throws SQLException
    {
        this.leaderUserId = userId;
        this.leader = null; // force reload
    }

    public List<Link> getDocumentLinks() throws SQLException
    {
        if(null == documentLinks)
            documentLinks = Learnweb.getInstance().getLinkManager().getLinksByGroupId(id, LinkType.DOCUMENT);

        return documentLinks;
    }

    public List<Link> getLinks() throws SQLException
    {
        if(null == links)
            links = Learnweb.getInstance().getLinkManager().getLinksByGroupId(id, LinkType.LINK);

        return Collections.unmodifiableList(links);
    }

    public boolean hasLinks()
    {
        try
        {
            if(linkCount < 0)
                linkCount = Learnweb.getInstance().getLinkManager().getLinksByGroupId(id, LinkType.LINK).size();

            return linkCount > 0;
        }
        catch(SQLException e)
        {
            log.error(e);
        }
        return false;
    }

    public void addLink(String title, String url, LinkType type) throws SQLException
    {
        Link link = new Link();
        link.setGroupId(id);
        link.setType(type);
        link.setTitle(title);
        link.setUrl(url);
        Learnweb.getInstance().getLinkManager().save(link);

        links = null; // force reload
        documentLinks = null;
    }

    public void deleteLink(int linkId) throws SQLException
    {
        Learnweb.getInstance().getLinkManager().deleteLink(linkId);

        links = null; // force reload
        documentLinks = null;
    }

    public static int time()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public void setLastVisit(User user) throws SQLException
    {
        int time = time();
        Learnweb.getInstance().getGroupManager().setLastVisit(user, this, time);
        lastVisitCache.put(user.getId(), time);
    }

    /**
     *
     * @param user
     * @return unix timestamp when the user has visited the group the last time; returns -1 if he never viewed the group
     * @throws Exception
     */
    public int getLastVisit(User user) throws Exception
    {
        Integer time = lastVisitCache.get(user.getId());
        if(null != time)
            return time;

        time = Learnweb.getInstance().getGroupManager().getLastVisit(user, this);
        lastVisitCache.put(user.getId(), time);
        return time;
    }

    /**
     *
     * @param actions if actions is null the default filter is used
     * @param limit if limit is -1 all log entries are returned
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getLogs(Action[] actions, int limit) throws SQLException
    {
        return Learnweb.getInstance().getLogManager().getLogsByGroup(id, actions, limit);
    }

    /**
     * returns the 5 newest log entries
     *
     * @return
     * @throws SQLException
     */
    public List<LogEntry> getLogs() throws SQLException
    {
        return getLogs(null, 5);
    }

    public int getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(int categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryTitle()
    {
        return categoryTitle;
    }

    public void setCategoryTitle(String categoryTitle)
    {
        this.categoryTitle = categoryTitle;
    }

    public String getCategoryAbbreviation()
    {
        return categoryAbbreviation;
    }

    public void setCategoryAbbreviation(String categoryAbbreviation)
    {
        this.categoryAbbreviation = categoryAbbreviation;
    }

    public boolean isRestrictionForumCategoryEnabled()
    {
        try
        {
            return getCourse().getOption(Option.Groups_Forum_categories_enabled);
        }
        catch(SQLException e)
        {
            log.fatal("can't load setting", e);
            return false;
        }
    }

    public boolean isRestrictionForumCategoryRequired()
    {
        return restrictionForumCategoryRequired;
    }

    public void setRestrictionForumCategoryRequired(boolean restrictionForumCategoryRequired)
    {
        this.restrictionForumCategoryRequired = restrictionForumCategoryRequired;
    }

    @Override
    public String toString()
    {
        return this.title;
    }

    public POLICY_JOIN getPolicyJoin()
    {
        return policyJoin;
    }

    public void setPolicyJoin(POLICY_JOIN policyJoin)
    {
        this.policyJoin = policyJoin;
    }

    public POLICY_ADD getPolicyAdd()
    {
        return policyAdd;
    }

    public void setPolicyAdd(POLICY_ADD policyAdd)
    {
        this.policyAdd = policyAdd;
    }

    public POLICY_EDIT getPolicyEdit()
    {
        return policyEdit;
    }

    public void setPolicyEdit(POLICY_EDIT policyEdit)
    {
        this.policyEdit = policyEdit;
    }

    public POLICY_VIEW getPolicyView()
    {
        return policyView;
    }

    public void setPolicyView(POLICY_VIEW policyView)
    {
        this.policyView = policyView;
    }

    public POLICY_ANNOTATE getPolicyAnnotate()
    {
        return policyAnnotate;
    }

    public void setPolicyAnnotate(POLICY_ANNOTATE policyAnnotate)
    {
        this.policyAnnotate = policyAnnotate;
    }

    public POLICY_JOIN[] getPolicyJoinOptions()
    {
        return POLICY_JOIN.values();
    }

    public POLICY_ADD[] getPolicyAddOptions()
    {
        return POLICY_ADD.values();
    }

    public POLICY_EDIT[] getPolicyEditOptions()
    {
        return POLICY_EDIT.values();
    }

    public POLICY_VIEW[] getPolicyViewOptions()
    {
        return POLICY_VIEW.values();
    }

    public POLICY_ANNOTATE[] getPolicyAnnotateOptions()
    {
        return POLICY_ANNOTATE.values();
    }

    public boolean canAddResources(User user) throws SQLException
    {
        if(user == null) // not logged in
            return false;

        if(user.isAdmin() || isLeader(user) || getCourse().isModerator(user))
            return true;

        if(policyAdd == POLICY_ADD.GROUP_MEMBERS && isMember(user))
            return true;

        return false;
    }

    /**
     * Used for Drag&Drop functionality, using which it is possible to move resource between folders and groups
     */
    public boolean canMoveResources(User user) throws SQLException
    {
        return canAddResources(user);
    }

    public boolean canDeleteResource(User user, AbstractResource resource) throws SQLException
    {
        return canEditResource(user, resource); // currently they share the same policy
    }

    public boolean canEditResource(User user, AbstractResource resource) throws SQLException
    {
        if(user == null) // not logged in
            return false;

        if(user.isAdmin() || isLeader(user) || getCourse().isModerator(user))
            return true;

        if(policyEdit == POLICY_EDIT.GROUP_MEMBERS && isMember(user))
            return true;

        if(policyEdit == POLICY_EDIT.GROUP_LEADER_AND_FILE_OWNER && resource != null && user != null && resource.getUserId() == user.getId())
            return true;

        return false;
    }

    public boolean canDeleteGroup(User user) throws SQLException
    {
        if(user == null)
            return false;

        if(user.isAdmin() || getCourse().isModerator(user) || isLeader(user))
            return true;

        return false;
    }

    public boolean canJoinGroup(User user) throws SQLException
    {
        if(user == null || isMember(user))
            return false;

        if(user.isAdmin() || getCourse().isModerator(user))
            return true;

        switch(policyJoin)
        {
        case ALL_LEARNWEB_USERS:
            return true;
        case COURSE_MEMBERS:
            return getCourse().isMember(user);
        case NOBODY:
            return false;
        }

        return false;
    }

    public boolean canViewResources(User user) throws SQLException
    {
        if(user == null)
            return false;

        if(user.isAdmin() || getCourse().isModerator(user))
            return true;

        //noinspection Duplicates
        switch(policyView)
        {
        case ALL_LEARNWEB_USERS:
            return true;
        case COURSE_MEMBERS:
            return getCourse().isMember(user) || isMember(user);
        case GROUP_MEMBERS:
            return isMember(user);
        case GROUP_LEADER:
            return isLeader(user);
        }

        return false;
    }

    public boolean canAnnotateResources(User user) throws SQLException
    {
        if(user == null)
            return false;

        if(user.isAdmin() || getCourse().isModerator(user))
            return true;

        //noinspection Duplicates
        switch(policyAnnotate)
        {
        case ALL_LEARNWEB_USERS:
            return true;
        case COURSE_MEMBERS:
            return getCourse().isMember(user) || isMember(user);
        case GROUP_MEMBERS:
            return isMember(user);
        case GROUP_LEADER:
            return isLeader(user);
        }

        return false;
    }

    /**
     * the owner of resources is not shown if true
     *
     * @return
     */
    public boolean isRestrictionAnonymousResources()
    {
        return restrictionAnonymousResources;
    }

    public void setRestrictionAnonymousResources(boolean restrictionAnonymousResources)
    {
        this.restrictionAnonymousResources = restrictionAnonymousResources;
    }

    public int getMaxMemberCount()
    {
        return maxMemberCount;
    }

    public void setMaxMemberCount(int maxMemberCount)
    {
        this.maxMemberCount = maxMemberCount;
    }

    public boolean isMemberCountLimited()
    {
        return maxMemberCount > -1;
    }

    public void setMemberCountLimited(boolean memberCountLimited)
    {
        if(!memberCountLimited) // if no limit > set the member limit infinite
            maxMemberCount = -1;
        else if(maxMemberCount <= 0) // if limit true but not defined yet > set default limit = 1
            maxMemberCount = 1;
    }

    public String getHypothesisLink()
    {
        return hypothesisLink;
    }

    public void setHypothesisLink(String hypothesisLink)
    {
        this.hypothesisLink = hypothesisLink;
    }

    public String getHypothesisToken()
    {
        return hypothesisToken;
    }

    public void setHypothesisToken(String hypothesisToken)
    {
        this.hypothesisToken = hypothesisToken;
    }

    public boolean isGoogleDocsSignInEnabled() throws SQLException
    {
        return getCourse().getOption(Course.Option.Groups_Google_Docs_sign_in_enabled);
    }

    /**
     * @see de.l3s.learnweb.group.GroupManager.deleteGroupHard
     * @throws SQLException
     */
    public void deleteHard() throws SQLException
    {
        Learnweb.getInstance().getGroupManager().deleteGroupHard(this);
    }

    /**
     * Flags the group and deleted and removes all users from the group
     */
    public void delete() throws SQLException
    {
        Learnweb.getInstance().getGroupManager().deleteGroupSoft(this);
    }
}

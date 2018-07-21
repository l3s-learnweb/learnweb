package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.BitSet;
import java.util.List;

import javax.validation.constraints.Size;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupCategory;
import de.l3s.util.HasId;

public class Course implements Serializable, Comparable<Course>, HasId
{
    private static final long serialVersionUID = -1101352995500154406L;
    private static final Logger log = Logger.getLogger(Course.class);

    // add new options add the end , don't delete options !!!!!
    // if you add 64 options you have to add one options_field{x} column in lw_course
    public static enum Option implements Comparable<Option>
    {
        Unused_1,
        Unused_2,
        Unused_3,
        Unused_4,
        Users_Require_mail_address,
        Users_Disable_Wizard,
        Groups_Google_Docs_Sign_In_enabled,
        Users_Require_Affiliation,
        Users_Require_Student_Id
    }

    private int id = -1;
    @Size(min = 1, max = 40)
    private String title;
    private int organisationId;
    private int defaultGroupId; // all users who join this course, automatically join this group
    @Size(min = 1, max = 90)
    private String wizardParam;
    private int nextXUsersBecomeModerator;
    @Size(min = 0, max = 65000)
    private String welcomeMessage;

    private BitSet options = new BitSet();

    // derived/cached values:
    private int memberCount = -1;
    private List<GroupCategory> groupCategories;

    public Course()
    {
    }

    protected void setOptions(long[] optionValues)
    {
        this.options = BitSet.valueOf(optionValues);
    }

    public boolean getOption(Option option)
    {
        return options.get(option.ordinal());
    }

    public void setOption(Option option, boolean value)
    {
        options.set(option.ordinal(), value);
    }

    public List<Group> getGroups() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getGroupsByCourseId(id);
    }

    public List<Group> getGroupsFilteredByUser(User user) throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getGroupsByUserIdFilteredByCourseId(user.getId(), id);
    }

    /**
     * A negative id indicates, that this object is not stored at the database
     *
     * @return
     */
    @Override
    public int getId()
    {
        return id;
    }

    /**
     * This method should only be called by CourseManager
     *
     * @param id
     */
    public void setId(int id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    protected long[] getOptions()
    {
        return options.toLongArray();
    }

    public int getOrganisationId()
    {
        return organisationId;
    }

    public Organisation getOrganisation() throws SQLException
    {
        if(organisationId < 0)
            return null;

        return Learnweb.getInstance().getOrganisationManager().getOrganisationById(organisationId);
    }

    public void setOrganisationId(int organisationId)
    {
        this.organisationId = organisationId;
    }

    public int getDefaultGroupId()
    {
        return defaultGroupId;
    }

    public void setDefaultGroupId(int defaultGroupId)
    {
        this.defaultGroupId = defaultGroupId;
    }

    public String getWizardParam()
    {
        return wizardParam;
    }

    public void setWizardParam(String wizardParam)
    {
        this.wizardParam = wizardParam;
    }

    public int getNextXUsersBecomeModerator()
    {
        return nextXUsersBecomeModerator;
    }

    public void setNextXUsersBecomeModerator(int nextXUsersBecomeModerator)
    {
        this.nextXUsersBecomeModerator = nextXUsersBecomeModerator;
    }

    public int getMemberCount() throws SQLException
    {
        if(memberCount == -1)
        {
            memberCount = Learnweb.getInstance().getUserManager().getUsersByCourseId(id).size();
        }
        return memberCount;
    }

    public List<User> getMembers() throws SQLException
    {
        return Learnweb.getInstance().getUserManager().getUsersByCourseId(id);
    }

    /**
     *
     * @return The userIds of all course members
     * @throws SQLException
     */
    public List<Integer> getUserIds() throws SQLException
    {
        return HasId.collectIds(getMembers());
    }

    public void addUser(User user) throws SQLException
    {
        if(memberCount != -1)
            memberCount++;
        Learnweb.getInstance().getCourseManager().addUser(this, user);
        user.clearCaches();
    }

    public void removeUser(User user) throws SQLException
    {
        if(memberCount != -1)
            memberCount--;
        Learnweb.getInstance().getCourseManager().removeUser(this, user);
        user.clearCaches();
    }

    /**
     * True if the given user is a moderator if this course
     *
     * @param user
     * @return True if the given user is a moderator of this course
     * @throws SQLException
     */
    public boolean isModerator(User user) throws SQLException
    {
        if(user.isAdmin())
            return true;

        if(!user.isModerator())
            return false;

        return isMember(user); // moderators can only moderator her own courses
    }

    public boolean isMember(User user) throws SQLException
    {
        return user.getCourses().contains(this); // moderators can only moderator her own courses
    }

    public String getWelcomeMessage()
    {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage)
    {
        this.welcomeMessage = welcomeMessage;
    }

    @Override
    public int compareTo(Course o) // sort primarily by organisationTitle, secondarily by course title
    {
        if(o.getOrganisationId() == getOrganisationId())
            return getTitle().compareTo(o.getTitle());

        try
        {
            String title1 = (getOrganisationId() < 1) ? "" : getOrganisation().getTitle();
            String title2 = (o.getOrganisationId() < 1) ? "" : o.getOrganisation().getTitle();
            return title1.compareTo(title2);
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);

            return 0;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null)
            return false;
        if(obj.getClass() != getClass())
            return false;

        Course otherCourse = (Course) obj;
        return otherCourse.getId() == getId();
    }

    public List<GroupCategory> getGroupCategories() throws SQLException
    {
        if(null == groupCategories)
            groupCategories = Learnweb.getInstance().getGroupManager().getGroupCategoriesByCourse(id);

        return groupCategories;
    }

    public void addGroupCategory(GroupCategory category) throws SQLException
    {
        category.setCourseId(id);
        Learnweb.getInstance().getGroupManager().save(category);

        if(groupCategories != null)
            groupCategories = null; // clear cache
    }

    @Override
    public String toString()
    {
        return "Course [id=" + id + ", title=" + title + "]";
    }

}

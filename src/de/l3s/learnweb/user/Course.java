package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.util.HasId;

public class Course implements Serializable, Comparable<Course>, HasId
{
    private static final long serialVersionUID = -1101352995500154406L;
    private static final Logger log = Logger.getLogger(Course.class);

    // add new options add the end , don't delete options !!!!!
    // if you add 64 options you have to add one options_field{x} column in lw_course
    public enum Option implements Comparable<Option>
    {
        Unused_1,
        Groups_Hypothesis_enabled,
        Groups_Forum_categories_enabled,
        Groups_Only_moderators_can_create_groups,
        Users_Require_mail_address,
        Users_Disable_wizard,
        Groups_Google_Docs_sign_in_enabled,
        Users_Require_affiliation,
        Users_Require_student_id
    }

    private int id = -1;
    @NotBlank
    @Length(min = 2, max = 40)
    private String title;
    private int organisationId;
    private int defaultGroupId; // all users who join this course, automatically join this group
    @NotBlank
    @Length(min = 2, max = 90)
    private String wizardParam;
    private int nextXUsersBecomeModerator;
    @Length(max = 65000)
    private String welcomeMessage;
    private LocalDateTime creationTimestamp;

    private BitSet options = new BitSet(Option.values().length);

    // derived/cached values:
    private int memberCount = -1;

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
        long[] array = options.toLongArray();
        // if all values are false the array will be empty. But we need to return an array representing the values
        if(array.length == 0)
            array = new long[1];
        return array;
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

    public synchronized void addUser(User user) throws SQLException
    {
        if(memberCount != -1)
            memberCount++;

        if(nextXUsersBecomeModerator > 0)
        {
            log.debug("User: " + user.getUsername() + " becomes moderator of course: " + getTitle());

            user.setModerator(true);

            nextXUsersBecomeModerator--;
            save();
        }

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
    public int compareTo(Course o)
    {
        return getTitle().compareTo(o.getTitle());
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

    @Override
    public String toString()
    {
        return "Course [id=" + id + ", title=" + title + "]";
    }

    public LocalDateTime getCreationTimestamp()
    {
        return creationTimestamp;
    }

    public void setCreationTimestamp(LocalDateTime creationTimestamp)
    {
        this.creationTimestamp = creationTimestamp;
    }

    public void save() throws SQLException
    {
        Learnweb.getInstance().getCourseManager().save(this);
    }
}

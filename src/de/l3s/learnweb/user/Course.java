package de.l3s.learnweb.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.util.HasId;

public class Course implements Serializable, Comparable<Course>, HasId {
    private static final long serialVersionUID = -1101352995500154406L;
    private static final Logger log = LogManager.getLogger(Course.class);

    // add new options add the end , don't delete options !!!!!
    // if you add 64 options you have to add one options_field{x} column in lw_course
    public enum Option {
        Unused_1,
        Groups_Hypothesis_enabled,
        Groups_Forum_categories_enabled,
        Groups_Only_moderators_can_create_groups,
        Users_Require_mail_address,
        Users_Disable_wizard,
        Unused_6,
        Users_Require_affiliation,
        Users_Require_student_id
    }

    private int id;
    @NotBlank
    @Length(min = 2, max = 40)
    private String title;
    private int organisationId;
    private int defaultGroupId; // all users who join this course, automatically join this group
    @Length(min = 2, max = 90)
    private String wizardParam;
    private int nextXUsersBecomeModerator;
    @Length(max = 65000)
    private String welcomeMessage;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    private BitSet options = new BitSet(Option.values().length);

    // derived/cached values:
    private int memberCount = -1;

    public Course() {
        // set default values; They are false by default
        setOption(Option.Users_Require_mail_address, true);
    }

    public boolean getOption(Option option) {
        return options.get(option.ordinal());
    }

    public void setOption(Option option, boolean value) {
        options.set(option.ordinal(), value);
    }

    public List<Group> getGroups() {
        return Learnweb.dao().getGroupDao().findByCourseId(id);
    }

    public List<Group> getGroupsFilteredByUser(User user) {
        return Learnweb.dao().getGroupDao().findByUserIdAndCourseId(user.getId(), id);
    }

    /**
     * Zero id indicates, that this object is not stored at the database.
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * This method should only be called by CourseManager.
     */
    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    protected long[] getOptions() {
        long[] array = options.toLongArray();
        // if all values are false the array will be empty. But we need to return an array representing the values
        if (array.length == 0) {
            array = new long[1];
        }
        return array;
    }

    protected void setOptions(long[] optionValues) {
        this.options = BitSet.valueOf(optionValues);
    }

    public boolean isWizardDisabled() {
        return getOption(Option.Users_Disable_wizard);
    }

    public boolean isWizardDisabledOrNull() {
        return getOption(Option.Users_Disable_wizard) || StringUtils.isBlank(wizardParam);
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }

    public Organisation getOrganisation() {
        if (organisationId == 0) {
            return null;
        }

        return Learnweb.dao().getOrganisationDao().findByIdOrElseThrow(organisationId);
    }

    public int getDefaultGroupId() {
        return defaultGroupId;
    }

    public void setDefaultGroupId(int defaultGroupId) {
        this.defaultGroupId = defaultGroupId;
    }

    public String getWizardParam() {
        return wizardParam;
    }

    public void setWizardParam(String wizardParam) {
        this.wizardParam = wizardParam;
    }

    public synchronized int getNextXUsersBecomeModerator() {
        return nextXUsersBecomeModerator;
    }

    public synchronized void setNextXUsersBecomeModerator(int nextXUsersBecomeModerator) {
        this.nextXUsersBecomeModerator = nextXUsersBecomeModerator;
    }

    public int getMemberCount() {
        if (memberCount == -1) {
            memberCount = Learnweb.dao().getUserDao().countByCourseId(id);
        }
        return memberCount;
    }

    public List<User> getMembers() {
        return Learnweb.dao().getUserDao().findByCourseId(id);
    }

    /**
     * @return The userIds of all course members
     */
    public List<Integer> getUserIds() {
        return HasId.collectIds(getMembers());
    }

    public synchronized void addUser(User user) {
        if (memberCount != -1) {
            memberCount++;
        }

        if (nextXUsersBecomeModerator > 0) {
            log.debug("User: {} becomes moderator of course: {}", user.getUsername(), getTitle());

            user.setModerator(true);

            nextXUsersBecomeModerator--;
            Learnweb.dao().getCourseDao().save(this);
        }

        Learnweb.dao().getCourseDao().insertUser(this, user);
        user.clearCaches();
    }

    public void removeUser(User user) {
        if (memberCount != -1) {
            memberCount--;
        }
        Learnweb.dao().getCourseDao().deleteUser(this, user);
        user.clearCaches();
    }

    /**
     * True if the given user is a moderator if this course.
     *
     * @return True if the given user is a moderator of this course
     */
    public boolean isModerator(User user) {
        if (user.isAdmin()) {
            return true;
        }

        if (!user.isModerator()) {
            return false;
        }

        return isMember(user); // moderators can only moderator her own courses
    }

    public boolean isMember(User user) {
        return user.getCourses().contains(this); // moderators can only moderator her own courses
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int compareTo(Course o) {
        return getTitle().compareTo(o.getTitle());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Course course = (Course) o;
        return id == course.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Course [id=" + id + ", title=" + title + "]";
    }
}

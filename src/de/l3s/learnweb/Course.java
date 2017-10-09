package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.Size;

import org.apache.log4j.Logger;

public class Course implements Serializable, Comparable<Course>
{
    private static final long serialVersionUID = -1101352995500154406L;
    private static final Logger log = Logger.getLogger(Course.class);

    // add new options add the end , don't delete options !!!!!
    // if you add 64 options you have to add one options_field{x} column in lw_course 
    public static enum Option implements Comparable<Option>
    {
        Resources_Enable_Star_rating,
        Resources_Enable_Thumb_rating,
        Users_Hide_language_switch,
        Services_Allow_user_to_logout_from_interweb,
        Users_Require_mail_address,
        Search_History_log_enabled, // this should be a organization option
        Course_Google_Docs_Sign_In_enabled, // this should be a organization option
        Users_Require_Affiliation,
        Users_Require_Student_Id
    }

    private int id;
    @Size(min = 1, max = 50)
    private String title;
    private int organisationId;
    private int defaultGroupId; // all users who join this course, automatically join this group
    @Size(min = 1, max = 100)
    private String wizardParam;
    private boolean wizardEnabled;
    private int nextXUsersBecomeModerator;
    @Size(max = 255)
    private String defaultInterwebUsername;
    @Size(max = 255)
    private String defaultInterwebPassword;
    private String welcomeMessage;
    private String bannerImage;
    private int bannerImageFileId;
    private String bannerColor;

    private long[] options = new long[CourseManager.FIELDS];

    // derived values:
    private int memberCount;
    private List<GroupCategory> groupCategories;

    /**
     * Constructs a temporary object. Can be persisted by CourseManager.save()
     */
    public Course()
    {
        this.id = -1;
        this.options[0] = 1L; // forum enabled
        this.wizardEnabled = true;
    }

    protected Course(ResultSet rs) throws SQLException
    {
        this.id = rs.getInt("course_id");
        this.title = rs.getString("title");
        this.organisationId = rs.getInt("organisation_id");
        this.defaultGroupId = rs.getInt("default_group_id");
        this.wizardParam = rs.getString("wizard_param");
        this.wizardEnabled = rs.getInt("wizard_enabled") == 1;
        this.nextXUsersBecomeModerator = rs.getInt("next_x_users_become_moderator");
        this.defaultInterwebUsername = rs.getString("default_interweb_username");
        this.defaultInterwebPassword = rs.getString("default_interweb_password");
        this.welcomeMessage = rs.getString("welcome_message");
        this.bannerColor = rs.getString("banner_color");
        this.bannerImageFileId = rs.getInt("banner_image_file_id");
        this.memberCount = -1;

        for(int i = 0; i < CourseManager.FIELDS;)
            options[i] = rs.getInt("options_field" + (++i));
    }

    public boolean getOption(Option option)
    {
        int bit = option.ordinal();
        int field = bit >> 6;
        long bitMask = 1L << (bit % 64);

        return (options[field] & bitMask) == bitMask;
    }

    public void setOption(Option option, boolean value)
    {
        int bit = option.ordinal();
        int field = bit >> 6;
        long bitMask = 1L << (bit % 64);

        if(value) // is true set Bit to 1
        {
            options[field] |= bitMask;
        }
        else
        {
            options[field] &= ~bitMask;
        }
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

    public long[] getOptions()
    {
        return options;
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

    public boolean isWizardEnabled()
    {
        return wizardEnabled;
    }

    public void setWizardEnabled(boolean wizardEnabled)
    {
        this.wizardEnabled = wizardEnabled;
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

    public void addUser(User user) throws SQLException
    {
        if(memberCount != -1)
            memberCount++;
        Learnweb.getInstance().getCourseManager().addUser(this, user);
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

    public String getDefaultInterwebUsername()
    {
        return defaultInterwebUsername;
    }

    public void setDefaultInterwebUsername(String defaultInterwebUsername)
    {
        this.defaultInterwebUsername = defaultInterwebUsername;
    }

    public String getDefaultInterwebPassword()
    {
        return defaultInterwebPassword;
    }

    public void setDefaultInterwebPassword(String defaultInterwebPassword)
    {
        this.defaultInterwebPassword = defaultInterwebPassword;
    }

    public String getWelcomeMessage()
    {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage)
    {
        this.welcomeMessage = welcomeMessage;
    }

    public String getBannerImage() throws SQLException
    {
        if(null == bannerImage)
        {
            if(bannerImageFileId < 1)
                return null;

            File file = Learnweb.getInstance().getFileManager().getFileById(bannerImageFileId);
            bannerImage = file.getUrl();
        }
        return bannerImage;
    }

    public int getBannerImageFileId()
    {
        return bannerImageFileId;
    }

    public void setBannerImageFileId(int bannerImageFileId)
    {
        this.bannerImageFileId = bannerImageFileId;
        this.bannerImage = null; // clear cache
    }

    public String getBannerColor()
    {
        return bannerColor;
    }

    public void setBannerColor(String bannerColor)
    {
        this.bannerColor = bannerColor;
    }

    @Override
    public String toString()
    {
        return "Course [id=" + id + ", title=" + title + ", options=" + Arrays.toString(options) + ", organisationId=" + organisationId + ", defaultGroupId=" + defaultGroupId + ", wizardParam=" + wizardParam + ", wizardEnabled=" + wizardEnabled + ", nextXUsersBecomeModerator="
                + nextXUsersBecomeModerator + ", defaultInterwebUsername=" + defaultInterwebUsername + ", defaultInterwebPassword=" + defaultInterwebPassword + ", memberCount=" + memberCount + "]";
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
}

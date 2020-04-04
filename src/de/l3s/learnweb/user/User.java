package de.l3s.learnweb.user;

import static org.apache.http.HttpHeaders.USER_AGENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ColorUtils;
import de.l3s.learnweb.forum.ForumPost;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Tag;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.util.HasId;
import de.l3s.util.Image;
import de.l3s.util.MD5;
import de.l3s.util.PBKDF2;
import de.l3s.util.StringHelper;
import de.l3s.util.email.Mail;

public class User implements Comparable<User>, Serializable, HasId
{
    private static final Logger log = Logger.getLogger(User.class);
    private static final long serialVersionUID = 2482790243930271009L;

    public enum PasswordHashing
    {
        EMPTY,
        MD5,
        PBKDF2
    }

    public enum Gender
    {
        UNASSIGNED,
        MALE,
        FEMALE,
        OTHER
    }

    private int id = -1;
    private boolean deleted;
    private int imageFileId; // profile image
    private int organisationId;
    @Length(max = 100)
    private String fullName;
    @Length(max = 100)
    private String affiliation; //affiliated with which institute
    @NotBlank
    @Length(min = 2, max = 50)
    private String username;
    @Email
    private String email = null; // it is important to set null instead of empty string
    private String emailConfirmationToken;
    private boolean emailConfirmed = true;
    private String password;
    private PasswordHashing hashing;

    private Gender gender = Gender.UNASSIGNED;
    private Date dateOfBirth;
    @Length(max = 250)
    private String address;
    @Length(max = 100)
    private String profession;
    @Length(max = 250)
    private String additionalInformation;
    @Length(max = 250)
    private String interest;
    @Length(max = 50)
    private String studentId;
    private Date registrationDate;
    @Length(max = 250)
    private String credits;
    private boolean acceptTermsAndConditions = false;

    private boolean admin;
    private boolean moderator;

    private HashMap<String, String> preferences;
    private TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");

    // caches
    private transient List<Course> courses;
    private transient List<Group> groups;
    private String imageUrl;
    private transient Instant lastLoginDate = null;
    private int forumPostCount = -1;
    private transient Organisation organisation;

    public User()
    {
    }

    public void clearCaches()
    {
        courses = null;
        groups = null;
        organisation = null;
        imageUrl = null;
        forumPostCount = -1;
    }

    public void onDestroy()
    {
        try
        {
            this.save();
        }
        catch(SQLException e)
        {
            log.error("Couldn't save user onDestroy", e);
        }
    }

    public void save() throws SQLException
    {
        Learnweb.getInstance().getUserManager().save(this);
    }

    public List<Course> getCourses() throws SQLException
    {
        if(courses == null)
        {
            courses = Learnweb.getInstance().getCourseManager().getCoursesByUserId(id);
            if(!courses.isEmpty())
                Collections.sort(courses);
        }

        return courses;
    }

    public boolean isEmailRequired() throws SQLException
    {
        for(Course course : getCourses())
        {
            if(course.getOption(Course.Option.Users_Require_mail_address))
                return true;
        }

        return false;
    }

    public boolean isMemberOfCourse(int courseId) throws SQLException
    {
        for(Course course : getCourses())
        {
            if(courseId == course.getId())
                return true;
        }

        return false;
    }

    public String getAdditionalInformation()
    {
        return additionalInformation;
    }

    public String getAddress()
    {
        return address;
    }

    public List<Comment> getComments() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getCommentsByUserId(this.getId());
    }

    public Date getDateOfBirth()
    {
        return dateOfBirth;
    }

    public String getEmail()
    {
        return email;
    }

    public String getEmailConfirmationToken()
    {
        return emailConfirmationToken;
    }

    public boolean isEmailConfirmed()
    {
        return emailConfirmed;
    }

    @Override
    public int getId()
    {
        return id;
    }

    public String getInterest()
    {
        return interest;
    }

    public String getStudentId()
    {
        return studentId;
    }

    public String getProfession()
    {
        return profession;
    }

    public Resource addResource(Resource resource) throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().addResource(resource, this);
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByUserId(this.getId());
    }

    public int getResourceCount() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourceCountByUserId(this.getId());
    }

    public List<Resource> getRatedResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getRatedResourcesByUserId(this.getId());
    }

    public List<Tag> getTags() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getTagsByUserId(this.getId());
    }

    public String getUsername()
    {
        if(getOrganisation().getOption(Option.Privacy_Anonymize_usernames))
            return "Anonymous";

        return username;
    }

    /**
     * getUsername() may return "Anonymous" for some organizations.
     * This method will always return the real username
     */
    public String getRealUsername()
    {
        return username;
    }

    public void setAdditionalInformation(String additionalInformation)
    {
        this.additionalInformation = additionalInformation;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public void setDateOfBirth(Date dateOfBirth)
    {
        this.dateOfBirth = dateOfBirth;
    }

    public void setEmail(String email)
    {
        if(StringUtils.isNotBlank(email) && !StringUtils.equalsIgnoreCase(email, this.email))
        {
            this.email = email;
            this.emailConfirmed = false;
            this.emailConfirmationToken = MD5.hash(RandomStringUtils.randomAlphanumeric(26) + this.id + email);
        }
        else
        {
            this.email = StringUtils.isNotBlank(email) ? email : null;
        }
    }

    /**
     * This method should be used when we read the email from database, but never for user input fields.
     */
    public void setEmailRaw(String email)
    {
        this.email = email;
    }

    public void setEmailConfirmationToken(String emailConfirmationToken)
    {
        this.emailConfirmationToken = emailConfirmationToken;
    }

    public void setEmailConfirmed(boolean isEmailConfirmed)
    {
        this.emailConfirmed = isEmailConfirmed;
    }

    public void setInterest(String interest)
    {
        this.interest = interest;
    }

    public void setStudentId(String studentId)
    {
        this.studentId = studentId;
    }

    public void setProfession(String profession)
    {
        this.profession = profession;
    }

    public void setUsername(String username)
    {
        this.username = StringUtils.trim(username);
    }

    public void setRealUsername(String username)
    {
        setUsername(username);
    }

    public void setOrganisationId(int organisationId)
    {
        this.organisationId = organisationId;
        this.organisation = null;
    }

    public int getOrganisationId()
    {
        return organisationId;
    }

    public Organisation getOrganisation()
    {
        if(organisation == null)
            organisation = Learnweb.getInstance().getOrganisationManager().getOrganisationById(organisationId);
        return organisation;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(null == obj)
            return false;

        if(obj.getClass() != this.getClass())
            return false;

        return ((User) obj).getId() == this.getId();
    }

    private long groupsCacheTime = 0L;

    /**
     * Returns the groups the user is member off.
     */
    public List<Group> getGroups() throws SQLException
    {
        if(null == groups || groupsCacheTime + 3000L < System.currentTimeMillis())
        {
            groups = Learnweb.getInstance().getGroupManager().getGroupsByUserId(id);
            groupsCacheTime = System.currentTimeMillis();
        }
        return groups;
    }

    /**
     * @return number of groups this user is member of
     */
    public int getGroupCount() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getGroupCountByUserId(id);
    }

    /**
     * Returns the groups the user can add resources to.
     */
    public List<Group> getWriteAbleGroups() throws SQLException
    {
        LinkedList<Group> writeAbleGroups = new LinkedList<>();
        for(Group group : getGroups())
        {
            if(group.canAddResources(this))
                writeAbleGroups.add(group);
        }
        return writeAbleGroups;
    }

    public void joinGroup(int groupId) throws SQLException
    {
        joinGroup(Learnweb.getInstance().getGroupManager().getGroupById(groupId));
    }

    public void joinGroup(Group group) throws SQLException
    {
        Learnweb.getInstance().getGroupManager().addUserToGroup(this, group);

        groups = null; // force reload

        group.clearCaches();
    }

    public void leaveGroup(Group group) throws SQLException
    {
        Learnweb.getInstance().getGroupManager().removeUserFromGroup(this, group);

        groups = null; // force reload

        group.clearCaches();
    }

    @Override
    public int compareTo(User o)
    {
        return getUsername().compareTo(o.getUsername());
    }

    public void setImage(InputStream inputStream) throws SQLException, IOException
    {
        // process image
        Image img = new Image(inputStream);

        // save image file
        File file = new File();
        file.setType(TYPE.PROFILE_PICTURE);
        file.setName("user_icon.png");
        file.setMimeType("image/png");

        Image thumbnail = img.getResizedToSquare2(200, 0.0);
        file = Learnweb.getInstance().getFileManager().save(file, thumbnail.getInputStream());
        thumbnail.dispose();
        inputStream.close();

        setImageFileId(file.getId());
        imageUrl = file.getUrl();
    }

    /**
     *
     * @return FALSE if an error occurred while sending this message
     */
    public boolean sendEmailConfirmation()
    {
        try
        {
            String confirmEmailUrl = Learnweb.getInstance().getServerUrl() + "/lw/user/confirm_email.jsf?" +
                    "email=" + StringHelper.urlEncode(getEmail()) +
                    "&token=" + getEmailConfirmationToken();

            Mail message = new Mail();
            message.setSubject("Confirmation request from Learnweb");
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(getEmail()));
            message.setText("Hi " + getRealUsername() + ",\n\n" +
                    "please use this link to confirm your mail address:\n" + confirmEmailUrl + "\n\n" +
                    "Or just ignore this email, if you haven't requested it.\n\n" +
                    "Best regards,\nLearnweb Team");

            message.sendMail();

            return true;
        }
        catch(MessagingException e)
        {
            log.error("Can't send confirmation mail to " + toString(), e);
        }
        return false;
    }

    public boolean isAdmin()
    {
        return admin;
    }

    public void setAdmin(boolean admin)
    {
        this.admin = admin;
    }

    public boolean isModerator()
    {
        return moderator || admin;
    }

    public void setModerator(boolean moderator)
    {
        this.moderator = moderator;
    }

    /**
     * @return the url of the users image or a default image if no image has been added
     */
    public String getImage() throws SQLException
    {
        if(imageUrl == null)
        {
            File imageFile = getImageFile();
            imageUrl = imageFile != null ? imageFile.getUrl() : getDefaultImage();
        }

        return imageUrl;
    }

    /**
     * return the File of the profile picture
     *
     * @return Null if not present
     * @throws SQLException
     */
    public File getImageFile() throws SQLException
    {
        if(imageFileId > 0)
            return Learnweb.getInstance().getFileManager().getFileById(imageFileId);
        return null;
    }

    /**
     * get default avatar for user
     */
    private String getDefaultImage()
    {
        String name = StringUtils.isNotBlank(fullName) ? fullName : username;
        String initials = "";

        if(StringUtils.isNumeric(name)) // happens when users use their student id as name
        {
            initials = name.substring(name.length() - 2);
        }
        else if(name.contains(" ") || name.contains(".")) // name consists of multiple terms separated by whitespaces or dots
        {
            for(String part : name.split("[\\s\\.]+"))
            {
                if(part.length() == 0)
                    continue;
                int index = StringUtils.isNumeric(part) ? (part.length() - 1) : 0; // if is number use last digit as initial
                initials += part.charAt(index);
            }
        }
        else if(!name.equals(name.toLowerCase()))
        {
            initials += name.charAt(0); // always at first char

            for(int i = 1; i < name.length() - 1; i++)
            {
                if(Character.isUpperCase(name.charAt(i)))
                {
                    initials += name.charAt(i);
                }
            }
        }

        if(StringUtils.isBlank(initials))
            initials = name.substring(0, 1);

        if(initials.startsWith(".")) // ui-avatars can't handle dots in the beginning
            initials = initials.replace(".", "X");

        String defaultAvatarUrl = "https://ui-avatars.com/api/" + initials + "/200/" + getDefaultColor() + "/ffffff";

        if(email != null)
            return "https://www.gravatar.com/avatar/" + MD5.hash(email) + "?d=" + StringHelper.urlEncode(defaultAvatarUrl);
        else
            return defaultAvatarUrl;
    }

    /**
     * Returns a color for the user
     * If the user is registered this method will return always the same color otherwise a "random" color is returned
     *
     * @return color code without hash mark
     */
    private String getDefaultColor()
    {
        return ColorUtils.getColor(HasId.getIdOrDefault(this, (int) (System.currentTimeMillis() / 1000))).substring(1);
    }

    /**
     * get default avatar for user
     */
    public InputStream getDefaultImageIS() throws IOException
    {
        URL url = new URL(getDefaultImage());

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK)
        {
            return con.getInputStream();
        }
        else
        {
            return null;
        }
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getImageFileId()
    {
        return imageFileId;
    }

    public void setImageFileId(int imageFileId)
    {
        if(this.imageFileId != 0 && imageFileId != this.imageFileId) // delete existing image
        {
            try
            {
                Learnweb.getInstance().getFileManager().delete(this.imageFileId);
            }
            catch(Exception e)
            {
                log.error("Can't delete profile image of user " + toString());
            }
        }
        this.imageFileId = imageFileId;
    }

    public Date getRegistrationDate()
    {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate)
    {
        this.registrationDate = registrationDate;
    }

    public HashMap<String, String> getPreferences()
    {
        return preferences;
    }

    public void setPreferences(HashMap<String, String> preferences)
    {
        this.preferences = preferences;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPasswordRaw(String password)
    {
        this.password = password;
    }

    public void setPassword(String password)
    {
        if(password != null)
        {
            this.password = PBKDF2.hashPassword(password);
            this.hashing = PasswordHashing.PBKDF2;
        }
        else
        {
            this.hashing = PasswordHashing.EMPTY;
        }
    }

    public boolean validatePassword(String password)
    {
        if(hashing.equals(PasswordHashing.MD5))
        {
            return this.password.equals(MD5.hash(password));
        }
        else if(hashing.equals(PasswordHashing.PBKDF2))
        {
            return PBKDF2.validatePassword(password, this.password);
        }

        return false;
    }

    public PasswordHashing getHashing()
    {
        return hashing;
    }

    public void setHashing(final String hashing)
    {
        this.hashing = PasswordHashing.valueOf(hashing);
    }

    public TimeZone getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    /**
     * @return The dateTime of the last recorded login event of the user. Returns the registration date if the user has never logged in.
     *         Note that this value is cached. Call {@link #updateLoginDate() updateLoginDate} to update it. This is useful to control whether the
     *         current or the penultimate login time is returned.
     * @throws SQLException
     */
    public Instant getLastLoginDate() throws SQLException
    {
        if(null == lastLoginDate)
        {
            updateLoginDate();
        }
        return lastLoginDate;
    }

    public void updateLoginDate() throws SQLException
    {
        this.lastLoginDate = Learnweb.getInstance().getUserManager().getLastLoginDate(getId()).orElse(registrationDate.toInstant());
    }

    @Override
    public String toString()
    {
        return "[userId: " + getId() + " name: " + getRealUsername() + "]";
    }

    public int getForumPostCount() throws SQLException
    {
        if(forumPostCount == -1)
        {
            forumPostCount = Learnweb.getInstance().getForumManager().getPostCountByUser(id);
        }
        return forumPostCount;
    }

    public void incForumPostCount()
    {
        if(forumPostCount != -1)
            forumPostCount++;
    }

    public String getCredits()
    {
        return credits;
    }

    public void setCredits(String credits)
    {
        if(credits != null)
        {
            if(credits.startsWith("<br>"))
                credits = credits.substring(4);

            credits = Jsoup.clean(credits, Whitelist.basic());
        }
        this.credits = credits;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getAffiliation()
    {
        return affiliation;
    }

    public void setAffiliation(String affiliation)
    {
        this.affiliation = affiliation;
    }

    public String getPreference(String key)
    {
        return preferences.get(key);
    }

    public void setPreference(String key, String value)
    {
        preferences.put(key, value);
    }

    public boolean isAcceptTermsAndConditions()
    {
        return acceptTermsAndConditions;
    }

    public void setAcceptTermsAndConditions(boolean acceptTermsAndConditions)
    {
        this.acceptTermsAndConditions = acceptTermsAndConditions;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    public Gender getGender()
    {
        return gender;
    }

    public void setGender(Gender gender)
    {
        this.gender = gender;
    }

    /**
     *
     * @return All forum posts this user created
     * @throws SQLException
     */
    public List<ForumPost> getForumPosts() throws SQLException
    {
        return Learnweb.getInstance().getForumManager().getPostsByUser(getId());
    }

    /**
     * returns true when this user is allowed to moderate the given user
     *
     * @param user
     * @return
     * @throws SQLException
     */
    public boolean canModerateUser(User user) throws SQLException
    {
        if(isAdmin())
            return true;

        if(isModerator() && user.getOrganisation().equals(this.getOrganisation())) // check whether the user is moderator of the given users organisation
            return true;

        return false;
    }
}

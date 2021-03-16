package de.l3s.learnweb.user;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.forum.ForumPost;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Tag;
import de.l3s.learnweb.resource.submission.Submission;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.util.Deletable;
import de.l3s.util.HasId;
import de.l3s.util.HashHelper;
import de.l3s.util.PBKDF2;
import de.l3s.util.ProfileImageHelper;
import de.l3s.util.StringHelper;
import de.l3s.util.email.Mail;

public class User implements Comparable<User>, Deletable, HasId, Serializable {
    private static final Logger log = LogManager.getLogger(User.class);
    private static final long serialVersionUID = 2482790243930271009L;

    public enum PasswordHashing {
        EMPTY,
        MD5,
        PBKDF2
    }

    public enum Gender {
        UNASSIGNED,
        MALE,
        FEMALE,
        OTHER
    }

    public enum Guide {
        HIDE,
        ADD_RESOURCE,
        JOIN_GROUP,
        ADD_PHOTO,
    }

    public enum NotificationFrequency {
        NEVER(-1),
        DAILY(1),
        WEEKLY(7),
        MONTHLY(30);

        private final int days; // how many days this enum represents

        NotificationFrequency(int days) {
            this.days = days;
        }

        public int getDays() {
            return days;
        }
    }

    private int id;
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
    private String email; // it is important to set null instead of empty string
    private boolean emailConfirmed = true;
    private String password;
    private PasswordHashing hashing;
    private NotificationFrequency preferredNotificationFrequency = NotificationFrequency.NEVER; // how often will users get updates by mail
    private Locale locale = Locale.forLanguageTag("en-US"); // preferred interface language

    private Gender gender = Gender.UNASSIGNED;
    private LocalDate dateOfBirth;
    @Length(max = 250)
    private String address;
    @Length(max = 100)
    private String profession;
    @Length(max = 250)
    private String interest;
    @Length(max = 50)
    private String studentId;
    @Length(max = 250)
    private String credits;
    private boolean acceptTermsAndConditions = false;

    private boolean admin;
    private boolean moderator;

    private HashMap<String, String> preferences = new HashMap<>();
    private ZoneId timeZone;
    private LocalDateTime createdAt;

    // caches
    private transient List<Course> courses;
    private long groupsCacheTime = 0L;
    private transient List<Group> groups;
    private String imageUrl;
    private transient LocalDateTime lastLoginDate;
    private int forumPostCount = -1;
    private transient Organisation organisation;
    private transient List<Submission> activeSubmissions;
    private BitSet guides = new BitSet(Guide.values().length);

    public void clearCaches() {
        courses = null;
        groups = null;
        organisation = null;
        imageUrl = null;
        forumPostCount = -1;
        activeSubmissions = null;
    }

    public List<Course> getCourses() {
        if (courses == null) {
            courses = Learnweb.dao().getCourseDao().findByUserId(id);
            if (!courses.isEmpty()) {
                Collections.sort(courses);
            }
        }

        return courses;
    }

    /**
     * @return number of courses this user is member of
     */
    public int getCoursesCount() {
        return getCourses().size();
    }

    public boolean isEmailRequired() {
        for (Course course : getCourses()) {
            if (course.getOption(Course.Option.Users_Require_mail_address)) {
                return true;
            }
        }

        return false;
    }

    public boolean isMemberOfCourse(int courseId) {
        for (Course course : getCourses()) {
            if (courseId == course.getId()) {
                return true;
            }
        }

        return false;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Comment> getComments() {
        return Learnweb.dao().getCommentDao().findByUserId(id);
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (StringUtils.isNotBlank(email) && !StringUtils.equalsIgnoreCase(email, this.email)) {
            this.email = email;
            this.emailConfirmed = false;

            Learnweb.dao().getTokenDao().override(id, Token.TokenType.EMAIL_CONFIRMATION, RandomStringUtils.randomAlphanumeric(32), LocalDateTime.now().plusYears(1));
        } else {
            this.email = StringUtils.isNotBlank(email) ? email : null;
        }
    }

    public boolean isEmailConfirmed() {
        return emailConfirmed;
    }

    public void setEmailConfirmed(boolean isEmailConfirmed) {
        this.emailConfirmed = isEmailConfirmed;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public List<Resource> getResources() {
        return Learnweb.dao().getResourceDao().findByOwnerId(id);
    }

    public int getResourceCount() {
        return Learnweb.dao().getResourceDao().countByOwnerId(id);
    }

    public List<Resource> getRatedResources() {
        return Learnweb.dao().getResourceDao().findRatedByUsedId(id);
    }

    public List<Tag> getTags() {
        return Learnweb.dao().getTagDao().findByUserId(id);
    }

    public String getUsername() {
        if (getOrganisation().getOption(Option.Privacy_Anonymize_usernames)) {
            return "Anonymous";
        }

        return username;
    }

    public void setUsername(String username) {
        this.username = StringUtils.trim(username);
    }

    /**
     * TODO: getUsername can be renamed to getDisplayName and getRealUsername to getUsername
     * getUsername() may return "Anonymous" for some organisation.
     * This method will always return the real username
     */
    public String getRealUsername() {
        return username;
    }

    public void setRealUsername(String username) {
        setUsername(username);
    }

    /**
     * This method should be used when we read the email from database, but never for user input fields.
     */
    public void setEmailRaw(String email) {
        this.email = email;
    }

    public NotificationFrequency getPreferredNotificationFrequency() {
        return preferredNotificationFrequency;
    }

    public void setPreferredNotificationFrequency(NotificationFrequency preferredNotificationFrequency) {
        this.preferredNotificationFrequency = preferredNotificationFrequency;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
        this.organisation = null;
    }

    public Organisation getOrganisation() {
        if (organisation == null) {
            organisation = Learnweb.dao().getOrganisationDao().findByIdOrElseThrow(organisationId);
        }
        return organisation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns the groups the user is member off.
     */
    public List<Group> getGroups() {
        if (null == groups || groupsCacheTime + 3000L < System.currentTimeMillis()) {
            groups = Learnweb.dao().getGroupDao().findByUserId(id);
            groupsCacheTime = System.currentTimeMillis();
        }
        return groups;
    }

    /**
     * @return number of groups this user is member of
     */
    public int getGroupCount() {
        return getGroups().size();
    }

    /**
     * Returns the groups the user can add resources to.
     */
    public List<Group> getWriteAbleGroups() {
        LinkedList<Group> writeAbleGroups = new LinkedList<>();
        for (Group group : getGroups()) {
            if (group.canAddResources(this)) {
                writeAbleGroups.add(group);
            }
        }
        return writeAbleGroups;
    }

    public void joinGroup(Group group) {
        Learnweb.dao().getGroupDao().insertUser(group.getId(), this, getPreferredNotificationFrequency());
        group.clearCaches(); // force reload members
        groups = null; // force reload
    }

    public void leaveGroup(Group group) {
        Learnweb.dao().getGroupDao().deleteUser(group.getId(), this);
        groups = null; // force reload
        group.clearCaches();
    }

    @Override
    public int compareTo(User o) {
        return getUsername().compareTo(o.getUsername());
    }

    /**
     * @return FALSE if an error occurred while sending this message
     */
    public boolean sendEmailConfirmation() {
        try {
            Token token = Learnweb.dao().getTokenDao().findByTypeAndUser(Token.TokenType.EMAIL_CONFIRMATION, id).orElseThrow();

            String confirmEmailUrl = Learnweb.config().getServerUrl() + "/lw/user/confirm_email.jsf?" +
                "email=" + StringHelper.urlEncode(getEmail()) +
                "&token=" + token.getId() + ":" + token.getToken();

            Mail message = new Mail();
            message.setSubject("Confirmation request from Learnweb");
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(getEmail()));
            message.setText("Hi " + getRealUsername() + ",\n\n" +
                "please use this link to confirm your mail address:\n" + confirmEmailUrl + "\n\n" +
                "Or just ignore this email, if you haven't requested it.\n\n" +
                "Best regards,\nLearnweb Team");

            message.sendMail();

            return true;
        } catch (MessagingException e) {
            log.error("Can't send confirmation mail to {}", this, e);
        }
        return false;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isModerator() {
        return moderator || admin;
    }

    public void setModerator(boolean moderator) {
        this.moderator = moderator;
    }

    /**
     * @return the url of the users image or a default image if no image has been added
     */
    public String getImageUrl() {
        if (imageUrl == null) {
            imageUrl = imageFileId != 0 ? getImageFile().getUrl() : ProfileImageHelper.getProfilePicture(StringUtils.firstNonBlank(fullName, username));
        }
        return imageUrl;
    }

    /**
     * return the File of the profile picture.
     *
     * @return Null if not present
     */
    public File getImageFile() {
        if (imageFileId != 0) {
            return Learnweb.dao().getFileDao().findByIdOrElseThrow(imageFileId);
        }
        return null;
    }

    public int getImageFileId() {
        return imageFileId;
    }

    public void setImageFileId(int imageFileId) {
        this.imageFileId = imageFileId;
        this.imageUrl = null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public HashMap<String, String> getPreferences() {
        return preferences;
    }

    public void setPreferences(HashMap<String, String> preferences) {
        this.preferences = preferences;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password != null) {
            this.password = PBKDF2.hashPassword(password);
            this.hashing = PasswordHashing.PBKDF2;
        } else {
            this.hashing = PasswordHashing.EMPTY;
        }
    }

    public void setPasswordRaw(String password) {
        this.password = password;
    }

    public boolean validatePassword(String password) {
        if (hashing == PasswordHashing.MD5) {
            return this.password.equals(HashHelper.md5(password));
        } else if (hashing == PasswordHashing.PBKDF2) {
            return PBKDF2.validatePassword(password, this.password);
        }

        return false;
    }

    public PasswordHashing getHashing() {
        return hashing;
    }

    public void setHashing(final String hashing) {
        this.hashing = PasswordHashing.valueOf(hashing);
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        Validate.notNull(timeZone);
        this.timeZone = timeZone;
    }

    /**
     * @return The dateTime of the last recorded login event of the user. Returns the registration date if the user has never logged in.
     * Note that this value is cached. Call {@link #updateLoginDate() updateLoginDate} to update it. This is useful to control whether the
     * current or the penultimate login time is returned.
     */
    public LocalDateTime getLastLoginDate() {
        if (null == lastLoginDate) {
            updateLoginDate();
        }
        return lastLoginDate;
    }

    public void updateLoginDate() {
        this.lastLoginDate = Learnweb.dao().getUserDao().findLastLoginDate(getId()).orElse(createdAt);
    }

    @Override
    public String toString() {
        return "[userId: " + getId() + ", name: " + getRealUsername() + ", email: " + getEmail() + "]";
    }

    public int getForumPostCount() {
        if (forumPostCount == -1) {
            forumPostCount = Learnweb.dao().getForumPostDao().countByUserId(id);
        }
        return forumPostCount;
    }

    public void incForumPostCount() {
        if (forumPostCount != -1) {
            forumPostCount++;
        }
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String credits) {
        if (credits != null) {
            if (credits.startsWith("<br>")) {
                credits = credits.substring(4);
            }

            credits = Jsoup.clean(credits, Whitelist.basic());
        }
        this.credits = credits;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getPreference(String key) {
        return preferences.get(key);
    }

    public void setPreference(String key, String value) {
        preferences.put(key, value);
    }

    public boolean isAcceptTermsAndConditions() {
        return acceptTermsAndConditions;
    }

    public void setAcceptTermsAndConditions(boolean acceptTermsAndConditions) {
        this.acceptTermsAndConditions = acceptTermsAndConditions;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    /**
     * @return All forum posts this user created
     */
    public List<ForumPost> getForumPosts() {
        return Learnweb.dao().getForumPostDao().findByUserId(id);
    }

    /**
     * returns true when this user is allowed to moderate the given user.
     */
    public boolean canModerateUser(User userToBeModerated) {
        if (isAdmin()) {
            return true;
        }

        // check whether the user is moderator of the given users organisation
        if (isModerator() && getOrganisation().equals(userToBeModerated.getOrganisation())) {
            return true;
        }

        return false;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Retrieves current submissions for a user to be displayed in the homepage.
     */
    public List<Submission> getActiveSubmissions() {
        if (null == activeSubmissions) {
            activeSubmissions = Learnweb.dao().getSubmissionDao().findActiveByCourseIds(HasId.collectIds(getCourses()));
        }
        return activeSubmissions;
    }

    public boolean getGuide(Guide guide) {
        boolean value = guides.get(guide.ordinal());
        if (!value) {
            value = getComputedGuide(guide);
            setGuide(guide, value);
        }
        return value;
    }

    private boolean getComputedGuide(Guide guide) {
        switch (guide) {
            case ADD_RESOURCE:
                return getResourceCount() > 0;
            case JOIN_GROUP:
                return getGroupCount() > 0;
            case ADD_PHOTO:
                return getImageFileId() != 0;
            default:
                return false;
        }
    }

    public void setGuide(Guide option, boolean value) {
        guides.set(option.ordinal(), value);
    }

    protected BitSet getGuides() {
        return guides;
    }

    protected void setGuides(BitSet guides) {
        this.guides = guides;
    }

}

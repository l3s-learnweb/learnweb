package de.l3s.learnweb;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.validation.constraints.Size;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.interwebj.AuthCredentials;
import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.Organisation.Option;
import de.l3s.util.HasId;
import de.l3s.util.Image;
import de.l3s.util.MD5;

public class User implements Comparable<User>, Serializable, HasId
{
    private static final Logger log = Logger.getLogger(User.class);
    private static final long serialVersionUID = 2482790243930271009L;

    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    private int id = -1;
    private InterWeb interweb;
    private int imageFileId; // profile image
    private int organisationId;
    private String fullName; //Full Name
    private String affiliation; //affiliated with which institute
    private String username;
    private String email;
    private String password; // md5 hash

    private int gender;
    private Date dateofbirth;
    private String address;
    private String profession;
    private String additionalInformation;
    private String interest;
    private String studentId;
    private Date registrationDate;
    @Size(max = 255)
    private String credits;

    private boolean destroyed = false;

    private Group activeGroup;
    private int activeGroupId;
    private int activeCourseId;

    private boolean admin;
    private boolean moderator;

    private String iwKey;
    private String iwSecret;

    private HashMap<String, String> preferences;

    // caches
    private transient List<Course> courses;
    private transient List<Resource> resources;
    private transient List<Group> groups;
    private transient LinkedList<Group> writeAbleGroups;
    private String imageUrl;
    private TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
    private transient Date lastLoginDate = null;
    private transient Date currentLoginDate = null;
    private int forumPostCount = -1;
    private transient Organisation organisation;

    public User()
    {
    }

    public void clearCaches()
    {
        courses = null;
        resources = null;
        groups = null;
        writeAbleGroups = null;
    }

    public void onDestroy()
    {
        if(destroyed)
            return;
        destroyed = true;

        try
        {
            this.save();
        }
        catch(SQLException e)
        {
            Logger.getLogger(User.class).error("Couldn't save user onDestroy", e);
        }
    }

    public void save() throws SQLException
    {
        Learnweb.getInstance().getUserManager().save(this);
    }

    public boolean isLoggedInInterweb()
    {
        return !(getInterweb().getIWToken() == null);
    }

    public List<Course> getCourses() throws SQLException
    {
        if(courses != null)
            return courses;

        courses = Learnweb.getInstance().getCourseManager().getCoursesByUserId(id);

        if(courses.size() > 1)
        {
            Collections.sort(courses);
        }

        return courses;
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

    public Date getDateofbirth()
    {
        return dateofbirth;
    }

    public String getEmail()
    {
        return email;
    }

    @Override
    public int getId()
    {
        return id;
    }

    /**
     * Returns User.GENDER_MALE, User.GENDER_FEMALE or 0 if not set
     * 
     * @return
     */
    public int getGender()
    {
        return gender;
    }

    public String getInterest()
    {
        return interest;
    }

    public InterWeb getInterweb()
    {
        if(null == interweb)
        {
            interweb = Learnweb.getInstance().getInterweb().clone();

            if(iwKey != null && iwSecret != null)
            {
                this.interweb.setIWToken(new AuthCredentials(iwKey, iwSecret));
            }
        }
        return interweb;
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
        Learnweb learnweb = Learnweb.getInstance();
        resource = learnweb.getResourceManager().addResource(resource, this);

        //learnweb.getArchiveUrlManager().addResourceToArchive(resource);

        if(null != resources)
            resources.add(resource);

        return resource;
    }

    public void deleteResource(Resource resource) throws SQLException
    {
        if(null != resources)
            resources.remove(resource);

        Learnweb.getInstance().getResourceManager().deleteResource(resource);
    }

    public List<Resource> getResources() throws SQLException
    {
        //if(null == resources)
        resources = Learnweb.getInstance().getResourceManager().getResourcesByUserId(this.getId());

        return resources;
    }

    public int getResourceCount() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourceCountByUserId(this.getId());
    }

    public List<Resource> getRatedResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getRatedResourcesByUserId(this.getId());
    }

    public List<Tag> getTags() throws Exception
    {
        return Learnweb.getInstance().getResourceManager().getTagsByUserId(this.getId());
    }

    public String getUsername()
    {
        if(getOrganisation().getId() == 1249 && getOrganisation().getOption(Option.Misc_Anonymize_usernames))
            return "Anonymous";
        //return "user " + id;
        return username;
    }

    public String getRealUsername()
    {
        return username;
    }

    public void setAdditionalinformation(String additionalinformation)
    {
        this.additionalInformation = additionalinformation;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public void setDateofbirth(Date dateofbirth)
    {
        this.dateofbirth = dateofbirth;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public void setGender(int gender)
    {
        this.gender = gender;
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
        this.username = username;
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

    private Course activeCourse;

    /**
     * returns the groups the user is member off
     * 
     * @return
     * @throws SQLException
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
     * 
     * @return number of groups this user is member of
     * @throws SQLException
     */
    public int getGroupCount() throws SQLException
    {
        return Learnweb.getInstance().getGroupManager().getGroupCountByUserId(id);
    }

    /**
     * returns the groups the user can add resources to
     * 
     * @return
     * @throws SQLException
     */
    public List<Group> getWriteAbleGroups() throws SQLException
    {
        //if(null == writeAbleGroups)
        //{
        writeAbleGroups = new LinkedList<Group>();
        List<Group> groups = getGroups();
        for(Group group : groups)
        {
            if(group.canAddResources(this))
                writeAbleGroups.add(group);
        }
        //}
        return writeAbleGroups;
    }

    public void joinGroup(int groupId) throws Exception
    {
        joinGroup(Learnweb.getInstance().getGroupManager().getGroupById(groupId));
    }

    public void joinGroup(Group group) throws Exception
    {
        Learnweb.getInstance().getGroupManager().addUserToGroup(this, group);

        groups = null; // force reload
        writeAbleGroups = null; // force reload

        group.clearCaches();

        setActiveGroup(group);
    }

    public void leaveGroup(Group group) throws Exception
    {
        Learnweb.getInstance().getGroupManager().removeUserFromGroup(this, group);

        groups = null; // force reload
        writeAbleGroups = null;

        group.clearCaches(); // removeMember(this);

        if(activeGroup == group)
            setActiveGroup(null);
    }

    public void deleteGroup(Group group) throws Exception
    {
        Learnweb.getInstance().getGroupManager().deleteGroup(group);

        if(null != groups)
            groups.remove(group);

        if(activeGroup == group)
            setActiveGroup(null);
    }

    @Override
    public int compareTo(User o)
    {
        return getUsername().compareTo(o.getUsername());
    }

    /**
     * Defines the group, the user ist currently working on
     * 
     * @param group
     * @throws SQLException
     */
    public void setActiveGroup(Group group) throws SQLException
    {
        setActiveGroup(null == group ? 0 : group.getId());

        this.activeGroup = group;
    }

    /**
     * Defines the group, the user ist currently working on
     * 
     * @param group
     * @throws SQLException
     */
    public void setActiveGroup(int groupId) throws SQLException
    {
        this.activeGroup = null; // force to reload group
        this.activeGroupId = groupId;
    }

    /**
     * Returns the group, which the user is currently working on
     * 
     * @return may be null if not yet set
     * @throws SQLException
     */
    public Group getActiveGroup() throws SQLException
    {
        if(null == activeGroup && activeGroupId != 0)
            activeGroup = Learnweb.getInstance().getGroupManager().getGroupById(activeGroupId);
        return activeGroup;
    }

    /**
     * Returns the id of the group, which the user is currently working on
     * 
     * @return may be null if not yet set
     * @throws SQLException
     */
    public int getActiveGroupId()
    {
        return activeGroupId;
    }

    public void setImage(InputStream inputStream) throws SQLException, IOException, IllegalArgumentException
    {
        FileManager fileManager = Learnweb.getInstance().getFileManager();

        if(imageFileId != 0) // delete old image
        {
            fileManager.delete(imageFileId);
        }

        // process image
        Image img = new Image(inputStream);
        Image thumbnail = img.getResizedToSquare(100, 0.05);

        // save image file
        File file = new File();
        file.setType(TYPE.PROFILE_PICTURE);
        file.setName("user_icon.png");
        file.setMimeType("image/png");
        file = fileManager.save(file, thumbnail.getInputStream());
        thumbnail.dispose();
        inputStream.close();

        imageFileId = file.getId();
        imageUrl = file.getUrl();

        this.save();
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
     * returns the url of the users image
     * or a default image if no image has been added
     * 
     * @return
     * @throws SQLException
     */
    public String getImage() throws SQLException
    {
        if(imageUrl == null)
        {
            /*
            File file = Learnweb.getInstance().getFileManager().getFileById(imageFileId);
            
            if(null == file)
            imageUrl = UtilBean.getLearnwebBean().getContextUrl() + "/resources/image/no_profile.jpg";
            else
            imageUrl = file.getUrl();
            */
            imageUrl = getImage(imageFileId);
        }

        return imageUrl;
    }

    /**
     * creates the URL for a given fileId of a profile image
     * 
     * @param fileId
     * @return
     */
    public static String getImage(int fileId)
    {
        if(fileId > 0)
            return Learnweb.getInstance().getFileManager().createUrl(fileId, "user_icon.png");

        return Learnweb.getInstance().getServerUrl() + "/resources/image/no_profile.jpg";
    }

    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * key of the interweb token
     * 
     * @return
     */
    public String getInterwebKey()
    {
        return iwKey;
    }

    /**
     * key of the interweb token
     * 
     * @param iwKey
     */
    public void setInterwebKey(String iwKey)
    {
        this.iwKey = iwKey;
        this.interweb = null; // force reload
    }

    /**
     * secret of the interweb token
     * 
     * @return
     */
    public String getInterwebSecret()
    {
        return iwSecret;
    }

    /**
     * secret of the interweb token
     * 
     * @param iwSecret
     */
    public void setInterwebSecret(String iwSecret)
    {
        this.iwSecret = iwSecret;
        this.interweb = null; // force reload
    }

    public void setInterwebToken(AuthCredentials auth)
    {
        getInterweb(); // make sure interweb is loaded

        this.interweb.setIWToken(auth);
        if(null == auth)
        {
            this.iwKey = null;
            this.iwSecret = null;
        }
        else
        {
            this.iwKey = auth.getKey();
            this.iwSecret = auth.getSecret();
        }
    }

    public int getImageFileId()
    {
        return imageFileId;
    }

    public void setImageFileId(int imageFileId)
    {
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

    public String getPassword()
    {
        return password;
    }

    public HashMap<String, String> getPreferences()
    {
        return preferences;
    }

    public void setPreferences(HashMap<String, String> preferences)
    {
        this.preferences = preferences;
    }

    /**
     * If the password is not encrypted (plain text) set isEncrypted to false
     * 
     * @param password
     * @param isEncrypted
     */
    public void setPassword(String password, boolean isEncrypted)
    {
        if(!isEncrypted)
            password = MD5.hash(password);
        this.password = password;
    }

    public TimeZone getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    public Date getLastLoginDate() throws SQLException
    {
        if(null == lastLoginDate)
        {
            lastLoginDate = Learnweb.getInstance().getUserManager().getLastLoginDate(id);
        }
        return lastLoginDate;
    }

    public Date getCurrentLoginDate()
    {
        return currentLoginDate;
    }

    public void setCurrentLoginDate(Date currentLoginDate) throws SQLException
    {
        if(this.currentLoginDate != null)
            lastLoginDate = this.currentLoginDate;
        else
            lastLoginDate = Learnweb.getInstance().getUserManager().getLastLoginDate(id);

        this.currentLoginDate = currentLoginDate;
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

    public int getActiveCourseId()
    {
        try
        {
            if(activeCourseId == 0 && getCourses().size() > 0) // the course id wasn't set yet ; size can be 0 when the user has just been registered
            {
                activeCourseId = getCourses().get(0).getId();
            }
        }
        catch(SQLException e)
        {
            log.error("Can't load courses of user: " + getId(), e);
        }

        return activeCourseId;
    }

    public Course getActiveCourse() throws SQLException
    {
        if(null == activeCourse)
            activeCourse = Learnweb.getInstance().getCourseManager().getCourseById(getActiveCourseId());

        return activeCourse;
    }

    public void setActiveCourseId(int activeCourseId)
    {
        this.activeCourseId = activeCourseId;
        this.activeCourse = null; // clear cache;
    }

}
